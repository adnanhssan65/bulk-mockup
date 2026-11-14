package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** Bulk Mockup — projects.conf parser ke unit tests (Override G2.7). */
public class ProjectsConfTest {

    @Test
    public void normalLines() {
        List<ProjectsConf.Project> ps = ProjectsConf.parse(
            "Project 1 | w1 | p1 | d1\nProject 8 | w8 | b8 | d8\n");
        assertEquals(2, ps.size());
        assertEquals("Project 8", ps.get(1).name);
        assertEquals("b8", ps.get(1).batchCmd);
    }

    @Test
    public void blankAndCommentLinesSkipped() {
        List<ProjectsConf.Project> ps = ProjectsConf.parse(
            "\n# comment\n   \nProject 1 | w1 | p1 | d1\n#Project 2 | w2 | p2 | d2\n");
        assertEquals(1, ps.size());
    }

    @Test
    public void extraSpacesTrimmed() {
        ProjectsConf.Project p = ProjectsConf.parseLine("  Project 3   |   w3  |  p3 | d3  ");
        assertEquals("Project 3", p.name);
        assertEquals("w3", p.workCmd);
    }

    @Test
    public void missingColumnsSkipped() {
        assertNull(ProjectsConf.parseLine("Project 1 | w1"));
        assertNull(ProjectsConf.parseLine("Project 1 | w1 | p1"));
        assertNull(ProjectsConf.parseLine("Project 1"));
        assertEquals(0, ProjectsConf.parse("A | a\nB | b | c\n").size());
    }

    @Test
    public void emptyPartsSkipped() {
        assertNull(ProjectsConf.parseLine("Project 1 | | p1 | d1"));
        assertNull(ProjectsConf.parseLine(" | w1 | p1 | d1"));
    }

    @Test
    public void nullAndGarbageSafe() {
        assertEquals(0, ProjectsConf.parse(null).size());
        assertEquals(0, ProjectsConf.parse("").size());
        assertEquals(0, ProjectsConf.parse("||||\nrandom text\n").size());
    }

    @Test
    public void defaultContentHasTenProjects() {
        List<ProjectsConf.Project> ps = ProjectsConf.parse(ProjectsConf.defaultContent());
        assertEquals(10, ps.size());
        assertEquals("w10", ps.get(9).workCmd);
        assertTrue(ps.get(0).deleteCmd.equals("d1"));
    }
}
