package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Termux Mod (v136) — ENGINE ka $HOME shift + drop folders ki
 * pehli-dafa setup. Owner ka final faisla:
 *
 *   - Shared storage me SIRF /storage/emulated/0/TermuxMod dikhe,
 *     us ke andar sirf "Project N" drop folders.
 *   - mockup_project_<N> shared storage me KAHIN na ho (na root
 *     par, na TermuxMod ke andar) or dobara kabhi na bane.
 *   - Har project ka engine (runner, assets, inputs/outputs)
 *     $HOME/mockup_project_<N> me rehta hai — file manager me
 *     nazar hi nahi ata.
 *
 * Ye pass pehli v136 launch par (Dashboard se, storage access
 * ho tab) background me EK dafa chalta hai:
 *   1. Har known project ke liye purani jagahen (tarjeeh: pehle
 *      TermuxMod/mockup_project_<N> [v133], phir root wali
 *      [v134/v135]) se engine $HOME me ata hai; outputs DONO
 *      jagahon se TermuxMod/Project N ke Mockups / Ready
 *      Wallpapers / Batches me jate hain; pending inputs $HOME
 *      wale wallpapers folder me merge hote hain (naam takraye
 *      to suffix — overwrite kabhi nahi, image kabhi zaya nahi).
 *   2. Har source folder ki copy file-count + bytes se VERIFY
 *      hoti hai — verify ke baad hi source delete hota hai
 *      (ye delete owner ke hukam par hai: verified move hai,
 *      data loss nahi).
 *   3. TermuxMod + Project N (+ Mockups / Ready Wallpapers)
 *      folders bante hain or `dropfolders_initialized` flag lag
 *      jata hai. Is ke BAAD app koi drop folder khud kabhi nahi
 *      banati — owner delete kare to waisa hi rehta hai; wapas
 *      lane ka wahid rasta Permissions -> Recreate Folders hai.
 *
 * Faisle (source tarjeeh, child-folder routing, lifecycle gate)
 * PURE hain — unit tests me hain. IO hissa kabhi overwrite nahi
 * karta or verify se pehle kuch delete nahi karta.
 */
public final class BulkHomeMigration {

    private static final String LOG_TAG = "BulkHomeMigration";
    private static final String PREFS = "bulk_mockup";
    private static final String KEY_PREFIX = "homemig_";
    public static final String KEY_DROP_INIT = "dropfolders_initialized";
    public static final String KEY_SUMMARY_SHOWN = "homemig_summary_shown";

    public static final String BATCHES_SUB = "Batches";

    private BulkHomeMigration() {}

    // ------------------------------------------------------------ pure

    /** Engine kis purani jagah se aye ga. */
    public enum EngineSource {
        FROM_MOD,   // TermuxMod/mockup_project_<N> (v133) — pehli tarjeeh
        FROM_ROOT,  // /storage/emulated/0/mockup_project_<N> (v134/v135)
        NONE        // kahin nahi — bundled ho to fresh extract hoga
    }

    /** Source tarjeeh (pure): TermuxMod wali copy pehle. */
    public static EngineSource chooseEngineSource(boolean inModExists, boolean rootExists) {
        if (inModExists) return EngineSource.FROM_MOD;
        if (rootExists) return EngineSource.FROM_ROOT;
        return EngineSource.NONE;
    }

    /** Project folder ke andar ki cheez ka rukh. */
    public enum ChildKind {
        INPUTS,    // <N>_wallpapers -> $HOME engine ke wallpapers me merge
        MOCKUPS,   // <N>_Mockups -> TermuxMod/Project N/Mockups
        READY,     // <N>_Ready_Wallpapers -> TermuxMod/Project N/Ready Wallpapers
        BATCHES,   // Ready_Upload_Batches -> TermuxMod/Project N/Batches
        ENGINE     // baqi sab (runner, assets, subfolders) -> $HOME engine
    }

    /** Ek child (file ya folder) ka naam se rukh pehchano (pure). */
    public static ChildKind classifyChild(int n, String childName) {
        if (childName == null) return ChildKind.ENGINE;
        if (childName.equals(n + "_wallpapers")) return ChildKind.INPUTS;
        if (childName.equals(n + "_Mockups")) return ChildKind.MOCKUPS;
        if (childName.equals(n + "_Ready_Wallpapers")) return ChildKind.READY;
        if (childName.equals("Ready_Upload_Batches")) return ChildKind.BATCHES;
        return ChildKind.ENGINE;
    }

