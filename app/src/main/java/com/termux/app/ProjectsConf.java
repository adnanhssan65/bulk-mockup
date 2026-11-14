package com.termux.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Bulk Mockup — projects.conf ka parser (pure Java, koi Android dependency
 * nahi, taake unit test ho sake).
 *
 * Format, ek project ek line me:
 *   Naam | work_cmd | batch_cmd | delete_cmd
 * Blank lines aur '#' se shuru hone wali lines ignore hoti hain. Ghalat
 * lines (columns kam hon, koi hissa khaali ho) chup chaap skip ho jati
 * hain — parser kabhi crash nahi karta, file kabhi overwrite nahi hoti.
 */
public final class ProjectsConf {

    private ProjectsConf() {}

    /** Ek project: naam + 3 commands. */
    public static final class Project {
        public final String name;
        public final String workCmd;
        public final String batchCmd;
        public final String deleteCmd;

        Project(String name, String workCmd, String batchCmd, String deleteCmd) {
            this.name = name;
            this.workCmd = workCmd;
            this.batchCmd = batchCmd;
            this.deleteCmd = deleteCmd;
        }
    }

    public static List<Project> parse(String content) {
        List<Project> result = new ArrayList<>();
        if (content == null) return result;
        for (String rawLine : content.split("\n", -1)) {
            Project p = parseLine(rawLine);
            if (p != null) result.add(p);
        }
        return result;
    }

    /** Ek line parse karo; ghalat/khaali line par null. */
    public static Project parseLine(String rawLine) {
        if (rawLine == null) return null;
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) return null;
        String[] parts = line.split("\\|", -1);
        if (parts.length < 4) return null;
        String name = parts[0].trim();
        String work = parts[1].trim();
        String batch = parts[2].trim();
        String delete = parts[3].trim();
        if (name.isEmpty() || work.isEmpty() || batch.isEmpty() || delete.isEmpty()) return null;
        return new Project(name, work, batch, delete);
    }

    /** Default file (Project 1..10) — sirf tab likhi jati hai jab file mojood na ho. */
    public static String defaultContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Bulk Mockup — projects list\n");
        sb.append("# Format: Naam | work command | batch command | delete command\n");
        sb.append("# In lines ko badal sakte ho; app har dafa file fresh parhti hai.\n");
        // Sirf wohi projects jinke asal shortcuts app me pehle se lage hain
        // (owner ki asal convention: w = process, b = batch, d = delete).
        // Baqi projects ki line owner khud jor sakta hai.
        sb.append("Project 1 | w1 | b1 | d1\n");
        sb.append("Project 2 | w2 | b2 | d2\n");
        sb.append("Project 8 | w8 | b8 | d8\n");
        return sb.toString();
    }
}
