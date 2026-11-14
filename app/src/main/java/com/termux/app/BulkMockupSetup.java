package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.os.PowerManager;
import android.provider.Settings;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Bulk Mockup first-run setup.
 *
 * Normal Termux bootstrap ke BAAD chalta hai (sirf pehli dafa):
 *  1. APK ke andar mojood termux-usr.tar.gz (owner ke working phone ke tamam
 *     packages) ko $PREFIX ke upar extract karta hai — koi pkg install nahi.
 *  2. ~/.bashrc, ~/.termux/colors.properties aur ~/projects.conf sirf tab
 *     lagata hai jab wo pehle se mojood na hon (kabhi overwrite nahi).
 *  3. Marker file likhta hai taake ye setup dobara na chale. App updates pe
 *     ye kabhi nahi chalta — user ki files bilkul mehfooz rehti hain.
 *  4. Storage permission aur battery optimization exemption mangta hai.
 */
public final class BulkMockupSetup {

    private static final String LOG_TAG = "BulkMockupSetup";
    private static final String USR_ASSET = "termux-usr.tar.gz";
    private static final String MARKER_NAME = ".bulk_mockup_setup_done";
    private static final String PREFS = "bulk_mockup";
    private static final String PREF_BATTERY_ASKED = "battery_asked";

    private BulkMockupSetup() {}

    public static File homeDir() {
        return new File(TermuxConstants.TERMUX_FILES_DIR_PATH, "home");
    }

    private static File markerFile() {
        return new File(homeDir(), MARKER_NAME);
    }

    public static boolean isSetupDone() {
        return markerFile().exists();
    }

    /** Bootstrap ke baad call karo. Setup pehle se ho chuka ho to foran whenDone chalta hai. */
    public static void runIfNeeded(final Activity activity, final Runnable whenDone) {
        if (isSetupDone()) {
            ensureBinWrappers(activity);
            whenDone.run();
            return;
        }

        final ProgressDialog progress = ProgressDialog.show(activity, null,
            "Setting up Bulk Mockup — please wait...\nAll packages are already inside, no internet needed.\nThis can take 1–3 minutes.",
            true, false);

        new Thread(() -> {
            try {
                extractUsrArchive(activity);
                seedHomeFiles(activity);
                installBinWrappers(activity);
                // Marker sab se aakhir me — beech me band ho jaye to agli dafa setup dobara try hoga
                FileOutputStream marker = new FileOutputStream(markerFile());
                marker.write(("done " + System.currentTimeMillis() + "\n").getBytes(StandardCharsets.UTF_8));
                marker.close();
                Logger.logInfo(LOG_TAG, "Bulk Mockup first-run setup complete.");

                activity.runOnUiThread(() -> {
                    dismiss(progress);
                    whenDone.run();
                    // Pehli dafa: wahi Permissions screen jo sidebar me hai (Override C)
                    try {
                        Intent intent = new Intent(activity, BulkPermissionsActivity.class);
                        intent.putExtra("first_run", true);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Logger.logError(LOG_TAG, "Permissions screen open failed: " + e.getMessage());
                    }
                });
            } catch (final Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Bulk Mockup setup failed", e);
                activity.runOnUiThread(() -> {
                    dismiss(progress);
                    new AlertDialog.Builder(activity)
                        .setTitle("Setup problem")
                        .setMessage("Bulk Mockup ki pehli setup mukammal nahi ho saki.\n\n" +
                            "Error: " + e.getMessage() + "\n\n" +
                            "Phone ko restart karke app dobara kholo, ya neeche Try Again dabao. Tumhari koi file delete nahi hui.")
                        .setPositiveButton("Try Again", (d, w) -> runIfNeeded(activity, whenDone))
                        .setNegativeButton("Close App", (d, w) -> activity.finish())
                        .setCancelable(false)
                        .show();
                });
            }
        }).start();
    }

