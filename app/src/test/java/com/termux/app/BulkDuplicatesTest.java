package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Termux Mod (v125) — BulkDuplicates ki unit tests: dedupe faisla, expiry faisla, index persist. */
public class BulkDuplicatesTest {

    private static final long HOUR = 3600_000L;
    private static final long NOW = 1_700_000_000_000L;

    // ------------------------------------------------------------ dedupe decision

    @Test
    public void duplicateWhenHashInStaging() {
        Set<String> staged = new HashSet<>(Arrays.asList("aaa", "bbb"));
        assertTrue(BulkDuplicates.isDuplicate(staged, new HashSet<>(), "bbb"));
    }

    @Test
    public void duplicateWhenHashInSameBatch() {
        Set<String> batch = new HashSet<>(Arrays.asList("ccc"));
        assertTrue(BulkDuplicates.isDuplicate(new HashSet<>(), batch, "ccc"));
    }

    @Test
    public void notDuplicateWhenHashNew() {
        Set<String> staged = new HashSet<>(Arrays.asList("aaa"));
        assertFalse(BulkDuplicates.isDuplicate(staged, new HashSet<>(), "zzz"));
        assertFalse(BulkDuplicates.isDuplicate(null, null, "zzz"));
    }

    @Test
    public void notDuplicateWhenHashUnknown() {
        Set<String> staged = new HashSet<>(Arrays.asList("aaa"));
        assertFalse(BulkDuplicates.isDuplicate(staged, new HashSet<>(), ""));
        assertFalse(BulkDuplicates.isDuplicate(staged, new HashSet<>(), null));
    }

    // ------------------------------------------------------------ expiry decision

    @Test
    public void defaultTtlIs24Hours() {
        assertEquals(24, BulkDuplicates.DEFAULT_TTL_HOURS);
    }

    @Test
    public void expiryBoundaryAt24Hours() {
        assertFalse(BulkDuplicates.isExpired(NOW - 23 * HOUR, NOW, 24));
        assertFalse(BulkDuplicates.isExpired(NOW - 24 * HOUR + 1, NOW, 24));
        assertTrue(BulkDuplicates.isExpired(NOW - 24 * HOUR, NOW, 24));
        assertTrue(BulkDuplicates.isExpired(NOW - 72 * HOUR, NOW, 24));
    }

    @Test
    public void expiryRespectsCustomTtl() {
        assertTrue(BulkDuplicates.isExpired(NOW - 2 * HOUR, NOW, 1));
        assertFalse(BulkDuplicates.isExpired(NOW - 30 * HOUR, NOW, 48));
        assertTrue(BulkDuplicates.isExpired(NOW - 8 * 24 * HOUR, NOW, 168));
    }

    @Test
    public void expiryOffWhenTtlZeroOrNegative() {
        assertFalse(BulkDuplicates.isExpired(NOW - 365 * 24 * HOUR, NOW, 0));
        assertFalse(BulkDuplicates.isExpired(NOW - 365 * 24 * HOUR, NOW, -5));
    }

    @Test
    public void expiryIgnoresMissingArrival() {
        assertFalse(BulkDuplicates.isExpired(0, NOW, 24));
    }

    @Test
    public void expiredAndRemainingPartition() {
        List<BulkDuplicates.Entry> entries = Arrays.asList(
            new BulkDuplicates.Entry("h1", "old.jpg", NOW - 30 * HOUR, "f1"),
            new BulkDuplicates.Entry("h2", "new.jpg", NOW - 2 * HOUR, "f2"),
            new BulkDuplicates.Entry("h3", "older.jpg", NOW - 90 * HOUR, "f3"));
        List<BulkDuplicates.Entry> expired = BulkDuplicates.expiredEntries(entries, NOW, 24);
        assertEquals(2, expired.size());
        assertEquals("f1", expired.get(0).fileName);
        assertEquals("f3", expired.get(1).fileName);
        List<BulkDuplicates.Entry> remaining = BulkDuplicates.remainingEntries(entries, NOW, 24);
        assertEquals(1, remaining.size());
        assertEquals("f2", remaining.get(0).fileName);
        // TTL band ho to kuch expire nahi hota
        assertTrue(BulkDuplicates.expiredEntries(entries, NOW, 0).isEmpty());
        assertEquals(3, BulkDuplicates.remainingEntries(entries, NOW, 0).size());
    }

    // ------------------------------------------------------------ index persist

    @Test
    public void encodeDecodeRoundtrip() {
        List<BulkDuplicates.Entry> entries = Arrays.asList(
            new BulkDuplicates.Entry("abc123", "photo one.jpg", 123456, "123456_photo_one.jpg"),
            new BulkDuplicates.Entry("def456", "two.png", 999, "999_two.png"));
        List<BulkDuplicates.Entry> back = BulkDuplicates.decode(BulkDuplicates.encode(entries));
        assertEquals(2, back.size());
        assertEquals("abc123", back.get(0).hash);
        assertEquals("photo one.jpg", back.get(0).name);
        assertEquals(123456, back.get(0).arrivalAt);
        assertEquals("123456_photo_one.jpg", back.get(0).fileName);
        assertEquals("def456", back.get(1).hash);
    }

    @Test
    public void decodeSkipsBadLines() {
        assertTrue(BulkDuplicates.decode("").isEmpty());
        assertTrue(BulkDuplicates.decode(null).isEmpty());
        // fileName ke baghair wali line skip; durust line rehti hai
        List<BulkDuplicates.Entry> back =
            BulkDuplicates.decode("h1\tname.jpg\t5\n" + "h2\tok.jpg\t7\tfile2");
        assertEquals(1, back.size());
        assertEquals("file2", back.get(0).fileName);
        assertEquals(7, back.get(0).arrivalAt);
    }
}
