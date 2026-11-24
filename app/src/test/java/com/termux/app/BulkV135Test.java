package com.termux.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * v135 — Control Panel ki pure logic: password hash/verify, defaults
 * registry + reset mukammal hona, home-rows tartib/show-hide model,
 * backup JSON round-trip, font-scale mapping, accent helpers,
 * launcher icon presets, logo crop hisaab.
 */
public class BulkV135Test {

    // ------------------------------------------------------------ password

    @Test
    public void defaultPasswordVerifiesAgainstDefaultStored() {
        assertTrue(BulkPanelSettings.verify(BulkPanelSettings.defaultStored(), "Adnan001"));
        assertTrue(BulkPanelSettings.verify("", "Adnan001"));
        assertTrue(BulkPanelSettings.verify(null, "Adnan001"));
    }

    @Test
    public void wrongPasswordFails() {
        assertFalse(BulkPanelSettings.verify(BulkPanelSettings.defaultStored(), "adnan001"));
        assertFalse(BulkPanelSettings.verify(BulkPanelSettings.defaultStored(), "Adnan002"));
        assertFalse(BulkPanelSettings.verify(BulkPanelSettings.defaultStored(), ""));
        assertFalse(BulkPanelSettings.verify(BulkPanelSettings.defaultStored(), null));
    }

    @Test
    public void customSaltRoundTrips() {
        String stored = BulkPanelSettings.makeStored("xyz123", "MyNewPass");
        assertTrue(BulkPanelSettings.verify(stored, "MyNewPass"));
        assertFalse(BulkPanelSettings.verify(stored, "Adnan001"));
        assertFalse(BulkPanelSettings.verify(stored, "mynewpass"));
    }

    @Test
    public void storedHashNeverContainsPlaintext() {
        String stored = BulkPanelSettings.makeStored("salt", "Adnan001");
        assertTrue(stored.contains("$"));
        assertFalse(stored.contains("Adnan001"));
        assertEquals(64, stored.substring(stored.indexOf('$') + 1).length());
    }

