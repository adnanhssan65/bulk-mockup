package com.termux.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Termux Mod v134 — move-back faisla, Recreate auto/manual gate,
 * gallery scroll-jump target.
 */
public class BulkV134Test {

    // ------------------------------------------------------------ Move-back decision

    @Test
    public void moveBackDecision() {
        // TermuxMod me kuch hai hi nahi — karne ko kuch nahi
        assertEquals(BulkMigrationBack.Decision.NOTHING,
            BulkMigrationBack.decide(false, false, false));
        assertEquals(BulkMigrationBack.Decision.NOTHING,
            BulkMigrationBack.decide(false, true, true));
        // Root par pehle se folder — kabhi merge/overwrite nahi,
        // runner safe ho ya na ho, copy wahin rehti hai
        assertEquals(BulkMigrationBack.Decision.STAY_CONFLICT,
            BulkMigrationBack.decide(true, true, true));
        assertEquals(BulkMigrationBack.Decision.STAY_CONFLICT,
            BulkMigrationBack.decide(true, true, false));
        // Runner purana/hardcoded — wahin rahe, wese hi chalega
        assertEquals(BulkMigrationBack.Decision.STAY_UNSAFE,
            BulkMigrationBack.decide(true, false, false));
        // Sirf ye surat me asal move: copy hai, root khaali, runner safe
        assertEquals(BulkMigrationBack.Decision.MOVE,
            BulkMigrationBack.decide(true, false, true));
    }

    // ------------------------------------------------------------ Recreate gate

    @Test
    public void recreateDefaultsAreManual() {
        assertFalse(BulkRecreateSettings.DEFAULT_FOLDERS_AUTO);
        assertFalse(BulkRecreateSettings.DEFAULT_VIDEOS_AUTO);
    }

    @Test
    public void recreateGateFollowsToggle() {
        // Manual (OFF): khud kabhi nahi banta
        assertFalse(BulkRecreateSettings.mayCreateFolders(false));
        assertFalse(BulkRecreateSettings.mayCopyVideos(false));
        // Auto (ON): v133 wala rawaiya — har toggle apne hisse ka
        assertTrue(BulkRecreateSettings.mayCreateFolders(true));
        assertTrue(BulkRecreateSettings.mayCopyVideos(true));
    }

    // ------------------------------------------------------------ Scroll-jump target

    @Test
    public void scrollJumpTargets() {
        assertEquals(0, BulkGalleryState.scrollJumpTarget(true, 250));
        assertEquals(0, BulkGalleryState.scrollJumpTarget(true, 0));
        assertEquals(249, BulkGalleryState.scrollJumpTarget(false, 250));
        assertEquals(0, BulkGalleryState.scrollJumpTarget(false, 1));
        assertEquals(0, BulkGalleryState.scrollJumpTarget(false, 0));
    }

    // ------------------------------------------------- A: selection default

    @Test
    public void selectProjectStartsUnselected() {
        // Persisted kuch bhi ho — screen bina selection ke khulti hai
        assertEquals(-1, BulkStaging.restoredSelection(2));
        assertEquals(-1, BulkStaging.restoredSelection(14));
        assertEquals(-1, BulkStaging.restoredSelection(-1));
    }

    // ------------------------------------------------- B: compact row slots

    @Test
    public void compactRowSlotMath() {
        // Row = 4 slots (thumbs + View-all tile + spacers)
        assertEquals(2, BulkStaging.compactSpacerCount(1));
        assertEquals(1, BulkStaging.compactSpacerCount(2));
        assertEquals(0, BulkStaging.compactSpacerCount(3));
        assertEquals(0, BulkStaging.compactSpacerCount(9));
        assertEquals(0, BulkStaging.compactSpacerCount(0));
    }

    // ------------------------------------------------- E: tab labels

    @Test
    public void readyTabsHaveNoTrailingSlash() {
        assertEquals("Mockups", BulkStrings.get(BulkStrings.MOCKUPS_TAB, false));
        assertEquals("Wallpapers", BulkStrings.get(BulkStrings.WALLPAPERS_TAB, false));
        assertEquals("Sequence", BulkStrings.get(BulkStrings.SEQUENCE_TAB, false));
        assertEquals("Mockups", BulkStrings.get(BulkStrings.MOCKUPS_TAB, true));
        assertEquals("Wallpapers", BulkStrings.get(BulkStrings.WALLPAPERS_TAB, true));
    }

    // ------------------------------------------------- G: language pairs + parse

    @Test
    public void logTemplatesHaveRealEnglish() {
        assertTrue(BulkStrings.get(BulkStrings.LOG_RUN_DONE, false).contains("run complete"));
        assertTrue(BulkStrings.get(BulkStrings.LOG_RUN_DONE, true).contains("run mukammal"));
        assertTrue(BulkStrings.get(BulkStrings.LOG_RUN_START, false).contains("run started"));
        assertTrue(BulkStrings.get(BulkStrings.LOG_RUN_START, true).contains("run shuru"));
        assertTrue(BulkStrings.get(BulkStrings.LOG_RUN_FAIL, false).contains("run failed"));
        assertFalse(BulkStrings.get(BulkStrings.LOG_IDLE, false).contains("yahan"));
    }

