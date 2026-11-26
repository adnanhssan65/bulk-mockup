package com.termux.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Termux Mod (v132) — Project ke FRAME PREVIEW (DP) ka source faisla
 * + project search filter. Bilkul pure Java (Android dependency nahi)
 * — unit tests isi liye chal sakte hain; asal file/asset parhna
 * BulkPreviewUi karta hai.
 *
 * SOURCE RULE (owner ka approved design — Add Wallpapers → Select
 * Project, or Projects screen ki DP, dono par yahi):
 *   1) Project ke <N>_Mockups folder me koi image ho to SAB SE NAYI
 *      asal mockup file (lastModified sab se zyada; tie par naam ka
 *      bara harf wala — faisla hamesha tay shuda).
 *   2) Warna bundled projects (1–18, 20, 21) ke liye app ke andar
 *      wala preview asset: assets/bulk/previews/p<N>.jpg (ye asal
 *      runner outputs se bane hain).
 *   3) Warna PLACEHOLDER — neutral tile project number ke sath.
 *
 * v149 — PERSISTENT PREVIEW: previews bar bar ghayab ho jati
 * theen (asal mockup delete hote hi DP bhi chali jati thi) or
 * naye ZIP projects ki preview pehle run se pehle banti hi
 * nahi thi. Ab loadBitmap ki tartib ye hai (loadOrder):
 *   1) SAVED — app ke andar save shuda preview (bulk_previews/
 *      p<N>.jpg), jo asal mockup istemal hone par khud ban jati
 *      hai or ZIP-add par project ke template se bhi.
 *   2) REAL — sab se nayi asal mockup (upar wala rule 1); ye
 *      istemal ho to saved preview (dobara) likhi jati hai.
 *   3) BUNDLED — andar wala asset (upar wala rule 2).
 *   4) PLACEHOLDER.
 * Kisi step par decode fail ho to faisla wahi nahi rukta —
 * agli source par girta hai.
 */
public final class BulkPreviewSource {

    public static final int KIND_REAL = 0;
    public static final int KIND_BUNDLED = 1;
    public static final int KIND_PLACEHOLDER = 2;
    /** v149: app ke andar save shuda persistent preview. */
    public static final int KIND_SAVED = 3;

    /** Sab se bara bundled project number (v147: 20/21 bhi bundled — 19 kabhi tha hi nahi). */
    public static final int LAST_BUNDLED_PROJECT = 21;

    private BulkPreviewSource() {}

    /** Ek mockup file ki maloomat (naam + lastModified millis). */
    public static final class Entry {
        public final String name;
        public final long lastModified;

        public Entry(String name, long lastModified) {
            this.name = name == null ? "" : name;
            this.lastModified = lastModified;
        }
    }

    /** Faisla: konsa source + (REAL ho to) konsi file. */
    public static final class Choice {
        public final int kind;
        public final String fileName; // sirf KIND_REAL par; warna null

        Choice(int kind, String fileName) {
            this.kind = kind;
            this.fileName = fileName;
        }
    }

    /** Bundled preview ka asset path, masalan "bulk/previews/p10.jpg". */
    public static String bundledAssetPath(int projectNumber) {
        return "bulk/previews/p" + projectNumber + ".jpg";
    }

    /**
     * Ye number bundled project hai? Bundled set = 1..18, 20, 21
     * (v139: 15–18; v147: 20/21 — Project 19 kabhi bana hi nahi,
     * is liye saada range nahi, asal set hi check hota hai).
     */
    public static boolean isBundledProject(int projectNumber) {
        return (projectNumber >= 1 && projectNumber <= 18)
            || projectNumber == 20 || projectNumber == 21;
    }

    /** Is number ke liye bundled preview mojood ho sakta hai? (bundled set hi) */
    public static boolean hasBundledRange(int projectNumber) {
        return isBundledProject(projectNumber);
    }

    /**
     * Preview source ka faisla.
     *
     * @param mockupFiles     <N>_Mockups folder ki files (null = khaali)
     * @param projectNumber   project ka number (<= 0 = maloom nahi)
     * @param bundledAvailable caller ne check kiya ke bundled asset
     *                         asal me khul sakta hai
     */
    public static Choice choose(List<Entry> mockupFiles, int projectNumber,
                                boolean bundledAvailable) {
        Entry best = null;
        if (mockupFiles != null) {
            for (Entry e : mockupFiles) {
                if (e == null) continue;
                // Sirf asal images — hidden (dot) files or video kabhi
                // preview nahi bante (counts wala hi usool, v129)
                if (!BulkProjects.isCountableWallpaper(e.name)) continue;
                if (best == null
                    || e.lastModified > best.lastModified
                    || (e.lastModified == best.lastModified
                        && e.name.compareTo(best.name) > 0)) {
                    best = e;
                }
            }
        }
        if (best != null) return new Choice(KIND_REAL, best.name);
        if (projectNumber > 0 && bundledAvailable && hasBundledRange(projectNumber)) {
            return new Choice(KIND_BUNDLED, null);
        }
        return new Choice(KIND_PLACEHOLDER, null);
    }

    // ------------------------------------------------------------ v149: load order + template

    /**
     * loadBitmap ki candidate tartib (pure): SAVED (agar file
     * hai) → REAL ya BUNDLED (choose() ke faisle se) →
     * PLACEHOLDER hamesha aakhir me (caller us par null lata
     * hai). Decode fail par caller is list ki agli kind par
     * girta hai — koi step dead-end nahi.
     */
    public static List<Integer> loadOrder(boolean savedExists, Choice choice) {
        List<Integer> out = new ArrayList<>();
        if (savedExists) out.add(KIND_SAVED);
        if (choice != null && choice.kind == KIND_REAL) out.add(KIND_REAL);
        else if (choice != null && choice.kind == KIND_BUNDLED) out.add(KIND_BUNDLED);
        out.add(KIND_PLACEHOLDER);
        return out;
    }

    /** ZIP-add par template dhoondhne ki naam tartib (pehle root, phir assets/). */
    public static final String[] TEMPLATE_NAMES = {
        "mockup.png", "mockup.jpg", "template.jpg", "template.png"
    };

    /**
     * Project folder se preview ke liye template file chuno
     * (pure): pehle project root me TEMPLATE_NAMES ki tartib
     * se, phir assets/ subfolder me usi tartib se. Mile to
     * relative path ("mockup.png" ya "assets/template.jpg"),
     * warna null.
     */
    public static String pickTemplate(Collection<String> rootNames,
                                      Collection<String> assetsNames) {
        for (String name : TEMPLATE_NAMES) {
            if (rootNames != null && rootNames.contains(name)) return name;
        }
        for (String name : TEMPLATE_NAMES) {
            if (assetsNames != null && assetsNames.contains(name)) {
                return "assets/" + name;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ search filter

    /**
     * Project search ka filter (Add-flow Select Project + Projects
     * screen, dono par yahi): query khaali ho to sab match. Warna
     * query (chhoti, trim shuda) project ke NAAM me ya NUMBER ki
     * string me mile to match — "10" Project 10 ko, "proj" sab ko,
     * "project 1" naam se match karta hai.
     */
    public static boolean matchesQuery(String projectName, int projectNumber, String query) {
        if (query == null) return true;
        String q = query.trim().toLowerCase(Locale.US);
        if (q.isEmpty()) return true;
        String name = projectName == null ? "" : projectName.toLowerCase(Locale.US);
        if (name.contains(q)) return true;
        if (projectNumber > 0 && String.valueOf(projectNumber).contains(q)) return true;
        return false;
    }
}
