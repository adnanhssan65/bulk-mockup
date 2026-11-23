package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Termux Mod (v131) — run progress parse (Item 4) ki tests.
 * Asal runner ke bar_format jesi lines istemal hoti hain:
 *   " {desc} |{bar:20}| {n}/{total}" — ANSI rang codes ke sath.
 */
public class BulkRunProgressTest {

    private static final String ESC = "\u001B";

    @Test
    public void parsesRunnerBarFormatWithAnsi() {
        BulkRunProgress p = new BulkRunProgress();
        String line = " " + ESC + "[95m✨ HD Mode " + ESC + "[0m " + ESC + "[95m|"
            + ESC + "[0m███████░░░░░░░░░░░░░" + ESC + "[95m|" + ESC + "[0m "
            + ESC + "[92m37/100" + ESC + "[0m";
        assertTrue(p.offer(line));
        assertTrue(p.hasProgress());
        assertEquals(37, p.done());
        assertEquals(100, p.total());
        assertEquals(37, p.percent());
    }

    @Test
    public void parsesStandardTqdmLine() {
        BulkRunProgress p = new BulkRunProgress();
        assertTrue(p.offer(" 42%|████▏     | 42/100 [00:31<00:43,  1.35it/s]"));
        assertEquals(42, p.done());
        assertEquals(100, p.total());
        assertEquals(42, p.percent());
    }

    @Test
    public void plainLinesAreNotProgress() {
        BulkRunProgress p = new BulkRunProgress();
        assertFalse(p.offer(" ✔ Sab already set hai."));
        assertFalse(p.hasProgress());
        assertEquals(-1, p.percent());
        // Fallback ke liye aakhri line mehfooz hai
        assertEquals("✔ Sab already set hai.", p.lastLine());
    }

    @Test
    public void spinnerLinesAreNotProgress() {
        BulkRunProgress p = new BulkRunProgress();
        // Runner ka \r spinner — koi n/total nahi
        assertFalse(p.offer(" ✿ o Loading mockup engine..."));
        assertFalse(p.hasProgress());
    }

    @Test
    public void progressUpdatesAndPercentClamps() {
        BulkRunProgress p = new BulkRunProgress();
        p.offer("0/250");
        assertEquals(0, p.percent());
        p.offer("125/250");
        assertEquals(50, p.percent());
        p.offer("250/250");
        assertEquals(100, p.percent());
        // total se zyada n aye to bhi percent 100 par rukta hai
        p.offer("260/250");
        assertEquals(100, p.percent());
        assertEquals(260, p.done());
    }

    @Test
    public void zeroOrBrokenTotalsIgnored() {
        BulkRunProgress p = new BulkRunProgress();
        assertFalse(p.offer("3/0"));
        assertFalse(p.hasProgress());
    }

    @Test
    public void stripAnsiRemovesColorCodes() {
        assertEquals("abc", BulkRunProgress.stripAnsi(ESC + "[1;32mabc" + ESC + "[0m"));
        assertEquals("", BulkRunProgress.stripAnsi(null));
        assertEquals("plain", BulkRunProgress.stripAnsi("plain"));
    }

    @Test
    public void formatElapsedMmSs() {
        assertEquals("00:00", BulkRunProgress.formatElapsed(0));
        assertEquals("00:09", BulkRunProgress.formatElapsed(9_400));
        assertEquals("02:15", BulkRunProgress.formatElapsed(135_000));
        assertEquals("07:03", BulkRunProgress.formatElapsed(423_000));
        assertEquals("62:15", BulkRunProgress.formatElapsed(3_735_000));
        assertEquals("00:00", BulkRunProgress.formatElapsed(-5));
    }

    /**
     * Item 3 ka device-less saboot (halka): bohat si lines ka flood
     * parse + tail-append se guzre to bhi kaam halka rehta hai —
     * 200k lines makhsoos waqt ke andar (JVM par aam tor par <1s;
     * sakht hadd 20s sirf regression pakarne ke liye hai).
     */
    @Test
    public void floodOfLinesStaysCheap() {
        BulkRunProgress p = new BulkRunProgress();
        StringBuilder tail = new StringBuilder();
        long start = System.nanoTime();
        for (int i = 0; i < 200_000; i++) {
            p.offer("line " + i + " processing image data");
            tail.append("print line ").append(i)
                .append(" — kuch aur text taake line lambi ho\n");
            BulkRunHelper.trimTail(tail);
        }
        long tookMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue("flood bohat sust: " + tookMs + "ms", tookMs < 20_000);
        // trimTail tail ko hadd ke andar rakhta hai (12000 + ek line)
        assertTrue(tail.length() <= 12_100);
    }
}
