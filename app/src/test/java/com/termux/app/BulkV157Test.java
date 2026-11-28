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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * v157 — Pre-run delete GATE: BulkRunGate ke pure faisle
 * (khaali list → run foran; pending → pehle delete;
 * gate ke baad cancel/fail par bhi run aage — items
 * Pending me rehte hain, loop kabhi nahi), gate sirf
 * Run par (Batch/Delete par nahi), BulkLog lines, or
 * source wiring (RunHelper ka gate + Stage ka in-place
 * Run Now sequence + gate host continuation). Asal
 * delete/run flow device-bound hai — us ka saboot
 * phone par.
 */
public class BulkV157Test {

    private static final String RUN_HELPER =
        "app/src/main/java/com/termux/app/BulkRunHelper.java";
    private static final String STAGE =
        "app/src/main/java/com/termux/app/BulkStageActivity.java";
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

    /** Source ka ek hissa (start marker se end marker tak) — na mile to "". */
    private static String region(String src, String start, String end) {
        if (src == null) return "";
        int i = src.indexOf(start);
        if (i < 0) return "";
        int j = src.indexOf(end, i + start.length());
        if (j < 0) j = src.length();
        return src.substring(i, j);
    }

    // ------------------------------------------------------------ gate faisla

    @Test
    public void emptyPendingRunsNow() {
        assertEquals(BulkRunGate.RUN_NOW, BulkRunGate.decide(0, false));
    }

    @Test
    public void nonEmptyPendingDeletesFirst() {
        assertEquals(BulkRunGate.DELETE_FIRST, BulkRunGate.decide(1, false));
        assertEquals(BulkRunGate.DELETE_FIRST, BulkRunGate.decide(4, false));
        assertEquals(BulkRunGate.DELETE_FIRST, BulkRunGate.decide(200, false));
    }

    @Test
    public void alreadyGatedAlwaysRunsNow() {
        // Gate ke baad — pending baqi bhi ho (cancel/fail)
        // to run aage barhta hai, gate loop nahi banta
        assertEquals(BulkRunGate.RUN_NOW, BulkRunGate.decide(4, true));
        assertEquals(BulkRunGate.RUN_NOW, BulkRunGate.decide(1, true));
        assertEquals(BulkRunGate.RUN_NOW, BulkRunGate.decide(0, true));
    }

    @Test
    public void badCountRunsNow() {
        assertEquals(BulkRunGate.RUN_NOW, BulkRunGate.decide(-1, false));
    }

    @Test
    public void gateOnlyForProcessAction() {
        assertTrue(BulkRunGate.gatesAction("process"));
        assertFalse(BulkRunGate.gatesAction("batch"));
        assertFalse(BulkRunGate.gatesAction("del"));
        assertFalse(BulkRunGate.gatesAction(""));
        assertFalse(BulkRunGate.gatesAction(null));
    }

    // ------------------------------------------------------------ gate ka poora chakkar (pure set hisaab se)

    @Test
    public void cancelLeavesItemsPendingAndRunProceeds() {
        // Owner ki phone surat: 4 move items pending
        Set<String> pending = BulkPendingDeletes.decode(
            "content://a\ncontent://b\ncontent://c\ncontent://d");
        assertEquals(BulkRunGate.DELETE_FIRST,
            BulkRunGate.decide(BulkPendingDeletes.count(pending), false));
        // Flow chala: 2 delete hue, 2 par user ne popup
        // cancel kiya — wo Pending me REHTE hain
        Set<String> remaining = BulkPendingDeletes.minus(pending,
            Arrays.asList("content://a", "content://b"));
        assertEquals(2, BulkPendingDeletes.count(remaining));
        assertTrue(remaining.contains("content://c"));
        assertTrue(remaining.contains("content://d"));
        // ...lekin run phir bhi shuru hota hai (alreadyGated)
        assertEquals(BulkRunGate.RUN_NOW,
            BulkRunGate.decide(BulkPendingDeletes.count(remaining), true));
    }

