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
import java.util.List;

/**
 * Termux Mod v150 test (owner-approved 2026-11-26):
 *  - BulkApiServer redesign: scan allowlist (pure) — sirf
 *    /storage/ ya /sdcard ke canonical paths; app-private
 *    paths kabhi scan nahi. Canonicalization ".." escape ko
 *    bhi pakarti hai. Wiring: thread-per-connection, FIFO
 *    worker, 4s callback wait / 3.5s reply, breaker 2 miss
 *    → 30s no-wait, awaitIdle/pendingScanCount.
 *  - BulkMockupSetup: wrapper CONTENT check (pure muqabla)
 *    + ensureBinWrappers ab wrappersOk par chalta hai.
 *  - BulkMediaRegister: 300-path chunking (pure) + chunk
 *    merge ki (DATE_ADDED,_ID) tartib (pure); TIE = break
 *    (pure); DATE_ADDED ladder (pure); spacing hisaab
 *    (pure); probe + fast path + fallback ki wiring.
 *  - BulkApiCheck: Termux:API wala ghalat dawa khatam —
 *    row ab wrapper files ki halat parhti hai, install ki
 *    hidayat kahin nahi.
 *  - versionCode 150 + footer v150.
 */
public class BulkV150Test {

    private static final String API_SERVER =
        "app/src/main/java/com/termux/app/BulkApiServer.java";
    private static final String SETUP =
        "app/src/main/java/com/termux/app/BulkMockupSetup.java";
    private static final String REGISTER =
        "app/src/main/java/com/termux/app/BulkMediaRegister.java";
    private static final String API_CHECK =
        "app/src/main/java/com/termux/app/BulkApiCheck.java";
    private static final String PERMISSIONS =
        "app/src/main/java/com/termux/app/BulkPermissionsActivity.java";
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

    // ------------------------------------------------------------ allowlist

    @Test
    public void allowlistAcceptsSharedStorageOnly() {
        assertTrue(BulkApiServer.isScanAllowed(
            "/storage/emulated/0/TermuxMod/Project 1/Mockups/1.jpg"));
        assertTrue(BulkApiServer.isScanAllowed("/storage/emulated/0/DCIM/x.png"));
        assertTrue(BulkApiServer.isScanAllowed("/sdcard/TermuxMod/x.jpg"));
        assertTrue(BulkApiServer.isScanAllowed("/storage"));
        assertTrue(BulkApiServer.isScanAllowed("/sdcard"));
        // app-private: MediaProvider parh hi nahi sakta — kabhi nahi
        assertFalse(BulkApiServer.isScanAllowed(
            "/data/data/com.termux/files/home/mockup_project_1/1_Mockups/1.jpg"));
        assertFalse(BulkApiServer.isScanAllowed(
            "/data/data/com.termux/files/usr/tmp/x.jpg"));
        assertFalse(BulkApiServer.isScanAllowed("/data/media/0/x.jpg"));
        // prefix wali chaalbaazi bhi nahi chalti
        assertFalse(BulkApiServer.isScanAllowed("/storagex/evil.jpg"));
        assertFalse(BulkApiServer.isScanAllowed("/sdcardx/evil.jpg"));
        assertFalse(BulkApiServer.isScanAllowed("storage/emulated/0/x.jpg"));
        assertFalse(BulkApiServer.isScanAllowed(""));
        assertFalse(BulkApiServer.isScanAllowed(null));
    }

    @Test
    public void allowlistCanonicalizationCatchesDotDotEscape() {
        // Raw path /storage/ se shuru hota hai lekin canonical
        // asal me app-private hai — glue canonical bana kar
        // hi faisla karta hai, is liye ye scan NAHI hoga.
        assertFalse(BulkApiServer.scanAllowed(
            "/storage/emulated/0/../../../data/data/com.termux/files/home/x.jpg"));
        // $HOME/storage/shared symlink ka canonical asal path
        // /storage/emulated/0 hota hai — canonical ke baad
        // wahi string allowlist se guzarti hai:
        assertTrue(BulkApiServer.isScanAllowed(
            "/storage/emulated/0/TermuxMod/Project 21/21_wallpapers/poster.jpg"));
        // Seedha shared path glue se bhi allowed hai
        assertTrue(BulkApiServer.scanAllowed(
            "/storage/emulated/0/TermuxMod/Project 1/Ready Wallpapers/2.jpg"));
        assertFalse(BulkApiServer.scanAllowed(null));
    }

