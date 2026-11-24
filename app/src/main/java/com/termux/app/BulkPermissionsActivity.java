package com.termux.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.termux.shared.android.PermissionUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;

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
    private static final int RED = 0xFFFF5252;
    private static final int REQ_NOTIFICATIONS = 1002;

    private LinearLayout mRoot;
    private boolean mAllowAll = false;
    // v129: Auto-start ka koi REAL system check Android/OEM par mojood
    // nahi — is liye is ki halat KABHI ✔ nahi dikhti (v128 me sirf
    // button dabane par ek saved flag se ✔ ajata tha, jo user ko
    // "fake show" lagta tha — bilkul durust shikayat). Ye field sirf
    // Allow-All chain ko ek dafa settings kholne ke baad aage barhane
    // ke liye hai; screen par halat ke tor par kabhi istemal nahi hota.
    private boolean mAutostartOpened = false;
    // Pehli dafa (first run): zaroori popups khud ek ek karke samne ate hain —
    // Storage, phir Battery, phir Notifications (owner ki hidayat 2026-11-14).
    private boolean mFirstRunMode = false;
    private boolean mAutoFirstRun = false;
    private int mAutoLaunchedStep = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        mRoot = new LinearLayout(this);
        mRoot.setOrientation(LinearLayout.VERTICAL);
        mRoot.setBackgroundColor(BulkTheme.appBg(this));
        mRoot.setPadding(dp(16), dp(16), dp(16), dp(16));
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BulkTheme.appBg(this));
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

    /** Storage ka ASAL test — shared probe (BulkStorageAccess), taake tamam screens ka mayaar ek hi ho. */
    private boolean testStorage() {
        return BulkStorageAccess.probeStorage();
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

    // v129: isAutostartDone() KHATAM — koi asal check hai hi nahi, to
    // ✔ dikhana jhoot tha. Auto-start ab nav row hai (status ke baghair).

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
        // Sirf chain ke liye yaad rakho ke settings kholi gayi — ye
        // "permission mil gayi" NAHI hai aur screen par wesa dikhta bhi nahi.
        mAutostartOpened = true;
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
                BulkTheme.showToast(this, BulkStrings.t(this, BulkStrings.PERM_DONE_TOAST));
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
        if (!mAutostartOpened) { fixAutostart(); return; }
        mAllowAll = false;
        BulkTheme.showToast(this, BulkStrings.t(this, BulkStrings.PERM_ALL_DONE_TOAST));
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
        title.setTextColor(BulkTheme.accent(this));
        title.setTextSize(20);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        mRoot.addView(title);

        TextView sub = new TextView(this);
        sub.setText(BulkStrings.t(this, BulkStrings.PERM_SUB));
        sub.setTextColor(BulkTheme.textMuted(this));
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(12));
        mRoot.addView(sub);

        TextView allowAll = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.ALLOW_ALL), v -> {
                mAllowAll = true;
                advanceAllowAll();
                rebuild();
            });
        allowAll.setTextSize(15);
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

        // v129: Auto-start par ✔/✖ wala jhoota status nahi — ye sirf
        // settings kholne wali nav row hai (asal ijazat phone ki
        // setting me hi milti hai, app use parh nahi sakta).
        addNavRow(BulkStrings.t(this, BulkStrings.PERM_AUTOSTART)
                + " · " + BulkStrings.t(this, BulkStrings.PERM_MANUAL_TAG),
            BulkStrings.t(this, BulkStrings.PERM_AUTOSTART_HINT),
            BulkStrings.t(this, BulkStrings.OPEN), this::fixAutostart);

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

        // v132: Recreate Folders — TermuxMod drop folders + project ke
        // standard subfolders dobara banao (sirf create, delete kabhi nahi)
        // v134: row ke top-right par gear — auto/manual settings popup
        addRecreateRow();

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
            TextView goDash = BulkTheme.primaryButton(this,
                BulkStrings.t(this, BulkStrings.GO_DASHBOARD) + "  →",
                v -> completeFirstRun());
            goDash.setTextSize(15);
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            glp.topMargin = dp(4);
            goDash.setLayoutParams(glp);
            mRoot.addView(goDash);
        }

        TextView footer = new TextView(this);
        footer.setText("\nApp: Termux Mod  •  Package: " + getPackageName()
            + "\nHome: " + TermuxConstants.TERMUX_HOME_DIR_PATH);
        footer.setTextColor(BulkTheme.textMuted(this));
        footer.setTextSize(11);
        mRoot.addView(footer);

        // v128 restyle: muntakhab font har rebuild ke baad dobara lagao
        BulkFonts.applyToTree(this, mRoot);
    }

    /**
     * v134: Recreate Folders wali row — addNavRow jesi hi shakal,
     * lekin top-right par chhota gear button bhi: us se auto/manual
     * wali settings ka popup khulta hai. Row ki baqi design bilkul
     * baqi rows jesi hi hai (koi bara block nahi).
     */
    private void addRecreateRow() {
        LinearLayout row = permCard();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView status = new TextView(this);
        status.setText("› ");
        status.setTextColor(BulkTheme.accent(this));
        status.setTextSize(17);
        top.addView(status);

        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.RECREATE_FOLDERS));
        label.setTextColor(BulkTheme.textMain(this));
        label.setTextSize(15);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(label);

        TextView btn = BulkTheme.secondaryButton(this,
            BulkStrings.t(this, BulkStrings.RECREATE), v -> recreateFolders());
        btn.setTextSize(12.5f);
        top.addView(btn);

        LinearLayout gear = BulkTheme.circleButton(this, com.termux.R.drawable.ic_bulk_gear, 30,
            v -> showRecreateSettings());
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        glp.leftMargin = dp(8);
        gear.setLayoutParams(glp);
        top.addView(gear);
        row.addView(top);

        TextView hintView = new TextView(this);
        hintView.setText(BulkStrings.t(this, BulkStrings.RECREATE_FOLDERS_HINT));
        hintView.setTextColor(BulkTheme.textMuted(this));
        hintView.setTextSize(12);
        hintView.setPadding(dp(26), dp(2), 0, 0);
        row.addView(hintView);

        mRoot.addView(row);
    }

    /**
     * v134: Recreate ki settings — do chhote switch rows (Folders /
     * Preview Videos ka Auto Recreate). Toggle dabate hi setting
     * save ho jati hai. DEFAULT dono OFF (manual): OFF ho to folders
     * or videos sirf Recreate Folders dabane se dobara bante hain,
     * khud kabhi wapas nahi ate.
     */
    private void showRecreateSettings() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.RECREATE_SETTINGS), dialog);
        panel.addView(recreateSwitchRow(
            BulkStrings.t(this, BulkStrings.RECREATE_FOLDERS_AUTO),
            BulkRecreateSettings.foldersAuto(this),
            on -> BulkRecreateSettings.setFoldersAuto(this, on)));
        panel.addView(recreateSwitchRow(
            BulkStrings.t(this, BulkStrings.RECREATE_VIDEOS_AUTO),
            BulkRecreateSettings.videosAuto(this),
            on -> BulkRecreateSettings.setVideosAuto(this, on)));
        TextView close = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.CLOSE), v -> dialog.dismiss());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(14);
        close.setLayoutParams(clp);
        panel.addView(close);
        BulkTheme.showPanelDialog(this, panel);
        BulkFonts.applyToTree(this, panel);
    }

    private interface RecreateToggle {
        void onChanged(boolean on);
    }

    /** Ek switch row: label + OFF wali note (left), Switch (right). */
    private LinearLayout recreateSwitchRow(String labelText, boolean initial,
                                           RecreateToggle toggle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(10);
        row.setLayoutParams(rlp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(BulkTheme.textMain(this));
        label.setTextSize(14.5f);
        texts.addView(label);
        TextView note = new TextView(this);
        note.setText(BulkStrings.t(this, BulkStrings.RECREATE_AUTO_OFF_NOTE));
        note.setTextColor(BulkTheme.textMuted(this));
        note.setTextSize(11.5f);
        texts.addView(note);
        row.addView(texts);

        Switch sw = new Switch(this);
        sw.setChecked(initial);
        try {
            android.content.res.ColorStateList tint =
                android.content.res.ColorStateList.valueOf(BulkTheme.accent(this));
            sw.setThumbTintList(tint);
            sw.setTrackTintList(tint);
        } catch (Exception ignored) {}
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> toggle.onChanged(isChecked));
        row.addView(sw);
        return row;
    }

    /** v132: structural folders dobara banao; natija design dialog me. */
    private void recreateFolders() {
        new Thread(() -> {
            int[] counts = BulkDropFolders.recreateAll(BulkDropFolders.knownNumbers());
            runOnUiThread(() -> {
                if (isFinishing()) return;
                android.app.Dialog dialog = new android.app.Dialog(this);
                LinearLayout panel = BulkTheme.modalPanel(this,
                    BulkStrings.t(this, BulkStrings.RECREATE_FOLDERS), dialog);
                TextView body = new TextView(this);
                body.setText(BulkStrings.format(this, BulkStrings.RECREATE_RESULT,
                    counts[0], counts[1]));
                body.setTextColor(BulkTheme.textMain(this));
                body.setTextSize(14.5f);
                panel.addView(body);
                TextView close = BulkTheme.primaryButton(this,
                    BulkStrings.t(this, BulkStrings.CLOSE), v -> dialog.dismiss());
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
                clp.topMargin = dp(14);
                close.setLayoutParams(clp);
                panel.addView(close);
                BulkTheme.showPanelDialog(this, panel);
                BulkFonts.applyToTree(this, panel);
            });
        }).start();
    }

    private void addRow(String name, String hint, boolean ok, String buttonText, Runnable fix) {
        LinearLayout row = permCard();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView status = new TextView(this);
        status.setText(ok ? "✔ " : "✖ ");
        status.setTextColor(ok ? BulkTheme.accent(this) : RED);
        status.setTextSize(17);
        top.addView(status);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(BulkTheme.textMain(this));
        label.setTextSize(15);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(label);

        if (!ok) {
            TextView btn = BulkTheme.primaryButton(this, buttonText, v -> fix.run());
            btn.setTextSize(12.5f);
            top.addView(btn);
        }
        row.addView(top);

        TextView hintView = new TextView(this);
        hintView.setText(hint);
        hintView.setTextColor(BulkTheme.textMuted(this));
        hintView.setTextSize(12);
        hintView.setPadding(dp(26), dp(2), 0, 0);
        row.addView(hintView);

        mRoot.addView(row);
    }

    /** v128 restyle: design wala glass card — har permission row isi par banti hai. */
    private LinearLayout permCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(BulkTheme.glassPanelStrong(this));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);
        return card;
    }

    /** addRow jesi hi row, lekin status ki jagah "›" or button hamesha — dusri screen kholne ke liye. */
    private void addNavRow(String name, String hint, String buttonText, Runnable action) {
        LinearLayout row = permCard();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView status = new TextView(this);
        status.setText("› ");
        status.setTextColor(BulkTheme.accent(this));
        status.setTextSize(17);
        top.addView(status);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(BulkTheme.textMain(this));
        label.setTextSize(15);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(label);

        TextView btn = BulkTheme.secondaryButton(this, buttonText, v -> action.run());
        btn.setTextSize(12.5f);
        top.addView(btn);
        row.addView(top);

        TextView hintView = new TextView(this);
        hintView.setText(hint);
        hintView.setTextColor(BulkTheme.textMuted(this));
        hintView.setTextSize(12);
        hintView.setPadding(dp(26), dp(2), 0, 0);
        row.addView(hintView);

        mRoot.addView(row);
    }
}