    @Test
    public void englishLogLinesParseStyled() {
        BulkLogStyle.Parsed done = BulkLogStyle.parse(
            "12:30:18  ✔ Project 13 — run complete — 18/18 • 00:15");
        assertEquals(BulkLogStyle.Kind.RUN_DONE, done.kind);
        assertEquals("run complete", done.mainText);
        assertEquals("DONE", done.pill);
        assertEquals("18/18", done.countText);

        BulkLogStyle.Parsed fail = BulkLogStyle.parse(
            "12:31:02  ✖ Project 5 — run failed • 00:07");
        assertEquals(BulkLogStyle.Kind.RUN_FAIL, fail.kind);
        assertEquals("FAIL", fail.pill);

        BulkLogStyle.Parsed start = BulkLogStyle.parse("▶ Project 5 — run started");
        assertEquals(BulkLogStyle.Kind.RUN_START, start.kind);

        // Roman Urdu lines bhi pehle ki tarah parse hoti hain
        BulkLogStyle.Parsed ru = BulkLogStyle.parse(
            "12:30:18  ✔ Project 13 — run mukammal — 18/18 • 00:15");
        assertEquals(BulkLogStyle.Kind.RUN_DONE, ru.kind);
        assertEquals("run mukammal", ru.mainText);
    }

    // ------------------------------------------------- Scroll handle math

    @Test
    public void scrollHandleFractions() {
        // List: top = 0, bottom = 1, darmiyan = 0.5; na-scrollable = 0
        assertEquals(0f, BulkScrollHandle.fractionForList(0, 10, 100), 0.001f);
        assertEquals(1f, BulkScrollHandle.fractionForList(90, 10, 100), 0.001f);
        assertEquals(0.5f, BulkScrollHandle.fractionForList(45, 10, 100), 0.001f);
        assertEquals(0f, BulkScrollHandle.fractionForList(0, 10, 10), 0.001f);
        assertEquals(0f, BulkScrollHandle.fractionForList(0, 0, 0), 0.001f);
        // ScrollView: wohi clamp
        assertEquals(0f, BulkScrollHandle.fractionForScroll(0, 500), 0.001f);
        assertEquals(1f, BulkScrollHandle.fractionForScroll(700, 500), 0.001f);
        assertEquals(0.5f, BulkScrollHandle.fractionForScroll(250, 500), 0.001f);
        assertEquals(0f, BulkScrollHandle.fractionForScroll(10, 0), 0.001f);
        // Scrub: fraction + harkat/track, dono taraf clamp
        assertEquals(0.75f, BulkScrollHandle.scrubFraction(0.5f, 50f, 200f), 0.001f);
        assertEquals(1f, BulkScrollHandle.scrubFraction(0.9f, 500f, 200f), 0.001f);
        assertEquals(0f, BulkScrollHandle.scrubFraction(0.1f, -500f, 200f), 0.001f);
        // Scrub position: 0..total-1
        assertEquals(0, BulkScrollHandle.scrubPosition(0f, 250));
        assertEquals(249, BulkScrollHandle.scrubPosition(1f, 250));
        assertEquals(0, BulkScrollHandle.scrubPosition(0.5f, 0));
    }

    @Test
    public void scrollButtonDefaultsToShow() {
        assertTrue(BulkScrollSettings.DEFAULT_SHOW);
        assertTrue(BulkScrollSettings.mayShow(true));
        assertFalse(BulkScrollSettings.mayShow(false));
    }

    // ------------------------------------------------- H: entry layout model

    @Test
    public void logEntryLayoutBarFirstPillLast() {
        BulkLogStyle.Parsed withCount = BulkLogStyle.parse(
            "12:30:18  ✔ Project 13 — run complete — 18/18 • 00:15");
        assertEquals(
            java.util.Arrays.asList(BulkLogStyle.LinePart.BAR,
                BulkLogStyle.LinePart.HEAD, BulkLogStyle.LinePart.SUB_WITH_PILL),
            BulkLogStyle.entryLayout(withCount));

        BulkLogStyle.Parsed noCount = BulkLogStyle.parse(
            "12:30:18  ✔ Project 13 — run complete • 00:15");
        assertEquals(
            java.util.Arrays.asList(BulkLogStyle.LinePart.HEAD,
                BulkLogStyle.LinePart.SUB_WITH_PILL),
            BulkLogStyle.entryLayout(noCount));

        // Baqi kinds is model me nahi aate
        assertTrue(BulkLogStyle.entryLayout(
            BulkLogStyle.parse("kuch aam line")).isEmpty());
    }
}
