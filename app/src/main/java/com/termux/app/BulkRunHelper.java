package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/**
 * Termux Mod (v127) — Run ka app-managed flow (Adnan 2026-11-23):
 *
 *  - AUTO mode (default): koi menu nahi — saved jawab pipe ho jate hain
 *    aur ek LIVE PROCESSING popup dikh kar run terminal me chala jata
 *    hai. Factory jawab: HD = script default, Size = Crop 9:16.
 *  - MANUAL mode: Run par design-style popup khulta hai jis me script ke
 *    verified sawal (Mode + Size) tappable options me ate hain; defaults
 *    pehle se select hote hain (script ke Enter-defaults).
 *  - Jin projects ke prompts verified nahi (number 1..14 se bahar),
 *    wo hamesha plain terminal run karte hain — koi pipe nahi.
 *
 * Jawab BulkRunSettings.answersFor se bante hain aur
 * BulkRunSettings.buildPipedCommand se command me pipe hote hain,
 * taake script terminal me typed jawab ka intezar na kare.
 */
public final class BulkRunHelper {

    private BulkRunHelper() {}

    // ------------------------------------------------------------ entry points

    /** Har Run yahin se guzarta hai (Quick Run, Projects, Stage Run Now). */
    public static void startRun(final Activity a, final BulkProjects.ProjectInfo p) {
        startRun(a, p, null);
    }

    public static void startRun(final Activity a, final BulkProjects.ProjectInfo p,
                                final Runnable onLaunched) {
        if (a == null || p == null || p.workCmd == null) return;
        if (!BulkRunSettings.hasVerifiedPrompts(p.number)) {
            launch(a, p.workCmd, onLaunched);
            return;
        }
        if (BulkRunSettings.isAuto(a)) {
            List<String> answers = BulkRunSettings.answersFor(
                p.number, BulkRunSettings.hdAnswer(a), BulkRunSettings.sizeAnswer(a));
            showProcessingThenLaunch(a, p,
                BulkRunSettings.buildPipedCommand(p.workCmd, answers), onLaunched);
        } else {
            showManualPopup(a, p, onLaunched);
        }
    }

    // ------------------------------------------------------------ MANUAL popup

    private static void showManualPopup(final Activity a, final BulkProjects.ProjectInfo p,
                                        final Runnable onLaunched) {
        final Dialog dialog = new Dialog(a);
        LinearLayout panel = BulkTheme.modalPanel(a, p.name, dialog);

        final boolean[] hdNormal = {BulkRunSettings.ANSWER_NORMAL.equals(BulkRunSettings.hdAnswer(a))};
        final boolean[] sizeCrop = {BulkRunSettings.ANSWER_CROP.equals(BulkRunSettings.sizeAnswer(a))};

        panel.addView(sectionLabel(a, BulkStrings.t(a, BulkStrings.SELECT_MODE)));
        final LinearLayout hdGroup = new LinearLayout(a);
        hdGroup.setOrientation(LinearLayout.VERTICAL);
        panel.addView(hdGroup);
        Runnable refreshHd = () -> {
            hdGroup.removeAllViews();
            hdGroup.addView(optionRow(a,
                BulkStrings.t(a, BulkStrings.HD_OPTION) + "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG),
                !hdNormal[0], () -> { hdNormal[0] = false; refreshHdGroup(hdGroup, a, hdNormal, sizeCrop, true); }));
            hdGroup.addView(optionRow(a, BulkStrings.t(a, BulkStrings.NORMAL_OPTION),
                hdNormal[0], () -> { hdNormal[0] = true; refreshHdGroup(hdGroup, a, hdNormal, sizeCrop, true); }));
        };
        refreshHd.run();

        panel.addView(sectionLabel(a, BulkStrings.t(a, BulkStrings.SELECT_SIZE)));
        final LinearLayout sizeGroup = new LinearLayout(a);
        sizeGroup.setOrientation(LinearLayout.VERTICAL);
        panel.addView(sizeGroup);
        Runnable refreshSize = () -> {
            sizeGroup.removeAllViews();
            sizeGroup.addView(optionRow(a,
                BulkStrings.t(a, BulkStrings.ORIGINAL_OPTION) + "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG),
                !sizeCrop[0], () -> { sizeCrop[0] = false; refreshSizeGroup(sizeGroup, a, hdNormal, sizeCrop); }));
            sizeGroup.addView(optionRow(a, BulkStrings.t(a, BulkStrings.CROP_OPTION),
                sizeCrop[0], () -> { sizeCrop[0] = true; refreshSizeGroup(sizeGroup, a, hdNormal, sizeCrop); }));
        };
        refreshSize.run();

        TextView run = BulkTheme.primaryButton(a, BulkStrings.t(a, BulkStrings.NAV_RUN), v -> {
            dialog.dismiss();
            List<String> answers = BulkRunSettings.answersFor(p.number,
                hdNormal[0] ? BulkRunSettings.ANSWER_NORMAL : BulkRunSettings.ANSWER_HD,
                sizeCrop[0] ? BulkRunSettings.ANSWER_CROP : BulkRunSettings.ANSWER_ORIGINAL);
            showProcessingThenLaunch(a, p,
                BulkRunSettings.buildPipedCommand(p.workCmd, answers), onLaunched);
        });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = BulkTheme.dp(a, 14);
        run.setLayoutParams(rp);
        panel.addView(run);

        BulkTheme.showPanelDialog(a, panel);
    }

