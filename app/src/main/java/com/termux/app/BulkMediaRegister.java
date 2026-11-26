package com.termux.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
 * v149 — ORDER FIX: runner ke apne scans kabhi kabhi ultay
 * lag jate hain (darkhwast jana or record lagna do alag
 * cheezein hain — 12 me se 1 pair cross ho jata tha). Owner
 * ke faisle par (2026-11-26) ye sirf NAYI files par lagu hai:
 * is version ki pehli pass maujooda backlog ko "settled" seed
 * kar deti hai, order nahi karti. More Settings me "Order Fix"
 * switch hai (default ON) — OFF ho to ye pass bilkul idle
 * rehta hai.
 *
 * v149 revision — SMART pass (owner: "order fix kam tezi sy
 * but clean way me"): nayi files ko aankh band karke dobara
 * register NAHI karte. Pehle EK query se un ki asal MediaStore
 * tartib (DATE_ADDED, phir _ID) parhi jati hai: tartib pehle
 * se sahi ho (jo runner ke scan-waqfa ke baad aam halat hai)
 * to sab settle — na delete, na scan. Sirf jahan tartib pehli
 * dafa tootti hai, WAHAN SE AAGE ki files ki purani rows
 * delete (EK query + per-qisam EK batched delete) karke
 * chained scanFile se record numeric tartib me dobara lagta
 * hai; peeche wali sahi files ko hath nahi lagta.
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
    /** v149: is project ki pehli pass ho chuki hai (backlog seed ho gaya)? */
    private static final String KEY_SEEDED_PREFIX = "seeded149_";
    /** v149 Item F: Order Fix switch (More Settings) — default ON. */
    private static final String KEY_ENABLED = "order_fix_enabled";

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

    // ------------------------------------------------------------ v149 pure helpers

    /**
     * Order Fix switch ka faisla (pure): saved value na ho
     * (pehli dafa) to ON — switch default ON hai.
     */
    public static boolean resolveEnabled(Boolean stored) {
        return stored == null || stored;
    }

    /**
     * Seed (pure): maujooda tamam files ke entryKeys — pehli
     * v149 pass inhe "settled" maan kar save karti hai, order
     * NAHI karti (owner ka faisla 2026-11-26: purani files
     * chhor do, sirf nayi files perfect hongi).
     */
    public static Set<String> seedKeys(List<String> orderedPaths,
                                       Map<String, String> signaturesByPath) {
        Set<String> out = new HashSet<>();
        if (orderedPaths != null) {
            for (String path : orderedPaths) {
                String sig = signaturesByPath != null
                    ? signaturesByPath.get(path) : null;
                if (sig != null) out.add(entryKey(path, sig));
            }
        }
        return out;
    }

    /** Media ki qisam (pure): mp4 → "video", baqi → "image". */
    public static String mediaKind(String path) {
        if (path != null && path.toLowerCase().endsWith(".mp4")) return "video";
        return "image";
    }

    /**
     * Batched delete ka plan (pure): toRegister me se sirf woh
     * files jin ki MediaStore row asal me mojood hai, media
     * qisam ke hisaab se group — har qisam ki EK delete call
     * hogi. Tartib (numeric) har group ke andar mehfooz rehti
     * hai. Row hi na ho to delete ki zaroorat hi nahi.
     */
    public static Map<String, List<String>> deletePlan(List<String> toRegister,
                                                       Set<String> rowsPresent) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        if (toRegister == null || rowsPresent == null) return out;
        for (String path : toRegister) {
            if (!rowsPresent.contains(path)) continue;
            String kind = mediaKind(path);
            List<String> group = out.get(kind);
            if (group == null) {
                group = new ArrayList<>();
                out.put(kind, group);
            }
            group.add(path);
        }
        return out;
    }

    /**
     * SMART pass ka faisla (pure): nayi files ki numeric tartib
     * or un ki ASAL MediaStore tartib (DATE_ADDED ke hisaab se,
     * sirf wohi files jo query me ayen) dekh kar — kin files ko
     * delete + dobara register karna hai.
     *
     * Asal tartib numeric tartib ki hi ek subsequence ho (har
     * file apni bari par, koi peeche nahi) to fix set KHALI hai
     * — sab settle. Pehli file jo ya to asal tartib me hai hi
     * nahi (row hi nahi — record lagna baqi hai) ya kisi pehle
     * wali file se PEHLE lag gayi hai (crossing), usi se AAGE
     * ki tamam files fix set me hain — kyunke dobara lagne
     * wali file hamesha aakhir me jati hai, is liye suffix ko
     * hi dobara lagana parta hai; prefix bilkul mehfooz rehta
     * hai. Tartib bilkul numeric ho (actual == numeric) to
     * zahir hai fix set khali hai.
     */
    public static List<String> smartFixSuffix(List<String> numericOrder,
                                              List<String> actualOrder) {
        List<String> out = new ArrayList<>();
        if (numericOrder == null || numericOrder.isEmpty()) return out;
        Map<String, Integer> pos = new HashMap<>();
        if (actualOrder != null) {
            for (int i = 0; i < actualOrder.size(); i++) {
                String p = actualOrder.get(i);
                if (p != null && !pos.containsKey(p)) pos.put(p, i);
            }
        }
        int last = -1;
        int breakAt = -1;
        for (int i = 0; i < numericOrder.size(); i++) {
            Integer p = pos.get(numericOrder.get(i));
            if (p == null || p < last) {
                breakAt = i;
                break;
            }
            last = p;
        }
        if (breakAt < 0) return out;
        out.addAll(numericOrder.subList(breakAt, numericOrder.size()));
        return out;
    }

    /**
     * Delete ke baad scan ke qabil files (pure), numeric tartib
     * me: jis file ki row thi hi nahi → seedha scan; jis ki row
     * thi or us ki qisam ki delete kamyab hui → scan; jis ki
     * qisam ki delete FAIL hui → is pass me skip (agli pass me
     * retry — aadha-order kabhi settle nahi hota).
     */
    public static List<String> proceedAfterDelete(List<String> toRegister,
                                                  Set<String> rowsPresent,
                                                  Set<String> deleteFailedKinds) {
        List<String> out = new ArrayList<>();
        if (toRegister == null) return out;
        Set<String> rows = rowsPresent != null ? rowsPresent : new HashSet<>();
        Set<String> failed = deleteFailedKinds != null
            ? deleteFailedKinds : new HashSet<>();
        for (String path : toRegister) {
            if (rows.contains(path) && failed.contains(mediaKind(path))) continue;
            out.add(path);
        }
        return out;
    }

    // ------------------------------------------------------------ Android glue

    /** Item F: Order Fix switch abhi ON hai? (default ON) */
    public static boolean isOrderFixEnabled(Context ctx) {
        if (ctx == null) return true;
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Boolean stored = prefs.contains(KEY_ENABLED)
                ? prefs.getBoolean(KEY_ENABLED, true) : null;
            return resolveEnabled(stored);
        } catch (Exception e) {
            return true;
        }
    }

    /** Item F: Order Fix switch save karo (More Settings row). */
    public static void setOrderFixEnabled(Context ctx, boolean on) {
        if (ctx == null) return;
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, on).apply();
        } catch (Exception ignored) {}
    }

    /**
     * Ek project ki NAYI gallery copies ka order fix karo.
     * Mirror sync ke foran baad, caller ke background thread par.
     *
     * v149 ka rawaiya:
     *   - Order Fix switch OFF ho to pass bilkul idle — kuch
     *     nahi hota (na seed, na delete, na scan).
     *   - Is version ki PEHLI pass (per project): maujooda tamam
     *     files "settled" seed ho jati hain — purani files ka
     *     order fix NAHI hota (owner ka faisla). Sirf is ke baad
     *     anay wali files "nayi" hain.
     *   - Nayi/badli files ke liye SMART pass: pehle EK query
     *     se un ki asal MediaStore tartib parho. Tartib sahi ho
     *     to sab settle (koi delete/scan nahi). Tartib jahan
     *     tootti hai, sirf wahan se aage ki files: har media
     *     qisam ki EK batched delete, phir har file ka scanFile
     *     numeric tartib me sakhti se ek ek karke — agli file
     *     tab jab pehli ka callback ah jaye. Is se gallery
     *     record ki tartib numeric pair tartib hi rehti hai,
     *     chahe runner ke apne scans ultay lagay hon.
     * @return is pass me order fix hui files ki tadaad
     */
    public static int registerProject(Context ctx, int n) {
        if (ctx == null || n <= 0) return 0;
        // Item F: switch OFF → pass bilkul idle
        if (!isOrderFixEnabled(ctx)) return 0;
        try {
            File dropDir = new File(BulkDropFolders.rootDir(),
                BulkDropFolders.dropFolderName(n));
            if (!dropDir.isDirectory()) return 0;
            File mockDir = new File(dropDir, BulkMirror.MOCKUPS_SUB);
            File readyDir = new File(dropDir, BulkMirror.READY_SUB);

            Map<String, String> sigs = new LinkedHashMap<>();
            collectMedia(mockDir, sigs);
            collectMedia(readyDir, sigs);
            List<String> ordered = orderByProduction(new ArrayList<>(sigs.keySet()));

            // Pehli v149 pass: backlog seed — order nahi karna
            if (!isSeeded(ctx, n)) {
                saveRegistered(ctx, n, seedKeys(ordered, sigs));
                setSeeded(ctx, n);
                return 0;
            }
            if (sigs.isEmpty()) {
                // Files hi nahi — purana record bhi saaf kar do
                saveRegistered(ctx, n, new HashSet<>());
                return 0;
            }
            Set<String> registered = loadRegistered(ctx, n);
            Plan plan = plan(ordered, sigs, registered);
            if (plan.toRegister.isEmpty()) {
                if (!plan.stillRegistered.equals(registered)) {
                    saveRegistered(ctx, n, plan.stillRegistered);
                }
                return 0;
            }

            // Step 1: EK query — nayi files ki ASAL tartib
            // (DATE_ADDED, phir _ID) parho; fail ho to pass abort
            List<String> actualOrder = queryRowOrder(ctx, plan.toRegister);
            if (actualOrder == null) {
                // Query hi fail — is pass me kuch settle nahi hoga
                return 0;
            }
            Set<String> rowsPresent = new HashSet<>(actualOrder);

            // Step 2: SMART faisla — tartib pehle se sahi ho to
            // sab settle, na delete na scan (ye aam halat hai)
            List<String> suffix = smartFixSuffix(plan.toRegister, actualOrder);
            Set<String> finalKeys = new HashSet<>(plan.stillRegistered);
            if (suffix.isEmpty()) {
                for (String p : plan.toRegister) {
                    finalKeys.add(entryKey(p, sigs.get(p)));
                }
                if (!finalKeys.equals(registered)) {
                    saveRegistered(ctx, n, finalKeys);
                }
                BulkLog.add("Project " + n + " order pehle se sahi hai");
                return 0;
            }
            // Tartib jahan tootti hai, us se pehle wali files
            // bilkul mehfooz — woh bhi settle ho jati hain
            Set<String> suffixSet = new HashSet<>(suffix);
            for (String p : plan.toRegister) {
                if (!suffixSet.contains(p)) {
                    finalKeys.add(entryKey(p, sigs.get(p)));
                }
            }

            // Step 3: sirf suffix par — har media qisam ki EK
            // batched delete (row hi na ho to delete skip)
            Set<String> deleteFailedKinds = new HashSet<>();
            Map<String, List<String>> deletes = deletePlan(suffix, rowsPresent);
            for (Map.Entry<String, List<String>> e : deletes.entrySet()) {
                if (!deleteRows(ctx, e.getKey(), e.getValue())) {
                    deleteFailedKinds.add(e.getKey());
                }
            }
            List<String> proceed = proceedAfterDelete(
                suffix, rowsPresent, deleteFailedKinds);

            // Step 4: chained scanFile — ek ek karke, numeric tartib
            int done = 0;
            int consecutiveTimeouts = 0;
            for (String path : proceed) {
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
                BulkLog.add("Project " + n + " order fix ho gaya (" + done + " files)");
            }
            return done;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Register fail Project " + n + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * EK MediaStore query: diye gaye paths ki rows ASAL tartib
     * me — DATE_ADDED barhti hui, barabar second par _ID (row
     * lagne ki asal tartib hamesha yahi hoti hai). Jo file list
     * me nahi ati, us ki row abhi lagi hi nahi. Query fail ho
     * to null (pass abort — kuch settle nahi hota).
     */
    private static List<String> queryRowOrder(Context ctx, List<String> paths) {
        List<String> out = new ArrayList<>();
        if (paths == null || paths.isEmpty()) return out;
        Cursor cursor = null;
        try {
            StringBuilder sel = new StringBuilder(MediaStore.MediaColumns.DATA)
                .append(" IN (");
            String[] args = new String[paths.size()];
            for (int i = 0; i < paths.size(); i++) {
                if (i > 0) sel.append(',');
                sel.append('?');
                args[i] = paths.get(i);
            }
            sel.append(')');
            ContentResolver cr = ctx.getContentResolver();
            cursor = cr.query(MediaStore.Files.getContentUri("external"),
                new String[]{MediaStore.MediaColumns.DATA},
                sel.toString(), args,
                MediaStore.MediaColumns.DATE_ADDED + " ASC, "
                    + MediaStore.MediaColumns._ID + " ASC");
            if (cursor == null) return null;
            int idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            if (idx < 0) return null;
            while (cursor.moveToNext()) {
                String p = cursor.getString(idx);
                if (p != null) out.add(p);
            }
            return out;
        } catch (Exception e) {
            return null;
        } finally {
            try { if (cursor != null) cursor.close(); } catch (Exception ignored) {}
        }
    }

    /** Ek media qisam ki tamam rows EK delete call me. File ko hath nahi lagta — sirf record. */
    private static boolean deleteRows(Context ctx, String kind, List<String> paths) {
        if (paths == null || paths.isEmpty()) return true;
        try {
            Uri uri = "video".equals(kind)
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            StringBuilder sel = new StringBuilder(MediaStore.MediaColumns.DATA)
                .append(" IN (");
            String[] args = new String[paths.size()];
            for (int i = 0; i < paths.size(); i++) {
                if (i > 0) sel.append(',');
                sel.append('?');
                args[i] = paths.get(i);
            }
            sel.append(')');
            ctx.getContentResolver().delete(uri, sel.toString(), args);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isSeeded(Context ctx, int n) {
        try {
            return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SEEDED_PREFIX + n, false);
        } catch (Exception e) {
            return false;
        }
    }

    private static void setSeeded(Context ctx, int n) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_SEEDED_PREFIX + n, true).apply();
        } catch (Exception ignored) {}
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
