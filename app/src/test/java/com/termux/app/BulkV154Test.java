package com.termux.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

/**
 * v154 — naye pure faisle: Auto Scan / Wait Time resolvers,
 * Pairs parity counts, batch naam (default + safai), Recycle
 * Bin ke folder naam, or Pairs/Batches constants.
 */
public class BulkV154Test {

    // ------------------------------------------------------------ Auto Scan + Wait Time

    @Test
    public void autoScanDefaultOn() {
        assertTrue(BulkMediaRegister.resolveAutoScan(null));
        assertTrue(BulkMediaRegister.resolveAutoScan(Boolean.TRUE));
        assertFalse(BulkMediaRegister.resolveAutoScan(Boolean.FALSE));
    }

    @Test
    public void waitSecondsDefaultAndClamp() {
        assertEquals(0, BulkMediaRegister.DEFAULT_WAIT_SECONDS);
        assertEquals(0, BulkMediaRegister.resolveWaitSeconds(null));
        assertEquals(0, BulkMediaRegister.resolveWaitSeconds(-5));
        assertEquals(30, BulkMediaRegister.resolveWaitSeconds(30));
        assertEquals(BulkMediaRegister.MAX_WAIT_SECONDS,
            BulkMediaRegister.resolveWaitSeconds(99999));
    }

    // ------------------------------------------------------------ Pairs parity counts

    private static File touch(File dir, String name) throws Exception {
        File f = new File(dir, name);
        Files.write(f.toPath(), new byte[]{1});
        return f;
    }

    @Test
    public void pairsCountsByParity() throws Exception {
        File dir = Files.createTempDirectory("pairs").toFile();
        touch(dir, "1.jpg");   // odd  -> mockup
        touch(dir, "2.jpg");   // even -> ready
        touch(dir, "3.png");   // odd
        touch(dir, "4.webp");  // even
        touch(dir, "5.txt");   // image nahi — skip
        touch(dir, "x.jpg");   // number nahi — skip
        File sub = new File(dir, "7.jpg");
        //noinspection ResultOfMethodCallIgnored
        sub.mkdirs();          // directory — skip
        assertArrayEquals(new int[]{2, 2}, BulkProjects.pairsCounts(dir));
    }

    @Test
    public void pairsCountsMissingDir() {
        assertArrayEquals(new int[]{0, 0}, BulkProjects.pairsCounts(null));
        assertArrayEquals(new int[]{0, 0},
            BulkProjects.pairsCounts(new File("/nonexistent-pairs-xyz")));
    }

    // ------------------------------------------------------------ batch name

    @Test
    public void defaultBatchNameCountsSubfolders() throws Exception {
        File wh = Files.createTempDirectory("warehouse").toFile();
        //noinspection ResultOfMethodCallIgnored
        new File(wh, "P3_Batch_1").mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(wh, "P3_Batch_2").mkdirs();
        touch(wh, "loose.txt"); // file ginti me nahi
        assertEquals("P3_Batch_3", BulkProjects.defaultBatchName(wh, 3));
        assertEquals("P3_Batch_1", BulkProjects.defaultBatchName(null, 3));
        assertEquals("P9_Batch_1",
            BulkProjects.defaultBatchName(new File("/nonexistent-wh-xyz"), 9));
    }

    @Test
    public void sanitizeBatchNameCleansSeparators() {
        assertEquals("My Batch", BulkProjects.sanitizeBatchName("My Batch"));
        assertEquals("a_b_c", BulkProjects.sanitizeBatchName("a/b\\c"));
        assertEquals("x", BulkProjects.sanitizeBatchName("..x"));
        assertEquals("pad", BulkProjects.sanitizeBatchName("  pad  "));
        assertEquals("", BulkProjects.sanitizeBatchName(null));
        assertEquals("", BulkProjects.sanitizeBatchName("   "));
    }

    // ------------------------------------------------------------ recycle bin naming

    @Test
    public void binFolderNameRoundTrip() {
        assertEquals("mockup_project_7", BulkRecycleBin.binFolderName(7));
        assertEquals(7, BulkRecycleBin.parseBinFolder("mockup_project_7"));
        assertEquals(21, BulkRecycleBin.parseBinFolder("mockup_project_21"));
        assertEquals(-1, BulkRecycleBin.parseBinFolder("mockup_project_x"));
        assertEquals(-1, BulkRecycleBin.parseBinFolder("mockup_project_0"));
        assertEquals(-1, BulkRecycleBin.parseBinFolder("something_else"));
        assertEquals(-1, BulkRecycleBin.parseBinFolder(null));
    }

    // ------------------------------------------------------------ folder constants

    @Test
    public void pairsAndBatchesConstants() {
        assertEquals("Pairs", BulkMirror.PAIRS_SUB);
        assertEquals("Batches", BulkMirror.BATCHES_SUB);
    }
}