    // Option groups ko dobara banana (selection badalne par) — chhote helpers
    private static void refreshHdGroup(LinearLayout group, Activity a,
                                       boolean[] hdNormal, boolean[] sizeCrop, boolean unused) {
        group.removeAllViews();
        group.addView(optionRow(a,
            BulkStrings.t(a, BulkStrings.HD_OPTION) + "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG),
            !hdNormal[0], () -> { hdNormal[0] = false; refreshHdGroup(group, a, hdNormal, sizeCrop, true); }));
        group.addView(optionRow(a, BulkStrings.t(a, BulkStrings.NORMAL_OPTION),
            hdNormal[0], () -> { hdNormal[0] = true; refreshHdGroup(group, a, hdNormal, sizeCrop, true); }));
    }

    private static void refreshSizeGroup(LinearLayout group, Activity a,
                                         boolean[] hdNormal, boolean[] sizeCrop) {
        group.removeAllViews();
        group.addView(optionRow(a,
            BulkStrings.t(a, BulkStrings.ORIGINAL_OPTION) + "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG),
            !sizeCrop[0], () -> { sizeCrop[0] = false; refreshSizeGroup(group, a, hdNormal, sizeCrop); }));
        group.addView(optionRow(a, BulkStrings.t(a, BulkStrings.CROP_OPTION),
            sizeCrop[0], () -> { sizeCrop[0] = true; refreshSizeGroup(group, a, hdNormal, sizeCrop); }));
    }

    // ------------------------------------------------------------ Run Settings popup

