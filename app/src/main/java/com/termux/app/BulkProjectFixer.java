package com.termux.app;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Termux Mod v145 — New Project Check: ZIP se add hue project ke
 * runner ki mashhoor kharabiyan import ke waqt khud fix karne wala
 * system. Transformations mechanical + pattern-based hain; har
 * item sirf tab lagu hota hai jab us ka pattern mile:
 *
 *   1. BASE PATH — hardcoded /storage/emulated/0/mockup_project_<N>
 *      wala assignment → MOCKUP_BASE env / script-dir wali family form
 *   2. HISTORY — private tracker (.mockup_time_tracker_p<N>.txt /
 *      .mockup_global_time.txt) → shared .mockup_time_tracker.txt
 *   3. LOADING DELAYS — animated_spinner / display_header ke andar
 *      ke decorative time.sleep(<1s) → time.sleep(0); bahar ke
 *      sleeps (engine poll, retry backoff) ko hath NAHI lagta
 *   4. WAKELOCK — set_wakelock ka os.system wala body (ya inline
 *      os.system calls) → subprocess.Popen fire-and-forget
 *   5. SCAN TIMEOUT — safe_scan/scan_media ka default timeout >1
 *      → 1; termux-media-scan calls par literal timeout=5 → 1;
 *      bina-timeout wali simple subprocess.run scan call par
 *      timeout=1 judta hai (sirf jab call saaf tor par band ho)
 *   6. GALLERY SEQUENCE — sirf family pair pattern par: helper
 *      mojood na ho or process_one me safe_scan(mock_path) +
 *      safe_scan(wall_path) ki adjacent jori mile to P1 wala
 *      proven gallery_pair_copy helper + call inject hota hai.
 *      Pattern na mile to inject NAHI hota — report me saaf
 *      likha jata hai ke ye project Muse ko bhejna hai.
 *
 * Ye class pure Java hai (Android deps nahi) taake JVM tests me
 * chal sake. Koi bhi fail ho to asal runner mehfooz rehta hai —
 * file sirf tab rewrite hoti hai jab kam az kam ek fix laga ho,
 * or us se pehle <script>.orig backup banta hai (ek dafa).
 */
public final class BulkProjectFixer {

    private BulkProjectFixer() {}

    public enum Status { FIXED, ALREADY_OK, NOT_APPLIED }

    public static final class Report {
        public Status basePath = Status.NOT_APPLIED;
        public Status history = Status.NOT_APPLIED;
        public Status delays = Status.NOT_APPLIED;
        public Status wakelock = Status.NOT_APPLIED;
        public Status scanTimeout = Status.NOT_APPLIED;
        public Status gallery = Status.NOT_APPLIED;
        /** Gallery helper nahi tha or pattern bhi nahi mila — owner ko Muse ke paas bhejna hai. */
        public boolean galleryNeedsClaude;
        /** Fixer khud fail ho gaya — asal runner waisa ka waisa raha. */
        public boolean failed;
        /** File asal me rewrite hui (kam az kam ek fix laga). */
        public boolean changed;
    }

    public static final class FixResult {
        public final String text;
        public final Report report;

        FixResult(String text, Report report) {
            this.text = text;
            this.report = report;
        }
    }

    // ------------------------------------------------------------ patterns

    /** Assignment line jis ka RHS sirf ek string literal ho jis me /storage/emulated/0/mockup_project_... ho (akhir me subfolder na ho). */
    private static final Pattern BASE_ASSIGN = Pattern.compile(
        "^(\\s*)([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*f?\"/storage/emulated/0/mockup_project_[^\"/]*\"\\s*(#.*)?$");

    private static final Pattern PRIVATE_TRACKER = Pattern.compile(
        "\\.mockup_time_tracker_p\\d+\\.txt|\\.mockup_global_time\\.txt");

    private static final Pattern SLEEP_CALL = Pattern.compile(
        "time\\.sleep\\(\\s*([0-9]*\\.?[0-9]+)\\s*\\)");

    private static final Pattern WAKE_DEF = Pattern.compile(
        "^\\s*def\\s+set_wakelock\\s*\\(([^)]*)\\)");

    private static final Pattern WAKE_INLINE = Pattern.compile(
        "^(\\s*)os\\.system\\(\\s*\"(termux-wake-lock|termux-wake-unlock)[^\"]*\"\\s*\\)\\s*$"
            + "|^(\\s*)os\\.system\\(\\s*'(termux-wake-lock|termux-wake-unlock)[^']*'\\s*\\)\\s*$");

