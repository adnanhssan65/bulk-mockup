package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Termux Mod v152 test (owner-approved 2026-11-26 — SIRF ek
 * defect ki fix, v151 ke phone test ke baad):
 *
 * ASAL DEFECT (code se sabit): Popup view me Run dabane par
 * panel "Running in the terminal" dikhata tha lekin run
 * chalta hi nahi tha. Wajah: TermuxService session banata
 * hai (TermuxSession.execute sirf TerminalSession object
 * banata hai) — subprocess sirf TerminalSession
 * .initializeEmulator() se spawn hota hai, jo sirf kisi
 * TerminalView ke updateSize() par bulta hai.
 * SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY me koi view
 * kabhi attach nahi hota, is liye bash -c session me para
 * rehta tha jab tak owner terminal khol na le.
 *
 * FIX: TermuxService.createTermuxSession me plugin
 * (ACTION_SERVICE_EXECUTE) sessions bantay hi fixed size
 * (48x24) par initialize ho jate hain — process foran khud
 * chalta hai, koi Enter/view nahi chahiye. Baad me view
 * attach ho to updateSize sirf resize karta hai — session
 * terminal me live dikhta rehta hai. Session action
 * (DONT_OPEN_ACTIVITY) NAHI badla.
 *
 * Panel ka Done (owner ki doosri hidayat): "Finished" ab
 * SIRF TermuxService ki session list ke sachche signal par
 * dikhta hai (panelWatchTick) — session list se ghayab, ya
 * chalte hue dikhne ke baad ruk gaya. Signal na mile to
 * panel hamesha Running — jhoota Done kabhi nahi.
 *
 * Live TerminalView panel ke andar NAHI lagayi gayi:
 * TerminalView ko redraw session ke ek hi
 * TerminalSessionClient slot se milta hai jo service/
 * activity ke pas hai — dialog-owned view use cheen kar
 * full-terminal hand-off ko khatre me dalta. Us ki jagah
 * panel SNAPSHOT dikhata hai: har tick session ki asal
 * screen (TerminalBuffer) ka text plain text me — sachcha
 * manzar, na rang na cursor (ye had imandari se qabool
 * hai).
 *
 * Typed rasta (Terminal view): ProjectsLauncher.runCommand
 * command ke sath "\n" pehle hi bhejta hai — khud-ba-khud
 * chalta hai; is test se pin hai.
 *
 * versionCode 152 + footer v152.
 */
public class BulkV152Test {

    private static final String SERVICE =
        "app/src/main/java/com/termux/app/TermuxService.java";
    private static final String CLIENT =
        "app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";
    private static final String REGISTER =
        "app/src/main/java/com/termux/app/BulkMediaRegister.java";
    private static final String MORE_SETTINGS =
        "app/src/main/java/com/termux/app/BulkMoreSettingsActivity.java";
    private static final String HELPER =
        "app/src/main/java/com/termux/app/BulkRunHelper.java";
    private static final String LAUNCHER =
        "app/src/main/java/com/termux/app/ProjectsLauncher.java";
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

    // ------------------------------------------------------------ pure: Done faisla

    @Test
    public void panelWatchDoneTruthTable() {
        // candidate hi na mila ho -> Done kabhi nahi
        assertFalse(BulkRunHelper.panelWatchIsDone(false, false, false, false));
        assertFalse(BulkRunHelper.panelWatchIsDone(false, true, true, false));
        // session list se ghayab = service ne khatam karke hataya
        assertTrue(BulkRunHelper.panelWatchIsDone(true, true, false, false));
        assertTrue(BulkRunHelper.panelWatchIsDone(true, true, true, true));
        // chalte hue (pid ke sath) dikha, ab ruk gaya -> Done
        assertTrue(BulkRunHelper.panelWatchIsDone(true, false, true, false));
        // abhi chal raha hai -> Done nahi
        assertFalse(BulkRunHelper.panelWatchIsDone(true, false, true, true));
        // kabhi chalta dikha hi nahi (pid 0 = abhi spawn nahi
        // hua) or rukne ka signal nahi -> Done nahi
        assertFalse(BulkRunHelper.panelWatchIsDone(true, false, false, false));
        assertFalse(BulkRunHelper.panelWatchIsDone(true, false, false, true));
    }

