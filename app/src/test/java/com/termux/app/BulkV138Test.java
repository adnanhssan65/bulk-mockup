package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Termux Mod v138 — owner ki v137 phone-test reports ke fixes:
 *  (A) koi bhi app-run shell alias par depend nahi karta: command
 *      run ke waqt number + asal script se banti hai
 *      (BulkProjects.effectiveCommand) — snapshot me bare alias
 *      ("w1") reh jaye to bhi execute wahi nahi hoti. fullCommand
 *      me MOCKUP_BASE hamesha engine folder hota hai.
 *  (B) drop folders ki one-time provision: record na ho to
 *      folder banta hai + record lagta hai; record ho to dobara
 *      kabhi nahi (owner ka delete kiya folder waisa hi rehta).
 */
public class BulkV138Test {

    // ------------------------------------------------------------ (A) effective command

    @Test
    public void effectiveCommandBuildsRealCommandFromScript() {
        String cmd = BulkProjects.effectiveCommand(1, "adnan_core.py", "w1", "process");
        assertEquals(BulkProjects.fullCommand(1, "adnan_core.py", "process"), cmd);
        assertTrue(cmd.contains("python adnan_core.py process 1"));
        assertTrue(cmd.contains("cd /data/data/com.termux/files/home/mockup_project_1"));
        assertFalse(BulkProjects.isBareAlias(cmd));
    }

    @Test
    public void effectiveCommandSetsMockupBaseToEngineDir() {
        String cmd = BulkProjects.effectiveCommand(17, "w17.py", "w17", "process");
        assertTrue(cmd.contains(
            "MOCKUP_BASE=/data/data/com.termux/files/home/mockup_project_17"));
        assertTrue(cmd.contains("python w17.py process 17"));
    }

    @Test
    public void effectiveCommandCoversBatchAndDelete() {
        String batch = BulkProjects.effectiveCommand(17, "w17.py", "b17", "batch");
        assertTrue(batch.contains("python w17.py batch 17"));
        assertFalse(BulkProjects.isBareAlias(batch));
        String del = BulkProjects.effectiveCommand(3, "adnan_core.py", "d3", "del");
        assertTrue(del.contains("python adnan_core.py del 3"));
        assertFalse(BulkProjects.isBareAlias(del));
    }

    @Test
    public void effectiveCommandFallsBackToSnapshotWithoutScript() {
        // Script maloom na ho to snapshot hi rehti hai — ghalat
        // script invent nahi hoti (ye sirf wahi surat hai jab
        // project ki koi script disk par hai hi nahi).
        assertEquals("w1", BulkProjects.effectiveCommand(1, null, "w1", "process"));
        assertEquals("w1", BulkProjects.effectiveCommand(0, "adnan_core.py", "w1", "process"));
        assertEquals("w1", BulkProjects.effectiveCommand(-2, "adnan_core.py", "w1", "process"));
    }

    @Test
    public void snapshotAliasSurvivesMergeButRunTimeCommandDoesNot() {
        // List snapshot: disk scan miss ho to merge bare alias
        // chhod deta hai (purana rawaj) — lekin run ke waqt ki
        // command (script maloom hote hi) hamesha poori hoti hai.
        List<ProjectsConf.Project> conf = ProjectsConf.parse("Project 1 | w1 | b1 | d1\n");
        List<BulkProjects.ProjectInfo> merged =
            BulkProjects.mergeProjects(conf, new HashMap<>());
        assertEquals(1, merged.size());
        assertEquals("w1", merged.get(0).workCmd); // snapshot kamzori mehfooz (documented)
        String runCmd = BulkProjects.effectiveCommand(
            merged.get(0).number, "adnan_core.py", merged.get(0).workCmd, "process");
        assertFalse(BulkProjects.isBareAlias(runCmd));
        assertTrue(runCmd.contains("python adnan_core.py process 1"));
    }

