package com.termux.app;

import android.content.Context;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Termux Mod (v124) — ek project ko asal jagah par lagane ke shared steps.
 * ZIP wali screen or app ke andar pehle se mojood (bundled) projects —
 * dono yahi istemal karte hain:
 *
 *   1. $HOME/mockup_project_<N> me atomic copy (v136: pehle se ho
 *      to ruk jao — overwrite kabhi nahi; runner marker ke sath)
 *   2. ghayab subfolders: <N>_wallpapers, <N>_Mockups,
 *      <N>_Ready_Wallpapers, Ready_Upload_Batches
 *   3. ~/.bashrc me w<N>/d<N>/b<N> aliases — sirf jo pehle se na hon
 *   4. ~/projects.conf me "Project <N>" line — sirf jab pehle se na ho
 */
public final class BulkProjectInstaller {

    private static final String LOG_TAG = "BulkProjectInstaller";

    private BulkProjectInstaller() {}

    /**
     * v145 — sab se aakhri installProject call ki Project Check
     * report (ZIP-add screen result me dikhane ke liye). Fixer
     * fail ho jaye to bhi install aage barhta hai; report null
     * ya failed ho sakti hai — parhne wala dono handle kare.
     */
    public static volatile BulkProjectFixer.Report lastFixReport;

