package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

/**
 * v133 — BulkLogStyle parser ke tests: asal BulkLog formats
 * (BulkStrings/BulkRunHelper ke lafz-ba-lafz namoonay), stamp
 * alag karna, progress/count/time, aur na-pehchani line ka
 * PLAIN fallback (line kabhi ghayab nahi hoti).
 */
public class BulkLogStyleTest {

    @Test
    public void runDone_withProgress() {
        BulkLogStyle.Parsed p = BulkLogStyle.parse(
            "12:30:18  ✔ Project 13 — run mukammal — 18/18 • 00:15");
        Assert.assertEquals("12:30:18", p.stamp);
        Assert.assertEquals(BulkLogStyle.Kind.RUN_DONE, p.kind);
        Assert.assertEquals("Project 13", p.project);
        Assert.assertEquals("run mukammal", p.mainText);
        Assert.assertEquals("18/18", p.countText);
        Assert.assertEquals(18, p.cur);
        Assert.assertEquals(18, p.total);
        Assert.assertEquals("00:15", p.timeText);
        Assert.assertEquals("DONE", p.pill);
        Assert.assertTrue(p.hasProgress());
        Assert.assertEquals(100, p.percent());
    }

    @Test
    public void runDone_withoutProgress() {
        BulkLogStyle.Parsed p = BulkLogStyle.parse(
            "09:01:02  ✔ Project 3 — run mukammal • 01:05");
        Assert.assertEquals(BulkLogStyle.Kind.RUN_DONE, p.kind);
        Assert.assertEquals("Project 3", p.project);
        Assert.assertNull(p.countText);
        Assert.assertFalse(p.hasProgress());
        Assert.assertEquals("01:05", p.timeText);
        Assert.assertEquals("DONE", p.pill);
    }

    @Test
    public void runFail_withProgress() {
        BulkLogStyle.Parsed p = BulkLogStyle.parse(
            "23:59:59  ✖ Project 7 — run fail — 3/18 • 00:07");
        Assert.assertEquals(BulkLogStyle.Kind.RUN_FAIL, p.kind);
        Assert.assertEquals("Project 7", p.project);
        Assert.assertEquals("run fail", p.mainText);
        Assert.assertEquals(3, p.cur);
        Assert.assertEquals(18, p.total);
        Assert.assertEquals("FAIL", p.pill);
        Assert.assertEquals(17, p.percent());
    }

    @Test
    public void runStart() {
        BulkLogStyle.Parsed p = BulkLogStyle.parse("10:00:00  ▶ Project 21 — run shuru");
        Assert.assertEquals(BulkLogStyle.Kind.RUN_START, p.kind);
        Assert.assertEquals("Project 21", p.project);
        Assert.assertEquals("run shuru", p.mainText);
        Assert.assertNull(p.pill);
        Assert.assertNull(p.timeText);
    }

    @Test
    public void statusLine_progress() {
        BulkLogStyle.Parsed p = BulkLogStyle.parse("Project 1 — 37/100 • 02:15");
        Assert.assertNull(p.stamp);
        Assert.assertEquals(BulkLogStyle.Kind.RUN_PROGRESS, p.kind);
        Assert.assertEquals("Project 1", p.project);
        Assert.assertEquals("37/100", p.countText);
        Assert.assertEquals(37, p.percent());
        Assert.assertEquals("02:15", p.timeText);
        Assert.assertNull(p.pill);
    }

    @Test
    public void movedLine() {
        BulkLogStyle.Parsed p = BulkLogStyle.parse(
            "12:31:04  18 original(s) deleted from gallery (moved)");
        Assert.assertEquals("12:31:04", p.stamp);
        Assert.assertEquals(BulkLogStyle.Kind.MOVED, p.kind);
        Assert.assertEquals("18", p.countText);
        Assert.assertEquals("original(s) deleted from gallery", p.mainText);
        Assert.assertEquals("MOVED", p.pill);
    }

    @Test
    public void unknownLine_plainWithStamp() {
        BulkLogStyle.Parsed p = BulkLogStyle.parse(
            "08:00:00  Project 5 <- 10 wallpapers (manual move)");
        Assert.assertEquals(BulkLogStyle.Kind.PLAIN, p.kind);
        Assert.assertEquals("08:00:00", p.stamp);
        Assert.assertEquals("Project 5 <- 10 wallpapers (manual move)", p.mainText);
    }

    @Test
    public void exportLine_plainNotRunDone() {
        // ✔ se shuru hoti hai lekin run format nahi — PLAIN rehni chahiye
        BulkLogStyle.Parsed p = BulkLogStyle.parse("08:00:00  ✔ Export: Project 5");
        Assert.assertEquals(BulkLogStyle.Kind.PLAIN, p.kind);
        Assert.assertEquals("✔ Export: Project 5", p.mainText);
    }

    @Test
    public void stampedProgressText_staysPlain() {
        // stamp wali progress-numa line status line nahi hoti
        BulkLogStyle.Parsed p = BulkLogStyle.parse("08:00:00  Project 1 — 37/100 • 02:15");
        Assert.assertEquals(BulkLogStyle.Kind.PLAIN, p.kind);
    }

    @Test
    public void nullAndEmpty_safe() {
        Assert.assertEquals(BulkLogStyle.Kind.PLAIN, BulkLogStyle.parse(null).kind);
        Assert.assertEquals("", BulkLogStyle.parse(null).mainText);
        Assert.assertEquals(BulkLogStyle.Kind.PLAIN, BulkLogStyle.parse("").kind);
        Assert.assertEquals(BulkLogStyle.Kind.PLAIN,
            BulkLogStyle.parse("kuch bhi aam output line").kind);
    }

    @Test
    public void percent_clamped() {
        BulkLogStyle.Parsed p = BulkLogStyle.parse("Project 2 — 25/20 • 00:10");
        Assert.assertEquals(100, p.percent());
    }
}
