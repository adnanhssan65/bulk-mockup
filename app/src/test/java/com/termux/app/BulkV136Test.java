package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Termux Mod v136 — hidden engine home + one-time migration +
 * drop-folder lifecycle ke pure-logic tests.
 *
 * Owner ka hukum: shared storage par sirf TermuxMod/Project N
 * folders; mockup_project_<N> wahan kabhi nahi. Engine $HOME me.
 * Ek dafa setup ke baad owner ka delete kiya hua Project N folder
 * app khud wapas NAHI banati.
 */
public class BulkV136Test {

    // ------------------------------------------------------------ paths

    @Test
    public void engineLivesInHome() {
        assertEquals("/data/data/com.termux/files/home", BulkProjects.HOME_PATH);
        assertEquals(com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH,
            BulkProjects.HOME_PATH);
        assertEquals("/data/data/com.termux/files/home/mockup_project_7",
            BulkProjects.homeProjectDir(7).getPath());
        assertEquals("/data/data/com.termux/files/home/mockup_project_7",
            BulkProjects.projectDir(7).getPath());
    }

    @Test
    public void engineSubfoldersFollowHome() {
        assertEquals("/data/data/com.termux/files/home/mockup_project_7/7_wallpapers",
            BulkProjects.wallpapersDir(7).getPath());
        assertEquals("/data/data/com.termux/files/home/mockup_project_7/7_Mockups",
            BulkProjects.mockupsDir(7).getPath());
        assertEquals("/data/data/com.termux/files/home/mockup_project_7/7_Ready_Wallpapers",
            BulkProjects.readyDir(7).getPath());
    }

    @Test
    public void sharedStorageHasNoEnginePath() {
        // projectDir kabhi shared storage ki taraf ishara nahi karta
        assertFalse(BulkProjects.projectDir(1).getPath()
            .startsWith(BulkProjects.STORAGE_ROOT));
        // Purani jagahen sirf migration SOURCES ke tor par rehti hain
        assertEquals("/storage/emulated/0/TermuxMod/mockup_project_7",
            BulkProjects.newProjectDir(7).getPath());
        assertEquals("/storage/emulated/0/mockup_project_7",
            BulkProjects.oldProjectDir(7).getPath());
    }

    // ------------------------------------------------- migration pure

    @Test
    public void engineSourcePriority() {
        // TermuxMod wali copy (v133) pehli tarjeeh
        assertEquals(BulkHomeMigration.EngineSource.FROM_MOD,
            BulkHomeMigration.chooseEngineSource(true, true));
        assertEquals(BulkHomeMigration.EngineSource.FROM_MOD,
            BulkHomeMigration.chooseEngineSource(true, false));
        assertEquals(BulkHomeMigration.EngineSource.FROM_ROOT,
            BulkHomeMigration.chooseEngineSource(false, true));
        assertEquals(BulkHomeMigration.EngineSource.NONE,
            BulkHomeMigration.chooseEngineSource(false, false));
    }

    @Test
    public void childClassification() {
        assertEquals(BulkHomeMigration.ChildKind.INPUTS,
            BulkHomeMigration.classifyChild(5, "5_wallpapers"));
        assertEquals(BulkHomeMigration.ChildKind.MOCKUPS,
            BulkHomeMigration.classifyChild(5, "5_Mockups"));
        assertEquals(BulkHomeMigration.ChildKind.READY,
            BulkHomeMigration.classifyChild(5, "5_Ready_Wallpapers"));
        assertEquals(BulkHomeMigration.ChildKind.BATCHES,
            BulkHomeMigration.classifyChild(5, "Ready_Upload_Batches"));
        // Dusre project ke subfolders engine ka hissa hain (rukh nahi badalta)
        assertEquals(BulkHomeMigration.ChildKind.ENGINE,
            BulkHomeMigration.classifyChild(5, "6_wallpapers"));
        assertEquals(BulkHomeMigration.ChildKind.ENGINE,
            BulkHomeMigration.classifyChild(5, "adnan_core.py"));
        assertEquals(BulkHomeMigration.ChildKind.ENGINE,
            BulkHomeMigration.classifyChild(5, null));
    }

    @Test
    public void outputRoutingToMirror() {
        assertEquals("Mockups",
            BulkHomeMigration.mirrorSubfolder(BulkHomeMigration.ChildKind.MOCKUPS));
        assertEquals("Ready Wallpapers",
            BulkHomeMigration.mirrorSubfolder(BulkHomeMigration.ChildKind.READY));
        assertEquals("Batches",
            BulkHomeMigration.mirrorSubfolder(BulkHomeMigration.ChildKind.BATCHES));
        // Inputs or engine mirror me nahi jate
        assertNull(BulkHomeMigration.mirrorSubfolder(BulkHomeMigration.ChildKind.INPUTS));
        assertNull(BulkHomeMigration.mirrorSubfolder(BulkHomeMigration.ChildKind.ENGINE));
        assertNull(BulkHomeMigration.mirrorSubfolder(null));
    }

    @Test
    public void mergeNamesNeverOverwrite() {
        Set<String> taken = new HashSet<>(Arrays.asList("a.jpg", "b.jpg"));
        List<String[]> plan = BulkHomeMigration.planMergeNames(
            Arrays.asList("a.jpg", "c.jpg", "a.jpg"), taken);
        assertEquals(3, plan.size());
        // Pehla a.jpg takraya -> suffix wala naam, koi overwrite nahi
        assertEquals("a.jpg", plan.get(0)[0]);
        assertFalse("a.jpg".equals(plan.get(0)[1]));
        assertTrue(taken.contains(plan.get(0)[1]));
        // Free naam waisa ka waisa
        assertEquals("c.jpg", plan.get(1)[1]);
        // Dusra a.jpg pehle plan wale se bhi alag
        assertFalse(plan.get(2)[1].equals(plan.get(0)[1]));
        assertFalse("a.jpg".equals(plan.get(2)[1]));
        // Plan ke baad saare dest taken me hain (agli merge bhi safe)
        assertTrue(taken.contains("c.jpg"));
        assertEquals(5, taken.size());
    }

    @Test
    public void mergeNamesHandlesNulls() {
        assertTrue(BulkHomeMigration.planMergeNames(null, null).isEmpty());
        List<String[]> plan = BulkHomeMigration.planMergeNames(
            new ArrayList<>(Arrays.asList("x.png")), null);
        assertEquals("x.png", plan.get(0)[1]);
    }

    // ---------------------------------------------------- lifecycle

    @Test
    public void dropFoldersCreatedOnlyAtInit() {
        // Pehli dafa setup: banana jaiz hai
        assertTrue(BulkHomeMigration.mayCreateDropFolders(false));
        // Init ke baad: app khud KABHI nahi banati (owner ka delete
        // kiya hua folder waisa hi rehta hai)
        assertFalse(BulkHomeMigration.mayCreateDropFolders(true));
    }

    @Test
    public void missingDropFolderIsSkipped() {
        assertTrue(BulkHomeMigration.shouldUseDropFolder(true));
        assertFalse(BulkHomeMigration.shouldUseDropFolder(false));
    }

    // ------------------------------------------------------------ version

    @Test
    public void versionCodeIs136() {
        assertEquals(136, com.termux.BuildConfig.VERSION_CODE);
    }
}
