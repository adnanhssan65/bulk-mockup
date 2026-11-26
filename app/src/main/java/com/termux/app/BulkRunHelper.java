package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.shared.termux.TermuxConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 *  - v137: ZIP-added projects (15+) bhi console popup me hi chalte
 *    hain — un ka prompt khandan asal script se pehchana jata hai
 *    (BulkRunSettings.PromptProfile); unknown script par sirf Enter
 *    (script defaults) pipe hote hain. Terminal run ka rasta
 *    KHATAM — koi bhi Run ab TermuxActivity nahi kholta.
 *  - v151 (owner 2026-11-26, AKHRI update): popup wala
 *    ProcessBuilder ENGINE owner ke phone par 25–35s/image
 *    sust nikla, terminal session wala ~1s/image. Ab Run ka
 *    kaam HAR haal me asal terminal session karta hai —
 *    startRun se popup engine kabhi nahi bulta. Run par jo
 *    dikhta hai wo sirf VIEW hai: default maujooda console
 *    popup, lekin DISPLAY-ONLY showpiece (timer + status;
 *    kaam peeche naye terminal session me); More Settings ki
 *    "Run View" switch ON ho to us ki jagah terminal samne
 *    ata hai. Terminal sirf Run tap se — ZIP add par kabhi
 *    nahi. Order Fix ka fresh default bhi OFF. Owner ki
 *    chauthi hidayat par Batch aur Delete bhi terminal
 *    request par hain (Batch button + Delete ka warehouse
 *    option pehle hi hand-off par thay; Delete ka piped
 *    option runCommandInPopup se ata tha — wo bhi ab
 *    dispatchView se guzarta hai). Popup engine
 *    (runLiveInPopup / ProcessBuilder) ab KISI user action
 *    se nahi bulta — code dead ke tor par baqi hai, koi
 *    caller nahi.
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
        if (a == null || p == null) return;
        // v138: command snapshot (p.workCmd) par bharosa NAHI —
        // asal command number + TAAZA script se yahin, run ke waqt
        // banti hai (BulkProjects.effectiveCommand). v137 par P1
        // ka run "w1: command not found" par fail hua tha: snapshot
        // me bare alias reh gayi thi or popup ka bash -c aliases
        // nahi janta. Ab jo command chalti hai wo hamesha poori
        // hoti hai (cd $HOME/... + MOCKUP_BASE + python <script>).
        final String cmd = BulkProjects.effectiveCommand(p, "process");
        if (cmd == null) return;
        // v151: run ka ENGINE hamesha terminal session hai —
        // dispatchRun sirf VIEW chunta hai (default popup
        // showpiece, ya terminal samne). Terminal SIRF is Run
        // tap se khulta hai — ZIP add/import par kabhi nahi
        // (v137 wali ghalti nahi dohrayi jayegi).
        // Prompt profile asal script se banta hai: bundled 1..14
        // verified ADNAN; ZIP-added projects (15+) un ke script
        // khandan (ADNAN / SYSTEM) ke hisab se; unknown script par
        // sirf Enter (script ke apne defaults) pipe hote hain.
        final BulkRunSettings.PromptProfile profile = BulkRunSettings.profileFor(
            p.number, p.number > 0 ? BulkProjects.resolveScript(p.number) : null);
        if (!BulkRunSettings.isAuto(a)
            && profile != BulkRunSettings.PromptProfile.UNKNOWN) {
            showManualPopup(a, p, cmd, onLaunched, profile);
            return;
        }
        List<String> answers = BulkRunSettings.answersForProfile(
            profile, BulkRunSettings.hdAnswer(a), BulkRunSettings.sizeAnswer(a));
        dispatchRun(a, p,
            BulkRunSettings.buildPipedCommand(cmd, answers), onLaunched);
    }

    /**
     * v133: pehle se bani hui piped command chalane ka rasta
     * (Projects screen ka Delete popup isay istemal karta hai —
     * printf se digit pipe hota hai).
     * v151 (owner ki chauthi hidayat): Delete bhi ab asal
     * terminal request se chalta hai — ye entry bhi dispatchView
     * se guzarti hai (engine hamesha terminal session; view
     * wahi jo Run ka hai). Naam purana hai, kaam naya hai:
     * popup engine isay bhi nahi bulta.
     */
    public static void runCommandInPopup(final Activity a, final BulkProjects.ProjectInfo p,
                                         final String pipedCommand) {
        if (a == null || p == null || pipedCommand == null) return;
        dispatchView(a, p, pipedCommand, null);
    }

    // ------------------------------------------------------------ v151: run ka rasta

    /**
     * v151 (owner 2026-11-26): Run ka ENGINE har haal me asal
     * terminal session hai — popup wala ProcessBuilder rasta
     * startRun se kabhi NAHI bulta. Ye dispatch sirf VIEW
     * chunta hai:
     *  - Terminal view (More Settings ki "Run View" switch ON):
     *    pre-v137 wala hand-off — TermuxActivity samne, command
     *    us ke session me type hoti hai.
     *  - Popup view (default): command TermuxService ke zariye
     *    NAYE terminal session me chalti hai (activity samne
     *    nahi ati) aur maujooda console popup sirf SHOWPIECE
     *    ban kar dikhta hai (display-only).
     * Ye dispatch Run (startRun + Manual jawab popup) aur
     * Delete (runCommandInPopup) se bulta hai — koi add/import
     * flow isay NAHI bulata.
     * Na sRunActive guard hai, na post-run mirror thread —
     * mirror + registration Dashboard/Add/Projects ke sync
     * wale hook se waise hi hoti rehti hai.
     */
    private static void dispatchRun(final Activity a, final BulkProjects.ProjectInfo p,
                                    final String pipedCommand, final Runnable onLaunched) {
        BulkLog.add(BulkStrings.format(a, BulkStrings.LOG_RUN_START, p.name));
        dispatchView(a, p, pipedCommand, onLaunched);
    }

    /**
     * Asal dispatch — Run, Batch (Projects ka Batch button
     * pehle hi terminal hand-off par tha) aur Delete teeno ka
     * engine yahin se terminal session banta hai. View ka
     * faisla More Settings ki Run View switch se: ON =
     * terminal samne, OFF (default) = peeche naya session +
     * display-only showpiece panel.
     */
    private static void dispatchView(final Activity a, final BulkProjects.ProjectInfo p,
                                     final String pipedCommand, final Runnable onLaunched) {
        if (BulkRunSettings.isTerminalView(a)) {
            launchInTerminal(a, pipedCommand, onLaunched);
        } else if (executeInTerminalSession(a, pipedCommand)) {
            showRunViewPanel(a, p, onLaunched);
        } else if (onLaunched != null) {
            onLaunched.run();
        }
    }

    /**
     * Terminal view: pre-v137 wala launch rasta — wahi
     * mechanism jo aaj bhi Batch/Delete (BulkProjectsActivity
     * .runInTerminal) istemal karta hai: TermuxActivity ko
     * EXTRA_RUN_COMMAND ke sath kholo; wo command ko session
     * me type kar deti hai (ProjectsLauncher.runCommand).
     */
    private static void launchInTerminal(final Activity a, final String command,
                                         final Runnable onLaunched) {
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

    /**
     * Popup view ka engine: TermuxService ko ACTION_SERVICE
     * _EXECUTE — command NAYE asal terminal session me chalti
     * hai (Runner.TERMINAL_SESSION), session current ban jata
     * hai lekin activity samne NAHI ati (session action
     * SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY). Owner
     * jab chahe panel ke Terminal button se wahi session
     * khol kar live dekh sakta hai.
     */
    private static boolean executeInTerminalSession(final Activity a, final String command) {
        if (command == null) return false;
        try {
            Intent exec = new Intent(TermuxConstants.TERMUX_SERVICE.ACTION_SERVICE_EXECUTE,
                Uri.fromFile(new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/bash")));
            exec.setClass(a, TermuxService.class);
            exec.putExtra(TermuxConstants.TERMUX_SERVICE.EXTRA_ARGUMENTS,
                new String[]{"-c", command});
            exec.putExtra(TermuxConstants.TERMUX_SERVICE.EXTRA_WORKDIR,
                TermuxConstants.TERMUX_HOME_DIR_PATH);
            exec.putExtra(TermuxConstants.TERMUX_SERVICE.EXTRA_SESSION_ACTION,
                String.valueOf(TermuxConstants.TERMUX_SERVICE
                    .VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY));
            a.startService(exec);
            return true;
        } catch (Exception e) {
            BulkTheme.showToast(a, "Terminal nahi khul saka.");
            return false;
        }
    }

    /** Terminal view ko sirf samne lao (koi command nahi — session pehle se chal raha hai). */
    private static void openTerminalView(final Activity a) {
        try {
            Intent intent = new Intent(a, TermuxActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            a.startActivity(intent);
        } catch (Exception e) {
            BulkTheme.showToast(a, "Terminal nahi khul saka.");
        }
    }

    /**
     * Popup view ka SHOWPIECE panel: maujooda console panel hi
     * hai (wahi chrome, wahi status row) — lekin DISPLAY-ONLY:
     *  - status line: "Run terminal me ho raha hai"
     *  - timer asal waqt se chalta hai (sirf display; koi
     *    process, koi output parse NAHI — counter is liye
     *    kabhi nahi dikhta, jhooti progress nahi banti)
     *  - log wala hissa panel se hata diya (wo is panel ka
     *    log hai hi nahi)
     *  - Background/Cancel wali row panel se POORI hata di —
     *    is panel ke pas koi process handle nahi, Cancel
     *    jhoot hota; Close ki jagah Terminal button hai —
     *    wahi chalti hui session khulta hai
     *  - "Finished" kabhi NAHI dikhta: execute intent se
     *    session ka koi handle wapas nahi milta, is liye
     *    "session abhi chal raha hai" ka koi sachcha signal
     *    is class ke pas hai hi nahi — jhooti completion
     *    dikhane se behtar hai panel sirf Running + timer
     *    par rahe. (Isi wajah se double-run guard bhi nahi
     *    lagaya gaya — ghalat guard na-hone se bura hai.)
     *  - X / dismiss se run par ZERO asar (run service ke
     *    session me hai, is dialog se us ka koi lena dena nahi)
     */
    private static void showRunViewPanel(final Activity a, final BulkProjects.ProjectInfo p,
                                         final Runnable onLaunched) {
        final Dialog dialog = new Dialog(a);
        final ConsoleViews cv = buildConsolePanel(a, p.name, dialog,
            () -> dialog.dismiss(), () -> {});
        cv.status.setText(BulkStrings.t(a, BulkStrings.RUNNING_IN_TERMINAL));
        // Background/Cancel wali row is panel se POORI hata do —
        // display-only panel ke pas koi process handle hai hi
        // nahi, Cancel yahan jhoot hota. Log wala hissa bhi
        // hata do (wo is panel ka log hai hi nahi). Sirf X
        // (dismiss) or neeche Terminal button rehta hai.
        if (cv.runActions.getParent() instanceof ViewGroup) {
            ((ViewGroup) cv.runActions.getParent()).removeView(cv.runActions);
        }
        if (cv.scroll.getParent() instanceof ViewGroup) {
            ((ViewGroup) cv.scroll.getParent()).removeView(cv.scroll);
        }
        cv.closeBtn.setVisibility(View.VISIBLE);
        cv.closeBtn.setText(BulkStrings.t(a, BulkStrings.TERMINAL));
        cv.closeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            openTerminalView(a);
        });
        final Dialog shown = BulkTheme.showPanelDialog(a, cv.panel);
        final boolean[] launchedOnce = {false};
        shown.setOnDismissListener(d -> {
            if (onLaunched != null && !launchedOnce[0]) {
                launchedOnce[0] = true;
                onLaunched.run();
            }
        });
        final long startMs = android.os.SystemClock.elapsedRealtime();
        final android.os.Handler uiHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable[] poster = new Runnable[1];
        poster[0] = () -> {
            if (!shown.isShowing()) return;
            cv.timer.setText(BulkRunProgress.formatElapsed(
                android.os.SystemClock.elapsedRealtime() - startMs));
            uiHandler.postDelayed(poster[0], 500);
        };
        uiHandler.post(poster[0]);
    }

    // ------------------------------------------------------------ MANUAL popup

    private static void showManualPopup(final Activity a, final BulkProjects.ProjectInfo p,
                                        final String cmd,
                                        final Runnable onLaunched,
                                        final BulkRunSettings.PromptProfile profile) {
        final Dialog dialog = new Dialog(a);
        LinearLayout panel = BulkTheme.modalPanel(a, p.name, dialog);
        // v137: DEFAULT tag usi option par jo script ka asal default
        // hai (ADNAN: HD; SYSTEM: Normal)
        final boolean hdIsDefault = profile != BulkRunSettings.PromptProfile.SYSTEM;

        final boolean[] hdNormal = {BulkRunSettings.ANSWER_NORMAL.equals(BulkRunSettings.hdAnswer(a))};
        final boolean[] sizeCrop = {BulkRunSettings.ANSWER_CROP.equals(BulkRunSettings.sizeAnswer(a))};

        panel.addView(sectionLabel(a, BulkStrings.t(a, BulkStrings.SELECT_MODE)));
        final LinearLayout hdGroup = new LinearLayout(a);
        hdGroup.setOrientation(LinearLayout.VERTICAL);
        panel.addView(hdGroup);
        Runnable refreshHd = () -> {
            hdGroup.removeAllViews();
            hdGroup.addView(optionRow(a,
                BulkStrings.t(a, BulkStrings.HD_OPTION) + (hdIsDefault ? "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG) : ""),
                !hdNormal[0], () -> { hdNormal[0] = false; refreshHdGroup(hdGroup, a, hdNormal, sizeCrop, hdIsDefault); }));
            hdGroup.addView(optionRow(a, BulkStrings.t(a, BulkStrings.NORMAL_OPTION) + (!hdIsDefault ? "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG) : ""),
                hdNormal[0], () -> { hdNormal[0] = true; refreshHdGroup(hdGroup, a, hdNormal, sizeCrop, hdIsDefault); }));
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
            List<String> answers = BulkRunSettings.answersForProfile(profile,
                hdNormal[0] ? BulkRunSettings.ANSWER_NORMAL : BulkRunSettings.ANSWER_HD,
                sizeCrop[0] ? BulkRunSettings.ANSWER_CROP : BulkRunSettings.ANSWER_ORIGINAL);
            dispatchRun(a, p,
                BulkRunSettings.buildPipedCommand(cmd, answers), onLaunched);
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
                                       boolean[] hdNormal, boolean[] sizeCrop, boolean hdIsDefault) {
        group.removeAllViews();
        group.addView(optionRow(a,
            BulkStrings.t(a, BulkStrings.HD_OPTION) + (hdIsDefault ? "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG) : ""),
            !hdNormal[0], () -> { hdNormal[0] = false; refreshHdGroup(group, a, hdNormal, sizeCrop, hdIsDefault); }));
        group.addView(optionRow(a, BulkStrings.t(a, BulkStrings.NORMAL_OPTION) + (!hdIsDefault ? "  " + BulkStrings.t(a, BulkStrings.DEFAULT_TAG) : ""),
            hdNormal[0], () -> { hdNormal[0] = true; refreshHdGroup(group, a, hdNormal, sizeCrop, hdIsDefault); }));
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
        // v149 Item G: ek waqt me EK hi run. Background me chalta
        // run ho to dusra run shuru NAHI hota — sirf chhota sa
        // pegham. (Is se pehle koi guard nahi tha.)
        if (sRunActive) {
            Toast.makeText(a, BulkStrings.t(a, BulkStrings.RUN_ALREADY_ACTIVE),
                Toast.LENGTH_LONG).show();
            return;
        }

        final Dialog dialog = new Dialog(a);
        // v149 Item G: process handle + cancel halat — worker thread
        // procRef bharta hai, Cancel button use parh kar tree kill
        // karta hai; Background sirf popup band karta hai (run
        // chalti rehti hai — X or purana Close bhi hamesha yahi
        // karte aye hain, process dismiss se kabhi nahi marti).
        final Process[] procRef = {null};
        final boolean[] cancelled = {false};
        final boolean[] finished = {false};
        final Runnable cancelRun = () -> {
            if (finished[0] || cancelled[0]) return;
            cancelled[0] = true;
            Process proc = procRef[0];
            if (proc != null) killProcessTree(proc);
        };
        final ConsoleViews cv = buildConsolePanel(a, p.name, dialog,
            () -> dialog.dismiss(), cancelRun);
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
                // v138: MOCKUP_BASE environment me bhi pakka taake
                // purani /storage-default wali runner copies (owner
                // ki installed P1 / 15–18) bhi app-run me hamesha
                // asal $HOME engine folder par kaam karen — command
                // ke andar wali assignment ke sath belt-and-braces.
                if (p.number > 0) {
                    pb.environment().put("MOCKUP_BASE",
                        BulkProjects.projectDir(p.number).getAbsolutePath());
                }
                pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                procRef[0] = proc;
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
            // Home bonus (Item 4): final line me progress + kul waqt.
            // v149 Item G: cancel par FAIL ki jagah sirf cancel line.
            final int kind = completionKind(cancelled[0], exit);
            String doneMsg = BulkStrings.format(a, kind == 0
                ? BulkStrings.LOG_RUN_CANCEL
                : (kind == 1 ? BulkStrings.LOG_RUN_DONE : BulkStrings.LOG_RUN_FAIL), p.name);
            if (progress.hasProgress()) {
                doneMsg += " — " + progress.done() + "/" + progress.total()
                    + " • " + totalText;
            } else {
                doneMsg += " • " + totalText;
            }
            BulkLog.setStatus(null);
            BulkLog.add(doneMsg);
            // v134: run ke outputs (mockups + ready) ka mirror
            // TermuxMod/Project N ke andar — background thread par,
            // run ki completion is par nahi rukti
            final int mirrorProject = p.number;
            new Thread(() -> BulkDropFolders.syncMirrorForProject(a, mirrorProject)).start();
            final int fExit = exit;
            a.runOnUiThread(() -> {
                if (!shown.isShowing()) return;
                cv.spinner.setVisibility(View.GONE);
                // v149 Item G: run khatam — Background/Cancel wali
                // row chhupao, sirf Close rakho; halat ka lafz bhi
                // cancel ko alag se pehchane
                cv.runActions.setVisibility(View.GONE);
                cv.closeBtn.setVisibility(View.VISIBLE);
                cv.status.setText(BulkStrings.t(a, kind == 0
                    ? BulkStrings.RUN_CANCELLED
                    : (kind == 1 ? BulkStrings.RUN_DONE : BulkStrings.RUN_FAILED)));
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

    // ------------------------------------------------------------ v149 Item G: cancel

    /** Completion ka faisla (pure): 0 = cancel, 1 = done, 2 = fail. */
    static int completionKind(boolean cancelled, int exitCode) {
        if (cancelled) return 0;
        return exitCode == 0 ? 1 : 2;
    }

    /**
     * Process tree ki kill tartib (pure): ppid map (pid → walid
     * pid) se root ke tamam descendants — sab se gehri nasal
     * PEHLE (bachay walidain se pehle maren taake koi dobara na
     * bane). Root khud list me NAHI (use aakhir me alag mara
     * jata hai).
     */
    static List<Integer> treeKillOrder(Map<Integer, Integer> ppidByPid, int rootPid) {
        List<Integer> order = new ArrayList<>();
        if (ppidByPid == null || rootPid <= 0) return order;
        Set<Integer> seen = new HashSet<>();
        seen.add(rootPid);
        List<List<Integer>> levels = new ArrayList<>();
        List<Integer> level = new ArrayList<>();
        level.add(rootPid);
        while (true) {
            List<Integer> next = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : ppidByPid.entrySet()) {
                if (e.getValue() != null && level.contains(e.getValue())
                    && !seen.contains(e.getKey())) {
                    seen.add(e.getKey());
                    next.add(e.getKey());
                }
            }
            if (next.isEmpty()) break;
            levels.add(next);
            level = next;
        }
        for (int i = levels.size() - 1; i >= 0; i--) order.addAll(levels.get(i));
        return order;
    }

    /** Chalti process ka pid (reflection — Android par is ka public API nahi). Na mile to -1. */
    private static int pidOf(Process proc) {
        Class<?> c = proc.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField("pid");
                f.setAccessible(true);
                return f.getInt(proc);
            } catch (Exception e) {
                c = c.getSuperclass();
            }
        }
        return -1;
    }

    /** /proc se ppid map (pid → walid pid); comm ke andar ke spaces/parens se mehfooz parse. */
    private static Map<Integer, Integer> readPpidMap() {
        Map<Integer, Integer> out = new HashMap<>();
        File[] dirs = new File("/proc").listFiles();
        if (dirs == null) return out;
        for (File d : dirs) {
            int pid;
            try { pid = Integer.parseInt(d.getName()); }
            catch (Exception e) { continue; }
            try {
                String stat = BulkProjectInstaller.readText(new File(d, "stat"));
                int close = stat.lastIndexOf(')');
                if (close < 0) continue;
                String[] parts = stat.substring(close + 2).trim().split("\\s+");
                // ')' ke baad ke hisse: state, ppid, pgrp, ...
                out.put(pid, Integer.parseInt(parts[1]));
            } catch (Exception ignored) {}
        }
        return out;
    }

    /**
     * Run ko saaf band karo: poora process tree (bash + python +
     * us ke tamam bachay) SIGTERM se, phir destroy() backstop.
     * Jo outputs ban chuke hain wo rehte hain — rollback koi
     * nahi; consumed inputs wapas nahi ate.
     */
    static void killProcessTree(Process proc) {
        if (proc == null) return;
        try {
            int pid = pidOf(proc);
            if (pid > 0) {
                for (int child : treeKillOrder(readPpidMap(), pid)) {
                    try { android.os.Process.sendSignal(child, 15); }
                    catch (Exception ignored) {}
                }
                try { android.os.Process.sendSignal(pid, 15); }
                catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try { proc.destroy(); } catch (Exception ignored) {}
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
        // v149 Item G: run ke dauran Background/Cancel wali row;
        // run khatam hone par ye chhupati hai or Close nazar ata hai
        LinearLayout runActions;
        TextView closeBtn;
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
                                                  final Dialog dialog,
                                                  final Runnable onBackground,
                                                  final Runnable onCancel) {
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

        // v149 Item G: run ke dauran neeche DO button — "Background
        // me chalao" (popup band, run chalti rehti hai; X bhi yahi
        // karta hai) or "Cancel" (poora process tree saaf band).
        // Run khatam hone par ye row chhup jati hai or sirf Close
        // rehta hai (completion update runLiveInPopup me hai).
        LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = BulkTheme.dp(a, 14);
        actions.setLayoutParams(alp);
        TextView bgBtn = BulkTheme.secondaryButton(a,
            BulkStrings.t(a, BulkStrings.RUN_BACKGROUND),
            v -> { if (onBackground != null) onBackground.run(); });
        LinearLayout.LayoutParams bgp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bgp.rightMargin = BulkTheme.dp(a, 5);
        bgBtn.setLayoutParams(bgp);
        actions.addView(bgBtn);
        TextView cancelBtn = BulkTheme.secondaryButton(a,
            BulkStrings.t(a, BulkStrings.CANCEL),
            v -> { if (onCancel != null) onCancel.run(); });
        LinearLayout.LayoutParams cnp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cnp.leftMargin = BulkTheme.dp(a, 5);
        cancelBtn.setLayoutParams(cnp);
        actions.addView(cancelBtn);
        panel.addView(actions);
        cv.runActions = actions;

        TextView close = BulkTheme.secondaryButton(a, BulkStrings.t(a, BulkStrings.CLOSE),
            v -> dialog.dismiss());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = BulkTheme.dp(a, 14);
        close.setLayoutParams(clp);
        close.setVisibility(View.GONE);
        panel.addView(close);
        cv.closeBtn = close;
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

    // v151: terminal launch wala rasta (launchInTerminal) WAPAS
    // hai — upar "v151: run ka rasta" wale hisse me. v137 me ye
    // is liye hataya gaya tha ke ZIP add par bhi app terminal me
    // kood jati thi; ab ye sirf startRun ke dispatch se bulta
    // hai, add/import flow se kabhi nahi.

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
        // v136: compact radio rows (gallery settings wali hi height family)
        row.setPadding(BulkTheme.dp(a, 12), BulkTheme.dp(a, 7), BulkTheme.dp(a, 12), BulkTheme.dp(a, 7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = BulkTheme.dp(a, 6);
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
