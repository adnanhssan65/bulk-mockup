package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
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
import java.io.FilterInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Termux Mod first-run setup.
 *
 * Normal Termux bootstrap ke BAAD chalta hai (sirf pehli dafa):
 *  1. APK ke andar mojood termux-usr.dat (owner ke working phone ke tamam
 *     packages) ko $PREFIX ke upar extract karta hai — koi pkg install nahi.
 *  2. ~/.bashrc, ~/.termux/colors.properties aur ~/projects.conf sirf tab
 *     lagata hai jab wo pehle se mojood na hon (kabhi overwrite nahi).
 *  3. Marker file likhta hai taake ye setup dobara na chale. App updates pe
 *     ye kabhi nahi chalta — user ki files bilkul mehfooz rehti hain.
 *  4. Storage permission aur battery optimization exemption mangta hai.
 */
public final class BulkMockupSetup {

    private static final String LOG_TAG = "BulkMockupSetup";
    // NOTE: .gz naam bilkul nahi rakhna — AAPT packaging ke waqt .gz asset ko
    // khud khol kar naam se .gz hata deta hai (v119 isi liye phone par fail hua:
    // APK ke andar file assets/termux-usr.tar ban gayi thi). Is liye .dat naam.
    private static final String USR_ASSET = "termux-usr.dat";
    private static final String MARKER_NAME = ".bulk_mockup_setup_done";
    private static final String PREFS = "bulk_mockup";
    private static final String PREF_BATTERY_ASKED = "battery_asked";
    private static final String PREF_FIRST_RUN_PERMS_DONE = "first_run_permissions_done";

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

    /**
     * Pehli-dafa wali permission popup chain (Storage → Battery →
     * Notifications) user ke samne aa chuki hai ya nahi (v126). Jab tak
     * ye false hai, app start par Dashboard se pehle Permissions screen
     * ati hai — popups hamesha sab se shuru me.
     */
    public static boolean isFirstRunPermissionsDone(Context context) {
        try {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(PREF_FIRST_RUN_PERMS_DONE, false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void markFirstRunPermissionsDone(Context context) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_FIRST_RUN_PERMS_DONE, true).apply();
        } catch (Exception ignored) {}
    }

