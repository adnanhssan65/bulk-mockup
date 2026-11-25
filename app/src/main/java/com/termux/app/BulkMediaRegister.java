package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Termux Mod (v148) — gallery registration APP ke andar se.
 *
 * Masla: runners apni outputs ko termux-media-scan (Termux:API)
 * se MediaStore me register karte aye hain. Lekin Mod app owner
 * ki apni key se signed hai — official Termux:API app Mod ki
 * scan calls ko serve nahi karti. Jis phone par sirf Mod hai,
 * wahan outputs TermuxMod/Project N me to ajati hain lekin
 * MediaStore me kabhi nahi jatin: gallery / TikTok me nazar hi
 * nahi atin (Android ka apna slow volume scan unhe bohat dair
 * se, folder-grouped tartib me utha leta hai).
 *
 * Hal: mirror sync ke baad APP KHUD un gallery copies ko
 * MediaScannerConnection se register karti hai — koi Termux:API
 * nahi chahiye. Registration SEQUENCE me hoti hai (ek file ka
 * scan mukammal, phir agli) taake MediaStore ki DATE_ADDED
 * pair order (mockup → wallpaper) me hi bane. Pehle se register
 * shuda files (path+size+mtime signature) dobara nahi bheji
 * jatin; ghayab files ke record khud saaf ho jate hain.
 *
 * Scope SIRF TermuxMod/Project N ke "Mockups" aur
 * "Ready Wallpapers" subfolders hain — Batches kabhi nahi,
 * $HOME ke engine folders kabhi nahi. Tamam kaam caller ke
 * background thread par hota hai; UI ya run kabhi nahi rukta;
 * har exception khamoshi se nigal jata hai.
 *
 * Is class ka planning hissa (number tartib, signature faisla)
 * PURE hai — unit tests me. Android wala hissa (scanFile chain,
 * SharedPreferences) sirf gulaab (glue) hai.
 */
public final class BulkMediaRegister {

    private static final String LOG_TAG = "BulkMediaRegister";

    private static final String PREFS = "bulk_media_register";
    private static final String KEY_PREFIX = "registered_";

    /** Ek file ke scan callback ka zyada se zyada intezar. */
    static final long SCAN_TIMEOUT_MS = 3000L;
    /** Itne lagatar timeout hon to pass rok do — scanner service
     *  mari hui lagti hai; entries save nahi hongi, agli pass me
     *  dobara koshish hogi. */
    static final int MAX_CONSECUTIVE_TIMEOUTS = 3;

    private BulkMediaRegister() {}

    // ------------------------------------------------------------ pure

