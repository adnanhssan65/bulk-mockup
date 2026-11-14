package com.termux.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Bulk Mockup — Permissions screen (sidebar se khulti hai, pehli dafa setup
 * ke baad bhi yahi screen ati hai).
 *
 * Har cheez ek row me: naam, saada hint, status (✔/✖) aur theek karne ka
 * button. "Allow all" dabane se tamam missing cheezein ek ek karke samne
 * ati hain. Status har dafa screen khulne par fresh check hota hai —
 * storage ka status asal file likh/parh kar test hota hai, sirf permission
 * flag se nahi.
 */
public class BulkPermissionsActivity extends Activity {

    private static final String LOG_TAG = "BulkPermissions";
    private static final int GREEN = 0xFF00FF41;
    private static final int RED = 0xFFFF5252;
    private static final int REQ_STORAGE = 1001;
    private static final int REQ_NOTIFICATIONS = 1002;
    private static final String PREFS = "bulk_permissions";
    private static final String PREF_AUTOSTART_OPENED = "autostart_opened";

    private LinearLayout mRoot;
    private boolean mAllowAll = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoot = new LinearLayout(this);
        mRoot.setOrientation(LinearLayout.VERTICAL);
        mRoot.setBackgroundColor(Color.BLACK);
        mRoot.setPadding(dp(16), dp(16), dp(16), dp(16));
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(mRoot);
        setContentView(scroll);
        setTitle("Permissions");
        if (getIntent() != null && getIntent().getBooleanExtra("first_run", false)) {
            mAllowAll = false; // pehli dafa user khud "Allow all" dabaye — surprise nahi
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
        if (mAllowAll) advanceAllowAll();
    }

    // ------------------------------------------------------------- status checks

