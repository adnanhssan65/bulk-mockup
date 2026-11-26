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
import com.termux.shared.shell.ShellUtils;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;

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

    /** v151: Projects ka Batch button bhi yahin se — Run wala hi fetch + terminal dispatch. */
    public static void startBatch(final Activity a, final BulkProjects.ProjectInfo p) {
        if (a == null || p == null) return;
        final String cmd = BulkProjects.effectiveCommand(p, "batch");
        if (cmd == null) return;
        dispatchView(a, p, cmd, null);
    }

    /**
     * Asal dispatch — Run, Batch aur Delete teeno ka engine
     * yahin se terminal session banta hai. View ka faisla
     * More Settings ki Run View switch se: ON = terminal
     * samne, OFF (default) = peeche naya session +
     * display-only showpiece panel.
     *
     * v151 (owner ki panchvi report — ASAL defect): execution
     * se PEHLE drop folders ka sync (BulkDropFolders.syncNow,
     * background thread par — Dashboard ki tarah). Wajah:
     * app se move/copy ki hui images pehle TermuxMod/Project
     * N (shared drop folder) me parti hain, jabke har run ka
     * input SIRF engine folder ($HOME/mockup_project_N/
     * N_wallpapers) hai — dono ke darmiyan ka pul yehi sync
     * hai, or ye sirf app ki screens khulne par chalta tha.
     * Popup run ko is liye images mil jati theen (owner Run
     * se pehle Dashboard/Projects dekh chuka hota tha);
     * terminal par hath se chalaye run ko sync kabhi nahi
     * milta tha — input khaali, "image hai hi nahi". Ab har
     * terminal dispatch pehle wahi sync karta hai, phir
     * command chalti hai — command waisa ka waisa hai
     * (fullCommand: cd engine folder + MOCKUP_BASE, yani
     * bilkul wahi input jo staging/fetch bharta hai).
     */
    private static void dispatchView(final Activity a, final BulkProjects.ProjectInfo p,
                                     final String pipedCommand, final Runnable onLaunched) {
        new Thread(() -> {
            try { BulkDropFolders.syncNow(a); } catch (Exception ignored) {}
            a.runOnUiThread(() -> {
                if (a.isFinishing()) {
                    if (onLaunched != null) onLaunched.run();
                    return;
                }
                if (BulkRunSettings.isTerminalView(a)) {
                    launchInTerminal(a, pipedCommand, onLaunched);
                } else if (executeInTerminalSession(a, pipedCommand)) {
                    showRunViewPanel(a, p, pipedCommand, onLaunched);
                } else if (onLaunched != null) {
                    onLaunched.run();
                }
            });
        }).start();
    }

    /**
     * v152: app-launched run sessions ka naam — isi nishan se
     * TermuxTerminalSessionActivityClient pehchanta hai ke ye
     * session app ke Run/Batch/Delete se aya hai (auto-return
     * sirf inhi par lagu hai; hath se khole gaye sessions ka
     * naam hamesha mukhtalif hota hai, unhe kabhi hath nahi
     * lagta).
     */
    public static final String RUN_SESSION_NAME_PREFIX = "BulkRun-";

    /** Is run ke liye khaas session naam (pure). */
    public static String runSessionName(long nowMs) {
        return RUN_SESSION_NAME_PREFIX + nowMs;
    }

    /** Ye session app-launched run ka hai? (pure) */
    public static boolean isRunSessionName(String name) {
        return name != null && name.startsWith(RUN_SESSION_NAME_PREFIX);
    }

    /**
     * Terminal view (v152 — owner ka aakhri faisla: sab kaam
     * terminal par hi): command ek NAYE, nishan-zada asal
     * terminal session me chalti hai or TermuxActivity KHUD
     * samne ati hai (session action SWITCH_TO_NEW_SESSION_AND
     * _OPEN_ACTIVITY — service activity kholti hai, NEW_TASK
     * flag ke sath; Bulk wali screen apni task me neeche
     * mehfooz rehti hai). Session ka process bash -c khud hai
     * — koi type karna, koi Enter nahi; view attach hote hi
     * (or TermuxService ke v152 initialize par) process chalta
     * hai, bilkul hath se chalaye terminal run ki speed par.
     * Run khatam (exit 0) hone par activity client wahi
     * session pehchan kar activity band kar deta hai —
     * owner khud-ba-khud usi Bulk screen par wapas.
     * (pre-v137 wala typed EXTRA_RUN_COMMAND rasta ab sirf
     * drawer/BulkProjectsActivity ke hath-wale commands ke
     * liye rehta hai — wahan koi auto-return nahi.)
     */
    private static void launchInTerminal(final Activity a, final String command,
                                         final Runnable onLaunched) {
        if (command == null) return;
        try {
            Intent exec = new Intent(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_SERVICE_EXECUTE,
                Uri.fromFile(new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/bash")));
            exec.setClass(a, TermuxService.class);
            exec.putExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_ARGUMENTS,
                new String[]{"-c", command});
            exec.putExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_WORKDIR,
                TermuxConstants.TERMUX_HOME_DIR_PATH);
            exec.putExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_SHELL_NAME,
                runSessionName(System.currentTimeMillis()));
            exec.putExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_SESSION_ACTION,
                String.valueOf(TermuxConstants.TERMUX_APP.TERMUX_SERVICE
                    .VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY));
            // v153: is khulne ka nishan — TermuxActivity ka
            // cosmetic fade-in sirf isi launch par lage ga
            noteTerminalLaunch(System.currentTimeMillis());
            a.startService(exec);
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
            Intent exec = new Intent(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_SERVICE_EXECUTE,
                Uri.fromFile(new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/bash")));
            exec.setClass(a, TermuxService.class);
            exec.putExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_ARGUMENTS,
                new String[]{"-c", command});
            exec.putExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_WORKDIR,
                TermuxConstants.TERMUX_HOME_DIR_PATH);
            exec.putExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_SESSION_ACTION,
                String.valueOf(TermuxConstants.TERMUX_APP.TERMUX_SERVICE
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
     *  - text area me session ki ASAL screen ka snapshot
     *    dikhta hai (v152): har tick emulator ke screen ka
     *    text (TerminalBuffer) plain text me — na rang, na
     *    cursor; ye session ka sachcha manzar hai, koi
     *    parse/mirror nahi. TerminalView panel me NAHI
     *    lagayi gayi — redraw session ke ek hi
     *    TerminalSessionClient slot se milta hai jo service/
     *    activity ke pas hai; dialog view use cheen kar
     *    full-terminal hand-off khatre me dalta.
     *  - v153 (owner-approved naya design): header saaf —
     *    dots + "Project N" + LIVE badge + timer pill + X;
     *    header ke neeche brand line NAHI (naam ab footer ki
     *    chhoti line "✿ <name> · <number> ✿" me hai);
     *    status row me spinner + chhoti loading line + dayein
     *    taraf transcript ki AAKHRI asal ginti se bari
     *    numbers ("5 / 8") or us ke neeche patli bar — ginti
     *    na mile to dono ghayab (nakli number kabhi nahi);
     *    chhoti screen me sirf aakhri ~4 asal lines or
     *    "[Process completed - press Enter]" wali line panel
     *    me KABHI nahi (sirf display filter); refresh ~200ms.
     *  - v153 buttons: Terminal button KHATAM — neeche
     *    Cancel (ASAL: session ka poora process tree group
     *    SIGKILL se marta hai — killSessionTree), Background
     *    or Close (dono sirf panel band karte hain; run
     *    service ke session me chalta rehta hai).
     *  - "Finished" (RUN_DONE) SIRF sachche signal par
     *    dikhta hai (v152, owner ki hidayat): panel har tick
     *    TermuxService ki session list (TermuxShellManager)
     *    dekhta hai — hamara session wahan naya banta hai or
     *    khatam hone par service use list se hata deti hai
     *    (onTermuxSessionExited); process rukne par
     *    TerminalSession.isRunning() bhi false ho jata hai.
     *    In me se koi signal na mile to panel hamesha
     *    Running par rehta hai — jhoota Done kabhi nahi.
     *    (Double-run guard phir bhi nahi lagaya gaya —
     *    ghalat guard na-hone se bura hai.)
     *  - v153 AUTO-OFF: exit status asal me 0 ho to Finished
     *    ~1.2s dikh kar panel KHUD band ho jata hai; ghalti
     *    (exit != 0 ya status na mile) par panel KHULA rehta
     *    hai — koi auto-off nahi, lines parhi ja sakti hain.
     *  - X / Background / Close se run par ZERO asar (run
     *    service ke session me hai, is dialog se us ka koi
     *    lena dena nahi) — sirf Cancel use rokta hai.
     */
    private static void showRunViewPanel(final Activity a, final BulkProjects.ProjectInfo p,
                                         final String pipedCommand,
                                         final Runnable onLaunched) {
        final Dialog dialog = new Dialog(a);
        final ConsoleViews cv = buildConsolePanel(a, p.name, dialog,
            () -> dialog.dismiss(), () -> {});
        // v153 (owner-approved naya design): header ke neeche
        // wali brand line KHATAM — naam ab footer ki chhoti
        // brand line me number ke sath ata hai (neeche dekho).
        // Status line wohi chhoti editable loading line hai;
        // LIVE badge run ke zinda rehne tak dikhta hai.
        cv.status.setText(BulkRunSettings.showpieceLoading(a,
            BulkStrings.t(a, BulkStrings.RUNNING_IN_TERMINAL)));
        cv.liveBadge.setVisibility(View.VISIBLE);
        // Chhoti screen: ~4 lines ki unchai (poora 230dp wala
        // dabba ab nahi — sirf aakhri asal lines)
        LinearLayout.LayoutParams scp =
            (LinearLayout.LayoutParams) cv.scroll.getLayoutParams();
        scp.height = BulkTheme.dp(a, 88);
        cv.scroll.setLayoutParams(scp);
        // Build-in Background/Cancel row or akela Close button
        // — dono hata kar nayi teen-button wali row (Cancel ka
        // handler watch state ke sath neeche judta hai)
        if (cv.runActions.getParent() instanceof ViewGroup) {
            ((ViewGroup) cv.runActions.getParent()).removeView(cv.runActions);
        }
        if (cv.closeBtn.getParent() instanceof ViewGroup) {
            ((ViewGroup) cv.closeBtn.getParent()).removeView(cv.closeBtn);
        }
        final boolean[] userGone = {false};
        final boolean[] cancelPressed = {false};
        final Runnable[] cancelAction = {null};
        LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = BulkTheme.dp(a, 14);
        actions.setLayoutParams(alp);
        TextView cancelBtn = BulkTheme.secondaryButton(a,
            BulkStrings.t(a, BulkStrings.CANCEL), v -> {
                if (cancelAction[0] != null) cancelAction[0].run();
            });
        cancelBtn.setTextColor(BulkTheme.danger(a));
        cancelBtn.setBackground(BulkTheme.dangerBg(a));
        LinearLayout.LayoutParams cnp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cnp.rightMargin = BulkTheme.dp(a, 5);
        cancelBtn.setLayoutParams(cnp);
        actions.addView(cancelBtn);
        TextView bgBtn = BulkTheme.secondaryButton(a,
            BulkStrings.t(a, BulkStrings.BACKGROUND), v -> {
                userGone[0] = true;
                dialog.dismiss();
            });
        LinearLayout.LayoutParams bgp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bgp.rightMargin = BulkTheme.dp(a, 5);
        bgBtn.setLayoutParams(bgp);
        actions.addView(bgBtn);
        TextView closeBtn = BulkTheme.primaryButton(a,
            BulkStrings.t(a, BulkStrings.CLOSE), v -> {
                userGone[0] = true;
                dialog.dismiss();
            });
        closeBtn.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(closeBtn);
        cv.panel.addView(actions);
        // Footer brand line: "✿ <name> · <number> ✿" — chhoti
        // si, darmiyan me. Naam/number Showpiece Edit se badal
        // sakte hain; dono khaali hon to line hi nahi banti.
        String footerText = BulkRunSettings.showpieceFooterText(
            BulkRunSettings.showpieceName(a), BulkRunSettings.showpieceNumber(a));
        if (!footerText.isEmpty()) {
            TextView footer = new TextView(a);
            footer.setText(footerText);
            footer.setTextSize(11.5f);
            footer.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            footer.setTextColor(BulkTheme.textMuted(a));
            footer.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            flp.topMargin = BulkTheme.dp(a, 10);
            footer.setLayoutParams(flp);
            cv.panel.addView(footer);
        }
        final Dialog shown = BulkTheme.showPanelDialog(a, cv.panel);
        final boolean[] launchedOnce = {false};
        shown.setOnDismissListener(d -> {
            if (onLaunched != null && !launchedOnce[0]) {
                launchedOnce[0] = true;
                onLaunched.run();
            }
        });
        // v152: Done-watch ka snapshot — panel khulne se pehle
        // jo sessions mojood thay. Hamara session is ke BAAD
        // bane ga (service intent abhi process hona hai), is
        // liye list me jo NAYA session dikhe wohi hamara hai;
        // command string ka match use aur pakka karta hai.
        final Set<TermuxSession> watchSnapshot = new HashSet<>();
        try {
            TermuxShellManager sm0 = TermuxShellManager.getShellManager();
            if (sm0 != null) watchSnapshot.addAll(sm0.mTermuxSessions);
        } catch (Exception ignored) {}
        final TermuxSession[] watchCandidate = {null};
        final boolean[] watchSeenRunning = {false};
        final boolean[] watchDone = {false};
        final String[] lastSnapshot = {null};
        final long startMs = android.os.SystemClock.elapsedRealtime();
        final android.os.Handler uiHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
        // v153: ASAL Cancel — session ka poora process tree
        // (killSessionTree: group SIGKILL + finishIfRunning
        // backstop). Candidate abhi na mila ho to session list
        // me command se dhoondo; phir bhi na mile to poster har
        // tick par cancelPressed dekh kar milte hi band kar
        // dega (session bante hi). Status sachcha "Cancelled"
        // ho jata hai or panel ek lamhe baad band — run ka
        // session service me hota hai, panel band hone se
        // pehle hi tree mar chuka hota hai.
        cancelAction[0] = () -> {
            if (cancelPressed[0] || watchDone[0]) return;
            cancelPressed[0] = true;
            TermuxSession target = watchCandidate[0];
            if (target == null) {
                try {
                    TermuxShellManager sm = TermuxShellManager.getShellManager();
                    if (sm != null) {
                        for (TermuxSession s : new ArrayList<>(sm.mTermuxSessions)) {
                            if (watchSnapshot.contains(s)) continue;
                            ExecutionCommand ec = s.getExecutionCommand();
                            if (ec != null && argumentsContainCommand(
                                    ec.arguments, pipedCommand)) {
                                target = s;
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (target != null) killSessionTree(target);
            cv.liveBadge.setVisibility(View.GONE);
            cv.spinner.setVisibility(View.GONE);
            cv.status.setText(BulkStrings.t(a, BulkStrings.RUN_CANCELLED));
            uiHandler.postDelayed(() -> {
                if (shown.isShowing()) shown.dismiss();
            }, 800);
        };
        final Runnable[] poster = new Runnable[1];
        poster[0] = () -> {
            if (!shown.isShowing()) return;
            cv.timer.setText(BulkRunProgress.formatElapsed(
                android.os.SystemClock.elapsedRealtime() - startMs));
            // v152/v153: har tick par sachcha Done signal dekho
            // — mil jaye to exit status parha jata hai: sirf 0
            // par "Finished" + AUTO-OFF; ghalti (ya status na
            // mil sake) par panel KHULA rehta hai taake owner
            // lines parh sake. Timer wahi ruk jata hai (poster
            // chain khatam).
            if (!watchDone[0] && panelWatchTick(watchSnapshot,
                    watchCandidate, watchSeenRunning, pipedCommand)) {
                watchDone[0] = true;
                int exit = Integer.MIN_VALUE;
                try {
                    if (watchCandidate[0] != null
                        && watchCandidate[0].getTerminalSession() != null
                        && !watchCandidate[0].getTerminalSession().isRunning()) {
                        exit = watchCandidate[0].getTerminalSession().getExitStatus();
                    }
                } catch (Exception ignored) {}
                cv.liveBadge.setVisibility(View.GONE);
                cv.spinner.setVisibility(View.GONE);
                if (!cancelPressed[0]) {
                    cv.status.setText(exit == 0
                        ? BulkRunSettings.showpieceDone(a,
                            BulkStrings.t(a, BulkStrings.RUN_DONE))
                        : BulkStrings.t(a, BulkStrings.RUN_FAILED));
                }
                if (exit == 0 && !cancelPressed[0]) {
                    // AUTO-OFF (owner ka hukm): Finished ek
                    // lamha (~1.2s) dikhe, phir panel KHUD band.
                    // Owner beech me Background/Close/X se gaya
                    // ho to ye kuch nahi karta (panel pehle hi
                    // band; userGone bhi nishan hai).
                    uiHandler.postDelayed(() -> {
                        if (shown.isShowing() && !userGone[0]) shown.dismiss();
                    }, 1200);
                }
            }
            // Cancel dabane ke baad candidate der se mile to
            // use bhi foran band karo (warna run peeche shuru
            // ho kar chalta rehta)
            if (cancelPressed[0] && !watchDone[0] && watchCandidate[0] != null) {
                killSessionTree(watchCandidate[0]);
            }
            // Snapshot: candidate milte hi har tick session ki
            // ASAL screen — panel me sirf aakhri ~4 lines or
            // "[Process completed - press Enter]" wali line
            // kabhi nahi (panelDisplayText ka filter). Done
            // wale tick par aakhri dafa bhi render hota hai,
            // phir chain ruk jati hai. Text badla na ho to
            // view ko nahi chhute (flicker nahi).
            if (watchCandidate[0] != null) {
                String raw = sessionTranscriptText(watchCandidate[0]);
                String snap = panelDisplayText(raw, 4);
                if (!snap.equals(lastSnapshot[0])) {
                    lastSnapshot[0] = snap;
                    cv.out.setText(snap);
                    cv.scroll.post(() ->
                        cv.scroll.fullScroll(View.FOCUS_DOWN));
                }
                // LIVE numbers: transcript ki AAKHRI asal ginti
                // hi bari ginti + patli bar banti hai — ginti na
                // mile to dono ghayab rehte hain (nakli number
                // kabhi nahi)
                int[] count = parseTranscriptCount(raw);
                if (count != null) {
                    String c = count[0] + " / " + count[1];
                    if (!c.equals(cv.countText.getText().toString())) {
                        cv.countText.setText(c);
                    }
                    if (cv.slimBar.getVisibility() != View.VISIBLE) {
                        cv.slimBar.setVisibility(View.VISIBLE);
                    }
                    BulkTheme.setProgress(cv.slimBar,
                        countPercent(count[0], count[1]));
                }
            }
            // v153: refresh 500ms -> ~200ms (panel live lage)
            if (!watchDone[0]) uiHandler.postDelayed(poster[0], 200);
        };
        uiHandler.post(poster[0]);
    }

    /**
     * v152: session ki asal terminal screen ka text
     * (ShellUtils -> emulator -> TerminalBuffer) — wahi
     * sachcha manzar jo owner full terminal me dekhta hai.
     * Emulator abhi na bana ho ya koi bhi khami ho to khaali
     * string (panel sirf timer/status par rahe ga).
     */
    static String sessionTranscriptText(final TermuxSession session) {
        try {
            if (session == null || session.getTerminalSession() == null) {
                return "";
            }
            String t = ShellUtils.getTerminalSessionTranscriptText(
                session.getTerminalSession(), false, false);
            return t == null ? "" : t;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * v152: transcript ka aakhri hissa — zyada se zyada
     * maxLines lines (panel ke text area ke liye). Khaali
     * aakhri lines (trailing newlines) hata di jati hain.
     * Khalis (pure) function — test isi par hota hai.
     */
    static String transcriptTail(String transcript, int maxLines) {
        if (transcript == null || maxLines <= 0) return "";
        String t = transcript.replace("\r\n", "\n").replace("\r", "\n");
        while (t.endsWith("\n")) t = t.substring(0, t.length() - 1);
        if (t.isEmpty()) return "";
        String[] lines = t.split("\n", -1);
        if (lines.length <= maxLines) return t;
        StringBuilder sb = new StringBuilder();
        for (int i = lines.length - maxLines; i < lines.length; i++) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(lines[i]);
        }
        return sb.toString();
    }

    /**
     * v152: panel ke Done-watch ka ek tick. TermuxService ki
     * session list (TermuxShellManager.mTermuxSessions) hi
     * wahid sachcha signal hai: hamara session wahan naya
     * banta hai, or khatam hone par service use list se hata
     * deti hai (TermuxService.onTermuxSessionExited);
     * process rukne par TerminalSession.isRunning() bhi
     * false ho jata hai (cleanupResources — view ho ya na
     * ho). Signal na mile to hamesha false: panel Running
     * par rahe ga, jhoota Done kabhi nahi bane ga.
     *
     * @return true jab run ka session sach-much khatam ho gaya ho
     */
    static boolean panelWatchTick(final Set<TermuxSession> snapshot,
                                  final TermuxSession[] candidate,
                                  final boolean[] seenRunning,
                                  final String command) {
        try {
            TermuxShellManager sm = TermuxShellManager.getShellManager();
            if (sm == null) return false;
            List<TermuxSession> now = new ArrayList<>(sm.mTermuxSessions);
            if (candidate[0] == null) {
                for (TermuxSession s : now) {
                    if (snapshot.contains(s)) continue;
                    ExecutionCommand ec = s.getExecutionCommand();
                    if (ec != null
                        && argumentsContainCommand(ec.arguments, command)) {
                        candidate[0] = s;
                        break;
                    }
                }
                if (candidate[0] == null) {
                    for (TermuxSession s : now) {
                        if (!snapshot.contains(s)) {
                            candidate[0] = s;
                            break;
                        }
                    }
                }
                if (candidate[0] == null) return false;
            }
            boolean gone = !now.contains(candidate[0]);
            boolean running = false;
            if (!gone) {
                if (candidate[0].getTerminalSession().getPid() > 0) {
                    seenRunning[0] = true;
                }
                running = candidate[0].getTerminalSession().isRunning();
            }
            return panelWatchIsDone(true, gone, seenRunning[0], running);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * v152: Done ka khalis (pure) faisla — candidate mila ho,
     * or ya to wo session list se ghayab ho (service ne khatam
     * karke hataya), ya chalte hue (pid ke sath) dikhne ke
     * baad ab ruk gaya ho. Chal raha ho ya kabhi mila hi na
     * ho to Done NAHI.
     */
    static boolean panelWatchIsDone(boolean candidateFound, boolean candidateGone,
                                    boolean seenRunning, boolean candidateRunning) {
        if (!candidateFound) return false;
        if (candidateGone) return true;
        return seenRunning && !candidateRunning;
    }

    /** v152: session ke arguments me hamari dispatched command string hai? */
    static boolean argumentsContainCommand(String[] arguments, String command) {
        if (arguments == null || command == null) return false;
        for (String arg : arguments) {
            if (command.equals(arg)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------ v153: panel sachchai

    /**
     * TerminalSession process khatam hone par screen par ye
     * line khud likhta hai ("\r\n[Process completed" +
     * " - press Enter]"). Panel me ye line KABHI nahi dikhti
     * (owner ka hukm) — sirf panel ke display se filter hoti
     * hai; TerminalSession ki asal tehreer ko hath nahi lagta.
     */
    static final String PROCESS_COMPLETED_LINE = "[Process completed - press Enter]";

    /** Ye line wahi dabane-wali line hai? (pure) */
    static boolean isProcessCompletedLine(String line) {
        return line != null && PROCESS_COMPLETED_LINE.equals(line.trim());
    }

    /**
     * v153: panel ki chhoti screen ka sachcha text (pure) —
     * transcript se "[Process completed - press Enter]" wali
     * lines hata kar aakhri maxLines lines. Trailing khaali
     * lines bhi hat jati hain. Ye sirf DISPLAY ka filter hai;
     * session ki asal screen aisi ki aisi rehti hai.
     */
    static String panelDisplayText(String transcript, int maxLines) {
        if (transcript == null || maxLines <= 0) return "";
        String t = transcript.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = t.split("\n", -1);
        List<String> kept = new ArrayList<>();
        for (String line : lines) {
            if (!isProcessCompletedLine(line)) kept.add(line);
        }
        while (!kept.isEmpty() && kept.get(kept.size() - 1).isEmpty()) {
            kept.remove(kept.size() - 1);
        }
        int from = Math.max(0, kept.size() - maxLines);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < kept.size(); i++) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(kept.get(i));
        }
        return sb.toString();
    }

    private static final java.util.regex.Pattern COUNT_PATTERN =
        java.util.regex.Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    /**
     * v153: transcript se AAKHRI "N/M" ginti (pure) — runner
     * apni asal screen par jo progress chhapta hai (tqdm
     * "HD Mode |█████░░| 5/8", delete "Mockups: 100% 15/15"),
     * wahi panel ke numbers bante hain. Koi match na mile ya
     * total 0 ho to null — panel number NAHI banata (nakli
     * ginti kabhi nahi).
     *
     * @return {done, total} ya null
     */
    static int[] parseTranscriptCount(String transcript) {
        if (transcript == null) return null;
        java.util.regex.Matcher m = COUNT_PATTERN.matcher(transcript);
        int[] last = null;
        while (m.find()) {
            try {
                int done = Integer.parseInt(m.group(1));
                int total = Integer.parseInt(m.group(2));
                if (total > 0) last = new int[]{done, total};
            } catch (NumberFormatException ignored) {}
        }
        return last;
    }

    /** Ginti se percent (pure): total <= 0 par 0; hamesha 0..100 ke andar. */
    static int countPercent(int done, int total) {
        if (total <= 0) return 0;
        int pct = (int) Math.round(done * 100.0 / total);
        if (pct < 0) return 0;
        if (pct > 100) return 100;
        return pct;
    }

    /**
     * v153: ASAL Cancel — run ke session ka POORA process
     * tree maro. Code se sabit tartib: JNI createSubprocess
     * child par setsid() bulata hai (terminal-emulator ka
     * termux.c) — shell apne process GROUP ka leader banta
     * hai (pgid == pid) or ghair-interactive bash ke bachay
     * (pipeline ka subshell, python) usi group me rehte
     * hain. Is liye group par SIGKILL (-pid) poora tree
     * khatam karta hai; sirf leader ko marna (finishIfRunning
     * akela) python ko yateem chor deta — wo backstop ke tor
     * par aakhir me bulta hai. Session list se hatana service
     * ka apna rasta karta hai (exit ke baad onTermuxSession
     * Exited); panel ka watch bhi !isRunning dekh kar Done
     * pehchan leta hai.
     */
    static void killSessionTree(TermuxSession session) {
        if (session == null || session.getTerminalSession() == null) return;
        com.termux.terminal.TerminalSession ts = session.getTerminalSession();
        try {
            int pid = ts.getPid();
            if (pid > 0) {
                try {
                    android.system.Os.kill(-pid, android.system.OsConstants.SIGKILL);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try { ts.finishIfRunning(); } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------ v153: terminal launch nishan

    /**
     * v153: launchInTerminal ne aakhri dafa TermuxActivity ko
     * app-run ke liye khola — isi nishan se TermuxActivity ka
     * cosmetic fade-in (sirf app-launched khulne par) or koi
     * aur launch-scoped rawaiya pehchana jata hai. Hath se
     * khola gaya terminal is nishan ko kabhi nahi bharta.
     */
    private static volatile long sTerminalLaunchMs = 0;

    static void noteTerminalLaunch(long nowMs) {
        sTerminalLaunchMs = nowMs;
    }

    /** Nishan abhi taaza hai? (pure) */
    static boolean terminalLaunchFresh(long launchedMs, long nowMs, long windowMs) {
        return launchedMs > 0 && nowMs >= launchedMs && nowMs - launchedMs <= windowMs;
    }

    /**
     * Taaza nishan ek dafa istemal karke saaf kar do — baad
     * wale hath ke khulne par asar nahi parta.
     */
    static synchronized boolean consumeTerminalLaunch(long nowMs, long windowMs) {
        long launched = sTerminalLaunchMs;
        sTerminalLaunchMs = 0;
        return terminalLaunchFresh(launched, nowMs, windowMs);
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
        // v153: naya panel design — LIVE badge (chrome me), status
        // row ke dayein taraf bari ginti, us ke neeche patli bar
        TextView liveBadge;
        TextView countText;
        LinearLayout slimBar;
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
        // v153: LIVE badge — sirf jab run asal me zinda ho
        // (showpiece panel ise chalata hai; default GONE)
        cv.liveBadge = new TextView(a);
        cv.liveBadge.setText("● LIVE");
        cv.liveBadge.setTextSize(10.5f);
        cv.liveBadge.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cv.liveBadge.setTextColor(BulkTheme.danger(a));
        LinearLayout.LayoutParams lbp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lbp.leftMargin = BulkTheme.dp(a, 8);
        cv.liveBadge.setLayoutParams(lbp);
        cv.liveBadge.setVisibility(View.GONE);
        chrome.addView(cv.liveBadge);
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
        // v153: status row ke dayein taraf bari ginti ("5 / 8") —
        // sirf jab transcript se asal ginti mile (showpiece panel
        // bharta hai; na mile to khaali rehti hai)
        cv.countText = new TextView(a);
        cv.countText.setTextSize(17);
        cv.countText.setTypeface(android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD);
        cv.countText.setTextColor(BulkTheme.textMain(a));
        cv.countText.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nlp.leftMargin = BulkTheme.dp(a, 9);
        cv.countText.setLayoutParams(nlp);
        statusRow.addView(cv.countText);
        panel.addView(statusRow);

        // v153: status row ke neeche patli progress bar — ginti
        // mile to hi dikhti hai (showpiece panel chalata hai)
        cv.slimBar = BulkTheme.progressTrack(a);
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(a, 5));
        sbp.topMargin = BulkTheme.dp(a, 8);
        cv.slimBar.setLayoutParams(sbp);
        cv.slimBar.setVisibility(View.GONE);
        panel.addView(cv.slimBar);

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
