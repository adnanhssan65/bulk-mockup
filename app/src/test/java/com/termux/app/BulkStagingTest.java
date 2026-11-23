package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Termux Mod (v125) — BulkStaging ki unit tests: persist/restore, sort, lock rules, grid columns. */
public class BulkStagingTest {

    private static BulkStaging.Item item(String uri, String name, long size, long addedAt) {
        return new BulkStaging.Item(uri, name, size, addedAt);
    }

    // ------------------------------------------------------------ persist / restore

    @Test
    public void encodeDecodeRoundtrip() {
        List<BulkStaging.Item> items = Arrays.asList(
            new BulkStaging.Item("content://a/1", "one.jpg", 100, 111, "hash1"),
            new BulkStaging.Item("content://a/2", "two.png", -1, 222, ""));
        List<BulkStaging.Item> back = BulkStaging.decode(BulkStaging.encode(items));
        assertEquals(2, back.size());
        assertEquals("content://a/1", back.get(0).uri);
        assertEquals("one.jpg", back.get(0).name);
        assertEquals(100, back.get(0).size);
        assertEquals(111, back.get(0).addedAt);
        assertEquals("hash1", back.get(0).hash);
        assertEquals("", back.get(1).hash);
        assertEquals(-1, back.get(1).size);
    }

    @Test
    public void decodeLegacyLinesWithoutHash() {
        // v125 se pehle ki saved lines (4 fields) — hash khaali aana chahiye
        List<BulkStaging.Item> back = BulkStaging.decode("content://x/9\told.jpg\t55\t999");
        assertEquals(1, back.size());
        assertEquals("old.jpg", back.get(0).name);
        assertEquals("", back.get(0).hash);
    }

    @Test
    public void decodeSkipsBadLines() {
        assertTrue(BulkStaging.decode("").isEmpty());
        assertTrue(BulkStaging.decode(null).isEmpty());
        List<BulkStaging.Item> back = BulkStaging.decode("\n\tbad\ncontent://ok\tf.jpg\t1\t2\t\n");
        assertEquals(1, back.size());
        assertEquals("content://ok", back.get(0).uri);
    }

    @Test
    public void encodeEmptyIsEmptyString() {
        assertEquals("", BulkStaging.encode(new ArrayList<>()));
        assertEquals("", BulkStaging.encode(null));
    }

    // ------------------------------------------------------------ dedupe by uri

    @Test
    public void addIfAbsentByUri() {
        List<BulkStaging.Item> items = new ArrayList<>();
        assertTrue(BulkStaging.addIfAbsent(items, item("u1", "a.jpg", 1, 1)));
        assertFalse(BulkStaging.addIfAbsent(items, item("u1", "a-copy.jpg", 1, 2)));
        assertTrue(BulkStaging.addIfAbsent(items, item("u2", "b.jpg", 1, 3)));
        assertEquals(2, items.size());
        assertTrue(BulkStaging.containsUri(items, "u2"));
        assertFalse(BulkStaging.containsUri(items, "u3"));
    }

    // ------------------------------------------------------------ sort

    @Test
    public void sortByName() {
        List<BulkStaging.Item> items = Arrays.asList(
            item("u1", "banana.jpg", 10, 3),
            item("u2", "Apple.jpg", 10, 2),
            item("u3", "cherry.jpg", 10, 1));
        List<BulkStaging.Item> asc = BulkStaging.sorted(items, BulkStaging.SORT_NAME_ASC);
        assertEquals("Apple.jpg", asc.get(0).name);
        assertEquals("banana.jpg", asc.get(1).name);
        assertEquals("cherry.jpg", asc.get(2).name);
        List<BulkStaging.Item> desc = BulkStaging.sorted(items, BulkStaging.SORT_NAME_DESC);
        assertEquals("cherry.jpg", desc.get(0).name);
        assertEquals("Apple.jpg", desc.get(2).name);
        // Asal list nahi badalti
        assertEquals("banana.jpg", items.get(0).name);
    }

    @Test
    public void sortByDateAdded() {
        List<BulkStaging.Item> items = Arrays.asList(
            item("u1", "a.jpg", 10, 300),
            item("u2", "b.jpg", 10, 100),
            item("u3", "c.jpg", 10, 200));
        List<BulkStaging.Item> asc = BulkStaging.sorted(items, BulkStaging.SORT_DATE_ASC);
        assertEquals("u2", asc.get(0).uri);
        assertEquals("u1", asc.get(2).uri);
        List<BulkStaging.Item> desc = BulkStaging.sorted(items, BulkStaging.SORT_DATE_DESC);
        assertEquals("u1", desc.get(0).uri);
        assertEquals("u2", desc.get(2).uri);
    }

    @Test
    public void sortBySize() {
        List<BulkStaging.Item> items = Arrays.asList(
            item("u1", "a.jpg", 500, 1),
            item("u2", "b.jpg", 100, 2),
            item("u3", "c.jpg", -1, 3)); // size maloom nahi — ASC me aakhir me
        List<BulkStaging.Item> asc = BulkStaging.sorted(items, BulkStaging.SORT_SIZE_ASC);
        assertEquals("u2", asc.get(0).uri);
        assertEquals("u1", asc.get(1).uri);
        assertEquals("u3", asc.get(2).uri);
        List<BulkStaging.Item> desc = BulkStaging.sorted(items, BulkStaging.SORT_SIZE_DESC);
        assertEquals("u1", desc.get(1).uri);
        assertEquals("u2", desc.get(2).uri);
    }

    // ------------------------------------------------------------ lock / commit rules

    @Test
    public void commitLockedWithoutProject() {
        assertFalse(BulkStaging.canCommit(-1, 3));
        assertFalse(BulkStaging.canCommit(0, 3));
        assertFalse(BulkStaging.canRun(-1, 3, true));
    }

    @Test
    public void commitLockedWhenStagingEmpty() {
        assertFalse(BulkStaging.canCommit(5, 0));
        assertFalse(BulkStaging.canRun(5, 0, true));
    }

    @Test
    public void commitUnlockedWithProjectAndItems() {
        assertTrue(BulkStaging.canCommit(5, 1));
        assertTrue(BulkStaging.canRun(5, 1, true));
        // Run ke liye work command bhi lazmi
        assertFalse(BulkStaging.canRun(5, 1, false));
    }

    @Test
    public void commitClearsStagingEncoding() {
        // Successful Add ke baad staging clear — encode khaali list ka "" hota hai,
        // or decode us se khaali list deta hai (restore par kuch wapas nahi aata)
        List<BulkStaging.Item> cleared = new ArrayList<>();
        assertEquals("", BulkStaging.encode(cleared));
        assertTrue(BulkStaging.decode(BulkStaging.encode(cleared)).isEmpty());
    }

    // ------------------------------------------------------------ grid columns

    @Test
    public void columnsClampAndCycle() {
        assertEquals(2, BulkStaging.clampColumns(2));
        assertEquals(3, BulkStaging.clampColumns(3));
        assertEquals(4, BulkStaging.clampColumns(4));
        assertEquals(2, BulkStaging.clampColumns(1));
        assertEquals(2, BulkStaging.clampColumns(5));
        assertEquals(2, BulkStaging.clampColumns(0));
        assertEquals(3, BulkStaging.nextColumns(2));
        assertEquals(4, BulkStaging.nextColumns(3));
        assertEquals(2, BulkStaging.nextColumns(4));
        assertEquals(3, BulkStaging.nextColumns(99)); // pehle clamp, phir cycle
    }
}
