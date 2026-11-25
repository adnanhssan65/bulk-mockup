package com.termux.app;

import android.content.Context;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Termux Mod (v124) — app ke andar pehle se mojood projects.
 *
 * Assets ke "bulk/projects" folder me jo mockup_project_<N>.zip files
 * hain, wo sab bundled projects hain (nayi zip dalna ho to bas is folder
 * me file rakho — code badalne ki zaroorat nahi).
 *
 * Dashboard khulne par peeche chalta hai: jis project ka folder
 * /storage/emulated/0 me NAHI hai, sirf usi ko extract + register karta
 * hai. Mojooda folder kabhi overwrite nahi hota. Storage ki ijazat na ho
 * to is dafa reh jata hai — agli dafa dashboard khulne par dobara koshish
 * ho jati hai.
 */
public final class BulkProjectBundles {

    private static final String LOG_TAG = "BulkProjectBundles";
    public static final String ASSETS_DIR = "bulk/projects";

    private static final AtomicBoolean sRunning = new AtomicBoolean(false);

    private BulkProjectBundles() {}

    /** Background me missing bundled projects lagao; kuch laga ho to onDone chalao. */
    public static void installInBackground(final Context context, final Runnable onDone) {
        if (!sRunning.compareAndSet(false, true)) return;
        new Thread(() -> {
            boolean changed = false;
            try {
                // v147: pehle upgrade-repair (bundled projects ki
                // purani/ajnabi runner files bundled wali se refresh),
                // phir ghayab projects ki fresh install — tartib yahi
                // hai taake lists bante waqt dono kaam ho chuke hon.
                BulkProjectRepair.repairOnUpgradeIfNeeded(context.getApplicationContext());
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Upgrade repair fail: " + e.getMessage());
            }
            try {
                changed = installMissing(context.getApplicationContext());
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Bundled projects check fail: " + e.getMessage());
            } finally {
                sRunning.set(false);
            }
            if (changed && onDone != null) onDone.run();
        }).start();
    }

    /** @return true agar kam az kam ek project naya laga ho. */
    static boolean installMissing(Context context) throws Exception {
        String[] assetNames = context.getAssets().list(ASSETS_DIR);
        if (assetNames == null) return false;
        boolean changed = false;
        for (String assetName : assetNames) {
            if (assetName == null || !assetName.toLowerCase().endsWith(".zip")) continue;
            String baseName = assetName.substring(0, assetName.length() - 4);
            int n = BulkProjects.parseFolderNumber(baseName);
            if (n <= 0) continue;
            if (BulkProjects.projectDir(n).exists()) continue; // pehle se hai — hath nahi lagana
            try {
                if (installOne(context, assetName)) changed = true;
            } catch (Exception e) {
                // Ek project atak jaye (masalan storage ijazat na ho) to baqi rukne nahi chahiye
                Logger.logError(LOG_TAG, "Bundled project " + assetName + " nahi laga: " + e.getMessage());
            }
        }
        return changed;
    }

    private static boolean installOne(Context context, String assetName) throws Exception {
        File tempDir = new File(context.getCacheDir(), "bundle_" + assetName + "_" + System.currentTimeMillis());
        try {
            File zipCopy = new File(tempDir, assetName);
            copyAsset(context, ASSETS_DIR + "/" + assetName, zipCopy);

            List<String> entries = BulkProjectInstaller.listEntries(zipCopy);
            String rootFolder = BulkProjects.findProjectRoot(entries);
            if (rootFolder == null) {
                Logger.logError(LOG_TAG, assetName + ": ZIP me project folder nahi mila");
                return false;
            }
            int n = BulkProjects.parseFolderNumber(rootFolder);
            if (n <= 0 || BulkProjects.projectDir(n).exists()) return false;

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
            if (script == null) {
                Logger.logError(LOG_TAG, assetName + ": main script nahi mili");
                return false;
            }

            File extractDir = new File(tempDir, "extract");
            BulkProjectInstaller.extractProject(zipCopy, prefix, extractDir);
            File extractedProject = new File(extractDir, rootFolder);
            if (!extractedProject.isDirectory()) return false;

            BulkProjectInstaller.installProject(context, extractedProject, n, script);
            Logger.logInfo(LOG_TAG, "Bundled project installed: Project " + n + " (" + script + ")");
            return true;
        } finally {
            BulkProjectInstaller.deleteRecursive(tempDir);
        }
    }

    private static void copyAsset(Context context, String assetPath, File dest) throws Exception {
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }
}
