package com.termux.app;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Termux Mod v133 — naye pure helpers ke tests:
 *   - Themes palette table + default mapping (BulkPalettes)
 *   - Delete popup ka menu map (BulkDeleteMenu)
 *   - Move auto-delete ka faisla (BulkStorageAccess.shouldDeleteDirectly)
 *   - Folder shift ki safety + location resolution (BulkMigration / BulkProjects)
 *   - Sort/Grid persisted prefs ke defaults (BulkGalleryState)
 *   - Drop folder preview video ke naam (BulkDropFolders)
 */
public class BulkV133Test {

    // ------------------------------------------------------------ Themes

    @Test
    public void themeTableSevenInOrder() {
        List<BulkPalettes.Palette> all = BulkPalettes.all();
        assertEquals(7, all.size());
        assertEquals("default", all.get(0).id);
        assertEquals("dark", all.get(1).id);
        assertEquals("midnight", all.get(2).id);
        assertEquals("royal", all.get(3).id);
        assertEquals("ocean", all.get(4).id);
        assertEquals("sunset", all.get(5).id);
        assertEquals("rose", all.get(6).id);
        assertEquals("Default", all.get(0).name);
        assertEquals("Royal Purple", all.get(3).name);
        // Ids sab unique
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (BulkPalettes.Palette p : all) assertTrue(ids.add(p.id));
    }

    @Test
    public void themeDefaultIsV132Light() {
        BulkPalettes.Palette d = BulkPalettes.defaultPalette();
        assertEquals("default", d.id);
        assertTrue(d.light);
        assertEquals(0xFFF3F8F3, d.bg);
        assertEquals(0xFF2E9E00, d.accent);
        assertEquals(0xFFFFFFFF, d.surface);
        // Factory default: koi saved choice na ho to Default hi
        assertEquals("default", BulkPalettes.resolve(null, null).id);
        assertEquals("default", BulkPalettes.resolve("bogus", null).id);
        assertEquals("default", BulkPalettes.resolve(null, Boolean.TRUE).id);
    }

    @Test
    public void themeDarkIsV132DarkAndLegacyMapping() {
        BulkPalettes.Palette dark = BulkPalettes.byId("dark");
        assertFalse(dark.light);
        assertEquals(0xFF030603, dark.bg);
        assertEquals(0xFF76FF03, dark.accent);
        // Purani boolean choice: light=false -> Dark theme
        assertEquals("dark", BulkPalettes.resolve(null, Boolean.FALSE).id);
        // Saved ui_theme hamesha legacy par jeet-ti hai
        assertEquals("ocean", BulkPalettes.resolve("ocean", Boolean.FALSE).id);
        assertEquals("dark", BulkPalettes.resolve("dark", Boolean.TRUE).id);
    }

    @Test
    public void themeNewPalettesKeyColors() {
        BulkPalettes.Palette mid = BulkPalettes.byId("midnight");
        assertFalse(mid.light);
        assertEquals(0xFF0F172A, mid.bg);
        assertEquals(0xFF1E293B, mid.surface);
        assertEquals(0xFF38BDF8, mid.accent);

        BulkPalettes.Palette royal = BulkPalettes.byId("royal");
        assertTrue(royal.light);
        assertEquals(0xFFF6F2FF, royal.bg);
        assertEquals(0xFFFFFFFF, royal.surface);
        assertEquals(0xFF7C3AED, royal.accent);

        BulkPalettes.Palette ocean = BulkPalettes.byId("ocean");
        assertTrue(ocean.light);
        assertEquals(0xFFEFFAF9, ocean.bg);
        assertEquals(0xFF0D9488, ocean.accent);

        BulkPalettes.Palette sunset = BulkPalettes.byId("sunset");
        assertTrue(sunset.light);
        assertEquals(0xFFFFF7ED, sunset.bg);
        assertEquals(0xFFEA580C, sunset.accent);

        BulkPalettes.Palette rose = BulkPalettes.byId("rose");
        assertTrue(rose.light);
        assertEquals(0xFFFFF5F7, rose.bg);
        assertEquals(0xFFE11D63, rose.accent);

        assertNull(BulkPalettes.byId("nope"));
        assertNull(BulkPalettes.byId(null));
        assertEquals("ui_theme", BulkPalettes.PREF_KEY);
    }