    @Test
    public void argumentsContainCommandMatching() {
        String cmd = "cd /x && MOCKUP_BASE=/x python w7.py process 7";
        assertTrue(BulkRunHelper.argumentsContainCommand(
            new String[]{"bash", "-c", cmd}, cmd));
        assertTrue(BulkRunHelper.argumentsContainCommand(
            new String[]{cmd}, cmd));
        // sirf substring mile to match NAHI (asal element chahiye)
        assertFalse(BulkRunHelper.argumentsContainCommand(
            new String[]{"bash", "-c", cmd + " extra"}, cmd));
        assertFalse(BulkRunHelper.argumentsContainCommand(
            new String[]{"bash", "-c"}, cmd));
        assertFalse(BulkRunHelper.argumentsContainCommand(new String[0], cmd));
        assertFalse(BulkRunHelper.argumentsContainCommand(null, cmd));
        assertFalse(BulkRunHelper.argumentsContainCommand(
            new String[]{"bash"}, null));
    }

    // ------------------------------------------------------------ core fix: service

    @Test
    public void pluginSessionsInitializeAtCreation() throws Exception {
        String src = findSource(SERVICE);
        assertNotNull(src);
        String create = window(src,
            "public synchronized TermuxSession createTermuxSession(ExecutionCommand executionCommand)",
            3400);
        assertFalse(create.isEmpty());
        int execIdx = create.indexOf("TermuxSession.execute(");
        int addIdx = create.indexOf("mShellManager.mTermuxSessions.add(newTermuxSession)");
        int initIdx = create.indexOf("getTerminalSession().updateSize(48, 24, 10, 20)");
        assertTrue(execIdx >= 0 && addIdx > execIdx);
        // headless init session banne ke BAAD hai
        assertTrue(initIdx > addIdx);
        // or SIRF plugin commands ke liye gated hai (interactive
        // sessions ka rasta bilkul waisa hi rehta hai)
        String gate = create.substring(addIdx, initIdx);
        assertTrue(gate.contains("if (executionCommand.isPluginExecutionCommand)"));
    }

    @Test
    public void sessionActionMechanismUnchanged() throws Exception {
        // fix ne session action NAHI badla — Popup view me
        // activity ab bhi samne NAHI ati
        String src = findSource(HELPER);
        assertNotNull(src);
        String exec = window(src, "private static boolean executeInTerminalSession", 1500);
        assertTrue(exec.contains(
            "VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY"));
        assertTrue(exec.contains("a.startService(exec)"));
    }

    @Test
    public void typedPathSelfTerminates() throws Exception {
        // Terminal view ka typed rasta: command ke sath
        // newline pehle se jata hai — Enter ka intezar nahi
        String src = findSource(LAUNCHER);
        assertNotNull(src);
        String run = window(src, "static void runCommand", 2600);
        assertTrue(run.contains("(command + \"\\n\")"));
        assertTrue(run.contains("session.write(bytes, 0, bytes.length)"));
    }

    // ------------------------------------------------------------ panel Done watch

