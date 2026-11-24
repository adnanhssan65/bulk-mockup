package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Termux Mod v137 — owner ki v136 phone-test reports ke fixes:
 *  (a) tamam project lists number ke hisab se ascending;
 *  (b) ZIP-added projects (conf + disk dono se) har list me;
 *  (c) Run list sirf un projects ki jin ke INPUT (<N>_wallpapers)
 *      me asal wallpaper ho — "_temp" wali file ginti nahi;
 *  (d) ZIP-added projects ke Run bhi console popup me — prompt
 *      profile asal script ke khandan se (ADNAN / SYSTEM /
 *      UNKNOWN), terminal kabhi nahi.
 */
public class BulkV137Test {

    // ------------------------------------------------------------ (a)+(b) sort + enumeration

    @Test
    public void mergedListIsSortedByNumber() {
        List<ProjectsConf.Project> conf = ProjectsConf.parse(
            "Project 8 | w8 | b8 | d8\nProject 1 | w1 | b1 | d1\nProject 10 | w10 | b10 | d10\n");
        Map<Integer, String> disk = new HashMap<>();
        disk.put(3, "adnan_core.py");
        disk.put(18, "w18.py");
        List<BulkProjects.ProjectInfo> merged = BulkProjects.mergeProjects(conf, disk);
        assertEquals(5, merged.size());
        assertEquals(1, merged.get(0).number);
        assertEquals(3, merged.get(1).number);
        assertEquals(8, merged.get(2).number);
        assertEquals(10, merged.get(3).number);
        assertEquals(18, merged.get(4).number);
    }

    @Test
    public void zipAddedProjectAppearsFromDiskAndConf() {
        // Sirf disk par (conf me line nahi) — phir bhi list me
        Map<Integer, String> disk = new HashMap<>();
        disk.put(17, "w17.py");
        List<BulkProjects.ProjectInfo> merged =
            BulkProjects.mergeProjects(new ArrayList<>(), disk);
        assertEquals(1, merged.size());
        assertEquals(17, merged.get(0).number);
        assertEquals("Project 17", merged.get(0).name);
        assertTrue(merged.get(0).workCmd.contains("python w17.py process 17"));
        // Sirf conf me (disk scan miss) — phir bhi list me
        List<ProjectsConf.Project> conf = ProjectsConf.parse("Project 18 | w18 | b18 | d18\n");
        List<BulkProjects.ProjectInfo> merged2 =
            BulkProjects.mergeProjects(conf, new HashMap<>());
        assertEquals(1, merged2.size());
        assertEquals(18, merged2.get(0).number);
    }

    @Test
    public void duplicateConfLinesKeepFirstOnly() {
        List<ProjectsConf.Project> conf = ProjectsConf.parse(
            "Project 5 | w5 | b5 | d5\nProject 5 | w5 | b5 | d5\n");
        List<BulkProjects.ProjectInfo> merged =
            BulkProjects.mergeProjects(conf, new HashMap<>());
        assertEquals(1, merged.size());
        assertEquals(5, merged.get(0).number);
    }

    @Test
    public void sortByNumberPutsUnknownLast() {
        List<ProjectsConf.Project> conf = ProjectsConf.parse(
            "Project 12 | w12 | b12 | d12\nProject 2 | w2 | b2 | d2\n");
        List<BulkProjects.ProjectInfo> merged =
            BulkProjects.mergeProjects(conf, new HashMap<>());
        List<BulkProjects.ProjectInfo> sorted = BulkProjects.sortByNumber(merged);
        assertEquals(2, sorted.get(0).number);
        assertEquals(12, sorted.get(1).number);
        assertEquals(0, BulkProjects.sortByNumber(null).size());
    }

    // ------------------------------------------------------------ (c) run filter = INPUT count

    @Test
    public void runFilterUsesInputCountsOnly() {
        List<ProjectsConf.Project> conf = ProjectsConf.parse(
            "Project 7 | w7 | b7 | d7\nProject 10 | w10 | b10 | d10\nProject 1 | w1 | b1 | d1\n");
        List<BulkProjects.ProjectInfo> all =
            BulkProjects.mergeProjects(conf, new HashMap<>());
        Map<Integer, Integer> inputs = new HashMap<>();
        inputs.put(7, 0);   // input khali — ready/outputs hon bhi to Run me NAHI
        inputs.put(10, 0);
        inputs.put(1, 14);
        List<BulkProjects.ProjectInfo> runnable = BulkProjects.filterRunnable(all, inputs);
        assertEquals(1, runnable.size());
        assertEquals(1, runnable.get(0).number);
        // count map me number hi na ho to bhi bahar
        assertEquals(0, BulkProjects.filterRunnable(all, new HashMap<>()).size());
    }

