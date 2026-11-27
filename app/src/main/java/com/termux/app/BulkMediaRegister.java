package com.termux.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
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
 * $HOME ke engine folders kabhi nahi.
 *
 * v154 — ORDER/SEQUENCE family KHATAM (owner ka faisla):
 * More Settings se Order Fix / Sequence Fix / Har File Alag
 * Second teeno options aur un ke execution raste (query /
 * delete / ladder / probe / sequence write) hat gaye. Ab
 * pass sirf PLAIN REGISTRATION hai: Pairs folder ki woh
 * files jo abhi register nahi huin (aam tor par is run ki
 * nayi files) ek dafa scan hoti hain — pehle se register
 * shuda file dobara KABHI scan nahi hoti, is liye owner ki
 * delete ki hui image is pass se wapas zinda nahi hoti.
 * Scope ab SIRF "Pairs" subfolder hai. More Settings me
 * "Auto Scan" switch (default ON) is pass ko band kar
 * sakta hai; usi option ke andar "Gallery Scan" button ek
 * dafa ka manual pass chalata hai. "Wait Time" (seconds,
 * default 0) auto pass se pehle ka settle waqfa hai. Tamam kaam caller ke
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
    /** v154: Auto Scan switch (More Settings) — default ON. */
    private static final String KEY_AUTO_SCAN = "auto_scan_enabled";
    /** v154: Wait Time (seconds) — auto pass se pehle ka settle waqfa. */
    private static final String KEY_WAIT_SECONDS = "register_wait_seconds";
    /** v154: Wait Time ki shipped default (seconds) — reset isi par jata hai. */
    public static final int DEFAULT_WAIT_SECONDS = 0;
    /** v154: Wait Time ki zyada se zyada qeemat (seconds). */
    public static final int MAX_WAIT_SECONDS = 600;
    /** v149 Item F: Order Fix switch (More Settings) — default ON. */
    private static final String KEY_ENABLED = "order_fix_enabled";
    /** v152: Sequence Fix switch (More Settings) — default OFF. */
    private static final String KEY_SEQ_FIX = "sequence_fix_enabled";
    /** v152: Har File Alag Second switch (More Settings) — default OFF. */
    private static final String KEY_ALAG_SECOND = "alag_second_enabled";

    /** Ek file ke scan callback ka zyada se zyada intezar. */
    static final long SCAN_TIMEOUT_MS = 3000L;
    /** Itne lagatar timeout hon to pass rok do — scanner service
     *  mari hui lagti hai; entries save nahi hongi, agli pass me
     *  dobara koshish hogi. */
    static final int MAX_CONSECUTIVE_TIMEOUTS = 3;

    /** v150: DATA IN (...) ki har chunk me zyada se zyada paths
     *  (purani SQLite ki 999-bind had se mehfooz faasla). */
    static final int DATA_IN_CHUNK = 300;

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

    /** v152: Sequence Fix switch ka faisla (pure) — default OFF. */
    public static boolean resolveSequenceFix(Boolean stored) {
        return stored != null && stored;
    }

    /** v152: Har File Alag Second switch ka faisla (pure) — default OFF. */
    public static boolean resolveAlagSecond(Boolean stored) {
        return stored != null && stored;
    }

    /** Pass chalega? (pure) — teeno switches me se koi ek ON ho. */
    public static boolean anyPassEnabled(boolean orderFix, boolean seqFix,
                                         boolean alagSecond) {
        return orderFix || seqFix || alagSecond;
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

    // ------------------------------------------------------------ v152 sequence pure

    /** Ek row ki poori pehchan (pure): teeno date fields ke sath. */
    public static final class RowFull {
        public final String path;
        public final long id;
        public final long dateAdded;
        public final long dateModified;
        public final long dateTaken;

        public RowFull(String path, long id, long dateAdded,
                       long dateModified, long dateTaken) {
            this.path = path;
            this.id = id;
            this.dateAdded = dateAdded;
            this.dateModified = dateModified;
            this.dateTaken = dateTaken;
        }
    }

    /** File ki MIME qisam (pure) — insert ke liye. */
    public static String mimeTypeFor(String path) {
        if (path == null) return "image/jpeg";
        String n = path.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".mp4")) return "video/mp4";
        return "image/jpeg";
    }

    /**
     * Sequence ladder (pure): count files ke liye teeno date
     * fields ki sakht barhti hui qeemtein — har file ke liye
     * {DATE_ADDED (sec), DATE_MODIFIED (sec), DATE_TAKEN (ms)};
     * aakhri file = nowSec, har pichli us se 1 second peeche.
     * Teeno fields ek sath barhte hain, is liye jo picker jis
     * field ko bhi parhe, tartib production wali hi milti
     * hai; DATE_ADDED kabhi tie nahi hoti, is liye _ID
     * tiebreak kabhi istemal hi nahi hota.
     */
    public static long[][] sequenceLadder(int count, long nowSec) {
        if (count <= 0) return new long[0][];
        long[][] out = new long[count][];
        for (int i = 0; i < count; i++) {
            long sec = nowSec - (count - 1 - i);
            out[i] = new long[]{sec, sec, sec * 1000L};
        }
        return out;
    }

    /**
     * Self-check (pure): kin files ki asal row assigned
     * qeematon se NAHI milti (row gayab ho, ya koi date field
     * mukhtalif ho)? Sirf yehi files marammat ki haqdar hain —
     * baqi ko hath nahi lagta. checkTaken false ho to
     * DATE_TAKEN muqable se bahar (jin providers par wo field
     * likha hi nahi jata, wahan baqi do fields hi tartib ka
     * saboot hain).
     */
    public static List<String> sequenceMismatches(List<String> order,
                                                  Map<String, RowFull> rowsByPath,
                                                  Map<String, long[]> assigned,
                                                  boolean checkTaken) {
        List<String> out = new ArrayList<>();
        if (order == null) return out;
        for (String p : order) {
            RowFull r = rowsByPath != null ? rowsByPath.get(p) : null;
            long[] want = assigned != null ? assigned.get(p) : null;
            if (r == null || want == null) {
                out.add(p);
                continue;
            }
            if (r.dateAdded != want[0] || r.dateModified != want[1]
                || (checkTaken && r.dateTaken != want[2])) {
                out.add(p);
            }
        }
        return out;
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

    /** v152: Sequence Fix switch abhi ON hai? (default OFF) */
    public static boolean isSequenceFixEnabled(Context ctx) {
        if (ctx == null) return false;
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Boolean stored = prefs.contains(KEY_SEQ_FIX)
                ? prefs.getBoolean(KEY_SEQ_FIX, false) : null;
            return resolveSequenceFix(stored);
        } catch (Exception e) {
            return false;
        }
    }

    /** v152: Sequence Fix switch save karo (More Settings row). */
    public static void setSequenceFixEnabled(Context ctx, boolean on) {
        if (ctx == null) return;
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_SEQ_FIX, on).apply();
        } catch (Exception ignored) {}
    }

    /** v152: Har File Alag Second switch abhi ON hai? (default OFF) */
    public static boolean isAlagSecondEnabled(Context ctx) {
        if (ctx == null) return false;
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Boolean stored = prefs.contains(KEY_ALAG_SECOND)
                ? prefs.getBoolean(KEY_ALAG_SECOND, false) : null;
            return resolveAlagSecond(stored);
        } catch (Exception e) {
            return false;
        }
    }

    /** v152: Har File Alag Second switch save karo (More Settings row). */
    public static void setAlagSecondEnabled(Context ctx, boolean on) {
        if (ctx == null) return;
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ALAG_SECOND, on).apply();
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------ v154: auto scan + wait

    /** v154: Auto Scan ka faisla (pure) — saved value na ho to ON (purana rawaiya). */
    public static boolean resolveAutoScan(Boolean stored) {
        return stored == null || stored;
    }

    /** v154: Wait Time ka faisla (pure) — default 0, hadd 0..MAX_WAIT_SECONDS. */
    public static int resolveWaitSeconds(Integer stored) {
        if (stored == null) return DEFAULT_WAIT_SECONDS;
        if (stored < 0) return 0;
        return Math.min(stored, MAX_WAIT_SECONDS);
    }

    /** v154: Auto Scan switch abhi ON hai? (default ON) */
    public static boolean isAutoScanEnabled(Context ctx) {
        if (ctx == null) return true;
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Boolean stored = prefs.contains(KEY_AUTO_SCAN)
                ? prefs.getBoolean(KEY_AUTO_SCAN, true) : null;
            return resolveAutoScan(stored);
        } catch (Exception e) {
            return true;
        }
    }

    /** v154: Auto Scan switch save karo (More Settings row). */
    public static void setAutoScanEnabled(Context ctx, boolean on) {
        if (ctx == null) return;
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_AUTO_SCAN, on).apply();
        } catch (Exception ignored) {}
    }

    /** v154: Wait Time (seconds) — shipped default DEFAULT_WAIT_SECONDS. */
    public static int waitSeconds(Context ctx) {
        if (ctx == null) return DEFAULT_WAIT_SECONDS;
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Integer stored = prefs.contains(KEY_WAIT_SECONDS)
                ? prefs.getInt(KEY_WAIT_SECONDS, DEFAULT_WAIT_SECONDS) : null;
            return resolveWaitSeconds(stored);
        } catch (Exception e) {
            return DEFAULT_WAIT_SECONDS;
        }
    }

    /** v154: Wait Time save karo (More Settings editor; reset bhi yahin se). */
    public static void setWaitSeconds(Context ctx, int seconds) {
        if (ctx == null) return;
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_WAIT_SECONDS, resolveWaitSeconds(seconds)).apply();
        } catch (Exception ignored) {}
    }

    /**
     * v154 — PLAIN REGISTRATION pass (ek project): Pairs folder
     * ki sirf woh files scan hoti hain jo abhi tak register nahi
     * huin (signature plan se) — aam tor par is run ki nayi
     * files. Register shuda file dobara scan NAHI hoti: owner
     * gallery se delete kar de to ye pass use wapas nahi lati,
     * or poore folder ka rescan kabhi nahi hota. Tartib
     * production (numeric) hi rehti hai — runner ki past-ladder
     * mtimes hi asal order hain, app koi date nahi badalta.
     *
     * Auto Scan OFF ho to (force=false par) pass bilkul idle.
     * Wait Time sirf auto pass par lagu hai (manual Gallery
     * Scan foran chalta hai). Caller ke background thread par
     * chalana hai (scan callbacks ka intezar hota hai).
     * @return is pass me register hui files ki tadaad
     */
    public static int registerProject(Context ctx, int n) {
        return registerProject(ctx, n, false);
    }

    /** registerProject — force=true = manual Gallery Scan (Auto Scan OFF bhi ho to chalega). */
    public static int registerProject(Context ctx, int n, boolean force) {
        if (ctx == null || n <= 0) return 0;
        if (!force && !isAutoScanEnabled(ctx)) return 0;
        try {
            if (!force) {
                int wait = waitSeconds(ctx);
                if (wait > 0) {
                    try {
                        Thread.sleep(wait * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return 0;
                    }
                }
            }
            File dropDir = new File(BulkDropFolders.rootDir(),
                BulkDropFolders.dropFolderName(n));
            if (!dropDir.isDirectory()) return 0;
            File pairsDir = new File(dropDir, BulkMirror.PAIRS_SUB);

            Map<String, String> sigs = new LinkedHashMap<>();
            collectMedia(pairsDir, sigs);
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

    /** Tamam projects ka ek pass (syncNow + manual Gallery Scan yahi bulate hain). */
    public static int registerAllProjects(Context ctx, java.util.Set<Integer> numbers,
                                          boolean force) {
        if (ctx == null || numbers == null) return 0;
        int total = 0;
        for (Integer n : numbers) {
            if (n != null && n > 0) total += registerProject(ctx, n, force);
        }
        return total;
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
