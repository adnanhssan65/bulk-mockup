package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v156 — Move Delete ka naya order: BulkMoveDelete ke
 * pure faisle (mode resolve, step routing — har step ek
 * hi dafa, koi retry nahi), saboot summary line, or
 * source wiring (Stage ka naya order + More Settings
 * ki chhoti row). Asal delete flow device-bound hai —
 * us ka saboot phone par (BulkLog line + 30s recheck).
 */
public class BulkV156Test {

    private static final String STAGE =
        "app/src/main/java/com/termux/app/BulkStageActivity.java";
    private static final String MORE_SETTINGS =
        "app/src/main/java/com/termux/app/BulkMoreSettingsActivity.java";
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

    // ------------------------------------------------------------ mode resolve

    @Test
    public void moveDeleteModeDefaultsToAuto() {
        assertEquals(BulkMoveDelete.MODE_AUTO, BulkMoveDelete.resolveMode(null));
        assertEquals(BulkMoveDelete.MODE_AUTO, BulkMoveDelete.resolveMode(""));
        assertEquals(BulkMoveDelete.MODE_AUTO, BulkMoveDelete.resolveMode("garbage"));
        assertEquals(BulkMoveDelete.MODE_AUTO, BulkMoveDelete.resolveMode("auto"));
        assertEquals(BulkMoveDelete.MODE_POPUP, BulkMoveDelete.resolveMode("popup"));
        assertFalse(BulkMoveDelete.isPopup(null));
        assertFalse(BulkMoveDelete.isPopup("auto"));
        assertTrue(BulkMoveDelete.isPopup("popup"));
        assertTrue(BulkMoveDelete.isPopup(BulkMoveDelete.MODE_POPUP));
    }

    // ------------------------------------------------------------ step routing

    @Test
    public void mediaStoreStepStopsAfterSecurityBlock() {
        assertTrue(BulkMoveDelete.shouldTryMediaStore(false));
        assertFalse(BulkMoveDelete.shouldTryMediaStore(true));
    }

    @Test
    public void routeForMsResultIsSingleAttempt() {
        // OK = ho gaya; SECURITY = fallback + aage ke liye
        // step 2 band; ZERO/ERROR/anjaan = sirf fallback.
        // Koi rasta "dobara koshish" nahi deta.
        assertEquals(BulkMoveDelete.ROUTE_DONE,
            BulkMoveDelete.routeForMsResult(BulkMoveDelete.MS_OK));
        assertEquals(BulkMoveDelete.ROUTE_FALLBACK_BLOCK,
            BulkMoveDelete.routeForMsResult(BulkMoveDelete.MS_SECURITY));
        assertEquals(BulkMoveDelete.ROUTE_FALLBACK,
            BulkMoveDelete.routeForMsResult(BulkMoveDelete.MS_ZERO));
        assertEquals(BulkMoveDelete.ROUTE_FALLBACK,
            BulkMoveDelete.routeForMsResult(BulkMoveDelete.MS_ERROR));
        assertEquals(BulkMoveDelete.ROUTE_FALLBACK,
            BulkMoveDelete.routeForMsResult(99));
    }

    // ------------------------------------------------------------ summary line

    @Test
    public void runSummaryCountsAndFitsBulkLogCap() {
        String s = BulkMoveDelete.runSummary("auto", 4, 1, 0, 0);
        assertEquals("Move delete (auto): store 4, direct 1, popup 0, fail 0", s);
        String p = BulkMoveDelete.runSummary("popup", 0, 0, 200, 3);
        assertTrue(p.contains("(popup)"));
        assertTrue(p.contains("popup 200"));
        // BulkLog ek line 90 chars par kaat deta hai — bare
        // counts par bhi summary us ke andar rehni chahiye
        String big = BulkMoveDelete.runSummary("auto", 999, 999, 999, 999);
        assertTrue(big.length() <= BulkLog.MAX_LINE_CHARS);
        // ghalat mode value summary me bhi resolved hi ati hai
        assertTrue(BulkMoveDelete.runSummary("xyz", 0, 0, 0, 0).contains("(auto)"));
    }

    // ------------------------------------------------------------ source wiring