    private static void dismiss(ProgressDialog p) {
        try { p.dismiss(); } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ usr extract
    private static void extractUsrArchive(Context context) throws Exception {
        File filesDir = new File(TermuxConstants.TERMUX_FILES_DIR_PATH);

        // Free space pehle check karo (G2.4): archive khulne par takreeban 4 guna jagah chahiye
        long assetSize;
        try (InputStream probe = context.getAssets().open(USR_ASSET)) {
            assetSize = probe.available();
        } catch (Exception e) {
            assetSize = 0;
        }
        // AssetManager.available() bari files par unreliable hai; sirf bara sa
        // safety margin dekho: kam az kam 1.5 GB free ho warna pehle bata do.
        try {
            StatFs stat = new StatFs(filesDir.getAbsolutePath());
            long free = stat.getAvailableBytes();
            if (free < 1_500_000_000L) {
                throw new Exception("Phone storage me jagah kam hai (" + (free / 1_000_000)
                    + " MB free). Bulk Mockup ko kam az kam 1.5 GB khaali jagah chahiye. "
                    + "Kuch files delete karke Try Again dabao. (Not enough free storage.)");
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("jagah kam hai")) throw e;
            // StatFs fail ho jaye to aage barho — extraction khud bata degi
        }

        // Staging dir me extract karo, phir rename se swap (atomic) — beech me
        // app band ho jaye to purana usr mehfooz rehta hai, agli dafa setup
        // dobara chalega (marker sab se aakhir me likha jata hai).
        File staging = new File(filesDir, ".bulk_staging");
        deleteRecursive(staging);
        if (!staging.mkdirs()) throw new Exception("Staging folder nahi ban saka");
        String stagingCanonical = staging.getCanonicalPath();

        InputStream raw = context.getAssets().open(USR_ASSET);
        TarArchiveInputStream tar = new TarArchiveInputStream(
            new GzipCompressorInputStream(new BufferedInputStream(raw, 1 << 16)), "UTF-8");

        byte[] buffer = new byte[1 << 16];
        TarArchiveEntry entry;
        long count = 0;
        while ((entry = tar.getNextEntry()) != null) {
            String name = entry.getName();
            if (name.startsWith("./")) name = name.substring(2);
            if (name.isEmpty() || name.equals(".")) continue;
            if (!name.startsWith("usr/") && !name.equals("usr")) continue; // sirf usr tree

            File dest = new File(staging, name);
            String destCanonical = dest.getCanonicalPath();
            if (!destCanonical.equals(stagingCanonical) &&
                !destCanonical.startsWith(stagingCanonical + "/")) {
                Logger.logError(LOG_TAG, "Skipping archive entry outside staging dir: " + name);
                continue;
            }

            if (entry.isDirectory()) {
                if (!dest.exists()) dest.mkdirs();
            } else if (entry.isSymbolicLink()) {
                ensureParent(dest);
                deleteIfExists(dest);
                android.system.Os.symlink(entry.getLinkName(), dest.getAbsolutePath());
            } else if (entry.isLink()) {
                ensureParent(dest);
                deleteIfExists(dest);
                String linkName = entry.getLinkName();
                if (linkName.startsWith("./")) linkName = linkName.substring(2);
                File target = new File(staging, linkName);
                android.system.Os.link(target.getAbsolutePath(), dest.getAbsolutePath());
            } else if (entry.isFile()) {
                ensureParent(dest);
                try (FileOutputStream out = new FileOutputStream(dest)) {
                    int read;
                    while ((read = tar.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                try {
                    android.system.Os.chmod(dest.getAbsolutePath(), entry.getMode() & 0777);
                } catch (Exception ignored) {}
            }
            count++;
        }
        tar.close();
        Logger.logInfo(LOG_TAG, "Extracted " + count + " entries from " + USR_ASSET + " into staging");

        File stagedUsr = new File(staging, "usr");
        if (!stagedUsr.isDirectory()) {
            deleteRecursive(staging);
            throw new Exception("Archive me usr folder nahi mila — file kharab hai.");
        }

        // Atomic swap: purana usr side par, naya usr uski jagah
        File usrDir = new File(filesDir, "usr");
        File oldUsr = new File(filesDir, ".bulk_old_usr");
        deleteRecursive(oldUsr);
        boolean movedOld = false;
        if (usrDir.exists()) {
            movedOld = usrDir.renameTo(oldUsr);
            if (!movedOld) {
                deleteRecursive(staging);
                throw new Exception("Purana usr side par nahi ho saka — setup ruk gaya, koi file delete nahi hui.");
            }
        }
        if (!stagedUsr.renameTo(usrDir)) {
            if (movedOld) { // rollback
                if (!oldUsr.renameTo(usrDir)) {
                    Logger.logError(LOG_TAG, "Rollback bhi fail — purana usr .bulk_old_usr me hai");
                }
            }
            deleteRecursive(staging);
            throw new Exception("Naya usr apni jagah nahi ja saka — setup ruk gaya.");
        }
        deleteRecursive(oldUsr);
        deleteRecursive(staging);
        Logger.logInfo(LOG_TAG, "usr swap complete");
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        try {
            if (f.isDirectory() && !isSymlink(f)) {
                File[] kids = f.listFiles();
                if (kids != null) for (File k : kids) deleteRecursive(k);
            }
        } catch (Exception ignored) {}
        try { f.delete(); } catch (Exception ignored) {}
    }

    private static void ensureParent(File f) {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }

    private static void deleteIfExists(File f) {
        try {
            if (f.exists() || isSymlink(f)) f.delete();
        } catch (Exception ignored) {}
    }

    private static boolean isSymlink(File f) {
        try {
            String canonical = f.getCanonicalPath();
            String absolute = f.getAbsolutePath();
            return !canonical.equals(absolute);
        } catch (Exception e) {
            return false;
        }
    }


    // ------------------------------------------------------------------ bin wrappers (built-in Termux:API)
    /**
     * $PREFIX/bin me termux-media-scan / termux-wake-lock / termux-wake-unlock
     * wrapper scripts lagata hai. Ye app ke andar mojood BulkApiServer se
     * filesystem socket par baat karte hain — koi dusri app nahi chahiye.
     * Paths TermuxConstants se ate hain taake hamesha app ke asal
     * prefix ke mutabiq sahi rahen.
     */
    private static String readAssetText(Context context, String path) throws Exception {
        try (InputStream in = context.getAssets().open(path)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toString("UTF-8");
        }
    }

    private static String wrapperScript(Context context, String mode) throws Exception {
        String template = readAssetText(context, "bulk/bulk_api_client.py");
        return "#!" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/python3\n"
            + template.replace("__SOCK__", BulkApiServer.socketPath())
                      .replace("__MODE__", mode);
    }

    private static void installBinWrappers(Context context) throws Exception {
        writeWrapper("termux-media-scan", wrapperScript(context, "scan"));
        writeWrapper("termux-wake-lock", wrapperScript(context, "wakelock"));
        writeWrapper("termux-wake-unlock", wrapperScript(context, "wakeunlock"));
        Logger.logInfo(LOG_TAG, "Bin wrappers installed (termux-media-scan / wake-lock / wake-unlock)");
    }

    private static void ensureBinWrappers(Context context) {
        try {
            File bin = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "bin");
            if (!new File(bin, "termux-media-scan").exists()
                || !new File(bin, "termux-wake-lock").exists()
                || !new File(bin, "termux-wake-unlock").exists()) {
                installBinWrappers(context);
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "ensureBinWrappers failed: " + e.getMessage());
        }
    }

    private static void writeWrapper(String name, String content) throws Exception {
        File bin = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "bin");
        if (!bin.exists()) bin.mkdirs();
        File f = new File(bin, name);
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        try { android.system.Os.chmod(f.getAbsolutePath(), 0700); } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ home seeding
    private static void seedHomeFiles(Context context) throws Exception {
        File home = homeDir();
        if (!home.exists()) home.mkdirs();

        copyAssetIfMissing(context, "bulk/bashrc.txt", new File(home, ".bashrc"));

        File termuxDir = new File(home, ".termux");
        if (!termuxDir.exists()) termuxDir.mkdirs();
        copyAssetIfMissing(context, "bulk/colors.properties", new File(termuxDir, "colors.properties"));

        ProjectsLauncher.seedProjectsConfIfMissing(context);
    }

    static void copyAssetIfMissing(Context context, String assetPath, File dest) throws Exception {
        if (dest.exists()) return;
        ensureParent(dest);
        try (InputStream in = context.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        }
        Logger.logInfo(LOG_TAG, "Seeded " + dest.getAbsolutePath());
    }
}
