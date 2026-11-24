package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod v141 — sequence fix:
 *  (1) Runners P2/P3/P4/P5/P7/P8/P9/P10 ka time tracker P1 wala
 *      hi SHARED .mockup_time_tracker.txt (bundled zips ke andar —
 *      ye tabke sirf mirror ordering ke pure helper ko test karta
 *      hai; runner tabdeeliyan VM par asal runs se verify hoti
 *      hain).
 *  (2) App mirror: copies ab poora Mockups block phir poora Ready
 *      block nahi — BulkMirror.interleavedCopyOrder se jora-war
 *      mixed tartib (Sequence tab wali jora tareef: naam tartib
 *      me nth mockup + nth ready; jora mockup ki mtime se hilta
 *      hai), har copy ke baad foran media scan — mirror copies
 *      ka DATE_ADDED bhi jora rakhta hai.
 */
public class BulkV141Test {

    private static Map<String, Long> mtimes(Object... kv) {
        Map<String, Long> out = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put((String) kv[i], ((Number) kv[i + 1]).longValue());
        }
        return out;
    }

    private static Set<String> set(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    private static List<String> steps(List<BulkMirror.CopyStep> order) {
        List<String> out = new ArrayList<>();
        for (BulkMirror.CopyStep s : order) {
            out.add((s.mockup ? "M:" : "R:") + s.name);
        }
        return out;
    }

    @Test
    public void mirrorCopyOrderInterleavesPairs() {
        // Aam jora case: mockups 1,3 (mtimes 100,220), readys 2,4
        // (160,280) — tartib M1,R2,M3,R4 hi banni chahiye.
        List<BulkMirror.CopyStep> order = BulkMirror.interleavedCopyOrder(
            Arrays.asList("1.jpg", "3.jpg"), mtimes("1.jpg", 100, "3.jpg", 220),
            Arrays.asList("2.jpg", "4.jpg"), mtimes("2.jpg", 160, "4.jpg", 280),
            set("1.jpg", "3.jpg"), set("2.jpg", "4.jpg"));
        assertEquals(Arrays.asList("M:1.jpg", "R:2.jpg", "M:3.jpg", "R:4.jpg"),
            steps(order));
    }

    @Test
    public void mirrorCopyOrderPairsMoveByMockupMtime() {
        // Naam tartib ka jora (1<->2, 3<->4) tootta nahi, lekin
        // joron ki tartib mockup ki mtime hai: 3 wala jora pehle
        // bana tha, wo pehle copy hoga.
        List<BulkMirror.CopyStep> order = BulkMirror.interleavedCopyOrder(
            Arrays.asList("1.jpg", "3.jpg"), mtimes("1.jpg", 500, "3.jpg", 100),
            Arrays.asList("2.jpg", "4.jpg"), mtimes("2.jpg", 560, "4.jpg", 160),
            set("1.jpg", "3.jpg"), set("2.jpg", "4.jpg"));
        assertEquals(Arrays.asList("M:3.jpg", "R:4.jpg", "M:1.jpg", "R:2.jpg"),
            steps(order));
    }

    @Test
    public void mirrorCopyOrderPartialCopyKeepsPairPositions() {
        // Sirf badli hui files copy hoti hain — R2 or M3 — lekin
        // un ki jora-wali jagah wohi rehti hai (R2 pehle, M3 baad).
        List<BulkMirror.CopyStep> order = BulkMirror.interleavedCopyOrder(
            Arrays.asList("1.jpg", "3.jpg"), mtimes("1.jpg", 100, "3.jpg", 220),
            Arrays.asList("2.jpg", "4.jpg"), mtimes("2.jpg", 160, "4.jpg", 280),
            set("3.jpg"), set("2.jpg"));
        assertEquals(Arrays.asList("R:2.jpg", "M:3.jpg"), steps(order));
    }

    @Test
    public void mirrorCopyOrderUnevenCounts() {
        // Collage wala case: 1 mockup, kai ready. Ready-only joron
        // ki chabi un ki apni mtime hai — tartib waqt ke hisab se.
        List<BulkMirror.CopyStep> order = BulkMirror.interleavedCopyOrder(
            Arrays.asList("1.jpg"), mtimes("1.jpg", 100),
            Arrays.asList("2.jpg", "3.jpg", "4.jpg"),
            mtimes("2.jpg", 160, "3.jpg", 220, "4.jpg", 280),
            set("1.jpg"), set("2.jpg", "3.jpg", "4.jpg"));
        assertEquals(
            Arrays.asList("M:1.jpg", "R:2.jpg", "R:3.jpg", "R:4.jpg"),
            steps(order));
    }

    @Test
    public void mirrorCopyOrderEmptyAndNull() {
        assertTrue(BulkMirror.interleavedCopyOrder(
            new ArrayList<>(), new HashMap<>(),
            new ArrayList<>(), new HashMap<>(),
            set(), set()).isEmpty());
        assertTrue(BulkMirror.interleavedCopyOrder(
            null, null, null, null, null, null).isEmpty());
        // Copy list khaali ho to steps bhi khaali — purani files
        // dobara copy nahi hotin.
        assertTrue(BulkMirror.interleavedCopyOrder(
            Arrays.asList("1.jpg"), mtimes("1.jpg", 100),
            Arrays.asList("2.jpg"), mtimes("2.jpg", 160),
            set(), set()).isEmpty());
    }

    @Test
    public void mirrorCopyOrderLeftoverNamesStillCopied() {
        // Defensive: copy list me aisi file jo jora-bandish wali
        // list me nahi (signature/list mismatch) — chhootni nahi
        // chahiye, aakhir me aa jati hai.
        List<BulkMirror.CopyStep> order = BulkMirror.interleavedCopyOrder(
            Arrays.asList("1.jpg"), mtimes("1.jpg", 100),
            Arrays.asList("2.jpg"), mtimes("2.jpg", 160),
            set("1.jpg", "9.jpg"), set("2.jpg"));
        assertEquals(Arrays.asList("M:1.jpg", "R:2.jpg", "M:9.jpg"),
            steps(order));
    }
}