    @Test
    public void stageUsesNewDeleteOrder() throws Exception {
        String src = findSource(STAGE);
        assertNotNull(src);
        // mode pehle parha jata hai; popup/access-na-ho par legacy
        assertTrue(src.contains("BulkMoveDelete.currentMode(this)"));
        assertTrue(src.contains("BulkMoveDelete.isPopup(mDeleteRunMode)"));
        // AUTO: MediaStore delete ki ek koshish + routing
        assertTrue(src.contains("BulkMoveDelete.routeForMsResult(msResult)"));
        assertTrue(src.contains("BulkMoveDelete.shouldTryMediaStore(msBlocked)"));
        // fallback: File.delete + DATA row delete + scan
        assertTrue(src.contains("tryDirectDeleteWithRow(path)"));
        assertTrue(src.contains("MediaStore.Files.getContentUri(\"external\")"));
        // saboot: 30s recheck + finish par summary line
        assertTrue(src.contains("scheduleRowRecheck(firstMsUri)"));
        assertTrue(src.contains("BulkMoveDelete.runSummary(mDeleteRunMode,"));
        // queue/commit design barqarar: pending prefs + sirf
        // copy-verify wale raste waisa hi
        assertTrue(src.contains("queuePendingDeletes(moveDelete)"));
        assertTrue(src.contains("processPendingDeletes()"));
    }

    @Test
    public void moreSettingsHasMoveDeleteRow() throws Exception {
        String src = findSource(MORE_SETTINGS);
        assertNotNull(src);
        assertTrue(src.contains("body.addView(moveDeleteRow());"));
        assertTrue(src.contains("BulkStrings.MOVE_DELETE"));
        assertTrue(src.contains("BulkMoveDelete.setMode(this, mode)"));
        assertTrue(src.contains("BulkStrings.MOVE_DELETE_POPUP"));
    }

    // -------------------------------------------------- pending list (pure)

    @Test
    public void pendingDecodeEncodeRoundTrip() {
        Set<String> s = BulkPendingDeletes.decode("content://a/1\ncontent://a/2\n");
        assertEquals(2, BulkPendingDeletes.count(s));
        assertEquals("content://a/1\ncontent://a/2", BulkPendingDeletes.encode(s));
        // khali lines + duplicate: decode safai karta hai,
        // tartib pehli-appearance wali rehti hai
        Set<String> d = BulkPendingDeletes.decode("\ncontent://a/2\n\ncontent://a/1\ncontent://a/2\n");
        assertEquals(Arrays.asList("content://a/2", "content://a/1"),
            BulkPendingDeletes.listOf(d));
        assertEquals(0, BulkPendingDeletes.count(BulkPendingDeletes.decode(null)));
        assertEquals(0, BulkPendingDeletes.count(BulkPendingDeletes.decode("")));
        assertEquals("", BulkPendingDeletes.encode(null));
        assertEquals(0, BulkPendingDeletes.count(null));
    }

    @Test
    public void pendingPlusMinus() {
        Set<String> base = BulkPendingDeletes.decode("u1\nu2\nu3");
        Set<String> added = BulkPendingDeletes.plus(base,
            Arrays.asList("u3", "u4", null, ""));
        assertEquals(Arrays.asList("u1", "u2", "u3", "u4"),
            BulkPendingDeletes.listOf(added));
        // asal set nahi badalta (plus naya set deta hai)
        assertEquals(3, BulkPendingDeletes.count(base));
        Set<String> removed = BulkPendingDeletes.minus(added, Arrays.asList("u2", "nope"));
        assertEquals(Arrays.asList("u1", "u3", "u4"), BulkPendingDeletes.listOf(removed));
        assertEquals(4, BulkPendingDeletes.count(added));
    }

    @Test
    public void pendingGhostTruthTable() {
        // ghost sirf tab jab file BHI gayab or media row BHI gayab
        assertTrue(BulkPendingDeletes.isGhost(false, false));
        assertFalse(BulkPendingDeletes.isGhost(true, false));
        assertFalse(BulkPendingDeletes.isGhost(false, true));
        assertFalse(BulkPendingDeletes.isGhost(true, true));
    }

    @Test
    public void pendingStoreKeysMatchLegacySave() {
        // purani saves (v155 tak ki) isi file/key par hain —
        // v156 wahi parhta hai, koi migration nahi
        assertEquals("bulk_wallpapers_stage", BulkPendingDeletes.PREFS);
        assertEquals("pending_move_deletes", BulkPendingDeletes.KEY);
    }

    @Test
    public void pendingNameFor() {
        assertEquals("IMG_1.jpg", BulkPendingUi.nameFor("/storage/emulated/0/DCIM/IMG_1.jpg", "content://x"));
        assertEquals("IMG_2.jpg", BulkPendingUi.nameFor(null, "content://media/external/images/media/IMG_2.jpg"));
        assertEquals("42", BulkPendingUi.nameFor("", "content://media/external/images/media/42"));
        assertEquals("?", BulkPendingUi.nameFor(null, null));
    }