    @Test
    public void mergeResolvesAliasWhenScriptKnown() {
        List<ProjectsConf.Project> conf = ProjectsConf.parse("Project 1 | w1 | b1 | d1\n");
        Map<Integer, String> disk = new HashMap<>();
        disk.put(1, "adnan_core.py");
        List<BulkProjects.ProjectInfo> merged = BulkProjects.mergeProjects(conf, disk);
        assertEquals(1, merged.size());
        assertTrue(merged.get(0).workCmd.contains("python adnan_core.py process 1"));
        assertFalse(BulkProjects.isBareAlias(merged.get(0).workCmd));
    }

    @Test
    public void bareAliasDetection() {
        assertTrue(BulkProjects.isBareAlias("w1"));
        assertTrue(BulkProjects.isBareAlias("b12"));
        assertTrue(BulkProjects.isBareAlias(" d3 "));
        assertFalse(BulkProjects.isBareAlias("cd /x && python a.py process 1"));
        assertFalse(BulkProjects.isBareAlias("python a.py process 1"));
        assertFalse(BulkProjects.isBareAlias(null));
        assertFalse(BulkProjects.isBareAlias(""));
        assertFalse(BulkProjects.isBareAlias("w"));
    }

    @Test
    public void fullCommandShape() {
        String cmd = BulkProjects.fullCommand(5, "w5.py", "process");
        assertEquals("cd /data/data/com.termux/files/home/mockup_project_5"
            + " && MOCKUP_BASE=/data/data/com.termux/files/home/mockup_project_5"
            + " python w5.py process 5", cmd);
    }

    // ------------------------------------------------------------ (B) one-time provision

    @Test
    public void provisionOnlyWhenNoRecord() {
        // Record na ho -> provision hoti hai (pehli dafa)
        assertTrue(BulkDropFolders.shouldProvision(false));
        // Record lag jaye -> dobara kabhi nahi (owner delete kare
        // to folder waisa hi rehta hai)
        assertFalse(BulkDropFolders.shouldProvision(true));
    }

    @Test
    public void provisionRecordKeyIsPerProject() {
        // Record per-project hai: ek project ka record doosre ko
        // provision se nahi rokta (15/16/17 alag alag provision
        // hote hain) — faisla har number ke apne record par hai.
        Map<Integer, Boolean> records = new HashMap<>();
        records.put(15, true);
        assertFalse(BulkDropFolders.shouldProvision(records.getOrDefault(15, false)));
        assertTrue(BulkDropFolders.shouldProvision(records.getOrDefault(16, false)));
        assertTrue(BulkDropFolders.shouldProvision(records.getOrDefault(17, false)));
        assertTrue(records.size() == 1 && new ArrayList<>(records.keySet()).get(0) == 15);
    }

    // ------------------------------------------------------------ (C) Remove Project

    @Test
    public void removeConfLinesDropsOnlyTargetProject() {
        String conf = "# Termux Mod — projects list\n"
            + "Project 1 | w1 | b1 | d1\n"
            + "Project 15 | w15 | b15 | d15\n"
            + "Project 3 | w3 | b3 | d3\n";
        String out = BulkProjects.removeConfLines(conf, 1);
        assertFalse(BulkProjects.confHasProject(out, 1));
        assertTrue(BulkProjects.confHasProject(out, 15));
        assertTrue(BulkProjects.confHasProject(out, 3));
        // comment line mehfooz rehti hai
        assertTrue(out.contains("# Termux Mod"));
    }

    @Test
    public void removeConfLinesDropsDuplicatesAndCmdMatchedLines() {
        String conf = "Project 1 | w1 | b1 | d1\n"
            + "Project 1 | w1 | b1 | d1\n"
            + "Custom Naam | w7 | b7 | d7\n";
        String out1 = BulkProjects.removeConfLines(conf, 1);
        assertFalse(BulkProjects.confHasProject(out1, 1));
        assertTrue(BulkProjects.confHasProject(out1, 7));
        // number work-command se match hone wali line bhi usi project ki hai
        String out7 = BulkProjects.removeConfLines(conf, 7);
        assertFalse(BulkProjects.confHasProject(out7, 7));
        assertTrue(BulkProjects.confHasProject(out7, 1));
    }

