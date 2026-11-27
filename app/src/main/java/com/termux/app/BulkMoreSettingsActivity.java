package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;

/**
 * Termux Mod (v145) — More Settings screen.
 *
 * Sidebar (drawer) lambi ho gayi thi — owner ke kehne par Backup,
 * Export Project aur Output Folder rows drawer se hat kar is apni
 * screen par ah gayi hain. Drawer me in ki jagah ek hi chhoti row
 * "More Settings" hai jo ye screen kholti hai. Teenon options ka
 * behavior bilkul wahi hai jo drawer rows ka tha — sirf jagah
 * badli hai: Backup → wahi Backup popup (BulkMoreActions),
 * Export Project → wahi export flow (BulkMoreActions),
 * Output Folder → wahi Output Folder screen.
 */
public class BulkMoreSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        setContentView(buildUi());
        BulkFonts.apply(this);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this,
            BulkStrings.t(this, BulkStrings.MORE_SETTINGS),
            BulkStrings.t(this, BulkStrings.MORE_SETTINGS_SUB)));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll);

        body.addView(menuRow(BulkStrings.t(this, BulkStrings.BACKUP),
            v -> BulkMoreActions.showBackupPopup(this)));
        body.addView(menuRow(BulkStrings.t(this, BulkStrings.EXPORT_PROJECT),
            v -> BulkMoreActions.startExportFlow(this)));
        body.addView(menuRow(BulkStrings.t(this, BulkStrings.OUTPUT_FOLDER), v -> {
            try {
                startActivity(new Intent(this, BulkOutputFolderActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Output Folder screen nahi khul saki.",
                    Toast.LENGTH_SHORT).show();
            }
        }));
        body.addView(autoScanRow());
        body.addView(waitTimeRow());
        body.addView(timeModeRow());
        body.addView(scanWaqfaRow());
        body.addView(runViewRow());
        body.addView(showpieceEditRow());
        return root;
    }

    /**
     * v152 — Showpiece Edit ki chhoti row: click par chhota
     * editor khulta hai jahan popup showpiece ka NAAM or
     * Loading/Done lines badli ja sakti hain (BulkRunSettings
     * me save; agli dafa panel khulte hi nayi values). Field
     * khaali = design wali default wapas.
     */
    private LinearLayout showpieceEditRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);
        row.setOnClickListener(v -> showShowpieceEditor());

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.SHOWPIECE_EDIT));
        label.setTextSize(14);
        label.setTypeface(medium());
        label.setTextColor(BulkTheme.textMain(this));
        texts.addView(label);
        TextView sub = new TextView(this);
        sub.setText(BulkStrings.t(this, BulkStrings.SHOWPIECE_EDIT_SUB));
        sub.setTextSize(11.5f);
        sub.setTextColor(BulkTheme.textMuted(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = BulkTheme.dp(this, 2);
        sub.setLayoutParams(slp);
        texts.addView(sub);
        row.addView(texts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        android.widget.ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        row.addView(chev);
        return row;
    }

    /** Showpiece editor — chaar fields: naam, number (v153), loading line, done line. */
    private void showShowpieceEditor() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(BulkTheme.glassPanelStrong(this));
        panel.setPadding(BulkTheme.dp(this, 16), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 16), BulkTheme.dp(this, 16));

        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, BulkStrings.SHOWPIECE_EDIT));
        title.setTextSize(15);
        title.setTypeface(medium());
        title.setTextColor(BulkTheme.textMain(this));
        panel.addView(title);

        final android.widget.EditText nameField = showpieceField(panel,
            BulkStrings.t(this, BulkStrings.SHOWPIECE_NAME_LABEL),
            BulkRunSettings.showpieceName(this));
        final android.widget.EditText numberField = showpieceField(panel,
            BulkStrings.t(this, BulkStrings.SHOWPIECE_NUMBER_LABEL),
            BulkRunSettings.showpieceNumber(this));
        final android.widget.EditText loadingField = showpieceField(panel,
            BulkStrings.t(this, BulkStrings.SHOWPIECE_LOADING_LABEL),
            BulkRunSettings.showpieceLoading(this,
                BulkStrings.t(this, BulkStrings.RUNNING_IN_TERMINAL)));
        final android.widget.EditText doneField = showpieceField(panel,
            BulkStrings.t(this, BulkStrings.SHOWPIECE_DONE_LABEL),
            BulkRunSettings.showpieceDone(this,
                BulkStrings.t(this, BulkStrings.RUN_DONE)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = BulkTheme.dp(this, 14);
        row.setLayoutParams(rowLp);
        TextView cancel = BulkTheme.secondaryButton(this,
            BulkStrings.t(this, BulkStrings.CANCEL), v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cLp.rightMargin = BulkTheme.dp(this, 5);
        cancel.setLayoutParams(cLp);
        row.addView(cancel);
        TextView save = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.SAVE), v -> {
                BulkRunSettings.setShowpieceName(this, nameField.getText().toString());
                BulkRunSettings.setShowpieceNumber(this, numberField.getText().toString());
                BulkRunSettings.setShowpieceLoading(this, loadingField.getText().toString());
                BulkRunSettings.setShowpieceDone(this, doneField.getText().toString());
                dialog.dismiss();
            });
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sLp.leftMargin = BulkTheme.dp(this, 5);
        save.setLayoutParams(sLp);
        row.addView(save);
        panel.addView(row);

        dialog.setContentView(panel);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(BulkTheme.dp(this, 320),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        // v155: popup sirf button se band hoga (bahir click / Back nahi)
        BulkTheme.lockDialog(dialog);
        dialog.show();
    }

    /** Editor ka ek label + field jora. */
    private android.widget.EditText showpieceField(LinearLayout panel, String label,
                                                   String value) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(12);
        tv.setTextColor(BulkTheme.textMuted(this));
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.topMargin = BulkTheme.dp(this, 10);
        tv.setLayoutParams(tvLp);
        panel.addView(tv);
        android.widget.EditText et = new android.widget.EditText(this);
        et.setText(value);
        et.setTextSize(13.5f);
        et.setTextColor(BulkTheme.textMain(this));
        et.setSingleLine(true);
        panel.addView(et);
        return et;
    }

    /** Switch wali chhoti row — Run View wali hi shakal, aam helper. */
    private interface SwitchSetter { void set(boolean on); }

    private LinearLayout switchRow(String label, String sub, boolean checked,
                                   final SwitchSetter setter) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextSize(14);
        labelTv.setTypeface(medium());
        labelTv.setTextColor(BulkTheme.textMain(this));
        texts.addView(labelTv);
        TextView subTv = new TextView(this);
        subTv.setText(sub);
        subTv.setTextSize(11.5f);
        subTv.setTextColor(BulkTheme.textMuted(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = BulkTheme.dp(this, 2);
        subTv.setLayoutParams(slp);
        texts.addView(subTv);
        row.addView(texts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        final Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener((button, on) -> setter.set(on));
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        return row;
    }

    /**
     * v151 — Run View ki switch wali chhoti row (Order Fix
     * wali row ki hi shakal). Halat BulkRunSettings ki
     * persisted setting hai (v152: fresh default ON =
     * terminal view; owner ki saved value ki hamesha izzat).
     * Switch sirf VIEW chunta hai: ON ho to Run par terminal
     * samne ata hai, OFF ho to console popup showpiece ban
     * kar dikhta hai — kaam dono haal me asal terminal
     * session hi karta hai.
     */
    private LinearLayout runViewRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.RUN_VIEW));
        label.setTextSize(14);
        label.setTypeface(medium());
        label.setTextColor(BulkTheme.textMain(this));
        texts.addView(label);
        TextView sub = new TextView(this);
        sub.setText(BulkStrings.t(this, BulkStrings.RUN_VIEW_SUB));
        sub.setTextSize(11.5f);
        sub.setTextColor(BulkTheme.textMuted(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = BulkTheme.dp(this, 2);
        sub.setLayoutParams(slp);
        texts.addView(sub);
        row.addView(texts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        final Switch sw = new Switch(this);
        sw.setChecked(BulkRunSettings.isTerminalView(this));
        sw.setOnCheckedChangeListener((button, on) ->
            BulkRunSettings.setTerminalView(this, on));
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        return row;
    }

    /**
     * v149 revision — Scan Waqfa ki switch wali chhoti row
     * (Order Fix wali row ki hi shakal). Halat BulkScanWaqfa ki
     * persisted setting hai (default ON); switch badalte hi
     * TermuxMod/.scan_waqfa marker taaza ho jata hai, taake
     * runners agle run se nayi speed par chalen. OFF = terminal
     * purani tez speed par, order fix app ke zimme.
     */
    private LinearLayout scanWaqfaRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.SCAN_WAQFA));
        label.setTextSize(14);
        label.setTypeface(medium());
        label.setTextColor(BulkTheme.textMain(this));
        texts.addView(label);
        TextView sub = new TextView(this);
        sub.setText(BulkStrings.t(this, BulkStrings.SCAN_WAQFA_SUB));
        sub.setTextSize(11.5f);
        sub.setTextColor(BulkTheme.textMuted(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = BulkTheme.dp(this, 2);
        sub.setLayoutParams(slp);
        texts.addView(sub);
        row.addView(texts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        final Switch sw = new Switch(this);
        sw.setChecked(BulkScanWaqfa.isEnabled(this));
        sw.setOnCheckedChangeListener((button, on) ->
            BulkScanWaqfa.setEnabled(this, on));
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        return row;
    }

    /**
     * v154 — Auto Scan ki option: switch wali chhoti row or USI
     * ke andar (neeche, divider ke baad) ek-dafa wala "Gallery
     * Scan" button. Switch OFF ho to runs ke baad ki automatic
     * registration ruk jati hai; Gallery Scan phir bhi ek dafa
     * ka manual pass chala deta hai. Default ON.
     */
    private LinearLayout autoScanRow() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(BulkTheme.pillBg(this));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        box.setLayoutParams(blp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        top.setClickable(true);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.AUTO_SCAN));
        label.setTextSize(14);
        label.setTypeface(medium());
        label.setTextColor(BulkTheme.textMain(this));
        texts.addView(label);
        TextView sub = new TextView(this);
        sub.setText(BulkStrings.t(this, BulkStrings.AUTO_SCAN_SUB));
        sub.setTextSize(11.5f);
        sub.setTextColor(BulkTheme.textMuted(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = BulkTheme.dp(this, 2);
        sub.setLayoutParams(slp);
        texts.addView(sub);
        top.addView(texts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        final Switch sw = new Switch(this);
        sw.setChecked(BulkMediaRegister.isAutoScanEnabled(this));
        sw.setOnCheckedChangeListener((button, on) ->
            BulkMediaRegister.setAutoScanEnabled(this, on));
        top.addView(sw);
        top.setOnClickListener(v -> sw.toggle());
        box.addView(top);

        View divider = new View(this);
        divider.setBackground(BulkTheme.hLine(this));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1);
        dlp.leftMargin = BulkTheme.dp(this, 14);
        dlp.rightMargin = BulkTheme.dp(this, 14);
        divider.setLayoutParams(dlp);
        box.addView(divider);

        LinearLayout scan = new LinearLayout(this);
        scan.setOrientation(LinearLayout.HORIZONTAL);
        scan.setGravity(Gravity.CENTER_VERTICAL);
        scan.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        scan.setClickable(true);
        LinearLayout scanTexts = new LinearLayout(this);
        scanTexts.setOrientation(LinearLayout.VERTICAL);
        TextView scanLabel = new TextView(this);
        scanLabel.setText(BulkStrings.t(this, BulkStrings.GALLERY_SCAN));
        scanLabel.setTextSize(13.5f);
        scanLabel.setTypeface(medium());
        scanLabel.setTextColor(BulkTheme.textMain(this));
        scanTexts.addView(scanLabel);
        TextView scanSub = new TextView(this);
        scanSub.setText(BulkStrings.t(this, BulkStrings.GALLERY_SCAN_SUB));
        scanSub.setTextSize(11.5f);
        scanSub.setTextColor(BulkTheme.textMuted(this));
        scanTexts.addView(scanSub);
        scan.addView(scanTexts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        android.widget.ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        scan.addView(chev);
        scan.setOnClickListener(v -> {
            Toast.makeText(this, BulkStrings.t(this, BulkStrings.GALLERY_SCAN_RUNNING),
                Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                final int done = BulkDropFolders.galleryScanNow(this);
                runOnUiThread(() -> Toast.makeText(this,
                    String.format(BulkStrings.t(this, BulkStrings.GALLERY_SCAN_DONE_FMT), done),
                    Toast.LENGTH_SHORT).show());
            }).start();
        });
        box.addView(scan);
        return box;
    }

    /**
     * v154 — Wait Time ki chhoti row: mojooda seconds sub me
     * dikhte hain; click par editor (seconds me qeemat) or
     * editor ke AAKHIR me reset icon jo shipped default
     * (0 seconds) wapas kar deta hai.
     */
    private LinearLayout waitTimeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.WAIT_TIME));
        label.setTextSize(14);
        label.setTypeface(medium());
        label.setTextColor(BulkTheme.textMain(this));
        texts.addView(label);
        final TextView sub = new TextView(this);
        sub.setText(String.format(BulkStrings.t(this, BulkStrings.WAIT_TIME_SUB_FMT),
            BulkMediaRegister.waitSeconds(this)));
        sub.setTextSize(11.5f);
        sub.setTextColor(BulkTheme.textMuted(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = BulkTheme.dp(this, 2);
        sub.setLayoutParams(slp);
        texts.addView(sub);
        row.addView(texts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        android.widget.ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        row.addView(chev);
        row.setOnClickListener(v -> showWaitTimeEditor(sub));
        return row;
    }

    /**
     * v155 — Time Mode ki chhoti row: mojooda mode sub me
     * dikhta hai; click par popup — do choices (Past
     * default / Future) or USI popup ke andar Time Reset.
     * Halat BulkTimeMode me save hoti hai or
     * TermuxMod/.time_mode marker se runners tak jati hai.
     */
    private LinearLayout timeModeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.TIME_MODE));
        label.setTextSize(14);
        label.setTypeface(medium());
        label.setTextColor(BulkTheme.textMain(this));
        texts.addView(label);
        final TextView sub = new TextView(this);
        sub.setText(String.format(BulkStrings.t(this, BulkStrings.TIME_MODE_SUB_FMT),
            timeModeLabel(BulkTimeMode.currentMode(this))));
        sub.setTextSize(11.5f);
        sub.setTextColor(BulkTheme.textMuted(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = BulkTheme.dp(this, 2);
        sub.setLayoutParams(slp);
        texts.addView(sub);
        row.addView(texts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        android.widget.ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        row.addView(chev);
        row.setOnClickListener(v -> showTimeModePopup(sub));
        return row;
    }

    /** Mode ki label (dono zubano me mode ka naam waisa hi rehta hai). */
    private String timeModeLabel(String mode) {
        if (BulkTimeMode.MODE_FUTURE.equals(mode)) {
            return BulkStrings.t(this, BulkStrings.TIME_MODE_FUTURE);
        }
        return BulkStrings.t(this, BulkStrings.TIME_MODE_PAST);
    }

    /**
     * Time Mode popup: do radio choices (Past / Future —
     * owner ka faisla, Current mode khatam) + neeche Time
     * Reset ki chhoti row (shared tracker delete + toast).
     * Band sirf button se (showPanelDialog ka lock).
     */
    private void showTimeModePopup(final TextView subView) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.TIME_MODE), dialog);
        final String current = BulkTimeMode.currentMode(this);
        String[] modes = {BulkTimeMode.MODE_PAST, BulkTimeMode.MODE_FUTURE};
        for (final String mode : modes) {
            TextView opt = new TextView(this);
            opt.setText((mode.equals(current) ? "●  " : "○  ") + timeModeLabel(mode));
            opt.setTextSize(14);
            opt.setTypeface(medium());
            opt.setTextColor(mode.equals(current)
                ? BulkTheme.accent(this) : BulkTheme.textMain(this));
            opt.setPadding(BulkTheme.dp(this, 4), BulkTheme.dp(this, 11),
                BulkTheme.dp(this, 4), BulkTheme.dp(this, 11));
            opt.setClickable(true);
            opt.setOnClickListener(v -> {
                BulkTimeMode.setMode(this, mode);
                subView.setText(String.format(
                    BulkStrings.t(this, BulkStrings.TIME_MODE_SUB_FMT),
                    timeModeLabel(mode)));
                dialog.dismiss();
            });
            panel.addView(opt, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        View line = new View(this);
        line.setBackground(BulkTheme.hLine(this));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BulkTheme.dp(this, 0.6f)));
        lineLp.setMargins(0, BulkTheme.dp(this, 8), 0, BulkTheme.dp(this, 4));
        line.setLayoutParams(lineLp);
        panel.addView(line);

        // Time Reset — usi popup ke andar: shared tracker
        // delete, toast, popup band (Wait Time wale reset
        // ki hi surat)
        TextView reset = new TextView(this);
        reset.setText("↺  " + BulkStrings.t(this, BulkStrings.TIME_RESET));
        reset.setTextSize(13.5f);
        reset.setTypeface(medium());
        reset.setTextColor(BulkTheme.danger(this));
        reset.setGravity(Gravity.CENTER);
        reset.setClickable(true);
        reset.setPadding(0, BulkTheme.dp(this, 10), 0, 0);
        panel.addView(reset);
        TextView resetSub = new TextView(this);
        resetSub.setText(BulkStrings.t(this, BulkStrings.TIME_RESET_SUB));
        resetSub.setTextSize(11f);
        resetSub.setTextColor(BulkTheme.textMuted(this));
        resetSub.setGravity(Gravity.CENTER);
        resetSub.setPadding(0, BulkTheme.dp(this, 3), 0, BulkTheme.dp(this, 2));
        panel.addView(resetSub);
        reset.setOnClickListener(v -> new Thread(() -> {
            BulkTimeMode.resetTracker();
            runOnUiThread(() -> {
                Toast.makeText(this, BulkStrings.t(this, BulkStrings.TIME_RESET_DONE),
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }).start());

        BulkTheme.showPanelDialog(this, panel);
    }

    /** Wait Time editor — numeric field (seconds) + neeche reset ki chhoti row. */
    private void showWaitTimeEditor(final TextView subView) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(BulkTheme.glassPanelStrong(this));
        panel.setPadding(BulkTheme.dp(this, 16), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 16), BulkTheme.dp(this, 16));

        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, BulkStrings.WAIT_TIME));
        title.setTextSize(15);
        title.setTypeface(medium());
        title.setTextColor(BulkTheme.textMain(this));
        panel.addView(title);

        final android.widget.EditText field = new android.widget.EditText(this);
        field.setText(String.valueOf(BulkMediaRegister.waitSeconds(this)));
        field.setHint(BulkStrings.t(this, BulkStrings.WAIT_TIME_HINT));
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        field.setSingleLine(true);
        field.setTextSize(13.5f);
        field.setTextColor(BulkTheme.textMain(this));
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fLp.topMargin = BulkTheme.dp(this, 10);
        field.setLayoutParams(fLp);
        panel.addView(field);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = BulkTheme.dp(this, 14);
        row.setLayoutParams(rowLp);
        TextView cancel = BulkTheme.secondaryButton(this,
            BulkStrings.t(this, BulkStrings.CANCEL), v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cLp.rightMargin = BulkTheme.dp(this, 5);
        cancel.setLayoutParams(cLp);
        row.addView(cancel);
        TextView save = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.SAVE), v -> {
                int secs = BulkMediaRegister.DEFAULT_WAIT_SECONDS;
                try {
                    secs = Integer.parseInt(field.getText().toString().trim());
                } catch (Exception ignored) {}
                BulkMediaRegister.setWaitSeconds(this, secs);
                subView.setText(String.format(
                    BulkStrings.t(this, BulkStrings.WAIT_TIME_SUB_FMT),
                    BulkMediaRegister.waitSeconds(this)));
                dialog.dismiss();
            });
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sLp.leftMargin = BulkTheme.dp(this, 5);
        save.setLayoutParams(sLp);
        row.addView(save);
        panel.addView(row);

        // Option ke aakhir me reset ki chhoti row: shipped default wapas
        TextView reset = new TextView(this);
        reset.setText("\u21BA  " + BulkStrings.t(this, BulkStrings.RESET_DEFAULT));
        reset.setTextSize(12.5f);
        reset.setTextColor(BulkTheme.textMuted(this));
        reset.setGravity(Gravity.CENTER);
        reset.setClickable(true);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = BulkTheme.dp(this, 12);
        reset.setLayoutParams(rLp);
        reset.setOnClickListener(v -> {
            BulkMediaRegister.setWaitSeconds(this, BulkMediaRegister.DEFAULT_WAIT_SECONDS);
            subView.setText(String.format(
                BulkStrings.t(this, BulkStrings.WAIT_TIME_SUB_FMT),
                BulkMediaRegister.DEFAULT_WAIT_SECONDS));
            dialog.dismiss();
        });
        panel.addView(reset);

        dialog.setContentView(panel);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(BulkTheme.dp(this, 320),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        // v155: popup sirf button se band hoga (bahir click / Back nahi)
        BulkTheme.lockDialog(dialog);
        dialog.show();
    }

    /** Chhoti si tappable row: label + chevron (owner ka UI rule). */
    private LinearLayout menuRow(String label, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);
        row.setOnClickListener(click);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(14);
        text.setTypeface(medium());
        text.setTextColor(BulkTheme.textMain(this));
        row.addView(text, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        android.widget.ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        row.addView(chev);
        return row;
    }

    private static Typeface medium() {
        try {
            return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }
}
