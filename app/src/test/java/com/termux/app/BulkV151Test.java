package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Termux Mod v151 test (owner-approved 2026-11-26, AKHRI update):
 *  - Run ka ENGINE har haal me asal terminal session hai —
 *    startRun dispatchRun se guzarta hai jo sirf VIEW chunta
 *    hai; popup wala ProcessBuilder engine startRun se
 *    kabhi nahi bulta.
 *  - Terminal view (More Settings ki Run View switch ON):
 *    pre-v137 wala EXTRA_RUN_COMMAND hand-off, TermuxActivity
 *    samne. Popup view (default): ACTION_SERVICE_EXECUTE se
 *    naya terminal session peeche chalta hai aur console
 *    popup DISPLAY-ONLY showpiece dikhta hai (timer + status;
 *    na process, na parse, na Cancel/Background, na jhoota
 *    Finished/counter).
 *  - Terminal raste sirf BulkRunHelper me hain — koi
 *    add/import flow unhe nahi bulata.
 *  - Batch aur Delete bhi terminal request par hain
 *    (owner ki chauthi hidayat): Delete ka piped option
 *    runCommandInPopup se ata hai — wo bhi dispatchView se
 *    guzarta hai. Popup engine (runLiveInPopup) ab kisi
 *    user action se nahi bulta — koi caller baqi nahi.
 *  - Order Fix fresh-install default OFF (v151); saved
 *    value ki izzat waisi hi.
 *  - versionCode 151 + footer v151.
 */
public class BulkV151Test {

    private static final String HELPER =
        "app/src/main/java/com/termux/app/BulkRunHelper.java";
    private static final String RUN_SETTINGS =
        "app/src/main/java/com/termux/app/BulkRunSettings.java";
    private static final String MORE_SETTINGS =
        "app/src/main/java/com/termux/app/BulkMoreSettingsActivity.java";
    private static final String TERMUX_ACTIVITY =
        "app/src/main/java/com/termux/app/TermuxActivity.java";
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

    // ------------------------------------------------------------ pure: view + order fix

    @Test
    public void terminalViewDefaultIsPopup() {
        // fresh install (stored null) → popup view; saved true → terminal view
        assertFalse(BulkRunSettings.resolveTerminalView(null));
        assertTrue(BulkRunSettings.resolveTerminalView(Boolean.TRUE));
        assertFalse(BulkRunSettings.resolveTerminalView(Boolean.FALSE));
    }

    @Test
    public void orderFixFreshDefaultOffStoredRespected() {
        assertFalse(BulkMediaRegister.resolveEnabled(null));
        assertTrue(BulkMediaRegister.resolveEnabled(Boolean.TRUE));
        assertFalse(BulkMediaRegister.resolveEnabled(Boolean.FALSE));
    }

    // ------------------------------------------------------------ routing wiring

