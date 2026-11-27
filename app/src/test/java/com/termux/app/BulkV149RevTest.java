package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Termux Mod v149 REVISION test (owner 2026-11-26 12:53–13:01):
 *  - R1/R4: scan WAQFA — canonical scan form me gallery copy ke
 *    scan ke baad time.sleep(0.25) + _waqfa_on() helper (marker
 *    TermuxMod/.scan_waqfa). Fixer Popen/waiting-bagair-waqfa ko
 *    canonical par lata hai; bundled runners ALREADY_OK.
 *  - R2: More Settings ki "Scan Waqfa" row + marker wiring
 *    (BulkScanWaqfa, default ON, app start par syncMarker).
 *  - R3: Order Fix SMART pass — smartFixSuffix ka faisla:
 *    tartib sahi → fix set khali; crossing position k par ho
 *    to fix set = suffix(k); row na ho to file suffix me
 *    shamil; prefix ko hath nahi lagta.
 *  - Item H: Permissions screen par Termux:API ki ASAL
 *    mojoodgi (BulkApiCheck) — missing → install ki hidayat +
 *    neeche download link; installed → sirf "Done".
 *    [v150 me superseded: scan built-in hai — yehi Item H
 *    tests ab wrapper-content check parhte hain.]
 */
public class BulkV149RevTest {

    private static final String FIXER =
        "app/src/main/java/com/termux/app/BulkProjectFixer.java";
    private static final String REGISTER =
        "app/src/main/java/com/termux/app/BulkMediaRegister.java";
    private static final String WAQFA =
        "app/src/main/java/com/termux/app/BulkScanWaqfa.java";
    private static final String API_CHECK =
        "app/src/main/java/com/termux/app/BulkApiCheck.java";
    private static final String MORE_SETTINGS =
        "app/src/main/java/com/termux/app/BulkMoreSettingsActivity.java";
    private static final String PERMISSIONS =
        "app/src/main/java/com/termux/app/BulkPermissionsActivity.java";
    private static final String DASH =
        "app/src/main/java/com/termux/app/BulkDashboardActivity.java";
    private static final String MANIFEST = "app/src/main/AndroidManifest.xml";

