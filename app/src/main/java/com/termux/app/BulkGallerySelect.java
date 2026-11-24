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

    // ------------------------------------------------------------ v132 paint + lock

    /**
     * v132 — PAINT (swipe) drag ka target state: drag jis cell se
     * shuru hua, us ki TOGGLED halat (selected tha to paint ab
     * UNSELECT karega, unselected tha to SELECT). LOCK on ho to
     * removal kabhi nahi hota — paint hamesha select hi karta hai
     * (owner ka rule: lock selection ko khud unselect hone se rokta
     * hai; unselect ka wahid rasta Clear button hai).
     */
    public static boolean paintTargetState(boolean anchorSelected, boolean locked) {
        if (locked) return true;
        return !anchorSelected;
    }

    /**
     * v132 — Paint drag ka result: base selection par range(anchor,
     * current) ke tamam positions ko targetState par set karo. Har
     * move par base se dobara hisaab hota hai — ungli peeche jaye to
     * range ke bahar wale cells apni base halat par wapas aa jate
     * hain. Base set mutate NAHI hota.
     */
    public static Set<Integer> paintSelection(Set<Integer> base, int anchor, int current,
                                              boolean targetState) {
        Set<Integer> out = new HashSet<>();
        if (base != null) out.addAll(base);
        for (int p : range(anchor, current)) {
            if (targetState) out.add(p);
            else out.remove(p);
        }
        return out;
    }

    /**
     * v132 — Ek tap ke baad ki halat, LOCK ke sath: lock on ho or
     * cell pehle se selected ho to koi tabdeeli nahi (selected hi
     * rehta hai); warna aam toggle.
     *
     * @return tap ke baad cell selected hai ya nahi
     */
    public static boolean toggledState(boolean selected, boolean locked) {
        if (locked && selected) return true;
        return !selected;
    }

    /** v132 — Clear: selection hamesha poori khaali — lock ho ya na ho (yehi unselect ka asal rasta hai). */
    public static Set<Integer> clearedSelection() {
        return new HashSet<>();
    }
}
