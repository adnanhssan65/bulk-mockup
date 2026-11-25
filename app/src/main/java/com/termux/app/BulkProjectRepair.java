package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;

import com.termux.shared.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Termux Mod (v147) — BUNDLED projects ki runner files ka
 * upgrade-repair.
 *
 * Masla (owner ki report, 2026-11-25): ek phone par Project 1 ki
 * $HOME copy kisi purani, ajnabi lineage ki thi (hardcoded
 * /storage/emulated/0/mockup_project_1 wala rasta — v136 ke baad
 * wo folder hota hi nahi, run shuru hote hi FileNotFoundError).
 * App mojooda project folders ko kabhi overwrite nahi karta, is
 * liye bundled zips ke andar ke fixes aisi copies tak kabhi
 * pohnchte hi nahi thay.
 *
 * Hal: jab app ko versionCode ka IZAFA nazar aye (pichli run se
 * mehfooz last-seen se zyada), to har BUNDLED project ke liye —
 * jis ka $HOME folder mojood ho — us ki main runner file bundled
 * zip wali se compare hoti hai. Mukhtalif ho to SIRF wahi ek file
 * bundled bytes se badal jati hai; purani file "<script>.bak_app"
 * ke naam se mehfooz rehti hai. Sath project ke standard
 * subfolders (<N>_wallpapers / <N>_Mockups / <N>_Ready_Wallpapers
 * / Ready_Upload_Batches) me jo ghayab hon, wo khaali bana diye
 * jate hain. Is ke ilawa KISI file ya subfolder ke content ko
 * hath nahi lagta. ZIP se add hue (ghair-bundled) projects is
 * repair me kabhi nahi ate.
 *
 * Usool: khamosh, idempotent (bytes barabar hon to kuch nahi
 * hota), har project ki khata alag nigal jati hai — repair app
 * startup ko kabhi nahi rok sakta. Har repaired project par
 * BulkLog me ek line: "Project N file refresh ho gaya".
 *
 * Faisla + file wala hissa pure Java hai (unit tests ke liye);
 * Android glue (assets / PackageManager / prefs) neeche hai.
 */
public final class BulkProjectRepair {

    private static final String LOG_TAG = "BulkProjectRepair";
    private static final String PREFS = "bulk_project_repair";
    private static final String KEY_LAST_SEEN = "last_seen_version_code";
    /** Badli hui purani runner is suffix ke sath mehfooz rehti hai. */
    public static final String BACKUP_SUFFIX = ".bak_app";

    private BulkProjectRepair() {}

    // ------------------------------------------------------------ pure: faisla

    /** Repair sirf tab jab current version pichli dafa se BARHA ho. */
    public static boolean shouldRunRepair(int lastSeenVersion, int currentVersion) {
        return currentVersion > lastSeenVersion;
    }

    /** Ek project ke repair ka faisla (pure). */
    public enum Decision { SKIP_NOT_BUNDLED, SKIP_NO_DIR, NOOP_IDENTICAL, REPLACE }

    public static Decision decide(boolean bundled, boolean dirExists, boolean runnerDiffers) {
        if (!bundled) return Decision.SKIP_NOT_BUNDLED;
        if (!dirExists) return Decision.SKIP_NO_DIR;
        return runnerDiffers ? Decision.REPLACE : Decision.NOOP_IDENTICAL;
    }

    /**
     * Runner file ka naam (pure): persisted .main_script marker
     * pehle — basharti ke wo saada file naam ho (koi '/' ya '\'
     * nahi; kharab marker par bundled naam hi sahi hai). Warna
     * bundled zip ki apni main script ka naam.
     */
    public static String resolveRunnerName(String persistedMarker, String bundledScript) {
        if (persistedMarker != null) {
            String m = persistedMarker.trim();
            if (!m.isEmpty() && m.indexOf('/') < 0 && m.indexOf('\\') < 0) return m;
        }
        return bundledScript;
    }

    // ------------------------------------------------------------ pure: file ka kaam

