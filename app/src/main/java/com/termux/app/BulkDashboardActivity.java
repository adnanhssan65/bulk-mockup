package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.shared.logger.Logger;

/**
 * Termux Mod — Home (v125). App ki front screen (LAUNCHER), approved
 * design ki native shakal: top bar (drawer + theme toggle), hero,
 * wave, 4 glass pill rows — Add Wallpapers / Projects / New Project
 * (ZIP) / Terminal — footer badge + credits + waves, or left sidebar
 * drawer. Projects ki list ab alag screen (BulkProjectsActivity) par hai.
 *
 * Pehli setup mukammal na ho to ye screen TermuxActivity ki taraf bhej
 * deti hai (wahan bootstrap + packages setup v123 ki tarah chalta hai);
 * setup ke baad app khud yahin wapas le ata hai.
 */
public class BulkDashboardActivity extends Activity {

    private static final String LOG_TAG = "BulkDashboard";

    /** TermuxActivity ko diya jane wala extra: ye command terminal me chalao. */
    public static final String EXTRA_RUN_COMMAND = "bulk_run_command";
    /** TermuxActivity ko diya jane wala extra: setup ke baad dashboard khol do. */
    public static final String EXTRA_OPEN_DASHBOARD_AFTER_SETUP = "bulk_open_dashboard_after_setup";

    private boolean mRoutedToSetup = false;
    private boolean mRoutedToPermissions = false;
    private View mScrim;
    private View mDrawer;
    private int mDrawerWidth;
    private boolean mDrawerOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Termux Mod");

        if (!BulkMockupSetup.isSetupDone()) {
            // Setup baqi hai — terminal ki taraf bhejo; setup ke baad wapas yahin ana hai.
            mRoutedToSetup = true;
            try {
                Intent intent = new Intent(this, TermuxActivity.class);
                intent.putExtra(EXTRA_OPEN_DASHBOARD_AFTER_SETUP, true);
                startActivity(intent);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Setup ke liye TermuxActivity nahi khula: " + e.getMessage());
                Toast.makeText(this, "Terminal nahi khul saka.", Toast.LENGTH_LONG).show();
            }
            finish();
            return;
        }

        if (!BulkMockupSetup.isFirstRunPermissionsDone(this)) {
            // v126: setup ho chuki lekin pehli-dafa wali permission chain
            // abhi user ke samne nahi ayi — dashboard se PEHLE wahi screen.
            // Chain mukammal (ya skip) hone par Permissions screen khud
            // dashboard kholti hai; ye gate har start par yehi tartib
            // pakki karta hai.
            mRoutedToPermissions = true;
            try {
                Intent intent = new Intent(this, BulkPermissionsActivity.class);
                intent.putExtra("first_run", true);
                startActivity(intent);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Permissions ke liye route nahi ho saka: " + e.getMessage());
                mRoutedToPermissions = false; // dashboard hi dikha do
                return;
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRoutedToSetup) return;
        if (mRoutedToPermissions) return;
        if (!BulkMockupSetup.isSetupDone()) return; // setup abhi chal raha hoga
        // Duplicates expiry — dashboard khulne par TTL guzri copies saaf
        new Thread(() -> BulkDuplicatesStore.runExpiry(this)).start();
        buildUi();
        // App ke andar wale projects: jo folder na ho, peeche lag jata hai
        BulkProjectBundles.installInBackground(this, () -> runOnUiThread(() -> {
            if (!isFinishing() && !mDrawerOpen) buildUi();
        }));
    }

    // ------------------------------------------------------------ UI

    private void buildUi() {
        BulkTheme.applyWindow(this);
        mDrawerOpen = false;
        int dp = BulkTheme.dp(this, 1);

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BulkTheme.appBg(this));