    // ------------------------------------------------ pending wiring

    @Test
    public void pendingListWiring() throws Exception {
        String stage = findSource(STAGE);
        assertNotNull(stage);
        // durable list: queue/load/remove sab BulkPendingDeletes
        // se; purana upfront-wipe wala constant khatam
        assertTrue(stage.contains("BulkPendingDeletes.addAll"));
        assertTrue(stage.contains("BulkPendingDeletes.load(this)"));
        assertTrue(stage.contains("BulkPendingDeletes.removeAll"));
        assertFalse(stage.contains("KEY_PENDING_DELETES"));
        String more = findSource(MORE_SETTINGS);
        assertNotNull(more);
        assertTrue(more.contains("body.addView(pendingDeleteRow());"));
        assertTrue(more.contains("BulkPendingUi.showPendingPopup(this)"));
        assertTrue(more.contains("BulkPendingUi.pruneGhosts"));
        assertTrue(more.contains("BulkPendingDeletes.countNow(this)"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        // Home badge: trash icon + refresh + wahi popup
        assertTrue(dash.contains("refreshPendingBadge()"));
        assertTrue(dash.contains("R.drawable.ic_bulk_trash"));
        assertTrue(dash.contains("BulkPendingUi.showPendingPopup(this)"));
        assertTrue(dash.contains("BulkPendingDeletes.countNow(this)"));
    }

    // -------------------------------------------- folders kabhi rukawat nahi

    @Test
    public void fixerTemplateCreatesProjectFolder() throws Exception {
        String fixer = findSource(
            "app/src/main/java/com/termux/app/BulkProjectFixer.java");
        assertNotNull(fixer);
        // v156: template ka helper ghayab Project folder KHUD
        // banata hai — khamosh return wala rasta khatam, or
        // nakami par visible line
        assertTrue(fixer.contains("os.makedirs(proj, exist_ok=True)"));
        assertTrue(fixer.contains("Gallery copy nahi hui"));
    }

    @Test
    public void diskOnlyProjectJoinsKnownSet() {
        // Gap-2 saboot: engine folder ($HOME/mockup_project_N)
        // disk par ho lekin projects.conf me na ho, tab bhi wo
        // known set me ata hai — ensure pass usay bhi cover
        // karti hai (knownNumbers isi merge ko istemal karti hai)
        Map<Integer, String> disk = new LinkedHashMap<>();
        disk.put(22, "w22.py");
        List<BulkProjects.ProjectInfo> merged = BulkProjects.mergeProjects(
            new ArrayList<>(), disk);
        boolean found = false;
        for (BulkProjects.ProjectInfo p : merged) {
            if (p.number == 22) found = true;
        }
        assertTrue(found);
    }

    @Test
    public void ensureProjectFolders_oldLayoutGetsPairsAndBatches() throws Exception {
        // Gap-3 saboot: purane layout wala Project folder
        // (Mockups + Ready Wallpapers, Pairs nahi) — ensure ke
        // baad Pairs + Batches ban jate hain or purane
        // subfolders bilkul nahi chhute
        File root = BulkDropFolders.rootDir();
        File proj = new File(root, BulkDropFolders.dropFolderName(999));
        deleteTree(proj);
        assertTrue(new File(proj, "Mockups").mkdirs());
        assertTrue(new File(proj, "Ready Wallpapers").mkdirs());
        try {
            int[] r = BulkDropFolders.ensureProjectFolders(999);
            assertTrue(new File(proj, "Pairs").isDirectory());
            assertTrue(new File(proj, "Batches").isDirectory());
            assertTrue(new File(proj, "Mockups").isDirectory());
            assertTrue(new File(proj, "Ready Wallpapers").isDirectory());
            assertTrue(r[0] >= 2);
            // dobara ensure: sab present, kuch naya nahi banta
            int[] r2 = BulkDropFolders.ensureProjectFolders(999);
            assertEquals(0, r2[0]);
        } finally {
            deleteTree(proj);
        }
    }

    private static void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) deleteTree(k);
        }
        f.delete();
    }

    // ------------------------------------------------------------ version

    @Test
    public void versionIs156() throws Exception {
        String gradle = findSource(GRADLE);
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 156"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v156"));
    }
}
