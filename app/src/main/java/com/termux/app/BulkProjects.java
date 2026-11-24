package com.termux.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Termux Mod — projects ki shared logic (v124).
 *
 * Ye class zyada tar pure Java hai (Android dependency nahi) taake is ki
 * unit tests chal saken: project number pehchanna, main script pehchanna,
 * ZIP ke andar project folder pehchanna, alias / projects.conf lines
 * banana or pehle se mojood hain ya nahi check karna.
 *
 * Convention (owner ki asal, tamam projects ke liye aik jesi):
 *   folder:  $HOME/mockup_project_<N>  (v136: engine app ke andar
 *            chhupa hai — shared storage me sirf TermuxMod/Project N
 *            drop folders dikhte hain, mockup_project_<N> wahan
 *            kabhi nahi banta)
 *   chalana: python <script> process|del|batch <N>
 *   aliases: w<N> = process, b<N> = batch, d<N> = delete
 */
public final class BulkProjects {

    public static final String STORAGE_ROOT = "/storage/emulated/0";

    /**
     * v136: Termux HOME ka path (TermuxConstants.TERMUX_HOME_DIR_PATH
     * ke barabar — ye class pure rehne ke liye literal rakhti hai;
     * BulkV136Test dono ki barabari verify karta hai). Tamam project
     * ENGINE folders ab yahin bante hain: file manager me nazar nahi
     * ate, or shared storage par mockup_project_<N> kabhi nahi banta.
     */
    public static final String HOME_PATH = "/data/data/com.termux/files/home";

    private static final Pattern NAME_NUMBER = Pattern.compile("(?i)project\\s*[-_ ]?\\s*(\\d+)");
    private static final Pattern CMD_NUMBER = Pattern.compile("(?i)^\\s*w\\s*(\\d+)\\s*$");
    private static final Pattern FOLDER_NUMBER = Pattern.compile("(?i)^mockup_project_(\\d+)$");
    private static final Pattern BARE_ALIAS = Pattern.compile("^\\s*([wbd])(\\d+)\\s*$");

    private BulkProjects() {}

    /** Ek project ki mukammal maloomat jo dashboard / chooser me chahiye. */
    public static final class ProjectInfo {
        public final int number;          // -1 = number maloom nahi
        public final String name;         // dikhai dene wala naam, masalan "Project 8"
        public final String workCmd;      // null = chalane ki command nahi ban saki
        public final String batchCmd;
        public final String deleteCmd;
        public final boolean fromConf;

        ProjectInfo(int number, String name, String workCmd, String batchCmd, String deleteCmd, boolean fromConf) {
            this.number = number;
            this.name = name;
            this.workCmd = workCmd;
            this.batchCmd = batchCmd;
            this.deleteCmd = deleteCmd;
            this.fromConf = fromConf;
        }

        /** Is project ka asal folder (number ho to), warna null. */
        public File dir() {
            return number > 0 ? projectDir(number) : null;
        }
    }

    // ------------------------------------------------------------ numbers / names

    public static String projectDirName(int n) {
        return "mockup_project_" + n;
    }

    /** v133 wali jagah — TermuxMod folder ke andar (v136 migration ka SOURCE 1). */
    public static File newProjectDir(int n) {
        return new File(new File(STORAGE_ROOT, "TermuxMod"), projectDirName(n));
    }

    /** v134/v135 wali jagah — storage root par (v136 migration ka SOURCE 2). */
    public static File oldProjectDir(int n) {
        return new File(STORAGE_ROOT, projectDirName(n));
    }

    /** v136: ENGINE ki asal jagah — Termux HOME ke andar (chhupa hua). */
    public static File homeProjectDir(int n) {
        return new File(HOME_PATH, projectDirName(n));
    }

    /**
     * Location ka faisla (pure) — v133/v134 wala resolution, ab
     * SIRF ek legacy helper (tests ke liye mehfooz). v136 me
     * projectDir() is ko istemal NAHI karta: engine hamesha $HOME
     * me hota hai, purani jagahen sirf migration ke sources hain.
     */
    public static File pickProjectDir(File newDir, File oldDir,
                                      boolean newExists, boolean oldExists) {
        if (oldExists) return oldDir;
        if (newExists) return newDir;
        return oldDir;
    }

