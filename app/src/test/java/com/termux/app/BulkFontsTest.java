package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/** Termux Mod (v127) — BulkFonts catalog ki tests: 10 bundled fonts, unique ids, asset naam, default rawaiya. */
public class BulkFontsTest {

    @Test
    public void tenBundledFontsPlusDefault() {
        assertEquals(10, BulkFonts.bundledCount());
        assertEquals(11, BulkFonts.fonts().size());
        assertEquals(BulkFonts.DEFAULT_ID, BulkFonts.fonts().get(0).id);
    }

    @Test
    public void idsUniqueAndAssetsAreTtf() {
        Set<String> ids = new HashSet<>();
        Set<String> files = new HashSet<>();
        for (BulkFonts.FontInfo f : BulkFonts.fonts()) {
            assertTrue("duplicate font id: " + f.id, ids.add(f.id));
            assertFalse("font naam khaali: " + f.id, f.name.trim().isEmpty());
            if (f.assetFile != null) {
                assertTrue("asset .ttf hona chahiye: " + f.assetFile, f.assetFile.endsWith(".ttf"));
                assertTrue("duplicate asset file: " + f.assetFile, files.add(f.assetFile));
                assertEquals(BulkFonts.ASSET_DIR + f.assetFile, f.assetPath());
            }
        }
    }

    @Test
    public void defaultHasNoAsset() {
        BulkFonts.FontInfo def = BulkFonts.byId(BulkFonts.DEFAULT_ID);
        assertNull(def.assetFile);
        assertNull(def.assetPath());
    }

    @Test
    public void byIdAndValidity() {
        assertEquals("Poppins", BulkFonts.byId("poppins").name);
        assertEquals("Bebas Neue", BulkFonts.byId("bebasneue").name);
        assertTrue(BulkFonts.isValidId("orbitron"));
        assertTrue(BulkFonts.isValidId(BulkFonts.DEFAULT_ID));
        assertFalse(BulkFonts.isValidId("comic-sans"));
        assertFalse(BulkFonts.isValidId(null));
        // Unknown id hamesha Default par girta hai
        assertEquals(BulkFonts.DEFAULT_ID, BulkFonts.byId("nope").id);
        assertEquals(BulkFonts.DEFAULT_ID, BulkFonts.byId(null).id);
    }

    @Test
    public void factoryDefaultIsPoppins() {
        // v128: saved choice na ho to factory font Poppins hai
        assertEquals("poppins", BulkFonts.FACTORY_ID);
        assertTrue(BulkFonts.isValidId(BulkFonts.FACTORY_ID));
        assertEquals("Poppins", BulkFonts.byId(BulkFonts.FACTORY_ID).name);
    }
}
