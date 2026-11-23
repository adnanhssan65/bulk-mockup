package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Termux Mod (v124) — BulkProjects ki unit tests: number/script/ZIP detection, alias + conf lines. */
public class BulkProjectsTest {

    // ------------------------------------------------------------ numbers

    @Test
    public void numberFromName() {
        assertEquals(8, BulkProjects.parseProjectNumber("Project 8", "w8"));
        assertEquals(15, BulkProjects.parseProjectNumber("Project 15", "w15"));
        assertEquals(3, BulkProjects.parseProjectNumber("project 3", null));
    }

    @Test
    public void numberFromCommandWhenNameHasNone() {
        assertEquals(12, BulkProjects.parseProjectNumber("Mera Project", "w12"));
        assertEquals(12, BulkProjects.parseProjectNumber(null, " w12 "));
    }

    @Test
    public void numberUnknown() {
        assertEquals(-1, BulkProjects.parseProjectNumber("Mera Project", "kuch-or"));
        assertEquals(-1, BulkProjects.parseProjectNumber(null, null));
        assertEquals(-1, BulkProjects.parseProjectNumber("", ""));
    }

    @Test
    public void folderNumber() {
        assertEquals(15, BulkProjects.parseFolderNumber("mockup_project_15"));
        assertEquals(1, BulkProjects.parseFolderNumber("mockup_project_1"));
        assertEquals(-1, BulkProjects.parseFolderNumber("mockup_project_x"));
        assertEquals(-1, BulkProjects.parseFolderNumber("project_15"));
        assertEquals(-1, BulkProjects.parseFolderNumber(null));
    }

    // ------------------------------------------------------------ script detection

    @Test
    public void scriptPriorityWFileWins() {
        List<String> files = Arrays.asList("adnan_core.py", "w15.py", "mockup.png", "shape.png");
        assertEquals("w15.py", BulkProjects.detectMainScript(files, 15));
    }

    @Test
    public void scriptPriorityCoreOrder() {
        assertEquals("adnan_core.py", BulkProjects.detectMainScript(
            Arrays.asList("dual_core.py", "adnan_core.py"), 4));
        assertEquals("dual_core.py", BulkProjects.detectMainScript(
            Arrays.asList("project3_core.py", "dual_core.py"), 4));
        assertEquals("project3_core.py", BulkProjects.detectMainScript(
            Arrays.asList("adnan_core_4.py", "project3_core.py"), 5));
        assertEquals("adnan_core_4.py", BulkProjects.detectMainScript(
            Arrays.asList("adnan_core_4.py", "make_assets.py"), 6));
    }

    @Test
    public void scriptFallbackSinglePy() {
        assertEquals("engine.py", BulkProjects.detectMainScript(
            Arrays.asList("engine.py", "make_assets.py", "mockup.png"), 9));
    }

    @Test
    public void scriptAmbiguousOrMissing() {
        assertNull(BulkProjects.detectMainScript(
            Arrays.asList("a.py", "b.py", "make_assets.py"), 9));
        assertNull(BulkProjects.detectMainScript(
            Arrays.asList("make_assets.py", "mockup.png"), 9));
        assertNull(BulkProjects.detectMainScript(Arrays.asList(), 9));
        assertNull(BulkProjects.detectMainScript(null, 9));
    }

    // ------------------------------------------------------------ ZIP root detection

    @Test
    public void zipRootFolderDetected() {
        List<String> entries = Arrays.asList(
            "mockup_project_14/w14.py",
            "mockup_project_14/fonts/Inter-Bold.otf",
            "mockup_project_14/README.txt",
            "mockup_project_14/14_wallpapers/");
        assertEquals("mockup_project_14", BulkProjects.findProjectRoot(entries));
    }

    @Test
    public void zipFilesAtRootNotDetected() {
        // Files seedha root par — number maloom nahi ho sakta, null ana chahiye
        List<String> entries = Arrays.asList("w15.py", "mockup.png", "fonts/a.ttf");
        assertNull(BulkProjects.findProjectRoot(entries));
    }