    /** Output kind ka mirror subfolder naam; inputs/engine ke liye null. */
    public static String mirrorSubfolder(ChildKind kind) {
        if (kind == null) return null;
        switch (kind) {
            case MOCKUPS: return BulkMirror.MOCKUPS_SUB;
            case READY: return BulkMirror.READY_SUB;
            case BATCHES: return BATCHES_SUB;
            default: return null;
        }
    }

    /**
     * Merge plan (pure): har source naam ke liye destination naam —
     * naam free ho to wahi, takraye to BulkDropFolders.uniqueDestName
     * wala suffix. Kabhi overwrite nahi. @return [src, dest] joray,
     * source order me; takenNames mutate hota hai (plan ke dest
     * us me shamil ho jate hain).
     */
    public static List<String[]> planMergeNames(List<String> sourceNames, Set<String> takenNames) {
        List<String[]> out = new ArrayList<>();
        if (sourceNames == null) return out;
        Set<String> taken = takenNames != null ? takenNames : new HashSet<>();
        for (String name : sourceNames) {
            String dest = BulkDropFolders.uniqueDestName(name, taken);
            taken.add(dest);
            out.add(new String[]{name, dest});
        }
        return out;
    }

    /**
     * Lifecycle gate (pure): drop folders sirf TAB bante hain jab
     * pehli-dafa setup (initialization) abhi na hua ho. Us ke baad
     * app khud kabhi nahi banati — owner ka delete kiya hua folder
     * waisa hi rehta hai.
     */
    public static boolean mayCreateDropFolders(boolean initialized) {
        return !initialized;
    }

    /** Lifecycle (pure): fetch/mirror sirf MOJOOD folder par chalta hai. */
    public static boolean shouldUseDropFolder(boolean folderExists) {
        return folderExists;
    }

    /** Ek pass ka natija (Dashboard summary ke liye). */
    public static final class Result {
        public int moved;         // projects jin ka engine $HOME me aya
        public int noSource;      // projects jin ka koi purana folder nahi tha
        public int images;        // mehfooz hue kul files (outputs + inputs)
        public int dropCreated;   // pehli setup me bane drop folders
        public boolean anyWork() {
            return moved > 0 || images > 0 || dropCreated > 0;
        }
    }

    // ------------------------------------------------------------ flags

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isMigrated(Context c, int n) {
        try {
            return prefs(c).getBoolean(KEY_PREFIX + n, false);
        } catch (Exception e) {
            return false;
        }
    }

    private static void markMigrated(Context c, int n) {
        try {
            prefs(c).edit().putBoolean(KEY_PREFIX + n, true).apply();
        } catch (Exception ignored) {}
    }

    public static boolean isDropInitialized(Context c) {
        try {
            return prefs(c).getBoolean(KEY_DROP_INIT, false);
        } catch (Exception e) {
            return false;
        }
    }

    private static void markDropInitialized(Context c) {
        try {
            prefs(c).edit().putBoolean(KEY_DROP_INIT, true).apply();
        } catch (Exception ignored) {}
    }

    public static boolean isSummaryShown(Context c) {
        try {
            return prefs(c).getBoolean(KEY_SUMMARY_SHOWN, false);
        } catch (Exception e) {
            return true; // parh na sake to popup kabhi mat dikhao
        }
    }

