package com.termux.app;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * v145 — New Project Check (BulkProjectFixer): ZIP-add par runner
 * ki auto-fix. Char scenario: purana-style runner (chhe ke chhe
 * fix), pehle-se-fixed runner (koi change nahi), group-style
 * runner (gallery skip + Claude wali note), garbage file (no
 * crash, no change). Sath me file-level backup/idempotence,
 * strings or wiring (source scan) ki tests.
 */
public class BulkV145Test {

    private static String lines(String... ls) {
        return String.join("\n", ls) + "\n";
    }

    // ------------------------------------------------------------ samples

    /** Purana-style P1-jaisa runner — chhe ke chhe pattern mojood. */
    private static final String OLD_STYLE = lines(
        "import os, sys, time, random, subprocess",
        "import cv2",
        "",
        "P_NUM = 9",
        "B = f\"/storage/emulated/0/mockup_project_{P_NUM}\"",
        "T_F = \"/storage/emulated/0/.mockup_time_tracker_p9.txt\"",
        "I = os.path.join(B, \"9_wallpapers\")",
        "O = os.path.join(B, \"9_Mockups\")",
        "R = os.path.join(B, \"9_Ready_Wallpapers\")",
        "",
        "def display_header():",
        "    boot_lines = [\"Initializing secure shell........\", \"Loading modules.......\"]",
        "    for line in boot_lines:",
        "        print(line)",
        "        time.sleep(0.03)",
        "    print(\"ADNAN\")",
        "",
        "def animated_spinner(text, duration=0.8):",
        "    frames = ['x', 'o', 'O', 'o']",
        "    for i in range(int(duration * 10)):",
        "        sys.stdout.write(\"\\r \" + text)",
        "        sys.stdout.flush()",
        "        time.sleep(0.08)",
        "    sys.stdout.write(\"\\r Done\\n\")",
        "",
        "def set_wakelock(enable=True):",
        "    try:",
        "        if enable: os.system(\"termux-wake-lock >/dev/null 2>&1\")",
        "        else: os.system(\"termux-wake-unlock >/dev/null 2>&1\")",
        "    except: pass",
        "",
        "def safe_scan(path, timeout_sec=5):",
        "    try:",
        "        subprocess.run([\"termux-media-scan\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)",
        "    except subprocess.TimeoutExpired:",
        "        pass",
        "    except Exception:",
        "        pass",
        "",
        "def process_one(f, idx, t_now):",
        "    raw_p = os.path.join(I, f)",
        "    mock_path = os.path.join(O, f\"{idx}.jpg\")",
        "    wall_path = os.path.join(R, f\"{idx+1}.jpg\")",
        "    mt, wt = t_now+60, t_now+120",
        "    save_image(raw_p, mock_path)",
        "    os.utime(mock_path, (mt, mt))",
        "    save_image(raw_p, wall_path)",
        "    os.utime(wall_path, (wt, wt))",
        "    safe_scan(mock_path)",
        "    safe_scan(wall_path)",
        "    return 'ok', idx+2, wt"
    );

    /** Pehle se fixed runner — fixer ko hath hi nahi lagana chahiye. */
    private static final String ALREADY_FIXED = lines(
        "import os, sys, time, subprocess",
        "",
        "P_NUM = 9",
        "B = os.environ.get(\"MOCKUP_BASE\", os.path.dirname(os.path.abspath(__file__)))",
        "T_F = \"/storage/emulated/0/.mockup_time_tracker.txt\"",
        "",
        "def display_header():",
        "    print(\"ADNAN\")",
        "",
        "def animated_spinner(text, duration=0.8):",
        "    for i in range(int(duration * 10)):",
        "        sys.stdout.flush()",
        "        time.sleep(0)",
        "",
        "def set_wakelock(enable=True):",
        "    try:",
        "        subprocess.Popen([\"termux-wake-lock\" if enable else \"termux-wake-unlock\"],",
        "                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)",
        "    except: pass",
        "",
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
        "",
        "def safe_scan(path, timeout_sec=5):",
        "    try:",
        "        subprocess.run([\"termux-media-scan\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)",
        "    except subprocess.TimeoutExpired:",
        "        pass",
        "    except Exception:",
        "        pass",
        "    if _waqfa_on() and \"/TermuxMod/Project \" in str(path):",
        "        time.sleep(0.25)",
        "",
        "TM_ROOT = \"/storage/emulated/0/TermuxMod\"",
        "def gallery_pair_copy(mock_path, wall_path, mt, wt):",
        "    pass",
        "",
        "def process_one(f, idx, t_now):",
        "    mt, wt = t_now+60, t_now+120",
        "    safe_scan(mock_path)",
        "    safe_scan(wall_path)",
        "    gallery_pair_copy(mock_path, wall_path, mt, wt)",
        "    return 'ok', idx+2, wt"
    );

