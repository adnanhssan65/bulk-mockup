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
}