    @Test
    public void sha256KnownVector() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            BulkPanelSettings.sha256Hex(""));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            BulkPanelSettings.sha256Hex("abc"));
    }

    // ------------------------------------------------------------ app name

    @Test
    public void defaultNameIsTermuxPro() {
        assertEquals("Termux Pro", BulkPanelSettings.DEFAULT_NAME);
        assertEquals("Termux Pro", BulkPanelSettings.sanitizeName(null));
        assertEquals("Termux Pro", BulkPanelSettings.sanitizeName("   "));
    }

    @Test
    public void sanitizeNameCollapsesSpaces() {
        assertEquals("My Cool App", BulkPanelSettings.sanitizeName("  My   Cool\tApp "));
    }

    @Test
    public void accentStartIsLastWord() {
        assertEquals(7, BulkPanelSettings.accentStart("Termux Pro"));
        assertEquals(8, BulkPanelSettings.accentStart("My Cool App"));
        assertEquals(-1, BulkPanelSettings.accentStart("Termux"));
        assertEquals(-1, BulkPanelSettings.accentStart(""));
        assertEquals(-1, BulkPanelSettings.accentStart(null));
    }

    @Test
    public void defaultSloganMatchesHero() {
        assertEquals("Powerful Terminal Experience", BulkPanelSettings.DEFAULT_SLOGAN);
    }

    // ------------------------------------------------------------ home rows

    @Test
    public void defaultOrderIsV129Order() {
        assertEquals(Arrays.asList("add", "run", "projects", "preview", "terminal"),
            BulkPanelSettings.parseOrder(null));
        assertEquals(Arrays.asList("add", "run", "projects", "preview", "terminal"),
            BulkPanelSettings.parseOrder(""));
        assertEquals("add,run,projects,preview,terminal", BulkPanelSettings.DEFAULT_ORDER_CSV);
    }

    @Test
    public void parseOrderKeepsCustomOrderAndAppendsMissing() {
        assertEquals(Arrays.asList("terminal", "add", "run", "projects", "preview"),
            BulkPanelSettings.parseOrder("terminal,add"));
    }

    @Test
    public void parseOrderDropsInvalidAndDuplicates() {
        assertEquals(Arrays.asList("run", "add", "projects", "preview", "terminal"),
            BulkPanelSettings.parseOrder("run,run,bogus,add"));
    }

    @Test
    public void orderRoundTrips() {
        List<String> order = BulkPanelSettings.parseOrder("preview,terminal,add,run,projects");
        assertEquals("preview,terminal,add,run,projects",
            BulkPanelSettings.serializeOrder(order));
        assertEquals(order, BulkPanelSettings.parseOrder(
            BulkPanelSettings.serializeOrder(order)));
    }

    @Test
    public void movedSwapsAndClampsAtEdges() {
        List<String> order = new ArrayList<>(BulkPanelSettings.DEFAULT_ORDER);
        assertEquals(Arrays.asList("run", "add", "projects", "preview", "terminal"),
            BulkPanelSettings.moved(order, 0, 1));
        assertEquals(Arrays.asList("add", "projects", "run", "preview", "terminal"),
            BulkPanelSettings.moved(order, 2, -1));
        // kinare par clamp: koi tabdili nahi
        assertEquals(order, BulkPanelSettings.moved(order, 0, -1));
        assertEquals(order, BulkPanelSettings.moved(order, 4, 1));
    }

    @Test
    public void hiddenRowsRoundTripAndFilter() {
        Set<String> hidden = BulkPanelSettings.parseHidden("run,terminal");
        assertEquals(2, hidden.size());
        assertEquals("run,terminal", BulkPanelSettings.serializeHidden(hidden));
        assertEquals(Arrays.asList("add", "projects", "preview"),
            BulkPanelSettings.visibleRows(BulkPanelSettings.DEFAULT_ORDER, hidden));
        assertTrue(BulkPanelSettings.parseHidden("bogus").isEmpty());
        assertTrue(BulkPanelSettings.parseHidden(null).isEmpty());
    }

    // ------------------------------------------------------------ font size

    @Test
    public void fontScaleMapping() {
        assertEquals(0.85f, BulkPanelSettings.fontScaleFor("small"), 0.001f);
        assertEquals(1.0f, BulkPanelSettings.fontScaleFor("normal"), 0.001f);
        assertEquals(1.15f, BulkPanelSettings.fontScaleFor("large"), 0.001f);
        assertEquals(1.30f, BulkPanelSettings.fontScaleFor("xlarge"), 0.001f);
        assertEquals(1.0f, BulkPanelSettings.fontScaleFor("bogus"), 0.001f);
        assertEquals(1.0f, BulkPanelSettings.fontScaleFor(null), 0.001f);
    }

    @Test
    public void fontSizeIdsSanitized() {
        assertEquals("large", BulkPanelSettings.sanitizeSizeId("large"));
        assertEquals("normal", BulkPanelSettings.sanitizeSizeId("huge"));
        assertEquals(4, BulkPanelSettings.SIZE_IDS.size());
    }

    // ------------------------------------------------------------ accent

    @Test
    public void accentOverrideWinsOverPalette() {
        assertEquals(0xFF111111, BulkPanelSettings.effectiveAccent(0xFF222222, 0xFF111111));
        assertEquals(0xFF222222,
            BulkPanelSettings.effectiveAccent(0xFF222222, BulkPanelSettings.ACCENT_NONE));
    }

    @Test
    public void onAccentContrast() {
        assertEquals(0xFFFFFFFF, BulkPanelSettings.onAccentFor(0xFF7C3AED)); // gehra purple -> safed
        assertEquals(0xFF0A1408, BulkPanelSettings.onAccentFor(0xFF76FF03)); // roshan lime -> gehra
        assertEquals(0xFFFFFFFF, BulkPanelSettings.onAccentFor(0xFF000000));
        assertEquals(0xFF0A1408, BulkPanelSettings.onAccentFor(0xFFFFFFFF));
    }

    @Test
    public void accentSwatchesAreEightOpaqueColors() {
        assertEquals(8, BulkPanelSettings.ACCENT_SWATCHES.length);
        for (int c : BulkPanelSettings.ACCENT_SWATCHES) {
            assertEquals(0xFF, (c >> 24) & 0xFF);
        }
    }

    // ------------------------------------------------------------ launcher icons

    @Test
    public void defaultLauncherPresetIsTermuxPro() {
        assertEquals("pro", BulkPanelSettings.DEFAULT_ICON);
        assertEquals("pro", BulkPanelSettings.sanitizeIconId(null));
        assertEquals("pro", BulkPanelSettings.sanitizeIconId("bogus"));
        assertEquals("tile", BulkPanelSettings.sanitizeIconId("tile"));
    }

    @Test
    public void iconIndexMatchesAliasTable() {
        assertEquals(4, BulkPanelSettings.ICON_IDS.size());
        assertEquals(4, BulkPanelSettings.ICON_ALIASES.length);
        assertEquals(0, BulkPanelSettings.iconIndex("tile"));
        assertEquals(1, BulkPanelSettings.iconIndex("glyph"));
        assertEquals(2, BulkPanelSettings.iconIndex("light"));
        assertEquals(3, BulkPanelSettings.iconIndex("pro"));
        assertEquals("com.termux.app.BulkIconTermuxPro", BulkPanelSettings.ICON_ALIASES[3]);
    }

    // ------------------------------------------------------------ contact + crop

    @Test
    public void contactDigitsStripped() {
        assertEquals("447404032136", BulkPanelSettings.digitsOf("+44 7404 032136"));
        assertEquals("", BulkPanelSettings.digitsOf(null));
        assertEquals("", BulkPanelSettings.digitsOf("abc"));
    }

    @Test
    public void coverCropMath() {
        int[] r = BulkPanelSettings.coverCropRect(1000, 500, 1f);
        assertEquals(250, r[0]); assertEquals(0, r[1]); assertEquals(500, r[2]);
        int[] z = BulkPanelSettings.coverCropRect(1000, 500, 2f);
        assertEquals(375, z[0]); assertEquals(125, z[1]); assertEquals(250, z[2]);
        int[] clamp = BulkPanelSettings.coverCropRect(100, 100, 0.5f);
        assertEquals(100, clamp[2]);
        int[] zero = BulkPanelSettings.coverCropRect(0, 100, 1f);
        assertEquals(0, zero[2]);
    }

    // ------------------------------------------------------------ registry + defaults

    @Test
    public void registryKeysAreUnique() {
        Set<String> seen = new java.util.HashSet<>();
        for (BulkPanelSettings.Entry e : BulkPanelSettings.registry()) {
            assertTrue("duplicate: " + e.composite(), seen.add(e.composite()));
            assertTrue(e.type == 's' || e.type == 'b' || e.type == 'i' || e.type == 'f');
        }
    }

    @Test
    public void factoryDefaultsCoverEveryRegistryKey() {
        Map<String, String> defs = BulkPanelSettings.factoryDefaults();
        for (BulkPanelSettings.Entry e : BulkPanelSettings.registry()) {
            assertTrue("missing default: " + e.composite(), defs.containsKey(e.composite()));
            String typed = defs.get(e.composite());
            assertEquals(e.type, typed.charAt(0));
            assertEquals(':', typed.charAt(1));
        }
        assertTrue(defs.containsKey(BulkPanelSettings.LOGO_BACKUP_KEY));
    }

    @Test
    public void factoryDefaultValuesAreTheSpecOnes() {
        Map<String, String> d = BulkPanelSettings.factoryDefaults();
        assertEquals("s:Termux Pro", d.get("bulk_mockup|panel_app_name"));
        assertEquals("s:Powerful Terminal Experience", d.get("bulk_mockup|panel_slogan"));
        assertEquals("s:pro", d.get("bulk_mockup|panel_icon"));
        assertEquals("s:normal", d.get("bulk_mockup|panel_font_size"));
        assertEquals("i:0", d.get("bulk_mockup|panel_accent"));
        assertEquals("b:false", d.get("bulk_mockup|panel_app_lock"));
        assertEquals("s:", d.get("bulk_mockup|panel_pw_hash"));
        assertEquals("s:+44 7404 032136", d.get("bulk_mockup|panel_contact"));
        assertEquals("s:add,run,projects,preview,terminal", d.get("bulk_mockup|panel_home_order"));
        assertEquals("s:en", d.get("bulk_mockup|ui_language"));
        assertEquals("s:default", d.get("bulk_mockup|ui_theme"));
        assertEquals("s:poppins", d.get("bulk_mockup|ui_font"));
        assertEquals("s:auto", d.get("bulk_mockup|run_answer_mode"));
        assertEquals("s:1", d.get("bulk_mockup|run_answer_hd"));
        assertEquals("s:2", d.get("bulk_mockup|run_answer_size"));
        assertEquals("b:false", d.get("bulk_mockup|recreate_folders_auto"));
        assertEquals("b:false", d.get("bulk_mockup|recreate_videos_auto"));
        assertEquals("b:true", d.get("bulk_mockup|recreate_videos_enabled"));
        assertEquals("b:true", d.get("bulk_gallery|scroll_button_show"));
        assertEquals("i:24", d.get("bulk_duplicates|ttl_hours"));
    }

    // ------------------------------------------------------------ backup JSON

    @Test
    public void backupRoundTrips() {
        Map<String, String> values = BulkPanelSettings.factoryDefaults();
        values.put("bulk_mockup|panel_app_name", "s:My \"Cool\" App \\ Test");
        values.put("bulk_mockup|panel_slogan", "s:Line1\nLine2 • Urdu");
        String json = BulkPanelSettings.buildBackup(values);
        Map<String, String> back = BulkPanelSettings.parseBackup(json);
        assertEquals(values, back);
    }

    @Test
    public void backupRejectsForeignOrBrokenJson() {
        assertNull(BulkPanelSettings.parseBackup(null));
        assertNull(BulkPanelSettings.parseBackup(""));
        assertNull(BulkPanelSettings.parseBackup("not json"));
        assertNull(BulkPanelSettings.parseBackup("{\"kind\":\"other\",\"version\":\"1\"}"));
        assertNull(BulkPanelSettings.parseBackup(
            "{\"kind\":\"termuxmod-settings\",\"version\":\"1\",\"k\":\"untyped\"}"));
        assertNull(BulkPanelSettings.parseBackup(
            "{\"kind\":\"termuxmod-settings\",\"version\":\"1\"}"));
    }

    @Test
    public void jsonEscapingIsExact() {
        assertEquals("a\\\"b\\\\c\\nd", BulkPanelSettings.escapeJson("a\"b\\c\nd"));
        Map<String, String> flat = BulkPanelSettings.parseFlatJsonObject(
            "{\"k\":\"a\\\"b\\\\c\\nd\"}");
        assertEquals("a\"b\\c\nd", flat.get("k"));
    }

    // ------------------------------------------------------------ recreate video gate

    @Test
    public void previewVideoGateNeedsAutoAndEnabled() {
        assertTrue(BulkRecreateSettings.mayCopyVideos(true, true));
        assertFalse(BulkRecreateSettings.mayCopyVideos(true, false));
        assertFalse(BulkRecreateSettings.mayCopyVideos(false, true));
        assertFalse(BulkRecreateSettings.mayCopyVideos(false, false));
        // purana 1-arg rawaiya mehfooz
        assertTrue(BulkRecreateSettings.mayCopyVideos(true));
        assertFalse(BulkRecreateSettings.mayCopyVideos(false));
        assertTrue(BulkRecreateSettings.DEFAULT_VIDEOS_ENABLED);
    }

    // ------------------------------------------------------------ version

    @Test
    public void versionCodeIs135() {
        assertEquals(135, com.termux.BuildConfig.VERSION_CODE);
        assertNotEquals(134, com.termux.BuildConfig.VERSION_CODE);
    }
}
