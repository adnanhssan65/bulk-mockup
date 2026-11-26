package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Termux Mod v149 test:
 *  - Item B: New Project Check (BulkProjectFixer) ki scan
 *    tabdeeli ka naya hadaf = restored W1 WAITING form
 *    (subprocess.run, timeout 5). Fire-and-forget Popen form ab
 *    FIX hota hai; waiting form = ALREADY_OK (byte-identical).
 *    Bundled zips ke asal runners par bhi scan item ALREADY_OK.
 *  - Item C: BulkMediaRegister ki nayi-files-only ordering —
 *    pehli pass = seed (purani files chhu'i nahi jatein), sirf
 *    seed ke BAAD anay wali files order hoti hain; ek hi query
 *    + media-type ke hisab se batched deletes ka plan (pure
 *    helpers); Order Fix switch (Item F) default ON, OFF =
 *    pass bilkul idle.
 *  - Item D: preview ka naya silsila — saved preview sab se
 *    pehle, phir live mockup / bundled, aakhir placeholder;
 *    kisi qadam ki decode nakami aglay par girti hai. ZIP-add
 *    template pick ki tartib.
 *  - Item G: run popup ke Background/Cancel button — completion
 *    faisla (pure) + process-tree kill tartib (pure) + wiring.
 *  - Item E: versionCode 149 + footer v149.
 */
public class BulkV149Test {

    private static final String FIXER =
        "app/src/main/java/com/termux/app/BulkProjectFixer.java";
    private static final String REGISTER =
        "app/src/main/java/com/termux/app/BulkMediaRegister.java";
    private static final String PREVIEW_SOURCE =
        "app/src/main/java/com/termux/app/BulkPreviewSource.java";
    private static final String PREVIEW_UI =
        "app/src/main/java/com/termux/app/BulkPreviewUi.java";
    private static final String INSTALLER =
        "app/src/main/java/com/termux/app/BulkProjectInstaller.java";
    private static final String MORE_SETTINGS =
        "app/src/main/java/com/termux/app/BulkMoreSettingsActivity.java";
    private static final String RUN_HELPER =
        "app/src/main/java/com/termux/app/BulkRunHelper.java";
    private static final String GRADLE = "app/build.gradle";
    private static final String DASH =
        "app/src/main/java/com/termux/app/BulkDashboardActivity.java";

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

    // ------------------------------------------------------- Item B samples

    /** v146 wala fire-and-forget Popen form (with-param family). */
    private static final String POPEN_RUNNER =
        "import os, subprocess\n"
        + "def safe_scan(path, timeout_sec=1):\n"
        + "    try:\n"
        + "        subprocess.Popen([\"termux-media-scan\", path],\n"
        + "            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)\n"
        + "    except Exception:\n"
        + "        pass\n"
        + "def main():\n"
        + "    safe_scan(\"/x/1.jpg\")\n";

    /** No-param Popen form (P2/P16 pattern). */
    private static final String POPEN_RUNNER_NOPARAM =
        "import os, subprocess\n"
        + "def safe_scan(path):\n"
        + "    try:\n"
        + "        subprocess.Popen([\"termux-media-scan\", path],\n"
        + "            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)\n"
        + "    except Exception:\n"
        + "        pass\n";

    /** Restored waiting form lekin default ghalat (3). */
    private static final String WAITING_WRONG_DEFAULT =
        "import os, subprocess\n"
        + "def safe_scan(path, timeout_sec=3):\n"
        + "    try:\n"
        + "        subprocess.run([\"termux-media-scan\", path],\n"
        + "            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,\n"
        + "            timeout=timeout_sec)\n"
        + "    except subprocess.TimeoutExpired:\n"
        + "        pass\n"
        + "    except Exception:\n"
        + "        pass\n";

    // ------------------------------------------------------------ Item B

    @Test
    public void popenFormIsConvertedToWaitingForm() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(POPEN_RUNNER, 9);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.scanTimeout);
        assertTrue(r.text.contains("def safe_scan(path, timeout_sec=5):"));
        assertTrue(r.text.contains(
            "subprocess.run([\"termux-media-scan\", path],"));
        assertTrue(r.text.contains("timeout=timeout_sec)"));
        assertTrue(r.text.contains("except subprocess.TimeoutExpired:"));
        assertFalse(r.text.contains(
            "Popen([\"termux-media-scan\""));
    }

    @Test
    public void convertedFormIsAlreadyOkOnSecondPass() {
        BulkProjectFixer.FixResult once = BulkProjectFixer.fixText(POPEN_RUNNER, 9);
        BulkProjectFixer.FixResult twice = BulkProjectFixer.fixText(once.text, 9);
        assertEquals(BulkProjectFixer.Status.ALREADY_OK, twice.report.scanTimeout);
        assertEquals(once.text, twice.text);
        assertFalse(twice.report.changed);
    }

    @Test
    public void noParamPopenFormGetsLiteralTimeout5() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(POPEN_RUNNER_NOPARAM, 2);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.scanTimeout);
        assertTrue(r.text.contains("def safe_scan(path):"));
        assertTrue(r.text.contains("timeout=5)"));
        assertFalse(r.text.contains("Popen([\"termux-media-scan\""));
    }

    @Test
    public void waitingFormWithWrongDefaultIsNormalizedTo5() {
        BulkProjectFixer.FixResult r = BulkProjectFixer.fixText(WAITING_WRONG_DEFAULT, 9);
        assertEquals(BulkProjectFixer.Status.FIXED, r.report.scanTimeout);
        assertTrue(r.text.contains("def safe_scan(path, timeout_sec=5):"));
        assertTrue(r.text.contains(
            "subprocess.run([\"termux-media-scan\", path],"));
    }

    @Test
    public void bundledRunnersScanItemIsAlreadyOk() throws Exception {
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
            BulkProjectFixer.FixResult r =
                BulkProjectFixer.fixText(runner, Integer.parseInt(c[2]));
            assertEquals(c[1] + " scan item",
                BulkProjectFixer.Status.ALREADY_OK, r.report.scanTimeout);
        }
    }

    @Test
    public void scanReportLabelIsOneByOne() {
        assertEquals("Scan (one-by-one)",
            BulkStrings.get(BulkStrings.CHECK_SCAN, false));
        assertEquals("Scan (ek ek karke)",
            BulkStrings.get(BulkStrings.CHECK_SCAN, true));
    }

    // ------------------------------------------------------------ Item C

    @Test
    public void orderFixDefaultIsOn() {
        // v151 (owner 2026-11-26): fresh-install default OFF me
        // badal gaya — saved value ki izzat waisi hi hai.
        assertFalse(BulkMediaRegister.resolveEnabled(null));
        assertTrue(BulkMediaRegister.resolveEnabled(Boolean.TRUE));
        assertFalse(BulkMediaRegister.resolveEnabled(Boolean.FALSE));
    }

    @Test
    public void seedCoversAllCurrentFiles() {
        List<String> paths = Arrays.asList(
            "/s/TermuxMod/Project 1/Mockups/1.jpg",
            "/s/TermuxMod/Project 1/Ready Wallpapers/2.jpg",
            "/s/TermuxMod/Project 1/Mockups/3.jpg");
        Map<String, String> sigs = new HashMap<>();
        sigs.put(paths.get(0), "10:100");
        sigs.put(paths.get(1), "20:200");
        // teesri file ka signature nahi — seed use skip karta hai
        java.util.Set<String> seed = BulkMediaRegister.seedKeys(paths, sigs);
        assertEquals(2, seed.size());
        // seed ke baad wohi files plan me "new" NAHI rehtin
        BulkMediaRegister.Plan plan =
            BulkMediaRegister.plan(paths, sigs, seed);
        assertTrue(plan.toRegister.isEmpty());
        assertEquals(2, plan.stillRegistered.size());
    }

    @Test
    public void onlyFilesAfterSeedAreNew() {
        String p1 = "/s/TermuxMod/Project 1/Mockups/1.jpg";
        String p2 = "/s/TermuxMod/Project 1/Ready Wallpapers/2.jpg";
        String p3 = "/s/TermuxMod/Project 1/Mockups/3.jpg";
        List<String> ordered = Arrays.asList(p1, p2, p3);
        Map<String, String> sigs = new HashMap<>();
        sigs.put(p1, "10:100");
        sigs.put(p2, "20:200");
        sigs.put(p3, "30:300");
        java.util.Set<String> seeded =
            BulkMediaRegister.seedKeys(Arrays.asList(p1, p2), sigs);
        BulkMediaRegister.Plan plan =
            BulkMediaRegister.plan(ordered, sigs, seeded);
        assertEquals(Arrays.asList(p3), plan.toRegister);
        assertEquals(2, plan.stillRegistered.size());
        // signature badal jaye to settled file bhi dobara new hai
        sigs.put(p2, "20:999");
        BulkMediaRegister.Plan plan2 =
            BulkMediaRegister.plan(ordered, sigs, seeded);
        assertEquals(Arrays.asList(p2, p3), plan2.toRegister);
    }

    @Test
    public void vanishedSeededFileIsPruned() {
        String p1 = "/s/TermuxMod/Project 1/Mockups/1.jpg";
        String p2 = "/s/TermuxMod/Project 1/Ready Wallpapers/2.jpg";
        Map<String, String> sigs = new HashMap<>();
        sigs.put(p1, "10:100");
        sigs.put(p2, "20:200");
        java.util.Set<String> seeded =
            BulkMediaRegister.seedKeys(Arrays.asList(p1, p2), sigs);
        String p2Key = BulkMediaRegister.seedKeys(
            Arrays.asList(p2), sigs).iterator().next();
        Map<String, String> nowSigs = new HashMap<>();
        nowSigs.put(p1, "10:100");
        BulkMediaRegister.Plan plan = BulkMediaRegister.plan(
            Arrays.asList(p1), nowSigs, seeded);
        assertTrue(plan.toRegister.isEmpty());
        assertEquals(1, plan.stillRegistered.size());
        assertFalse(plan.stillRegistered.contains(p2Key));
    }

    @Test
    public void mediaKindByExtension() {
        assertEquals("video", BulkMediaRegister.mediaKind("/a/b/12.mp4"));
        assertEquals("video", BulkMediaRegister.mediaKind("/a/b/12.MP4"));
        assertEquals("image", BulkMediaRegister.mediaKind("/a/b/12.jpg"));
        assertEquals("image", BulkMediaRegister.mediaKind("/a/b/12.png"));
    }

    @Test
    public void deletePlanGroupsOnlyExistingRowsByKind() {
        String i1 = "/s/P/Mockups/1.jpg";
        String v1 = "/s/P/Ready Wallpapers/2.mp4";
        String i2 = "/s/P/Mockups/3.jpg";
        List<String> toRegister = Arrays.asList(i1, v1, i2);
        // i2 ki koi MediaStore row nahi — us ka delete skip
        java.util.Set<String> rows = new HashSet<>(Arrays.asList(i1, v1));
        Map<String, List<String>> plan =
            BulkMediaRegister.deletePlan(toRegister, rows);
        assertEquals(Arrays.asList(i1), plan.get("image"));
        assertEquals(Arrays.asList(v1), plan.get("video"));
        assertEquals(2, plan.size());
    }

    @Test
    public void proceedAfterDeleteSkipsOnlyFailedKinds() {
        String i1 = "/s/P/Mockups/1.jpg";
        String v1 = "/s/P/Ready Wallpapers/2.mp4";
        String i2 = "/s/P/Mockups/3.jpg";
        List<String> toRegister = Arrays.asList(i1, v1, i2);
        java.util.Set<String> rows = new HashSet<>(Arrays.asList(i1, v1));
        // images ka batched delete fail — images rukti hain,
        // video (delete ok) or i2 (row hi nahi thi) aage barhte hain
        List<String> go = BulkMediaRegister.proceedAfterDelete(
            toRegister, rows, new HashSet<>(Arrays.asList("image")));
        assertEquals(Arrays.asList(v1, i2), go);
        // koi delete fail nahi — sab aage
        List<String> all = BulkMediaRegister.proceedAfterDelete(
            toRegister, rows, new HashSet<String>());
        assertEquals(toRegister, all);
    }

    @Test
    public void mediaRegisterWiringForV149() throws Exception {
        String src = findSource(REGISTER);
        assertNotNull(src);
        assertTrue(src.contains("order fix ho gaya"));
        assertTrue(src.contains("seeded149_"));
        // v152: gate ab teeno switches (Order Fix / Sequence
        // Fix / Har File Alag Second) ka hai — koi ek ON ho
        // tab hi pass chalta hai
        assertTrue(src.contains("if (!isOrderFixEnabled(ctx) && !isSequenceFixEnabled(ctx)"));
        assertTrue(src.contains("MediaStore.Files.getContentUri"));
        assertTrue(src.contains("ContentResolver"));
        assertTrue(src.contains("KEY_ENABLED = \"order_fix_enabled\""));
    }

    @Test
    public void orderFixRowOnMoreSettings() throws Exception {
        String src = findSource(MORE_SETTINGS);
        assertNotNull(src);
        assertTrue(src.contains("orderFixRow"));
        assertTrue(src.contains("BulkStrings.ORDER_FIX"));
        assertTrue(src.contains("BulkStrings.ORDER_FIX_SUB"));
        assertTrue(src.contains("Switch"));
        assertTrue(src.contains("BulkMediaRegister.setOrderFixEnabled"));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.ORDER_FIX));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.ORDER_FIX_SUB));
        assertEquals("Order Fix", BulkStrings.get(BulkStrings.ORDER_FIX, false));
    }

    // ------------------------------------------------------------ Item D

    private static BulkPreviewSource.Choice choice(int kind, String name) {
        return new BulkPreviewSource.Choice(kind, name);
    }

    @Test
    public void loadOrderSavedFirstThenLive() {
        List<Integer> withReal = BulkPreviewSource.loadOrder(true,
            choice(BulkPreviewSource.KIND_REAL, "9.jpg"));
        assertEquals(Arrays.asList(BulkPreviewSource.KIND_SAVED,
            BulkPreviewSource.KIND_REAL, BulkPreviewSource.KIND_PLACEHOLDER),
            withReal);
        List<Integer> withBundled = BulkPreviewSource.loadOrder(true,
            choice(BulkPreviewSource.KIND_BUNDLED, null));
        assertEquals(Arrays.asList(BulkPreviewSource.KIND_SAVED,
            BulkPreviewSource.KIND_BUNDLED, BulkPreviewSource.KIND_PLACEHOLDER),
            withBundled);
    }

    @Test
    public void loadOrderWithoutSavedMatchesOldChain() {
        assertEquals(Arrays.asList(BulkPreviewSource.KIND_REAL,
                BulkPreviewSource.KIND_PLACEHOLDER),
            BulkPreviewSource.loadOrder(false,
                choice(BulkPreviewSource.KIND_REAL, "9.jpg")));
        assertEquals(Arrays.asList(BulkPreviewSource.KIND_PLACEHOLDER),
            BulkPreviewSource.loadOrder(false,
                choice(BulkPreviewSource.KIND_PLACEHOLDER, null)));
        assertEquals(Arrays.asList(BulkPreviewSource.KIND_SAVED,
                BulkPreviewSource.KIND_PLACEHOLDER),
            BulkPreviewSource.loadOrder(true,
                choice(BulkPreviewSource.KIND_PLACEHOLDER, null)));
    }

    @Test
    public void templatePickRootBeatsAssetsAndFollowsNameOrder() {
        // root me template.png hai, assets me mockup.png (jo naam
        // tartib me pehle hai) — phir bhi ROOT jeet-ta hai
        assertEquals("template.png", BulkPreviewSource.pickTemplate(
            Arrays.asList("template.png", "other.jpg"),
            Arrays.asList("mockup.png")));
        // root ke andar naam tartib: mockup.png sab se pehle
        assertEquals("mockup.png", BulkPreviewSource.pickTemplate(
            Arrays.asList("template.jpg", "mockup.png"),
            new ArrayList<String>()));
        // root khali — assets fallback, assets/ prefix ke sath
        assertEquals("assets/template.png", BulkPreviewSource.pickTemplate(
            new ArrayList<String>(), Arrays.asList("x.png", "template.png")));
        // kuch nahi mila
        assertNull(BulkPreviewSource.pickTemplate(
            Arrays.asList("a.jpg"), Arrays.asList("b.jpg")));
        assertNull(BulkPreviewSource.pickTemplate(null, null));
    }

    @Test
    public void previewWiringForV149() throws Exception {
        String ui = findSource(PREVIEW_UI);
        assertNotNull(ui);
        assertTrue(ui.contains("bulk_previews"));
        assertTrue(ui.contains("ensureSavedPreviewFromTemplate"));
        assertTrue(ui.contains("loadOrder"));
        assertTrue(ui.contains("writeSavedPreview"));
        String installer = findSource(INSTALLER);
        assertNotNull(installer);
        assertTrue(installer.contains("ensureSavedPreviewFromTemplate"));
    }

    // ------------------------------------------------------------ Item G

    @Test
    public void completionKindDecisions() {
        assertEquals(0, BulkRunHelper.completionKind(true, 0));
        assertEquals(0, BulkRunHelper.completionKind(true, 1));
        assertEquals(1, BulkRunHelper.completionKind(false, 0));
        assertEquals(2, BulkRunHelper.completionKind(false, 1));
        assertEquals(2, BulkRunHelper.completionKind(false, -1));
    }

    @Test
    public void treeKillOrderDeepestFirst() {
        Map<Integer, Integer> ppid = new HashMap<>();
        ppid.put(2, 1);
        ppid.put(3, 1);
        ppid.put(4, 2);
        ppid.put(5, 4);
        ppid.put(9, 99); // ghair mutalliq — shamil nahi hoga
        List<Integer> order = BulkRunHelper.treeKillOrder(ppid, 1);
        assertEquals(4, order.size());
        assertEquals(Integer.valueOf(5), order.get(0));
        assertEquals(Integer.valueOf(4), order.get(1));
        assertEquals(new HashSet<>(Arrays.asList(2, 3)),
            new HashSet<>(order.subList(2, 4)));
        assertFalse(order.contains(1));
        assertFalse(order.contains(9));
        assertTrue(BulkRunHelper.treeKillOrder(ppid, 0).isEmpty());
        assertTrue(BulkRunHelper.treeKillOrder(null, 1).isEmpty());
    }

    @Test
    public void runPopupButtonsWiring() throws Exception {
        String src = findSource(RUN_HELPER);
        assertNotNull(src);
        assertTrue(src.contains("BulkStrings.RUN_BACKGROUND"));
        assertTrue(src.contains("BulkStrings.RUN_ALREADY_ACTIVE"));
        assertTrue(src.contains("BulkStrings.LOG_RUN_CANCEL"));
        assertTrue(src.contains("killProcessTree"));
        assertTrue(src.contains("runActions"));
        assertTrue(src.contains("if (sRunActive)"));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.RUN_BACKGROUND));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.RUN_CANCELLED));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.RUN_ALREADY_ACTIVE));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.LOG_RUN_CANCEL));
        assertEquals("Background me chalao",
            BulkStrings.get(BulkStrings.RUN_BACKGROUND, true));
    }

    // ------------------------------------------------------------ Item E

    @Test
    public void versionIs150() throws Exception {
        String gradle = findSource(GRADLE);
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 153"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v153"));
    }
}
