package com.termux.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Termux Mod v155 test (owner-approved 2026-11-27 — v152 ke
 * phone test ke baad ke hukm):
 *
 *  1) PANEL KA NAYA DESIGN (Run/Batch/Delete ka shared
 *     console panel): header me dots + "Project N" + LIVE
 *     badge + timer pill + X — header ke neeche brand line
 *     KHATAM; naam ab footer ki chhoti line
 *     "✿ <name> · <number> ✿" me (naam default "Adnan",
 *     number naya pref, default owner ka number; dono
 *     Showpiece Edit se badal sakte hain). Status row:
 *     spinner + chhoti loading line + dayein taraf bari
 *     ginti; neeche patli bar; chhoti screen me sirf aakhri
 *     ~4 asal lines.
 *  2) LIVE NUMBERS SACHCHE: ginti session ke ASAL
 *     transcript ki aakhri "N/M" se (parseTranscriptCount) —
 *     na mile to number/bar NAHI banta. Refresh 500ms se
 *     ~200ms. "[Process completed - press Enter]" wali line
 *     panel me kabhi nahi (panelDisplayText ka filter —
 *     sirf display; TerminalSession ki tehreer waisi hi).
 *  3) ASAL CANCEL: killSessionTree — JNI setsid() ki wajah
 *     se shell process-group leader hai (pgid == pid), is
 *     liye group SIGKILL (-pid) poora tree (bash + python)
 *     marta hai; finishIfRunning sirf backstop hai.
 *  4) AUTO-OFF: exit asal me 0 ho to Finished ~1.2s dikh
 *     kar panel khud band; ghalti par panel khula rehta hai.
 *  5) TERMINAL FLOW: v152 auto-return ki wiring ki
 *     tasdeeq — activity samne ho to onSessionFinished
 *     activity client ko hi milta hai (bind par client swap
 *     hota hai); lekin run bind se PEHLE khatam ho jaye to
 *     khabar service client par ruk jati thi (base class ki
 *     onSessionFinished khaali hai). v153: service client
 *     record rakhta hai, service bind par activity client
 *     ko deta hai, wahi auto-return wahan bhi hota hai.
 *  6) KEYBOARD: BulkRun- session screen par ho to
 *     TermuxActivity khulne par keyboard khud nahi khulta
 *     (hidden-on-startup wala rasta, sirf nishan par scope);
 *     hath ke sessions or global setting waisi hi.
 *  7) ANIMATION: app-launched khulne par halki fade-in —
 *     launchInTerminal ka nishan, ek dafa istemal; sirf
 *     cosmetic, run se koi lena dena nahi.
 *  + ADD-ON: Run View ka fresh-install default wapas
 *    POPUP (stored value ki izzat waisi hi).
 *
 * versionCode 155 + footer v153.
 */
public class BulkV153Test {

    private static final String SERVICE =
        "app/src/main/java/com/termux/app/TermuxService.java";
    private static final String SERVICE_CLIENT =
        "app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionServiceClient.java";
    private static final String ACTIVITY_CLIENT =
        "app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";
    private static final String VIEW_CLIENT =
        "app/src/main/java/com/termux/app/terminal/TermuxTerminalViewClient.java";
    private static final String ACTIVITY =
        "app/src/main/java/com/termux/app/TermuxActivity.java";
    private static final String TERMINAL_SESSION =
        "terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java";
    private static final String MORE_SETTINGS =
        "app/src/main/java/com/termux/app/BulkMoreSettingsActivity.java";
    private static final String HELPER =
        "app/src/main/java/com/termux/app/BulkRunHelper.java";
    private static final String GRADLE = "app/build.gradle";
    private static final String DASH =
        "app/src/main/java/com/termux/app/BulkDashboardActivity.java";