    @Test
    public void fullDeleteEmptiesListAndRunProceeds() {
        Set<String> pending = BulkPendingDeletes.decode("content://a\ncontent://b");
        Set<String> remaining = BulkPendingDeletes.minus(pending,
            Arrays.asList("content://a", "content://b"));
        assertEquals(0, BulkPendingDeletes.count(remaining));
        assertEquals(BulkRunGate.RUN_NOW,
            BulkRunGate.decide(BulkPendingDeletes.count(remaining), true));
        // Agli dafa bhi gate nahi lagega — list khaali hai
        assertEquals(BulkRunGate.RUN_NOW,
            BulkRunGate.decide(BulkPendingDeletes.count(remaining), false));
    }

    @Test
    public void failureKeepsEverythingPendingAndRunProceeds() {
        Set<String> pending = new LinkedHashSet<>(
            Arrays.asList("content://x", "content://y", "content://z"));
        // Flow fail: kuch delete nahi hua — list joon ki toon
        Set<String> remaining = BulkPendingDeletes.minus(pending,
            Arrays.<String>asList());
        assertEquals(pending, remaining);
        assertEquals(BulkRunGate.RUN_NOW,
            BulkRunGate.decide(BulkPendingDeletes.count(remaining), true));
    }

    // ------------------------------------------------------------ BulkLog lines

    @Test
    public void gateLogLinesFitCap() {
        String fire = BulkRunGate.gateLogLine(4);
        assertTrue(fire.contains("4"));
        assertTrue("line: " + fire, fire.length() <= 90);
        String done = BulkRunGate.gateDoneLine(2);
        assertTrue(done.contains("2"));
        assertTrue("line: " + done, done.length() <= 90);
    }

    // ------------------------------------------------------------ source wiring

    @Test
    public void runHelperHasGate() throws Exception {
        String src = findSource(RUN_HELPER);
        assertNotNull(src);
        assertTrue(src.contains("BulkRunGate.decide"));
        assertTrue(src.contains("launchDeleteGate"));
        assertTrue(src.contains("startRunAfterGate"));
        // Terminal dispatch par gate sirf process ke liye
        assertTrue(src.contains("BulkRunGate.gatesAction(action)"));
        assertTrue(src.contains("startInTerminalAfterGate"));
    }

    @Test
    public void batchAndDeleteDispatchesAreNotGated() throws Exception {
        String src = findSource(RUN_HELPER);
        assertNotNull(src);
        String batch = region(src, "public static void startBatch",
            "private static void dispatchView");
        assertFalse(batch.contains("BulkRunGate"));
        String del = region(src, "public static void runCommandInPopup",
            "private static void dispatchRun");
        assertFalse(del.contains("BulkRunGate"));
    }

    @Test
    public void stageSequencesRunNowAfterDeletes() throws Exception {
        String src = findSource(STAGE);
        assertNotNull(src);
        // Run Now ab deletes ka intezar karta hai
        assertTrue(src.contains("mRunNowProject = sel"));
        assertTrue(src.contains("mReprocessAfterFinish = true"));
        // Purana skip rawaiya khatam — wo comment bhi gaya
        assertFalse(src.contains("Move-deletes yahan process nahi hote"));
        // finishDeleteOriginals intezar wala run shuru karta hai
        String finish = region(src, "private void finishDeleteOriginals",
            "private void continueGate");
        assertTrue(finish.contains("startRunAfterGate"));
        assertTrue(finish.contains("continueGate()"));
    }

    @Test
    public void stageIsGateHost() throws Exception {
        String src = findSource(STAGE);
        assertNotNull(src);
        assertTrue(src.contains("EXTRA_GATE_PROJECT"));
        assertTrue(src.contains("EXTRA_GATE_TERMINAL"));
        assertTrue(src.contains("private void continueGate()"));
        assertTrue(src.contains("startInTerminalAfterGate(this, target, \"process\""));
        assertTrue(src.contains("BulkRunGate.gateDoneLine"));
    }

    @Test
    public void versionRolledForward() throws Exception {
        String gradle = findSource(GRADLE);
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 157"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v157"));
    }
}
