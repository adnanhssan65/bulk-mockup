package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Termux Mod v139 — owner ke teen approved kaam:
 *  (1) Projects 15–18 bhi bundled (kul 18): preview range 1..18,
 *      un ke prompt profiles (15/17/18 = ADNAN, 16 = SYSTEM) or
 *      drop-folder provisioning ki seeding ka pure faisla.
 *  (2) Factory default launcher preset + app naam = Termux Mod
 *      (tile). Explicit choice mehfooz; baghair choice wale
 *      installs upgrade par naye default par.
 *  (3) Run console popup ka dark panel + DejaVu Sans Mono console
 *      font (banner glyphs) — style constants + asset path.
 */
public class BulkV139Test {

    // ------------------------------------------------------------ (1) bundled 15-18: previews

    @Test
    public void bundledPreviewRangeCovers15To18() {
        assertEquals(18, BulkPreviewSource.LAST_BUNDLED_PROJECT);
        for (int n = 15; n <= 18; n++) {
            assertTrue("p" + n, BulkPreviewSource.hasBundledRange(n));
            assertEquals("bulk/previews/p" + n + ".jpg",
                BulkPreviewSource.bundledAssetPath(n));
            assertEquals("bulk/previews/p" + n + ".mp4",
                BulkDropFolders.previewAssetPath(n));
            assertEquals(BulkPreviewSource.KIND_BUNDLED,
                BulkPreviewSource.choose(new ArrayList<>(), n, true).kind);
        }
        assertFalse(BulkPreviewSource.hasBundledRange(19));
    }

    @Test
    public void bundledNumbersParseFor15To18() {
        // Provisioning seeding (BulkDropFolders.seedProvisionRecordsForBundled)
        // bundled numbers assets ke zip naamon se isi parse se nikalti hai —
        // 15..18 ke naam sahi parse hon to un ke records seed ho jate hain
        // (folder khud NAHI banta — v138 wala usool).
        for (int n = 15; n <= 18; n++) {
            assertEquals(n, BulkProjects.parseFolderNumber("mockup_project_" + n));
            assertTrue("provision decision p" + n, BulkDropFolders.shouldProvision(false));
            assertFalse(BulkDropFolders.shouldProvision(true));
        }
    }

    @Test
    public void mainScriptDetectionFor15To18() {
        assertEquals("w15.py", BulkProjects.detectMainScript(
            Arrays.asList("w15.py", "README.txt"), 15));
        assertEquals("system_core.py", BulkProjects.detectMainScript(
            Arrays.asList("system_core.py", "Inter.ttf"), 16));
        assertEquals("w17.py", BulkProjects.detectMainScript(
            Arrays.asList("w17.py", "README.txt"), 17));
        assertEquals("w18.py", BulkProjects.detectMainScript(
            Arrays.asList("w18.py", "README.txt"), 18));
    }

    // ------------------------------------------------------------ (1) prompt profiles 15-18

    @Test
    public void promptProfilesForBundled15To18() {
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileFor(15, "w15.py"));
        assertEquals(BulkRunSettings.PromptProfile.SYSTEM,
            BulkRunSettings.profileFor(16, "system_core.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileFor(17, "w17.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileFor(18, "w18.py"));
    }

    @Test
    public void promptAnswersForP16AreSystemStyle() {
        // SYSTEM khandan: Mode '1'=Normal (default) / '2'=HD — ulta ADNAN se
        assertEquals(Arrays.asList("2", "2"),
            BulkRunSettings.answersForProfile(BulkRunSettings.PromptProfile.SYSTEM,
                BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP));
        assertEquals(Arrays.asList("1", "1"),
            BulkRunSettings.answersForProfile(BulkRunSettings.PromptProfile.SYSTEM,
                BulkRunSettings.ANSWER_NORMAL, BulkRunSettings.ANSWER_ORIGINAL));
        // ADNAN (15/17/18): '1'=HD (default), Size wahi tartib
        assertEquals(Arrays.asList("1", "2"),
            BulkRunSettings.answersForProfile(BulkRunSettings.PromptProfile.ADNAN,
                BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP));
    }

    // ------------------------------------------------------------ (2) default preset = Termux Mod

    @Test
    public void factoryDefaultsAreTermuxMod() {
        assertEquals("Termux Mod", BulkPanelSettings.DEFAULT_NAME);
        assertEquals(BulkPanelSettings.ICON_TILE, BulkPanelSettings.DEFAULT_ICON);
        // Hero two-tone: "Termux" dark + "Mod" accent (akhri lafz index 7 se)
        assertEquals(7, BulkPanelSettings.accentStart(BulkPanelSettings.DEFAULT_NAME));
        Map<String, String> d = BulkPanelSettings.factoryDefaults();
        assertEquals("s:Termux Mod", d.get("bulk_mockup|panel_app_name"));
        assertEquals("s:tile", d.get("bulk_mockup|panel_icon"));
        assertEquals("b:false", d.get("bulk_mockup|panel_icon_explicit"));
    }

    @Test
    public void effectiveIconFallsToNewDefaultWithoutExplicitChoice() {
        // Fresh install: kuch stored hi nahi
        assertEquals("tile", BulkPanelSettings.effectiveIconId(null, false));
        // Purana install jis ne kabhi preset nahi chuna — stored "pro"
        // (purana default) ho tab bhi upgrade par naya default lagu
        assertEquals("tile", BulkPanelSettings.effectiveIconId("pro", false));
        assertEquals("tile", BulkPanelSettings.effectiveIconId("tile", false));
    }

    @Test
    public void effectiveIconPreservesExplicitChoice() {
        assertEquals("pro", BulkPanelSettings.effectiveIconId("pro", true));
        assertEquals("glyph", BulkPanelSettings.effectiveIconId("glyph", true));
        assertEquals("light", BulkPanelSettings.effectiveIconId("light", true));
        assertEquals("tile", BulkPanelSettings.effectiveIconId("tile", true));
        // Explicit ho lekin value kharab ho to sanitize -> default
        assertEquals("tile", BulkPanelSettings.effectiveIconId("bogus", true));
    }

    @Test
    public void termuxModAliasIsFirstInTable() {
        // Manifest: BulkIconTermuxMod enabled=true (default) — table ka
        // pehla alias wahi hai or iconIndex tile ko usi par le jata hai
        assertEquals("com.termux.app.BulkIconTermuxMod", BulkPanelSettings.ICON_ALIASES[0]);
        assertEquals(0, BulkPanelSettings.iconIndex("tile"));
        assertEquals(3, BulkPanelSettings.iconIndex("pro"));
    }

    // ------------------------------------------------------------ (3) console style

    @Test
    public void consoleStyleConstantsMatchApprovedPreview() {
        assertEquals(0xFF0A1A0C, BulkRunHelper.CONSOLE_BG);
        assertEquals(0xFF9BE29B, BulkRunHelper.CONSOLE_TEXT);
        assertEquals("bulk/fonts/dejavu-sans-mono.ttf", BulkFonts.CONSOLE_ASSET);
        // Console font UI catalog me NAHI — picker ke 10 fonts waisay hi
        assertEquals(10, BulkFonts.bundledCount());
        assertFalse(BulkFonts.isValidId("dejavu-sans-mono"));
    }
}