    public static byte[] readAllBytes(File f) throws Exception {
        try (InputStream in = new FileInputStream(f);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1 << 15];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            return out.toByteArray();
        }
    }

    private static void writeAllBytes(File f, byte[] data) throws Exception {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
    }

    /**
     * Project ke standard subfolders me jo ghayab hon, khaali bana
     * do. @return kitne subfolders asal me bane.
     */
    public static int ensureSubfolders(File projectDir, int n) {
        if (projectDir == null || n <= 0) return 0;
        int created = 0;
        for (String sub : BulkProjects.subfolderNames(n)) {
            File dir = new File(projectDir, sub);
            if (!dir.isDirectory() && (dir.mkdirs() || dir.isDirectory())) created++;
        }
        return created;
    }

    /**
     * Ek project folder ka repair (pure IO): pehle ghayab
     * subfolders, phir runner compare — mukhtalif (ya ghayab) ho
     * to purani file "<runner>.bak_app" me mehfooz karke bundled
     * bytes likh do. Barabar ho to bilkul kuch nahi badalta
     * (idempotent — dusri dafa bhi yahi natija).
     *
     * @return true agar runner asal me badli gayi ho.
     */
    public static boolean repairProjectDir(File projectDir, int n,
                                           String runnerName, byte[] bundledBytes)
        throws Exception {
        if (projectDir == null || runnerName == null || bundledBytes == null) return false;
        ensureSubfolders(projectDir, n);
        File target = new File(projectDir, runnerName);
        byte[] existing = target.isFile() ? readAllBytes(target) : null;
        if (existing != null && Arrays.equals(existing, bundledBytes)) return false;
        if (existing != null) {
            writeAllBytes(new File(projectDir, runnerName + BACKUP_SUFFIX), existing);
        }
        writeAllBytes(target, bundledBytes);
        return true;
    }

    // ------------------------------------------------------------ Android glue

    /** Is install ka versionCode (PackageManager se); na mile to -1. */
    static int currentVersionCode(Context ctx) {
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Upgrade-repair ka pass — BulkProjectBundles ke background
     * thread se, installMissing se PEHLE chalta hai. Version barha
     * hi na ho to foran wapas (har aam Dashboard open par koi kaam
     * nahi). Pass ke aakhir me last-seen = current mehfooz ho jata
     * hai (downgrade par bhi, taake dobara upgrade par repair phir
     * chal sake). Har project ki khata alag nigal jati hai.
     */
    public static void repairOnUpgradeIfNeeded(Context ctx) {
        if (ctx == null) return;
        int current = currentVersionCode(ctx);
        if (current <= 0) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int lastSeen = prefs.getInt(KEY_LAST_SEEN, 0);
        if (current == lastSeen) return;
        if (shouldRunRepair(lastSeen, current)) {
            Set<Integer> bundled = BulkDropFolders.bundledNumbers(ctx);
            for (int n : bundled) {
                try {
                    repairOne(ctx, n);
                } catch (Exception e) {
                    // Ek project atak jaye to baqi rukne nahi chahiye,
                    // or app startup kabhi nahi rukta.
                    Logger.logError(LOG_TAG, "Repair fail Project " + n + ": " + e.getMessage());
                }
            }
        }
        prefs.edit().putInt(KEY_LAST_SEEN, current).apply();
    }

    private static void repairOne(Context ctx, int n) throws Exception {
        File dir = BulkProjects.projectDir(n);
        if (!dir.isDirectory()) return; // ghayab folder installMissing ka kaam hai
        BundledRunner br = readBundledRunner(ctx, n);
        if (br == null || br.script == null || br.bytes == null) return;
        String name = resolveRunnerName(BulkProjects.readPersistedScript(dir), br.script);
        if (name == null) return;
        if (repairProjectDir(dir, n, name, br.bytes)) {
            BulkLog.add("Project " + n + " file refresh ho gaya");
            Logger.logInfo(LOG_TAG, "Project " + n + " runner refreshed (" + name + ")");
        }
    }

    /** Bundled zip ke andar ki main runner: naam + bytes. */
    static final class BundledRunner {
        final String script;
        final byte[] bytes;
        BundledRunner(String script, byte[] bytes) {
            this.script = script;
            this.bytes = bytes;
        }
    }

    /**
     * Assets wale bundled zip se main runner parho: zip cache me
     * copy karke ZipFile se sirf runner wali entry nikali jati hai
     * (poora extract nahi hota). Na mile to null.
     */
    static BundledRunner readBundledRunner(Context ctx, int n) throws Exception {
        String assetName = "mockup_project_" + n + ".zip";
        File tempDir = new File(ctx.getCacheDir(), "repair_" + assetName + "_" + System.currentTimeMillis());
        try {
            File zipCopy = new File(tempDir, assetName);
            File parent = zipCopy.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (InputStream in = ctx.getAssets().open(BulkProjectBundles.ASSETS_DIR + "/" + assetName);
                 OutputStream out = new FileOutputStream(zipCopy)) {
                byte[] buf = new byte[1 << 16];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }
            List<String> entries = BulkProjectInstaller.listEntries(zipCopy);
            String rootFolder = BulkProjects.findProjectRoot(entries);
            if (rootFolder == null) return null;
            List<String> rootFiles = new ArrayList<>();
            String prefix = rootFolder + "/";
            for (String e : entries) {
                String norm = e.replace('\\', '/');
                if (norm.startsWith(prefix)) {
                    String rest = norm.substring(prefix.length());
                    if (!rest.isEmpty() && !rest.contains("/")) rootFiles.add(rest);
                }
            }
            String script = BulkProjects.detectMainScript(rootFiles, n);
            if (script == null) return null;
            try (ZipFile zip = new ZipFile(zipCopy)) {
                ZipEntry entry = zip.getEntry(prefix + script);
                if (entry == null) return null;
                try (InputStream in = zip.getInputStream(entry);
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[1 << 15];
                    int r;
                    while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                    return new BundledRunner(script, out.toByteArray());
                }
            }
        } finally {
            BulkProjectInstaller.deleteRecursive(tempDir);
        }
    }
}