    /**
     * Project ka ASAL (engine) folder — v136: hamesha $HOME ke
     * andar. App ka har kaam (counts, run, batch, delete, staging
     * commit, Ready Assets, drop-fetch) isi ko istemal karta hai.
     * Shared storage par ye folder kabhi nahi banta.
     */
    public static File projectDir(int n) {
        return homeProjectDir(n);
    }

    public static File wallpapersDir(int n) {
        return new File(projectDir(n), n + "_wallpapers");
    }

    public static File mockupsDir(int n) {
        return new File(projectDir(n), n + "_Mockups");
    }

    public static File readyDir(int n) {
        return new File(projectDir(n), n + "_Ready_Wallpapers");
    }

    /** Naam image file wala hai ya nahi (preview grids ke liye). */
    public static boolean isImageFile(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
            || n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".bmp")
            || n.endsWith(".avif") || n.endsWith(".heic");
    }

    /**
     * v129 — COUNT ke liye wallpaper: image file jo hidden (dot se shuru)
     * na ho. Runner scripts khud bhi bilkul yehi usool rakhte hain
     * (masalan adnan_core.py: endswith image ext + not startswith '.'),
     * is liye app ka count aur script ka asal kaam ab ek hi hisaab hai.
     * Hidden files file manager me bhi nazar nahi atin — unhe ginne se
     * badge ka number user ko hamesha ghalat lagta tha (v128 ki shikayat).
     */
    public static boolean isCountableWallpaper(String name) {
        if (name == null || name.startsWith(".")) return false;
        // v137: runner scripts "_temp" wali files ko process nahi
        // karte (un ki apni filter me "_temp" not in f shart hai) —
        // app ka count bhi unhe na gine, warna khali project bhi
        // Run list me "1 Ready" ke sath ajata tha (owner ki report).
        if (name.contains("_temp")) return false;
        return isImageFile(name);
    }

    /**
     * v128: video kabhi wallpaper nahi. Folder scans, counts aur staging
     * intake — har jagah video khamoshi se bahar. (MIME wala check
     * staging intake me is ke sath mil kar lagta hai.)
     */
    public static boolean isVideoFile(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".mov")
            || n.endsWith(".avi") || n.endsWith(".3gp") || n.endsWith(".webm")
            || n.endsWith(".wmv") || n.endsWith(".flv") || n.endsWith(".m4v")
            || n.endsWith(".ts") || n.endsWith(".mpg") || n.endsWith(".mpeg");
    }

    /** Project install par banne wale subfolders (installer inhi ko banata hai). */
    public static List<String> subfolderNames(int n) {
        List<String> subs = new ArrayList<>();
        subs.add(n + "_wallpapers");
        subs.add(n + "_Mockups");
        subs.add(n + "_Ready_Wallpapers");
        subs.add("Ready_Upload_Batches");
        return subs;
    }

    /** "mockup_project_15" se 15; na mile to -1. */
    public static int parseFolderNumber(String folderName) {
        if (folderName == null) return -1;
        Matcher m = FOLDER_NUMBER.matcher(folderName.trim());
        if (!m.matches()) return -1;
        try {
            return Integer.parseInt(m.group(1));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Project ka number pehchano: pehle naam se ("Project 8"), phir work
     * command se ("w8"). Na mile to -1.
     */
    public static int parseProjectNumber(String name, String workCmd) {
        if (name != null) {
            Matcher m = NAME_NUMBER.matcher(name);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (Exception ignored) {}
            }
        }
        if (workCmd != null) {
            Matcher m = CMD_NUMBER.matcher(workCmd);
            if (m.matches()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    // ------------------------------------------------------------ commands

    /**
     * Poori command line (alias par bharosa kiye baghair):
     * cd (resolved dir) + MOCKUP_BASE=<resolved dir> + python.
     * v133: MOCKUP_BASE har app-issued command me jata hai taake
     * env-aware runners hamesha asal folder par kaam karen — project
     * nayi jagah ho ya purani.
     */
    public static String fullCommand(int n, String script, String action) {
        String dir = projectDir(n).getAbsolutePath();
        return "cd " + dir + " && MOCKUP_BASE=" + dir
            + " python " + script + " " + action + " " + n;
    }

    /** Alias ka harf → script ka action: w = process, b = batch, d = del. */
    public static String actionForAliasLetter(char letter) {
        switch (Character.toLowerCase(letter)) {
            case 'w': return "process";
            case 'b': return "batch";
            case 'd': return "del";
            default: return null;
        }
    }

    /**
     * v129 — DEFECT 1 ka asal fix. Command agar BARE ALIAS hai ("w1",
     * "b12", "d3" — jesa projects.conf me likha hota hai) to use us
     * project ki asal script se POORI command me badal do
     * (cd ... && python <script> <action> <N>). Wajah: app ke background
     * run (bash -c) me .bashrc load nahi hota, is liye alias wahan
     * "command not found" deta tha (v128 par asal fail). Poori command
     * kisi bhi shell me chalti hai — alias ho ya na ho.
     * Jo command pehle se poori hai (ya koi aur shakal hai) wo waisi ki
     * waisi rehti hai. Script maloom na ho (diskScripts me na mile) to
     * bhi command nahi badalti — ghalat script invent nahi karni.
     */
    public static String resolveCommand(String cmd, Map<Integer, String> diskScripts) {
        if (cmd == null || diskScripts == null) return cmd;
        Matcher m = BARE_ALIAS.matcher(cmd);
        if (!m.matches()) return cmd;
        int n;
        try {
            n = Integer.parseInt(m.group(2));
        } catch (Exception e) {
            return cmd;
        }
        String action = actionForAliasLetter(m.group(1).charAt(0));
        String script = diskScripts.get(n);
        if (action == null || script == null) return cmd;
        return fullCommand(n, script, action);
    }

    /** .bashrc ke liye ek alias line, masalan: alias w15='cd ... && python w15.py process 15' */
    public static String aliasLine(String aliasName, int n, String script, String action) {
        return "alias " + aliasName + "='" + fullCommand(n, script, action) + "'";
    }

    /** Teenon alias lines: w<N> (process), d<N> (del), b<N> (batch) — isi order me. */
    public static List<String> aliasLines(int n, String script) {
        List<String> lines = new ArrayList<>();
        lines.add(aliasLine("w" + n, n, script, "process"));
        lines.add(aliasLine("d" + n, n, script, "del"));
        lines.add(aliasLine("b" + n, n, script, "batch"));
        return lines;
    }

    /** .bashrc me ye alias pehle se defined hai? ("w1" ko "w15" se confuse nahi karta.) */
    public static boolean aliasAlreadyDefined(String bashrcContent, String aliasName) {
        if (bashrcContent == null || aliasName == null) return false;
        Pattern p = Pattern.compile("(?m)^\\s*alias\\s+" + Pattern.quote(aliasName) + "\\s*=");
        return p.matcher(bashrcContent).find();
    }

    // ------------------------------------------------------------ projects.conf

    /** projects.conf ki line: "Project <N> | w<N> | b<N> | d<N>" */
    public static String confLine(int n) {
        return "Project " + n + " | w" + n + " | b" + n + " | d" + n;
    }

    /** projects.conf me Project <N> ki line pehle se hai? (kharab lines bhi gin lo.) */
    public static boolean confHasProject(String confContent, int n) {
        if (confContent == null) return false;
        String wanted = "Project " + n;
        for (String rawLine : confContent.split("\n", -1)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String firstCell = line.split("\\|", -1)[0].trim();
            if (firstCell.equals(wanted)) return true;
            ProjectsConf.Project p = ProjectsConf.parseLine(line);
            if (p != null && parseProjectNumber(p.name, p.workCmd) == n) return true;
        }
        return false;
    }

    // ------------------------------------------------------------ script detection

    /**
     * Project ki main script pehchano. Tarjeeh (priority) isi order me:
     * w<N>.py, adnan_core.py, dual_core.py, project3_core.py, adnan_core_4.py,
     * warna root me akeli .py file (make_assets.py ko chor kar).
     * Na mile ya do se zyada umeedwar hon to null.
     */
    public static String detectMainScript(List<String> rootFileNames, int n) {
        if (rootFileNames == null) return null;
        List<String> names = new ArrayList<>();
        for (String f : rootFileNames) {
            if (f != null) names.add(f.trim());
        }
        String[] priority = {"w" + n + ".py", "adnan_core.py", "dual_core.py", "project3_core.py", "adnan_core_4.py"};
        for (String candidate : priority) {
            if (names.contains(candidate)) return candidate;
        }
        List<String> py = new ArrayList<>();
        for (String f : names) {
            if (f.toLowerCase().endsWith(".py") && !f.equals("make_assets.py")) py.add(f);
        }
        return py.size() == 1 ? py.get(0) : null;
    }

    /**
     * v136: install ke waqt muntakhib runner script project folder
     * ke andar is marker file me hamesha ke liye mehfooz (persist)
     * rehti hai — Run/Batch/Delete har dafa wahi script istemal
     * karte hain, folder me baad me or .py ajaye to bhi faisla
     * nahi badalta.
     */
    public static final String MAIN_SCRIPT_MARKER = ".main_script";

    /**
     * Script ka faisla (pure): persisted choice pehle — basharti
     * ke wo ab bhi folder ki files me mojood ho; warna fresh
     * detection (detectMainScript).
     */
    public static String chooseScript(String persisted, List<String> rootFileNames, int n) {
        if (persisted != null && rootFileNames != null) {
            String p = persisted.trim();
            if (!p.isEmpty() && p.toLowerCase().endsWith(".py")) {
                for (String f : rootFileNames) {
                    if (f != null && f.trim().equals(p)) return p;
                }
            }
        }
        return detectMainScript(rootFileNames, n);
    }

    /** Marker file ka content parho (na ho / kharab ho to null). */
    public static String readPersistedScript(File projectDir) {
        if (projectDir == null) return null;
        File marker = new File(projectDir, MAIN_SCRIPT_MARKER);
        if (!marker.isFile()) return null;
        try {
            byte[] data = new byte[(int) Math.min(marker.length(), 256)];
            try (java.io.FileInputStream in = new java.io.FileInputStream(marker)) {
                int r = in.read(data);
                if (r <= 0) return null;
                return new String(data, 0, r, "UTF-8").trim();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Ek project ki asal script: persisted pehle, warna detection. */
    public static String resolveScript(int n) {
        File dir = projectDir(n);
        return chooseScript(readPersistedScript(dir), listFileNames(dir), n);
    }

    /**
     * ZIP file ke naam se project number (pure) — fallback jab ZIP
     * ke andar folder na ho lekin file ka naam hi
     * mockup_project_<N> (.zip ke sath ya bagher) ho.
     */
    public static int parseZipBaseNumber(String zipFileName) {
        if (zipFileName == null) return -1;
        String name = zipFileName.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) name = name.substring(slash + 1);
        if (name.toLowerCase().endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        return parseFolderNumber(name);
    }

    // ------------------------------------------------------------ ZIP root detection

    /**
     * ZIP ke entries me project folder pehchano. Sirf tab match hota hai jab
     * kisi entry ka PEHLA hissa "mockup_project_<N>" ho (yani folder ZIP ki
     * root par ho). Files seedha root par hon to null — caller wahan saaf
     * error dikhata hai.
     *
     * @return folder ka naam, masalan "mockup_project_15"; na mile to null.
     */
    public static String findProjectRoot(List<String> entryNames) {
        if (entryNames == null) return null;
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String entry : entryNames) {
            if (entry == null) continue;
            String e = entry.replace('\\', '/');
            int slash = e.indexOf('/');
            if (slash <= 0) continue; // root par pari file — folder nahi
            String first = e.substring(0, slash);
            if (first.equals("__MACOSX")) continue;
            if (parseFolderNumber(first) > 0) {
                Integer c = counts.get(first);
                counts.put(first, c == null ? 1 : c + 1);
            }
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> en : counts.entrySet()) {
            if (en.getValue() > bestCount) {
                bestCount = en.getValue();
                best = en.getKey();
            }
        }
        return best;
    }

    // ------------------------------------------------------------ merge (conf + disk)

    /**
     * v137: list ko project NUMBER ke hisab se ascending sort karo
     * (owner ka rule: har list 1, 2, 3, ... tartib me ho — conf ki
     * jumbled tartib kabhi na dikhe). Number <= 0 (maloom nahi)
     * wale aakhir me, apni asal tartib me (sort stable hai).
     */
    public static List<ProjectInfo> sortByNumber(List<ProjectInfo> projects) {
        List<ProjectInfo> out = new ArrayList<>();
        if (projects != null) {
            for (ProjectInfo p : projects) {
                if (p != null) out.add(p);
            }
        }
        Collections.sort(out, (x, y) -> {
            int kx = x.number <= 0 ? Integer.MAX_VALUE : x.number;
            int ky = y.number <= 0 ? Integer.MAX_VALUE : y.number;
            return Integer.compare(kx, ky);
        });
        return out;
    }

    /**
     * projects.conf wale projects + disk par mile extra projects mila kar
     * aik list banao. Yehi EK enumeration tamam lists istemal karti hain
     * (Select Project, Projects screen, Run popup, Preview, Ready
     * Assets) — is liye ZIP se add hua project (conf line + $HOME
     * engine folder dono installer likhta hai) har jagah foran nazar
     * ata hai. Duplicate number ki pehli entry jeet ti hai.
     * v137: aakhri list hamesha number ke hisab se ascending hoti
     * hai ({@link #sortByNumber}). Disk-only project ki commands us
     * ki detected script se poori banai jati hain (alias par bharosa
     * nahi); script na mile to us ki commands null hongi.
     *
     * @param diskScripts disk par mile har number ke liye detected script (null ho sakti hai)
     */
    public static List<ProjectInfo> mergeProjects(List<ProjectsConf.Project> confProjects,
                                                  Map<Integer, String> diskScripts) {
        List<ProjectInfo> result = new ArrayList<>();
        TreeSet<Integer> covered = new TreeSet<>();
        if (confProjects != null) {
            for (ProjectsConf.Project p : confProjects) {
                int n = parseProjectNumber(p.name, p.workCmd);
                if (n > 0 && !covered.add(n)) continue; // duplicate line — pehli jeet ti hai
                // v129: conf ki bare aliases (w<N>/b<N>/d<N>) yahin poori
                // commands me badal jati hain — koi bhi run rasta (popup
                // background run, terminal, Batch/Delete) ab alias par
                // depend nahi karta.
                result.add(new ProjectInfo(n, p.name,
                    resolveCommand(p.workCmd, diskScripts),
                    resolveCommand(p.batchCmd, diskScripts),
                    resolveCommand(p.deleteCmd, diskScripts), true));
            }
        }
        if (diskScripts != null) {
            List<Integer> numbers = new ArrayList<>(diskScripts.keySet());
            Collections.sort(numbers);
            for (int n : numbers) {
                if (covered.contains(n)) continue;
                String script = diskScripts.get(n);
                String work = null, batch = null, delete = null;
                if (script != null) {
                    work = fullCommand(n, script, "process");
                    batch = fullCommand(n, script, "batch");
                    delete = fullCommand(n, script, "del");
                }
                result.add(new ProjectInfo(n, "Project " + n, work, batch, delete, false));
            }
        }
        return sortByNumber(result);
    }

    // ------------------------------------------------------------ run filter (v127)

    /**
     * Run ke liye project tab hi eligible hai jab us ke wallpapers
     * folder me kam az kam 1 wallpaper ho (owner ka rule): 0 wallpapers
     * wale project Run list me nahi ate or un ka Run action nahi dikhta.
     * Batch / Delete par is ka koi asar nahi.
     */
    public static boolean canRun(int wallpapersCount) {
        return wallpapersCount > 0;
    }

    /**
     * Sirf wohi projects jo Run ke eligible hain (un ke number ke
     * wallpapers count {@link #canRun(int)} pass kare). Number <= 0 ya
     * count maloom na ho (map me na ho) to project bahar rehta hai.
     */
    public static List<ProjectInfo> filterRunnable(List<ProjectInfo> projects,
                                                   Map<Integer, Integer> wallpapersByNumber) {
        List<ProjectInfo> out = new ArrayList<>();
        if (projects == null) return out;
        for (ProjectInfo p : projects) {
            if (p == null || p.number <= 0 || wallpapersByNumber == null) continue;
            Integer w = wallpapersByNumber.get(p.number);
            if (w != null && canRun(w)) out.add(p);
        }
        return out;
    }

    // ------------------------------------------------------------ disk IO helpers

    /**
     * ENGINE home ($HOME) me jitne mockup_project_<N> folders hain,
     * un ke numbers + scripts. v136: disk scan ka matlab ab sirf
     * $HOME hai — shared storage kabhi scan nahi hoti (wahan ke
     * folders sirf migration ke sources hain, neeche dekho).
     */
    public static Map<Integer, String> scanDiskProjects() {
        Map<Integer, String> map = new LinkedHashMap<>();
        List<Integer> numbers = new ArrayList<>();
        File[] kids = new File(HOME_PATH).listFiles();
        if (kids != null) {
            for (File k : kids) {
                if (!k.isDirectory()) continue;
                int n = parseFolderNumber(k.getName());
                if (n > 0 && !numbers.contains(n)) numbers.add(n);
            }
        }
        Collections.sort(numbers);
        for (int n : numbers) {
            File dir = projectDir(n);
            map.put(n, chooseScript(readPersistedScript(dir), listFileNames(dir), n));
        }
        return map;
    }

    /**
     * v136 migration ke liye: SHARED storage par bache hue purane
     * project folders (root + TermuxMod ke andar) ke numbers +
     * scripts. Script migration-source ki tarjeeh se parhi jati
     * hai: TermuxMod wali copy pehle, warna root wali. Ye scan
     * SIRF migration istemal karti hai — aam app flow kabhi nahi.
     */
    public static Map<Integer, String> scanSharedProjects() {
        Map<Integer, String> map = new LinkedHashMap<>();
        List<Integer> numbers = new ArrayList<>();
        File[] roots = {new File(STORAGE_ROOT), new File(STORAGE_ROOT, "TermuxMod")};
        for (File root : roots) {
            File[] kids = root.listFiles();
            if (kids == null) continue;
            for (File k : kids) {
                if (!k.isDirectory()) continue;
                int n = parseFolderNumber(k.getName());
                if (n > 0 && !numbers.contains(n)) numbers.add(n);
            }
        }
        Collections.sort(numbers);
        for (int n : numbers) {
            File src = newProjectDir(n).isDirectory() ? newProjectDir(n) : oldProjectDir(n);
            map.put(n, detectMainScript(listFileNames(src), n));
        }
        return map;
    }

    /** Ek folder ke andar ki files ke naam (sirf files, folders nahi). */
    public static List<String> listFileNames(File dir) {
        List<String> names = new ArrayList<>();
        if (dir == null) return names;
        File[] kids = dir.listFiles();
        if (kids == null) return names;
        for (File k : kids) {
            if (k.isFile()) names.add(k.getName());
        }
        return names;
    }

    /**
     * Ek folder me kitne WALLPAPERS hain (dashboard ki counts line,
     * run filter waghera ke liye). SAKHT USOOL (v129): sirf IMAGE files
     * jo seedha is folder ke ANDAR hon — recursion bilkul nahi,
     * subfolders (masalan backgrounds/) ke andar ki files kabhi nahi,
     * video kabhi nahi, hidden (dot) files nahi (script bhi unhe
     * nahi chalata). Har screen/popup khulne par ye FRESH ginna hai —
     * count kahin cache nahi hota.
     */
    public static int countFiles(File dir) {
        if (dir == null) return 0;
        File[] kids = dir.listFiles();
        if (kids == null) return 0;
        int count = 0;
        for (File k : kids) {
            if (k.isFile() && isCountableWallpaper(k.getName())) count++;
        }
        return count;
    }

    /** Counts wali line: "Wallpapers: 3 · Mockups: 1 · Ready: 2" */
    public static String countsLine(int wallpapers, int mockups, int ready) {
        return "Wallpapers: " + wallpapers + " · Mockups: " + mockups + " · Ready: " + ready;
    }

    /** Ek project ke teenon counts us ke asal folder se parho. */
    public static String countsLineFor(int n) {
        File dir = projectDir(n);
        int w = countFiles(new File(dir, n + "_wallpapers"));
        int m = countFiles(new File(dir, n + "_Mockups"));
        int r = countFiles(new File(dir, n + "_Ready_Wallpapers"));
        return countsLine(w, m, r);
    }
}
