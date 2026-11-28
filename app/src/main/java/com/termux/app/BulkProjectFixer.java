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
 *   5. SCAN (ONE-BY-ONE) — v149 se target RESTORED W1 wala
 *      waiting form hai: safe_scan/scan_media ka body
 *      subprocess.run (ruk kar, one-by-one) ho, timeout_sec ka
 *      default 5 (bina timeout param wale par literal timeout=5).
 *      Body pehle se isi form me ho to ALREADY_OK — koi change
 *      nahi. Fire-and-forget Popen wala body ab FIX hota hai.
 *      safe_scan_dir or bulk/batch scan helpers ko hath NAHI lagta.
 *      v149 revision: canonical form me SCAN WAQFA bhi shamil hai —
 *      gallery copy (TermuxMod/Project wala path) ke scan ke baad
 *      time.sleep(0.25), or us ke liye _waqfa_on() helper (marker
 *      TermuxMod/.scan_waqfa, "off"/"0" = band, warna ON) scan def
 *      se pehle. Bundled runners isi form me hain — ALREADY_OK.
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
            boolean[] needTimeImport = {false};

            fixHistory(lines, report);
            fixBasePath(lines, report);
            fixDelays(lines, report);
            fixWakelock(lines, report, needSubprocessImport);
            fixScanTimeout(lines, report, needSubprocessImport, needTimeImport);
            fixGallery(lines, report, projectNumber);
            if (needSubprocessImport[0]) ensureSubprocessImport(lines);
            if (needTimeImport[0]) ensureTimeImport(lines);

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

    // ------------------------------------------------------------ item 5: scan (one-by-one)

    /**
     * Canonical scan form ka waqfa helper — bundled runners ke
     * bytes se HU-BAHU yahi text hai (is liye woh ALREADY_OK
     * parte hain). Marker ek run me ek dafa parha jata hai;
     * "off"/"0" = waqfa band, marker na ho ya kuch aur ho = ON.
     */
    private static final String[] WAQFA_HELPER = {
        "_WAQFA = None",
        "def _waqfa_on():",
        "    # Scan waqfa (v149): gallery copy ke scan ke baad chhota sa gap, taake",
        "    # MediaStore ka record sahi tartib me lage. Marker TermuxMod/.scan_waqfa",
        "    # me \"off\" (ya \"0\") ho to waqfa band; marker na ho ya kuch aur ho to ON.",
        "    # Marker ek run me sirf ek dafa parha jata hai.",
        "    global _WAQFA",
        "    if _WAQFA is None:",
        "        _WAQFA = True",
        "        try:",
        "            with open(\"/storage/emulated/0/TermuxMod/.scan_waqfa\") as _f:",
        "                if _f.read().strip().lower() in (\"off\", \"0\"):",
        "                    _WAQFA = False",
        "        except Exception:",
        "            pass",
        "    return _WAQFA",
    };

    /**
     * Per-file scan function (safe_scan, warna scan_media) ka target
     * v149 se RESTORED W1 wala waiting form hai (owner ka hukm
     * 2026-11-26: gallery ki asal tartib sirf one-by-one ruk kar
     * scan karne se perfect rehti hai):
     *   timeout_sec param ho  → def ka default 5, body me
     *       subprocess.run(..., timeout=timeout_sec)
     *   timeout param na ho   → body me subprocess.run(...,
     *       timeout=5)  (P2/P16 wali surat)
     * Body pehle se bilkul isi form me ho to ALREADY_OK or koi
     * change nahi (byte-identical ki guarantee). Fire-and-forget
     * Popen wala body ab FIX hota hai — pura body canonical
     * waiting form se badal jata hai; def ka default 5 ke ilawa
     * ho to 5 kar dete hain. safe_scan_dir / bulk helpers is me
     * nahi ate — un ke naam alag hain or unhe jaan bujh kar nahi
     * cherhte.
     *
     * v149 revision — canonical form me SCAN WAQFA bhi hai: body
     * ke aakhir me gallery-copy wale path par time.sleep(0.25),
     * or file me _waqfa_on() helper (na ho to scan def se pehle
     * inject). Waiting form ho lekin waqfa na ho to waqfa FIX
     * hota hai; dono hon to ALREADY_OK.
     */
    private static void fixScanTimeout(List<String> lines, Report report,
                                       boolean[] needImport, boolean[] needTimeImport) {
        int[] range = null;
        String param = null;
        String fnName = null;
        boolean hasTimeoutParam = false;
        for (String fn : new String[]{"safe_scan", "scan_media"}) {
            int[] r = findFunction(lines, fn);
            if (r == null) continue;
            Matcher dm = Pattern.compile("def\\s+" + Pattern.quote(fn) + "\\s*\\(([^)]*)\\)")
                .matcher(lines.get(r[0]));
            if (!dm.find()) continue;
            String ps = dm.group(1).trim();
            if (ps.isEmpty()) continue;
            String first = ps.split(",")[0].split("=")[0].trim();
            if (!first.matches("[A-Za-z_][A-Za-z0-9_]*")) continue;
            range = r;
            param = first;
            fnName = fn;
            for (String p : ps.split(",")) {
                if (p.split("=")[0].trim().equals("timeout_sec")) hasTimeoutParam = true;
            }
            break;
        }
        if (range == null) return; // scan function hi nahi — NOT_APPLIED

        StringBuilder body = new StringBuilder();
        for (int i = range[1]; i < range[2]; i++) body.append(lines.get(i)).append('\n');
        String b = body.toString();
        boolean waiting = b.contains("subprocess.run") && b.contains("termux-media-scan");
        boolean popen = b.contains("subprocess.Popen") && b.contains("termux-media-scan");
        if (!waiting && !popen) {
            return; // ajeeb body — hath nahi lagana
        }

        boolean changed = false;

        // Def ka default timeout_sec: 5 ke ilawa ho to 5 (target form)
        if (hasTimeoutParam) {
            Matcher m = SCAN_DEF_TIMEOUT.matcher(lines.get(range[0]));
            if (m.find() && parseIntSafe(m.group(2), -1) != 5) {
                lines.set(range[0], m.replaceFirst("$1" + "5"));
                changed = true;
            }
        }

        // Body bilkul target form me hai? (waiting + sahi timeout
        // istemal + waqfa wali do lines)
        String waqfaIf = "if _waqfa_on() and \"/TermuxMod/Project \" in str(" + param + "):";
        boolean bodyExact = (hasTimeoutParam
            ? (waiting && b.contains("timeout=timeout_sec"))
            : (waiting && b.contains("timeout=5")))
            && b.contains(waqfaIf) && b.contains("time.sleep(0.25)");
        if (!bodyExact) {
            // Body ka asal hissa (akhir ki khali lines mehfooz rakho)
            int bodyEnd = range[2];
            while (bodyEnd > range[1] && lines.get(bodyEnd - 1).trim().isEmpty()) bodyEnd--;

            String defIndent = indentStr(lines.get(range[0]));
            String timeoutArg = hasTimeoutParam ? "timeout_sec" : "5";
            List<String> newBody = new ArrayList<>();
            newBody.add(defIndent + "    try:");
            newBody.add(defIndent + "        subprocess.run([\"termux-media-scan\", " + param
                + "], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=" + timeoutArg + ")");
            newBody.add(defIndent + "    except subprocess.TimeoutExpired:");
            newBody.add(defIndent + "        pass");
            newBody.add(defIndent + "    except Exception:");
            newBody.add(defIndent + "        pass");
            newBody.add(defIndent + "    " + waqfaIf);
            newBody.add(defIndent + "        time.sleep(0.25)");
            replaceRange(lines, range[1], bodyEnd, newBody);
            changed = true;
        }

        // Waqfa helper file me na ho to scan def se pehle inject
        // (bundled runners ke bytes jesa hi block + ek khali line)
        if (!join(lines).contains("def _waqfa_on():")) {
            int[] r2 = findFunction(lines, fnName);
            if (r2 != null) {
                List<String> helper = new ArrayList<>();
                for (String h : WAQFA_HELPER) helper.add(h);
                helper.add("");
                lines.addAll(r2[0], helper);
                changed = true;
            }
        }

        if (changed) {
            needImport[0] = true;
            needTimeImport[0] = true;
            report.scanTimeout = Status.FIXED;
        } else {
            report.scanTimeout = Status.ALREADY_OK;
        }
    }

    // ------------------------------------------------------------ item 6: gallery sequence

    private static final String[] HELPER_TEMPLATE = {
        "",
        "# --- Gallery copies (TermuxMod) — jori ki tartib me, EK hi Pairs folder ---",
        "# Run ke sath hi har jori ko TermuxMod/Project N/Pairs me laga do:",
        "# pehle mockup, phir usi ka wallpaper, har copy ke foran baad scan.",
        "# Project folder mojood na ho to runner KHUD bana leta hai (v156) —",
        "# ghayab folder delivery ko khamoshi se nahi nigal sakta.",
        "TM_ROOT = \"/storage/emulated/0/TermuxMod\"",
        "def gallery_pair_copy(mock_path, wall_path, mt, wt):",
        "    try:",
        "        proj = os.path.join(TM_ROOT, f\"Project {__PNUM__}\")",
        "        if not os.path.isdir(proj):",
        "            os.makedirs(proj, exist_ok=True)",
        "        dp = os.path.join(proj, \"Pairs\")",
        "        if not os.path.isdir(dp):",
        "            os.makedirs(dp, exist_ok=True)",
        "        import shutil",
        "        dm_p = os.path.join(dp, os.path.basename(mock_path))",
        "        shutil.copyfile(mock_path, dm_p)",
        "        os.utime(dm_p, (mt, mt))",
        "        safe_scan(dm_p)",
        "        dr_p = os.path.join(dp, os.path.basename(wall_path))",
        "        shutil.copyfile(wall_path, dr_p)",
        "        os.utime(dr_p, (wt, wt))",
        "        safe_scan(dr_p)",
        "    except Exception as _ge:",
        "        print(f\"⚠ Gallery copy nahi hui (Project {__PNUM__}): {_ge}\", flush=True)",
        "",
        "# --- Pairs safai (delete ke sath) ---",
        "# parity: 'odd' = sirf mockups, 'even' = sirf wallpapers, 'all' = dono.",
        "def tm_pairs_cleanup(parity):",
        "    try:",
        "        dp = os.path.join(TM_ROOT, f\"Project {__PNUM__}\", \"Pairs\")",
        "        if not os.path.isdir(dp):",
        "            return",
        "        for f in os.listdir(dp):",
        "            if f.startswith('.'):",
        "                continue",
        "            low = f.lower()",
        "            if not low.endswith(('.jpg', '.jpeg', '.png', '.webp')):",
        "                continue",
        "            stem = os.path.splitext(f)[0]",
        "            if not stem.isdigit():",
        "                continue",
        "            n = int(stem)",
        "            if parity == 'odd' and n % 2 == 0:",
        "                continue",
        "            if parity == 'even' and n % 2 == 1:",
        "                continue",
        "            p = os.path.join(dp, f)",
        "            try:",
        "                os.remove(p)",
        "            except Exception:",
        "                pass",
        "            safe_scan(p)",
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
        addImportLine(lines, "import subprocess");
    }

    private static final Pattern TIME_IMPORT = Pattern.compile(
        "^\\s*import\\s+.*\\btime\\b|^\\s*from\\s+time\\s+import\\b");

    /** Waqfa wala canonical body time.sleep istemal karta hai — import na ho to jod do. */
    private static void ensureTimeImport(List<String> lines) {
        for (String l : lines) {
            if (TIME_IMPORT.matcher(l).find()) return;
        }
        addImportLine(lines, "import time");
    }

    /** Upar wale import block ke akhir me ek import line jod do. */
    private static void addImportLine(List<String> lines, String stmt) {
        int insertAt = 0;
        for (int i = 0; i < Math.min(lines.size(), 80); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("import ") || t.startsWith("from ")) {
                insertAt = i + 1;
            } else if (insertAt > 0 && !t.isEmpty() && !t.startsWith("#")) {
                break;
            }
        }
        lines.add(insertAt, stmt);
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
