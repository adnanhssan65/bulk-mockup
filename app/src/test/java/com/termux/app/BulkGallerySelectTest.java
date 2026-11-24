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

    @Test
    public void paintSweepGrowsAndShrinksWithFinger() {
        // v129 paint-select: ungli aage jaye to range barhta hai,
        // peeche aye to wohi range wapas sikurta hai (base mehfooz)
        Set<Integer> base = new HashSet<>(Arrays.asList(30));
        Set<Integer> swept = BulkGallerySelect.dragSelection(base, 2, 7);
        assertEquals(new HashSet<>(Arrays.asList(2, 3, 4, 5, 6, 7, 30)), swept);
        Set<Integer> back = BulkGallerySelect.dragSelection(base, 2, 4);
        assertEquals(new HashSet<>(Arrays.asList(2, 3, 4, 30)), back);
        // Ungli anchor se ulta (upar ki taraf) — range phir bhi sahi
        Set<Integer> up = BulkGallerySelect.dragSelection(base, 7, 3);
        assertEquals(new HashSet<>(Arrays.asList(3, 4, 5, 6, 7, 30)), up);
    }

    // ------------------------------------------------------------ v132 paint target

    @Test
    public void paintTargetIsAnchorToggledState() {
        // Anchor unselected tha -> paint select karega
        assertEquals(true, BulkGallerySelect.paintTargetState(false, false));
        // Anchor selected tha -> paint unselect karega
        assertEquals(false, BulkGallerySelect.paintTargetState(true, false));
    }

    @Test
    public void paintTargetLockedNeverRemoves() {
        // Lock on: anchor selected ho tab bhi paint sirf select karega
        assertEquals(true, BulkGallerySelect.paintTargetState(true, true));
        assertEquals(true, BulkGallerySelect.paintTargetState(false, true));
    }

    @Test
    public void paintSelectionAppliesTargetAcrossRange() {
        Set<Integer> base = new HashSet<>(Arrays.asList(0, 3, 4, 30));
        // Deselect-paint: range 2..5 se selected hat jate hain, bahar wale mehfooz
        Set<Integer> out = BulkGallerySelect.paintSelection(base, 2, 5, false);
        assertEquals(new HashSet<>(Arrays.asList(0, 30)), out);
        // Select-paint: range ke tamam select
        Set<Integer> out2 = BulkGallerySelect.paintSelection(base, 2, 5, true);
        assertEquals(new HashSet<>(Arrays.asList(0, 2, 3, 4, 5, 30)), out2);
        // base mutate nahi hua
        assertEquals(new HashSet<>(Arrays.asList(0, 3, 4, 30)), base);
    }

    @Test
    public void paintSelectionShrinksBackToBase() {
        // Ungli peeche aye to range se bahar hue cells base halat par wapas
        Set<Integer> base = new HashSet<>(Arrays.asList(9));
        Set<Integer> wide = BulkGallerySelect.paintSelection(base, 2, 6, true);
        assertEquals(new HashSet<>(Arrays.asList(2, 3, 4, 5, 6, 9)), wide);
        Set<Integer> narrow = BulkGallerySelect.paintSelection(base, 2, 3, true);
        assertEquals(new HashSet<>(Arrays.asList(2, 3, 9)), narrow);
        // Deselect-paint wapas aye to base ke selected cells phir mehfooz
        Set<Integer> base2 = new HashSet<>(Arrays.asList(4, 9));
        Set<Integer> un = BulkGallerySelect.paintSelection(base2, 2, 6, false);
        assertEquals(new HashSet<>(Arrays.asList(9)), un);
        Set<Integer> unBack = BulkGallerySelect.paintSelection(base2, 2, 3, false);
        assertEquals(new HashSet<>(Arrays.asList(4, 9)), unBack);
    }

    // ------------------------------------------------------------ v132 lock / clear

    @Test
    public void lockedTapCannotUnselect() {
        // Lock on + selected -> selected hi rehta hai (koi tabdeeli nahi)
        assertEquals(true, BulkGallerySelect.toggledState(true, true));
        // Lock on + unselected -> select ho jata hai (add hamesha chalta hai)
        assertEquals(true, BulkGallerySelect.toggledState(false, true));
    }

    @Test
    public void unlockedTapTogglesNormally() {
        assertEquals(false, BulkGallerySelect.toggledState(true, false));
        assertEquals(true, BulkGallerySelect.toggledState(false, false));
    }

    @Test
    public void clearAlwaysEmpties() {
        assertTrue(BulkGallerySelect.clearedSelection().isEmpty());
    }
}