    public static void markSummaryShown(Context c) {
        try {
            prefs(c).edit().putBoolean(KEY_SUMMARY_SHOWN, true).apply();
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------ pass

    /** Migration ke known numbers: conf + $HOME scan + shared scan. */
    public static Set<Integer> migrationNumbers() {
        Set<Integer> out = new TreeSet<>();
        try {
            out.addAll(BulkDropFolders.knownNumbers());
        } catch (Exception ignored) {}
        try {
            out.addAll(BulkProjects.scanSharedProjects().keySet());
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Poora pehla pass: har known project ki migration (per-project
     * flag), phir drop folders ki pehli setup (ek dafa). Background
     * thread se chalao; caller storage access pehle check karta hai.
     */
    public static Result runFirstRunPass(Context context) {
        Result result = new Result();
        Set<Integer> known = migrationNumbers();
        for (int n : known) {
            if (n <= 0 || isMigrated(context, n)) continue;
            try {
                migrateProject(context, n, result);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Migration fail Project " + n + ": " + e.getMessage());
            }
        }
        if (mayCreateDropFolders(isDropInitialized(context))) {
            try {
                int[] counts = BulkDropFolders.ensureDropFoldersFull(known);
                result.dropCreated = counts[0];
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Drop folders setup fail: " + e.getMessage());
            }
            markDropInitialized(context);
        }
        return result;
    }

    private static void migrateProject(Context context, int n, Result result) throws Exception {
        File inModDir = BulkProjects.newProjectDir(n);
        File rootDir = BulkProjects.oldProjectDir(n);
        File homeDir = BulkProjects.projectDir(n);
        boolean modExists = inModDir.isDirectory();
        boolean rootExists = rootDir.isDirectory();
        EngineSource source = chooseEngineSource(modExists, rootExists);
        if (source == EngineSource.NONE) {
            result.noSource++;
            markMigrated(context, n);
            return;
        }
        File primary = source == EngineSource.FROM_MOD ? inModDir : rootDir;
        File secondary = source == EngineSource.FROM_MOD
            ? (rootExists ? rootDir : null)
            : (modExists ? inModDir : null);

        boolean homeExisted = homeDir.isDirectory();
        long[] handled = {0, 0}; // files, bytes — verify ke liye hisaab

        // 1) ENGINE: primary ki poori copy $HOME me. Home pehle se
        //    ho (fresh extract race / aadha pass) to skip-existing
        //    merge — overwrite kabhi nahi.
        if (!homeExisted) {
            BulkProjectInstaller.copyDir(primary, homeDir);
            if (!sameTreeStats(primary, homeDir)) {
                // Verify fail — aadhi copy saaf karo, flag nahi
                // lagate: agli launch par dobara koshish hogi or
                // source mehfooz para hai.
                BulkProjectInstaller.deleteRecursive(homeDir);
                Logger.logError(LOG_TAG, "Project " + n + " copy verify fail — source mehfooz hai");
                return;
            }
            long[] st = treeStats(primary);
            handled[0] += st[0];
            handled[1] += st[1];
            // Fresh copy me primary ke inputs pehle se home ke
            // andar hain — summary ke images hisaab me gin lo
            // (outputs step 4 me mirror par gine jayenge).
            result.images += treeStats(new File(primary, n + "_wallpapers"))[0];
        } else {
            handled = add(handled, mergeEngineTree(primary, homeDir, n));
        }

        // 2) SECONDARY ke engine bache-khache (jo primary me nahi
        //    thay) — skip-existing merge, overwrite kabhi nahi.
        if (secondary != null) {
            handled = add(handled, mergeEngineTree(secondary, homeDir, n));
        }

        // 3) INPUTS: dono sources ke pending wallpapers $HOME ke
        //    wallpapers folder me merge (suffix rule). Home primary
        //    se fresh copy hua ho to primary ke inputs pehle se
        //    andar hain — un ka hisaab step 1 me ho chuka.
        if (homeExisted) {
            handled = add(handled, mergeInputs(primary, homeDir, n, result));
        }
        if (secondary != null) {
            handled = add(handled, mergeInputs(secondary, homeDir, n, result));
        }

        // 4) OUTPUTS: dono sources se mirror (TermuxMod/Project N
        //    ke Mockups / Ready Wallpapers / Batches) me — suffix
        //    rule, kabhi overwrite nahi.
        handled = add(handled, moveOutputsToMirror(primary, n, result));
        if (secondary != null) {
            handled = add(handled, moveOutputsToMirror(secondary, n, result));
        }

        // 5) VERIFY + DELETE: har source ke woh hisse jo route hue
        //    thay (engine + content folders) hisaab se pure hue hon
        //    tab hi source folder delete hota hai. Fresh-copy case
        //    me step 1 ka tree-verify hi primary ki zamanat hai;
        //    content folders ka hisaab steps 3–4 se milta hai.
        //    Kuch adhoora rahe to source REHTA hai (data pehle).
        boolean primaryOk = verifySource(primary, homeDir, n, homeExisted);
        boolean secondaryOk = secondary == null || verifySource(secondary, homeDir, n, true);
        if (!primaryOk || !secondaryOk) {
            Logger.logError(LOG_TAG, "Project " + n + " verify adhoora — purana folder mehfooz rakha");
            return;
        }
        BulkProjectInstaller.deleteRecursive(primary);
        if (secondary != null) BulkProjectInstaller.deleteRecursive(secondary);
        if (primary.isDirectory() || (secondary != null && secondary.isDirectory())) {
            Logger.logError(LOG_TAG, "Project " + n + " source delete nahi ho saka");
            return;
        }

        String script = BulkProjects.detectMainScript(
            BulkProjects.listFileNames(homeDir), n);
        if (script != null) appendHomeAliases(n, script);
        BulkLog.add(BulkStrings.format(context, BulkStrings.LOG_HOMEMIG, n));
        result.moved++;
        markMigrated(context, n);
    }

    // ------------------------------------------------------------ merge helpers

    /**
     * Engine files (content folders ke ilawa sab) src -> dst merge:
     * dest na ho to copy; dest ho + size barabar to skip (present
     * gina jata hai); dest ho + size mukhtalif to source version
     * "<name>_v136" naam se copy (koi version zaya nahi hota,
     * asal file kabhi overwrite nahi hoti).
     * @return long[]{filesHandled, bytesHandled}
     */
    private static long[] mergeEngineTree(File src, File dst, int n) {
        long[] out = {0, 0};
        File[] kids = src.listFiles();
        if (kids == null) return out;
        if (!dst.isDirectory()) dst.mkdirs();
        for (File k : kids) {
            if (classifyChild(n, k.getName()) != ChildKind.ENGINE) continue;
            File d = new File(dst, k.getName());
            if (k.isDirectory()) {
                long[] sub = mergeEngineTreeRaw(k, d);
                out[0] += sub[0];
                out[1] += sub[1];
            } else {
                long[] one = mergeOneFile(k, d);
                out[0] += one[0];
                out[1] += one[1];
            }
        }
        return out;
    }

    /** Engine subfolder ke andar ka merge (andar classification nahi). */
    private static long[] mergeEngineTreeRaw(File src, File dst) {
        long[] out = {0, 0};
        File[] kids = src.listFiles();
        if (kids == null) return out;
        if (!dst.isDirectory()) dst.mkdirs();
        for (File k : kids) {
            File d = new File(dst, k.getName());
            if (k.isDirectory()) {
                long[] sub = mergeEngineTreeRaw(k, d);
                out[0] += sub[0];
                out[1] += sub[1];
            } else {
                long[] one = mergeOneFile(k, d);
                out[0] += one[0];
                out[1] += one[1];
            }
        }
        return out;
    }

    /** Ek file ka merge (upar wale usool). @return {handled 0/1, bytes} */
    private static long[] mergeOneFile(File src, File dst) {
        try {
            if (!dst.isFile()) {
                if (copyFileVerified(src, dst)) return new long[]{1, src.length()};
                return new long[]{0, 0};
            }
            if (dst.length() == src.length()) return new long[]{1, src.length()};
            File alt = new File(dst.getParentFile(), dst.getName() + "_v136");
            if (!alt.isFile() && copyFileVerified(src, alt)) return new long[]{1, src.length()};
            if (alt.isFile() && alt.length() == src.length()) return new long[]{1, src.length()};
            return new long[]{0, 0};
        } catch (Exception e) {
            return new long[]{0, 0};
        }
    }

    /**
     * Source ke <N>_wallpapers ki files home ke wallpapers folder
     * me merge (planMergeNames suffix rule; same naam + same size
     * pehle se ho to wahi file present gini jati hai — dobara copy
     * nahi). @return {handled, bytes}; result.images bhi badhta hai.
     */
    private static long[] mergeInputs(File sourceDir, File homeDir, int n, Result result) {
        long[] out = {0, 0};
        File srcWalls = new File(sourceDir, n + "_wallpapers");
        if (!srcWalls.isDirectory()) return out;
        File dstWalls = new File(homeDir, n + "_wallpapers");
        if (!dstWalls.isDirectory()) dstWalls.mkdirs();
        List<String> names = new ArrayList<>();
        for (File f : sortedFiles(srcWalls)) names.add(f.getName());
        Set<String> taken = new HashSet<>(BulkProjects.listFileNames(dstWalls));
        // Pehle se mojood same-naam files: size barabar ho to plan
        // se bahar (present), warna suffix plan me shamil.
        List<String> toPlan = new ArrayList<>();
        for (String name : names) {
            File s = new File(srcWalls, name);
            File d = new File(dstWalls, name);
            if (d.isFile() && d.length() == s.length()) {
                out[0]++;
                out[1] += s.length();
                result.images++;
            } else {
                toPlan.add(name);
            }
        }
        for (String[] pair : planMergeNames(toPlan, taken)) {
            File s = new File(srcWalls, pair[0]);
            File d = new File(dstWalls, pair[1]);
            if (copyFileVerified(s, d)) {
                out[0]++;
                out[1] += s.length();
                result.images++;
            }
        }
        return out;
    }

    /**
     * Source ke outputs (Mockups / Ready / Batches) mirror me
     * merge — inputs wala hi usool (present skip, warna suffix).
     * @return {handled, bytes}; result.images bhi badhta hai.
     */
    private static long[] moveOutputsToMirror(File sourceDir, int n, Result result) {
        long[] out = {0, 0};
        File dropDir = new File(BulkDropFolders.rootDir(), BulkDropFolders.dropFolderName(n));
        ChildKind[] kinds = {ChildKind.MOCKUPS, ChildKind.READY, ChildKind.BATCHES};
        String[] childNames = {n + "_Mockups", n + "_Ready_Wallpapers", "Ready_Upload_Batches"};
        for (int i = 0; i < kinds.length; i++) {
            File srcDir = new File(sourceDir, childNames[i]);
            if (!srcDir.isDirectory()) continue;
            File dstDir = new File(dropDir, mirrorSubfolder(kinds[i]));
            long[] sub = mergeTreeCounted(srcDir, dstDir, result);
            out[0] += sub[0];
            out[1] += sub[1];
        }
        return out;
    }

    /** Poora tree (structure mehfooz) mirror-style merge — batches ke liye bhi. */
    private static long[] mergeTreeCounted(File srcDir, File dstDir, Result result) {
        long[] out = {0, 0};
        File[] kids = srcDir.listFiles();
        if (kids == null) return out;
        List<File> files = new ArrayList<>();
        for (File k : kids) {
            if (k.isDirectory()) {
                long[] sub = mergeTreeCounted(k, new File(dstDir, k.getName()), result);
                out[0] += sub[0];
                out[1] += sub[1];
            } else {
                files.add(k);
            }
        }
        if (files.isEmpty()) return out;
        if (!dstDir.isDirectory()) dstDir.mkdirs();
        List<String> names = new ArrayList<>();
        for (File f : files) names.add(f.getName());
        Set<String> taken = new HashSet<>(BulkProjects.listFileNames(dstDir));
        List<String> toPlan = new ArrayList<>();
        for (String name : names) {
            File s = new File(srcDir, name);
            File d = new File(dstDir, name);
            if (d.isFile() && d.length() == s.length()) {
                out[0]++;
                out[1] += s.length();
                result.images++;
            } else {
                toPlan.add(name);
            }
        }
        for (String[] pair : planMergeNames(toPlan, taken)) {
            File s = new File(srcDir, pair[0]);
            File d = new File(dstDir, pair[1]);
            if (copyFileVerified(s, d)) {
                out[0]++;
                out[1] += s.length();
                result.images++;
            }
        }
        return out;
    }

    /**
     * Source delete se pehle ki zamanat: har routed file apni
     * manzil par (size ke sath) mojood hai.
     *  - ENGINE files: home me same naam + same size (ya _v136 wali)
     *  - INPUTS: home wallpapers me same naam/size ya koi suffix wali same size
     *  - OUTPUTS: mirror me same naam/size ya suffix wali same size
     * Fresh-copy case me primary ka engine hissa tree-verify se
     * pehle hi sabit hai — yahan sirf content folders dekhte hain
     * (engineCheck=false tab jab copy abhi verify hui ho).
     */
    private static boolean verifySource(File sourceDir, File homeDir, int n, boolean engineCheck) {
        File[] kids = sourceDir.listFiles();
        if (kids == null) return false;
        File dropDir = new File(BulkDropFolders.rootDir(), BulkDropFolders.dropFolderName(n));
        for (File k : kids) {
            ChildKind kind = classifyChild(n, k.getName());
            switch (kind) {
                case ENGINE:
                    if (engineCheck && !verifyTree(k, new File(homeDir, k.getName()), true)) {
                        return false;
                    }
                    break;
                case INPUTS:
                    if (!verifyDirLoose(k, new File(homeDir, k.getName()))) return false;
                    break;
                default: {
                    File dst = new File(dropDir, mirrorSubfolder(kind));
                    if (!verifyDirLoose(k, dst)) return false;
                    break;
                }
            }
        }
        return true;
    }

    /** Sakht verify: har file same naam + same size par (ya _v136 wali par). */
    private static boolean verifyTree(File src, File dst, boolean allowAlt) {
        if (src.isDirectory()) {
            if (!dst.isDirectory()) return false;
            File[] kids = src.listFiles();
            if (kids == null) return false;
            for (File k : kids) {
                if (!verifyTree(k, new File(dst, k.getName()), allowAlt)) return false;
            }
            return true;
        }
        if (dst.isFile() && dst.length() == src.length()) return true;
        if (allowAlt) {
            File alt = new File(dst.getParentFile(), dst.getName() + "_v136");
            if (alt.isFile() && alt.length() == src.length()) return true;
        }
        return false;
    }

    /** Dheela verify: file same naam/size par ho, ya usi size ki koi suffix wali. */
    private static boolean verifyDirLoose(File srcDir, File dstDir) {
        if (!srcDir.isDirectory()) return true;
        if (!dstDir.isDirectory()) {
            File[] probe = srcDir.listFiles();
            return probe == null || probe.length == 0;
        }
        return verifyTreeLoose(srcDir, dstDir);
    }

    private static boolean verifyTreeLoose(File src, File dst) {
        if (src.isDirectory()) {
            File[] kids = src.listFiles();
            if (kids == null) return false;
            for (File k : kids) {
                if (!verifyTreeLoose(k, new File(dst, k.getName()))) return false;
            }
            return true;
        }
        if (dst.isFile() && dst.length() == src.length()) return true;
        // Suffix wali talash: base_<digits>.ext ya base_v136.ext, same size
        File parent = dst.getParentFile();
        if (parent == null) return false;
        String name = src.getName();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        File[] sibs = parent.listFiles();
        if (sibs == null) return false;
        for (File s : sibs) {
            if (!s.isFile() || s.length() != src.length()) continue;
            String sn = s.getName();
            if (sn.equals(name)) return true;
            if (sn.startsWith(base + "_") && sn.endsWith(ext)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------ file helpers

    private static List<File> sortedFiles(File dir) {
        List<File> out = new ArrayList<>();
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File k : kids) if (k.isFile()) out.add(k);
        }
        out.sort((a, b) -> a.getName().compareTo(b.getName()));
        return out;
    }

    private static long[] add(long[] a, long[] b) {
        return new long[]{a[0] + b[0], a[1] + b[1]};
    }

    /** Dono trees ki files ki kul tadaad/size barabar hai? */
    private static boolean sameTreeStats(File a, File b) {
        long[] sa = treeStats(a);
        long[] sb = treeStats(b);
        return sa[0] == sb[0] && sa[1] == sb[1];
    }

    /** @return long[]{fileCount, totalBytes} */
    private static long[] treeStats(File dir) {
        long[] out = {0, 0};
        File[] kids = dir != null ? dir.listFiles() : null;
        if (kids == null) return out;
        for (File k : kids) {
            if (k.isDirectory()) {
                long[] sub = treeStats(k);
                out[0] += sub[0];
                out[1] += sub[1];
            } else {
                out[0]++;
                out[1] += k.length();
            }
        }
        return out;
    }

    /** Copy + size verify (BulkDropFolders wala hi pattern). */
    private static boolean copyFileVerified(File src, File dest) {
        InputStream in = null;
        OutputStream out = null;
        try {
            File parent = dest.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            out.flush();
            if (dest.length() != src.length()) {
                //noinspection ResultOfMethodCallIgnored
                dest.delete();
                return false;
            }
            return true;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Copy fail " + src.getName() + ": " + e.getMessage());
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Shift hue project ke $HOME wale aliases ~/.bashrc me APPEND
     * (poori commands — aliasLines resolution se $HOME path hi
     * banta hai). Purani lines chhedi nahi jatin; baad wali jeet-ti
     * hain.
     */
    private static void appendHomeAliases(int n, String script) {
        try {
            File bashrc = new File(BulkMockupSetup.homeDir(), ".bashrc");
            StringBuilder add = new StringBuilder();
            add.append("\n# Termux Mod v136: Project ").append(n)
                .append(" ka engine ab app ke andar ($HOME) hai\n");
            for (String line : BulkProjects.aliasLines(n, script)) {
                add.append(line).append("\n");
            }
            try (FileOutputStream out = new FileOutputStream(bashrc, true)) {
                out.write(add.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Alias append fail Project " + n + ": " + e.getMessage());
        }
    }
}