    private static final Pattern SCAN_DEF_TIMEOUT = Pattern.compile(
        "(def\\s+(?:safe_scan|scan_media)\\s*\\([^)]*?timeout_sec\\s*=\\s*)(\\d+)");

    private static final Pattern TIMEOUT_LITERAL = Pattern.compile(
        "timeout\\s*=\\s*(\\d+)");

    private static final Pattern SCAN_LINE_MOCK = Pattern.compile(
        "^(\\s*)safe_scan\\(mock_path\\)\\s*$");
    private static final Pattern SCAN_LINE_WALL = Pattern.compile(
        "^\\s*safe_scan\\(wall_path\\)\\s*$");

    private static final Pattern MT_WT_ASSIGN = Pattern.compile(
        "\\bmt\\s*,\\s*wt\\s*=");
    private static final Pattern MT_ASSIGN = Pattern.compile("\\bmt\\s*=");
    private static final Pattern WT_ASSIGN = Pattern.compile("\\bwt\\s*=");

    // ------------------------------------------------------------ entry points

    /** Pure text transform — tests yahi istemal karte hain. Kabhi throw nahi karta. */
    public static FixResult fixText(String source, int projectNumber) {
        Report report = new Report();
        if (source == null) {
            report.failed = true;
            return new FixResult(null, report);
        }
        boolean crlf = source.contains("\r\n");
        String work = crlf ? source.replace("\r\n", "\n") : source;
        try {
            List<String> lines = new ArrayList<>();
            for (String l : work.split("\n", -1)) lines.add(l);

            boolean[] needSubprocessImport = {false};

            fixHistory(lines, report);
            fixBasePath(lines, report);
            fixDelays(lines, report);
            fixWakelock(lines, report, needSubprocessImport);
            fixScanTimeout(lines, report, needSubprocessImport);
            fixGallery(lines, report, projectNumber);
            if (needSubprocessImport[0]) ensureSubprocessImport(lines);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append('\n');
                sb.append(lines.get(i));
            }
            String out = sb.toString();
            report.changed = !out.equals(work);
            if (crlf && report.changed) out = out.replace("\n", "\r\n");
            return new FixResult(out, report);
        } catch (Exception e) {
            Report fail = new Report();
            fail.failed = true;
            return new FixResult(source, fail);
        }
    }

    /**
     * File par fix lagao. Rewrite sirf tab jab koi fix laga ho;
     * pehle <script>.orig backup (sirf jab pehle se na ho).
     * Kabhi throw nahi karta — fail par report.failed = true or
     * asal file mehfooz.
     */
    public static Report fixRunnerFile(File scriptFile, int projectNumber) {
        try {
            if (scriptFile == null || !scriptFile.isFile()) {
                Report r = new Report();
                r.failed = true;
                return r;
            }
            String source = new String(Files.readAllBytes(scriptFile.toPath()),
                StandardCharsets.UTF_8);
            FixResult result = fixText(source, projectNumber);
            Report report = result.report;
            if (report.failed || !report.changed) return report;

            File backup = new File(scriptFile.getParentFile(),
                scriptFile.getName() + ".orig");
            if (!backup.exists()) {
                Files.write(backup.toPath(), source.getBytes(StandardCharsets.UTF_8));
            }
            File tmp = new File(scriptFile.getParentFile(),
                scriptFile.getName() + ".fixing-" + System.currentTimeMillis());
            Files.write(tmp.toPath(), result.text.getBytes(StandardCharsets.UTF_8));
            if (!tmp.renameTo(scriptFile)) {
                // rename na ho to seedha likh do (backup pehle se hai)
                try (FileOutputStream out = new FileOutputStream(scriptFile)) {
                    out.write(result.text.getBytes(StandardCharsets.UTF_8));
                }
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
            return report;
        } catch (Exception e) {
            Report r = new Report();
            r.failed = true;
            return r;
        }
    }

    // ------------------------------------------------------------ item 2: history

    private static void fixHistory(List<String> lines, Report report) {
        int count = 0;
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = PRIVATE_TRACKER.matcher(lines.get(i));
            if (m.find()) {
                lines.set(i, m.replaceAll(".mockup_time_tracker.txt"));
                count++;
            }
        }
        if (count > 0) {
            report.history = Status.FIXED;
        } else if (join(lines).contains(".mockup_time_tracker.txt")) {
            report.history = Status.ALREADY_OK;
        }
    }

    // ------------------------------------------------------------ item 1: base path

    private static void fixBasePath(List<String> lines, Report report) {
        int count = 0;
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = BASE_ASSIGN.matcher(lines.get(i));
            if (m.matches()) {
                lines.set(i, m.group(1) + m.group(2)
                    + " = os.environ.get(\"MOCKUP_BASE\", os.path.dirname(os.path.abspath(__file__)))");
                count++;
            }
        }
        if (count > 0) {
            report.basePath = Status.FIXED;
        } else if (join(lines).contains("os.environ.get(\"MOCKUP_BASE\"")) {
            report.basePath = Status.ALREADY_OK;
        }
    }

    // ------------------------------------------------------------ item 3: loading delays

    private static void fixDelays(List<String> lines, Report report) {
        boolean anyFunction = false;
        int fixed = 0;
        for (String fn : new String[]{"animated_spinner", "display_header"}) {
            int[] range = findFunction(lines, fn);
            if (range == null) continue;
            anyFunction = true;
            for (int i = range[1]; i < range[2]; i++) {
                Matcher m = SLEEP_CALL.matcher(lines.get(i));
                StringBuffer sb = new StringBuffer();
                boolean lineChanged = false;
                while (m.find()) {
                    double v;
                    try {
                        v = Double.parseDouble(m.group(1));
                    } catch (Exception e) {
                        v = 0;
                    }
                    if (v > 0 && v < 1) {
                        m.appendReplacement(sb, "time.sleep(0)");
                        lineChanged = true;
                    } else {
                        m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                    }
                }
                m.appendTail(sb);
                if (lineChanged) {
                    lines.set(i, sb.toString());
                    fixed++;
                }
            }
        }
        if (fixed > 0) report.delays = Status.FIXED;
        else if (anyFunction) report.delays = Status.ALREADY_OK;
    }

    // ------------------------------------------------------------ item 4: wakelock

    private static void fixWakelock(List<String> lines, Report report, boolean[] needImport) {
        String all = join(lines);
        if (!all.contains("termux-wake-lock") && !all.contains("termux-wake-unlock")) {
            return; // wakelock hai hi nahi — NOT_APPLIED
        }
        int[] range = findFunction(lines, "set_wakelock");
        if (range != null) {
            StringBuilder body = new StringBuilder();
            for (int i = range[1]; i < range[2]; i++) body.append(lines.get(i)).append('\n');
            if (body.toString().contains("subprocess.Popen")) {
                report.wakelock = Status.ALREADY_OK;
                return;
            }
            if (!body.toString().contains("os.system")
                || !body.toString().contains("termux-wake")) {
                return; // ajeeb body — hath nahi lagana
            }
            // Parameter ka naam (aam tor par "enable")
            Matcher dm = WAKE_DEF.matcher(lines.get(range[0]));
            String param = "enable";
            if (dm.find()) {
                String params = dm.group(1).trim();
                if (!params.isEmpty()) {
                    String first = params.split(",")[0].split("=")[0].trim();
                    if (first.matches("[A-Za-z_][A-Za-z0-9_]*")) param = first;
                    else return; // param samajh na aye to skip
                } else {
                    return; // bina param ke conditional nahi ban sakta
                }
            }
            String defIndent = indentStr(lines.get(range[0]));
            List<String> newBody = new ArrayList<>();
            newBody.add(defIndent + "    # Fire-and-forget: app ke session-less process me ye command atak");
            newBody.add(defIndent + "    # sakti hai — run ko is ke jawab ka intezar kabhi nahi karna.");
            newBody.add(defIndent + "    try:");
            newBody.add(defIndent + "        subprocess.Popen([\"termux-wake-lock\" if " + param
                + " else \"termux-wake-unlock\"],");
            newBody.add(defIndent + "                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)");
            newBody.add(defIndent + "    except: pass");
            replaceRange(lines, range[1], range[2], newBody);
            needImport[0] = true;
            report.wakelock = Status.FIXED;
            return;
        }
        // Inline standalone os.system calls
        int fixed = 0;
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = WAKE_INLINE.matcher(lines.get(i));
            if (m.matches()) {
                String ind = m.group(1) != null ? m.group(1) : m.group(3);
                String cmd = m.group(2) != null ? m.group(2) : m.group(4);
                List<String> repl = new ArrayList<>();
                repl.add(ind + "try:");
                repl.add(ind + "    subprocess.Popen([\"" + cmd
                    + "\"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)");
                repl.add(ind + "except: pass");
                replaceRange(lines, i, i + 1, repl);
                i += repl.size() - 1;
                fixed++;
            }
        }
        if (fixed > 0) {
            needImport[0] = true;
            report.wakelock = Status.FIXED;
        }
    }

    // ------------------------------------------------------------ item 5: scan timeout

    private static void fixScanTimeout(List<String> lines, Report report, boolean[] needImport) {
        boolean fixed = false;
        boolean ok = false;

        // (a) safe_scan / scan_media ki definition ka default
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = SCAN_DEF_TIMEOUT.matcher(lines.get(i));
            if (m.find()) {
                int v = parseIntSafe(m.group(2), -1);
                if (v > 1) {
                    lines.set(i, m.replaceFirst("$1" + "1"));
                    fixed = true;
                } else if (v == 1) {
                    ok = true;
                }
            }
        }

        // (b) media-scan wali lines par literal timeout=5 → 1 (sirf 5:
        //     bulk/dir scan ke bare timeout jaan bujh kar nahi cherhte —
        //     published fixed runners me wo wese ke wese rehte hain)
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.contains("termux-media-scan")) continue;
            Matcher m = TIMEOUT_LITERAL.matcher(line);
            StringBuffer sb = new StringBuffer();
            boolean lineChanged = false;
            while (m.find()) {
                int v = parseIntSafe(m.group(1), -1);
                if (v == 5) {
                    m.appendReplacement(sb, "timeout=1");
                    lineChanged = true;
                } else {
                    if (v == 1) ok = true;
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                }
            }
            m.appendTail(sb);
            if (lineChanged) {
                lines.set(i, sb.toString());
                fixed = true;
            }
        }

        // (c) bina timeout wali simple subprocess.run scan call — call ke
        //     band hone ki jagah saaf mile to timeout=1 jod do
        String text = join(lines);
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (true) {
            int start = text.indexOf("subprocess.run(", idx);
            if (start < 0) {
                out.append(text.substring(idx));
                break;
            }
            int end = matchingParen(text, start + "subprocess.run".length(), 800);
            if (end < 0) {
                out.append(text.substring(idx));
                break;
            }
            String call = text.substring(start, end + 1);
            out.append(text, idx, start);
            if (call.contains("termux-media-scan") && !call.contains("timeout")) {
                out.append(call, 0, call.length() - 1).append(", timeout=1)");
                fixed = true;
                needImport[0] = true; // subprocess pehle se istemal ho raha hai, phir bhi pakka kar lo
            } else {
                out.append(call);
            }
            idx = end + 1;
        }
        String newText = out.toString();
        if (!newText.equals(text)) {
            lines.clear();
            for (String l : newText.split("\n", -1)) lines.add(l);
        }

        if (fixed) report.scanTimeout = Status.FIXED;
        else if (ok) report.scanTimeout = Status.ALREADY_OK;
    }

    // ------------------------------------------------------------ item 6: gallery sequence

    private static final String[] HELPER_TEMPLATE = {
        "",
        "# --- Gallery copies (TermuxMod) — jori ki tartib me ---",
        "# Run ke sath hi har jori ko TermuxMod/Project N folder me bhi laga do:",
        "# pehle mockup, phir usi ka wallpaper, har copy ke foran baad scan.",
        "# Folder mojood na ho to khamoshi se skip — folder banana app ka kaam hai.",
        "TM_ROOT = \"/storage/emulated/0/TermuxMod\"",
        "def gallery_pair_copy(mock_path, wall_path, mt, wt):",
        "    try:",
        "        proj = os.path.join(TM_ROOT, f\"Project {__PNUM__}\")",
        "        dm = os.path.join(proj, \"Mockups\")",
        "        dr = os.path.join(proj, \"Ready Wallpapers\")",
        "        if not (os.path.isdir(dm) and os.path.isdir(dr)):",
        "            return",
        "        import shutil",
        "        dm_p = os.path.join(dm, os.path.basename(mock_path))",
        "        shutil.copyfile(mock_path, dm_p)",
        "        os.utime(dm_p, (mt, mt))",
        "        safe_scan(dm_p)",
        "        dr_p = os.path.join(dr, os.path.basename(wall_path))",
        "        shutil.copyfile(wall_path, dr_p)",
        "        os.utime(dr_p, (wt, wt))",
        "        safe_scan(dr_p)",
        "    except Exception:",
        "        pass",
        "",
    };

    private static void fixGallery(List<String> lines, Report report, int projectNumber) {
        String all = join(lines);
        if (all.contains("gallery_copy_outputs") || all.contains("gallery_pair_copy")) {
            report.gallery = Status.ALREADY_OK;
            return;
        }
        report.galleryNeedsClaude = true; // neeche pattern mil jaye to hat jayega
        int[] range = findFunction(lines, "process_one");
        if (range == null) return;

        int mockIdx = -1;
        int wallIdx = -1;
        String indent = null;
        for (int i = range[1]; i < range[2]; i++) {
            Matcher mm = SCAN_LINE_MOCK.matcher(lines.get(i));
            if (mm.matches()) {
                // agli non-blank line wall wali honi chahiye
                int j = i + 1;
                while (j < range[2] && lines.get(j).trim().isEmpty()) j++;
                if (j < range[2] && SCAN_LINE_WALL.matcher(lines.get(j)).matches()) {
                    mockIdx = i;
                    wallIdx = j;
                    indent = mm.group(1);
                    break;
                }
            }
        }
        if (mockIdx < 0) return;

        // Waqt wale variables: mt/wt mojood hon to wahi, warna t_now se
        String args = null;
        boolean hasMtWt = false;
        boolean hasTNow = false;
        for (int i = range[1]; i < mockIdx; i++) {
            String l = lines.get(i);
            if (MT_WT_ASSIGN.matcher(l).find()
                || (MT_ASSIGN.matcher(l).find() && WT_ASSIGN.matcher(l).find())) {
                hasMtWt = true;
            }
            if (l.contains("t_now")) hasTNow = true;
        }
        if (hasMtWt) args = "mt, wt";
        else if (hasTNow) args = "t_now+60, t_now+120";
        if (args == null) return; // waqt ka hisaab samajh na aye — skip

        // Pehle call lagao (body ke andar), phir helper def se pehle
        lines.add(wallIdx + 1, indent + "gallery_pair_copy(mock_path, wall_path, " + args + ")");

        String pnum = Pattern.compile("^P_NUM\\s*=", Pattern.MULTILINE).matcher(all).find()
            ? "P_NUM" : String.valueOf(projectNumber);
        List<String> helper = new ArrayList<>();
        for (String h : HELPER_TEMPLATE) helper.add(h.replace("__PNUM__", pnum));
        int[] range2 = findFunction(lines, "process_one");
        lines.addAll(range2[0], helper);

        report.gallery = Status.FIXED;
        report.galleryNeedsClaude = false;
    }

    // ------------------------------------------------------------ helpers

    private static final Pattern SUBPROCESS_IMPORT = Pattern.compile(
        "^\\s*import\\s+.*\\bsubprocess\\b");

    private static void ensureSubprocessImport(List<String> lines) {
        for (String l : lines) {
            if (SUBPROCESS_IMPORT.matcher(l).find()) return;
        }
        // Upar wale import block ke akhir me jod do
        int insertAt = 0;
        for (int i = 0; i < Math.min(lines.size(), 80); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("import ") || t.startsWith("from ")) {
                insertAt = i + 1;
            } else if (insertAt > 0 && !t.isEmpty() && !t.startsWith("#")) {
                break;
            }
        }
        lines.add(insertAt, "import subprocess");
    }

    /** def line + body ka range: [defIdx, bodyStart, bodyEndExclusive). Na mile to null. */
    private static int[] findFunction(List<String> lines, String name) {
        Pattern def = Pattern.compile("^(\\s*)def\\s+" + Pattern.quote(name) + "\\s*\\(");
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = def.matcher(lines.get(i));
            if (m.find()) {
                int defIndent = m.group(1).length();
                int j = i + 1;
                while (j < lines.size()) {
                    String l = lines.get(j);
                    if (l.trim().isEmpty()) {
                        j++;
                        continue;
                    }
                    if (indentOf(l) <= defIndent) break;
                    j++;
                }
                return new int[]{i, i + 1, j};
            }
        }
        return null;
    }

    private static void replaceRange(List<String> lines, int from, int toExclusive, List<String> repl) {
        for (int i = toExclusive - 1; i >= from; i--) lines.remove(i);
        lines.addAll(from, repl);
    }

    /** openIdx par '(' hai — us ki matching ')' ki position, window ke andar na mile to -1. */
    private static int matchingParen(String text, int openIdx, int window) {
        int depth = 0;
        int limit = Math.min(text.length(), openIdx + window);
        for (int i = openIdx; i < limit; i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static int indentOf(String line) {
        int n = 0;
        while (n < line.length() && (line.charAt(n) == ' ' || line.charAt(n) == '\t')) n++;
        return n;
    }

    private static String indentStr(String line) {
        int n = indentOf(line);
        return line.substring(0, n);
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        return sb.toString();
    }
}