    private static File findFile(String relative) {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 8 && dir != null; i++) {
            File a = new File(dir, relative);
            if (a.isFile()) return a;
            File b = new File(dir, "bulk_mockup_fork/termux-app/" + relative);
            if (b.isFile()) return b;
            File c = new File(dir, "workspace/bulk_mockup_fork/termux-app/" + relative);
            if (c.isFile()) return c;
            dir = dir.getParentFile();
        }
        return null;
    }

    private static String findSource(String relative) throws Exception {
        File f = findFile(relative);
        if (f == null) return null;
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static String readZipEntry(File zip, String entryName) throws Exception {
        try (ZipFile zf = new ZipFile(zip)) {
            ZipEntry e = zf.getEntry(entryName);
            assertNotNull("entry missing: " + entryName + " in " + zip, e);
            try (InputStream in = zf.getInputStream(e)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }

    // ------------------------------------------------------------ R4 samples

    /** Fire-and-forget Popen form, time import BHI nahi (import fix ka saboot). */
    private static final String POPEN_NO_TIME =
        "import os, subprocess\n"
        + "def safe_scan(path, timeout_sec=1):\n"
        + "    try:\n"
        + "        subprocess.Popen([\"termux-media-scan\", path],\n"
        + "            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)\n"
        + "    except Exception:\n"
        + "        pass\n";

    /** Restored waiting form lekin waqfa ke baghair (v149 ki pehli canonical). */
    private static final String WAITING_NO_WAQFA =
        "import os, subprocess, time\n"
        + "def safe_scan(path, timeout_sec=5):\n"
        + "    try:\n"
        + "        subprocess.run([\"termux-media-scan\", path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=timeout_sec)\n"
        + "    except subprocess.TimeoutExpired:\n"
        + "        pass\n"
        + "    except Exception:\n"
        + "        pass\n";

    // ------------------------------------------------------------ R4

    @Test
    public void popenFormConvertsToWaqfaCanonical() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(POPEN_NO_TIME, 9);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.scanTimeout);
        // canonical waiting body
        assertTrue(r.text.contains("def safe_scan(path, timeout_sec=5):"));
        assertTrue(r.text.contains("subprocess.run([\"termux-media-scan\", path],"));
        assertTrue(r.text.contains("timeout=timeout_sec)"));
        // waqfa: helper + gallery shart + gap
        assertTrue(r.text.contains("def _waqfa_on():"));
        assertTrue(r.text.contains("/storage/emulated/0/TermuxMod/.scan_waqfa"));
        assertTrue(r.text.contains("in (\"off\", \"0\")"));
        assertTrue(r.text.contains(
            "if _waqfa_on() and \"/TermuxMod/Project \" in str(path):"));
        assertTrue(r.text.contains("time.sleep(0.25)"));
        // time import khud juda (sample me nahi tha)
        assertTrue(r.text.contains("import time"));
        assertFalse(r.text.contains("Popen([\"termux-media-scan\""));
    }

    @Test
    public void waqfaCanonicalIsAFixedPoint() {
        BulkProjectFixer.FixResult once = BulkProjectFixer.fixText(POPEN_NO_TIME, 9);
        BulkProjectFixer.FixResult twice = BulkProjectFixer.fixText(once.text, 9);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, twice.report.scanTimeout);
        assertEquals(once.text, twice.text);
        assertFalse(twice.report.changed);
    }

    @Test
    public void waitingFormWithoutWaqfaGetsWaqfa() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(WAITING_NO_WAQFA, 9);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.scanTimeout);
        assertTrue(r.text.contains("def _waqfa_on():"));
        assertTrue(r.text.contains("time.sleep(0.25)"));
        // helper scan def se PEHLE ata hai
        assertTrue(r.text.indexOf("def _waqfa_on():")
            < r.text.indexOf("def safe_scan(path"));
        BulkProjectFixer.FixResult twice = BulkProjectFixer.fixText(r.text, 9);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, twice.report.scanTimeout);
        assertEquals(r.text, twice.text);
    }

    @Test
    public void bundledRunnersCarryWaqfaAndReadAlreadyOk() throws Exception {
        String[][] cases = {
            {"app/src/main/assets/bulk/projects/mockup_project_1.zip",
                "mockup_project_1/adnan_core.py", "1"},
            {"app/src/main/assets/bulk/projects/mockup_project_2.zip",
                "mockup_project_2/dual_core.py", "2"},
            {"app/src/main/assets/bulk/projects/mockup_project_16.zip",
                "mockup_project_16/system_core.py", "16"},
        };
        for (String[] c : cases) {
            File zip = findFile(c[0]);
            assertNotNull("zip not found: " + c[0], zip);
            String runner = readZipEntry(zip, c[1]);
            // R1 bake: waqfa helper + gap asal runner me mojood
            assertTrue(c[1] + " me _waqfa_on hona chahiye",
                runner.contains("def _waqfa_on():"));
            assertTrue(c[1] + " me waqfa gap hona chahiye",
                runner.contains("time.sleep(0.25)"));
            assertTrue(c[1] + " me gallery shart honi chahiye",
                runner.contains("\"/TermuxMod/Project \" in str(path)"));
            // R4: updated fixer ke liye yehi canonical hai
            BulkProjectFixer.FixResult r =
                BulkProjectFixer.fixText(runner, Integer.parseInt(c[2]));
            assertEquals(c[1] + " scan item",
                BulkProjectFixer.Status.ALREADY_OK, r.report.scanTimeout);
        }
    }

    @Test
    public void fixerSourceCarriesWaqfaTemplate() throws Exception {
        String src = findSource(FIXER);
        assertNotNull(src);
        assertTrue(src.contains("WAQFA_HELPER"));
        assertTrue(src.contains("ensureTimeImport"));
        assertTrue(src.contains(".scan_waqfa"));
    }

    // ------------------------------------------------------------ R3

    private static final String M = "/s/TermuxMod/Project 1/Mockups/";
    private static final String R = "/s/TermuxMod/Project 1/Ready Wallpapers/";

    private static List<String> numeric(int... ns) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < ns.length; i++) {
            out.add((i % 2 == 0 ? M : R) + ns[i] + ".jpg");
        }
        return out;
    }

    @Test
    public void smartAlreadyOrderedFixesNothing() {
        List<String> numeric = numeric(1, 2, 3, 4);
        assertTrue(BulkMediaRegister.smartFixSuffix(numeric, numeric).isEmpty());
        // asal tartib me sirf inhi files ki rows hain lekin koi
        // aur (purani) file darmiyan me ho — relative tartib phir
        // bhi sahi hai, fix set khali
        List<String> actual = Arrays.asList(
            "/s/TermuxMod/Project 1/Mockups/99.jpg",
            numeric.get(0), numeric.get(1), numeric.get(2), numeric.get(3));
        assertTrue(BulkMediaRegister.smartFixSuffix(numeric, actual).isEmpty());
    }

    @Test
    public void smartCrossingFixesSuffixFromCrossing() {
        List<String> numeric = numeric(1, 2, 3, 4);
        // asal tartib: 1, 3, 2, 4 — crossing file 3 (position 2) par
        List<String> actual = Arrays.asList(
            numeric.get(0), numeric.get(2), numeric.get(1), numeric.get(3));
        List<String> fix = BulkMediaRegister.smartFixSuffix(numeric, actual);
        assertEquals(Arrays.asList(numeric.get(2), numeric.get(3)), fix);
        // prefix (1, 2) bilkul mehfooz
        assertFalse(fix.contains(numeric.get(0)));
        assertFalse(fix.contains(numeric.get(1)));
    }

    @Test
    public void smartCrossingAtStartFixesAlmostAll() {
        List<String> numeric = numeric(1, 2, 3);
        List<String> actual = Arrays.asList(
            numeric.get(1), numeric.get(0), numeric.get(2));
        assertEquals(Arrays.asList(numeric.get(1), numeric.get(2)),
            BulkMediaRegister.smartFixSuffix(numeric, actual));
    }

    @Test
    public void smartMissingRowJoinsSuffix() {
        List<String> numeric = numeric(1, 2, 3, 4);
        // file 2 ki row hi nahi — wahi se suffix shuru
        List<String> actual = Arrays.asList(
            numeric.get(0), numeric.get(2), numeric.get(3));
        assertEquals(Arrays.asList(
                numeric.get(1), numeric.get(2), numeric.get(3)),
            BulkMediaRegister.smartFixSuffix(numeric, actual));
        // aakhri file ki row nahi — sirf wahi fix set me
        List<String> actual2 = Arrays.asList(
            numeric.get(0), numeric.get(1), numeric.get(2));
        assertEquals(Arrays.asList(numeric.get(3)),
            BulkMediaRegister.smartFixSuffix(numeric, actual2));
        // koi row hi nahi — sab fix set me
        assertEquals(numeric, BulkMediaRegister.smartFixSuffix(
            numeric, new ArrayList<String>()));
    }

    @Test
    public void smartNullAndEmptySafety() {
        assertTrue(BulkMediaRegister.smartFixSuffix(null, null).isEmpty());
        assertTrue(BulkMediaRegister.smartFixSuffix(
            new ArrayList<String>(), null).isEmpty());
        List<String> numeric = numeric(1, 2);
        assertEquals(numeric, BulkMediaRegister.smartFixSuffix(numeric, null));
    }

    @Test
    public void smartPassWiring() throws Exception {
        String src = findSource(REGISTER);
        assertNotNull(src);
        // v154: pass ab PLAIN REGISTRATION hai — Pairs folder ki
        // sirf na-registered files, signature plan ke sath; purani
        // tie-suffix/queryRows/DATE_ADDED wiring superseded
        assertTrue(src.contains("public static int registerProject(Context ctx, int n, boolean force)"));
        assertTrue(src.contains("BulkMirror.PAIRS_SUB"));
        assertTrue(src.contains("plan(ordered, sigs, registered)"));
        assertFalse(src.contains("sequenceFixPass"));
        assertFalse(src.contains("applyDateAddedLadder"));
    }

    // ------------------------------------------------------------ R2

    @Test
    public void waqfaSettingDefaultsAndContent() {
        assertTrue(BulkScanWaqfa.resolveEnabled(null));
        assertTrue(BulkScanWaqfa.resolveEnabled(Boolean.TRUE));
        assertFalse(BulkScanWaqfa.resolveEnabled(Boolean.FALSE));
        assertEquals("on", BulkScanWaqfa.markerContent(true));
        assertEquals("off", BulkScanWaqfa.markerContent(false));
        assertEquals(".scan_waqfa", BulkScanWaqfa.MARKER_NAME);
    }

    @Test
    public void waqfaRowAndMarkerWiring() throws Exception {
        String more = findSource(MORE_SETTINGS);
        assertNotNull(more);
        assertTrue(more.contains("scanWaqfaRow"));
        assertTrue(more.contains("BulkStrings.SCAN_WAQFA"));
        assertTrue(more.contains("BulkStrings.SCAN_WAQFA_SUB"));
        assertTrue(more.contains("BulkScanWaqfa.setEnabled"));
        assertTrue(more.contains("BulkScanWaqfa.isEnabled"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("BulkScanWaqfa.syncMarker"));
        String waqfa = findSource(WAQFA);
        assertNotNull(waqfa);
        assertTrue(waqfa.contains("KEY_ENABLED = \"scan_waqfa_enabled\""));
        assertTrue(waqfa.contains("markerContent"));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.SCAN_WAQFA));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.SCAN_WAQFA_SUB));
        assertEquals("Scan Waqfa", BulkStrings.get(BulkStrings.SCAN_WAQFA, false));
        assertEquals(
            "OFF karne par terminal purani tez speed par chalta hai, order fix app kare ga",
            BulkStrings.get(BulkStrings.SCAN_WAQFA_SUB, true));
    }

    // ------------------------------------------------------------ Item H

    // v150: Item H ka purana Termux:API-presence wala matlab
    // khatam — scan built-in hai. Ab ye tests wrapper-content
    // check (BulkApiCheck.resolveOk / isScanReady) parhte hain.

    @Test
    public void apiPresenceDecisionIsPure() {
        assertTrue(BulkApiCheck.resolveOk(Boolean.TRUE));
        assertFalse(BulkApiCheck.resolveOk(Boolean.FALSE));
        assertFalse(BulkApiCheck.resolveOk(null));
    }

    @Test
    public void apiCheckIsWrapperContentLookup() throws Exception {
        String src = findSource(API_CHECK);
        assertNotNull(src);
        assertTrue(src.contains("BulkMockupSetup.wrappersOk"));
        assertTrue(src.contains("resolveOk"));
        // purana ghalat dawa kahin nahi hona chahiye
        assertFalse(src.contains("getPackageInfo"));
        assertFalse(src.contains("f-droid.org"));
    }

    @Test
    public void apiRowWiringMissingShowsInstallAndLink() throws Exception {
        String src = findSource(PERMISSIONS);
        assertNotNull(src);
        assertTrue(src.contains("addApiRow()"));
        assertTrue(src.contains("BulkApiCheck.isScanReady(this)"));
        // kharab halat: sirf repair ki hidayat — install/link NAHI
        assertTrue(src.contains("BulkStrings.PERM_API_INSTALL"));
        assertFalse(src.contains("API_STORE_URL"));
        assertFalse(src.contains("openApiStorePage"));
        assertFalse(src.contains("f-droid.org"));
        // durust halat: sirf Done
        assertTrue(src.contains("BulkStrings.PERM_API_DONE"));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.PERM_API));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.PERM_API_INSTALL));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.PERM_API_DONE));
        assertEquals("Ye app ke andar hi hai — Termux Mod dobara kholo, scan files khud theek ho jayengi",
            BulkStrings.get(BulkStrings.PERM_API_INSTALL, true));
        assertEquals("Done", BulkStrings.get(BulkStrings.PERM_API_DONE, false));
        assertEquals("Done", BulkStrings.get(BulkStrings.PERM_API_DONE, true));
    }
}