    @Test
    public void zipMacJunkIgnored() {
        List<String> entries = Arrays.asList(
            "__MACOSX/mockup_project_99/._w99.py",
            "mockup_project_15/w15.py",
            "mockup_project_15/mockup.png");
        assertEquals("mockup_project_15", BulkProjects.findProjectRoot(entries));
    }

    @Test
    public void zipEmptyOrNull() {
        assertNull(BulkProjects.findProjectRoot(Arrays.asList()));
        assertNull(BulkProjects.findProjectRoot(null));
    }

    // ------------------------------------------------------------ aliases

    @Test
    public void aliasLinesUniform() {
        List<String> lines = BulkProjects.aliasLines(15, "w15.py");
        assertEquals(3, lines.size());
        assertEquals("alias w15='cd /storage/emulated/0/mockup_project_15 && python w15.py process 15'", lines.get(0));
        assertEquals("alias d15='cd /storage/emulated/0/mockup_project_15 && python w15.py del 15'", lines.get(1));
        assertEquals("alias b15='cd /storage/emulated/0/mockup_project_15 && python w15.py batch 15'", lines.get(2));
    }

    @Test
    public void aliasAlreadyDefinedCheck() {
        String bashrc = "# meri bashrc\nalias w1='cd x && python adnan_core.py process 1'\nalias w15='cd y'\n";
        assertTrue(BulkProjects.aliasAlreadyDefined(bashrc, "w1"));
        assertTrue(BulkProjects.aliasAlreadyDefined(bashrc, "w15"));
        assertFalse(BulkProjects.aliasAlreadyDefined(bashrc, "d1"));
        // "w1" ki talaash "w15" wali line se match nahi honi chahiye
        assertFalse(BulkProjects.aliasAlreadyDefined("alias w15='cd y'\n", "w1"));
        assertFalse(BulkProjects.aliasAlreadyDefined(null, "w1"));
        assertFalse(BulkProjects.aliasAlreadyDefined("", "w1"));
    }

    // ------------------------------------------------------------ projects.conf

    @Test
    public void confLineFormat() {
        assertEquals("Project 15 | w15 | b15 | d15", BulkProjects.confLine(15));
    }

    @Test
    public void confHasProjectCheck() {
        String conf = "# list\nProject 1 | w1 | b1 | d1\nProject 15 | w15 | b15 | d15\n";
        assertTrue(BulkProjects.confHasProject(conf, 1));
        assertTrue(BulkProjects.confHasProject(conf, 15));
        assertFalse(BulkProjects.confHasProject(conf, 5));
        // 1 ki talaash 15 wali line se match nahi honi chahiye
        assertFalse(BulkProjects.confHasProject("Project 15 | w15 | b15 | d15\n", 1));
        assertFalse(BulkProjects.confHasProject(null, 1));
        assertFalse(BulkProjects.confHasProject("", 1));
    }

    // ------------------------------------------------------------ merge

    @Test
    public void mergeConfFirstThenDisk() {
        List<ProjectsConf.Project> conf = ProjectsConf.parse(
            "Project 1 | w1 | b1 | d1\nProject 8 | w8 | b8 | d8\n");
        Map<Integer, String> disk = new HashMap<>();
        disk.put(8, "w8.py");      // conf me pehle se hai — dobara nahi ana chahiye
        disk.put(15, "w15.py");    // naya, script ke sath
        disk.put(16, null);        // naya, script nahi mili
        List<BulkProjects.ProjectInfo> merged = BulkProjects.mergeProjects(conf, disk);
        assertEquals(4, merged.size());
        assertEquals("Project 1", merged.get(0).name);
        assertTrue(merged.get(0).fromConf);
        assertEquals("Project 8", merged.get(1).name);
        assertEquals("Project 15", merged.get(2).name);
        assertFalse(merged.get(2).fromConf);
        assertEquals("cd /storage/emulated/0/mockup_project_15 && python w15.py process 15",
            merged.get(2).workCmd);
        assertEquals("cd /storage/emulated/0/mockup_project_15 && python w15.py batch 15",
            merged.get(2).batchCmd);
        assertEquals("cd /storage/emulated/0/mockup_project_15 && python w15.py del 15",
            merged.get(2).deleteCmd);
        assertEquals("Project 16", merged.get(3).name);
        assertNull(merged.get(3).workCmd);
    }

