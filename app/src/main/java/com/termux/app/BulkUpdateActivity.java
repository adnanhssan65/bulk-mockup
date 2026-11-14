package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.termux.shared.logger.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Termux Mod — "Check for updates" (sidebar se khulta hai).
 *
 * Ye app ki WAHID network feature hai, aur sirf tab chalti hai jab user
 * khud ye screen khole. GitHub Releases se taaza version dekhti hai,
 * nayi ho to release notes + "Download and install" button dikhati hai.
 * Update wohi keystore se signed hoti hai, is liye purani app par hi
 * install ho jati hai; ~/.bashrc, ~/projects.conf aur tamam data mehfooz
 * rehta hai.
 */
public class BulkUpdateActivity extends Activity {

    private static final String LOG_TAG = "BulkUpdate";

    /**
     * Owner ka GitHub repo — "username/repo-naam" ki shakal me.
     * (Repo badalna ho to sirf yahi ek line badalni hai.)
     */
    public static final String GITHUB_REPO = "adnanhssan65/bulk-mockup";

    private TextView mStatus;
    private TextView mNotes;
    private ProgressBar mProgress;
    private Button mInstallButton;
    private String mApkUrl;
    private String mNewVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("CHECK FOR UPDATES");
        title.setTextColor(0xFF00FF41);
        title.setTextSize(19);
        root.addView(title);

        mStatus = new TextView(this);
        mStatus.setTextColor(Color.WHITE);
        mStatus.setTextSize(15);
        mStatus.setPadding(0, pad, 0, pad / 2);
        mStatus.setText("Dekh raha hai... (Checking...)");
        root.addView(mStatus);

        mProgress = new ProgressBar(this);
        root.addView(mProgress);

        mNotes = new TextView(this);
        mNotes.setTextColor(0xFFB0B0B0);
        mNotes.setTextSize(13);
        root.addView(mNotes);

        mInstallButton = new Button(this);
        mInstallButton.setText("Download and install");
        mInstallButton.setVisibility(View.GONE);
        mInstallButton.setOnClickListener(v -> downloadAndInstall());
        root.addView(mInstallButton);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(root);
        setContentView(scroll);
        setTitle("Check for updates");

        checkForUpdate();
    }

    private long installedVersionCode() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= 28) return pi.getLongVersionCode();
            //noinspection deprecation
            return pi.versionCode;
        } catch (Exception e) {
            return -1;
        }
    }

    private String installedVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private void checkForUpdate() {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(15_000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "BulkMockup");
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("GitHub se jawab nahi aya (code " + code + ")");
                String body = readAll(conn.getInputStream());
                JSONObject release = new JSONObject(body);
                String tag = release.optString("tag_name", "");
                long remoteCode = parseVersionCode(tag);
                String notes = release.optString("body", "");

                String apk = null;
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.optJSONObject(i);
                        if (a != null && "BulkMockup.apk".equals(a.optString("name"))) {
                            apk = a.optString("browser_download_url");
                            break;
                        }
                    }
                }

                final long installed = installedVersionCode();
                final String fname = apk;
                final String ftag = tag;
                runOnUiThread(() -> {
                    mProgress.setVisibility(View.GONE);
                    if (remoteCode > installed && fname != null) {
                        mApkUrl = fname;
                        mNewVersion = ftag;
                        mStatus.setText("Nayi update hai ✔  (" + ftag + ")\n"
                            + "Tumhara version: " + installedVersionName());
                        mNotes.setText(notes);
                        mInstallButton.setVisibility(View.VISIBLE);
                    } else if (remoteCode > installed) {
                        mStatus.setText("Nayi update hai (" + ftag + ") lekin release me BulkMockup.apk nahi mili.");
                    } else {
                        mStatus.setText("App up to date hai ✔\nVersion: " + installedVersionName());
                        mNotes.setText("");
                    }
                });
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "update check failed: " + e.getMessage());
                runOnUiThread(() -> {
                    mProgress.setVisibility(View.GONE);
                    mStatus.setText("Check nahi ho saka.\nInternet hai to dobara koshish karo. "
                        + "(No internet, or the server did not answer.)");
                    mNotes.setText("");
                });
            }
        }).start();
    }

    /** Tag "v123" ya "123" se versionCode nikalo; na mile to -1. */
    static long parseVersionCode(String tag) {
        if (tag == null) return -1;
        String digits = tag.trim();
        if (digits.startsWith("v") || digits.startsWith("V")) digits = digits.substring(1);
        try {
            return Long.parseLong(digits.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private void downloadAndInstall() {
        // Install ki ijazat na ho to pehle Permissions screen ki taraf bhejo
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            mStatus.setText("Pehle 'Install updates' wali ijazat do — Permissions screen khul rahi hai.");
            startActivity(new Intent(this, BulkPermissionsActivity.class));
            return;
        }
        mInstallButton.setEnabled(false);
        mProgress.setVisibility(View.VISIBLE);
        mStatus.setText("Download ho raha hai... (Downloading " + mNewVersion + ")");
        new Thread(() -> {
            try {
                File out = new File(getExternalCacheDir() != null
                    ? getExternalCacheDir() : getCacheDir(), "BulkMockup-update.apk");
                HttpURLConnection conn = (HttpURLConnection) new URL(mApkUrl).openConnection();
                conn.setConnectTimeout(20_000);
                conn.setReadTimeout(30_000);
                conn.setRequestProperty("User-Agent", "BulkMockup");
                conn.setInstanceFollowRedirects(true);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Download fail (code " + code + ")");
                try (InputStream in = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[1 << 16];
                    int r;
                    while ((r = in.read(buf)) != -1) fos.write(buf, 0, r);
                }
                if (out.length() < 1_000_000) throw new Exception("File adhoori download hui");
                runOnUiThread(() -> installApk(out));
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "download failed: " + e.getMessage());
                runOnUiThread(() -> {
                    mProgress.setVisibility(View.GONE);
                    mInstallButton.setEnabled(true);
                    mStatus.setText("Download adhoora reh gaya. Internet check karke dobara dabao. "
                        + "(Download failed — try again.)");
                });
            }
        }).start();
    }

    private void installApk(File apk) {
        mProgress.setVisibility(View.GONE);
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".bulkfiles", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            mStatus.setText("Install wali screen khul gayi hai — wahan Install dabao. "
                + "Tumhara data, .bashrc aur projects list mehfooz rahegi.");
        } catch (Exception e) {
            mStatus.setText("Install screen nahi khul saki: " + e.getMessage());
            mInstallButton.setEnabled(true);
        }
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            char[] buf = new char[8192];
            int r;
            while ((r = reader.read(buf)) != -1) sb.append(buf, 0, r);
        }
        return sb.toString();
    }
}
