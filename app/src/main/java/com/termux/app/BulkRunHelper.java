package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.termux.R;
import com.termux.shared.termux.TermuxConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Termux Mod (v127) — Run ka app-managed flow (Adnan 2026-11-23):
 *
 *  - AUTO mode (default): koi menu nahi — saved jawab pipe ho jate hain
 *    aur run LIVE popup ke andar hi chalta hai (v128: terminal samne
 *    NAHI ata; script background process me chalti hai, output popup
 *    me live ata hai, aakhir me Finished/Fail + Close).
 *    Factory jawab: HD = script default, Size = Crop 9:16.
 *  - MANUAL mode: Run par design-style popup khulta hai jis me script ke
 *    verified sawal (Mode + Size) tappable options me ate hain; defaults
 *    pehle se select hote hain (script ke Enter-defaults). Jawab ke baad
 *    wahi live popup wala run — terminal samne nahi ata.
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
            BulkLog.add(BulkStrings.format(a, BulkStrings.LOG_RUN_START, p.name));
            launch(a, p.workCmd, onLaunched);
            return;
        }
        if (BulkRunSettings.isAuto(a)) {
            List<String> answers = BulkRunSettings.answersFor(
                p.number, BulkRunSettings.hdAnswer(a), BulkRunSettings.sizeAnswer(a));
            runLiveInPopup(a, p,
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
            runLiveInPopup(a, p,
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

    /**
     * v128 (DEFECT A fix): run TERMINAL ko samne NAHI lata — script isi
     * live popup ke andar chalti hai. Command asal Termux bash se ek
     * background process me chalti hai (jawab pehle se pipe shuda);
     * khatam hone par saaf Finished/Fail halat. Popup band karne se
     * process NAHI rukta — wo background me chalta rehta hai.
     * onLaunched popup band hone par chalta hai (Stage apni screen
     * finish karti hai; popup usi activity ka hai, is liye pehle popup
     * band, phir finish — ulta karne se popup activity ke sath mar
     * jata).
     *
     * v131 (Item 4 + extension): popup ab PC-console jesa hai — window
     * chrome (3 dots, Home hero card wali) + project naam, bara
     * progress counter (done/total + %) tqdm stream se parse ho kar,
     * us ke neeche asal progress BAR, live elapsed timer (mm:ss) aur
     * monospace log. Progress na mile to counter/bar GONE rehte hain —
     * sirf timer + log (nakli number kabhi nahi).
     *
     * v131 (Item 3 — SPEED): output drain ka usool sakht hai —
     *  - reader thread bara buffer (64KB) ke sath MUSALSAL parhta hai;
     *    kisi line par UI ya mehenga kaam NAHI hota;
     *  - lines batch hoti hain: har ~250ms par ek flush — BulkLog me
     *    sirf asal print lines (tqdm bar lines nahi), tail me amortized
     *    katai (har line par poori string copy NAHI);
     *  - UI updates ek hi throttled poster (500ms) se: counter, bar,
     *    timer, log tail — sab setText, koi per-line layout nahi;
     *  - BulkLog.add ab shared formatter istemal karta hai (BulkLog).
     */
    private static void runLiveInPopup(final Activity a, final BulkProjects.ProjectInfo p,
                                       final String pipedCommand, final Runnable onLaunched) {
        final Dialog dialog = new Dialog(a);
        final ConsoleViews cv = buildConsolePanel(a, p.name, dialog);
        final LinearLayout panel = cv.panel;

        final Dialog shown = BulkTheme.showPanelDialog(a, panel);
        final boolean[] launchedOnce = {false};
        shown.setOnDismissListener(d -> {
            if (onLaunched != null && !launchedOnce[0]) {
                launchedOnce[0] = true;
                onLaunched.run();
            }
        });

        // v129: Home ke live-log card ke liye — run ki shuruaat darj karo
        BulkLog.add(BulkStrings.format(a, BulkStrings.LOG_RUN_START, p.name));

        final BulkRunProgress progress = new BulkRunProgress();
        final StringBuilder tail = new StringBuilder();
        final long startMs = android.os.SystemClock.elapsedRealtime();
        final boolean[] finished = {false};
        final int[] exitCode = {-1};
        sRunActive = true;

        // UI poster — har 500ms: timer/counter/bar/log sirf tab badlo
        // jab asal me badla ho. Run khatam hone par ek aakhri update.
        final android.os.Handler uiHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable[] poster = new Runnable[1];
        poster[0] = () -> {
            if (!shown.isShowing()) return;
            long elapsed = android.os.SystemClock.elapsedRealtime() - startMs;
            String t = BulkRunProgress.formatElapsed(elapsed);
            if (!t.equals(cv.timer.getText().toString())) cv.timer.setText(t);
            if (progress.hasProgress()) {
                if (cv.counterBlock.getVisibility() != View.VISIBLE) {
                    cv.counterBlock.setVisibility(View.VISIBLE);
                }
                String c = progress.done() + " / " + progress.total();
                if (!c.equals(cv.counter.getText().toString())) cv.counter.setText(c);
                String pc = progress.percent() + "%";
                if (!pc.equals(cv.percent.getText().toString())) cv.percent.setText(pc);
                BulkTheme.setProgress(cv.bar, progress.percent());
            }
            final String snap;
            synchronized (tail) { snap = tail.toString(); }
            if (!snap.equals(cv.out.getText().toString())) {
                cv.out.setText(snap);
                cv.scroll.fullScroll(View.FOCUS_DOWN);
            }
            if (!finished[0]) uiHandler.postDelayed(poster[0], 500);
        };
        uiHandler.post(poster[0]);

        new Thread(() -> {
            int exit = -1;
            // Flush ke darmiyan jama honay wali print lines (tqdm bar
            // lines is me NAHI jatin — wo sirf progress banati hain)
            final java.util.List<String> pendingLog = new java.util.ArrayList<>();
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/bash", "-c", pipedCommand);
                pb.environment().putAll(runEnvironment(pb.environment()));
                pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                // Jawab printf pipe se ja rahe hain — apna stdin band rakho
                try { proc.getOutputStream().close(); } catch (Exception ignored) {}
                // v131 (Item 3): bara reader buffer — drain kabhi
                // chhoti chunks ki wajah se na ruke
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), "UTF-8"), 65536);
                String line;
                long lastFlush = System.currentTimeMillis();
                while ((line = reader.readLine()) != null) {
                    // Per-line kaam SIRF: ANSI saaf + progress parse
                    // (halka regex) — koi UI, koi bara string copy nahi
                    boolean wasProgress = progress.offer(line);
                    String clean = BulkRunProgress.stripAnsi(line).trim();
                    if (!wasProgress && !clean.isEmpty()) {
                        synchronized (tail) {
                            tail.append(clean).append('\n');
                            trimTail(tail);
                        }
                        synchronized (pendingLog) { pendingLog.add(clean); }
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastFlush >= 250) {
                        lastFlush = now;
                        flushPendingLog(pendingLog);
                        updateStatusLine(a, p, progress, startMs);
                    }
                }
                exit = proc.waitFor();
            } catch (Exception e) {
                synchronized (tail) {
                    tail.append("\n[").append(String.valueOf(e.getMessage())).append("]\n");
                    trimTail(tail);
                }
                synchronized (pendingLog) {
                    pendingLog.add("[" + String.valueOf(e.getMessage()) + "]");
                }
            }
            flushPendingLog(pendingLog);
            exitCode[0] = exit;
            finished[0] = true;
            sRunActive = false;
            final long totalMs = android.os.SystemClock.elapsedRealtime() - startMs;
            final String totalText = BulkRunProgress.formatElapsed(totalMs);
            // Home bonus (Item 4): final line me progress + kul waqt
            String doneMsg = BulkStrings.format(a, exit == 0
                ? BulkStrings.LOG_RUN_DONE : BulkStrings.LOG_RUN_FAIL, p.name);
            if (progress.hasProgress()) {
                doneMsg += " — " + progress.done() + "/" + progress.total()
                    + " • " + totalText;
            } else {
                doneMsg += " • " + totalText;
            }
            BulkLog.setStatus(null);
            BulkLog.add(doneMsg);
            final int fExit = exit;
            a.runOnUiThread(() -> {
                if (!shown.isShowing()) return;
                cv.spinner.setVisibility(View.GONE);
                cv.status.setText(BulkStrings.t(a, fExit == 0
                    ? BulkStrings.RUN_DONE : BulkStrings.RUN_FAILED));
                cv.timer.setText(totalText);
                if (progress.hasProgress()) {
                    if (cv.counterBlock.getVisibility() != View.VISIBLE) {
                        cv.counterBlock.setVisibility(View.VISIBLE);
                    }
                    cv.counter.setText(progress.done() + " / " + progress.total());
                    cv.percent.setText(progress.percent() + "%");
                    BulkTheme.setProgress(cv.bar, progress.percent());
                }
                final String snap;
                synchronized (tail) { snap = tail.toString(); }
                cv.out.setText(snap);
                cv.scroll.fullScroll(View.FOCUS_DOWN);
            });
        }).start();
    }

    /** Drain ki jama print lines BulkLog ko do (batch — har line alag add, lekin flush throttled hai). */
    private static void flushPendingLog(java.util.List<String> pendingLog) {
        synchronized (pendingLog) {
            for (String s : pendingLog) BulkLog.add(s);
            pendingLog.clear();
        }
    }

    /** Item 4 bonus: Home live-log ki status line — "Project N — 37/100 • 02:15". */
    private static void updateStatusLine(Activity a, BulkProjects.ProjectInfo p,
                                         BulkRunProgress progress, long startMs) {
        if (!progress.hasProgress()) return;
        long elapsed = android.os.SystemClock.elapsedRealtime() - startMs;
        BulkLog.setStatus(p.name + " — " + progress.done() + "/" + progress.total()
            + " • " + BulkRunProgress.formatElapsed(elapsed));
    }

    /**
     * Tail ko ~6000 chars ke andar rakho — lekin katai SIRF tab jab
     * 12000 se barh jaye (amortized): v130 har line par poori string
     * copy karta tha, ab zyada tar lines sirf ek append hain.
     */
    static void trimTail(StringBuilder tail) {
        if (tail.length() > 12000) {
            tail.delete(0, tail.length() - 6000);
        }
    }

    /** v131 (Item 3): koi app-run chal rahi hai? (Home ticker is ka ehtiram karta hai.) */
    private static volatile boolean sRunActive = false;

    public static boolean isRunActive() {
        return sRunActive;
    }

    // ------------------------------------------------------------ console popup (Item 4 ext)

    /** Console popup ke views — poster inhe taaza karta hai. */
    private static final class ConsoleViews {
        LinearLayout panel;
        TextView timer;
        TextView status;
        ProgressBar spinner;
        LinearLayout counterBlock;
        TextView counter;
        TextView percent;
        LinearLayout bar;
        ScrollView scroll;
        TextView out;
    }

    /**
     * PC-console jesi processing screen: dark mode me gehra terminal
     * panel (Home hero card wala gradient + neon border), light mode
     * me saaf safed usi shakal ka panel. Upar window chrome — 3 dots
     * (hero card jese) + project naam + band karne ka X; dayein timer.
     * Neeche: status row, bara counter + % (progress milne par hi),
     * progress bar, monospace log (gehre inset par), Close button.
     */
    private static ConsoleViews buildConsolePanel(final Activity a, String title,
                                                  final Dialog dialog) {
        ConsoleViews cv = new ConsoleViews();
        boolean light = BulkTheme.isLight(a);

        LinearLayout panel = new LinearLayout(a);
        panel.setTag(dialog);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(light ? BulkTheme.glassPanelStrong(a) : BulkTheme.heroCardBg(a));
        panel.setPadding(BulkTheme.dp(a, 16), BulkTheme.dp(a, 14),
            BulkTheme.dp(a, 16), BulkTheme.dp(a, 16));
        cv.panel = panel;

        // Window chrome: 3 dots + title + timer + X
        LinearLayout chrome = new LinearLayout(a);
        chrome.setOrientation(LinearLayout.HORIZONTAL);
        chrome.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < 3; i++) {
            View dot = new View(a);
            android.graphics.drawable.GradientDrawable dotBg =
                new android.graphics.drawable.GradientDrawable();
            dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dotBg.setColor(BulkTheme.textMuted(a));
            dot.setBackground(dotBg);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                BulkTheme.dp(a, 7), BulkTheme.dp(a, 7));
            if (i > 0) dlp.leftMargin = BulkTheme.dp(a, 5);
            dot.setLayoutParams(dlp);
            chrome.addView(dot);
        }
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(13.5f);
        t.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        t.setTextColor(BulkTheme.accent(a));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = BulkTheme.dp(a, 10);
        t.setLayoutParams(tp);
        chrome.addView(t);
        cv.timer = new TextView(a);
        cv.timer.setText("00:00");
        cv.timer.setTextSize(13);
        cv.timer.setTypeface(android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD);
        cv.timer.setTextColor(BulkTheme.textSub(a));
        chrome.addView(cv.timer);
        LinearLayout closeX = BulkTheme.circleButton(a, R.drawable.ic_bulk_close, 28,
            v -> dialog.dismiss());
        LinearLayout.LayoutParams xp = new LinearLayout.LayoutParams(
            BulkTheme.dp(a, 28), BulkTheme.dp(a, 28));
        xp.leftMargin = BulkTheme.dp(a, 8);
        closeX.setLayoutParams(xp);
        chrome.addView(closeX);
        panel.addView(chrome);

        View line = new View(a);
        line.setBackground(BulkTheme.hLine(a));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BulkTheme.dp(a, 0.6f)));
        lineLp.setMargins(0, BulkTheme.dp(a, 10), 0, BulkTheme.dp(a, 12));
        line.setLayoutParams(lineLp);
        panel.addView(line);

        // Status row: spinner + Processing…
        LinearLayout statusRow = new LinearLayout(a);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        cv.spinner = new ProgressBar(a);
        cv.spinner.setIndeterminate(true);
        statusRow.addView(cv.spinner, new LinearLayout.LayoutParams(
            BulkTheme.dp(a, 22), BulkTheme.dp(a, 22)));
        cv.status = new TextView(a);
        cv.status.setText(BulkStrings.t(a, BulkStrings.PROCESSING));
        cv.status.setTextColor(BulkTheme.textSub(a));
        cv.status.setTextSize(12.5f);
        cv.status.setTypeface(android.graphics.Typeface.MONOSPACE);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.leftMargin = BulkTheme.dp(a, 9);
        cv.status.setLayoutParams(slp);
        statusRow.addView(cv.status);
        panel.addView(statusRow);

        // Bara counter block — SIRF jab asal progress parse ho
        cv.counterBlock = new LinearLayout(a);
        cv.counterBlock.setOrientation(LinearLayout.VERTICAL);
        cv.counterBlock.setVisibility(View.GONE);
        LinearLayout counterRow = new LinearLayout(a);
        counterRow.setOrientation(LinearLayout.HORIZONTAL);
        counterRow.setGravity(Gravity.BOTTOM);
        cv.counter = new TextView(a);
        cv.counter.setTextSize(34);
        cv.counter.setTypeface(android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD);
        cv.counter.setTextColor(BulkTheme.textMain(a));
        counterRow.addView(cv.counter);
        cv.percent = new TextView(a);
        cv.percent.setTextSize(17);
        cv.percent.setTypeface(android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD);
        cv.percent.setTextColor(BulkTheme.accent(a));
        LinearLayout.LayoutParams pcp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pcp.leftMargin = BulkTheme.dp(a, 10);
        pcp.bottomMargin = BulkTheme.dp(a, 4);
        cv.percent.setLayoutParams(pcp);
        counterRow.addView(cv.percent);
        cv.counterBlock.addView(counterRow);
        cv.bar = BulkTheme.progressTrack(a);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(a, 8));
        bp.topMargin = BulkTheme.dp(a, 8);
        cv.bar.setLayoutParams(bp);
        cv.counterBlock.addView(cv.bar);
        LinearLayout.LayoutParams cbp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbp.topMargin = BulkTheme.dp(a, 12);
        cv.counterBlock.setLayoutParams(cbp);
        panel.addView(cv.counterBlock);

        // Log — gehra inset, monospace (asal terminal wali feeling)
        cv.scroll = new ScrollView(a);
        cv.scroll.setBackground(BulkTheme.roundedBg(a, BulkTheme.appBg(a), 10,
            BulkTheme.accentBorder(a), 1));
        LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(a, 230));
        scp.topMargin = BulkTheme.dp(a, 12);
        cv.scroll.setLayoutParams(scp);
        cv.out = new TextView(a);
        cv.out.setTypeface(android.graphics.Typeface.MONOSPACE);
        cv.out.setTextSize(11);
        cv.out.setTextColor(BulkTheme.textSub(a));
        cv.out.setPadding(BulkTheme.dp(a, 10), BulkTheme.dp(a, 8),
            BulkTheme.dp(a, 10), BulkTheme.dp(a, 8));
        cv.scroll.addView(cv.out);
        panel.addView(cv.scroll);

        TextView close = BulkTheme.secondaryButton(a, BulkStrings.t(a, BulkStrings.CLOSE),
            v -> dialog.dismiss());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = BulkTheme.dp(a, 14);
        close.setLayoutParams(clp);
        panel.addView(close);
        return cv;
    }

    /** Background run ke liye Termux environment — pure, test ho sakta hai. */
    static Map<String, String> runEnvironment(Map<String, String> base) {
        Map<String, String> env = new HashMap<>();
        if (base != null) env.putAll(base);
        String prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH;
        env.put("PREFIX", prefix);
        env.put("HOME", TermuxConstants.TERMUX_HOME_DIR_PATH);
        String oldPath = env.get("PATH");
        env.put("PATH", prefix + "/bin"
            + (oldPath != null && !oldPath.isEmpty() ? ":" + oldPath : ""));
        env.put("LD_LIBRARY_PATH", prefix + "/lib");
        env.put("TMPDIR", prefix + "/tmp");
        env.put("TERM", "xterm-256color");
        env.put("LANG", "en_US.UTF-8");
        // v131 (Item 3): Python ka stdout pipe par block-buffer na ho —
        // prints foran niklen, koi stdio buffer rukawat na bane
        env.put("PYTHONUNBUFFERED", "1");
        return env;
    }

    /** Output ka sirf aakhri hissa rakho — popup halka rahe. */
    static String keepTail(String text, int maxChars) {
        if (text == null) return "";
        if (maxChars < 0) maxChars = 0;
        return text.length() <= maxChars ? text : text.substring(text.length() - maxChars);
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