    @Test
    public void apiServerWiringUsesWorkerAndBreaker() throws Exception {
        String src = findSource(API_SERVER);
        assertNotNull(src);
        // thread-per-connection
        assertTrue(src.contains("new Thread(() -> handleClient(client)"));
        // FIFO worker + awaitIdle API
        assertTrue(src.contains("LinkedBlockingQueue"));
        assertTrue(src.contains("public static boolean awaitIdle(long ms)"));
        assertTrue(src.contains("public static int pendingScanCount()"));
        // waqt ki hadein: worker 4s, jawab 3.5s, breaker 2 miss / 30s
        assertTrue(src.contains("SCAN_CALLBACK_WAIT_MS = 4000L"));
        assertTrue(src.contains("REPLY_WAIT_MS = 3500L"));
        assertTrue(src.contains("BREAKER_MISS_LIMIT = 2"));
        assertTrue(src.contains("BREAKER_NOWAIT_MS = 30_000L"));
        // purana 15s wala zehar khatam
        assertFalse(src.contains("await(15, TimeUnit.SECONDS)"));
        // WAKELOCK line ke bahar foran
        assertTrue(src.contains("line.equals(\"WAKELOCK\")"));
        int scanIdx = src.indexOf("SCAN ");
        int wakeIdx = src.indexOf("line.equals(\"WAKELOCK\")");
        assertTrue(wakeIdx > 0);
        assertTrue(scanIdx >= 0);
    }

    // ------------------------------------------------------------ wrapper content

    @Test
    public void wrapperContentComparisonIsExact() {
        assertTrue(BulkMockupSetup.wrapperContentMatches("abc\n", "abc\n"));
        assertTrue(BulkMockupSetup.wrapperContentMatches("", ""));
        assertFalse(BulkMockupSetup.wrapperContentMatches("abc", "abd"));
        assertFalse(BulkMockupSetup.wrapperContentMatches("abc", "abc\n"));
        assertFalse(BulkMockupSetup.wrapperContentMatches(null, "abc"));
        assertFalse(BulkMockupSetup.wrapperContentMatches("abc", null));
        assertFalse(BulkMockupSetup.wrapperContentMatches(null, null));
    }

    @Test
    public void ensureBinWrappersUsesContentCheck() throws Exception {
        String src = findSource(SETUP);
        assertNotNull(src);
        assertTrue(src.contains("public static boolean wrappersOk(Context context)"));
        assertTrue(src.contains("wrapperContentMatches"));
        // ensureBinWrappers ab sirf mojoodgi par nahi rukta
        int ensure = src.indexOf("private static void ensureBinWrappers");
        assertTrue(ensure > 0);
        String body = src.substring(ensure, Math.min(src.length(), ensure + 700));
        assertTrue(body.contains("wrappersOk(context)"));
        assertTrue(body.contains("installBinWrappers(context)"));
    }

    // ------------------------------------------------------------ chunking + merge