    @Test
    public void removeAliasLinesDropsOnlyTargetAliases() {
        String bashrc = "export PATH=$PATH:~/bin\n"
            + "alias w1='cd /x && python adnan_core.py process 1'\n"
            + "alias d1='cd /x && python adnan_core.py del 1'\n"
            + "alias b1='cd /x && python adnan_core.py batch 1'\n"
            + "alias w15='cd /y && python w15.py process 15'\n"
            + "alias ll='ls -la'\n";
        String out = BulkProjects.removeAliasLines(bashrc, 1);
        assertFalse(BulkProjects.aliasAlreadyDefined(out, "w1"));
        assertFalse(BulkProjects.aliasAlreadyDefined(out, "d1"));
        assertFalse(BulkProjects.aliasAlreadyDefined(out, "b1"));
        // w15 + ghair-project content mehfooz
        assertTrue(BulkProjects.aliasAlreadyDefined(out, "w15"));
        assertTrue(out.contains("alias ll='ls -la'"));
        assertTrue(out.contains("export PATH=$PATH:~/bin"));
    }

    @Test
    public void removeAliasLinesDoesNotTouchLongerNumbers() {
        String bashrc = "alias w15='a'\nalias d15='b'\nalias b15='c'\n";
        // 1 hatane se 15 ki lines bilkul waisi ki waisi
        assertEquals(bashrc, BulkProjects.removeAliasLines(bashrc, 1));
        String out15 = BulkProjects.removeAliasLines(bashrc, 15);
        assertFalse(BulkProjects.aliasAlreadyDefined(out15, "w15"));
        assertFalse(BulkProjects.aliasAlreadyDefined(out15, "d15"));
        assertFalse(BulkProjects.aliasAlreadyDefined(out15, "b15"));
    }

    @Test
    public void removePickerContentsAreNumericSorted() {
        // Picker mergeProjects ki sorted list se banta hai —
        // jumbled conf order + disk-only projects bhi number
        // ke hisab se ascending ate hain.
        List<ProjectsConf.Project> conf = ProjectsConf.parse(
            "Project 8 | w8 | b8 | d8\n"
            + "Project 1 | w1 | b1 | d1\n"
            + "Project 10 | w10 | b10 | d10\n");
        Map<Integer, String> disk = new HashMap<>();
        disk.put(3, "adnan_core.py");
        disk.put(18, "w18.py");
        List<BulkProjects.ProjectInfo> merged = BulkProjects.mergeProjects(conf, disk);
        List<Integer> numbers = new ArrayList<>();
        for (BulkProjects.ProjectInfo p : merged) numbers.add(p.number);
        assertEquals(java.util.Arrays.asList(1, 3, 8, 10, 18), numbers);
    }

    @Test
    public void removeProjectStringsExistInBothLanguages() {
        for (String key : new String[]{BulkStrings.REMOVE_PROJECT,
                BulkStrings.REMOVE_PROJECT_CONFIRM_TITLE,
                BulkStrings.REMOVE_PROJECT_CONFIRM_MSG,
                BulkStrings.REMOVE_BTN, BulkStrings.REMOVED_TOAST,
                BulkStrings.REMOVE_FAILED}) {
            assertTrue("dono zubanein lazmi: " + key, BulkStrings.hasBothLanguages(key));
        }
        assertEquals("Remove Project", BulkStrings.get(BulkStrings.REMOVE_PROJECT, false));
        assertEquals("Project Remove Karo", BulkStrings.get(BulkStrings.REMOVE_PROJECT, true));
    }
}