    @Test
    public void startRunRoutesThroughDispatch() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        // startRun ka body: effectiveCommand se runCommandInPopup tak
        int from = src.indexOf("final String cmd = BulkProjects.effectiveCommand");
        int to = src.indexOf("public static void runCommandInPopup", from);
        assertTrue(from >= 0 && to > from);
        String start = src.substring(from, to);
        assertTrue(start.contains("dispatchRun(a, p,"));
        assertTrue(start.contains("BulkRunSettings.buildPipedCommand(cmd, answers)"));
        // popup engine startRun se NAHI bulta
        assertFalse(start.contains("runLiveInPopup"));
    }

    @Test
    public void dispatchAlwaysExecutesInTerminal() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        // dispatchRun sirf log + dispatchView hai
        String run = window(src, "private static void dispatchRun", 500);
        assertTrue(run.contains("dispatchView(a, p, pipedCommand, onLaunched)"));
        assertFalse(run.contains("runLiveInPopup"));
        // asal dispatch: pehle drop sync, phir view ka faisla —
        // engine hamesha terminal
        String dispatch = window(src, "private static void dispatchView", 2600);
        assertTrue(dispatch.contains("BulkDropFolders.syncNow(a)"));
        assertTrue(dispatch.contains("BulkRunSettings.isTerminalView(a)"));
        assertTrue(dispatch.contains("launchInTerminal(a, pipedCommand, onLaunched)"));
        assertTrue(dispatch.contains("executeInTerminalSession(a, pipedCommand)"));
        assertTrue(dispatch.contains("showRunViewPanel(a, p, onLaunched)"));
        // sync execution se PEHLE hai (warna terminal run ko
        // staged images phir nahi milengi — asal defect)
        int syncIdx = dispatch.indexOf("BulkDropFolders.syncNow");
        assertTrue(syncIdx >= 0);
        assertTrue(syncIdx < dispatch.indexOf("launchInTerminal"));
        assertTrue(syncIdx < dispatch.indexOf("executeInTerminalSession"));
        // dispatch popup engine ko NAHI bulata
        assertFalse(dispatch.contains("runLiveInPopup"));
    }

    // ------------------------------------------------------------ staged inputs (5th report)

    @Test
    public void terminalCommandReferencesStagedInputs() {
        // command ka engine folder = staging/fetch ka input folder
        String dir = BulkProjects.projectDir(7).getAbsolutePath();
        assertEquals(dir + "/7_wallpapers",
            BulkProjects.wallpapersDir(7).getAbsolutePath());
        String cmd = BulkProjects.fullCommand(7, "w7.py", "process");
        assertEquals("cd " + dir + " && MOCKUP_BASE=" + dir
            + " python w7.py process 7", cmd);
        // terminal alias (w7) bhi bilkul yahi command hai —
        // app command or hath se type ki hui command ek hi
        // input folder par kaam karti hain
        assertEquals("alias w7='" + cmd + "'",
            BulkProjects.aliasLine("w7", 7, "w7.py", "process"));
        // batch bhi wahi engine folder
        assertEquals("cd " + dir + " && MOCKUP_BASE=" + dir
            + " python w7.py batch 7",
            BulkProjects.fullCommand(7, "w7.py", "batch"));
    }

    @Test
    public void stagingCommitLandsInRunnerInput() throws Exception {
        // Stage screen ka commit seedha engine input me copy
        // karta hai — wahi folder jo command istemal karta hai
        String stage = findSource(
            "app/src/main/java/com/termux/app/BulkStageActivity.java");
        assertNotNull(stage);
        assertTrue(stage.contains(
            "final File destDir = BulkProjects.wallpapersDir(sel.number);"));
        // or drop folder se fetch bhi isi input me lata hai
        String drop = findSource(
            "app/src/main/java/com/termux/app/BulkDropFolders.java");
        assertNotNull(drop);
        assertTrue(drop.contains("File destDir = BulkProjects.wallpapersDir(n);"));
    }

    @Test
    public void batchRoutesThroughRunHelper() throws Exception {
        String projects = findSource(
            "app/src/main/java/com/termux/app/BulkProjectsActivity.java");
        assertNotNull(projects);
        assertTrue(projects.contains("BulkRunHelper.startBatch(this, p)"));
        String src = findSource(HELPER);
        assertNotNull(src);
        String batch = window(src, "public static void startBatch", 500);
        assertTrue(batch.contains("BulkProjects.effectiveCommand(p, \"batch\")"));
        assertTrue(batch.contains("dispatchView(a, p, cmd, null);"));
    }

    @Test
    public void terminalViewLaunchUsesHandoffAndStaysLean() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        String launch = window(src, "private static void launchInTerminal", 900);
        // pre-v137 wala hand-off: TermuxActivity + EXTRA_RUN_COMMAND
        assertTrue(launch.contains("new Intent(a, TermuxActivity.class)"));
        assertTrue(launch.contains("BulkDashboardActivity.EXTRA_RUN_COMMAND"));
        assertTrue(launch.contains("Intent.FLAG_ACTIVITY_NEW_TASK"));
        // terminal raste me: na mirror thread, na guard, na process spawn
        assertFalse(launch.contains("syncMirrorForProject"));
        assertFalse(launch.contains("sRunActive"));
        assertFalse(launch.contains("ProcessBuilder"));
    }

    @Test
    public void popupViewExecutesViaServiceSession() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        String exec = window(src, "private static boolean executeInTerminalSession", 1500);
        assertTrue(exec.contains("TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_SERVICE_EXECUTE"));
        assertTrue(exec.contains("exec.setClass(a, TermuxService.class)"));
        assertTrue(exec.contains("TERMUX_SERVICE.EXTRA_ARGUMENTS"));
        assertTrue(exec.contains(
            "VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY"));
        assertTrue(exec.contains("a.startService(exec)"));
        assertFalse(exec.contains("ProcessBuilder"));
    }

    @Test
    public void showpiecePanelIsDisplayOnly() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        String panel = window(src, "private static void showRunViewPanel", 2900);
        // maujooda console panel hi reuse hota hai
        assertTrue(panel.contains("buildConsolePanel(a, p.name, dialog"));
        assertTrue(panel.contains("BulkStrings.RUNNING_IN_TERMINAL"));
        assertTrue(panel.contains("BulkRunProgress.formatElapsed"));
        // Cancel/Background wali row panel se hatayi jati hai
        assertTrue(panel.contains("removeView(cv.runActions)"));
        // Terminal button wahi session kholta hai
        assertTrue(panel.contains("openTerminalView(a)"));
        // display-only: na process, na popup engine, na jhoota Finished
        assertFalse(panel.contains("ProcessBuilder"));
        assertFalse(panel.contains("runLiveInPopup"));
        assertFalse(panel.contains("RUN_DONE"));
        assertFalse(panel.contains("progress.offer"));
    }

    @Test
    public void terminalPathsOnlyReferencedFromRunHelper() throws Exception {
        File helper = findFile(HELPER);
        assertNotNull(helper);
        File dir = helper.getParentFile();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".java"));
        assertNotNull(files);
        for (File f : files) {
            if (f.getName().equals("BulkRunHelper.java")) continue;
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertFalse(f.getName() + " launchInTerminal na bulaye",
                s.contains("launchInTerminal"));
            assertFalse(f.getName() + " executeInTerminalSession na bulaye",
                s.contains("executeInTerminalSession"));
            assertFalse(f.getName() + " showRunViewPanel na bulaye",
                s.contains("showRunViewPanel"));
        }
        // add/import flows khaas taur par saaf hon
        String add = findSource(
            "app/src/main/java/com/termux/app/BulkAddProjectActivity.java");
        assertNotNull(add);
        assertFalse(add.contains("dispatchRun"));
        String installer = findSource(
            "app/src/main/java/com/termux/app/BulkProjectInstaller.java");
        assertNotNull(installer);
        assertFalse(installer.contains("dispatchRun"));
    }

    @Test
    public void receiverSideHandoffIntact() throws Exception {
        // TermuxActivity aaj bhi EXTRA_RUN_COMMAND parh kar
        // command session me type karti hai — ye muahida na toota ho
        String act = findSource(TERMUX_ACTIVITY);
        assertNotNull(act);
        assertTrue(act.contains("getStringExtra(BulkDashboardActivity.EXTRA_RUN_COMMAND)"));
        assertTrue(act.contains("ProjectsLauncher.runCommand(this, cmd)"));
    }

    @Test
    public void deleteRoutesViaTerminalToo() throws Exception {
        // Delete (runCommandInPopup) bhi dispatchView se —
        // popup engine ka ab KOI caller nahi (dead code)
        String src = findSource(HELPER);
        assertNotNull(src);
        String del = window(src, "public static void runCommandInPopup", 1200);
        assertTrue(del.contains("dispatchView(a, p, pipedCommand, null);"));
        assertFalse(del.contains("runLiveInPopup"));
        assertFalse(src.contains("runLiveInPopup(a, p"));
    }

    // ------------------------------------------------------------ option: More Settings only

    @Test
    public void viewSettingStorageWiring() throws Exception {
        String settings = findSource(RUN_SETTINGS);
        assertNotNull(settings);
        assertTrue(settings.contains("KEY_RUN_VIEW_TERMINAL = \"run_view_terminal\""));
        assertTrue(settings.contains("public static boolean isTerminalView(Context c)"));
        assertTrue(settings.contains("public static void setTerminalView(Context c, boolean terminal)"));
    }

    @Test
    public void viewOptionLivesInMoreSettingsOnly() throws Exception {
        String more = findSource(MORE_SETTINGS);
        assertNotNull(more);
        assertTrue(more.contains("private LinearLayout runViewRow()"));
        assertTrue(more.contains("body.addView(runViewRow());"));
        assertTrue(more.contains("BulkRunSettings.isTerminalView(this)"));
        assertTrue(more.contains("BulkRunSettings.setTerminalView(this, on)"));
        assertTrue(more.contains("BulkStrings.RUN_VIEW"));
        assertTrue(more.contains("BulkStrings.RUN_VIEW_SUB"));
        // Run Settings popup me ye option NAHI hai
        String src = findSource(HELPER);
        assertNotNull(src);
        String popup = window(src, "public static void showRunSettings", 6500);
        assertFalse(popup.contains("RUN_VIEW"));
        assertFalse(popup.contains("setTerminalView"));
    }

    @Test
    public void runViewStringsHaveBothLanguages() {
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.RUN_VIEW));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.RUN_VIEW_SUB));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.RUNNING_IN_TERMINAL));
        assertEquals("Show After Run: Popup / Terminal",
            BulkStrings.get(BulkStrings.RUN_VIEW, false));
        assertEquals("Run ke baad dikhao: Popup / Terminal",
            BulkStrings.get(BulkStrings.RUN_VIEW, true));
        assertEquals("Running in the terminal",
            BulkStrings.get(BulkStrings.RUNNING_IN_TERMINAL, false));
        assertEquals("Run terminal me ho raha hai",
            BulkStrings.get(BulkStrings.RUNNING_IN_TERMINAL, true));
    }

    // ------------------------------------------------------------ version

    @Test
    public void versionIs151() throws Exception {
        String gradle = findSource(GRADLE);
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 151"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v151"));
        assertFalse(dash.contains("Termux Mod v150"));
    }
}