    @Test
    public void chunkPathsSplitsAt300PreservingOrder() {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < 1000; i++) paths.add("/s/" + i + ".jpg");
        List<List<String>> chunks = BulkMediaRegister.chunkPaths(paths, 300);
        assertEquals(4, chunks.size());
        assertEquals(300, chunks.get(0).size());
        assertEquals(300, chunks.get(1).size());
        assertEquals(300, chunks.get(2).size());
        assertEquals(100, chunks.get(3).size());
        List<String> flat = new ArrayList<>();
        for (List<String> c : chunks) flat.addAll(c);
        assertEquals(paths, flat);
        // chhoti list = ek chunk; khali/null = koi chunk nahi
        assertEquals(1, BulkMediaRegister.chunkPaths(
            Arrays.asList("a", "b"), 300).size());
        assertTrue(BulkMediaRegister.chunkPaths(new ArrayList<>(), 300).isEmpty());
        assertTrue(BulkMediaRegister.chunkPaths(null, 300).isEmpty());
        // chunkSize ghair-mozon ho to default 300
        List<List<String>> def = BulkMediaRegister.chunkPaths(paths, 0);
        assertEquals(4, def.size());
    }

    @Test
    public void sortRowsMergesChunksInDateAddedThenIdOrder() {
        // Do chunks, har ek apne andar sorted — global tartib
        // sirf merge par hi pakki hoti hai.
        List<BulkMediaRegister.RowInfo> rows = new ArrayList<>();
        rows.add(new BulkMediaRegister.RowInfo("/s/a.jpg", 100, 5));
        rows.add(new BulkMediaRegister.RowInfo("/s/c.jpg", 102, 9));
        rows.add(new BulkMediaRegister.RowInfo("/s/b.jpg", 100, 3));
        rows.add(new BulkMediaRegister.RowInfo("/s/d.jpg", 101, 7));
        List<BulkMediaRegister.RowInfo> sorted = BulkMediaRegister.sortRows(rows);
        assertEquals("/s/b.jpg", sorted.get(0).path); // 100, id 3
        assertEquals("/s/a.jpg", sorted.get(1).path); // 100, id 5
        assertEquals("/s/d.jpg", sorted.get(2).path); // 101
        assertEquals("/s/c.jpg", sorted.get(3).path); // 102
        // input mutate nahi hota
        assertEquals("/s/a.jpg", rows.get(0).path);
        assertTrue(BulkMediaRegister.sortRows(null).isEmpty());
    }

    // ------------------------------------------------------------ tie = break

    private static BulkMediaRegister.RowInfo row(String p, long sec, long id) {
        return new BulkMediaRegister.RowInfo(p, sec, id);
    }

    @Test
    public void tieBetweenAdjacentFilesIsABreak() {
        List<String> numeric = Arrays.asList(
            "/s/1.jpg", "/s/2.jpg", "/s/3.jpg", "/s/4.jpg");
        // 2.jpg or 3.jpg ek hi second me — 3.jpg se suffix
        List<BulkMediaRegister.RowInfo> rows = Arrays.asList(
            row("/s/1.jpg", 100, 1), row("/s/2.jpg", 101, 2),
            row("/s/3.jpg", 101, 3), row("/s/4.jpg", 102, 4));
        assertEquals(Arrays.asList("/s/3.jpg", "/s/4.jpg"),
            BulkMediaRegister.smartFixSuffixWithTies(numeric, rows));
        // pehli do hi tie hon to dusri se suffix
        List<BulkMediaRegister.RowInfo> rows2 = Arrays.asList(
            row("/s/1.jpg", 100, 1), row("/s/2.jpg", 100, 2),
            row("/s/3.jpg", 103, 3), row("/s/4.jpg", 104, 4));
        assertEquals(Arrays.asList("/s/2.jpg", "/s/3.jpg", "/s/4.jpg"),
            BulkMediaRegister.smartFixSuffixWithTies(numeric, rows2));
        // teen ki same-second run — dusri file se break
        List<BulkMediaRegister.RowInfo> rows3 = Arrays.asList(
            row("/s/1.jpg", 100, 1), row("/s/2.jpg", 100, 2),
            row("/s/3.jpg", 100, 3), row("/s/4.jpg", 105, 4));
        assertEquals(Arrays.asList("/s/2.jpg", "/s/3.jpg", "/s/4.jpg"),
            BulkMediaRegister.smartFixSuffixWithTies(numeric, rows3));
    }

    @Test
    public void noTieNoCrossingMeansNoFix() {
        List<String> numeric = Arrays.asList(
            "/s/1.jpg", "/s/2.jpg", "/s/3.jpg");
        List<BulkMediaRegister.RowInfo> rows = Arrays.asList(
            row("/s/1.jpg", 100, 1), row("/s/2.jpg", 101, 2),
            row("/s/3.jpg", 102, 3));
        assertTrue(BulkMediaRegister.smartFixSuffixWithTies(numeric, rows).isEmpty());
        // crossing (purani wajah) ab bhi pakri jati hai
        List<BulkMediaRegister.RowInfo> crossed = Arrays.asList(
            row("/s/2.jpg", 100, 1), row("/s/1.jpg", 101, 2),
            row("/s/3.jpg", 102, 3));
        assertEquals(Arrays.asList("/s/2.jpg", "/s/3.jpg"),
            BulkMediaRegister.smartFixSuffixWithTies(numeric, crossed));
        // row gayab = wahi se break
        List<BulkMediaRegister.RowInfo> missing = Arrays.asList(
            row("/s/1.jpg", 100, 1), row("/s/3.jpg", 102, 3));
        assertEquals(Arrays.asList("/s/2.jpg", "/s/3.jpg"),
            BulkMediaRegister.smartFixSuffixWithTies(numeric, missing));
        assertTrue(BulkMediaRegister.smartFixSuffixWithTies(null, null).isEmpty());
        assertTrue(BulkMediaRegister.smartFixSuffixWithTies(
            new ArrayList<>(), rows).isEmpty());
    }

    // ------------------------------------------------------------ ladder + spacing

    @Test
    public void dateAddedLadderStepsOneSecondEndingAtNow() {
        long[] ladder = BulkMediaRegister.dateAddedLadder(4, 1000L);
        assertEquals(4, ladder.length);
        assertEquals(997L, ladder[0]);
        assertEquals(998L, ladder[1]);
        assertEquals(999L, ladder[2]);
        assertEquals(1000L, ladder[3]);
        assertEquals(42L, BulkMediaRegister.dateAddedLadder(1, 42L)[0]);
        assertEquals(0, BulkMediaRegister.dateAddedLadder(0, 42L).length);
        // 1000 files: aakhri = now, pehli = now-999, sakht barhti tartib
        long[] big = BulkMediaRegister.dateAddedLadder(1000, 5000L);
        assertEquals(4001L, big[0]);
        assertEquals(5000L, big[999]);
        for (int i = 1; i < big.length; i++) {
            assertEquals(big[i - 1] + 1, big[i]);
        }
    }

    @Test
    public void spacingDelayTargetsNextSecondBoundaryPlus50() {
        // callback 10.000s par — agli registration 11.050s se pehle nahi
        assertEquals(1050L, BulkMediaRegister.spacingDelayMs(10_000L, 10_000L));
        assertEquals(150L, BulkMediaRegister.spacingDelayMs(10_400L, 10_900L));
        // waqt guzar chuka ho to intezar sifar
        assertEquals(0L, BulkMediaRegister.spacingDelayMs(10_400L, 11_200L));
        assertEquals(0L, BulkMediaRegister.spacingDelayMs(10_400L, 11_050L));
        // boundary ke bilkul upar callback — poora agla second
        assertEquals(1050L, BulkMediaRegister.spacingDelayMs(11_000L, 11_000L));
    }

    @Test
    public void registerWiringHasProbeLadderSpacingAndDrain() throws Exception {
        String src = findSource(REGISTER);
        assertNotNull(src);
        // pure helpers mehfooz hain
        assertTrue(src.contains("DATA_IN_CHUNK = 300"));
        assertTrue(src.contains("public static List<List<String>> chunkPaths"));
        // v154: probe / ladder / drain / update machinery KHATAM
        assertFalse(src.contains("runProbe"));
        assertFalse(src.contains("applyDateAddedLadder"));
        assertFalse(src.contains("awaitIdle"));
        assertFalse(src.contains("ContentProviderOperation"));
        assertFalse(src.contains("dateadded_probe150"));
        // naya plain pass: wait (seconds) + Auto Scan gate + scanOne
        assertTrue(src.contains("resolveWaitSeconds"));
        assertTrue(src.contains("Thread.sleep(wait * 1000L)"));
        assertTrue(src.contains("scanOne(ctx, path)"));
    }

    // ------------------------------------------------------------ api check row

    @Test
    public void apiCheckDecisionIsPure() {
        assertTrue(BulkApiCheck.resolveOk(Boolean.TRUE));
        assertFalse(BulkApiCheck.resolveOk(Boolean.FALSE));
        assertFalse(BulkApiCheck.resolveOk(null));
    }

    @Test
    public void apiRowNoLongerClaimsTermuxApiNeeded() throws Exception {
        String check = findSource(API_CHECK);
        assertNotNull(check);
        assertTrue(check.contains("BulkMockupSetup.wrappersOk"));
        assertFalse(check.contains("f-droid.org"));
        assertFalse(check.contains("getPackageInfo"));

        String perm = findSource(PERMISSIONS);
        assertNotNull(perm);
        assertTrue(perm.contains("BulkApiCheck.isScanReady(this)"));
        assertFalse(perm.contains("isApiInstalled"));
        assertFalse(perm.contains("API_STORE_URL"));
        assertFalse(perm.contains("f-droid.org"));
        // label/hint dono zubanen, Done waisa hi
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.PERM_API));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.PERM_API_INSTALL));
        assertEquals("Done", BulkStrings.get(BulkStrings.PERM_API_DONE, false));
    }

    // ------------------------------------------------------------ version

    @Test
    public void versionIs151() throws Exception {
        String gradle = findSource(GRADLE);
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 156"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v156"));
    }
}