    /** Group-style runner — pair pattern nahi, gallery skip hona chahiye. */
    private static final String GROUP_STYLE = lines(
        "import os, sys, time, subprocess",
        "",
        "P_NUM = 10",
        "BASE_DIR = \"/storage/emulated/0/mockup_project_10\"",
        "T_F = \"/storage/emulated/0/.mockup_global_time.txt\"",
        "",
        "def display_header():",
        "    for line in [\"boot\"]:",
        "        print(line)",
        "        time.sleep(0.03)",
        "",
        "def animated_spinner(text, duration=0.8):",
        "    for i in range(int(duration * 10)):",
        "        time.sleep(0.08)",
        "",
        "def set_wakelock(enable=True):",
        "    try:",
        "        if enable: os.system(\"termux-wake-lock >/dev/null 2>&1\")",
        "        else: os.system(\"termux-wake-unlock >/dev/null 2>&1\")",
        "    except: pass",
        "",
        "def safe_scan(path, timeout_sec=5):",
        "    try:",
        "        subprocess.run([\"termux-media-scan\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)",
        "    except subprocess.TimeoutExpired:",
        "        pass",
        "",
        "def scan_group(paths):",
        "    for p in paths:",
        "        subprocess.run([\"termux-media-scan\", p], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)",
        "",
        "def process_group(files, t_now):",
        "    outs = build_group(files)",
        "    scan_group(outs)",
        "    return len(outs)"
    );

    private static final String GARBAGE = lines(
        "{{{{ random bytes !!!",
        "def broken(:",
        "    x = (((",
        "print 'old style'",
        "\u0001\u0002\u0003"
    );

    // ------------------------------------------------------------ scenario 1: old style

