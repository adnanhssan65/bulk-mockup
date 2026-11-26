package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod v148 — gallery registration APP ke andar se.
 *
 * Wajah: Mod owner ki apni key se signed hai, is liye official
 * Termux:API app Mod ki termux-media-scan calls serve nahi
 * karti — sirf-Mod walay phone par outputs TermuxMod/Project N
 * me to hain lekin MediaStore/gallery/TikTok me nahi atay.
 * v148 me mirror sync ke baad app khud MediaScannerConnection
 * se register karti hai (BulkMediaRegister), pair tartib me,
 * signature-skip ke sath.
 *
 * Ye tests PURE planning hisse ke hain (number tartib +
 * signature faisla) — asal MediaScanner ka rawaiya Android
 * ka hai, wo sirf phone par verify ho sakta hai.
 */
public class BulkV148Test {

    private static final String M = "/storage/emulated/0/TermuxMod/Project 1/Mockups/";
    private static final String R = "/storage/emulated/0/TermuxMod/Project 1/Ready Wallpapers/";

    // ------------------------------------------------------------ numeric stem

    @Test
    public void numericStemCases() {
        assertEquals(1, BulkMediaRegister.numericStem("1.jpg"));
        assertEquals(12, BulkMediaRegister.numericStem("12.png"));
        assertEquals(3, BulkMediaRegister.numericStem("3.mp4"));
        assertEquals(7, BulkMediaRegister.numericStem("7"));
        assertEquals(-1, BulkMediaRegister.numericStem("abc.jpg"));
        assertEquals(-1, BulkMediaRegister.numericStem(".5.jpg")); // hidden
        assertEquals(-1, BulkMediaRegister.numericStem(""));
        assertEquals(-1, BulkMediaRegister.numericStem(null));
        assertEquals(-1, BulkMediaRegister.numericStem("_temp_2.jpg"));
    }

    // ------------------------------------------------------------ media filter

    @Test
    public void registerableMediaCases() {
        assertTrue(BulkMediaRegister.isRegisterableMedia("1.jpg"));
        assertTrue(BulkMediaRegister.isRegisterableMedia("2.JPG"));
        assertTrue(BulkMediaRegister.isRegisterableMedia("3.png"));
        assertTrue(BulkMediaRegister.isRegisterableMedia("4.mp4"));
        assertFalse(BulkMediaRegister.isRegisterableMedia("5.webp")); // scope se bahar
        assertFalse(BulkMediaRegister.isRegisterableMedia("6.gif"));
        assertFalse(BulkMediaRegister.isRegisterableMedia(".7.jpg")); // hidden
        assertFalse(BulkMediaRegister.isRegisterableMedia("notes.txt"));
        assertFalse(BulkMediaRegister.isRegisterableMedia(null));
    }

    // ------------------------------------------------------------ tartib (order)

    @Test
    public void pairOrderAcrossFolders() {
        // P1 pattern: mockups tek (1,3,5), ready jor (2,4,6) —
        // mili hui list pair tartib me hi ani chahiye
        List<String> in = Arrays.asList(
            R + "6.jpg", M + "5.jpg", R + "2.jpg", M + "1.jpg", R + "4.jpg", M + "3.jpg");
        assertEquals(Arrays.asList(
            M + "1.jpg", R + "2.jpg", M + "3.jpg", R + "4.jpg", M + "5.jpg", R + "6.jpg"),
            BulkMediaRegister.orderByProduction(in));
    }

    @Test
    public void tripleOrderP16Pattern() {
        // P16: har set me 1 mockup + 2 ready (1,2,3 / 4,5,6)
        List<String> in = Arrays.asList(
            R + "3.jpg", R + "6.jpg", M + "4.jpg", R + "2.jpg", M + "1.jpg", R + "5.jpg");
        assertEquals(Arrays.asList(
            M + "1.jpg", R + "2.jpg", R + "3.jpg", M + "4.jpg", R + "5.jpg", R + "6.jpg"),
            BulkMediaRegister.orderByProduction(in));
    }