    // ------------------------------------------------------------ Delete menu

    @Test
    public void deleteMenuStandardFiveOptions() {
        List<BulkDeleteMenu.Option> opts = BulkDeleteMenu.optionsFor("adnan_core.py");
        assertEquals(5, opts.size());
        assertEquals(1, opts.get(0).digit);
        assertEquals("Tayaar Maal (Mockups + Ready Wallpapers)", opts.get(0).label);
        assertEquals(2, opts.get(1).digit);
        assertEquals("Sirf Mockups", opts.get(1).label);
        assertEquals(3, opts.get(2).digit);
        assertEquals("Input Wallpapers", opts.get(2).label);
        assertEquals(4, opts.get(3).digit);
        assertTrue(opts.get(3).warehouse);
        assertEquals(5, opts.get(4).digit);
        assertEquals("Backup se Wapas Lao (Restore)", opts.get(4).label);
        assertFalse(opts.get(0).warehouse);
        assertFalse(opts.get(4).warehouse);
    }

    @Test
    public void deleteMenuSystemCoreThreeOptions() {
        List<BulkDeleteMenu.Option> opts = BulkDeleteMenu.optionsFor("system_core.py");
        assertEquals(3, opts.size());
        assertEquals("Input (Images Only)", opts.get(0).label);
        assertEquals("Tayaar Maal (Images Only)", opts.get(1).label);
        assertEquals(3, opts.get(2).digit);
        assertTrue(opts.get(2).warehouse);
    }

    @Test
    public void deleteMenuWarehouseDigits() {
        assertTrue(BulkDeleteMenu.isWarehouse("w9.py", 4));
        assertFalse(BulkDeleteMenu.isWarehouse("w9.py", 1));
        assertFalse(BulkDeleteMenu.isWarehouse("w9.py", 5));
        assertTrue(BulkDeleteMenu.isWarehouse("system_core.py", 3));
        assertFalse(BulkDeleteMenu.isWarehouse("system_core.py", 4));
        assertNull(BulkDeleteMenu.optionFor("w9.py", 9));
        // Script maloom na ho to standard menu hi (del command wese bhi script se banta hai)
        assertEquals(5, BulkDeleteMenu.optionsFor(null).size());
        assertEquals(5, BulkDeleteMenu.optionsFor("dual_core.py").size());
        assertEquals(5, BulkDeleteMenu.optionsFor("project3_core.py").size());
        assertEquals(5, BulkDeleteMenu.optionsFor("adnan_core_4.py").size());
    }

    // ------------------------------------------------------------ Move auto-delete decision

    @Test
    public void directDeleteDecision() {
        assertTrue(BulkStorageAccess.shouldDeleteDirectly(true, "/storage/emulated/0/DCIM/a.jpg"));
        assertFalse(BulkStorageAccess.shouldDeleteDirectly(false, "/storage/emulated/0/DCIM/a.jpg"));
        assertFalse(BulkStorageAccess.shouldDeleteDirectly(true, null));
        assertFalse(BulkStorageAccess.shouldDeleteDirectly(true, ""));
        assertFalse(BulkStorageAccess.shouldDeleteDirectly(false, null));
    }

    // ------------------------------------------------------------ Migration safety + resolution

    @Test
    public void relocationSafetyPredicate() {
        assertTrue(BulkMigration.isRelocationSafe(
            "B = os.environ.get(\"MOCKUP_BASE\", \"/storage/emulated/0/mockup_project_1\")"));
        assertTrue(BulkMigration.isRelocationSafe(
            "BASE = os.path.dirname(os.path.abspath(__file__))"));
        assertFalse(BulkMigration.isRelocationSafe(
            "B = \"/storage/emulated/0/mockup_project_3\""));
        assertFalse(BulkMigration.isRelocationSafe(""));
        assertFalse(BulkMigration.isRelocationSafe(null));
    }