    /** Quick Run screen ke gear se khulti hai — global, instant save. */
    public static void showRunSettings(final Activity a) {
        final Dialog dialog = new Dialog(a);
        LinearLayout panel = BulkTheme.modalPanel(a, BulkStrings.t(a, BulkStrings.RUN_SETTINGS), dialog);

        panel.addView(sectionLabel(a, BulkStrings.t(a, BulkStrings.ANSWER_MODE)));
        final LinearLayout modeGroup = new LinearLayout(a);
        modeGroup.setOrientation(LinearLayout.VERTICAL);
        panel.addView(modeGroup);
        Runnable refreshMode = () -> {
            modeGroup.removeAllViews();
            boolean auto = BulkRunSettings.isAuto(a);
            modeGroup.addView(optionRow(a, BulkStrings.t(a, BulkStrings.AUTO_OPTION), auto,
                () -> { BulkRunSettings.setMode(a, true); refreshModeGroup(modeGroup, a); }));
            modeGroup.addView(optionRow(a, BulkStrings.t(a, BulkStrings.MANUAL_OPTION), !auto,
                () -> { BulkRunSettings.setMode(a, false); refreshModeGroup(modeGroup, a); }));
        };
        refreshMode.run();

        panel.addView(sectionLabel(a, BulkStrings.t(a, BulkStrings.SELECT_MODE)));
        final LinearLayout hdGroup = new LinearLayout(a);
        hdGroup.setOrientation(LinearLayout.VERTICAL);
        panel.addView(hdGroup);
        Runnable refreshHd = () -> {
            hdGroup.removeAllViews();
            boolean normal = BulkRunSettings.ANSWER_NORMAL.equals(BulkRunSettings.hdAnswer(a));
            hdGroup.addView(optionRow(a,
                BulkStrings.t(a, BulkStrings.HD_OPTION) + "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG),
                !normal, () -> { BulkRunSettings.setHdAnswer(a, false); refreshSettingsHd(hdGroup, a); }));
            hdGroup.addView(optionRow(a, BulkStrings.t(a, BulkStrings.NORMAL_OPTION), normal,
                () -> { BulkRunSettings.setHdAnswer(a, true); refreshSettingsHd(hdGroup, a); }));
        };
        refreshHd.run();

        panel.addView(sectionLabel(a, BulkStrings.t(a, BulkStrings.SELECT_SIZE)));
        final LinearLayout sizeGroup = new LinearLayout(a);
        sizeGroup.setOrientation(LinearLayout.VERTICAL);
        panel.addView(sizeGroup);
        Runnable refreshSize = () -> {
            sizeGroup.removeAllViews();
            boolean crop = BulkRunSettings.ANSWER_CROP.equals(BulkRunSettings.sizeAnswer(a));
            sizeGroup.addView(optionRow(a,
                BulkStrings.t(a, BulkStrings.ORIGINAL_OPTION) + "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG),
                !crop, () -> { BulkRunSettings.setSizeAnswer(a, false); refreshSettingsSize(sizeGroup, a); }));
            sizeGroup.addView(optionRow(a, BulkStrings.t(a, BulkStrings.CROP_OPTION), crop,
                () -> { BulkRunSettings.setSizeAnswer(a, true); refreshSettingsSize(sizeGroup, a); }));
        };
        refreshSize.run();

        TextView ok = BulkTheme.primaryButton(a, BulkStrings.t(a, BulkStrings.OK), v -> dialog.dismiss());
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        op.topMargin = BulkTheme.dp(a, 14);
        ok.setLayoutParams(op);
        panel.addView(ok);

        BulkTheme.showPanelDialog(a, panel);
    }

    private static void refreshModeGroup(LinearLayout group, Activity a) {
        group.removeAllViews();
        boolean auto = BulkRunSettings.isAuto(a);
        group.addView(optionRow(a, BulkStrings.t(a, BulkStrings.AUTO_OPTION), auto,
            () -> { BulkRunSettings.setMode(a, true); refreshModeGroup(group, a); }));
        group.addView(optionRow(a, BulkStrings.t(a, BulkStrings.MANUAL_OPTION), !auto,
            () -> { BulkRunSettings.setMode(a, false); refreshModeGroup(group, a); }));
    }

    private static void refreshSettingsHd(LinearLayout group, Activity a) {
        group.removeAllViews();
        boolean normal = BulkRunSettings.ANSWER_NORMAL.equals(BulkRunSettings.hdAnswer(a));
        group.addView(optionRow(a,
            BulkStrings.t(a, BulkStrings.HD_OPTION) + "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG),
            !normal, () -> { BulkRunSettings.setHdAnswer(a, false); refreshSettingsHd(group, a); }));
        group.addView(optionRow(a, BulkStrings.t(a, BulkStrings.NORMAL_OPTION), normal,
            () -> { BulkRunSettings.setHdAnswer(a, true); refreshSettingsHd(group, a); }));
    }