        BulkWaveView rays = new BulkWaveView(this, BulkWaveView.MODE_RAYS);
        frame.addView(rays, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ---- main column (scroll sirf chhoti screens par kaam ayega)
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        // Top utility bar: drawer button (left) + theme toggle (right)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(BulkTheme.dp(this, 20), BulkTheme.dp(this, 14), BulkTheme.dp(this, 20), BulkTheme.dp(this, 4));
        topBar.addView(BulkTheme.circleButton(this, R.drawable.ic_bulk_menu, 34, v -> openDrawer()));
        topBar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        // Language option (v126) — theme button ke sath; English default,
        // Roman Urdu select karne par tamam labels badal jate hain.
        topBar.addView(BulkTheme.circleButton(this, R.drawable.ic_bulk_language, 34, v -> showLanguageChooser()));
        View langGap = new View(this);
        langGap.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 10), 1));
        topBar.addView(langGap);
        int themeIcon = BulkTheme.isLight(this) ? R.drawable.ic_bulk_moon : R.drawable.ic_bulk_sun;
        topBar.addView(BulkTheme.circleButton(this, themeIcon, 34, v -> {
            BulkTheme.toggle(this);
            buildUi();
        }));
        column.addView(topBar);

        // Hero
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setClipChildren(false);
        hero.setPadding(BulkTheme.dp(this, 20), BulkTheme.dp(this, 4), BulkTheme.dp(this, 20), 0);

        LinearLayout heroLeft = new LinearLayout(this);
        heroLeft.setOrientation(LinearLayout.VERTICAL);
        heroLeft.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        android.widget.ImageView monitor = BulkTheme.icon(this, R.drawable.ic_bulk_monitor, BulkTheme.accent(this), 42);
        monitor.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 42), BulkTheme.dp(this, 37)));
        heroLeft.addView(monitor);

        TextView title = new TextView(this);
        title.setText(twoToneTitle());
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, BulkTheme.dp(this, 6), 0, 0);
        title.setLayoutParams(titleLp);
        heroLeft.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(BulkStrings.t(this, BulkStrings.SUBTITLE));
        subtitle.setTextSize(11.5f);
        subtitle.setTypeface(medium());
        subtitle.setTextColor(BulkTheme.textSub(this));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, BulkTheme.dp(this, 6), 0, 0);
        subtitle.setLayoutParams(subLp);
        heroLeft.addView(subtitle);

        View accentBar = new View(this);
        accentBar.setBackground(BulkTheme.accentBarBg(this));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(BulkTheme.dp(this, 96), BulkTheme.dp(this, 2));
        barLp.setMargins(0, BulkTheme.dp(this, 8), 0, 0);
        accentBar.setLayoutParams(barLp);
        heroLeft.addView(accentBar);
        hero.addView(heroLeft);

        // 3D tilted terminal card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(BulkTheme.heroCardBg(this));
        card.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 10), BulkTheme.dp(this, 12), BulkTheme.dp(this, 10));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(BulkTheme.dp(this, 124), BulkTheme.dp(this, 126));
        card.setLayoutParams(cardLp);
        card.setRotationY(-14f);
        card.setRotationX(4f);
        card.setCameraDistance(12000f);

        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 3; i++) {
            View dot = new View(this);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(BulkTheme.textMuted(this));
            dot.setBackground(dotBg);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(BulkTheme.dp(this, 6), BulkTheme.dp(this, 6));
            if (i > 0) dlp.setMargins(BulkTheme.dp(this, 6), 0, 0, 0);
            dot.setLayoutParams(dlp);
            dots.addView(dot);
        }
        card.addView(dots);
        card.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        TextView prompt = new TextView(this);
        prompt.setText("$_");
        prompt.setTextSize(19);
        prompt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        prompt.setTextColor(BulkTheme.accent(this));
        card.addView(prompt);
        card.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        View cardBar = new View(this);
        GradientDrawable cardBarBg = new GradientDrawable();
        cardBarBg.setColor(BulkTheme.accentSoft(this));
        cardBarBg.setCornerRadius(BulkTheme.dp(this, 999));
        cardBar.setBackground(cardBarBg);
        card.addView(cardBar, new LinearLayout.LayoutParams(BulkTheme.dp(this, 36), BulkTheme.dp(this, 4)));
        hero.addView(card);
        column.addView(hero);

        // Wave divider
        BulkWaveView wave = new BulkWaveView(this, BulkWaveView.MODE_HERO_WAVE);
        LinearLayout.LayoutParams waveLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 32));
        waveLp.setMargins(0, -BulkTheme.dp(this, 8), 0, 0);
        wave.setLayoutParams(waveLp);
        column.addView(wave);

        // 4 pill rows (naya design): Add Wallpapers / Run / Projects / Terminal
        LinearLayout pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.VERTICAL);
        pills.setPadding(BulkTheme.dp(this, 20), BulkTheme.dp(this, 6), BulkTheme.dp(this, 20), 0);
        pills.addView(pill(R.drawable.ic_bulk_image, BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS),
            v -> openScreen(BulkWallpapersActivity.class, "Wallpapers wali screen nahi khul saki.")));
        pills.addView(pill(R.drawable.ic_bulk_play, BulkStrings.t(this, BulkStrings.NAV_RUN),
            v -> showQuickRun()), gapLp(dp));
        pills.addView(pill(R.drawable.ic_bulk_folder, BulkStrings.t(this, BulkStrings.PROJECTS),
            v -> openScreen(BulkProjectsActivity.class, "Projects screen nahi khul saki.")), gapLp(dp));
        pills.addView(pill(R.drawable.ic_bulk_terminal, BulkStrings.t(this, BulkStrings.TERMINAL),
            v -> openTerminal()), gapLp(dp));
        column.addView(pills);

        // Spacer
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        spacerLp.setMargins(0, BulkTheme.dp(this, 24), 0, 0);
        spacer.setLayoutParams(spacerLp);
        column.addView(spacer);

        // Footer: lines + >_ badge
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        badgeRow.setPadding(BulkTheme.dp(this, 48), 0, BulkTheme.dp(this, 48), 0);
        View lineL = new View(this);
        lineL.setBackground(BulkTheme.lineBg(this, true));
        badgeRow.addView(lineL, new LinearLayout.LayoutParams(0, Math.max(2, BulkTheme.dp(this, 1.5f)), 1));
        TextView badge = new TextView(this);
        badge.setText(">_");
        badge.setTextSize(13.5f);
        badge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        badge.setTextColor(BulkTheme.accent(this));
        badge.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(BulkTheme.glassBg(this));
        badgeBg.setStroke(BulkTheme.dp(this, 2), BulkTheme.accent(this));
        badge.setBackground(badgeBg);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(BulkTheme.dp(this, 38), BulkTheme.dp(this, 38));
        badgeLp.setMargins(BulkTheme.dp(this, 12), 0, BulkTheme.dp(this, 12), 0);
        badge.setLayoutParams(badgeLp);
        badge.setClickable(true);
        badge.setOnClickListener(v -> openTerminal());
        badgeRow.addView(badge);
        View lineR = new View(this);
        lineR.setBackground(BulkTheme.lineBg(this, false));
        badgeRow.addView(lineR, new LinearLayout.LayoutParams(0, Math.max(2, BulkTheme.dp(this, 1.5f)), 1));
        column.addView(badgeRow);

        // Credits
        TextView credits = new TextView(this);
        credits.setText(creditsText());
        credits.setTextSize(10);
        credits.setTypeface(Typeface.DEFAULT_BOLD);
        credits.setGravity(Gravity.CENTER);
        try { credits.setLetterSpacing(0.2f); } catch (Exception ignored) {}
        LinearLayout.LayoutParams creditsLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        creditsLp.setMargins(0, BulkTheme.dp(this, 10), 0, 0);
        credits.setLayoutParams(creditsLp);
        column.addView(credits);

        TextView phone = new TextView(this);
        phone.setText("+44 7404 032136");
        phone.setTextSize(10);
        phone.setTypeface(Typeface.MONOSPACE);
        phone.setTextColor(BulkTheme.textMuted(this));
        phone.setGravity(Gravity.CENTER);
        try { phone.setLetterSpacing(0.18f); } catch (Exception ignored) {}
        LinearLayout.LayoutParams phoneLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        phoneLp.setMargins(0, BulkTheme.dp(this, 2), 0, 0);
        phone.setLayoutParams(phoneLp);
        column.addView(phone);

        // Footer waves
        BulkWaveView footerWaves = new BulkWaveView(this, BulkWaveView.MODE_FOOTER_WAVES);
        LinearLayout.LayoutParams fwLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 44));
        fwLp.setMargins(0, BulkTheme.dp(this, 6), 0, 0);
        footerWaves.setLayoutParams(fwLp);
        column.addView(footerWaves);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(column, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.addView(scroll, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        buildDrawer(frame);
        setContentView(frame);
        BulkFonts.apply(this);
    }

    private LinearLayout.LayoutParams gapLp(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 52));
        lp.setMargins(0, BulkTheme.dp(this, 10), 0, 0);
        return lp;
    }

    private LinearLayout pill(int iconRes, String label, View.OnClickListener click) {
        int height = BulkTheme.dp(this, 52);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), 0, BulkTheme.dp(this, 12), 0);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        row.setClickable(true);
        row.setOnClickListener(click);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackground(BulkTheme.iconBoxBg(this));
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 34), BulkTheme.dp(this, 34)));
        android.widget.ImageView iv = BulkTheme.icon(this, iconRes, BulkTheme.accent(this), 18);
        iv.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 18), BulkTheme.dp(this, 18)));
        iconBox.addView(iv);
        row.addView(iconBox);

        View divider = new View(this);
        divider.setBackgroundColor(BulkTheme.accentBorder(this));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(Math.max(1, BulkTheme.dp(this, 0.6f)), BulkTheme.dp(this, 20));
        divLp.setMargins(BulkTheme.dp(this, 11), 0, BulkTheme.dp(this, 11), 0);
        divider.setLayoutParams(divLp);
        row.addView(divider);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(14.5f);
        text.setTypeface(medium());
        text.setTextColor(BulkTheme.textMain(this));
        text.setSingleLine(true);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout arrow = new LinearLayout(this);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(BulkTheme.arrowCircleBg(this));
        arrow.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 26), BulkTheme.dp(this, 26)));
        android.widget.ImageView av = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.accent(this), 14);
        av.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        arrow.addView(av);
        row.addView(arrow);
        return row;
    }

    // ------------------------------------------------------------ drawer

    private void buildDrawer(FrameLayout frame) {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        mDrawerWidth = Math.min((int) (screenW * 0.78f), BulkTheme.dp(this, 286));

        mScrim = new View(this);
        mScrim.setBackgroundColor(0xAD000000);
        mScrim.setAlpha(0f);
        mScrim.setVisibility(View.GONE);
        mScrim.setOnClickListener(v -> closeDrawer());
        frame.addView(mScrim, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout drawerWrap = new FrameLayout(this);
        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(BulkTheme.drawerBg(this));
        drawer.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 18), BulkTheme.dp(this, 14), BulkTheme.dp(this, 16));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP);
        TextView logo = new TextView(this);
        logo.setText(">_");
        logo.setTextSize(14);
        logo.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        logo.setTextColor(BulkTheme.accent(this));
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(BulkTheme.roundedBg(this, BulkTheme.accentSoft(this), 12, BulkTheme.accentStrong(this), 1));
        logo.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 36), BulkTheme.dp(this, 36)));
        header.addView(logo);

        LinearLayout headTexts = new LinearLayout(this);
        headTexts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        htLp.setMargins(BulkTheme.dp(this, 10), 0, BulkTheme.dp(this, 6), 0);
        headTexts.setLayoutParams(htLp);
        TextView dTitle = new TextView(this);
        dTitle.setText(twoToneTitle());
        dTitle.setTextSize(16);
        dTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headTexts.addView(dTitle);
        TextView dDev = new TextView(this);
        dDev.setText("Developer: Mehar Adnan");
        dDev.setTextSize(10.5f);
        dDev.setTypeface(medium());
        dDev.setTextColor(BulkTheme.textSub(this));
        headTexts.addView(dDev);
        TextView dPhone = new TextView(this);
        dPhone.setText("+44 7404 032136");
        dPhone.setTextSize(10);
        dPhone.setTypeface(Typeface.MONOSPACE);
        dPhone.setTextColor(BulkTheme.textMuted(this));
        headTexts.addView(dPhone);
        header.addView(headTexts);
        // v127: FONT option — drawer ke sab se upar icon; tap par font picker
        header.addView(BulkTheme.circleButton(this, R.drawable.ic_bulk_font, 28, v -> showFontPicker()));
        View fontGap = new View(this);
        fontGap.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 6), 1));
        header.addView(fontGap);
        header.addView(BulkTheme.circleButton(this, R.drawable.ic_bulk_close, 28, v -> closeDrawer()));
        drawer.addView(header);

        View hLine = new View(this);
        hLine.setBackground(BulkTheme.hLine(this));
        LinearLayout.LayoutParams hlLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BulkTheme.dp(this, 0.6f)));
        hlLp.setMargins(0, BulkTheme.dp(this, 14), 0, BulkTheme.dp(this, 10));
        hLine.setLayoutParams(hlLp);
        drawer.addView(hLine);

        // Nav rows (design ki tartib)
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.HOME), true, v -> closeDrawer()));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS), false, v -> {
            closeDrawer();
            openScreen(BulkWallpapersActivity.class, "Wallpapers wali screen nahi khul saki.");
        }));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.PROJECTS), false, v -> {
            closeDrawer();
            openScreen(BulkProjectsActivity.class, "Projects screen nahi khul saki.");
        }));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.NEW_PROJECT_ZIP), false, v -> {
            closeDrawer();
            openScreen(BulkAddProjectActivity.class, "Project add wali screen nahi khul saki.");
        }));
        // v127 (Adnan 17:12): Export Project sidebar ki apni row hai
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.EXPORT_PROJECT), false, v -> {
            closeDrawer();
            startExportFlow();
        }));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.PREVIEW_READY), false, v -> {
            closeDrawer();
            openScreen(BulkReadyAssetsActivity.class, "Ready Assets screen nahi khul saki.");
        }));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.OUTPUT_FOLDER), false, v -> {
            closeDrawer();
            openScreen(BulkOutputFolderActivity.class, "Output Folder screen nahi khul saki.");
        }));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.TERMINAL), false, v -> {
            closeDrawer();
            openTerminal();
        }));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.PERMISSIONS), false, v -> {
            closeDrawer();
            openScreen(BulkPermissionsActivity.class, "Permissions screen nahi khul saki.");
        }));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.CHECK_FOR_UPDATES), false, v -> {
            closeDrawer();
            openScreen(BulkUpdateActivity.class, "Update screen nahi khul saki.");
        }));
        drawer.addView(drawerRow(BulkStrings.t(this, BulkStrings.ABOUT), false, v -> {
            closeDrawer();
            BulkTheme.showAboutDialog(this);
        }));
        drawer.addView(contactAdminRow());

        drawer.addView(new View(this), new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // Footer
        View fLine = new View(this);
        fLine.setBackground(BulkTheme.hLine(this));
        LinearLayout.LayoutParams flLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BulkTheme.dp(this, 0.6f)));
        flLp.setMargins(0, 0, 0, BulkTheme.dp(this, 12));
        fLine.setLayoutParams(flLp);
        drawer.addView(fLine);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        TextView version = new TextView(this);
        version.setText("Termux Mod v128");
        version.setTextSize(11);
        version.setTextColor(BulkTheme.textMuted(this));
        footer.addView(version, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView switchTheme = new TextView(this);
        switchTheme.setText(BulkStrings.t(this, BulkStrings.SWITCH_THEME));
        switchTheme.setTextSize(11);
        switchTheme.setTypeface(Typeface.DEFAULT_BOLD);
        switchTheme.setTextColor(BulkTheme.accent(this));
        switchTheme.setBackground(BulkTheme.roundedBg(this, BulkTheme.accentSoft(this), 8, BulkTheme.accentBorder(this), 1));
        switchTheme.setPadding(BulkTheme.dp(this, 10), BulkTheme.dp(this, 4), BulkTheme.dp(this, 10), BulkTheme.dp(this, 4));
        switchTheme.setClickable(true);
        switchTheme.setOnClickListener(v -> {
            BulkTheme.toggle(this);
            buildUi();
        });
        footer.addView(switchTheme);
        drawer.addView(footer);

        drawerWrap.addView(drawer, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View rightBorder = new View(this);
        rightBorder.setBackgroundColor(BulkTheme.accentStrong(this));
        FrameLayout.LayoutParams rbLp = new FrameLayout.LayoutParams(Math.max(1, BulkTheme.dp(this, 0.8f)),
            ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
        drawerWrap.addView(rightBorder, rbLp);

        FrameLayout.LayoutParams dwLp = new FrameLayout.LayoutParams(mDrawerWidth,
            ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT);
        drawerWrap.setLayoutParams(dwLp);
        drawerWrap.setTranslationX(-mDrawerWidth - BulkTheme.dp(this, 16));
        drawerWrap.setVisibility(View.GONE);
        frame.addView(drawerWrap);
        mDrawer = drawerWrap;
    }

    private LinearLayout drawerRow(String label, boolean active, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.drawerRowBg(this, active));
        row.setPadding(BulkTheme.dp(this, 11), BulkTheme.dp(this, 9), BulkTheme.dp(this, 11), BulkTheme.dp(this, 9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 4));
        row.setLayoutParams(lp);
        row.setClickable(true);
        row.setOnClickListener(click);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(13.5f);
        text.setTypeface(medium());
        text.setTextColor(active ? BulkTheme.accent(this) : BulkTheme.textMain(this));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        android.widget.ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            active ? BulkTheme.accent(this) : BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        row.addView(chev);
        return row;
    }

    private void openDrawer() {
        if (mDrawer == null || mScrim == null) return;
        mDrawerOpen = true;
        mScrim.setVisibility(View.VISIBLE);
        mDrawer.setVisibility(View.VISIBLE);
        mScrim.animate().alpha(1f).setDuration(200).start();
        mDrawer.animate().translationX(0).setDuration(260).start();
    }

    private void closeDrawer() {
        if (mDrawer == null || mScrim == null) return;
        mDrawerOpen = false;
        mScrim.animate().alpha(0f).setDuration(200).start();
        mDrawer.animate().translationX(-mDrawerWidth - BulkTheme.dp(this, 16)).setDuration(260)
            .withEndAction(() -> {
                if (!mDrawerOpen) {
                    mDrawer.setVisibility(View.GONE);
                    mScrim.setVisibility(View.GONE);
                }
            }).start();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerOpen) {
            closeDrawer();
            return;
        }
        super.onBackPressed();
    }

    /** Font picker (drawer header ke icon se) — choice tamam Bulk screens par lagu. */
    private void showFontPicker() {
        java.util.List<BulkFonts.FontInfo> fonts = BulkFonts.fonts();
        String[] names = new String[fonts.size()];
        for (int i = 0; i < fonts.size(); i++) names[i] = fonts.get(i).name;
        BulkTheme.showChoice(this, BulkStrings.t(this, BulkStrings.FONT), names, idx -> {
            BulkFonts.setFont(this, fonts.get(idx).id);
            buildUi();
        });
    }

    /**
     * Quick Run popup (Home ki Run row se): search + sirf RUNNABLE
     * projects (jin ke <N>_wallpapers me kam az kam 1 wallpaper ho —
     * BulkProjects.filterRunnable). Upar gear icon = Run Settings
     * (HD + Size + Auto/Manual). Tap par BulkRunHelper run karta hai.
     */
    private void showQuickRun() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.QUICK_RUN_TITLE), dialog);

        // Search + gear row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        final android.widget.EditText search = new android.widget.EditText(this);
        search.setHint(BulkStrings.t(this, BulkStrings.SEARCH_PROJECT));
        search.setSingleLine(true);
        search.setTextSize(13);
        search.setTextColor(BulkTheme.textMain(this));
        search.setHintTextColor(BulkTheme.textMuted(this));
        search.setBackground(BulkTheme.pillBg(this));
        search.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 9),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 9));
        topRow.addView(search, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView gear = new TextView(this);
        gear.setText("⚙");
        gear.setTextSize(17);
        gear.setTextColor(BulkTheme.accent(this));
        gear.setGravity(Gravity.CENTER);
        gear.setBackground(BulkTheme.circleBg(this));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 36), BulkTheme.dp(this, 36));
        gp.leftMargin = BulkTheme.dp(this, 8);
        gear.setLayoutParams(gp);
        gear.setOnClickListener(v -> BulkRunHelper.showRunSettings(this));
        topRow.addView(gear);
        panel.addView(topRow);

        final LinearLayout listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lbp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lbp.topMargin = BulkTheme.dp(this, 10);
        listBox.setLayoutParams(lbp);
        panel.addView(listBox);

        // Runnable projects + counts (ek dafa hisaab)
        final java.util.List<BulkProjects.ProjectInfo> runnable = new java.util.ArrayList<>();
        final java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        try {
            java.util.List<BulkProjects.ProjectInfo> all = BulkProjects.mergeProjects(
                ProjectsLauncher.readProjectsList(), BulkProjects.scanDiskProjects());
            for (BulkProjects.ProjectInfo p : all) {
                if (p.number > 0) {
                    counts.put(p.number, BulkProjects.countFiles(
                        BulkProjects.wallpapersDir(p.number)));
                }
            }
            runnable.addAll(BulkProjects.filterRunnable(all, counts));
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Quick Run list nahi bani: " + e.getMessage());
        }

        final Runnable refreshList = () -> {
            listBox.removeAllViews();
            String q = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase();
            int shown = 0;
            for (final BulkProjects.ProjectInfo p : runnable) {
                if (!q.isEmpty() && !p.name.toLowerCase().contains(q)) continue;
                shown++;
                listBox.addView(quickRunRow(p, counts.get(p.number), () -> {
                    dialog.dismiss();
                    BulkRunHelper.startRun(this, p);
                }));
            }
            if (shown == 0) {
                TextView none = new TextView(this);
                none.setText(BulkStrings.t(this, BulkStrings.NO_MATCHING));
                none.setTextColor(BulkTheme.textMuted(this));
                none.setTextSize(12);
                none.setPadding(0, BulkTheme.dp(this, 8), 0, BulkTheme.dp(this, 4));
                listBox.addView(none);
            }
            BulkFonts.applyToTree(this, listBox);
        };
        refreshList.run();
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { refreshList.run(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        BulkTheme.showPanelDialog(this, panel);
    }

    private LinearLayout quickRunRow(final BulkProjects.ProjectInfo p, Integer wallCount,
                                     final Runnable onTap) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = BulkTheme.dp(this, 8);
        row.setLayoutParams(lp);

        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(13.5f);
        name.setTypeface(medium());
        row.addView(name, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView badge = new TextView(this);
        badge.setText(BulkStrings.format(this, BulkStrings.READY_COUNT,
            wallCount == null ? 0 : wallCount));
        badge.setTextSize(10.5f);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(BulkTheme.onAccent(this));
        badge.setBackground(BulkTheme.roundedBg(this, BulkTheme.accent(this), 8, 0, 0));
        badge.setPadding(BulkTheme.dp(this, 8), BulkTheme.dp(this, 3),
            BulkTheme.dp(this, 8), BulkTheme.dp(this, 3));
        row.addView(badge);

        row.setOnClickListener(v -> { if (onTap != null) onTap.run(); });
        return row;
    }

    /** Contact Admin row (drawer): WhatsApp icon + label + number; tap → WhatsApp. */
    private LinearLayout contactAdminRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.drawerRowBg(this, false));
        row.setPadding(BulkTheme.dp(this, 11), BulkTheme.dp(this, 9), BulkTheme.dp(this, 11), BulkTheme.dp(this, 9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 4));
        row.setLayoutParams(lp);
        row.setClickable(true);

        android.widget.ImageView wa = BulkTheme.icon(this, R.drawable.ic_bulk_whatsapp, 0xFF25D366, 17);
        wa.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 17), BulkTheme.dp(this, 17)));
        row.addView(wa);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tp.leftMargin = BulkTheme.dp(this, 9);
        texts.setLayoutParams(tp);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.CONTACT_ADMIN));
        label.setTextSize(13.5f);
        label.setTypeface(medium());
        label.setTextColor(BulkTheme.textMain(this));
        texts.addView(label);
        TextView number = new TextView(this);
        number.setText("+44 7404 032136");
        number.setTextSize(10.5f);
        number.setTypeface(Typeface.MONOSPACE);
        number.setTextColor(BulkTheme.textMuted(this));
        texts.addView(number);
        row.addView(texts);

        android.widget.ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        row.addView(chev);

        row.setOnClickListener(v -> {
            closeDrawer();
            openWhatsApp();
        });
        return row;
    }

    /** WhatsApp par number kholo; app na ho to browser fallback (wa.me). */
    private void openWhatsApp() {
        android.net.Uri uri = android.net.Uri.parse("https://wa.me/447404032136");
        try {
            Intent wa = new Intent(Intent.ACTION_VIEW, uri);
            wa.setPackage("com.whatsapp");
            startActivity(wa);
            return;
        } catch (Exception ignored) {}
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp nahi khul saka.", Toast.LENGTH_SHORT).show();
        }
    }

    /** Language chooser — English (default) ya Roman Urdu; choice foran lagu. */
    private void showLanguageChooser() {
        BulkTheme.showChoice(this, BulkStrings.t(this, BulkStrings.LANGUAGE),
            new String[]{"English", "Roman Urdu"}, idx -> {
                BulkStrings.setUrdu(this, idx == 1);
                buildUi();
            });
    }

    // ------------------------------------------------------------ helpers

    private SpannableString twoToneTitle() {
        SpannableString s = new SpannableString("Termux Mod");
        s.setSpan(new ForegroundColorSpan(BulkTheme.textMain(this)), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(BulkTheme.accent(this)), 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private SpannableString creditsText() {
        SpannableString s = new SpannableString("DEVELOPER • MEHAR ADNAN");
        s.setSpan(new ForegroundColorSpan(BulkTheme.textSub(this)), 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(BulkTheme.accent(this)), 12, 23, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private static Typeface medium() {
        try {
            return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    private void openScreen(Class<?> cls, String errorToast) {
        try {
            startActivity(new Intent(this, cls));
        } catch (Exception e) {
            Toast.makeText(this, errorToast, Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------ Export Project (sidebar)

    /** Sidebar row se: project chuno → FOLDER copy Download/TermuxMod me (ZIP nahi). */
    private void startExportFlow() {
        final java.util.List<BulkProjects.ProjectInfo> projects = new java.util.ArrayList<>();
        java.util.List<BulkProjects.ProjectInfo> all;
        try {
            all = BulkProjects.mergeProjects(
                ProjectsLauncher.readProjectsList(), BulkProjects.scanDiskProjects());
        } catch (Exception e) {
            all = new java.util.ArrayList<>();
        }
        for (BulkProjects.ProjectInfo p : all) {
            if (p.number > 0) projects.add(p);
        }
        if (projects.isEmpty()) {
            BulkTheme.showToast(this, BulkStrings.t(this, BulkStrings.NO_PROJECTS));
            return;
        }
        String[] names = new String[projects.size()];
        for (int i = 0; i < projects.size(); i++) names[i] = projects.get(i).name;
        BulkTheme.showChoice(this, BulkStrings.t(this, BulkStrings.EXPORT_PROJECT), names,
            idx -> exportProject(projects.get(idx)));
    }

    private void exportProject(final BulkProjects.ProjectInfo p) {
        final java.io.File src = BulkProjects.projectDir(p.number);
        final java.io.File dst = new java.io.File(BulkFileManager.EXPORT_ROOT, src.getName());

        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.EXPORT_PROJECT), dialog);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.EXPORTING) + "  " + p.name);
        label.setTextColor(BulkTheme.textSub(this));
        label.setTextSize(12.5f);
        panel.addView(label);
        final LinearLayout track = BulkTheme.progressTrack(this);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 4));
        tp.topMargin = BulkTheme.dp(this, 10);
        track.setLayoutParams(tp);
        panel.addView(track);
        dialog.setCancelable(false);
        BulkTheme.showPanelDialog(this, panel);

        new Thread(() -> {
            BulkFileManager.deleteRecursive(dst);
            final boolean ok = BulkFileManager.copyFolder(src, dst,
                pct -> runOnUiThread(() -> BulkTheme.setProgress(track, pct)));
            runOnUiThread(() -> {
                try { dialog.dismiss(); } catch (Exception ignored) {}
                if (ok) showExportDone(dst);
                else BulkTheme.showToast(BulkDashboardActivity.this, "Export fail ho gaya.");
            });
        }).start();
    }

    private void showExportDone(final java.io.File dst) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.EXPORT_PROJECT), dialog);
        TextView body = new TextView(this);
        body.setText(BulkStrings.format(this, BulkStrings.EXPORT_DONE, dst.getAbsolutePath()));
        body.setTextColor(BulkTheme.textSub(this));
        body.setTextSize(12.5f);
        panel.addView(body);

        TextView open = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.OPEN_FILE_MANAGER), v -> {
                dialog.dismiss();
                BulkFileManager.openFolder(this, dst);
            });
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        op.topMargin = BulkTheme.dp(this, 14);
        open.setLayoutParams(op);
        panel.addView(open);

        TextView okBtn = BulkTheme.secondaryButton(this, BulkStrings.t(this, BulkStrings.OK),
            v -> dialog.dismiss());
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        kp.topMargin = BulkTheme.dp(this, 8);
        okBtn.setLayoutParams(kp);
        panel.addView(okBtn);

        BulkTheme.showPanelDialog(this, panel);
    }

    private void openTerminal() {
        try {
            startActivity(new Intent(this, TermuxActivity.class));
        } catch (Exception e) {
            Toast.makeText(this, "Terminal nahi khul saka.", Toast.LENGTH_SHORT).show();
        }
    }
}