    /** ZIP file ke tamam entries ke naam. */
    public static List<String> listEntries(File zipFile) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                names.add(en.nextElement().getName());
            }
        }
        return names;
    }

    /** ZIP se sirf project folder (prefix) extract karo — zip-slip guard ke sath. */
    public static void extractProject(File zipFile, String prefix, File extractDir) throws Exception {
        String extractCanonical = extractDir.getCanonicalPath();
        byte[] buf = new byte[1 << 16];
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("__MACOSX/")) continue;
                if (!name.startsWith(prefix)) continue;
                File dest = new File(extractDir, name);
                String destCanonical = dest.getCanonicalPath();
                if (!destCanonical.equals(extractCanonical)
                    && !destCanonical.startsWith(extractCanonical + "/")) {
                    Logger.logError(LOG_TAG, "Zip-slip entry skip: " + name);
                    continue;
                }
                if (entry.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    dest.mkdirs();
                } else {
                    File parent = dest.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    try (OutputStream out = new FileOutputStream(dest)) {
                        int r;
                        while ((r = zin.read(buf)) != -1) out.write(buf, 0, r);
                    }
                }
            }
        }
    }

    /**
     * Extract hue project folder ko asal jagah lagao + register karo.
     * Target pehle se ho to Exception (saaf bilingual message ke
     * sath) — overwrite nahi.
     *
     * v136 (ZIP-add self-setting): install ATOMIC hai — pehle ek
     * temp sibling folder me poora copy + subfolders + runner
     * marker banta hai, phir ek rename se asal jagah par ajata
     * hai. Darmiyan me koi fail ho to temp saaf ho jata hai or
     * kuch register NAHI hota (aadha-set project kabhi nahi).
     * Runner script marker (.main_script) me persist hoti hai —
     * Run/Batch/Delete hamesha wahi script istemal karte hain.
     */
    public static void installProject(Context context, File extractedProjectDir, int n, String script) throws Exception {
        File target = BulkProjects.projectDir(n);
        if (target.exists()) {
            throw new Exception(BulkStrings.format(context, BulkStrings.PROJECT_EXISTS, n));
        }
        File staging = new File(target.getParentFile(),
            target.getName() + ".installing-" + System.currentTimeMillis());
        try {
            copyDir(extractedProjectDir, staging);
            for (String sub : BulkProjects.subfolderNames(n)) {
                File d = new File(staging, sub);
                if (!d.exists()) d.mkdirs();
            }
            // Runner choice hamesha ke liye mehfooz
            try (FileOutputStream out = new FileOutputStream(
                    new File(staging, BulkProjects.MAIN_SCRIPT_MARKER))) {
                out.write((script + "\n").getBytes(StandardCharsets.UTF_8));
            }
            // v145: New Project Check — runner ki mashhoor kharabiyan
            // (path / history / delay / wakelock / scan / gallery)
            // staging me hi fix karo, rename se pehle. Fixer kabhi
            // install ko nahi rokta: fail ho to asal runner mehfooz
            // rehta hai or report me "check nahi ho saka" ata hai.
            BulkProjectFixer.Report fixReport = null;
            try {
                File scriptFile = new File(staging, script);
                if (scriptFile.isFile()) {
                    fixReport = BulkProjectFixer.fixRunnerFile(scriptFile, n);
                }
            } catch (Exception ignored) {
                fixReport = null;
            }
            lastFixReport = fixReport;
            if (!staging.renameTo(target)) {
                throw new Exception("Project folder asal jagah par nahi ja saka.");
            }
        } catch (Exception e) {
            deleteRecursive(staging);
            throw e;
        }
        appendAliases(n, script);
        appendConfLine(context, n);
        // v149: add hote hi preview — project ke template asset
        // se saved preview bana do (pehle run ka intezar nahi).
        // Fail ho to bhi install par koi asar nahi.
        try {
            BulkPreviewUi.ensureSavedPreviewFromTemplate(context, n, target);
        } catch (Exception ignored) {}
        Logger.logInfo(LOG_TAG, "Project " + n + " installed (" + script + ")");
    }

    /** ~/.bashrc me teenon aliases — sirf jo pehle se defined na hon. */
    public static void appendAliases(int n, String script) throws Exception {
        File bashrc = new File(BulkMockupSetup.homeDir(), ".bashrc");
        String content = bashrc.exists() ? readText(bashrc) : "";
        StringBuilder add = new StringBuilder();
        String[] aliasNames = {"w" + n, "d" + n, "b" + n};
        List<String> lines = BulkProjects.aliasLines(n, script);
        for (int i = 0; i < aliasNames.length; i++) {
            if (!BulkProjects.aliasAlreadyDefined(content, aliasNames[i])) {
                add.append(lines.get(i)).append("\n");
            }
        }
        if (add.length() == 0) return;
        try (FileOutputStream out = new FileOutputStream(bashrc, true)) {
            if (!content.isEmpty() && !content.endsWith("\n")) {
                out.write("\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(add.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /** ~/projects.conf me line — sirf jab Project <N> pehle se na ho. */
    public static void appendConfLine(Context context, int n) throws Exception {
        ProjectsLauncher.seedProjectsConfIfMissing(context);
        File conf = ProjectsLauncher.projectsConfFile();
        String content = conf.exists() ? readText(conf) : "";
        if (BulkProjects.confHasProject(content, n)) return;
        try (FileOutputStream out = new FileOutputStream(conf, true)) {
            if (!content.isEmpty() && !content.endsWith("\n")) {
                out.write("\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write((BulkProjects.confLine(n) + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * v138 — Remove Project: project ki installed copy + us ki
     * tamam registration saaf karo:
     *   1. $HOME/mockup_project_<N> engine folder (poora)
     *   2. ~/projects.conf me Project <N> ki line(s)
     *   3. ~/.bashrc me w<N>/d<N>/b<N> alias lines
     *   4. drop-folder provision record — taake dobara add hone
     *      par TermuxMod folder phir se provision ho sake
     * TermuxMod/Project N or us ki images ko hath NAHI lagta —
     * wo owner ke outputs hain, hamesha mehfooz.
     *
     * Bundled project: Projects screen khud kabhi extract nahi
     * karti, is liye removal ke baad project tamam lists se
     * foran ghayab ho jata hai; agli dafa Dashboard khulne par
     * BulkProjectBundles ki only-if-missing extraction fresh
     * bundled copy wapas laga degi (installProject aliases +
     * conf line bhi dobara register kar deta hai). ZIP wala
     * project dobara ZIP add karne tak ghayab rehta hai, or
     * re-add bilkul saaf register hota hai.
     */
    public static void removeProject(Context context, int n) throws Exception {
        if (n <= 0) return;
        deleteRecursive(BulkProjects.projectDir(n));
        unregisterProject(context, n);
        Logger.logInfo(LOG_TAG, "Project " + n + " removed");
    }

    /**
     * v154 — Remove Project ka naya rasta: engine folder delete
     * NAHI hota, Recycle Bin (BulkRecycleBin) me chala jata hai;
     * registration ki safai (conf + aliases + provision record)
     * bilkul removeProject wali hi hai, taake project tamam
     * lists se foran ghayab ho jaye. Bin se Restore karne par
     * folder + registration dono wapas aa jate hain.
     */
    public static void removeProjectToBin(Context context, int n, String displayName)
            throws Exception {
        if (n <= 0) return;
        File srcDir = BulkProjects.projectDir(n);
        File binDir = BulkRecycleBin.entryDir(n);
        if (srcDir.isDirectory()) {
            deleteRecursive(binDir);
            File parent = binDir.getParentFile();
            if (parent != null) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            if (!srcDir.renameTo(binDir)) {
                copyDir(srcDir, binDir);
                deleteRecursive(srcDir);
            }
        }
        BulkRecycleBin.writeMeta(n, displayName);
        unregisterProject(context, n);
        Logger.logInfo(LOG_TAG, "Project " + n + " moved to recycle bin");
    }

    /** removeProject or removeProjectToBin ki mushtaraka registration safai. */
    private static void unregisterProject(Context context, int n) throws Exception {
        File conf = ProjectsLauncher.projectsConfFile();
        if (conf.exists()) {
            String updated = BulkProjects.removeConfLines(readText(conf), n);
            if (updated != null) writeText(conf, updated);
        }
        File bashrc = new File(BulkMockupSetup.homeDir(), ".bashrc");
        if (bashrc.exists()) {
            String updated = BulkProjects.removeAliasLines(readText(bashrc), n);
            if (updated != null) writeText(bashrc, updated);
        }
        BulkDropFolders.clearProvisionRecord(context, n);
    }

    private static void writeText(File f, String content) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f, false)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ------------------------------------------------------------ file helpers

    public static void copyDir(File src, File dest) throws Exception {
        if (!dest.exists()) dest.mkdirs();
        File[] kids = src.listFiles();
        if (kids == null) return;
        byte[] buf = new byte[1 << 16];
        for (File k : kids) {
            File d = new File(dest, k.getName());
            if (k.isDirectory()) {
                copyDir(k, d);
            } else {
                try (InputStream in = new FileInputStream(k);
                     OutputStream out = new FileOutputStream(d)) {
                    int r;
                    while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                }
            }
        }
    }

    public static String readText(File f) throws Exception {
        try (InputStream in = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toString("UTF-8");
        }
    }

    public static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null && f.isDirectory()) {
            for (File k : kids) deleteRecursive(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