    /**
     * File naam ka numeric stem: "12.jpg" → 12, "3.png" → 3.
     * Number se shuru na ho (ya hidden naam ho) to -1.
     */
    public static long numericStem(String name) {
        if (name == null || name.startsWith(".")) return -1;
        int dot = name.indexOf('.');
        String stem = dot >= 0 ? name.substring(0, dot) : name;
        if (stem.isEmpty()) return -1;
        try {
            return Long.parseLong(stem);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Register ke qabil media: numbered outputs ki shaklein
     * (jpg/png/mp4). Hidden files kabhi nahi.
     */
    public static boolean isRegisterableMedia(String name) {
        if (name == null || name.startsWith(".")) return false;
        String n = name.toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".jpeg")
            || n.endsWith(".png") || n.endsWith(".mp4");
    }

    /** Ek gallery file ka entry: full path + signature. */
    public static String entryKey(String path, String signature) {
        return path + "|" + signature;
    }

    /**
     * Paths ko production tartib me: basename ke numeric stem ke
     * hisaab se (dono folders mila kar — pair/triple/group patterns
     * isi ek number line se bante hain). Number barabar ho to
     * Mockups wali pehle (path ki alphabetic tartib), phir naam.
     * Number wali files pehle; baghair-number wali (is istemal me
     * hoti nahi) aakhir me naam ke hisaab se. Input mutate nahi hota.
     */
    public static List<String> orderByProduction(List<String> paths) {
        List<String> out = new ArrayList<>();
        if (paths != null) out.addAll(paths);
        out.sort((a, b) -> {
            long na = numericStem(baseName(a));
            long nb = numericStem(baseName(b));
            if (na >= 0 && nb >= 0 && na != nb) return Long.compare(na, nb);
            if (na >= 0 && nb < 0) return -1;
            if (na < 0 && nb >= 0) return 1;
            int byPath = a.compareTo(b);
            if (byPath != 0) return byPath;
            return 0;
        });
        return out;
    }

    private static String baseName(String path) {
        if (path == null) return "";
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /** Registration plan (pure). */
    public static final class Plan {
        /** Tartib me woh paths jinhe (dobara) register karna hai. */
        public final List<String> toRegister;
        /** Woh purane entryKeys jo aaj bhi durust hain (file waisi ki waisi). */
        public final Set<String> stillRegistered;

        Plan(List<String> toRegister, Set<String> stillRegistered) {
            this.toRegister = toRegister;
            this.stillRegistered = stillRegistered;
        }
    }

    /**
     * Faisla (pure): current files (tartib shuda paths + un ke
     * signatures) aur pehle se register shuda entryKeys dekh kar —
     * nayi ya badli hui file → toRegister; bilkul waisi file →
     * stillRegistered (dobara scan nahi); ghayab file ka purana
     * record kisi set me nahi ata (prune khud ho jata hai, kyunke
     * agli save sirf inhi do seton se banti hai).
     */
    public static Plan plan(List<String> orderedPaths,
                            Map<String, String> signaturesByPath,
                            Set<String> registeredKeys) {
        List<String> toRegister = new ArrayList<>();
        Set<String> still = new HashSet<>();
        Set<String> registered = registeredKeys != null
            ? registeredKeys : new HashSet<>();
        if (orderedPaths != null) {
            for (String path : orderedPaths) {
                String sig = signaturesByPath != null
                    ? signaturesByPath.get(path) : null;
                if (sig == null) continue;
                String key = entryKey(path, sig);
                if (registered.contains(key)) {
                    still.add(key);
                } else {
                    toRegister.add(path);
                }
            }
        }
        return new Plan(toRegister, still);
    }

    // ------------------------------------------------------------ Android glue

    /**
     * Ek project ki gallery copies register karo (jo nayi/badli
     * hon). Mirror sync ke foran baad, caller ke background
     * thread par chalaya jata hai. Drop folder mojood na ho to
     * khamosh skip.
     * @return is pass me register hui files ki tadaad
     */
    public static int registerProject(Context ctx, int n) {
        if (ctx == null || n <= 0) return 0;
        try {
            File dropDir = new File(BulkDropFolders.rootDir(),
                BulkDropFolders.dropFolderName(n));
            if (!dropDir.isDirectory()) return 0;
            File mockDir = new File(dropDir, BulkMirror.MOCKUPS_SUB);
            File readyDir = new File(dropDir, BulkMirror.READY_SUB);

            Map<String, String> sigs = new LinkedHashMap<>();
            collectMedia(mockDir, sigs);
            collectMedia(readyDir, sigs);
            if (sigs.isEmpty()) {
                // Files hi nahi — purana record bhi saaf kar do
                saveRegistered(ctx, n, new HashSet<>());
                return 0;
            }
            List<String> ordered = orderByProduction(new ArrayList<>(sigs.keySet()));
            Set<String> registered = loadRegistered(ctx, n);
            Plan plan = plan(ordered, sigs, registered);

            Set<String> finalKeys = new HashSet<>(plan.stillRegistered);
            int done = 0;
            int consecutiveTimeouts = 0;
            for (String path : plan.toRegister) {
                int outcome = scanOne(ctx, path);
                if (outcome == SCAN_OK) {
                    finalKeys.add(entryKey(path, sigs.get(path)));
                    done++;
                    consecutiveTimeouts = 0;
                } else if (outcome == SCAN_TIMEOUT) {
                    consecutiveTimeouts++;
                    if (consecutiveTimeouts >= MAX_CONSECUTIVE_TIMEOUTS) {
                        Logger.logError(LOG_TAG,
                            "Scanner jawab nahi de raha — Project " + n + " pass roka");
                        break;
                    }
                }
                // SCAN_FAILED: entry save nahi hoti — agli pass me retry
            }
            if (!finalKeys.equals(registered)) {
                saveRegistered(ctx, n, finalKeys);
            }
            if (done > 0) {
                BulkLog.add("Project " + n + " gallery scan ho gaya (" + done + " files)");
            }
            return done;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Register fail Project " + n + ": " + e.getMessage());
            return 0;
        }
    }

    /** Ek folder ki numbered media files ke signatures jama karo. */
    private static void collectMedia(File dir, Map<String, String> out) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.isFile()) continue;
            String name = f.getName();
            if (!isRegisterableMedia(name)) continue;
            if (numericStem(name) < 0) continue;
            out.put(f.getAbsolutePath(),
                BulkMirror.signature(f.length(), f.lastModified()));
        }
    }

    private static final int SCAN_OK = 0;
    private static final int SCAN_FAILED = 1;
    private static final int SCAN_TIMEOUT = 2;

    /**
     * Ek file ka scan — callback ka intezar latch se (taake
     * tartib bani rahe), lekin SCAN_TIMEOUT_MS se zyada kabhi
     * nahi: callback na aye to agay barh jao, pass nahi rukta.
     * Callback jis thread par bhi aye, latch use jaga deta hai.
     */
    private static int scanOne(Context ctx, String path) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean fired = new AtomicBoolean(false);
        final AtomicReference<Uri> result = new AtomicReference<>();
        try {
            MediaScannerConnection.scanFile(ctx, new String[]{path}, null,
                (scannedPath, uri) -> {
                    result.set(uri);
                    fired.set(true);
                    latch.countDown();
                });
        } catch (Exception e) {
            return SCAN_FAILED;
        }
        try {
            boolean completed = latch.await(SCAN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!completed || !fired.get()) return SCAN_TIMEOUT;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SCAN_FAILED;
        }
        return result.get() != null ? SCAN_OK : SCAN_FAILED;
    }

    private static Set<String> loadRegistered(Context ctx, int n) {
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Set<String> stored = prefs.getStringSet(KEY_PREFIX + n, null);
            return stored != null ? new HashSet<>(stored) : new HashSet<>();
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    private static void saveRegistered(Context ctx, int n, Set<String> keys) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_PREFIX + n, new HashSet<>(keys)).apply();
        } catch (Exception ignored) {}
    }

    /** Test/debug ke liye: is project ke saved entryKeys ki copy. */
    static Set<String> registeredKeysForTest(Context ctx, int n) {
        return loadRegistered(ctx, n);
    }
}