    @Test
    public void projectDirResolution() {
        File newDir = new File("/x/TermuxMod/mockup_project_5");
        File oldDir = new File("/x/mockup_project_5");
        assertEquals(newDir, BulkProjects.pickProjectDir(newDir, oldDir, true, false));
        assertEquals(oldDir, BulkProjects.pickProjectDir(newDir, oldDir, false, true));
        assertEquals(newDir, BulkProjects.pickProjectDir(newDir, oldDir, true, true));
        // Dono na hon to NAYI jagah (fresh install wahin banta hai)
        assertEquals(newDir, BulkProjects.pickProjectDir(newDir, oldDir, false, false));
    }

    @Test
    public void projectDirPaths() {
        assertEquals("/storage/emulated/0/TermuxMod/mockup_project_7",
            BulkProjects.newProjectDir(7).getPath());
        assertEquals("/storage/emulated/0/mockup_project_7",
            BulkProjects.oldProjectDir(7).getPath());
        // Test machine par /storage nahi — resolution nayi jagah dega
        assertEquals("/storage/emulated/0/TermuxMod/mockup_project_7",
            BulkProjects.projectDir(7).getPath());
    }

    @Test
    public void fullCommandSetsMockupBase() {
        String cmd = BulkProjects.fullCommand(4, "adnan_core.py", "process");
        assertTrue(cmd.startsWith("cd /storage/emulated/0/TermuxMod/mockup_project_4 && "));
        assertTrue(cmd.contains("MOCKUP_BASE=/storage/emulated/0/TermuxMod/mockup_project_4 "));
        assertTrue(cmd.endsWith("python adnan_core.py process 4"));
    }

    // ------------------------------------------------------------ Sort/Grid prefs

    @Test
    public void sortPrefDefaultsAndClamp() {
        assertEquals(BulkGalleryState.SORT_NEWEST,
            BulkGalleryState.resolveSortPref(BulkGalleryState.PREF_ABSENT));
        assertEquals(BulkGalleryState.SORT_NEWEST, BulkGalleryState.resolveSortPref(0));
        assertEquals(BulkGalleryState.SORT_OLDEST, BulkGalleryState.resolveSortPref(1));
        assertEquals(BulkGalleryState.SORT_NAME, BulkGalleryState.resolveSortPref(2));
        assertEquals(BulkGalleryState.SORT_NEWEST, BulkGalleryState.resolveSortPref(99));
        assertEquals(BulkGalleryState.SORT_NEWEST, BulkGalleryState.resolveSortPref(-7));
    }

    @Test
    public void gridPrefDefaultIsThree() {
        assertEquals(3, BulkGalleryState.DEFAULT_GRID);
        assertEquals(3, BulkGalleryState.resolveGridPref(BulkGalleryState.PREF_ABSENT));
        assertEquals(2, BulkGalleryState.resolveGridPref(2));
        assertEquals(4, BulkGalleryState.resolveGridPref(4));
        assertEquals(6, BulkGalleryState.resolveGridPref(6));
        assertEquals(3, BulkGalleryState.resolveGridPref(1));
        assertEquals(3, BulkGalleryState.resolveGridPref(7));
        assertEquals(3, BulkGalleryState.resolveGridPref(0));
    }

    // ------------------------------------------------------------ Preview videos

    @Test
    public void previewVideoNames() {
        assertEquals("Preview - Project 5.mp4", BulkDropFolders.previewFileName(5));
        assertEquals("Preview - Project 14.mp4", BulkDropFolders.previewFileName(14));
        assertEquals("bulk/previews/p5.mp4", BulkDropFolders.previewAssetPath(5));
        // Preview video wallpaper kabhi nahi gini jati (fetch/count dono se bahar)
        assertFalse(BulkProjects.isCountableWallpaper(BulkDropFolders.previewFileName(5)));
        assertTrue(BulkProjects.isVideoFile(BulkDropFolders.previewFileName(5)));
    }
}
