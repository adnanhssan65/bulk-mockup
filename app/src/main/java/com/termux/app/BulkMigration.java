package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Termux Mod (v133) — project folders ki SAFE shift (relocation).
 *
 * Usool (owner ki manzoori shuda scheme):
 *   - Nayi jagah: /storage/emulated/0/TermuxMod/mockup_project_<N>
 *   - Har project SIRF EK dafa check hota hai (persisted flag).
 *   - Shift SIRF tab jab project ke asal runner ka text ye sabit
 *     kare ke wo folder ki jagah env/__file__ se leta hai
 *     ("MOCKUP_BASE" ya "__file__" mojood) — warna project purani
 *     jagah par hi rehta hai or resolution ki wajah se bilkul
 *     pehle ki tarah chalta rehta hai.
 *   - Destination par folder pehle se ho to KABHI overwrite nahi;
 *     kuch bhi delete nahi hota (copy fallback me bhi source sirf
 *     size-verify ke baad hat-ta hai — asal move ka hissa).
 *   - Shift hue project ke liye ~/.bashrc me corrected alias lines
 *     APPEND hoti hain (purani lines chhedi nahi jatin — nayi lines
 *     baad me hone ki wajah se jeet-ti hain), taake Original Termux
 *     se bhi project nayi jagah se chale.
 */
public final class BulkMigration {

    private static final String LOG_TAG = "BulkMigration";
    private static final String PREFS = "bulk_mockup";
    private static final String KEY_PREFIX = "migrated_";
    public static final String KEY_SUMMARY_SHOWN = "migration_summary_shown";

    private BulkMigration() {}

    // ------------------------------------------------------------ pure

    /**
     * Runner ka text relocation-safe hai? Sirf tab jab folder ki
     * jagah hardcoded na ho: text me "MOCKUP_BASE" (env pattern)
     * ya "__file__" (script-relative) mojood ho.
     */
    public static boolean isRelocationSafe(String runnerText) {
        if (runnerText == null) return false;
        return runnerText.contains("MOCKUP_BASE") || runnerText.contains("__file__");
    }

    /** Ek pass ka natija. */
    public static final class Result {
        public int moved;
        public int stayed;
        public int evaluated; // moved + stayed (jin par asal faisla hua)
    }

    // ------------------------------------------------------------ flags

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isDone(Context c, int n) {
        try {
            return prefs(c).getBoolean(KEY_PREFIX + n, false);
        } catch (Exception e) {
            return false;
        }
    }

    private static void markDone(Context c, int n) {
        try {
            prefs(c).edit().putBoolean(KEY_PREFIX + n, true).apply();
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

    /**
     * Ek migration pass: har known project jo abhi tak check nahi
     * hua or jis ka folder PURANI jagah par hai. Background thread
     * se chalao (file IO hai). Storage access na ho to caller hi
     * na bulaye.
     */
    public static Result migrateAll(Context context) {
        Result result = new Result();
        Set<Integer> known = BulkDropFolders.knownNumbers();
        if (known == null || known.isEmpty()) return result;
        for (int n : known) {
            if (n <= 0 || isDone(context, n)) continue;
            try {
                evaluate(context, n, result);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Migration fail Project " + n + ": " + e.getMessage());
            }
        }
        return result;
    }

    private static void evaluate(Context context, int n, Result result) throws Exception {
        File oldDir = BulkProjects.oldProjectDir(n);
        File newDir = BulkProjects.newProjectDir(n);
        if (!oldDir.isDirectory()) {
            // Purani jagah kuch hai hi nahi — shift ka sawal hi nahi
            markDone(context, n);
            return;
        }
        if (newDir.isDirectory()) {
            // Destination pehle se hai — kabhi overwrite nahi.
            // Resolution nayi jagah ko tarjeeh deta hai, is liye
            // project wahin se chalta rahega.
            markDone(context, n);
            return;
        }
        String script = BulkProjects.detectMainScript(
            BulkProjects.listFileNames(oldDir), n);
        String text = null;
        if (script != null) {
            File runner = new File(oldDir, script);
            if (runner.isFile()) text = BulkProjectInstaller.readText(runner);
        }
        if (!isRelocationSafe(text)) {
            result.stayed++;
            result.evaluated++;
            BulkLog.add("Project " + n + " purani jagah par hi rahega (runner purana hai — bilkul pehle ki tarah chalega)");
            markDone(context, n);
            return;
        }
        File parent = newDir.getParentFile();
        if (parent != null && !parent.isDirectory()) parent.mkdirs();
        boolean movedOk = moveFolder(oldDir, newDir);
        if (!movedOk) {
            // Shift na ho saki — project purani jagah se chalta
            // rahega; flag nahi lagate taake agli dafa dobara
            // koshish ho (kuch delete nahi hua).
            Logger.logError(LOG_TAG, "Project " + n + " shift nahi ho saka — purani jagah par mehfooz hai");
            return;
        }
        result.moved++;
        result.evaluated++;
        BulkLog.add("Project " + n + " nayi jagah (TermuxMod folder) shift ho gaya");
        if (script != null) appendCorrectedAliases(n, script);
        markDone(context, n);
    }

    /** renameTo pehle; fail ho to copy-tree + size verify + source delete. */
    private static boolean moveFolder(File src, File dest) {
        try {
            if (src.renameTo(dest)) return true;
        } catch (Exception ignored) {}
        try {
            BulkProjectInstaller.copyDir(src, dest);
            if (!sameSizeTree(src, dest)) {
                BulkProjectInstaller.deleteRecursive(dest);
                return false;
            }
            BulkProjectInstaller.deleteRecursive(src);
            return !src.exists() && dest.isDirectory();
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Copy-move fail: " + e.getMessage());
            return false;
        }
    }

    /** Dono trees ki files ki kul size barabar hai? (copy verify) */
    private static boolean sameSizeTree(File a, File b) {
        long[] sa = treeStats(a);
        long[] sb = treeStats(b);
        return sa[0] == sb[0] && sa[1] == sb[1];
    }

    /** @return long[]{fileCount, totalBytes} */
    private static long[] treeStats(File dir) {
        long[] out = {0, 0};
        File[] kids = dir.listFiles();
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

    /**
     * Shift hue project ke corrected aliases ~/.bashrc me APPEND
     * (w/d/b — resolved dir + MOCKUP_BASE wali poori commands).
     * Purani lines kabhi nahi chhedi jatin; shell me baad wali
     * definition jeet-ti hai.
     */
    private static void appendCorrectedAliases(int n, String script) {
        try {
            File bashrc = new File(BulkMockupSetup.homeDir(), ".bashrc");
            StringBuilder add = new StringBuilder();
            add.append("\n# Termux Mod v133: Project ").append(n)
                .append(" nayi jagah (TermuxMod) shift ho gaya\n");
            for (String line : BulkProjects.aliasLines(n, script)) {
                add.append(line).append("\n");
            }
            try (FileOutputStream out = new FileOutputStream(bashrc, true)) {
                out.write(add.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Alias append fail Project " + n + ": " + e.getMessage());
        }
    }
}
