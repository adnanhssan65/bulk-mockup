package com.termux.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

/**
 * Termux Mod v147 — owner ke do kaam:
 *  (A) Bundled-runner AUTO-REPAIR on upgrade: version barhne par
 *      mojooda bundled projects ki purani/ajnabi runner file
 *      bundled wali se khud refresh (purani .bak_app me mehfooz),
 *      ghayab standard subfolders bhi ban jate hain. ZIP-added
 *      projects kabhi nahi chhede jate.
 *  (B) Projects 20/21 bhi BUNDLED (kul 20): preview range, prompt
 *      profiles (20 = SINGLE — sirf Mode sawal; 21 = NONE — koi
 *      sawal nahi), naam parse + script detection.
 */
public class BulkV147Test {

    // ------------------------------------------------------------ (A) version gate

    @Test
    public void repairRunsOnlyOnVersionIncrease() {
        assertTrue(BulkProjectRepair.shouldRunRepair(146, 147));
        assertTrue(BulkProjectRepair.shouldRunRepair(0, 147)); // fresh install record
        assertFalse(BulkProjectRepair.shouldRunRepair(147, 147)); // same version
        assertFalse(BulkProjectRepair.shouldRunRepair(147, 146)); // downgrade
    }

    // ------------------------------------------------------------ (A) faisla (decide)

    @Test
    public void repairDecisionCases() {
        // bundled + runner mukhtalif → REPLACE
        assertEquals(BulkProjectRepair.Decision.REPLACE,
            BulkProjectRepair.decide(true, true, true));
        // bundled + runner identical → NOOP
        assertEquals(BulkProjectRepair.Decision.NOOP_IDENTICAL,
            BulkProjectRepair.decide(true, true, false));
        // ghair-bundled (ZIP-added) → kabhi nahi, runner mukhtalif ho tab bhi
        assertEquals(BulkProjectRepair.Decision.SKIP_NOT_BUNDLED,
            BulkProjectRepair.decide(false, true, true));
        // bundled lekin folder hi nahi → SKIP (fresh install ka kaam hai)
        assertEquals(BulkProjectRepair.Decision.SKIP_NO_DIR,
            BulkProjectRepair.decide(true, false, true));
    }

    @Test
    public void runnerNameResolution() {
        // marker pehle
        assertEquals("adnan_core.py",
            BulkProjectRepair.resolveRunnerName("adnan_core.py", "w1.py"));
        // marker na ho / khaali ho → bundled naam
        assertEquals("w7.py", BulkProjectRepair.resolveRunnerName(null, "w7.py"));
        assertEquals("w7.py", BulkProjectRepair.resolveRunnerName("  ", "w7.py"));
        // kharab marker (path wala) → bundled naam hi mehfooz hai
        assertEquals("w7.py", BulkProjectRepair.resolveRunnerName("../evil.py", "w7.py"));
    }

    // ------------------------------------------------------------ (A) repair simulation

    private static void write(File f, byte[] data) throws Exception {
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
    }

    @Test
    public void repairSimulationStaleForeignRunner() throws Exception {
        // Wohi manzar jo owner ke dost ke phone par mila: $HOME me
        // Project 1 ka folder hai lekin runner kisi purani ajnabi
        // lineage ki hai (garbage bytes), ek subfolder ghayab hai,
        // or user ki apni image Mockups me pari hai.
        File root = Files.createTempDirectory("v147repair").toFile();
        try {
            File dir = new File(root, "mockup_project_1");
            byte[] garbage = "PURANI AJNABI FILE — hardcoded /storage wala rasta"
                .getBytes(StandardCharsets.UTF_8);
            byte[] bundled = "# bundled asal runner (fire-and-forget)\nprint('ok')\n"
                .getBytes(StandardCharsets.UTF_8);
            write(new File(dir, "adnan_core.py"), garbage);
            byte[] userImage = new byte[]{(byte) 0xFF, (byte) 0xD8, 1, 2, 3};
            write(new File(dir, "1_Mockups/user_keep.jpg"), userImage);
            write(new File(dir, "1_wallpapers/in.jpg"), userImage);
            // 1_Ready_Wallpapers or Ready_Upload_Batches jaan boojh kar ghayab

            boolean replaced = BulkProjectRepair.repairProjectDir(dir, 1, "adnan_core.py", bundled);
            assertTrue("runner badli jani chahiye", replaced);

            // runner ab bundled bytes ki hai
            assertArrayEquals(bundled,
                Files.readAllBytes(new File(dir, "adnan_core.py").toPath()));
            // purani file .bak_app me mehfooz
            assertArrayEquals(garbage,
                Files.readAllBytes(new File(dir, "adnan_core.py" + BulkProjectRepair.BACKUP_SUFFIX).toPath()));
            // user ki image bilkul waisi ki waisi
            assertArrayEquals(userImage,
                Files.readAllBytes(new File(dir, "1_Mockups/user_keep.jpg").toPath()));
            // ghayab subfolders ban gaye
            assertTrue(new File(dir, "1_Ready_Wallpapers").isDirectory());
            assertTrue(new File(dir, "Ready_Upload_Batches").isDirectory());

            // Dusri dafa: idempotent — kuch nahi badalta, .bak_app bhi wahi
            boolean again = BulkProjectRepair.repairProjectDir(dir, 1, "adnan_core.py", bundled);
            assertFalse("dusri dafa no-op hona chahiye", again);
            assertArrayEquals(garbage,
                Files.readAllBytes(new File(dir, "adnan_core.py" + BulkProjectRepair.BACKUP_SUFFIX).toPath()));
            assertArrayEquals(bundled,
                Files.readAllBytes(new File(dir, "adnan_core.py").toPath()));
        } finally {
            BulkProjectInstaller.deleteRecursive(root);
        }
    }

