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
            if (!staging.renameTo(target)) {
                throw new Exception("Project folder asal jagah par nahi ja saka.");
            }
        } catch (Exception e) {
            deleteRecursive(staging);
            throw e;
        }
        appendAliases(n, script);
        appendConfLine(context, n);
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