    /** Storage ka ASAL test: /storage/emulated/0 me file likho, parho, delete karo. */
    private boolean testStorage() {
        try {
            File dir = Environment.getExternalStorageDirectory();
            File test = new File(dir, ".bulk_mockup_perm_test");
            try (FileOutputStream out = new FileOutputStream(test)) {
                out.write(42);
            }
            boolean ok;
            try (FileInputStream in = new FileInputStream(test)) {
                ok = in.read() == 42;
            }
            //noinspection ResultOfMethodCallIgnored
            test.delete();
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasStorageLinks() {
        return new File(BulkMockupSetup.homeDir(), "storage/shared").exists()
            || new File(BulkMockupSetup.homeDir(), "storage").exists();
    }

    private boolean isBatteryOk() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNotificationsOk() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isInstallOk() {
        if (Build.VERSION.SDK_INT < 26) return true;
        try {
            return getPackageManager().canRequestPackageInstalls();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAutostartDone() {
        return getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_AUTOSTART_OPENED, false);
    }

    // ------------------------------------------------------------- fix actions

    private void fixStorage() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE"}, REQ_STORAGE);
        }
    }

    private void fixBattery() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {}
        }
    }

    private void fixNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIFICATIONS);
        }
    }

    private void fixInstall() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES));
            } catch (Exception ignored) {}
        }
    }

    /** Phone ki company ke hisab se background/auto-start wali screen kholna (best effort). */
    private void fixAutostart() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(PREF_AUTOSTART_OPENED, true).apply();
        String brand = (Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase());
        Intent intent = null;
        try {
            if (brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutostartManagementActivity"));
            } else if (brand.contains("oppo") || brand.contains("realme")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (brand.contains("vivo") || brand.contains("iqoo")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if (brand.contains("huawei") || brand.contains("honor")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
            } else if (brand.contains("samsung")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"));
            }
        } catch (Exception ignored) {
            intent = null;
        }
        if (intent != null) {
            try {
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }
        // Fallback: app ki apni system settings page
        try {
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.parse("package:" + getPackageName()));
            startActivity(fallback);
        } catch (Exception ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        rebuild();
        if (mAllowAll) advanceAllowAll();
    }

    // ------------------------------------------------------------- Allow all

    private void advanceAllowAll() {
        if (!testStorage()) { fixStorage(); return; }
        ensureStorageLinks();
        if (!isBatteryOk()) { fixBattery(); return; }
        if (!isNotificationsOk()) { fixNotifications(); return; }
        if (!isInstallOk()) { fixInstall(); return; }
        if (!isAutostartDone()) { fixAutostart(); return; }
        mAllowAll = false;
        Toast.makeText(this, "Sab permissions ho gayi ✔", Toast.LENGTH_LONG).show();
    }

    private void ensureStorageLinks() {
        if (testStorage() && !hasStorageLinks()) {
            try {
                TermuxInstaller.setupStorageSymlinks(this);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "setupStorageSymlinks failed: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------- UI

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void rebuild() {
        ensureStorageLinks();
        mRoot.removeAllViews();

        TextView title = new TextView(this);
        title.setText("PERMISSIONS");
        title.setTextColor(GREEN);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        mRoot.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Ye sab cheezein app ke sahi kaam ke liye zaroori hain.\n(All of these are needed for the app to work properly.)");
        sub.setTextColor(Color.LTGRAY);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(12));
        mRoot.addView(sub);

        Button allowAll = new Button(this);
        allowAll.setText("✔  ALLOW ALL  (Sab ek sath)");
        allowAll.setTextSize(16);
        allowAll.setOnClickListener(v -> {
            mAllowAll = true;
            advanceAllowAll();
            rebuild();
        });
        mRoot.addView(allowAll);

        addRow("Storage (files)",
            "Wallpapers parhne/likhne ke liye. (To read and save your wallpapers.)",
            testStorage(), "Allow", this::fixStorage);

        addRow("Battery",
            "Kaam ke doran app band na ho. (So the app is not stopped during work.)",
            isBatteryOk(), "Allow", this::fixBattery);

        if (Build.VERSION.SDK_INT >= 33) {
            addRow("Notifications",
                "Kaam mukammal hone ki khabar ke liye. (To show progress messages.)",
                isNotificationsOk(), "Allow", this::fixNotifications);
        }

        if (Build.VERSION.SDK_INT >= 26) {
            addRow("Install updates",
                "Nayi update isi app se lagane ke liye. (To install app updates from here.)",
                isInstallOk(), "Allow", this::fixInstall);
        }

        addRow("Background / Auto-start",
            "Screen band hone par bhi app chalta rahe — apne phone ki setting kholo or Bulk Mockup ko Allow karo. (Let the app keep running in the background.)",
            isAutostartDone(), "Open", this::fixAutostart);

        TextView footer = new TextView(this);
        footer.setText("\nApp: Bulk Mockup  •  Package: " + getPackageName()
            + "\nHome: " + TermuxConstants.TERMUX_HOME_DIR_PATH);
        footer.setTextColor(Color.GRAY);
        footer.setTextSize(11);
        mRoot.addView(footer);
    }

    private void addRow(String name, String hint, boolean ok, String buttonText, Runnable fix) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(12), dp(10), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView status = new TextView(this);
        status.setText(ok ? "✔ " : "✖ ");
        status.setTextColor(ok ? GREEN : RED);
        status.setTextSize(18);
        top.addView(status);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(label);

        if (!ok) {
            Button btn = new Button(this);
            btn.setText(buttonText);
            btn.setOnClickListener(v -> fix.run());
            top.addView(btn);
        }
        row.addView(top);

        TextView hintView = new TextView(this);
        hintView.setText(hint);
        hintView.setTextColor(Color.LTGRAY);
        hintView.setTextSize(12);
        hintView.setPadding(dp(26), dp(2), 0, 0);
        row.addView(hintView);

        mRoot.addView(row);

        View line = new View(this);
        line.setBackgroundColor(0xFF1B5E20);
        line.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        mRoot.addView(line);
    }
}
