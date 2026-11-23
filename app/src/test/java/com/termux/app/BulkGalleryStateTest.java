package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Termux Mod (v130) — gallery toolbar (sort/grid/mode) ki pure logic tests. */
public class BulkGalleryStateTest {

    private static BulkGalleryState.Entry e(long id, String name, long date) {
        return new BulkGalleryState.Entry(id, name, date);
    }

    private static List<Long> ids(List<BulkGalleryState.Entry> list) {
        List<Long> out = new ArrayList<>();
        for (BulkGalleryState.Entry en : list) out.add(en.id);
        return out;
    }

    @Test
    public void sortCycleNewestOldestName() {
        assertEquals(BulkGalleryState.SORT_OLDEST, BulkGalleryState.nextSort(BulkGalleryState.SORT_NEWEST));
        assertEquals(BulkGalleryState.SORT_NAME, BulkGalleryState.nextSort(BulkGalleryState.SORT_OLDEST));
        assertEquals(BulkGalleryState.SORT_NEWEST, BulkGalleryState.nextSort(BulkGalleryState.SORT_NAME));
        // Ghalat value mehfooz tor par Newest par wapas
        assertEquals(BulkGalleryState.SORT_NEWEST, BulkGalleryState.nextSort(99));
    }

    @Test
    public void columnsCycleTwoToSix() {
        assertEquals(3, BulkGalleryState.nextColumns(2));
        assertEquals(4, BulkGalleryState.nextColumns(3));
        assertEquals(5, BulkGalleryState.nextColumns(4));
        assertEquals(6, BulkGalleryState.nextColumns(5));
        assertEquals(2, BulkGalleryState.nextColumns(6));
    }

    @Test
    public void clampColumnsRange() {
        assertEquals(2, BulkGalleryState.clampColumns(2));
        assertEquals(6, BulkGalleryState.clampColumns(6));
        assertEquals(2, BulkGalleryState.clampColumns(1));
        assertEquals(2, BulkGalleryState.clampColumns(7));
        assertEquals(2, BulkGalleryState.clampColumns(0));
        // Cycle ghalat value se bhi mehfooz
        assertEquals(3, BulkGalleryState.nextColumns(99));
    }

    @Test
    public void sortedNewestFirst() {
        List<BulkGalleryState.Entry> in = Arrays.asList(
            e(1, "a.jpg", 100), e(2, "b.jpg", 300), e(3, "c.jpg", 200));
        assertEquals(Arrays.asList(2L, 3L, 1L),
            ids(BulkGalleryState.sorted(in, BulkGalleryState.SORT_NEWEST)));
    }

    @Test
    public void sortedOldestFirst() {
        List<BulkGalleryState.Entry> in = Arrays.asList(
            e(1, "a.jpg", 100), e(2, "b.jpg", 300), e(3, "c.jpg", 200));
        assertEquals(Arrays.asList(1L, 3L, 2L),
            ids(BulkGalleryState.sorted(in, BulkGalleryState.SORT_OLDEST)));
    }

    @Test
    public void sortedByNameCaseInsensitive() {
        List<BulkGalleryState.Entry> in = Arrays.asList(
            e(1, "banana.jpg", 100), e(2, "Apple.jpg", 300), e(3, "cherry.jpg", 200));
        assertEquals(Arrays.asList(2L, 1L, 3L),
            ids(BulkGalleryState.sorted(in, BulkGalleryState.SORT_NAME)));
    }

    @Test
    public void sortedTiesDeterministicAndInputUntouched() {
        List<BulkGalleryState.Entry> in = new ArrayList<>(Arrays.asList(
            e(5, "same.jpg", 100), e(9, "same.jpg", 100), e(7, "same.jpg", 100)));
        // Newest: date tie -> bari id pehle; Name: name tie -> chhoti id pehle
        assertEquals(Arrays.asList(9L, 7L, 5L),
            ids(BulkGalleryState.sorted(in, BulkGalleryState.SORT_NEWEST)));
        assertEquals(Arrays.asList(5L, 7L, 9L),
            ids(BulkGalleryState.sorted(in, BulkGalleryState.SORT_NAME)));
        // Asal list nahi badli
        assertEquals(Arrays.asList(5L, 9L, 7L), ids(in));
    }

    @Test
    public void sortedHandlesNullAndEmpty() {
        assertTrue(BulkGalleryState.sorted(null, BulkGalleryState.SORT_NEWEST).isEmpty());
        assertTrue(BulkGalleryState.sorted(new ArrayList<>(), BulkGalleryState.SORT_NAME).isEmpty());
    }

    @Test
    public void allSelectedRule() {
        assertTrue(BulkGalleryState.isAllSelected(5, 5));
        assertFalse(BulkGalleryState.isAllSelected(4, 5));
        assertFalse(BulkGalleryState.isAllSelected(0, 0));
        assertFalse(BulkGalleryState.isAllSelected(0, 5));
    }

    @Test
    public void sortFilesByNameAndDate() throws Exception {
        java.io.File dir = java.nio.file.Files.createTempDirectory("bulkgallery").toFile();
        try {
            java.io.File a = new java.io.File(dir, "a.jpg");
            java.io.File b = new java.io.File(dir, "b.jpg");
            java.io.File c = new java.io.File(dir, "c.jpg");
            for (java.io.File f : Arrays.asList(a, b, c)) {
                assertTrue(f.createNewFile());
            }
            assertTrue(a.setLastModified(100_000L));
            assertTrue(b.setLastModified(300_000L));
            assertTrue(c.setLastModified(200_000L));
            List<java.io.File> in = Arrays.asList(c, a, b);
            assertEquals(Arrays.asList(a, b, c),
                BulkGalleryState.sortFiles(in, BulkGalleryState.SORT_NAME));
            assertEquals(Arrays.asList(b, c, a),
                BulkGalleryState.sortFiles(in, BulkGalleryState.SORT_NEWEST));
            assertEquals(Arrays.asList(a, c, b),
                BulkGalleryState.sortFiles(in, BulkGalleryState.SORT_OLDEST));
            // Asal list nahi badli
            assertEquals(Arrays.asList(c, a, b), in);
            assertTrue(BulkGalleryState.sortFiles(null, BulkGalleryState.SORT_NAME).isEmpty());
        } finally {
            for (java.io.File f : dir.listFiles()) f.delete();
            dir.delete();
        }
    }
}