    @Test
    public void tempFilesAreNotCountableWallpapers() {
        assertFalse(BulkProjects.isCountableWallpaper("img_temp.jpg"));
        assertFalse(BulkProjects.isCountableWallpaper("12_temp.png"));
        assertTrue(BulkProjects.isCountableWallpaper("photo.jpg"));
        assertTrue(BulkProjects.isCountableWallpaper("attempt.jpg")); // 'temp' lafz ka hissa — lekin "_temp" nahi
        assertFalse(BulkProjects.isCountableWallpaper(".hidden.jpg"));
        assertFalse(BulkProjects.isCountableWallpaper("clip.mp4"));
    }

    // ------------------------------------------------------------ (d) prompt profiles

    @Test
    public void profileFromScriptName() {
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileForScript("adnan_core.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileForScript("adnan_core_4.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileForScript("dual_core.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileForScript("project3_core.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileForScript("w17.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileForScript("w18.py"));
        assertEquals(BulkRunSettings.PromptProfile.SYSTEM,
            BulkRunSettings.profileForScript("system_core.py"));
        assertEquals(BulkRunSettings.PromptProfile.UNKNOWN,
            BulkRunSettings.profileForScript("kuch_or.py"));
        assertEquals(BulkRunSettings.PromptProfile.UNKNOWN,
            BulkRunSettings.profileForScript(null));
    }

    @Test
    public void profileForProjectNumbers() {
        // Bundled 1..14 hamesha ADNAN (verified)
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileFor(1, "adnan_core.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileFor(14, "w14.py"));
        // ZIP-added: script ke hisab se
        assertEquals(BulkRunSettings.PromptProfile.SYSTEM,
            BulkRunSettings.profileFor(16, "system_core.py"));
        assertEquals(BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.profileFor(18, "w18.py"));
        assertEquals(BulkRunSettings.PromptProfile.UNKNOWN,
            BulkRunSettings.profileFor(19, null));
    }

    @Test
    public void answersPerProfile() {
        // ADNAN: HD='1', Normal='2'; Crop='2', Original='1'
        assertEquals(Arrays.asList("1", "2"), BulkRunSettings.answersForProfile(
            BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP));
        assertEquals(Arrays.asList("2", "1"), BulkRunSettings.answersForProfile(
            BulkRunSettings.PromptProfile.ADNAN,
            BulkRunSettings.ANSWER_NORMAL, BulkRunSettings.ANSWER_ORIGINAL));
        // SYSTEM: Mode ulta — Normal='1', HD='2'; Size waisa hi
        assertEquals(Arrays.asList("2", "2"), BulkRunSettings.answersForProfile(
            BulkRunSettings.PromptProfile.SYSTEM,
            BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP));
        assertEquals(Arrays.asList("1", "1"), BulkRunSettings.answersForProfile(
            BulkRunSettings.PromptProfile.SYSTEM,
            BulkRunSettings.ANSWER_NORMAL, BulkRunSettings.ANSWER_ORIGINAL));
        // UNKNOWN: sirf Enter (script defaults)
        assertEquals(Arrays.asList("", ""), BulkRunSettings.answersForProfile(
            BulkRunSettings.PromptProfile.UNKNOWN,
            BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP));
    }

    @Test
    public void unknownProfilePipedCommandFeedsEnters() {
        String cmd = BulkRunSettings.buildPipedCommand(
            "cd /x && python kuch_or.py process 19",
            BulkRunSettings.answersForProfile(BulkRunSettings.PromptProfile.UNKNOWN,
                BulkRunSettings.ANSWER_HD, BulkRunSettings.ANSWER_CROP));
        assertTrue(cmd.startsWith("printf '%s\\n' '' ''"));
        assertTrue(cmd.contains("python kuch_or.py process 19"));
    }
}