    @Test
    public void mergeEmptyInputs() {
        assertEquals(0, BulkProjects.mergeProjects(null, null).size());
        assertEquals(0, BulkProjects.mergeProjects(ProjectsConf.parse(""), new HashMap<>()).size());
    }

    // ------------------------------------------------------------ counts

    @Test
    public void countsLineFormat() {
        assertEquals("Wallpapers: 3 · Mockups: 1 · Ready: 2", BulkProjects.countsLine(3, 1, 2));
        assertEquals("Wallpapers: 0 · Mockups: 0 · Ready: 0", BulkProjects.countsLine(0, 0, 0));
    }

    @Test
    public void countFilesRealDir() throws Exception {
        File dir = Files.createTempDirectory("bulkprojects_test").toFile();
        try {
            assertEquals(0, BulkProjects.countFiles(dir));
            assertTrue(new File(dir, "a.jpg").createNewFile());
            assertTrue(new File(dir, "b.jpg").createNewFile());
            assertTrue(new File(dir, "sub").mkdir());
            assertEquals(2, BulkProjects.countFiles(dir)); // folder count nahi hota
            assertEquals(0, BulkProjects.countFiles(new File(dir, "nahi-hai")));
            assertEquals(0, BulkProjects.countFiles(null));
        } finally {
            File[] kids = dir.listFiles();
            if (kids != null) for (File k : kids) k.delete();
            dir.delete();
        }
    }

    @Test
    public void fullCommandFormat() {
        assertEquals("cd /storage/emulated/0/mockup_project_12 && python adnan_core.py process 12",
            BulkProjects.fullCommand(12, "adnan_core.py", "process"));
    }

    // ------------------------------------------------------------ v125: Run/Batch/Delete commands

    @Test
    public void runBatchDeleteCommands() {
        // Projects screen ke teenon actions — command ki shakal har action par
        assertEquals("cd /storage/emulated/0/mockup_project_9 && python w9.py process 9",
            BulkProjects.fullCommand(9, "w9.py", "process"));
        assertEquals("cd /storage/emulated/0/mockup_project_9 && python w9.py batch 9",
            BulkProjects.fullCommand(9, "w9.py", "batch"));
        assertEquals("cd /storage/emulated/0/mockup_project_9 && python w9.py del 9",
            BulkProjects.fullCommand(9, "w9.py", "del"));
    }

    // ------------------------------------------------------------ v125: ZIP add — exactly once

    /** BulkProjectInstaller.appendAliases ka decision loop (pure hissa) yahan simulate hota hai. */
    private static String simulateAliasAppend(String content, int n, String script) {
        StringBuilder add = new StringBuilder();
        String[] aliasNames = {"w" + n, "d" + n, "b" + n};
        List<String> lines = BulkProjects.aliasLines(n, script);
        for (int i = 0; i < aliasNames.length; i++) {
            if (!BulkProjects.aliasAlreadyDefined(content, aliasNames[i])) {
                add.append(lines.get(i)).append("\n");
            }
        }
        return content + add;
    }

    /** BulkProjectInstaller.appendConfLine ka decision (pure hissa). */
    private static String simulateConfAppend(String content, int n) {
        if (BulkProjects.confHasProject(content, n)) return content;
        return content + BulkProjects.confLine(n) + "\n";
    }

    private static int occurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    @Test
    public void aliasAppendExactlyOnceOnReAdd() {
        String bashrc = "# user ki apni file\n";
        bashrc = simulateAliasAppend(bashrc, 9, "w9.py");
        assertEquals(1, occurrences(bashrc, "alias w9="));
        assertEquals(1, occurrences(bashrc, "alias d9="));
        assertEquals(1, occurrences(bashrc, "alias b9="));
        // Dobara add (re-add) — kuch nahi badalna chahiye
        String again = simulateAliasAppend(bashrc, 9, "w9.py");
        assertEquals(bashrc, again);
    }

