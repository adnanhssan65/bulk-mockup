package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/** Termux Mod (v129) — Home live-log buffer (BulkLog) ki tests. */
public class BulkLogTest {

    @Before
    public void reset() {
        BulkLog.clear();
    }

    @Test
    public void emptyBufferShowsIdleLine() {
        assertEquals("IDLE", BulkLog.tailText(6, "IDLE"));
        assertEquals(0, BulkLog.size());
    }

    @Test
    public void tailReturnsNewestLinesInOrder() {
        BulkLog.add("pehli");
        BulkLog.add("dusri");
        BulkLog.add("teesri");
        String tail = BulkLog.tailText(2, "IDLE");
        assertFalse(tail.contains("pehli"));
        assertTrue(tail.contains("dusri"));
        assertTrue(tail.contains("teesri"));
        // Tartib: purani pehle, nayi sab se neeche (auto-scroll wali shakal)
        assertTrue(tail.indexOf("dusri") < tail.indexOf("teesri"));
    }

    @Test
    public void bufferIsCapped() {
        for (int i = 0; i < BulkLog.MAX_LINES + 25; i++) {
            BulkLog.add("line " + i);
        }
        assertEquals(BulkLog.MAX_LINES, BulkLog.size());
        // Sab se purani lines gir gayin, nayi wali mehfooz hai
        String tail = BulkLog.tailText(6, "IDLE");
        assertTrue(tail.contains("line " + (BulkLog.MAX_LINES + 24)));
        assertFalse(BulkLog.lines().get(0).contains("line 0 "));
    }

    @Test
    public void blankAndNullLinesIgnored() {
        BulkLog.add(null);
        BulkLog.add("");
        BulkLog.add("   ");
        assertEquals(0, BulkLog.size());
    }

    @Test
    public void statusLineShowsAsNewestLine() {
        // v131 (Item 4 bonus): live status line buffer ki line NAHI —
        // tail ke aakhir me ati hai, buffer size nahi barhta
        BulkLog.add("run start");
        BulkLog.setStatus("Project 1 — 37/100 • 02:15");
        String tail = BulkLog.tailText(6, "IDLE");
        assertTrue(tail.endsWith("Project 1 — 37/100 • 02:15"));
        assertEquals(1, BulkLog.size());
        BulkLog.setStatus(null);
        assertFalse(BulkLog.tailText(6, "IDLE").contains("37/100"));
    }

    @Test
    public void statusAloneShowsWithoutBufferLines() {
        BulkLog.setStatus("Project 2 — 3/9 • 00:41");
        assertEquals("Project 2 — 3/9 • 00:41", BulkLog.tailText(6, "IDLE"));
        BulkLog.clear();
        assertEquals("IDLE", BulkLog.tailText(6, "IDLE"));
    }

    @Test
    public void longLinesAreTrimmed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) sb.append('x');
        BulkLog.add(sb.toString());
        String stored = BulkLog.lines().get(0);
        // "HH:mm:ss  " (10 chars) + kata hua text
        assertTrue(stored.length() <= 10 + BulkLog.MAX_LINE_CHARS);
    }
}