    private static void refreshSettingsSize(LinearLayout group, Activity a) {
        group.removeAllViews();
        boolean crop = BulkRunSettings.ANSWER_CROP.equals(BulkRunSettings.sizeAnswer(a));
        group.addView(optionRow(a,
            BulkStrings.t(a, BulkStrings.ORIGINAL_OPTION) + "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG),
            !crop, () -> { BulkRunSettings.setSizeAnswer(a, false); refreshSettingsSize(group, a); }));
        group.addView(optionRow(a, BulkStrings.t(a, BulkStrings.CROP_OPTION), crop,
            () -> { BulkRunSettings.setSizeAnswer(a, true); refreshSettingsSize(group, a); }));
    }

    // ------------------------------------------------------------ processing + launch

    /** Design-style LIVE PROCESSING popup — thori der dikh kar terminal khulta hai. */
    private static void showProcessingThenLaunch(final Activity a, final BulkProjects.ProjectInfo p,
                                                 final String command, final Runnable onLaunched) {
        final Dialog dialog = new Dialog(a);
        dialog.setCancelable(false);
        LinearLayout panel = BulkTheme.modalPanel(a, p.name, dialog);

        LinearLayout center = new LinearLayout(a);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(0, BulkTheme.dp(a, 6), 0, BulkTheme.dp(a, 6));
        ProgressBar bar = new ProgressBar(a);
        bar.setIndeterminate(true);
        center.addView(bar, new LinearLayout.LayoutParams(
            BulkTheme.dp(a, 40), BulkTheme.dp(a, 40)));
        TextView label = new TextView(a);
        label.setText(BulkStrings.t(a, BulkStrings.PROCESSING));
        label.setTextColor(BulkTheme.textSub(a));
        label.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(a, 10);
        label.setLayoutParams(lp);
        center.addView(label);
        panel.addView(center);

        BulkTheme.showPanelDialog(a, panel);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try { dialog.dismiss(); } catch (Exception ignored) {}
            launch(a, command, onLaunched);
        }, 900);
    }

    private static void launch(Activity a, String command, Runnable onLaunched) {
        if (command == null) return;
        try {
            Intent intent = new Intent(a, TermuxActivity.class);
            intent.putExtra(BulkDashboardActivity.EXTRA_RUN_COMMAND, command);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            a.startActivity(intent);
            if (onLaunched != null) onLaunched.run();
        } catch (Exception e) {
            BulkTheme.showToast(a, "Terminal nahi khul saka.");
        }
    }

    // ------------------------------------------------------------ small builders

    private static TextView sectionLabel(Activity a, String text) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextSize(11);
        t.setTextColor(BulkTheme.textMuted(a));
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(a, 10);
        lp.bottomMargin = BulkTheme.dp(a, 6);
        t.setLayoutParams(lp);
        return t;
    }

    /** Radio-style tappable option row (design popup style). */
    private static LinearLayout optionRow(final Activity a, String text, boolean selected,
                                          final Runnable onTap) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(selected ? BulkTheme.secondaryBg(a) : BulkTheme.pillBg(a));
        row.setPadding(BulkTheme.dp(a, 12), BulkTheme.dp(a, 11), BulkTheme.dp(a, 12), BulkTheme.dp(a, 11));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = BulkTheme.dp(a, 8);
        row.setLayoutParams(lp);

        TextView dot = new TextView(a);
        dot.setText(selected ? "●" : "○");
        dot.setTextColor(selected ? BulkTheme.accent(a) : BulkTheme.textMuted(a));
        dot.setTextSize(13);
        row.addView(dot);

        TextView label = new TextView(a);
        label.setText(text);
        label.setTextColor(BulkTheme.textMain(a));
        label.setTextSize(13.5f);
        label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.leftMargin = BulkTheme.dp(a, 9);
        label.setLayoutParams(llp);
        row.addView(label);

        row.setOnClickListener(v -> { if (onTap != null) onTap.run(); });
        return row;
    }
}