    @Test
    public void groupOrderP10Pattern() {
        // P10: group mockup (1, 5) + us ke ready (2,3,4 / 6,7,8)
        List<String> in = Arrays.asList(
            R + "8.jpg", M + "5.jpg", R + "3.jpg", R + "7.jpg",
            M + "1.jpg", R + "2.jpg", R + "6.jpg", R + "4.jpg");
        assertEquals(Arrays.asList(
            M + "1.jpg", R + "2.jpg", R + "3.jpg", R + "4.jpg",
            M + "5.jpg", R + "6.jpg", R + "7.jpg", R + "8.jpg"),
            BulkMediaRegister.orderByProduction(in));
    }

    @Test
    public void numericNotLexicographic() {
        // 9 se pehle 10 nahi ana chahiye (naam ki alphabetic
        // tartib "10" ko "9" se pehle le ati hai — number wali
        // tartib hi production tartib hai)
        List<String> in = Arrays.asList(R + "10.jpg", M + "9.jpg", R + "2.jpg");
        assertEquals(Arrays.asList(R + "2.jpg", M + "9.jpg", R + "10.jpg"),
            BulkMediaRegister.orderByProduction(in));
    }

    @Test
    public void tieBreaksMockupsFirst() {
        // Ek hi number dono folders me ho (nadir) to Mockups pehle
        List<String> in = Arrays.asList(R + "2.jpg", M + "2.jpg");
        assertEquals(Arrays.asList(M + "2.jpg", R + "2.jpg"),
            BulkMediaRegister.orderByProduction(in));
    }

    @Test
    public void orderSafetyOnEmptyAndNull() {
        assertTrue(BulkMediaRegister.orderByProduction(new ArrayList<>()).isEmpty());
        assertTrue(BulkMediaRegister.orderByProduction(null).isEmpty());
        // Input list mutate nahi hoti
        List<String> in = new ArrayList<>(Arrays.asList(R + "2.jpg", M + "1.jpg"));
        BulkMediaRegister.orderByProduction(in);
        assertEquals(Arrays.asList(R + "2.jpg", M + "1.jpg"), in);
    }

    // ------------------------------------------------------------ signature faisla (plan)

