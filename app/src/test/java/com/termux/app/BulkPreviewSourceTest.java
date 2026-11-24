package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Termux Mod (v132) — frame preview source rule + project search filter ki tests. */
public class BulkPreviewSourceTest {

    private static BulkPreviewSource.Entry e(String name, long mtime) {
        return new BulkPreviewSource.Entry(name, mtime);
    }

    private static List<BulkPreviewSource.Entry> list(BulkPreviewSource.Entry... es) {
        return new ArrayList<>(Arrays.asList(es));
    }

    // ------------------------------------------------------------ source rule

    @Test
    public void newestRealMockupWins() {
        BulkPreviewSource.Choice c = BulkPreviewSource.choose(
            list(e("1.jpg", 100), e("2.jpg", 300), e("3.jpg", 200)), 5, true);
        assertEquals(BulkPreviewSource.KIND_REAL, c.kind);
        assertEquals("2.jpg", c.fileName);
    }

    @Test
    public void tieBreakIsDeterministic() {
        BulkPreviewSource.Choice c = BulkPreviewSource.choose(
            list(e("a.jpg", 500), e("b.jpg", 500)), 5, true);
        assertEquals(BulkPreviewSource.KIND_REAL, c.kind);
        assertEquals("b.jpg", c.fileName);
    }

    @Test
    public void hiddenAndVideoFilesNeverWin() {
        BulkPreviewSource.Choice c = BulkPreviewSource.choose(
            list(e(".keep", 9999), e(".hidden.jpg", 9999), e("clip.mp4", 9999),
                e("real.png", 10)), 5, true);
        assertEquals(BulkPreviewSource.KIND_REAL, c.kind);
        assertEquals("real.png", c.fileName);
    }

    @Test
    public void onlyJunkInMockupsFallsToBundled() {
        BulkPreviewSource.Choice c = BulkPreviewSource.choose(
            list(e(".keep", 9999), e("clip.mp4", 9999)), 5, true);
        assertEquals(BulkPreviewSource.KIND_BUNDLED, c.kind);
        assertNull(c.fileName);
    }

    @Test
    public void emptyMockupsBundledForOneToFourteen() {
        assertEquals(BulkPreviewSource.KIND_BUNDLED,
            BulkPreviewSource.choose(new ArrayList<>(), 1, true).kind);
        assertEquals(BulkPreviewSource.KIND_BUNDLED,
            BulkPreviewSource.choose(null, 14, true).kind);
    }

    @Test
    public void emptyMockupsPlaceholderBeyondBundled() {
        assertEquals(BulkPreviewSource.KIND_PLACEHOLDER,
            BulkPreviewSource.choose(new ArrayList<>(), 15, true).kind);
        assertEquals(BulkPreviewSource.KIND_PLACEHOLDER,
            BulkPreviewSource.choose(new ArrayList<>(), 17, true).kind);
    }

    @Test
    public void bundledUnavailableMeansPlaceholder() {
        assertEquals(BulkPreviewSource.KIND_PLACEHOLDER,
            BulkPreviewSource.choose(new ArrayList<>(), 3, false).kind);
    }

    @Test
    public void unknownNumberNeverBundled() {
        assertEquals(BulkPreviewSource.KIND_PLACEHOLDER,
            BulkPreviewSource.choose(new ArrayList<>(), -1, true).kind);
        assertEquals(BulkPreviewSource.KIND_PLACEHOLDER,
            BulkPreviewSource.choose(new ArrayList<>(), 0, true).kind);
    }

    @Test
    public void realMockupBeatsBundledEvenForNewProjects() {
        BulkPreviewSource.Choice c = BulkPreviewSource.choose(
            list(e("9.jpg", 42)), 17, false);
        assertEquals(BulkPreviewSource.KIND_REAL, c.kind);
        assertEquals("9.jpg", c.fileName);
    }

    @Test
    public void bundledAssetPathShape() {
        assertEquals("bulk/previews/p1.jpg", BulkPreviewSource.bundledAssetPath(1));
        assertEquals("bulk/previews/p14.jpg", BulkPreviewSource.bundledAssetPath(14));
        assertTrue(BulkPreviewSource.hasBundledRange(1));
        assertTrue(BulkPreviewSource.hasBundledRange(14));
        assertFalse(BulkPreviewSource.hasBundledRange(15));
        assertFalse(BulkPreviewSource.hasBundledRange(0));
    }

    // ------------------------------------------------------------ search filter

    @Test
    public void emptyQueryMatchesEverything() {
        assertTrue(BulkPreviewSource.matchesQuery("Project 3", 3, ""));
        assertTrue(BulkPreviewSource.matchesQuery("Project 3", 3, "   "));
        assertTrue(BulkPreviewSource.matchesQuery("Project 3", 3, null));
    }

    @Test
    public void numberQueryMatches() {
        assertTrue(BulkPreviewSource.matchesQuery("Project 10", 10, "10"));
        assertTrue(BulkPreviewSource.matchesQuery("Project 10", 10, "1"));
        assertFalse(BulkPreviewSource.matchesQuery("Project 9", 9, "10"));
    }

    @Test
    public void nameQueryCaseInsensitive() {
        assertTrue(BulkPreviewSource.matchesQuery("Project 7", 7, "PROJ"));
        assertTrue(BulkPreviewSource.matchesQuery("Project 7", 7, "project 7"));
        assertFalse(BulkPreviewSource.matchesQuery("Project 7", 7, "xyz"));
    }

    @Test
    public void queryTrimmed() {
        assertTrue(BulkPreviewSource.matchesQuery("Project 12", 12, "  12  "));
    }
}
