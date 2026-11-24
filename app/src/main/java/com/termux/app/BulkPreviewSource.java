package com.termux.app;

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
 *   2) Warna bundled projects (1–14) ke liye app ke andar wala
 *      preview asset: assets/bulk/previews/p<N>.jpg (ye asal runner
 *      outputs se bane hain).
 *   3) Warna PLACEHOLDER — neutral tile project number ke sath.
 */
public final class BulkPreviewSource {

    public static final int KIND_REAL = 0;
    public static final int KIND_BUNDLED = 1;
    public static final int KIND_PLACEHOLDER = 2;

    /** Bundled previews sirf projects 1..14 ke liye hain. */
    public static final int LAST_BUNDLED_PROJECT = 14;

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

    /** Is number ke liye bundled preview mojood ho sakta hai? (1..14) */
    public static boolean hasBundledRange(int projectNumber) {
        return projectNumber >= 1 && projectNumber <= LAST_BUNDLED_PROJECT;
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
