package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Termux Mod (v126) — BulkStrings language table ki tests: har key ki dono zubanein, owner ki di hui exact Roman Urdu labels, or jo alfaaz dono me ek hi rehne hain. */
public class BulkStringsTest {

    // ------------------------------------------------------------ completeness

    @Test
    public void everyKeyHasBothLanguages() {
        assertFalse("strings table khaali nahi honi chahiye", BulkStrings.keys().isEmpty());
        for (String key : BulkStrings.keys()) {
            assertTrue("key ki dono zubanein lazmi: " + key, BulkStrings.hasBothLanguages(key));
            assertFalse("EN value khaali: " + key, BulkStrings.get(key, false).trim().isEmpty());
            assertFalse("UR value khaali: " + key, BulkStrings.get(key, true).trim().isEmpty());
        }
    }

    @Test
    public void unknownKeyFallsBackToKey() {
        assertEquals("no_such_key_xyz", BulkStrings.get("no_such_key_xyz", false));
        assertEquals("no_such_key_xyz", BulkStrings.get("no_such_key_xyz", true));
    }

    // ------------------------------------------------------------ owner-mandated Roman Urdu labels

    @Test
    public void ownerMandatedUrduLabels() {
        assertEquals("Wallpaper Add Karo", BulkStrings.get(BulkStrings.ADD_WALLPAPERS, true));
        assertEquals("Naya Project (ZIP)", BulkStrings.get(BulkStrings.NEW_PROJECT_ZIP, true));
        assertEquals("Project Select Karo", BulkStrings.get(BulkStrings.SELECT_PROJECT, true));
        assertEquals("Add Karo", BulkStrings.get(BulkStrings.ADD, true));
        assertEquals("Abhi Chalao", BulkStrings.get(BulkStrings.RUN_NOW, true));
        assertEquals("Add Kiye Hue Wallpapers", BulkStrings.get(BulkStrings.PREVIEW_ADDED, true));
        assertEquals("Saaf Karo", BulkStrings.get(BulkStrings.CLEAR, true));
        assertEquals("Select Karo", BulkStrings.get(BulkStrings.SELECT, true));
        assertEquals("Sort Karo", BulkStrings.get(BulkStrings.SORT, true));
        assertEquals("Update Check Karo", BulkStrings.get(BulkStrings.CHECK_FOR_UPDATES, true));
        assertEquals("App Ke Bare Me", BulkStrings.get(BulkStrings.ABOUT, true));
        assertEquals("Wapas", BulkStrings.get(BulkStrings.BACK, true));
        assertEquals("Koi Project Install Nahi", BulkStrings.get(BulkStrings.NO_PROJECTS, true));
        assertEquals("Project Install Ho Gaya!", BulkStrings.get(BulkStrings.INSTALLED_TITLE, true));
    }

    @Test
    public void englishDefaults() {
        assertEquals("Add Wallpapers", BulkStrings.get(BulkStrings.ADD_WALLPAPERS, false));
        assertEquals("New Project (ZIP)", BulkStrings.get(BulkStrings.NEW_PROJECT_ZIP, false));
        assertEquals("Select Project", BulkStrings.get(BulkStrings.SELECT_PROJECT, false));
        assertEquals("Add", BulkStrings.get(BulkStrings.ADD, false));
        assertEquals("Run Now", BulkStrings.get(BulkStrings.RUN_NOW, false));
        assertEquals("Preview Added Images", BulkStrings.get(BulkStrings.PREVIEW_ADDED, false));
        assertEquals("Clear", BulkStrings.get(BulkStrings.CLEAR, false));
        assertEquals("Check for Updates", BulkStrings.get(BulkStrings.CHECK_FOR_UPDATES, false));
        assertEquals("About", BulkStrings.get(BulkStrings.ABOUT, false));
        assertEquals("Back", BulkStrings.get(BulkStrings.BACK, false));
        assertEquals("No Projects Installed", BulkStrings.get(BulkStrings.NO_PROJECTS, false));
        assertEquals("Project Installed!", BulkStrings.get(BulkStrings.INSTALLED_TITLE, false));
    }

    /** Ye alfaaz owner khud dono zubano me yehi istemal karta hai — badalne nahi chahiye. */
    @Test
    public void keptWordsSameInBothLanguages() {
        String[] kept = {
            BulkStrings.PROJECTS, BulkStrings.TERMINAL,
            BulkStrings.PERMISSIONS, BulkStrings.DUPLICATES,
        };
        for (String key : kept) {
            assertEquals("dono zubano me ek hi: " + key,
                BulkStrings.get(key, false), BulkStrings.get(key, true));
        }
        assertEquals("Projects", BulkStrings.get(BulkStrings.PROJECTS, true));
        assertEquals("Terminal", BulkStrings.get(BulkStrings.TERMINAL, true));
        assertEquals("Permissions", BulkStrings.get(BulkStrings.PERMISSIONS, true));
        assertEquals("Duplicates", BulkStrings.get(BulkStrings.DUPLICATES, true));
    }
}
