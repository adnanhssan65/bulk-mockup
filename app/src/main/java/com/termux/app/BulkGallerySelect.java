package com.termux.app;

import java.util.HashSet;
import java.util.Set;

/**
 * Termux Mod (v127) — gallery drag-select ki pure logic.
 * Long-press se drag shuru hota hai (anchor), ungli jis position tak
 * jaye wahan tak ka poora range select ho jata hai (Google Photos
 * style). Drag ke doran selection = drag se pehle wali selection
 * (base) ∪ range(anchor, current) — har move par dobara hisaab, taake
 * peeche drag karne se range shrink bhi ho.
 */
public final class BulkGallerySelect {

    private BulkGallerySelect() {}

    /** anchor aur current ke darmiyan tamam positions (dono shaamil), kisi bhi direction me. */
    public static Set<Integer> range(int anchor, int current) {
        Set<Integer> out = new HashSet<>();
        if (anchor < 0 || current < 0) return out;
        int lo = Math.min(anchor, current);
        int hi = Math.max(anchor, current);
        for (int i = lo; i <= hi; i++) out.add(i);
        return out;
    }

    /** Drag ke doran ki selection: base ∪ range(anchor, current). */
    public static Set<Integer> dragSelection(Set<Integer> base, int anchor, int current) {
        Set<Integer> out = new HashSet<>();
        if (base != null) out.addAll(base);
        out.addAll(range(anchor, current));
        return out;
    }
}
