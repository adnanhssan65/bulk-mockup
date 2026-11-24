package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.termux.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod (v135) — Control Panel.
 *
 * Drawer ki ek chhoti row se khulta hai; khulte hi hamesha panel
 * password (default owner ka tay karda, sirf hash save hota hai —
 * BulkPanelSettings). Andar tamam settings chhoti rows me hain
 * (owner ka UI rule): har row icon + naam + value/chevron, tabdili
 * foran lagti hai or wohi prefs keys istemal hoti hain jo baqi
 * screens pehle se parhti hain — ek hi source of truth.
 *
 * Unlock is process + is panel-visit ke liye hai: activity finish
 * (back) par session flag saaf — agli dafa phir password. Theme /
 * font waghera ke recreate() par dobara nahi poochhta.
 */
public class BulkControlPanelActivity extends Activity {

    private static final int REQ_PICK_LOGO = 13501;

    private static boolean sSessionUnlocked = false;

    private boolean mUnlocked = false;
    private boolean mGateOpen = false;
    private LinearLayout mRoot;
    private Bitmap mPickedLogo;

    // ------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        setTitle(BulkStrings.t(this, BulkStrings.CONTROL_PANEL));
        if (sSessionUnlocked) mUnlocked = true;
        // Gate tak peeche khaali themed background
        View blank = new View(this);
        blank.setBackgroundColor(BulkTheme.appBg(this));
        setContentView(blank);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mUnlocked) {
            showPasswordGate();
        } else if (mRoot == null) {
            buildUi();
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) sSessionUnlocked = false;
        super.onDestroy();
    }

    // ------------------------------------------------------------ password gate

    private void showPasswordGate() {
        if (mGateOpen || isFinishing()) return;
        mGateOpen = true;
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.CONTROL_PANEL), dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        TextView hint = new TextView(this);
        hint.setText(BulkStrings.t(this, BulkStrings.CP_ENTER_PASSWORD));
        hint.setTextSize(12.5f);
        hint.setTextColor(BulkTheme.textSub(this));
        panel.addView(hint);

        final EditText input = passwordField(BulkStrings.t(this, BulkStrings.CP_PASSWORD));
        panel.addView(input);

        final TextView error = errorLabel();
        panel.addView(error);

        TextView unlock = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.CP_UNLOCK), null);
        LinearLayout.LayoutParams unLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        unLp.setMargins(0, dp(14), 0, 0);
        unlock.setLayoutParams(unLp);
        unlock.setOnClickListener(v -> {
            if (BulkPanelSettings.verifyPassword(this, input.getText().toString())) {
                mUnlocked = true;
                sSessionUnlocked = true;
                mGateOpen = false;
                dialog.dismiss();
                buildUi();
            } else {
                error.setText(BulkStrings.t(this, BulkStrings.CP_WRONG_PASSWORD));
                error.setVisibility(View.VISIBLE);
            }
        });
        panel.addView(unlock);

        TextView cancel = BulkTheme.secondaryButton(this,
            BulkStrings.t(this, BulkStrings.CANCEL), v -> {
                mGateOpen = false;
                dialog.dismiss();
                finish();
            });
        LinearLayout.LayoutParams caLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        caLp.setMargins(0, dp(8), 0, 0);
        cancel.setLayoutParams(caLp);
        panel.addView(cancel);

        // X se band ho to bhi gate wapas (unlock tak entry nahi)
        dialog.setOnDismissListener(d -> {
            if (!mUnlocked && !isFinishing()) {
                mGateOpen = false;
                showPasswordGate();
            }
        });
        BulkTheme.showPanelDialog(this, panel);
    }

    // ------------------------------------------------------------ main UI

    private void buildUi() {
        mRoot = new LinearLayout(this);
        mRoot.setOrientation(LinearLayout.VERTICAL);
        mRoot.setBackgroundColor(BulkTheme.appBg(this));

        mRoot.addView(BulkTheme.subHeader(this,
            BulkStrings.t(this, BulkStrings.CONTROL_PANEL),
            BulkPanelSettings.appName(this)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(12), dp(16), dp(20));

        // ---- App section
        body.addView(sectionLabel(BulkStrings.t(this, BulkStrings.CP_APP_SECTION)));
        LinearLayout appCard = card();
        appCard.addView(switchRow(R.drawable.ic_bulk_lock,
            BulkStrings.t(this, BulkStrings.CP_APP_LOCK),
            BulkStrings.t(this, BulkStrings.CP_APP_LOCK_HINT),
            BulkPanelSettings.appLockEnabled(this),
            on -> BulkPanelSettings.setAppLock(this, on)));
        appCard.addView(divider());
        appCard.addView(valueRow(R.drawable.ic_bulk_lock,
            BulkStrings.t(this, BulkStrings.CP_CHANGE_PASSWORD), "",
            v -> showChangePassword()));
        body.addView(appCard);

        // ---- Home section
        body.addView(sectionLabel(BulkStrings.t(this, BulkStrings.CP_HOME_SECTION)));
        LinearLayout homeCard = card();
        homeCard.addView(valueRow(R.drawable.ic_bulk_image,
            BulkStrings.t(this, BulkStrings.CP_APP_LOGO),
            BulkPanelSettings.hasCustomLogo(this)
                ? BulkStrings.t(this, BulkStrings.CP_PICK_IMAGE)
                : BulkStrings.t(this, BulkStrings.DEFAULT_TAG),
            v -> showLogoPopup()));
        homeCard.addView(divider());
        homeCard.addView(valueRow(R.drawable.ic_bulk_monitor,
            BulkStrings.t(this, BulkStrings.CP_APP_NAME),
            BulkPanelSettings.appName(this),
            v -> showTextPopup(BulkStrings.t(this, BulkStrings.CP_APP_NAME),
                BulkPanelSettings.appName(this), false,
                text -> {
                    BulkPanelSettings.setAppName(this, text);
                    refresh();
                })));
        homeCard.addView(divider());
        homeCard.addView(valueRow(R.drawable.ic_bulk_file,
            BulkStrings.t(this, BulkStrings.CP_HOME_SLOGAN),
            BulkPanelSettings.slogan(this),
            v -> showTextPopup(BulkStrings.t(this, BulkStrings.CP_HOME_SLOGAN),
                BulkPanelSettings.slogan(this), false,
                text -> {
                    BulkPanelSettings.setSlogan(this, text);
                    refresh();
                })));
        homeCard.addView(divider());
        int totalRows = BulkPanelSettings.DEFAULT_ORDER.size();
        int visibleCount = BulkPanelSettings.visibleRows(
            BulkPanelSettings.homeOrder(this), BulkPanelSettings.homeHidden(this)).size();
        homeCard.addView(valueRow(R.drawable.ic_bulk_menu,
            BulkStrings.t(this, BulkStrings.CP_HOME_ROWS),
            visibleCount + "/" + totalRows,
            v -> showHomeRowsPopup()));
        homeCard.addView(divider());
        homeCard.addView(valueRow(R.drawable.ic_bulk_phone,
            BulkStrings.t(this, BulkStrings.CP_CONTACT_NUMBER),
            BulkPanelSettings.contactNumber(this),
            v -> showTextPopup(BulkStrings.t(this, BulkStrings.CP_CONTACT_NUMBER),
                BulkPanelSettings.contactNumber(this), false,
                text -> {
                    BulkPanelSettings.setContactNumber(this, text);
                    refresh();
                })));
        body.addView(homeCard);

        // ---- Appearance section
        body.addView(sectionLabel(BulkStrings.t(this, BulkStrings.CP_APPEARANCE_SECTION)));
        LinearLayout lookCard = card();
        lookCard.addView(valueRow(R.drawable.ic_bulk_language,
            BulkStrings.t(this, BulkStrings.LANGUAGE),
            BulkStrings.isUrdu(this) ? "Roman Urdu" : "English",
            v -> BulkTheme.showChoice(this, BulkStrings.t(this, BulkStrings.LANGUAGE),
                new String[]{"English", "Roman Urdu"}, idx -> {
                    BulkStrings.setUrdu(this, idx == 1);
                    recreate();
                })));
        lookCard.addView(divider());
        lookCard.addView(valueRow(R.drawable.ic_bulk_sun,
            BulkStrings.t(this, BulkStrings.THEMES),
            BulkTheme.currentPalette(this).name,
            v -> BulkTheme.showThemesPopup(this)));
        lookCard.addView(divider());
        lookCard.addView(valueRow(R.drawable.ic_bulk_font,
            BulkStrings.t(this, BulkStrings.FONT),
            BulkFonts.byId(BulkFonts.currentId(this)).name,
            v -> showFontPicker()));
        lookCard.addView(divider());
        lookCard.addView(valueRow(R.drawable.ic_bulk_sort,
            BulkStrings.t(this, BulkStrings.CP_FONT_SIZE),
            fontSizeLabel(BulkPanelSettings.fontSizeId(this)),
            v -> showFontSizePicker()));
        lookCard.addView(divider());
        lookCard.addView(accentRow());
        lookCard.addView(divider());
        lookCard.addView(valueRow(R.drawable.ic_bulk_grid,
            BulkStrings.t(this, BulkStrings.CP_LAUNCHER_PRESET),
            launcherLabel(BulkPanelSettings.iconChoice(this)),
            v -> showLauncherPopup()));
        body.addView(lookCard);

        // ---- Run & Folders section
        body.addView(sectionLabel(BulkStrings.t(this, BulkStrings.CP_RUN_SECTION)));
        LinearLayout runCard = card();
        runCard.addView(valueRow(R.drawable.ic_bulk_play,
            BulkStrings.t(this, BulkStrings.RUN_SETTINGS),
            runSummary(),
            v -> BulkRunHelper.showRunSettings(this)));
        runCard.addView(divider());
        runCard.addView(valueRow(R.drawable.ic_bulk_folder,
            BulkStrings.t(this, BulkStrings.RECREATE_FOLDERS),
            recreateSummary(),
            v -> showDropFoldersPopup()));
        runCard.addView(divider());
        runCard.addView(switchRow(R.drawable.ic_bulk_chevron,
            BulkStrings.t(this, BulkStrings.SCROLL_BUTTON),
            "",
            BulkScrollSettings.isShown(this),
            on -> BulkScrollSettings.setShown(this, on)));
        runCard.addView(divider());
        runCard.addView(valueRow(R.drawable.ic_bulk_copy,
            BulkStrings.t(this, BulkStrings.AUTO_DELETE_AFTER),
            ttlLabel(BulkDuplicatesStore.getTtlHours(this)),
            v -> showDuplicatesPicker()));
        body.addView(runCard);

        // ---- More section
        body.addView(sectionLabel(BulkStrings.t(this, BulkStrings.CP_MORE_SECTION)));
        LinearLayout moreCard = card();
        moreCard.addView(valueRow(R.drawable.ic_bulk_upload,
            BulkStrings.t(this, BulkStrings.CP_SETTINGS_BACKUP), "",
            v -> showBackupPopup()));
        moreCard.addView(divider());
        moreCard.addView(valueRow(R.drawable.ic_bulk_trash,
            BulkStrings.t(this, BulkStrings.CP_RESET), "",
            v -> BulkTheme.showConfirm(this,
                BulkStrings.t(this, BulkStrings.CP_RESET),
                BulkStrings.t(this, BulkStrings.CP_RESET_MSG),
                BulkStrings.t(this, BulkStrings.CP_RESET), true,
                () -> {
                    BulkPanelSettings.resetAll(this);
                    applyIconAliases(BulkPanelSettings.iconChoice(this));
                    toast(BulkStrings.t(this, BulkStrings.CP_RESET_DONE));
                    recreate();
                })));
        body.addView(moreCard);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BulkTheme.appBg(this));
        scroll.addView(body);
        mRoot.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(mRoot);
        BulkFonts.apply(this);
    }

    private void refresh() {
        mRoot = null;
        buildUi();
    }

    // ------------------------------------------------------------ row builders

    private int dp(float v) {
        return BulkTheme.dp(this, v);
    }

    private String S(String key) {
        return BulkStrings.t(this, key);
    }

    private LinearLayout sectionLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(11);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(BulkTheme.textMuted(this));
        try { t.setLetterSpacing(0.12f); } catch (Exception ignored) {}
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(2), dp(14), 0, dp(6));
        t.setLayoutParams(lp);
        LinearLayout wrap = new LinearLayout(this);
        wrap.addView(t);
        return wrap;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(BulkTheme.glassPanel(this));
        c.setPadding(dp(6), dp(2), dp(6), dp(2));
        return c;
    }

    private View divider() {
        View line = new View(this);
        line.setBackground(BulkTheme.hLine(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(0.6f)));
        lp.setMargins(dp(10), 0, dp(10), 0);
        line.setLayoutParams(lp);
        return line;
    }

    /** Chhoti row: icon box + naam + value (right) + chevron. */
    private LinearLayout valueRow(int iconRes, String label, String value,
                                  View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(11), dp(8), dp(11));
        row.setClickable(true);
        row.setOnClickListener(click);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackground(BulkTheme.iconBoxBg(this));
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(30)));
        ImageView iv = BulkTheme.icon(this, iconRes, BulkTheme.accent(this), 16);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(16), dp(16)));
        iconBox.addView(iv);
        row.addView(iconBox);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(13.5f);
        text.setTextColor(BulkTheme.textMain(this));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tp.setMargins(dp(10), 0, dp(8), 0);
        text.setLayoutParams(tp);
        row.addView(text);

        if (value != null && !value.isEmpty()) {
            TextView val = new TextView(this);
            val.setText(value);
            val.setTextSize(12);
            val.setTextColor(BulkTheme.textMuted(this));
            val.setSingleLine(true);
            val.setMaxWidth(dp(140));
            val.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(val);
        }

        ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(dp(14), dp(14)));
        LinearLayout.LayoutParams chLp = (LinearLayout.LayoutParams) chev.getLayoutParams();
        chLp.setMargins(dp(6), 0, 0, 0);
        row.addView(chev);
        return row;
    }

    private interface BoolChange {
        void onChanged(boolean on);
    }

    /** Switch wali chhoti row (Permissions wale pattern par). */
    private LinearLayout switchRow(int iconRes, String label, String note,
                                   boolean initial, final BoolChange change) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(9), dp(8), dp(9));

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackground(BulkTheme.iconBoxBg(this));
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(30)));
        ImageView iv = BulkTheme.icon(this, iconRes, BulkTheme.accent(this), 16);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(16), dp(16)));
        iconBox.addView(iv);
        row.addView(iconBox);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tp.setMargins(dp(10), 0, dp(8), 0);
        texts.setLayoutParams(tp);
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(13.5f);
        text.setTextColor(BulkTheme.textMain(this));
        texts.addView(text);
        if (note != null && !note.isEmpty()) {
            TextView n = new TextView(this);
            n.setText(note);
            n.setTextSize(11);
            n.setTextColor(BulkTheme.textMuted(this));
            texts.addView(n);
        }
        row.addView(texts);

        Switch sw = new Switch(this);
        sw.setChecked(initial);
        try {
            android.content.res.ColorStateList tint =
                android.content.res.ColorStateList.valueOf(BulkTheme.accent(this));
            sw.setThumbTintList(tint);
            sw.setTrackTintList(tint);
        } catch (Exception ignored) {}
        sw.setOnCheckedChangeListener((bv, isChecked) -> change.onChanged(isChecked));
        row.addView(sw);
        return row;
    }

    /** Accent row: mojooda rang ka gol nishan + naam. */
    private LinearLayout accentRow() {
        int ov = BulkPanelSettings.accentOverride(this);
        String value = ov == BulkPanelSettings.ACCENT_NONE
            ? S(BulkStrings.CP_ACCENT_THEME)
            : String.format("#%06X", ov & 0xFFFFFF);
        LinearLayout row = valueRow(R.drawable.ic_bulk_moon,
            S(BulkStrings.CP_ACCENT_COLOR), value, v -> showAccentPopup());
        // value text se pehle rang ka dot lagao — row ke children:
        // [iconBox, label, value, chevron] — dot value ke left me
        View dot = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(BulkTheme.accent(this));
        g.setStroke(dp(1), BulkTheme.accentBorder(this));
        dot.setBackground(g);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(14), dp(14));
        row.addView(dot, 2, dlp);
        return row;
    }

    // ------------------------------------------------------------ shared popup helpers

    private EditText textField(String initial, boolean password) {
        EditText e = new EditText(this);
        if (initial != null) e.setText(initial);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setTextColor(BulkTheme.textMain(this));
        e.setHintTextColor(BulkTheme.textMuted(this));
        e.setInputType(password
            ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
            : InputType.TYPE_CLASS_TEXT);
        e.setBackground(BulkTheme.pillBg(this));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        e.setLayoutParams(lp);
        return e;
    }

    private EditText passwordField(String hint) {
        EditText e = textField("", true);
        e.setHint(hint);
        return e;
    }

    private TextView errorLabel() {
        TextView error = new TextView(this);
        error.setTextSize(12);
        error.setTextColor(BulkTheme.danger(this));
        error.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        error.setLayoutParams(lp);
        return error;
    }

    private interface TextDone {
        void onText(String text);
    }

    /** Ek-field wala text popup (naam / slogan / number). */
    private void showTextPopup(String title, String initial, boolean password,
                               final TextDone done) {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, title, dialog);
        final EditText input = textField(initial, password);
        panel.addView(input);
        TextView save = BulkTheme.primaryButton(this, S(BulkStrings.SAVE), v -> {
            dialog.dismiss();
            if (done != null) done.onText(input.getText().toString());
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, dp(14), 0, 0);
        save.setLayoutParams(slp);
        panel.addView(save);
        BulkTheme.showPanelDialog(this, panel);
    }

    private void toast(String msg) {
        BulkTheme.showToast(this, msg);
    }

    // ------------------------------------------------------------ change password

    private void showChangePassword() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, S(BulkStrings.CP_CHANGE_PASSWORD), dialog);
        final EditText current = passwordField(S(BulkStrings.CP_CURRENT_PASSWORD));
        panel.addView(current);
        final EditText next = passwordField(S(BulkStrings.CP_NEW_PASSWORD));
        panel.addView(next);
        final TextView error = errorLabel();
        panel.addView(error);
        TextView save = BulkTheme.primaryButton(this, S(BulkStrings.SAVE), null);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, dp(14), 0, 0);
        save.setLayoutParams(slp);
        save.setOnClickListener(v -> {
            String cur = current.getText().toString();
            String nw = next.getText().toString();
            if (!BulkPanelSettings.verifyPassword(this, cur)) {
                error.setText(S(BulkStrings.CP_PASSWORD_MISMATCH));
                error.setVisibility(View.VISIBLE);
                return;
            }
            if (nw.isEmpty()) {
                error.setText(S(BulkStrings.CP_WRONG_PASSWORD));
                error.setVisibility(View.VISIBLE);
                return;
            }
            String salt = Long.toHexString(System.nanoTime());
            BulkPanelSettings.setPassword(this, salt, nw);
            dialog.dismiss();
            toast(S(BulkStrings.CP_PASSWORD_CHANGED));
        });
        panel.addView(save);
        BulkTheme.showPanelDialog(this, panel);
    }

    // ------------------------------------------------------------ app logo

    private void showLogoPopup() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, S(BulkStrings.CP_APP_LOGO), dialog);

        final ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(BulkTheme.roundedBg(this, BulkTheme.accentSoft(this), 14,
            BulkTheme.accentBorder(this), 1));
        LinearLayout.LayoutParams pvp = new LinearLayout.LayoutParams(dp(120), dp(120));
        pvp.gravity = Gravity.CENTER_HORIZONTAL;
        preview.setLayoutParams(pvp);
        panel.addView(preview);

        final Bitmap source = mPickedLogo != null
            ? mPickedLogo : BulkPanelSettings.loadLogo(this);
        final float[] zoom = {BulkPanelSettings.logoZoom(this)};

        TextView zoomLabel = new TextView(this);
        zoomLabel.setText(S(BulkStrings.CP_LOGO_ZOOM));
        zoomLabel.setTextSize(12);
        zoomLabel.setTextColor(BulkTheme.textSub(this));
        LinearLayout.LayoutParams zlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        zlp.setMargins(0, dp(12), 0, 0);
        zoomLabel.setLayoutParams(zlp);
        panel.addView(zoomLabel);

        SeekBar seek = new SeekBar(this);
        seek.setMax(200);
        seek.setProgress((int) ((zoom[0] - 1f) * 100));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                zoom[0] = 1f + progress / 100f;
                updateLogoPreview(preview, source, zoom[0]);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        panel.addView(seek);
        updateLogoPreview(preview, source, zoom[0]);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(14), 0, 0);
        row.setLayoutParams(rlp);
        TextView pick = BulkTheme.secondaryButton(this, S(BulkStrings.CP_PICK_IMAGE), v -> {
            dialog.dismiss();
            pickLogoImage();
        });
        LinearLayout.LayoutParams pkLp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        pkLp.setMargins(0, 0, dp(5), 0);
        pick.setLayoutParams(pkLp);
        row.addView(pick);
        TextView def = BulkTheme.secondaryButton(this, S(BulkStrings.DEFAULT_TAG), v -> {
            BulkPanelSettings.clearLogo(this);
            mPickedLogo = null;
            dialog.dismiss();
            toast(S(BulkStrings.CP_SAVED));
            refresh();
        });
        LinearLayout.LayoutParams dfLp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        dfLp.setMargins(dp(5), 0, 0, 0);
        def.setLayoutParams(dfLp);
        row.addView(def);
        panel.addView(row);

        if (source != null) {
            TextView save = BulkTheme.primaryButton(this, S(BulkStrings.SAVE), v -> {
                Bitmap cropped = cropLogo(source, zoom[0]);
                if (cropped != null && BulkPanelSettings.saveLogo(this, cropped)) {
                    BulkPanelSettings.setLogoZoom(this, zoom[0]);
                    mPickedLogo = null;
                    toast(S(BulkStrings.CP_SAVED));
                }
                dialog.dismiss();
                refresh();
            });
            LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            svLp.setMargins(0, dp(10), 0, 0);
            save.setLayoutParams(svLp);
            panel.addView(save);
        }
        BulkTheme.showPanelDialog(this, panel);
    }

    private void updateLogoPreview(ImageView preview, Bitmap source, float zoom) {
        if (preview == null) return;
        if (source == null) {
            preview.setImageResource(R.mipmap.ic_launcher_pro);
            return;
        }
        Bitmap cropped = cropLogo(source, zoom);
        if (cropped != null) preview.setImageBitmap(cropped);
    }

    /** Source bitmap se markazi square crop (zoom ke sath), 512px par scale. */
    private static Bitmap cropLogo(Bitmap src, float zoom) {
        try {
            int[] r = BulkPanelSettings.coverCropRect(src.getWidth(), src.getHeight(), zoom);
            if (r[2] <= 0) return null;
            Bitmap sq = Bitmap.createBitmap(src, r[0], r[1], r[2], r[2]);
            if (sq.getWidth() != 512) {
                Bitmap scaled = Bitmap.createScaledBitmap(sq, 512, 512, true);
                if (scaled != sq) sq.recycle();
                return scaled;
            }
            return sq;
        } catch (Exception e) {
            return null;
        }
    }

    private void pickLogoImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_PICK_LOGO);
        } catch (Exception e) {
            toast(S(BulkStrings.CP_IMPORT_FAILED));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_LOGO || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp != null) {
                mPickedLogo = bmp;
                showLogoPopup();
            }
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------ home rows popup

    private void showHomeRowsPopup() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, S(BulkStrings.CP_HOME_ROWS), dialog);
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        panel.addView(list);
        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            list.removeAllViews();
            final List<String> order = BulkPanelSettings.homeOrder(this);
            final Set<String> hidden = BulkPanelSettings.homeHidden(this);
            for (int i = 0; i < order.size(); i++) {
                final int idx = i;
                final String id = order.get(i);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(4), dp(7), dp(4), dp(7));

                TextView label = new TextView(this);
                label.setText(homeRowLabel(id));
                label.setTextSize(13.5f);
                label.setTextColor(BulkTheme.textMain(this));
                row.addView(label, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                TextView up = arrowButton("↑", idx > 0, v -> {
                    BulkPanelSettings.setHomeOrder(this,
                        BulkPanelSettings.moved(BulkPanelSettings.homeOrder(this), idx, -1));
                    rebuild[0].run();
                });
                row.addView(up);
                TextView down = arrowButton("↓", idx < order.size() - 1, v -> {
                    BulkPanelSettings.setHomeOrder(this,
                        BulkPanelSettings.moved(BulkPanelSettings.homeOrder(this), idx, 1));
                    rebuild[0].run();
                });
                row.addView(down);

                Switch sw = new Switch(this);
                sw.setChecked(!hidden.contains(id));
                try {
                    android.content.res.ColorStateList tint =
                        android.content.res.ColorStateList.valueOf(BulkTheme.accent(this));
                    sw.setThumbTintList(tint);
                    sw.setTrackTintList(tint);
                } catch (Exception ignored) {}
                sw.setOnCheckedChangeListener((bv, isChecked) -> {
                    Set<String> h = new LinkedHashSet<>(BulkPanelSettings.homeHidden(this));
                    if (isChecked) h.remove(id); else h.add(id);
                    BulkPanelSettings.setHomeHidden(this, h);
                });
                row.addView(sw);
                list.addView(row);
                if (i < order.size() - 1) list.addView(divider());
            }
        };
        rebuild[0].run();
        BulkTheme.showPanelDialog(this, panel);
    }

    private TextView arrowButton(String text, boolean enabled, View.OnClickListener click) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(enabled ? BulkTheme.accent(this) : BulkTheme.textMuted(this));
        b.setBackground(BulkTheme.circleBg(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(32), dp(32));
        lp.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(lp);
        if (enabled) b.setOnClickListener(click);
        return b;
    }

    private String homeRowLabel(String id) {
        switch (id) {
            case BulkPanelSettings.ROW_RUN: return S(BulkStrings.NAV_RUN);
            case BulkPanelSettings.ROW_PROJECTS: return S(BulkStrings.PROJECTS);
            case BulkPanelSettings.ROW_PREVIEW: return S(BulkStrings.PREVIEW_READY);
            case BulkPanelSettings.ROW_TERMINAL: return S(BulkStrings.TERMINAL);
            case BulkPanelSettings.ROW_ADD:
            default: return S(BulkStrings.ADD_WALLPAPERS);
        }
    }

    // ------------------------------------------------------------ font / size / duplicates pickers

    private void showFontPicker() {
        List<BulkFonts.FontInfo> fonts = BulkFonts.fonts();
        String[] names = new String[fonts.size()];
        for (int i = 0; i < fonts.size(); i++) names[i] = fonts.get(i).name;
        BulkTheme.showChoice(this, S(BulkStrings.FONT), names, idx -> {
            BulkFonts.setFont(this, fonts.get(idx).id);
            recreate();
        });
    }

    private String fontSizeLabel(String id) {
        if (BulkPanelSettings.SIZE_SMALL.equals(id)) return S(BulkStrings.CP_SIZE_SMALL);
        if (BulkPanelSettings.SIZE_LARGE.equals(id)) return S(BulkStrings.CP_SIZE_LARGE);
        if (BulkPanelSettings.SIZE_XLARGE.equals(id)) return S(BulkStrings.CP_SIZE_XLARGE);
        return S(BulkStrings.CP_SIZE_NORMAL);
    }

    private void showFontSizePicker() {
        String[] labels = {
            S(BulkStrings.CP_SIZE_SMALL), S(BulkStrings.CP_SIZE_NORMAL),
            S(BulkStrings.CP_SIZE_LARGE), S(BulkStrings.CP_SIZE_XLARGE)};
        BulkTheme.showChoice(this, S(BulkStrings.CP_FONT_SIZE), labels, idx -> {
            BulkPanelSettings.setFontSizeId(this, BulkPanelSettings.SIZE_IDS.get(idx));
            recreate();
        });
    }

    private static final int[] TTL_VALUES = {6, 12, 24, 48, 72, 168, 0};

    private String ttlLabel(int hours) {
        if (hours == 0) return S(BulkStrings.TTL_NEVER);
        if (hours == 168) return S(BulkStrings.TTL_WEEK);
        return BulkStrings.format(this, BulkStrings.TTL_HOURS, hours);
    }

    private void showDuplicatesPicker() {
        String[] labels = new String[TTL_VALUES.length];
        for (int i = 0; i < TTL_VALUES.length; i++) labels[i] = ttlLabel(TTL_VALUES[i]);
        BulkTheme.showChoice(this, S(BulkStrings.AUTO_DELETE_AFTER), labels, idx -> {
            BulkDuplicatesStore.setTtlHours(this, TTL_VALUES[idx]);
            refresh();
        });
    }

    // ------------------------------------------------------------ accent popup

    private void showAccentPopup() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, S(BulkStrings.CP_ACCENT_COLOR), dialog);
        final int current = BulkPanelSettings.accentOverride(this);

        TextView themeRow = choiceTextRow(S(BulkStrings.CP_ACCENT_THEME),
            current == BulkPanelSettings.ACCENT_NONE, v -> {
                BulkPanelSettings.setAccentOverride(this, BulkPanelSettings.ACCENT_NONE);
                dialog.dismiss();
                recreate();
            });
        panel.addView(themeRow);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        grid.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.setMargins(0, dp(12), 0, 0);
        grid.setLayoutParams(glp);
        for (final int color : BulkPanelSettings.ACCENT_SWATCHES) {
            View dot = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            g.setStroke(dp(color == current ? 3 : 1),
                color == current ? BulkTheme.textMain(this) : BulkTheme.accentBorder(this));
            dot.setBackground(g);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(30), dp(30));
            dlp.setMargins(dp(3), 0, dp(3), 0);
            dot.setLayoutParams(dlp);
            dot.setClickable(true);
            dot.setOnClickListener(v -> {
                BulkPanelSettings.setAccentOverride(this, color);
                dialog.dismiss();
                recreate();
            });
            grid.addView(dot);
        }
        panel.addView(grid);
        BulkTheme.showPanelDialog(this, panel);
    }

    private TextView choiceTextRow(String text, boolean selected, View.OnClickListener click) {
        TextView row = new TextView(this);
        row.setText(selected ? text + "  ✓" : text);
        row.setTextSize(13.5f);
        row.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.setTextColor(selected ? BulkTheme.accent(this) : BulkTheme.textMain(this));
        row.setPadding(dp(4), dp(11), dp(4), dp(11));
        row.setClickable(true);
        row.setOnClickListener(click);
        return row;
    }

    // ------------------------------------------------------------ launcher name & icon

    private String launcherLabel(String id) {
        if (BulkPanelSettings.ICON_TILE.equals(id)) return S(BulkStrings.CP_ICON_TILE);
        if (BulkPanelSettings.ICON_GLYPH.equals(id)) return S(BulkStrings.CP_ICON_GLYPH);
        if (BulkPanelSettings.ICON_LIGHT.equals(id)) return S(BulkStrings.CP_ICON_LIGHT);
        return S(BulkStrings.CP_ICON_PRO);
    }

    private static int launcherIconRes(String id) {
        if (BulkPanelSettings.ICON_TILE.equals(id)) return R.mipmap.ic_launcher;
        if (BulkPanelSettings.ICON_GLYPH.equals(id)) return R.mipmap.ic_launcher_glyph;
        if (BulkPanelSettings.ICON_LIGHT.equals(id)) return R.mipmap.ic_launcher_light;
        return R.mipmap.ic_launcher_pro;
    }

    private void showLauncherPopup() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, S(BulkStrings.CP_LAUNCHER_PRESET), dialog);
        final String current = BulkPanelSettings.iconChoice(this);
        for (final String id : BulkPanelSettings.ICON_IDS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(6), dp(9), dp(6), dp(9));
            row.setClickable(true);

            ImageView iv = new ImageView(this);
            iv.setImageResource(launcherIconRes(id));
            iv.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
            row.addView(iv);

            TextView label = new TextView(this);
            label.setText(launcherLabel(id));
            label.setTextSize(14);
            label.setTextColor(BulkTheme.textMain(this));
            if (id.equals(current)) label.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            llp.setMargins(dp(12), 0, 0, 0);
            label.setLayoutParams(llp);
            row.addView(label);

            if (id.equals(current)) {
                TextView check = new TextView(this);
                check.setText("✓");
                check.setTextSize(15);
                check.setTypeface(Typeface.DEFAULT_BOLD);
                check.setTextColor(BulkTheme.accent(this));
                row.addView(check);
            }
            row.setOnClickListener(v -> {
                BulkPanelSettings.setIconChoice(this, id);
                applyIconAliases(id);
                dialog.dismiss();
                toast(S(BulkStrings.CP_ICON_CHANGED));
                refresh();
            });
            panel.addView(row);
        }
        BulkTheme.showPanelDialog(this, panel);
    }

    /** Chuna hua alias ENABLED, baqi teen DISABLED (label + icon usi ka). */
    public static void applyIconAliases(Activity a, String iconId) {
        try {
            PackageManager pm = a.getPackageManager();
            int chosen = BulkPanelSettings.iconIndex(iconId);
            for (int i = 0; i < BulkPanelSettings.ICON_ALIASES.length; i++) {
                ComponentName comp = new ComponentName(a, BulkPanelSettings.ICON_ALIASES[i]);
                pm.setComponentEnabledSetting(comp,
                    i == chosen
                        ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            }
        } catch (Exception ignored) {}
    }

    private void applyIconAliases(String iconId) {
        applyIconAliases(this, iconId);
    }

    // ------------------------------------------------------------ drop folders popup

    private String recreateSummary() {
        List<String> parts = new ArrayList<>();
        if (BulkRecreateSettings.foldersAuto(this)) parts.add(S(BulkStrings.AUTO_OPTION));
        if (BulkRecreateSettings.videosAuto(this)) parts.add(S(BulkStrings.CP_PREVIEW_VIDEO));
        return parts.isEmpty() ? S(BulkStrings.MANUAL_OPTION) : join(parts);
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(p);
        }
        return sb.toString();
    }

    private void showDropFoldersPopup() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, S(BulkStrings.RECREATE_FOLDERS), dialog);
        panel.addView(switchRow(0,
            S(BulkStrings.RECREATE_FOLDERS_AUTO), "",
            BulkRecreateSettings.foldersAuto(this),
            on -> {
                BulkRecreateSettings.setFoldersAuto(this, on);
                refresh();
            }));
        panel.addView(switchRow(0,
            S(BulkStrings.RECREATE_VIDEOS_AUTO), "",
            BulkRecreateSettings.videosAuto(this),
            on -> {
                BulkRecreateSettings.setVideosAuto(this, on);
                refresh();
            }));
        panel.addView(switchRow(0,
            S(BulkStrings.CP_PREVIEW_VIDEO), "",
            BulkRecreateSettings.videosEnabled(this),
            on -> {
                BulkRecreateSettings.setVideosEnabled(this, on);
                refresh();
            }));
        BulkTheme.showPanelDialog(this, panel);
    }

    // ------------------------------------------------------------ run summary

    private String runSummary() {
        List<String> parts = new ArrayList<>();
        parts.add(BulkRunSettings.isAuto(this)
            ? S(BulkStrings.AUTO_OPTION) : S(BulkStrings.MANUAL_OPTION));
        parts.add(BulkRunSettings.ANSWER_NORMAL.equals(BulkRunSettings.hdAnswer(this))
            ? S(BulkStrings.NORMAL_OPTION) : S(BulkStrings.HD_OPTION));
        parts.add(BulkRunSettings.ANSWER_CROP.equals(BulkRunSettings.sizeAnswer(this))
            ? S(BulkStrings.CROP_OPTION) : S(BulkStrings.ORIGINAL_OPTION));
        return join(parts);
    }

    // ------------------------------------------------------------ settings backup

    private void showBackupPopup() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, S(BulkStrings.CP_SETTINGS_BACKUP), dialog);
        TextView export = BulkTheme.primaryButton(this, S(BulkStrings.CP_EXPORT), v -> {
            dialog.dismiss();
            exportSettings();
        });
        panel.addView(export);
        TextView imp = BulkTheme.secondaryButton(this, S(BulkStrings.CP_IMPORT), v -> {
            dialog.dismiss();
            importSettings();
        });
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.setMargins(0, dp(10), 0, 0);
        imp.setLayoutParams(ilp);
        panel.addView(imp);
        BulkTheme.showPanelDialog(this, panel);
    }

    private File backupFile() {
        return new File(BulkFileManager.EXPORT_ROOT, "settings-backup.json");
    }

    private void exportSettings() {
        new Thread(() -> {
            boolean ok = false;
            try {
                File f = backupFile();
                if (f.getParentFile() != null) f.getParentFile().mkdirs();
                String json = BulkPanelSettings.buildBackup(BulkPanelSettings.exportValues(this));
                try (FileOutputStream out = new FileOutputStream(f)) {
                    out.write(json.getBytes("UTF-8"));
                }
                ok = f.isFile() && f.length() > 0;
            } catch (Exception ignored) {}
            final boolean done = ok;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                toast(done
                    ? BulkStrings.format(this, BulkStrings.CP_EXPORT_DONE,
                        "Download/TermuxMod/settings-backup.json")
                    : S(BulkStrings.CP_IMPORT_FAILED));
            });
        }).start();
    }

    private void importSettings() {
        final File f = backupFile();
        if (!f.isFile()) {
            toast(S(BulkStrings.CP_IMPORT_NOT_FOUND));
            return;
        }
        BulkTheme.showConfirm(this,
            S(BulkStrings.CP_IMPORT_CONFIRM_TITLE),
            S(BulkStrings.CP_IMPORT_CONFIRM_MSG),
            S(BulkStrings.CP_IMPORT), false,
            () -> new Thread(() -> {
                Map<String, String> values = null;
                try {
                    byte[] bytes = new byte[(int) f.length()];
                    try (InputStream in = new FileInputStream(f)) {
                        int off = 0;
                        int r;
                        while (off < bytes.length
                            && (r = in.read(bytes, off, bytes.length - off)) > 0) {
                            off += r;
                        }
                    }
                    values = BulkPanelSettings.parseBackup(new String(bytes, "UTF-8"));
                } catch (Exception ignored) {}
                final Map<String, String> parsed = values;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (parsed == null) {
                        toast(S(BulkStrings.CP_IMPORT_FAILED));
                        return;
                    }
                    BulkPanelSettings.applyValues(this, parsed);
                    applyIconAliases(BulkPanelSettings.iconChoice(this));
                    toast(S(BulkStrings.CP_IMPORT_DONE));
                    recreate();
                });
            }).start());
    }
}
