package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Termux Mod (v134) — project folders ki WAPSI (move-back).
 *
 * v133 me projects /storage/emulated/0/TermuxMod/mockup_project_<N>
 * shift hue thay; owner ka faisla: TermuxMod folder ke andar SIRF
 * "Project N" drop folders rahen — asal project folders wapas
 * storage ROOT par: /storage/emulated/0/mockup_project_<N>.
 *
 * Usool (v133 wale hi safety usool, ulta rukh):
 *   - Har project SIRF EK dafa check hota hai (persisted flag,
 *     v133 wale flags se alag — "movedback_<N>").
 *   - Wapsi SIRF tab jab TermuxMod wala folder mojood ho, root par
 *     usi naam ka folder NA ho, or runner ka text relocation-safe
 *     ho (BulkMigration.isRelocationSafe — "MOCKUP_BASE" / "__file__").
 *   - Root par folder pehle se ho to KABHI overwrite/merge nahi —
 *     TermuxMod wali copy wahin rehti hai or summary me gin-ti hai.
 *   - Move = renameTo; fail ho to copy-tree + size verify, verify
 *     ke baad hi source delete (asal move ka hissa). Kuch bhi
 *     kabhi delete nahi hota is ke ilawa.
 *   - Wapas hue project ke corrected aliases ~/.bashrc me APPEND
 *     (root wali commands; baad wali lines jeet-ti hain).
 */
public final class BulkMigrationBack {

    private static final String LOG_TAG = "BulkMigrationBack";
    private static final String PREFS = "bulk_mockup";
    private static final String KEY_PREFIX = "movedback_";
    public static final String KEY_SUMMARY_SHOWN = "moveback_summary_shown";

    private BulkMigrationBack() {}

    // ------------------------------------------------------------ pure

    /** Ek project ke liye faisla. */
    public enum Decision {
        /** TermuxMod -> root move karo. */
        MOVE,
        /** Root par pehle se folder hai — copy wahin rahe, kabhi merge nahi. */
        STAY_CONFLICT,
        /** Runner purana/hardcoded hai — copy wahin rahe, wese hi chalegi. */
        STAY_UNSAFE,
        /** TermuxMod me kuch hai hi nahi — karne ko kuch nahi. */
        NOTHING
    }

    /**
     * Faisla (pure): TermuxMod wali copy ho, root par na ho, or
     * runner safe ho — tabhi MOVE. Root par pehle se ho to
     * STAY_CONFLICT (safety check se pehle — conflict hamesha jeet-ta
     * hai, kyun ke merge/overwrite har haal me mana hai).
     */
    public static Decision decide(boolean inModExists, boolean rootExists,
                                  boolean runnerSafe) {
        if (!inModExists) return Decision.NOTHING;
        if (rootExists) return Decision.STAY_CONFLICT;
        if (!runnerSafe) return Decision.STAY_UNSAFE;
        return Decision.MOVE;
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
     * Ek move-back pass: har known project jo abhi tak check nahi
     * hua. Background thread se chalao (file IO hai).
     */
    public static Result moveBackAll(Context context) {
        Result result = new Result();
        Set<Integer> known = BulkDropFolders.knownNumbers();
        if (known == null || known.isEmpty()) return result;
        for (int n : known) {
            if (n <= 0 || isDone(context, n)) continue;
            try {
                evaluate(context, n, result);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Move-back fail Project " + n + ": " + e.getMessage());
            }
        }
        return result;
    }

    private static void evaluate(Context context, int n, Result result) throws Exception {
        File inModDir = BulkProjects.newProjectDir(n);
        File rootDir = BulkProjects.oldProjectDir(n);
        if (!inModDir.isDirectory()) {
            markDone(context, n);
            return;
        }
        String script = BulkProjects.detectMainScript(
            BulkProjects.listFileNames(inModDir), n);
        String text = null;
        if (script != null) {
            File runner = new File(inModDir, script);
            if (runner.isFile()) text = BulkProjectInstaller.readText(runner);
        }
        Decision decision = decide(true, rootDir.isDirectory(),
            BulkMigration.isRelocationSafe(text));
        switch (decision) {
            case STAY_CONFLICT:
                result.stayed++;
                result.evaluated++;
                BulkLog.add(BulkStrings.format(context, BulkStrings.LOG_MOVEBACK_STAY_ROOT, n));
                markDone(context, n);
                return;
            case STAY_UNSAFE:
                result.stayed++;
                result.evaluated++;
                BulkLog.add(BulkStrings.format(context, BulkStrings.LOG_MOVEBACK_STAY_RUNNER, n));
                markDone(context, n);
                return;
            case MOVE:
                break;
            default:
                markDone(context, n);
                return;
        }
        boolean movedOk = moveFolder(inModDir, rootDir);
        if (!movedOk) {
            // Wapsi na ho saki — project TermuxMod wali jagah se
            // chalta rahega; flag nahi lagate taake agli dafa
            // dobara koshish ho (kuch delete nahi hua).
            Logger.logError(LOG_TAG, "Project " + n + " wapas nahi ho saka — TermuxMod me mehfooz hai");
            return;
        }
        result.moved++;
        result.evaluated++;
        BulkLog.add(BulkStrings.format(context, BulkStrings.LOG_MOVEBACK_MOVED, n));
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
     * Wapas hue project ke corrected aliases ~/.bashrc me APPEND
     * (root wali poori commands — aliasLines resolution se root
     * path hi banati hai kyun ke folder ab root par hai).
     */
    private static void appendCorrectedAliases(int n, String script) {
        try {
            File bashrc = new File(BulkMockupSetup.homeDir(), ".bashrc");
            StringBuilder add = new StringBuilder();
            add.append("\n# Termux Mod v134: Project ").append(n)
                .append(" wapas main storage (root) par shift ho gaya\n");
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