    @Test
    public void repairWritesMissingRunnerWithoutBackup() throws Exception {
        File root = Files.createTempDirectory("v147repair2").toFile();
        try {
            File dir = new File(root, "mockup_project_7");
            assertTrue(dir.mkdirs());
            byte[] bundled = "# w7 bundled\n".getBytes(StandardCharsets.UTF_8);
            boolean replaced = BulkProjectRepair.repairProjectDir(dir, 7, "w7.py", bundled);
            assertTrue(replaced);
            assertArrayEquals(bundled, Files.readAllBytes(new File(dir, "w7.py").toPath()));
            assertFalse(new File(dir, "w7.py" + BulkProjectRepair.BACKUP_SUFFIX).exists());
            // chaaron standard subfolders bhi ban gaye
            for (String sub : BulkProjects.subfolderNames(7)) {
                assertTrue(sub, new File(dir, sub).isDirectory());
            }
        } finally {
            BulkProjectInstaller.deleteRecursive(root);
        }
    }

    @Test
    public void ensureSubfoldersCountsOnlyMissing() throws Exception {
        File root = Files.createTempDirectory("v147repair3").toFile();
        try {
            File dir = new File(root, "mockup_project_3");
            assertTrue(new File(dir, "3_wallpapers").mkdirs());
            int created = BulkProjectRepair.ensureSubfolders(dir, 3);
            assertEquals(3, created); // 3_Mockups, 3_Ready_Wallpapers, Ready_Upload_Batches
            assertEquals(0, BulkProjectRepair.ensureSubfolders(dir, 3));
        } finally {
            BulkProjectInstaller.deleteRecursive(root);
        }
    }

    // ------------------------------------------------------------ (B) bundled 20/21: previews + parse

    @Test
    public void bundledPreviewRangeCovers20And21() {
        assertEquals(21, BulkPreviewSource.LAST_BUNDLED_PROJECT);
        for (int n : new int[]{20, 21}) {
            assertTrue("p" + n, BulkPreviewSource.hasBundledRange(n));
            assertTrue("bundled p" + n, BulkPreviewSource.isBundledProject(n));
            assertEquals("bulk/previews/p" + n + ".jpg",
                BulkPreviewSource.bundledAssetPath(n));
            assertEquals("bulk/previews/p" + n + ".mp4",
                BulkDropFolders.previewAssetPath(n));
            assertEquals(BulkPreviewSource.KIND_BUNDLED,
                BulkPreviewSource.choose(Collections.emptyList(), n, true).kind);
        }
        // Project 19 kabhi tha hi nahi — bundled set me nahi
        assertFalse(BulkPreviewSource.hasBundledRange(19));
        assertFalse(BulkPreviewSource.isBundledProject(19));
    }

    @Test
    public void bundledNumbersParseFor20And21() {
        // Provisioning seeding bundled numbers assets ke zip naamon
        // se isi parse se nikalti hai — 20/21 ke naam sahi parse hon
        // to un ke records bhi baqiyon ki tarah seed ho jate hain.
        assertEquals(20, BulkProjects.parseFolderNumber("mockup_project_20"));
        assertEquals(21, BulkProjects.parseFolderNumber("mockup_project_21"));
        assertTrue(BulkDropFolders.shouldProvision(false));
        assertFalse(BulkDropFolders.shouldProvision(true));
    }

    @Test
    public void mainScriptDetectionFor20And21() {
        assertEquals("w20.py", BulkProjects.detectMainScript(
            Arrays.asList("w20.py", "README.txt"), 20));
        assertEquals("w21.py", BulkProjects.detectMainScript(
            Arrays.asList("w21.py", "template.png", "README.txt"), 21));
    }

    // ------------------------------------------------------------ (B) prompt profiles 20/21

    @Test
    public void promptProfilesForBundled20And21() {
        assertEquals(BulkRunSettings.PromptProfile.SINGLE,
            BulkRunSettings.profileFor(20, "w20.py"));
        assertEquals(BulkRunSettings.PromptProfile.SINGLE,
            BulkRunSettings.profileForScript("w20.py"));
        assertEquals(BulkRunSettings.PromptProfile.NONE,
            BulkRunSettings.profileFor(21, "w21.py"));
        assertEquals(BulkRunSettings.PromptProfile.NONE,
            BulkRunSettings.profileForScript("w21.py"));
        // Aam w<N> wale projects ADNAN hi rehte hain
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileForScript("w15.py"));
    }

    @Test
    public void promptAnswersFor20And21() {
        // P20: sirf mode jawab — factory default (HD) par '1'
        assertEquals(Collections.singletonList("1"),
            BulkRunSettings.answersForProfile(BulkRunSettings.PromptProfile.SINGLE,
                BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP));
        assertEquals(Collections.singletonList("2"),
            BulkRunSettings.answersForProfile(BulkRunSettings.PromptProfile.SINGLE,
                BulkRunSettings.ANSWER_NORMAL, BulkRunSettings.ANSWER_ORIGINAL));
        // P21: koi jawab nahi — command bina pipe ke waisi ki waisi
        assertTrue(BulkRunSettings.answersForProfile(BulkRunSettings.PromptProfile.NONE,
            BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP).isEmpty());
        String cmd = "cd /x && python w21.py process 21";
        assertEquals(cmd, BulkRunSettings.buildPipedCommand(cmd,
            BulkRunSettings.answersForProfile(BulkRunSettings.PromptProfile.NONE,
                BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP)));
    }
}
