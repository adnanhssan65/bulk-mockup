package com.termux.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Termux Mod (v130) — Gallery (All Images) toolbar ki pure logic.
 *
 * Owner ke apne HTML design (All_Images_Gallery_Manager) ke mutabiq:
 *   - Sort button ka cycle: Newest -> Oldest -> Name -> Newest
 *   - Grid button ka cycle: ek line me 2 -> 3 -> 4 -> 5 -> 6 -> 2 images
 *   - Copy / Move mode (Move = copy verify hone ke baad gallery ka
 *     original delete — asal delete BulkStageActivity commit par karti
 *     hai; yahan sirf mode ki halat hai)
 *
 * Ye class bilkul pure Java hai (Android dependency nahi) — unit tests
 * isi liye chal sakte hain.
 */
public final class BulkGalleryState {

    public static final int SORT_NEWEST = 0;
    public static final int SORT_OLDEST = 1;
    public static final int SORT_NAME = 2;

    public static final int MODE_COPY = 0;
    public static final int MODE_MOVE = 1;
    /** Koi preset mode nahi — Add par popup se poochho (Manual). */
    public static final int MODE_NONE = -1;

    // v132 redesign: Add ka Copy/Move ikhtiyar — persisted setting
    public static final int CHOICE_MANUAL = 0;     // har dafa popup
    public static final int CHOICE_AUTO_COPY = 1;  // hamesha Copy
    public static final int CHOICE_AUTO_MOVE = 2;  // hamesha Move

    /** Setting se asal mode: Auto par preset mode, Manual par MODE_NONE (poochho). */
    public static int resolveMode(int choice) {
        if (choice == CHOICE_AUTO_COPY) return MODE_COPY;
        if (choice == CHOICE_AUTO_MOVE) return MODE_MOVE;
        return MODE_NONE;
    }

    /** Prefs se aya hua choice mehfooz range me rakho.
     *  Ghalat value par factory default (Auto–Move). */
    public static int clampChoice(int choice) {
        return (choice == CHOICE_MANUAL || choice == CHOICE_AUTO_COPY
            || choice == CHOICE_AUTO_MOVE) ? choice : CHOICE_AUTO_MOVE;
    }

    public static final int MIN_COLUMNS = 2;
    public static final int MAX_COLUMNS = 6;

    private BulkGalleryState() {}

    /** Gallery ki ek image — MediaStore se (id, display name, date added seconds). */
    public static final class Entry {
        public final long id;
        public final String name;
        public final long dateAdded;

        public Entry(long id, String name, long dateAdded) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.dateAdded = dateAdded;
        }
    }

    /** Sort button ka cycle: Newest -> Oldest -> Name -> Newest. Ghalat value = Newest se agla (Oldest nahi, seedha cycle mehfooz). */
    public static int nextSort(int mode) {
        switch (mode) {
            case SORT_NEWEST: return SORT_OLDEST;
            case SORT_OLDEST: return SORT_NAME;
            default: return SORT_NEWEST;
        }
    }

    /** Grid button ka cycle: 2 -> 3 -> 4 -> 5 -> 6 -> 2. */
    public static int nextColumns(int cols) {
        int c = clampColumns(cols);
        return c >= MAX_COLUMNS ? MIN_COLUMNS : c + 1;
    }

    /** Columns hamesha 2..6 — is ke ilawa jo bhi ho, 2 par wapas. */
    public static int clampColumns(int cols) {
        return (cols >= MIN_COLUMNS && cols <= MAX_COLUMNS) ? cols : MIN_COLUMNS;
    }

    /** Sort ki hui NAYI list wapas karo (asal list nahi badalti). Ties par id ka faisla — halat hamesha tay shuda. */
    public static List<Entry> sorted(List<Entry> entries, int mode) {
        List<Entry> out = new ArrayList<>();
        if (entries != null) out.addAll(entries);
        Comparator<Entry> cmp;
        switch (mode) {
            case SORT_OLDEST:
                cmp = (a, b) -> {
                    int d = Long.compare(a.dateAdded, b.dateAdded);
                    return d != 0 ? d : Long.compare(a.id, b.id);
                };
                break;
            case SORT_NAME:
                cmp = (a, b) -> {
                    int d = a.name.toLowerCase().compareTo(b.name.toLowerCase());
                    return d != 0 ? d : Long.compare(a.id, b.id);
                };
                break;
            case SORT_NEWEST:
            default:
                cmp = (a, b) -> {
                    int d = Long.compare(b.dateAdded, a.dateAdded);
                    return d != 0 ? d : Long.compare(b.id, a.id);
                };
                break;
        }
        Collections.sort(out, cmp);
        return out;
    }

    /**
     * v131 (Defect 2 fix): gallery ab MediaStore se SIRF ids load karti
     * hai (v129 wala proven query — {_ID}, DATE_ADDED DESC, yani load
     * order = Newest pehle). Display order yahan banta hai:
     *   Newest = load order jesa hai; Oldest = usi ka ulta;
     *   Name = names map se (naam na mile to khaali naam — pehle ata
     *   hai; tie par bara id pehle, yani nayi image pehle).
     * Asal list NAHI badalti; nayi list wapas hoti hai.
     */
    public static List<Long> orderIds(List<Long> loadOrder, int mode,
                                      java.util.Map<Long, String> names) {
        List<Long> out = new ArrayList<>();
        if (loadOrder != null) out.addAll(loadOrder);
        switch (mode) {
            case SORT_OLDEST:
                Collections.reverse(out);
                break;
            case SORT_NAME:
                Collections.sort(out, (a, b) -> {
                    String na = names != null && names.get(a) != null ? names.get(a) : "";
                    String nb = names != null && names.get(b) != null ? names.get(b) : "";
                    int d = na.toLowerCase().compareTo(nb.toLowerCase());
                    return d != 0 ? d : Long.compare(b, a);
                });
                break;
            case SORT_NEWEST:
            default:
                break; // load order hi Newest hai
        }
        return out;
    }

    /** "Select All" kab "Unselect All" banta hai: list khaali na ho or sab select hon. */
    public static boolean isAllSelected(int selectedCount, int totalCount) {
        return totalCount > 0 && selectedCount == totalCount;
    }

    /**
     * File list par wohi gallery wala sort (v130, Preview surfaces ke
     * liye): Newest/Oldest = lastModified, Name = file name. NAYI list
     * wapas hoti hai; asal list nahi badalti.
     */
    public static List<java.io.File> sortFiles(List<java.io.File> files, int mode) {
        List<java.io.File> out = new ArrayList<>();
        if (files == null || files.isEmpty()) return out;
        List<Entry> es = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            java.io.File f = files.get(i);
            es.add(new Entry(i, f.getName(), f.lastModified() / 1000L));
        }
        for (Entry e : sorted(es, mode)) out.add(files.get((int) e.id));
        return out;
    }
}
