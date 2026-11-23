package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Termux Mod (v127) — gallery drag-select range ki tests. */
public class BulkGallerySelectTest {

    @Test
    public void rangeBothDirections() {
        assertEquals(new HashSet<>(Arrays.asList(2, 3, 4, 5)), BulkGallerySelect.range(2, 5));
        assertEquals(new HashSet<>(Arrays.asList(2, 3, 4, 5)), BulkGallerySelect.range(5, 2));
        assertEquals(new HashSet<>(Arrays.asList(7)), BulkGallerySelect.range(7, 7));
    }

    @Test
    public void rangeRejectsNegative() {
        assertTrue(BulkGallerySelect.range(-1, 4).isEmpty());
        assertTrue(BulkGallerySelect.range(3, -2).isEmpty());
    }

    @Test
    public void dragSelectionUnionsBaseAndRange() {
        Set<Integer> base = new HashSet<>(Arrays.asList(0, 20));
        Set<Integer> out = BulkGallerySelect.dragSelection(base, 3, 5);
        assertEquals(new HashSet<>(Arrays.asList(0, 3, 4, 5, 20)), out);
        // base set mutate nahi hona chahiye
        assertEquals(new HashSet<>(Arrays.asList(0, 20)), base);
    }

    @Test
    public void dragSelectionNullBase() {
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), BulkGallerySelect.dragSelection(null, 1, 2));
    }
}
