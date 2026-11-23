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

import com.termux.shared.android.PermissionUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Termux Mod — Permissions screen (sidebar se khulti hai, pehli dafa setup
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
    private static final int REQ_NOTIFICATIONS = 1002;
    private static final String PREFS = "bulk_permissions";
    private static final String PREF_AUTOSTART_OPENED = "autostart_opened";

    private LinearLayout mRoot;
    private boolean mAllowAll = false;
    // Pehli dafa (first run): zaroori popups khud ek ek karke samne ate hain —
    // Storage, phir Battery, phir Notifications (owner ki hidayat 2026-11-14).
    private boolean mFirstRunMode = false;
    private boolean mAutoFirstRun = false;
    private int mAutoLaunchedStep = -1;

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
        BulkFonts.apply(this);
        setTitle("Permissions");
        if (getIntent() != null && getIntent().getBooleanExtra("first_run", false)) {
            mFirstRunMode = true;
            mAutoFirstRun = true; // popups wali auto flow — onResume se shuru hogi
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
        if (mAllowAll) advanceAllowAll();
        else if (mAutoFirstRun) advanceAutoPopups();
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
        // Termux ka apna proven tareeqa (v126): PermissionUtils legacy
        // permission ya — Android 11+ par — MANAGE_EXTERNAL_STORAGE wali
        // settings khud chunta hai. Purana seedha requestPermissions
        // (WRITE/READ) naye Android par popup dikhaye bager hi fail ho
        // jata tha, is liye chain pehle step par hi ruk jati thi.
        // Result onRequestPermissionsResult / onActivityResult
        // (REQUEST_GRANT_STORAGE_PERMISSION) se wapas ata hai.
        boolean granted = PermissionUtils.checkAndRequestLegacyOrManageExternalStoragePermission(
            this, PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION, false);
        if (granted && testStorage()) {
            // Permission pehle se mili hui thi — koi callback nahi ayega,
            // chain yahin se aage barhao (test phir bhi fail ho to loop
            // nahi: row ✖ hi rahegi, user Terminal fallback use kar sakta hai)
            rebuild();
            if (mAllowAll) advanceAllowAll();
            else if (mAutoFirstRun) advanceAutoPopups();
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
        else if (mAutoFirstRun) advanceAutoPopups();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Storage ka MANAGE_EXTERNAL_STORAGE wala rasta Settings se
        // wapas yahin ata hai — chain yahan se aage barhao
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            rebuild();
            if (mAllowAll) advanceAllowAll();
            else if (mAutoFirstRun) advanceAutoPopups();
        }
    }

    // ------------------------------------------------- first-run hand-off (v126)

    /**
     * First-run chain mukammal (ya user ne "Dashboard Par Jao" se skip
     * kiya): flag lagao taake popups agli dafa start par na ayen, phir
     * dashboard khol kar ye screen band kar do. First-run mode na ho to
     * kuch nahi hota (sidebar se khuli screen waisi hi rehti hai).
     */
    private void completeFirstRun() {
        if (!mFirstRunMode) return;
        mFirstRunMode = false;
        BulkMockupSetup.markFirstRunPermissionsDone(this);
        try {
            startActivity(new Intent(this, BulkDashboardActivity.class));
        } catch (Exception ignored) {}
        finish();
    }

    // ------------------------------------------------- first-run auto popups

    /**
     * Sirf popup wali zaroori permissions, ek ek karke: Storage → Battery →
     * Notifications. Jo pehle se mili ho wo skip. User kisi popup ko mana kar
     * de to auto flow wahi ruk jata hai (loop nahi) — baqi list se khud kar
     * sakta hai. Install / Auto-start is me nahi — wo settings screens hain,
     * pehli dafa ke liye bhaari hain; Allow all me milte hain.
     */
    private void advanceAutoPopups() {
        int step;
        Runnable fix;
        if (!testStorage()) {
            step = 0; fix = this::fixStorage;
        } else {
            ensureStorageLinks();
            if (!isBatteryOk()) {
                step = 1; fix = this::fixBattery;
            } else if (!isNotificationsOk()) {
                step = 2; fix = this::fixNotifications;
            } else {
                mAutoFirstRun = false;
                Toast.makeText(this, BulkStrings.t(this, BulkStrings.PERM_DONE_TOAST), Toast.LENGTH_LONG).show();
                completeFirstRun();
                return;
            }
        }
        if (step == mAutoLaunchedStep) {
            // Ye step pehle try ho chuka or permission abhi tak nahi mili —
            // user ne mana kiya hoga. Auto band; neeche list se khud kar le.
            mAutoFirstRun = false;
            return;
        }
        mAutoLaunchedStep = step;
        fix.run();
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
        Toast.makeText(this, BulkStrings.t(this, BulkStrings.PERM_ALL_DONE_TOAST), Toast.LENGTH_LONG).show();
        completeFirstRun();
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
        sub.setText(BulkStrings.t(this, BulkStrings.PERM_SUB));
        sub.setTextColor(Color.LTGRAY);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(12));
        mRoot.addView(sub);

        Button allowAll = new Button(this);
        allowAll.setText(BulkStrings.t(this, BulkStrings.ALLOW_ALL));
        allowAll.setTextSize(16);
        allowAll.setOnClickListener(v -> {
            mAllowAll = true;
            advanceAllowAll();
            rebuild();
        });
        mRoot.addView(allowAll);

        addRow("Storage (files)",
            BulkStrings.t(this, BulkStrings.PERM_STORAGE_HINT),
            testStorage(), BulkStrings.t(this, BulkStrings.ALLOW), this::fixStorage);

        addRow(BulkStrings.t(this, BulkStrings.PERM_BATTERY),
            BulkStrings.t(this, BulkStrings.PERM_BATTERY_HINT),
            isBatteryOk(), BulkStrings.t(this, BulkStrings.ALLOW), this::fixBattery);

        if (Build.VERSION.SDK_INT >= 33) {
            addRow(BulkStrings.t(this, BulkStrings.PERM_NOTIFICATIONS),
                BulkStrings.t(this, BulkStrings.PERM_NOTIFICATIONS_HINT),
                isNotificationsOk(), BulkStrings.t(this, BulkStrings.ALLOW), this::fixNotifications);
        }

        if (Build.VERSION.SDK_INT >= 26) {
            addRow(BulkStrings.t(this, BulkStrings.PERM_INSTALL),
                BulkStrings.t(this, BulkStrings.PERM_INSTALL_HINT),
                isInstallOk(), BulkStrings.t(this, BulkStrings.ALLOW), this::fixInstall);
        }

        addRow(BulkStrings.t(this, BulkStrings.PERM_AUTOSTART),
            BulkStrings.t(this, BulkStrings.PERM_AUTOSTART_HINT),
            isAutostartDone(), BulkStrings.t(this, BulkStrings.OPEN), this::fixAutostart);

        // Terminal (v126 fallback): popups kaam na karen to permission
        // commands terminal me ek ek karke chalane wali screen
        addNavRow(BulkStrings.t(this, BulkStrings.TERMINAL),
            BulkStrings.t(this, BulkStrings.PERM_TERMINAL_HINT),
            BulkStrings.t(this, BulkStrings.OPEN),
            () -> {
                try {
                    startActivity(new Intent(this, BulkPermissionsTerminalActivity.class));
                } catch (Exception ignored) {}
            });

        if (!mFirstRunMode) {
            // v127 (design): pehli-dafa wale popups dobara chalana —
            // flag reset karke yehi screen first-run mode me khulti hai
            addNavRow(BulkStrings.t(this, BulkStrings.REPLAY_POPUPS),
                BulkStrings.t(this, BulkStrings.REPLAY_POPUPS_HINT),
                BulkStrings.t(this, BulkStrings.OPEN),
                () -> {
                    BulkMockupSetup.resetFirstRunPermissionsDone(this);
                    try {
                        Intent intent = new Intent(this, BulkPermissionsActivity.class);
                        intent.putExtra("first_run", true);
                        startActivity(intent);
                    } catch (Exception ignored) {}
                    finish();
                });
        }

        if (mFirstRunMode) {
            // First-run escape hatch: chain kisi wajah se ruk jaye to user
            // yahan se dashboard ja sakta hai (flag lag jata hai — popups
            // agli start par dobara nahi ayenge; Permissions sidebar se
            // hamesha khul sakti hai).
            Button goDash = new Button(this);
            goDash.setText(BulkStrings.t(this, BulkStrings.GO_DASHBOARD) + "  →");
            goDash.setTextSize(15);
            goDash.setOnClickListener(v -> completeFirstRun());
            mRoot.addView(goDash);
        }

        TextView footer = new TextView(this);
        footer.setText("\nApp: Termux Mod  •  Package: " + getPackageName()
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

    /** addRow jesi hi row, lekin status ki jagah "›" or button hamesha — dusri screen kholne ke liye. */
    private void addNavRow(String name, String hint, String buttonText, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(12), dp(10), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView status = new TextView(this);
        status.setText("› ");
        status.setTextColor(GREEN);
        status.setTextSize(18);
        top.addView(status);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(label);

        Button btn = new Button(this);
        btn.setText(buttonText);
        btn.setOnClickListener(v -> action.run());
        top.addView(btn);
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