    @Test
    public void oldStyleRunnerGetsFixesScanAlreadyOk() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(OLD_STYLE, 9);
        assertFalse(r.report.failed);
        assertTrue(r.report.changed);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.basePath);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.history);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.delays);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.wakelock);
        // v149 revision: OLD_STYLE ka scan waiting form me hai lekin
        // waqfa nahi — canonical form me waqfa bhi hai, is liye FIXED
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.scanTimeout);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.gallery);
        assertFalse(r.report.galleryNeedsClaude);

        String out = r.text;
        // 1: base path family form
        assertTrue(out.contains(
            "B = os.environ.get(\"MOCKUP_BASE\", os.path.dirname(os.path.abspath(__file__)))"));
        assertFalse(out.contains("/storage/emulated/0/mockup_project_{P_NUM}"));
        // 2: shared tracker
        assertTrue(out.contains(".mockup_time_tracker.txt"));
        assertFalse(out.contains("_p9.txt"));
        // 3: decorative sleeps zeroed, function ke bahar wala koi sleep nahi tha
        assertFalse(out.contains("time.sleep(0.03)"));
        assertFalse(out.contains("time.sleep(0.08)"));
        assertTrue(out.contains("time.sleep(0)"));
        // 4: wakelock fire-and-forget
        assertTrue(out.contains("subprocess.Popen([\"termux-wake-lock\" if enable else \"termux-wake-unlock\"]"));
        assertFalse(out.contains("os.system(\"termux-wake-lock"));
        // 5: scan (one-by-one) — waiting form raha + waqfa juda
        assertTrue(out.contains("def safe_scan(path, timeout_sec=5)"));
        assertTrue(out.contains(
            "subprocess.run([\"termux-media-scan\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)"));
        assertFalse(out.contains("subprocess.Popen([\"termux-media-scan\", path]"));
        assertTrue(out.contains("def _waqfa_on():"));
        assertTrue(out.contains(
            "if _waqfa_on() and \"/TermuxMod/Project \" in str(path):"));
        assertTrue(out.contains("time.sleep(0.25)"));
        // 6: gallery helper + call, sahi tartib me (scan ke baad)
        assertTrue(out.contains("def gallery_pair_copy(mock_path, wall_path, mt, wt):"));
        int scanAt = out.indexOf("safe_scan(mock_path)");
        int callAt = out.indexOf("gallery_pair_copy(mock_path, wall_path, mt, wt)", scanAt);
        assertTrue("gallery call scan ke baad hona chahiye", callAt > scanAt);
    }

    // ------------------------------------------------------------ scenario 2: already fixed

    @Test
    public void alreadyFixedRunnerStaysByteIdentical() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(ALREADY_FIXED, 9);
        assertFalse(r.report.failed);
        assertFalse("fixed runner par koi change nahi hona chahiye", r.report.changed);
        assertEquals(ALREADY_FIXED, r.text);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, r.report.basePath);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, r.report.history);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, r.report.delays);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, r.report.wakelock);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, r.report.scanTimeout);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, r.report.gallery);
        assertFalse(r.report.galleryNeedsClaude);
    }

    // ------------------------------------------------------------ scenario 3: group style

    @Test
    public void groupStyleRunnerSkipsGalleryWithClaudeNote() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(GROUP_STYLE, 10);
        assertFalse(r.report.failed);
        assertTrue(r.report.changed);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.basePath);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.history);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.delays);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.wakelock);
        // v149 revision: GROUP_STYLE ka scan waiting form me hai
        // lekin waqfa nahi — waqfa FIX hota hai
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.scanTimeout);
        assertEquals(BulkProjectFixer.Status.NOT_APPLIED, r.report.gallery);
        assertTrue("gallery skip par Claude wali note lazmi", r.report.galleryNeedsClaude);
        // mukhtalif variable naam ke sath base path fix
        assertTrue(r.text.contains(
            "BASE_DIR = os.environ.get(\"MOCKUP_BASE\", os.path.dirname(os.path.abspath(__file__)))"));
        // safe_scan ka waiting body raha + waqfa juda
        assertTrue(r.text.contains(
            "subprocess.run([\"termux-media-scan\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)"));
        assertFalse(r.text.contains("subprocess.Popen([\"termux-media-scan\", path]"));
        // bulk scan_group helper ko hath NAHI laga (na timeout juda, na Popen bana)
        assertTrue(r.text.contains(
            "subprocess.run([\"termux-media-scan\", p], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)"));
        assertFalse(r.text.contains("timeout=1)"));
        assertFalse(r.text.contains("gallery_pair_copy"));
    }

    // ------------------------------------------------------------ scenario 4: garbage

    @Test
    public void garbageFileNoCrashNoChange() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(GARBAGE, 3);
        assertNotNull(r);
        assertFalse(r.report.failed);
        assertFalse(r.report.changed);
        assertEquals(GARBAGE, r.text);
        // null input bhi crash nahi
        BulkProjectFixer.FixResult rn = BulkProjectFixer.fixText(null, 3);
        assertTrue(rn.report.failed);
    }

    // ------------------------------------------------------------ scenario 5: scan_media (P5-style)

    /** P5-style runner — scan_media naam wala function + safe_scan_dir helper. */
    private static final String SCAN_MEDIA_STYLE = lines(
        "import os, subprocess",
        "",
        "P_NUM = 5",
        "B = os.environ.get(\"MOCKUP_BASE\", os.path.dirname(os.path.abspath(__file__)))",
        "T_F = \"/storage/emulated/0/.mockup_time_tracker.txt\"",
        "",
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
        "",
        "def scan_media(path, timeout_sec=5):",
        "    try:",
        "        subprocess.run([\"termux-media-scan\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)",
        "    except subprocess.TimeoutExpired:",
        "        pass",
        "    if _waqfa_on() and \"/TermuxMod/Project \" in str(path):",
        "        time.sleep(0.25)",
        "",
        "def safe_scan_dir(path, timeout_sec=20):",
        "    try:",
        "        subprocess.run([\"termux-media-scan\", \"-r\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)",
        "    except subprocess.TimeoutExpired:",
        "        pass"
    );

    @Test
    public void scanMediaVariantAlreadyOkAndDirHelperUntouched() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(SCAN_MEDIA_STYLE, 5);
        assertFalse(r.report.failed);
        // v149 revision: scan_media canonical form (waiting + waqfa) me hai
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, r.report.scanTimeout);
        assertFalse(r.report.changed);
        assertEquals(SCAN_MEDIA_STYLE, r.text);
        assertTrue(r.text.contains("def scan_media(path, timeout_sec=5):"));
        assertTrue(r.text.contains(
            "subprocess.run([\"termux-media-scan\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)"));
        // safe_scan_dir bilkul waisa ka waisa (bulk helper ko hath nahi lagta)
        assertTrue(r.text.contains("def safe_scan_dir(path, timeout_sec=20):"));
        assertTrue(r.text.contains(
            "subprocess.run([\"termux-media-scan\", \"-r\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)"));
    }

    // ------------------------------------------------------------ file level

    @Test
    public void fileLevelBackupRewriteAndIdempotence() throws Exception {
        File dir = Files.createTempDirectory("bulkfix").toFile();
        File script = new File(dir, "w9.py");
        Files.write(script.toPath(), OLD_STYLE.getBytes(StandardCharsets.UTF_8));

        BulkProjectFixer.Report r1 = BulkProjectFixer.fixRunnerFile(script, 9);
        assertFalse(r1.failed);
        assertTrue(r1.changed);
        String after = new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8);
        assertTrue(after.contains("def gallery_pair_copy"));
        assertTrue(after.contains("timeout_sec=5"));
        assertTrue(after.contains("subprocess.run([\"termux-media-scan\", path]"));
        assertFalse(after.contains("subprocess.Popen([\"termux-media-scan\", path]"));

        File backup = new File(dir, "w9.py.orig");
        assertTrue("backup .orig banna chahiye", backup.isFile());
        assertEquals(OLD_STYLE,
            new String(Files.readAllBytes(backup.toPath()), StandardCharsets.UTF_8));

        // Dusri dafa: sab ALREADY_OK, koi change nahi, backup bhi waisa ka waisa
        BulkProjectFixer.Report r2 = BulkProjectFixer.fixRunnerFile(script, 9);
        assertFalse(r2.failed);
        assertFalse(r2.changed);
        assertEquals(OLD_STYLE,
            new String(Files.readAllBytes(backup.toPath()), StandardCharsets.UTF_8));

        // Ghayab file: fail report, crash nahi
        BulkProjectFixer.Report r3 = BulkProjectFixer.fixRunnerFile(new File(dir, "nope.py"), 9);
        assertTrue(r3.failed);
    }

    // ------------------------------------------------------------ strings

    @Test
    public void projectCheckStringsHaveBothLanguages() {
        String[] keys = {
            BulkStrings.CHECK_TITLE, BulkStrings.CHECK_BASE_PATH,
            BulkStrings.CHECK_HISTORY, BulkStrings.CHECK_DELAYS,
            BulkStrings.CHECK_WAKELOCK, BulkStrings.CHECK_SCAN,
            BulkStrings.CHECK_GALLERY, BulkStrings.CHECK_FIXED,
            BulkStrings.CHECK_OK, BulkStrings.CHECK_NA,
            BulkStrings.CHECK_FAILED, BulkStrings.CHECK_GALLERY_CLAUDE
        };
        for (String k : keys) {
            assertTrue("dono zubanen: " + k, BulkStrings.hasBothLanguages(k));
            assertFalse(BulkStrings.get(k, false).trim().isEmpty());
            assertFalse(BulkStrings.get(k, true).trim().isEmpty());
        }
        assertEquals("Gallery sequence nahi laga — ye project Muse ko bhejo, wo laga de ga",
            BulkStrings.get(BulkStrings.CHECK_GALLERY_CLAUDE, true));
        assertEquals("Fix kar diya", BulkStrings.get(BulkStrings.CHECK_FIXED, true));
        assertEquals("Pehle se theek", BulkStrings.get(BulkStrings.CHECK_OK, true));
    }

    // ------------------------------------------------------------ wiring (source scan)

    private static String findSource(String rel) throws Exception {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 8 && dir != null; i++) {
            File a = new File(dir, rel);
            if (a.isFile()) return new String(Files.readAllBytes(a.toPath()), StandardCharsets.UTF_8);
            File b = new File(dir, "bulk_mockup_fork/termux-app/" + rel);
            if (b.isFile()) return new String(Files.readAllBytes(b.toPath()), StandardCharsets.UTF_8);
            File c = new File(dir, "workspace/bulk_mockup_fork/termux-app/" + rel);
            if (c.isFile()) return new String(Files.readAllBytes(c.toPath()), StandardCharsets.UTF_8);
            dir = dir.getParentFile();
        }
        return null;
    }

    @Test
    public void fixerIsWiredIntoInstallAndResultScreen() throws Exception {
        String installer = findSource("app/src/main/java/com/termux/app/BulkProjectInstaller.java");
        assertNotNull("installer source milna chahiye", installer);
        assertTrue(installer.contains("BulkProjectFixer.fixRunnerFile"));
        assertTrue(installer.contains("lastFixReport"));

        String add = findSource("app/src/main/java/com/termux/app/BulkAddProjectActivity.java");
        assertNotNull(add);
        assertTrue(add.contains("mDoneCheck"));
        assertTrue(add.contains("buildCheckText"));
        assertTrue(add.contains("BulkStrings.CHECK_TITLE"));
        assertTrue(add.contains("BulkStrings.CHECK_GALLERY_CLAUDE"));
    }

    @Test
    public void versionBumpedTo149() throws Exception {
        String dash = findSource("app/src/main/java/com/termux/app/BulkDashboardActivity.java");
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v153"));
        String gradle = findSource("app/build.gradle");
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 153"));
    }
}
