package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Termux Mod (v127) — Run jawab mapping ki tests.
 * Verified prompt map (bundled runners): har project 1..14 pehle Mode
 * (1=HD default / 2=Normal) phir Size (1=Original default / 2=Crop)
 * poochta hai. Is range se bahar koi pre-answer nahi.
 */
public class BulkRunSettingsTest {

    @Test
    public void verifiedRangeIsBundledProjects() {
        assertTrue(BulkRunSettings.hasVerifiedPrompts(1));
        assertTrue(BulkRunSettings.hasVerifiedPrompts(14));
        assertFalse(BulkRunSettings.hasVerifiedPrompts(0));
        assertFalse(BulkRunSettings.hasVerifiedPrompts(15));
        assertFalse(BulkRunSettings.hasVerifiedPrompts(17));
        assertFalse(BulkRunSettings.hasVerifiedPrompts(-3));
    }

    @Test
    public void answersFollowScriptPromptOrder() {
        // [Mode, Size] — script isi order me poochta hai
        assertEquals(Arrays.asList("1", "1"),
            BulkRunSettings.answersFor(1, BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_ORIGINAL));
        assertEquals(Arrays.asList("2", "2"),
            BulkRunSettings.answersFor(9, BulkRunSettings.ANSWER_NORMAL, BulkRunSettings.ANSWER_CROP));
        assertEquals(Arrays.asList("1", "2"),
            BulkRunSettings.answersFor(14, BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP));
    }

    @Test
    public void unknownAnswersFallBackToScriptDefaults() {
        // Ghalat/unknown jawab → script ke Enter-defaults (HD / Original)
        assertEquals(Arrays.asList("1", "1"), BulkRunSettings.answersFor(3, "x", null));
        assertEquals(Arrays.asList("1", "1"), BulkRunSettings.answersFor(3, "", ""));
    }

    @Test
    public void unverifiedProjectsGetNoAnswers() {
        assertTrue(BulkRunSettings.answersFor(15, "1", "1").isEmpty());
        assertTrue(BulkRunSettings.answersFor(18, "2", "2").isEmpty());
        assertTrue(BulkRunSettings.answersFor(0, "1", "1").isEmpty());
    }

    @Test
    public void pipedCommandWrapsAliasAndCompound() {
        assertEquals("printf '%s\\n' '1' '2' | (w1)",
            BulkRunSettings.buildPipedCommand("w1", Arrays.asList("1", "2")));
        assertEquals("printf '%s\\n' '1' '1' | (cd /storage/emulated/0/mockup_project_3 && python adnan_core.py process 3)",
            BulkRunSettings.buildPipedCommand(
                "cd /storage/emulated/0/mockup_project_3 && python adnan_core.py process 3",
                Arrays.asList("1", "1")));
    }

    @Test
    public void pipedCommandEdgeCases() {
        // Khaali jawab → asal command waisa ka waisa (plain terminal run)
        assertEquals("w5", BulkRunSettings.buildPipedCommand("w5", Collections.<String>emptyList()));
        assertEquals("w5", BulkRunSettings.buildPipedCommand("w5", null));
        assertNull(BulkRunSettings.buildPipedCommand(null, Arrays.asList("1")));
    }
}