    @Test
    public void panelWatchesServiceSessionList() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        // sachcha signal: TermuxShellManager ki session list
        assertTrue(src.contains("TermuxShellManager.getShellManager()"));
        assertTrue(src.contains("sm.mTermuxSessions"));
        assertTrue(src.contains("panelWatchTick(watchSnapshot,"));
        // Done par status RUN_DONE hota hai or poster rukta hai
        // (v153: method lamba ho gaya hai — window bara rakha)
        String panel = window(src, "private static void showRunViewPanel", 12000);
        assertTrue(panel.contains("BulkStrings.RUN_DONE"));
        assertTrue(panel.contains("watchSnapshot"));
        // panel ab bhi display-only hai: koi process nahi
        assertFalse(panel.contains("ProcessBuilder"));
        assertFalse(panel.contains("runLiveInPopup"));
    }

    @Test
    public void runDoneStringBothLanguages() {
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.RUN_DONE));
        assertEquals("Finished ✔", BulkStrings.get(BulkStrings.RUN_DONE, false));
        assertEquals("Ho Gaya ✔", BulkStrings.get(BulkStrings.RUN_DONE, true));
    }

    @Test
    public void liveScreenNotEmbeddedInPanel() throws Exception {
        // feasibility gate: panel ke andar TerminalView NAHI
        // lagayi gayi (session ka client slot service/activity
        // ke pas hai — dialog view use cheen nahi sakta)
        String src = findSource(HELPER);
        assertNotNull(src);
        assertFalse(src.contains("import com.termux.view"));
        assertFalse(src.contains("new TerminalView("));
        assertFalse(src.contains("attachSession"));
    }

    // ------------------------------------------------------------ panel snapshot

    @Test
    public void transcriptTailKeepsLastLines() {
        assertEquals("", BulkRunHelper.transcriptTail(null, 40));
        assertEquals("", BulkRunHelper.transcriptTail("", 40));
        assertEquals("", BulkRunHelper.transcriptTail("\n\n\n", 40));
        assertEquals("", BulkRunHelper.transcriptTail("a\nb", 0));
        // cap se kam lines -> waisa hi (trailing newline saaf)
        assertEquals("a\nb", BulkRunHelper.transcriptTail("a\nb\n", 40));
        assertEquals("a\nb", BulkRunHelper.transcriptTail("a\nb", 2));
        // cap se zyada -> sirf aakhri lines
        assertEquals("c\nd\ne", BulkRunHelper.transcriptTail("a\nb\nc\nd\ne", 3));
        assertEquals("e", BulkRunHelper.transcriptTail("a\nb\nc\nd\ne\n\n", 1));
        // CRLF bhi saaf hota hai
        assertEquals("a\nb", BulkRunHelper.transcriptTail("a\r\nb\r\n", 40));
    }

    @Test
    public void panelShowsSessionSnapshot() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        // snapshot asal screen se ata hai (ShellUtils ->
        // emulator -> TerminalBuffer), parse/mirror nahi
        assertTrue(src.contains(
            "ShellUtils.getTerminalSessionTranscriptText("));
        assertTrue(src.contains("sessionTranscriptText(watchCandidate[0])"));
        assertTrue(src.contains("transcriptTail("));
        assertTrue(src.contains("cv.out.setText(snap)"));
        // text area panel me REHTA hai (snapshot ke liye) —
        // scroll ab remove NAHI hota
        String panel = window(src, "private static void showRunViewPanel", 5800);
        assertFalse(panel.contains("removeView(cv.scroll)"));
        // Cancel/Background wali row ab bhi hati hui hai
        assertTrue(panel.contains("removeView(cv.runActions)"));
    }

    // ------------------------------------------------------------ version

    // ------------------------------------------------------------ auto-return

    @Test
    public void runSessionNames() {
        assertEquals("BulkRun-123", BulkRunHelper.runSessionName(123L));
        assertTrue(BulkRunHelper.isRunSessionName("BulkRun-123"));
        assertFalse(BulkRunHelper.isRunSessionName("bash"));
        assertFalse(BulkRunHelper.isRunSessionName(null));
        assertFalse(BulkRunHelper.isRunSessionName("BulkRun"));
    }

    @Test
    public void autoReturnWiring() throws Exception {
        String src = findSource(CLIENT);
        assertNotNull(src);
        String finish = window(src, "public void onSessionFinished", 4200);
        // auto-return: sirf nishan-zada app-run session, exit 0,
        // activity samne or wahi session screen par — phir
        // session hata kar activity band (owner wapas Bulk par)
        assertTrue(finish.contains("BulkRunHelper.isRunSessionName(finishedSession.mSessionName)"));
        assertTrue(finish.contains("finishedSession.getExitStatus() == 0"));
        assertTrue(finish.contains("mActivity.isVisible()"));
        assertTrue(finish.contains("finishedSession == mActivity.getCurrentSession()"));
        assertTrue(finish.contains("removeFinishedSession(finishedSession)"));
        assertTrue(finish.contains("mActivity.finishActivityIfNotFinishing()"));
    }

    // ------------------------------------------------------------ sequence fix

    @Test
    public void sequenceTogglesDefaultOff() {
        assertFalse(BulkMediaRegister.resolveSequenceFix(null));
        assertTrue(BulkMediaRegister.resolveSequenceFix(Boolean.TRUE));
        assertFalse(BulkMediaRegister.resolveSequenceFix(Boolean.FALSE));
        assertFalse(BulkMediaRegister.resolveAlagSecond(null));
        assertTrue(BulkMediaRegister.resolveAlagSecond(Boolean.TRUE));
        assertFalse(BulkMediaRegister.resolveAlagSecond(Boolean.FALSE));
        assertFalse(BulkMediaRegister.anyPassEnabled(false, false, false));
        assertTrue(BulkMediaRegister.anyPassEnabled(true, false, false));
        assertTrue(BulkMediaRegister.anyPassEnabled(false, true, false));
        assertTrue(BulkMediaRegister.anyPassEnabled(false, false, true));
    }

    @Test
    public void sequenceLadderIsStrictInAllThreeFields() {
        long[][] ladder = BulkMediaRegister.sequenceLadder(4, 10_000L);
        assertEquals(4, ladder.length);
        // aakhri file = nowSec; har field sakht barhta hua
        assertEquals(10_000L, ladder[3][0]);
        for (int i = 1; i < ladder.length; i++) {
            assertTrue(ladder[i][0] > ladder[i - 1][0]);
            assertTrue(ladder[i][1] > ladder[i - 1][1]);
            assertTrue(ladder[i][2] > ladder[i - 1][2]);
        }
        // DATE_ADDED/DATE_MODIFIED seconds me, DATE_TAKEN ms me
        assertEquals(ladder[2][0], ladder[2][1]);
        assertEquals(ladder[2][0] * 1000L, ladder[2][2]);
        assertEquals(0, BulkMediaRegister.sequenceLadder(0, 5L).length);
    }

    @Test
    public void sequenceMismatchesPure() {
        java.util.List<String> order = Arrays.asList("/a/1.jpg", "/a/2.jpg");
        long[][] ladder = BulkMediaRegister.sequenceLadder(2, 5000L);
        java.util.Map<String, long[]> assigned = new java.util.HashMap<>();
        assigned.put("/a/1.jpg", ladder[0]);
        assigned.put("/a/2.jpg", ladder[1]);
        java.util.Map<String, BulkMediaRegister.RowFull> rows =
            new java.util.HashMap<>();
        rows.put("/a/1.jpg", new BulkMediaRegister.RowFull(
            "/a/1.jpg", 11L, ladder[0][0], ladder[0][1], ladder[0][2]));
        rows.put("/a/2.jpg", new BulkMediaRegister.RowFull(
            "/a/2.jpg", 12L, ladder[1][0], ladder[1][1], ladder[1][2]));
        // sab durust → koi mismatch nahi
        assertTrue(BulkMediaRegister.sequenceMismatches(
            order, rows, assigned, true).isEmpty());
        // DATE_TAKEN ghalat → sirf checkTaken par pakra jaye
        rows.put("/a/2.jpg", new BulkMediaRegister.RowFull(
            "/a/2.jpg", 12L, ladder[1][0], ladder[1][1], 999L));
        assertEquals(Arrays.asList("/a/2.jpg"), BulkMediaRegister
            .sequenceMismatches(order, rows, assigned, true));
        assertTrue(BulkMediaRegister.sequenceMismatches(
            order, rows, assigned, false).isEmpty());
        // row gayab → mismatch
        rows.remove("/a/1.jpg");
        assertEquals(Arrays.asList("/a/1.jpg"), BulkMediaRegister
            .sequenceMismatches(order, rows, assigned, false));
        // MIME pehchan
        assertEquals("image/png", BulkMediaRegister.mimeTypeFor("/a/2.PNG"));
        assertEquals("video/mp4", BulkMediaRegister.mimeTypeFor("/a/3.mp4"));
        assertEquals("image/jpeg", BulkMediaRegister.mimeTypeFor("/a/4.jpg"));
    }

    @Test
    public void sequencePassWiringIsDeleteFree() throws Exception {
        String src = findSource(REGISTER);
        assertNotNull(src);
        // v154: sequence pass hi KHATAM — poori file me koi
        // MediaStore delete/update machinery nahi; pass sirf
        // scanOne se register karta hai (delete-free se aage:
        // write-free)
        assertFalse(src.contains("sequenceFixPass"));
        assertFalse(src.contains("deleteRows"));
        assertFalse(src.contains("ContentProviderOperation"));
        int from = src.indexOf("public static int registerProject(Context ctx, int n, boolean force)");
        int to = src.indexOf("public static int registerAllProjects");
        assertTrue(from >= 0 && to > from);
        String pass = src.substring(from, to);
        assertTrue(pass.contains("scanOne"));
        assertFalse(pass.contains(".delete("));
        assertFalse(pass.contains("newUpdate"));
        assertFalse(pass.contains("newInsert"));
    }

    // ------------------------------------------------------------ showpiece

    @Test
    public void showpieceSettingsPure() {
        // v153: naam ki default owner ke hukm par sirf "Adnan"
        assertEquals("Adnan", BulkRunSettings.SHOWPIECE_NAME_DEFAULT);
        assertEquals("X", BulkRunSettings.resolveShowpieceText("X", "D"));
        assertEquals("D", BulkRunSettings.resolveShowpieceText(null, "D"));
        assertEquals("D", BulkRunSettings.resolveShowpieceText("   ", "D"));
        assertEquals("seq_fix", BulkStrings.SEQ_FIX);
        assertEquals("alag_second", BulkStrings.ALAG_SECOND);
        assertEquals("showpiece_edit", BulkStrings.SHOWPIECE_EDIT);
    }

    @Test
    public void moreSettingsFamilyComplete() throws Exception {
        String src = findSource(MORE_SETTINGS);
        assertNotNull(src);
        // family rows body me lagti hain (v154: sequence/alag ki
        // jagah autoScan + waitTime)
        assertTrue(src.contains("body.addView(runViewRow());"));
        assertTrue(src.contains("body.addView(showpieceEditRow());"));
        assertTrue(src.contains("body.addView(autoScanRow());"));
        assertTrue(src.contains("body.addView(waitTimeRow());"));
        assertFalse(src.contains("body.addView(sequenceFixRow());"));
        assertFalse(src.contains("body.addView(alagSecondRow());"));
        // editor teen fields save karta hai
        assertTrue(src.contains("BulkRunSettings.setShowpieceName"));
        assertTrue(src.contains("BulkRunSettings.setShowpieceLoading"));
        assertTrue(src.contains("BulkRunSettings.setShowpieceDone"));
        // nayi settings apni apni jagah par
        assertTrue(src.contains("BulkMediaRegister.setAutoScanEnabled"));
        assertTrue(src.contains("BulkMediaRegister.setWaitSeconds"));
    }

    @Test
    public void panelShowpieceWiring() throws Exception {
        String src = findSource(HELPER);
        assertNotNull(src);
        String panel = window(src, "private static void showRunViewPanel", 12000);
        // v153: naam ab footer ki brand line me (showpieceName
        // footer composer ko jata hai) + editable loading/done
        assertTrue(panel.contains("BulkRunSettings.showpieceName(a)"));
        assertTrue(panel.contains("BulkRunSettings.showpieceLoading(a,"));
        assertTrue(panel.contains("BulkRunSettings.showpieceDone(a,"));
    }

    @Test
    public void versionIs152() throws Exception {
        String gradle = findSource(GRADLE);
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 156"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v156"));
        assertFalse(dash.contains("Termux Mod v151"));
    }
}