    @Test
    public void aliasAppendKeepsExistingUserAlias() {
        // User ki apni w9 pehle se hai (alag script) — sirf d9/b9 add hon, w9 waisi ki waisi
        String bashrc = "alias w9='cd /storage/emulated/0/mockup_project_9 && python custom.py process 9'\n";
        String out = simulateAliasAppend(bashrc, 9, "w9.py");
        assertEquals(1, occurrences(out, "alias w9="));
        assertTrue(out.contains("custom.py"));
        assertEquals(1, occurrences(out, "alias d9="));
        assertEquals(1, occurrences(out, "alias b9="));
    }

    @Test
    public void confAppendExactlyOnceOnReAdd() {
        String conf = simulateConfAppend("", 9);
        assertEquals(1, occurrences(conf, "Project 9 |"));
        String again = simulateConfAppend(conf, 9);
        assertEquals(conf, again);
        // Dusra project add ho sakta hai — pehla wala duplicate nahi hona chahiye
        String two = simulateConfAppend(again, 10);
        assertEquals(1, occurrences(two, "Project 9 |"));
        assertEquals(1, occurrences(two, "Project 10 |"));
    }

    @Test
    public void confAppendSkipsWhenCustomNameExists() {
        // projects.conf me custom naam ke sath project 9 pehle se hai (command w9 se pehchan)
        String conf = "Mera Project | w9 | b9 | d9\n";
        assertTrue(BulkProjects.confHasProject(conf, 9));
        assertEquals(conf, simulateConfAppend(conf, 9));
    }

    // ------------------------------------------------------------ v125: subfolders + dir helpers

    @Test
    public void subfolderNamesForInstall() {
        assertEquals(
            Arrays.asList("9_wallpapers", "9_Mockups", "9_Ready_Wallpapers", "Ready_Upload_Batches"),
            BulkProjects.subfolderNames(9));
        assertEquals(
            Arrays.asList("17_wallpapers", "17_Mockups", "17_Ready_Wallpapers", "Ready_Upload_Batches"),
            BulkProjects.subfolderNames(17));
    }

    @Test
    public void isImageFileCheck() {
        assertTrue(BulkProjects.isImageFile("photo.JPG"));
        assertTrue(BulkProjects.isImageFile("a.png"));
        assertTrue(BulkProjects.isImageFile("b.webp"));
        assertFalse(BulkProjects.isImageFile("script.py"));
        assertFalse(BulkProjects.isImageFile("notes.txt"));
        assertFalse(BulkProjects.isImageFile(null));
    }

    // ------------------------------------------------------------ v127 run filter

    @Test
    public void canRunNeedsAtLeastOneWallpaper() {
        assertFalse(BulkProjects.canRun(0));
        assertFalse(BulkProjects.canRun(-1));
        assertTrue(BulkProjects.canRun(1));
        assertTrue(BulkProjects.canRun(250));
    }

    @Test
    public void filterRunnableKeepsOnlyProjectsWithWallpapers() {
        java.util.List<BulkProjects.ProjectInfo> all = java.util.Arrays.asList(
            new BulkProjects.ProjectInfo(1, "Project 1", "w1", "b1", "d1", true),
            new BulkProjects.ProjectInfo(2, "Project 2", "w2", "b2", "d2", true),
            new BulkProjects.ProjectInfo(3, "Project 3", "w3", "b3", "d3", true),
            new BulkProjects.ProjectInfo(0, "Zero", null, null, null, false));
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        counts.put(1, 5);
        counts.put(2, 0);
        // Project 3 ka count maloom hi nahi — run list me nahi ayega
        java.util.List<BulkProjects.ProjectInfo> out = BulkProjects.filterRunnable(all, counts);
        assertEquals(1, out.size());
        assertEquals(1, out.get(0).number);
    }

    @Test
    public void dirHelpersPointInsideProject() {
        assertEquals("/storage/emulated/0/mockup_project_7/7_wallpapers",
            BulkProjects.wallpapersDir(7).getPath());
        assertEquals("/storage/emulated/0/mockup_project_7/7_Mockups",
            BulkProjects.mockupsDir(7).getPath());
        assertEquals("/storage/emulated/0/mockup_project_7/7_Ready_Wallpapers",
            BulkProjects.readyDir(7).getPath());
    }
}