    private static Map<String, String> sigs(String... pathSigPairs) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < pathSigPairs.length; i += 2) {
            out.put(pathSigPairs[i], pathSigPairs[i + 1]);
        }
        return out;
    }

    @Test
    public void planSkipsAlreadyRegistered() {
        List<String> ordered = Arrays.asList(M + "1.jpg", R + "2.jpg");
        Map<String, String> sigs = sigs(M + "1.jpg", "100:111", R + "2.jpg", "200:222");
        Set<String> registered = new HashSet<>(Arrays.asList(
            BulkMediaRegister.entryKey(M + "1.jpg", "100:111"),
            BulkMediaRegister.entryKey(R + "2.jpg", "200:222")));
        BulkMediaRegister.Plan plan = BulkMediaRegister.plan(ordered, sigs, registered);
        assertTrue(plan.toRegister.isEmpty());
        assertEquals(registered, plan.stillRegistered);
    }

    @Test
    public void planRegistersOnlyNewFiles() {
        List<String> ordered = Arrays.asList(M + "1.jpg", R + "2.jpg", M + "3.jpg");
        Map<String, String> sigs = sigs(
            M + "1.jpg", "100:111", R + "2.jpg", "200:222", M + "3.jpg", "300:333");
        Set<String> registered = new HashSet<>(Arrays.asList(
            BulkMediaRegister.entryKey(M + "1.jpg", "100:111"),
            BulkMediaRegister.entryKey(R + "2.jpg", "200:222")));
        BulkMediaRegister.Plan plan = BulkMediaRegister.plan(ordered, sigs, registered);
        assertEquals(Collections.singletonList(M + "3.jpg"), plan.toRegister);
        assertEquals(registered, plan.stillRegistered);
    }

    @Test
    public void planReregistersChangedFile() {
        // Wohi naam, lekin size/mtime badal gaya (mirror ne nayi
        // copy rakhi) → dobara register; purana record still me nahi
        List<String> ordered = Collections.singletonList(M + "1.jpg");
        Map<String, String> sigs = sigs(M + "1.jpg", "150:999");
        Set<String> registered = new HashSet<>(Collections.singletonList(
            BulkMediaRegister.entryKey(M + "1.jpg", "100:111")));
        BulkMediaRegister.Plan plan = BulkMediaRegister.plan(ordered, sigs, registered);
        assertEquals(ordered, plan.toRegister);
        assertTrue(plan.stillRegistered.isEmpty());
    }

    @Test
    public void planPrunesVanishedFiles() {
        // Register shuda file ab mojood hi nahi → us ka record
        // stillRegistered me nahi ana chahiye (save par prune)
        List<String> ordered = Collections.singletonList(M + "1.jpg");
        Map<String, String> sigs = sigs(M + "1.jpg", "100:111");
        Set<String> registered = new HashSet<>(Arrays.asList(
            BulkMediaRegister.entryKey(M + "1.jpg", "100:111"),
            BulkMediaRegister.entryKey(R + "8.jpg", "800:888"))); // ghayab
        BulkMediaRegister.Plan plan = BulkMediaRegister.plan(ordered, sigs, registered);
        assertTrue(plan.toRegister.isEmpty());
        assertEquals(new HashSet<>(Collections.singletonList(
            BulkMediaRegister.entryKey(M + "1.jpg", "100:111"))), plan.stillRegistered);
    }

    @Test
    public void planFirstRunRegistersEverythingInOrder() {
        // Upgrade ke baad pehli pass: backlog poora, pair tartib me
        List<String> ordered = Arrays.asList(
            M + "1.jpg", R + "2.jpg", M + "3.jpg", R + "4.jpg");
        Map<String, String> sigs = sigs(
            M + "1.jpg", "1:1", R + "2.jpg", "2:2", M + "3.jpg", "3:3", R + "4.jpg", "4:4");
        BulkMediaRegister.Plan plan =
            BulkMediaRegister.plan(ordered, sigs, new HashSet<>());
        assertEquals(ordered, plan.toRegister);
        assertTrue(plan.stillRegistered.isEmpty());
    }

    @Test
    public void planSafetyOnEmptyInputs() {
        BulkMediaRegister.Plan empty = BulkMediaRegister.plan(
            new ArrayList<>(), new HashMap<>(), new HashSet<>());
        assertTrue(empty.toRegister.isEmpty());
        assertTrue(empty.stillRegistered.isEmpty());
        BulkMediaRegister.Plan nullSafe = BulkMediaRegister.plan(null, null, null);
        assertTrue(nullSafe.toRegister.isEmpty());
        assertTrue(nullSafe.stillRegistered.isEmpty());
    }

    // ------------------------------------------------------------ wiring + version

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
    public void registerHookedAfterMirrorSync() throws Exception {
        String drop = findSource("app/src/main/java/com/termux/app/BulkDropFolders.java");
        assertNotNull("BulkDropFolders source milna chahiye", drop);
        assertTrue(drop.contains("BulkMediaRegister.registerProject(ctx, n)"));
        // Hook mirror ke DONO syncOneMirror calls ke BAAD hai
        int hook = drop.indexOf("BulkMediaRegister.registerProject");
        int lastMirror = drop.lastIndexOf("syncOneMirror(ctx, n");
        assertTrue("hook mirror sync ke baad hona chahiye", hook > lastMirror);
    }

    @Test
    public void versionBumpedTo150() throws Exception {
        String dash = findSource("app/src/main/java/com/termux/app/BulkDashboardActivity.java");
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v153"));
        String gradle = findSource("app/build.gradle");
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 153"));
    }
}
