package com.termux.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * v155 — naye pure faisle: Status strip/popup ka data
 * (entries filter, compact text, do-column rows), Auto
 * Recreate ke guard hisaab, or Recreate count fix
 * (recreateAll = sirf drop set ki ginti, koi phuli hui
 * dusri ginti nahi).
 */
public class BulkV155Test {

    // ------------------------------------------------------------ Status entries

    @Test
    public void statusEntriesOnlyPositive() {
        List<int[]> e = BulkProjectsActivity.statusEntries(
            new int[]{1, 2, 18, 20}, new int[]{6, 0, 5, 3}, true);
        assertEquals(3, e.size());
        assertArrayEquals(new int[]{1, 6}, e.get(0));
        assertArrayEquals(new int[]{18, 5}, e.get(1));
        assertArrayEquals(new int[]{20, 3}, e.get(2));
    }

    @Test
    public void statusEntriesAllKeepsZeros() {
        List<int[]> e = BulkProjectsActivity.statusEntries(
            new int[]{1, 2, 3}, new int[]{6, 0, 0}, false);
        assertEquals(3, e.size());
        assertArrayEquals(new int[]{2, 0}, e.get(1));
        assertArrayEquals(new int[]{3, 0}, e.get(2));
    }

    @Test
    public void statusEntriesSkipsBadNumbersAndNulls() {
        assertTrue(BulkProjectsActivity.statusEntries(null, null, true).isEmpty());
        assertTrue(BulkProjectsActivity.statusEntries(
            new int[]{1}, null, true).isEmpty());
        List<int[]> e = BulkProjectsActivity.statusEntries(
            new int[]{0, -4, 7}, new int[]{9, 9, 2}, false);
        assertEquals(1, e.size());
        assertArrayEquals(new int[]{7, 2}, e.get(0));
        // lambai ka farq: chhoti wali lambai tak hi
        List<int[]> m = BulkProjectsActivity.statusEntries(
            new int[]{1, 2, 3}, new int[]{4}, false);
        assertEquals(1, m.size());
    }

    // ------------------------------------------------------------ Status strip text

    @Test
    public void statusStripTextCompact() {
        List<int[]> e = BulkProjectsActivity.statusEntries(
            new int[]{1, 18, 20}, new int[]{6, 5, 5}, true);
        assertEquals("P1 6 · P18 5 · P20 5",
            BulkProjectsActivity.statusStripText(e));
        assertEquals("", BulkProjectsActivity.statusStripText(null));
        assertEquals("", BulkProjectsActivity.statusStripText(
            BulkProjectsActivity.statusEntries(
                new int[]{1}, new int[]{0}, true)));
    }

    // ------------------------------------------------------------ Status popup rows

    @Test
    public void statusPairRowsTwoPerRow() {
        List<int[]> e = BulkProjectsActivity.statusEntries(
            new int[]{1, 2, 3, 4, 5}, new int[]{6, 0, 0, 2, 0}, false);
        List<List<int[]>> rows = BulkProjectsActivity.statusPairRows(e);
        assertEquals(3, rows.size());
        assertEquals(2, rows.get(0).size());
        assertEquals(2, rows.get(1).size());
        assertEquals(1, rows.get(2).size());
        assertArrayEquals(new int[]{5, 0}, rows.get(2).get(0));
        assertTrue(BulkProjectsActivity.statusPairRows(null).isEmpty());
        assertTrue(BulkProjectsActivity.statusPairRows(
            BulkProjectsActivity.statusEntries(null, null, false)).isEmpty());
    }

    // ------------------------------------------------------------ Auto Recreate guards

    @Test
    public void ensureProjectFoldersRejectsBadNumbers() {
        // storage ko hath lagaye baghair: ghalat number par
        // koi folder nahi banta, count {0, 0}
        assertArrayEquals(new int[]{0, 0}, BulkDropFolders.ensureProjectFolders(0));
        assertArrayEquals(new int[]{0, 0}, BulkDropFolders.ensureProjectFolders(-7));
    }

    // ------------------------------------------------------------ Recreate count fix

    @Test
    public void recreateAllCountsOnlyTheDropSet() {
        // Regression: purana recreateAll TermuxMod ke andar
        // engine-naam wale subfolders gin kar "present" phula
        // deta tha (phone par 121). Ab recreateAll bilkul wahi
        // hai jo ensureDropFoldersFull hai — usi set ki ginti.
        // created/present ki taqseem call-order par hai (jo
        // pehle chalaye root bana lega, dusra use present
        // payega), is liye muqabla KUL tadaad ka hai.
        Set<Integer> none = new HashSet<>();
        int[] a = BulkDropFolders.recreateAll(none);
        int[] b = BulkDropFolders.ensureDropFoldersFull(none);
        assertEquals(a[0] + a[1], b[0] + b[1]);
        int[] c = BulkDropFolders.recreateAll(null);
        int[] d = BulkDropFolders.ensureDropFoldersFull(null);
        assertEquals(c[0] + c[1], d[0] + d[1]);
    }
}