    private static File findFile(String relative) {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 8 && dir != null; i++) {
            File a = new File(dir, relative);
            if (a.isFile()) return a;
            File b = new File(dir, "bulk_mockup_fork/termux-app/" + relative);
            if (b.isFile()) return b;
            File c = new File(dir, "workspace/bulk_mockup_fork/termux-app/" + relative);
            if (c.isFile()) return c;
            dir = dir.getParentFile();
        }
        return null;
    }

    private static String findSource(String relative) throws Exception {
        File f = findFile(relative);
        if (f == null) return null;
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    /** Source me marker se agla `len` chars ka hissa (na mile to khaali). */
    private static String window(String src, String marker, int len) {
        int i = src.indexOf(marker);
        if (i < 0) return "";
        return src.substring(i, Math.min(src.length(), i + len));
    }

    // ------------------------------------------------------------ pure: press-Enter filter

    @Test
    public void processCompletedLineDetected() {
        assertTrue(BulkRunHelper.isProcessCompletedLine(
            "[Process completed - press Enter]"));
        assertTrue(BulkRunHelper.isProcessCompletedLine(
            "  [Process completed - press Enter] "));
        assertFalse(BulkRunHelper.isProcessCompletedLine(
            "[Process completed (code 1) - press Enter]"));
        assertFalse(BulkRunHelper.isProcessCompletedLine("press Enter"));
        assertFalse(BulkRunHelper.isProcessCompletedLine(null));
        assertFalse(BulkRunHelper.isProcessCompletedLine(""));
    }

    @Test
    public void panelDisplayTextFiltersAndTails() {
        assertEquals("", BulkRunHelper.panelDisplayText(null, 4));
        assertEquals("", BulkRunHelper.panelDisplayText("", 4));
        assertEquals("", BulkRunHelper.panelDisplayText("a\nb", 0));
        // press-Enter wali line ghayab, baqi asal lines mehfooz
        assertEquals("a\nb", BulkRunHelper.panelDisplayText(
            "a\nb\n[Process completed - press Enter]", 4));
        assertEquals("a\nb", BulkRunHelper.panelDisplayText(
            "a\n[Process completed - press Enter]\nb", 4));
        // sirf aakhri 4 lines
        assertEquals("c\nd\ne\nf", BulkRunHelper.panelDisplayText(
            "a\nb\nc\nd\ne\nf", 4));
        // trailing khaali lines hat jati hain, CRLF durust
        assertEquals("a\nb", BulkRunHelper.panelDisplayText("a\r\nb\r\n\r\n", 4));
        assertEquals("a", BulkRunHelper.panelDisplayText(
            "a\n\n[Process completed - press Enter]\n", 4));
    }

    // ------------------------------------------------------------ pure: live numbers

    @Test
    public void transcriptCountFromTqdmLine() {
        assertArrayEquals(new int[]{5, 8}, BulkRunHelper.parseTranscriptCount(
            "✨ HD Mode |█████░░| 5/8"));
        assertArrayEquals(new int[]{8, 8}, BulkRunHelper.parseTranscriptCount(
            "kuch pehle\nHD Mode |████████| 8/8\n✔ ready"));
    }

    @Test
    public void transcriptCountFromDeleteLine() {
        assertArrayEquals(new int[]{15, 15}, BulkRunHelper.parseTranscriptCount(
            "Mockups: 100% 15/15"));
    }

    @Test
    public void transcriptCountTakesLastMatch() {
        // purani ginti peeche reh jaye to bhi AAKHRI wali chalti hai
        assertArrayEquals(new int[]{7, 9}, BulkRunHelper.parseTranscriptCount(
            "3/9\n5/9\n7/9"));
    }

    @Test
    public void transcriptCountAbsentMeansNoNumbers() {
        assertNull(BulkRunHelper.parseTranscriptCount(null));
        assertNull(BulkRunHelper.parseTranscriptCount(""));
        assertNull(BulkRunHelper.parseTranscriptCount("koi ginti nahi"));
        assertNull(BulkRunHelper.parseTranscriptCount("0/0"));
        assertNull(BulkRunHelper.parseTranscriptCount("sirf 42 number"));
    }

    @Test
    public void countPercentClamps() {
        assertEquals(0, BulkRunHelper.countPercent(0, 8));
        assertEquals(63, BulkRunHelper.countPercent(5, 8));
        assertEquals(100, BulkRunHelper.countPercent(8, 8));
        assertEquals(100, BulkRunHelper.countPercent(9, 8));
        assertEquals(0, BulkRunHelper.countPercent(3, 0));
        assertEquals(0, BulkRunHelper.countPercent(-1, 8));
    }

    // ------------------------------------------------------------ pure: footer + defaults

    @Test
    public void showpieceFooterComposition() {
        assertEquals("✿ Adnan · +44 7404 032136 ✿",
            BulkRunSettings.showpieceFooterText("Adnan", "+44 7404 032136"));
        assertEquals("✿ Adnan ✿",
            BulkRunSettings.showpieceFooterText("Adnan", ""));
        assertEquals("✿ +44 7404 032136 ✿",
            BulkRunSettings.showpieceFooterText("", "+44 7404 032136"));
        assertEquals("", BulkRunSettings.showpieceFooterText("", ""));
        assertEquals("", BulkRunSettings.showpieceFooterText(null, null));
        assertEquals("✿ Adnan ✿",
            BulkRunSettings.showpieceFooterText("  Adnan  ", "   "));
    }

    @Test
    public void showpieceDefaults153() {
        assertEquals("Adnan", BulkRunSettings.SHOWPIECE_NAME_DEFAULT);
        assertEquals("+44 7404 032136", BulkRunSettings.SHOWPIECE_NUMBER_DEFAULT);
        assertEquals("showpiece_number", BulkRunSettings.SHOWPIECE_KEY_NUMBER);
    }

    @Test
    public void runViewFreshDefaultIsPopupAgain() {
        // v153 add-on: fresh install (stored null) → POPUP;
        // stored value ki izzat waisi hi
        assertFalse(BulkRunSettings.resolveTerminalView(null));
        assertTrue(BulkRunSettings.resolveTerminalView(Boolean.TRUE));
        assertFalse(BulkRunSettings.resolveTerminalView(Boolean.FALSE));
    }

    @Test
    public void terminalLaunchMarkerFreshness() {
        assertTrue(BulkRunHelper.terminalLaunchFresh(1000, 1500, 10000));
        assertTrue(BulkRunHelper.terminalLaunchFresh(1000, 11000, 10000));
        assertFalse(BulkRunHelper.terminalLaunchFresh(1000, 11001, 10000));
        assertFalse(BulkRunHelper.terminalLaunchFresh(0, 1000, 10000));
        assertFalse(BulkRunHelper.terminalLaunchFresh(2000, 1000, 10000));
        // ek dafa istemal — dusri dafa nahi
        BulkRunHelper.noteTerminalLaunch(5000);
        assertTrue(BulkRunHelper.consumeTerminalLaunch(6000, 10000));
        assertFalse(BulkRunHelper.consumeTerminalLaunch(6000, 10000));
    }

    // ------------------------------------------------------------ panel wiring

    @Test
    public void panelUsesFilteredSnapshotAndLiveNumbers() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        // v155: window 12000 -> 15000 (item 4 ke Close/X GONE+VISIBLE
        // blocks ne method lamba kar diya; asal asserts wese ke wese)
        String panel = window(src, "private static void showRunViewPanel", 15000);
        assertFalse(panel.isEmpty());
        // filter shuda chhota snapshot + asal ginti + tez refresh
        assertTrue(panel.contains("panelDisplayText(raw, 4)"));
        assertTrue(panel.contains("parseTranscriptCount(raw)"));
        assertTrue(panel.contains("countPercent(count[0], count[1])"));
        assertTrue(panel.contains("postDelayed(poster[0], 200)"));
        // LIVE badge + footer brand line
        assertTrue(panel.contains("cv.liveBadge.setVisibility(View.VISIBLE)"));
        assertTrue(panel.contains("BulkRunSettings.showpieceFooterText("));
        assertTrue(panel.contains("BulkRunSettings.showpieceNumber(a)"));
        // header wali purani brand line KHATAM
        assertFalse(panel.contains("addView(brand"));
        // Terminal button KHATAM
        assertFalse(panel.contains("openTerminalView(a)"));
        assertFalse(panel.contains("BulkStrings.TERMINAL"));
    }

    @Test
    public void panelButtonsCancelBackgroundClose() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        String panel = window(src, "private static void showRunViewPanel", 12000);
        assertTrue(panel.contains("BulkStrings.CANCEL"));
        assertTrue(panel.contains("BulkStrings.BACKGROUND"));
        assertTrue(panel.contains("BulkStrings.CLOSE"));
        assertTrue(panel.contains("BulkTheme.dangerBg(a)"));
        // Cancel asal kill se judta hai or status sachcha Cancelled
        assertTrue(panel.contains("killSessionTree(target)"));
        assertTrue(panel.contains("killSessionTree(watchCandidate[0])"));
        assertTrue(panel.contains("BulkStrings.RUN_CANCELLED"));
    }

    @Test
    public void panelAutoOffOnlyOnCleanExit() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        String panel = window(src, "private static void showRunViewPanel", 12000);
        // exit status asal TerminalSession se parha jata hai
        assertTrue(panel.contains("getTerminalSession().getExitStatus()"));
        // auto-off sirf exit == 0 par, ~1.2s ke baad
        assertTrue(panel.contains("if (exit == 0 && !cancelPressed[0])"));
        assertTrue(panel.contains("}, 1200);"));
        // ghalti par Failed ka sachcha status (auto-off nahi)
        assertTrue(panel.contains("BulkStrings.RUN_FAILED"));
    }

    @Test
    public void consolePanelHasLiveBadgeAndSlimBar() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        assertTrue(src.contains("cv.liveBadge.setText(\"● LIVE\")"));
        assertTrue(src.contains("cv.slimBar = BulkTheme.progressTrack(a)"));
        assertTrue(src.contains("cv.countText"));
    }

    @Test
    public void killSessionTreeTargetsProcessGroup() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        String kill = window(src, "static void killSessionTree", 1500);
        assertFalse(kill.isEmpty());
        // group kill (-pid) pehle, finishIfRunning backstop baad me
        int groupIdx = kill.indexOf("Os.kill(-pid");
        int finishIdx = kill.indexOf("finishIfRunning()");
        assertTrue(groupIdx >= 0);
        assertTrue(finishIdx > groupIdx);
        assertTrue(kill.contains("OsConstants.SIGKILL"));
    }

    @Test
    public void terminalSessionLibraryNotPatched() throws Exception {
        // "[Process completed...]" wali tehreer library me
        // WAISE HI hai — v153 use chhuta nahi, sirf panel ke
        // display se filter karta hai
        String src = findSource(TERMINAL_SESSION);
        assertNotNull(src);
        assertTrue(src.contains("\"\\r\\n[Process completed\""));
        assertTrue(src.contains("exitDescription += \" - press Enter]\""));
    }

    // ------------------------------------------------------------ terminal flow wiring

    @Test
    public void autoReturnBindGapFixed() throws Exception {
        String serviceClient = findSource(SERVICE_CLIENT);
        assertNotNull(serviceClient);
        // service client ab khatam hone ki khabar record karta hai
        assertTrue(serviceClient.contains("public void onSessionFinished("));
        assertTrue(serviceClient.contains("noteBulkRunSessionFinishedBeforeBind"));
        assertTrue(serviceClient.contains("BulkRunHelper.isRunSessionName"));
        assertTrue(serviceClient.contains("getExitStatus() == 0"));

        String service = findSource(SERVICE);
        assertNotNull(service);
        assertTrue(service.contains(
            "public synchronized void noteBulkRunSessionFinishedBeforeBind"));
        String bind = window(service,
            "public synchronized void setTermuxTerminalSessionClient", 2200);
        assertTrue(bind.contains("onBulkRunSessionFinishedBeforeBind(finished)"));
        assertTrue(bind.contains("BULK_RUN_FINISHED_BIND_WINDOW_MS"));

        String client = findSource(ACTIVITY_CLIENT);
        assertNotNull(client);
        String m = window(client, "public void onBulkRunSessionFinishedBeforeBind", 1200);
        assertFalse(m.isEmpty());
        // wohi sakht shart: screen wala session wahi ho
        assertTrue(m.contains("finishedSession != mActivity.getCurrentSession()"));
        assertTrue(m.contains("removeFinishedSession(finishedSession)"));
        assertTrue(m.contains("finishActivityIfNotFinishing()"));
    }

    @Test
    public void keyboardSuppressedOnlyForBulkRunSessions() throws Exception {
        String src = findSource(VIEW_CLIENT);
        assertNotNull(src);
        String state = window(src,
            "public void setSoftKeyboardState(boolean isStartup", 3600);
        assertFalse(state.isEmpty());
        // hidden-on-startup wala rasta BulkRun nishan par bhi
        assertTrue(state.contains("shouldSoftKeyboardBeHiddenOnStartup()"));
        assertTrue(state.contains("isCurrentSessionBulkRun()"));
        // pehchan sirf session ke naam ke nishan se — global
        // setting ko hath nahi lagta
        String helper = window(src, "private boolean isCurrentSessionBulkRun()", 600);
        assertTrue(helper.contains("BulkRunHelper.isRunSessionName(session.mSessionName)"));
        assertFalse(helper.contains("setSoftKeyboardEnabled"));
    }

    @Test
    public void launchAnimationScopedToBulkRunLaunches() throws Exception {
        String helper = findSource(HELPER);
        assertNotNull(helper);
        String launch = window(helper, "private static void launchInTerminal", 2200);
        assertTrue(launch.contains("noteTerminalLaunch(System.currentTimeMillis())"));

        String activity = findSource(ACTIVITY);
        assertNotNull(activity);
        assertTrue(activity.contains(
            "BulkRunHelper.consumeTerminalLaunch(System.currentTimeMillis(), 10000)"));
        assertTrue(activity.contains(
            "overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)"));
    }

    @Test
    public void showpieceEditorHasNumberField() throws Exception {
        String src = findSource(MORE_SETTINGS);
        assertNotNull(src);
        assertTrue(src.contains("BulkStrings.SHOWPIECE_NUMBER_LABEL"));
        assertTrue(src.contains("BulkRunSettings.showpieceNumber(this)"));
        assertTrue(src.contains("BulkRunSettings.setShowpieceNumber"));
    }

    @Test
    public void versionIs153() throws Exception {
        String gradle = findSource(GRADLE);
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 156"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v156"));
        assertFalse(dash.contains("Termux Mod v153"));
    }
}