    /** v127: "Replay First-Time Popups" — flag saaf karo taake chain dobara chal sake. */
    public static void resetFirstRunPermissionsDone(Context context) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_FIRST_RUN_PERMS_DONE, false).apply();
        } catch (Exception ignored) {}
    }

    /** Bootstrap ke baad call karo. Setup pehle se ho chuka ho to foran whenDone chalta hai. */
    public static void runIfNeeded(final Activity activity, final Runnable whenDone) {
        if (isSetupDone()) {
            ensureBinWrappers(activity);
            whenDone.run();
            return;
        }

        // VIP loading: asal percentage wali progress bar (0–100). Extraction ka
        // kaam peeche background thread me chalta hai; screen par sirf asal
        // progress nazar ati hai — phone hang nahi hota.
        // v128: design-style setup popup — stock ProgressDialog ki jagah
        // BulkTheme wala dark glass panel, neon title + progress bar.
        // Text, asal 0–100 progress aur flow bilkul wohi hai.
        android.widget.LinearLayout panel = new android.widget.LinearLayout(activity);
        panel.setOrientation(android.widget.LinearLayout.VERTICAL);
        panel.setBackground(BulkTheme.modalBg(activity));
        panel.setPadding(BulkTheme.dp(activity, 16), BulkTheme.dp(activity, 16),
            BulkTheme.dp(activity, 16), BulkTheme.dp(activity, 16));

        android.widget.TextView title = new android.widget.TextView(activity);
        title.setText("Termux Mod — Pehli Setup");
        title.setTextSize(15);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(BulkTheme.accent(activity));
        panel.addView(title);

        final android.widget.TextView message = new android.widget.TextView(activity);
        message.setText(setupMessage(0));
        message.setTextSize(12.5f);
        message.setTextColor(BulkTheme.textSub(activity));
        message.setLineSpacing(0, 1.35f);
        android.widget.LinearLayout.LayoutParams mlp = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = BulkTheme.dp(activity, 10);
        message.setLayoutParams(mlp);
        panel.addView(message);

        final android.widget.ProgressBar bar = new android.widget.ProgressBar(
            activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        bar.setIndeterminate(false);
        try {
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                BulkTheme.accent(activity)));
        } catch (Exception ignored) {}
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = BulkTheme.dp(activity, 14);
        bar.setLayoutParams(blp);
        panel.addView(bar);

        final Dialog progress = BulkTheme.showPanelDialog(activity, panel);
        progress.setCancelable(false);

        new Thread(() -> {
            try {
                extractUsrArchive(activity, percent -> activity.runOnUiThread(() -> {
                    try {
                        bar.setProgress(percent);
                        message.setText(setupMessage(percent));
                    } catch (Exception ignored) {}
                }));
                seedHomeFiles(activity);
                installBinWrappers(activity);
                // Marker sab se aakhir me — beech me band ho jaye to agli dafa setup dobara try hoga
                FileOutputStream marker = new FileOutputStream(markerFile());
                marker.write(("done " + System.currentTimeMillis() + "\n").getBytes(StandardCharsets.UTF_8));
                marker.close();
                Logger.logInfo(LOG_TAG, "Termux Mod first-run setup complete.");

                activity.runOnUiThread(() -> {
                    dismiss(progress);
                    // Pehli dafa: Permissions screen SAB SE PEHLE (v126 fix).
                    // v124/v125 me whenDone pehle chalta tha or wo Dashboard
                    // khol deta tha — popups dashboard ke peeche reh jate thay,
                    // user seedha dashboard par land karta tha. Ab pehle
                    // Permissions, phir whenDone (session waghera); Dashboard
                    // TermuxActivity me bhi is flag se gated hai or asal
                    // dashboard Permissions chain ke baad khud khulta hai.
                    try {
                        Intent intent = new Intent(activity, BulkPermissionsActivity.class);
                        intent.putExtra("first_run", true);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Logger.logError(LOG_TAG, "Permissions screen open failed: " + e.getMessage());
                    }
                    whenDone.run();
                });
            } catch (final Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Termux Mod setup failed", e);
                activity.runOnUiThread(() -> {
                    dismiss(progress);
                    showSetupError(activity, e, whenDone);
                });
            }
        }).start();
    }

    /** Setup popup ka message — text pehle wala hi hai, sirf % badalta hai. */
    private static String setupMessage(int percent) {
        return "Packages lag rahe hain... " + percent + "%\n"
            + "Ye sirf pehli dafa hota hai — 1–3 minute lagenge.\n"
            + "Internet ki zaroorat nahi, sab kuch app ke andar hai.";
    }

    /** Setup fail hone par design-style error popup (stock AlertDialog nahi). */
    private static void showSetupError(final Activity activity, final Exception e,
                                       final Runnable whenDone) {
        final Dialog dialog = new Dialog(activity);
        android.widget.LinearLayout panel = BulkTheme.modalPanel(activity, "Setup problem", dialog);
        android.widget.TextView body = new android.widget.TextView(activity);
        body.setText("Termux Mod ki pehli setup mukammal nahi ho saki.\n\n"
            + "Error: " + e.getMessage() + "\n\n"
            + "Phone ko restart karke app dobara kholo, ya neeche Try Again dabao. Tumhari koi file delete nahi hui.");
        body.setTextSize(12.5f);
        body.setTextColor(BulkTheme.textSub(activity));
        body.setLineSpacing(0, 1.35f);
        panel.addView(body);

        android.widget.TextView again = BulkTheme.primaryButton(activity, "Try Again", v -> {
            dialog.dismiss();
            runIfNeeded(activity, whenDone);
        });
        android.widget.LinearLayout.LayoutParams alp = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = BulkTheme.dp(activity, 14);
        again.setLayoutParams(alp);
        panel.addView(again);

        android.widget.TextView closeApp = BulkTheme.secondaryButton(activity, "Close App",
            v -> activity.finish());
        android.widget.LinearLayout.LayoutParams clp = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = BulkTheme.dp(activity, 10);
        closeApp.setLayoutParams(clp);
        panel.addView(closeApp);

        Dialog shown = BulkTheme.showPanelDialog(activity, panel);
        shown.setCancelable(false);
    }

    private static void dismiss(Dialog p) {
        try { p.dismiss(); } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ usr extract
    interface ProgressCallback { void onProgress(int percent); }

    /** Asset stream ke parhe gaye bytes gin kar asal progress % deta hai. */
    private static final class CountingInputStream extends FilterInputStream {
        private final long total;
        private final ProgressCallback cb;
        private long read = 0;
        private int lastPercent = -1;

        CountingInputStream(InputStream in, long total, ProgressCallback cb) {
            super(in);
            this.total = total;
            this.cb = cb;
        }

        @Override public int read(byte[] b, int off, int len) throws java.io.IOException {
            int n = super.read(b, off, len);
            if (n > 0) add(n);
            return n;
        }

        @Override public int read() throws java.io.IOException {
            int r = super.read();
            if (r >= 0) add(1);
            return r;
        }

        private void add(long n) {
            read += n;
            if (cb != null && total > 0) {
                int p = (int) Math.min(99, (read * 100) / total);
                if (p != lastPercent) {
                    lastPercent = p;
                    cb.onProgress(p);
                }
            }
        }
    }

    private static void extractUsrArchive(Context context, ProgressCallback cb) throws Exception {
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
                    + " MB free). Termux Mod ko kam az kam 1.5 GB khaali jagah chahiye. "
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
        // Asal total size: asset STORED (uncompressed) hai, openFd exact length deta hai
        long totalBytes = -1;
        try (AssetFileDescriptor fd = context.getAssets().openFd(USR_ASSET)) {
            totalBytes = fd.getLength();
        } catch (Exception ignored) {}
        CountingInputStream counting = new CountingInputStream(raw, totalBytes, cb);
        BufferedInputStream buffered = new BufferedInputStream(counting, 1 << 16);
        buffered.mark(2);
        int magic1 = buffered.read();
        int magic2 = buffered.read();
        buffered.reset();
        // Andar ka data gzip hai ya seedha tar — magic bytes (1f 8b) se khud
        // pehchano, naam/extension par kabhi bharosa nahi (v119 wala sabak).
        InputStream untar = (magic1 == 0x1f && magic2 == 0x8b)
            ? new GzipCompressorInputStream(buffered) : buffered;
        TarArchiveInputStream tar = new TarArchiveInputStream(untar, "UTF-8");

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
        if (cb != null) cb.onProgress(100);
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
            + template.replace("__SOCK__", BulkApiServer.socketName())
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
