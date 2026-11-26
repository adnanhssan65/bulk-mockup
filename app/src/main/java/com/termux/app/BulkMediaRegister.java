package com.termux.app;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
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
import java.util.concurrent.atomic.AtomicLong;
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
 * v150 — teen mazid durustgiyan (owner-approved 2026-11-26):
 *  1. CHUNKING: DATA IN (...) wali query/delete ab 300 paths
 *     ke tukron me hoti hai (purani SQLite 999-bind had —
 *     dost ke 500 images = 1000 files par purana pass chup
 *     chap abort ho jata tha). Chunk natije Java me
 *     (DATE_ADDED, _ID) tartib se merge hote hain.
 *  2. TIE = BREAK: tartib check me do SATH-WALI files ka
 *     DATE_ADDED second barabar ho to wo bhi break hai —
 *     sirf second-resolution wale pickers (TikTok) unhi ko
 *     ghumate hain, jabke _ID tiebreak par hamara check
 *     unhe "sahi" keh deta tha.
 *  3. DATE_ADDED LADDER: ek dafa ki capability probe
 *     (scan → update → read-back → delete-test, natija
 *     cache) pass ho to re-register shuda files ki
 *     DATE_ADDED ContentResolver se 1-second-step ladder
 *     (aakhri file = abhi ka second) se set ho jati hai —
 *     koi intezar nahi. Probe fail ho to purana chained
 *     scanFile, lekin har registration agle second me
 *     girne ki spacing ke sath. Sab kuch usi Order Fix
 *     switch ke neeche hai.
 *  Pass shuru honay se pehle BulkApiServer ki scan queue
 *  ka drain (zyada se zyada 20s) bhi isi liye hai ke query
 *  aadhi-lagi tartib na parhe.
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

    /** v150: DATA IN (...) ki har chunk me zyada se zyada paths
     *  (purani SQLite ki 999-bind had se mehfooz faasla). */
    static final int DATA_IN_CHUNK = 300;
    /** v150: DATE_ADDED probe ka cached natija (SharedPreferences). */
    private static final String KEY_PROBE = "dateadded_probe150";
    static final int PROBE_UNKNOWN = 0;
    static final int PROBE_OK = 1;
    static final int PROBE_FAIL = 2;
    /** v150: pass se pehle server scan queue ke drain ki had. */
    static final long SERVER_DRAIN_MS = 20_000L;

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
     * (pehli dafa) to OFF — v151 se fresh-install default OFF
     * hai (owner 2026-11-26: terminal runs ko Order Fix ki
     * zaroorat nahi). Saved value hamesha izzat pati hai.
     */
    public static boolean resolveEnabled(Boolean stored) {
        return stored != null && stored;
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

    // ------------------------------------------------------------ v150 pure helpers

    /** MediaStore ki ek row ki pehchan (pure): path + DATE_ADDED (seconds) + _ID. */
    public static final class RowInfo {
        public final String path;
        public final long dateAdded;
        public final long id;

        public RowInfo(String path, long dateAdded, long id) {
            this.path = path;
            this.dateAdded = dateAdded;
            this.id = id;
        }
    }

    /**
     * Paths ko barabar tukron me banto (pure) — har tukra zyada
     * se zyada chunkSize ka. DATA IN (...) wali query/delete
     * isi se chalti hain taake 999-bind wali had kabhi na aye.
     * Input mutate nahi hota; tartib mehfooz rehti hai.
     */
    public static List<List<String>> chunkPaths(List<String> paths, int chunkSize) {
        List<List<String>> out = new ArrayList<>();
        if (paths == null || paths.isEmpty()) return out;
        int size = chunkSize > 0 ? chunkSize : DATA_IN_CHUNK;
        for (int i = 0; i < paths.size(); i += size) {
            out.add(new ArrayList<>(
                paths.subList(i, Math.min(paths.size(), i + size))));
        }
        return out;
    }

    /**
     * Rows ki kul tartib (pure): DATE_ADDED barhti hui, barabar
     * second par _ID. Chunk queries ke natije isi se merge
     * hote hain (har chunk apne andar sorted hota hai, lekin
     * chunks ke darmiyan tartib sirf yahan se pakki hoti hai).
     * Input mutate nahi hota.
     */
    public static List<RowInfo> sortRows(List<RowInfo> rows) {
        List<RowInfo> out = new ArrayList<>();
        if (rows != null) out.addAll(rows);
        out.sort((a, b) -> {
            if (a.dateAdded != b.dateAdded) {
                return Long.compare(a.dateAdded, b.dateAdded);
            }
            return Long.compare(a.id, b.id);
        });
        return out;
    }

    /**
     * SMART pass ka faisla — TIE wali sakhti ke sath (pure).
     * smartFixSuffix wale usool (row gayab = break, peeche
     * reh jana/crossing = break) ke ilawa: do SATH-WALI
     * (expected tartib me lagatar) files ka DATE_ADDED second
     * BARABAR ho to baad wali file se break — kyunke sirf
     * second tak parhne wale pickers (TikTok) ke paas un ki
     * tartib ka koi saboot nahi hota, wo unhe ghuma dete
     * hain; _ID tiebreak par wo "sahi" nazar ati theen.
     */
    public static List<String> smartFixSuffixWithTies(List<String> numericOrder,
                                                      List<RowInfo> rows) {
        List<String> out = new ArrayList<>();
        if (numericOrder == null || numericOrder.isEmpty()) return out;
        Map<String, RowInfo> byPath = new HashMap<>();
        Map<String, Integer> pos = new HashMap<>();
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                RowInfo r = rows.get(i);
                if (r == null || r.path == null) continue;
                if (!byPath.containsKey(r.path)) {
                    byPath.put(r.path, r);
                    pos.put(r.path, i);
                }
            }
        }
        int lastPos = -1;
        RowInfo prev = null;
        int breakAt = -1;
        for (int i = 0; i < numericOrder.size(); i++) {
            String p = numericOrder.get(i);
            RowInfo r = byPath.get(p);
            if (r == null) {
                breakAt = i;
                break;
            }
            int position = pos.get(p);
            if (position < lastPos) {
                breakAt = i;
                break;
            }
            if (prev != null && r.dateAdded == prev.dateAdded) {
                breakAt = i;
                break;
            }
            lastPos = position;
            prev = r;
        }
        if (breakAt < 0) return out;
        out.addAll(numericOrder.subList(breakAt, numericOrder.size()));
        return out;
    }

    /**
     * DATE_ADDED ladder (pure): count files ke liye 1-second
     * step — aakhri file = nowSec, har pichli file us se 1
     * second peeche. Tartib registration wali hi hai (index 0
     * = pehle register hui file).
     */
    public static long[] dateAddedLadder(int count, long nowSec) {
        if (count <= 0) return new long[0];
        long[] out = new long[count];
        for (int i = 0; i < count; i++) {
            out[i] = nowSec - (count - 1 - i);
        }
        return out;
    }

    /**
     * Spacing ka hisaab (pure): pichli registration ka callback
     * prevCallbackMs par aya tha — agli registration is waqt
     * se pehle NAHI girni chahiye: agle second ki boundary
     * + 50ms. Abhi ka waqt nowMs ho to kitni dair rukna hai
     * (kabhi manfi nahi).
     */
    public static long spacingDelayMs(long prevCallbackMs, long nowMs) {
        long target = (prevCallbackMs / 1000L + 1L) * 1000L + 50L;
        return Math.max(0L, target - nowMs);
    }

    // ------------------------------------------------------------ Android glue

    /** Item F: Order Fix switch abhi ON hai? (fresh default OFF — v151) */
    public static boolean isOrderFixEnabled(Context ctx) {
        if (ctx == null) return false;
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Boolean stored = prefs.contains(KEY_ENABLED)
                ? prefs.getBoolean(KEY_ENABLED, true) : null;
            return resolveEnabled(stored);
        } catch (Exception e) {
            return false;
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
     *   - Nayi/badli files ke liye SMART pass: pehle chunked
     *     queries se un ki asal MediaStore tartib parho (v150:
     *     pehle server queue drain; same-second tie bhi break
     *     hai). Tartib sahi ho
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

            // v150: runner ke apne scans BulkApiServer ki FIFO
            // line se lagte hain — query se pehle wo line drain
            // ho jaye (zyada se zyada 20s), warna query aadhi
            // lagi hui tartib parh kar ghalat faisla kare gi.
            try { BulkApiServer.awaitIdle(SERVER_DRAIN_MS); } catch (Exception ignored) {}

            // Step 1: chunked queries — nayi files ki ASAL tartib
            // (DATE_ADDED, phir _ID) parho; fail ho to pass abort
            List<RowInfo> rows = queryRows(ctx, plan.toRegister);
            if (rows == null) {
                // Query hi fail — is pass me kuch settle nahi hoga
                return 0;
            }
            List<String> actualOrder = new ArrayList<>();
            for (RowInfo r : rows) actualOrder.add(r.path);
            Set<String> rowsPresent = new HashSet<>(actualOrder);

            // Step 2: SMART faisla (v150: same-second TIE bhi
            // break hai) — tartib pehle se sahi ho to sab
            // settle, na delete na scan (ye aam halat hai)
            List<String> suffix = smartFixSuffixWithTies(plan.toRegister, rows);
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

            // Step 4: chained scanFile — ek ek karke, numeric tartib.
            // v150: pehle ek-dafa ki DATE_ADDED probe (cached) —
            // pass ho to FAST PATH (registration ke baad ladder
            // se seconds set, koi intezar nahi); fail ho to
            // FALLBACK spacing (har registration agle second me).
            boolean fastPath = false;
            if (!proceed.isEmpty()) {
                int probe = probeState(ctx);
                if (probe == PROBE_UNKNOWN) {
                    probe = runProbe(ctx) ? PROBE_OK : PROBE_FAIL;
                    saveProbeState(ctx, probe);
                }
                fastPath = probe == PROBE_OK;
            }
            int done = 0;
            int consecutiveTimeouts = 0;
            long prevCallbackMs = -1L;
            List<String> succeeded = new ArrayList<>();
            for (String path : proceed) {
                if (!fastPath && prevCallbackMs > 0) {
                    long delay = spacingDelayMs(
                        prevCallbackMs, System.currentTimeMillis());
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                AtomicLong callbackMs = new AtomicLong(-1L);
                int outcome = scanOne(ctx, path, callbackMs);
                if (callbackMs.get() > 0) {
                    prevCallbackMs = callbackMs.get();
                } else if (outcome != SCAN_FAILED) {
                    // Callback ka waqt na mila (timeout) — return
                    // ka waqt hi hifazati reference hai.
                    prevCallbackMs = System.currentTimeMillis();
                }
                if (outcome == SCAN_OK) {
                    finalKeys.add(entryKey(path, sigs.get(path)));
                    succeeded.add(path);
                    done++;
                    consecutiveTimeouts = 0;
                    if (!fastPath && done % 25 == 0 && done < proceed.size()) {
                        BulkLog.add("Order fix " + done + "/" + proceed.size());
                    }
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
            if (fastPath && !succeeded.isEmpty()) {
                applyDateAddedLadder(ctx, succeeded);
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
     * MediaStore queries (v150: 300-path chunks): diye gaye
     * paths ki rows ASAL tartib me — DATE_ADDED barhti hui,
     * barabar second par _ID (row lagne ki asal tartib hamesha
     * yahi hoti hai). Har chunk apne andar sorted ata hai;
     * chunk natije sortRows se merge hote hain. Jo file list
     * me nahi ati, us ki row abhi lagi hi nahi. Koi bhi chunk
     * fail ho to null (pass abort — kuch settle nahi hota).
     */
    private static List<RowInfo> queryRows(Context ctx, List<String> paths) {
        List<RowInfo> all = new ArrayList<>();
        if (paths == null || paths.isEmpty()) return all;
        try {
            ContentResolver cr = ctx.getContentResolver();
            String[] projection = {
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns._ID
            };
            String sort = MediaStore.MediaColumns.DATE_ADDED + " ASC, "
                + MediaStore.MediaColumns._ID + " ASC";
            for (List<String> chunk : chunkPaths(paths, DATA_IN_CHUNK)) {
                StringBuilder sel = new StringBuilder(MediaStore.MediaColumns.DATA)
                    .append(" IN (");
                String[] args = new String[chunk.size()];
                for (int i = 0; i < chunk.size(); i++) {
                    if (i > 0) sel.append(',');
                    sel.append('?');
                    args[i] = chunk.get(i);
                }
                sel.append(')');
                Cursor cursor = null;
                try {
                    cursor = cr.query(MediaStore.Files.getContentUri("external"),
                        projection, sel.toString(), args, sort);
                    if (cursor == null) return null;
                    int dIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                    int aIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);
                    int iIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
                    if (dIdx < 0) return null;
                    while (cursor.moveToNext()) {
                        String p = cursor.getString(dIdx);
                        if (p == null) continue;
                        long added = aIdx >= 0 ? cursor.getLong(aIdx) : 0L;
                        long id = iIdx >= 0 ? cursor.getLong(iIdx) : 0L;
                        all.add(new RowInfo(p, added, id));
                    }
                } finally {
                    try { if (cursor != null) cursor.close(); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            return null;
        }
        return sortRows(all);
    }

    /** Ek hi path ki pehli row (probe ke liye); na mile to null. */
    private static RowInfo querySingleRow(Context ctx, String path) {
        List<RowInfo> rows = queryRows(ctx, java.util.Collections.singletonList(path));
        if (rows == null || rows.isEmpty()) return null;
        return rows.get(0);
    }

    /** Ek path ki kul rows ki tadaad (probe ke liye); fail par -1. */
    private static int countRows(Context ctx, String path) {
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DATA + " = ?",
                new String[]{path}, null);
            if (cursor == null) return -1;
            return cursor.getCount();
        } catch (Exception e) {
            return -1;
        } finally {
            try { if (cursor != null) cursor.close(); } catch (Exception ignored) {}
        }
    }

    // ------------------------------------------------------------ v150 probe + ladder

    private static int probeState(Context ctx) {
        try {
            return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_PROBE, PROBE_UNKNOWN);
        } catch (Exception e) {
            return PROBE_UNKNOWN;
        }
    }

    private static void saveProbeState(Context ctx, int state) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_PROBE, state).apply();
        } catch (Exception ignored) {}
    }

    /**
     * Ek-dafa ki capability probe: is device/provider par
     * ContentResolver.update se DATE_ADDED asal me badalta
     * hai ya nahi? Chhoti temp image (TermuxMod root me)
     * scan karke row banao, phir:
     *   (a) DATE_ADDED = abhi-10000s update karke read-back
     *       — value hu-ba-hu mili?
     *   (b) us path ki rows bilkul 1 hon or koi "(1)" wali
     *       sibling file na bani ho;
     *   (c) row delete karne ke baad temp FILE disk par
     *       zinda ho (Android 10+ par row delete se asal
     *       file bhi ud sakti hai — yehi sab se khatarnak
     *       imkaan hai, is liye ye test lazmi hai).
     * Temp file har surat me saaf hoti hai. Koi bhi qadam
     * fail → probe FAIL (fallback spacing hamesha mehfooz
     * hai).
     */
    private static boolean runProbe(Context ctx) {
        File tmp = null;
        Uri rowUri = null;
        try {
            File root = BulkDropFolders.rootDir();
            if (!root.isDirectory() && !root.mkdirs()) return false;
            tmp = new File(root, "bulk_probe150.jpg");
            Bitmap bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            FileOutputStream fos = new FileOutputStream(tmp);
            try {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            } finally {
                fos.close();
                bmp.recycle();
            }
            if (!tmp.isFile() || tmp.length() == 0) return false;
            String path = tmp.getAbsolutePath();
            if (scanOne(ctx, path) != SCAN_OK) return false;
            RowInfo row = querySingleRow(ctx, path);
            if (row == null) return false;
            rowUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, row.id);
            long targetSec = System.currentTimeMillis() / 1000L - 10_000L;
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATE_ADDED, targetSec);
            if (ctx.getContentResolver().update(rowUri, values, null, null) <= 0) {
                return false;
            }
            RowInfo after = querySingleRow(ctx, path);
            if (after == null || after.dateAdded != targetSec) return false;
            if (countRows(ctx, path) != 1) return false;
            if (new File(root, "bulk_probe150 (1).jpg").exists()) return false;
            if (ctx.getContentResolver().delete(rowUri, null, null) <= 0) {
                return false;
            }
            rowUri = null; // row ja chuki — finally dobara na mare
            return tmp.isFile();
        } catch (Exception e) {
            return false;
        } finally {
            // Jo row reh gayi ho use best-effort saaf karo,
            // phir temp file har surat me delete.
            if (rowUri != null) {
                try { ctx.getContentResolver().delete(rowUri, null, null); }
                catch (Exception ignored) {}
            }
            try { if (tmp != null && tmp.isFile()) tmp.delete(); }
            catch (Exception ignored) {}
        }
    }

    /**
     * FAST PATH: register ho chuki files ki DATE_ADDED ek
     * 1-second-step ladder se set karo (aakhri file = abhi
     * ka second). applyBatch chunk-wise; batch fail ho to
     * per-file update ki koshish. Ye best-effort hai — fail
     * ho jaye to files apni asal (scan wali) DATE_ADDED par
     * rehti hain, pass phir bhi kamyab hai.
     */
    private static void applyDateAddedLadder(Context ctx, List<String> orderedPaths) {
        if (orderedPaths == null || orderedPaths.isEmpty()) return;
        long[] ladder = dateAddedLadder(
            orderedPaths.size(), System.currentTimeMillis() / 1000L);
        try {
            String authority =
                MediaStore.Files.getContentUri("external").getAuthority();
            for (List<String> chunk : chunkPaths(orderedPaths, DATA_IN_CHUNK)) {
                int base = orderedPaths.indexOf(chunk.get(0));
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                for (int i = 0; i < chunk.size(); i++) {
                    String path = chunk.get(i);
                    Uri uri = "video".equals(mediaKind(path))
                        ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    ops.add(ContentProviderOperation.newUpdate(uri)
                        .withSelection(MediaStore.MediaColumns.DATA + " = ?",
                            new String[]{path})
                        .withValue(MediaStore.MediaColumns.DATE_ADDED,
                            ladder[base + i])
                        .build());
                }
                ctx.getContentResolver().applyBatch(authority, ops);
            }
        } catch (Exception e) {
            try {
                for (int i = 0; i < orderedPaths.size(); i++) {
                    String path = orderedPaths.get(i);
                    Uri uri = "video".equals(mediaKind(path))
                        ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DATE_ADDED, ladder[i]);
                    ctx.getContentResolver().update(uri, v,
                        MediaStore.MediaColumns.DATA + " = ?",
                        new String[]{path});
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Ek media qisam ki tamam rows delete — v150: 300-path
     * chunks me (999-bind had se bachne ke liye), har chunk
     * apni delete call. File ko hath nahi lagta — sirf record.
     * Koi chunk fail ho to poora delete fail mana jata hai.
     */
    private static boolean deleteRows(Context ctx, String kind, List<String> paths) {
        if (paths == null || paths.isEmpty()) return true;
        try {
            Uri uri = "video".equals(kind)
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            for (List<String> chunk : chunkPaths(paths, DATA_IN_CHUNK)) {
                StringBuilder sel = new StringBuilder(MediaStore.MediaColumns.DATA)
                    .append(" IN (");
                String[] args = new String[chunk.size()];
                for (int i = 0; i < chunk.size(); i++) {
                    if (i > 0) sel.append(',');
                    sel.append('?');
                    args[i] = chunk.get(i);
                }
                sel.append(')');
                ctx.getContentResolver().delete(uri, sel.toString(), args);
            }
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
        return scanOne(ctx, path, null);
    }

    /**
     * scanOne — callback ka waqt (ms) callbackMsOut me bhi
     * deta hai (v150 ki spacing isi par chalti hai); callback
     * na aye to callbackMsOut -1 hi rehta hai.
     */
    private static int scanOne(Context ctx, String path, AtomicLong callbackMsOut) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean fired = new AtomicBoolean(false);
        final AtomicReference<Uri> result = new AtomicReference<>();
        try {
            MediaScannerConnection.scanFile(ctx, new String[]{path}, null,
                (scannedPath, uri) -> {
                    if (callbackMsOut != null) {
                        callbackMsOut.set(System.currentTimeMillis());
                    }
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
