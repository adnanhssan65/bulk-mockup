package com.termux.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Termux Mod (v134) — output mirror ka PURE planner. Project ke
 * asal output folders (storage root par <N>_Mockups /
 * <N>_Ready_Wallpapers) ka aks TermuxMod/Project N ke andar
 * "Mockups" / "Ready Wallpapers" subfolders me rehta hai, taake
 * owner ko har project ke outputs drop folder ke andar bhi milen.
 *
 * Planner sirf naam -> signature (size:mtime) maps par faisla
 * karta hai: copy = source me naya ya badla hua; delete = mirror
 * me mojood lekin source se ghayab (true mirror — jama nahi
 * hota). Project side se is sync me KABHI kuch delete nahi hota.
 */
public final class BulkMirror {

    public static final String MOCKUPS_SUB = "Mockups";
    public static final String READY_SUB = "Ready Wallpapers";

    /** v154: gallery copies ka EK hi folder — jori (mockup + wallpaper)
     *  production tartib me isi me rehti hai. */
    public static final String PAIRS_SUB = "Pairs";
    /** v154: batch folders ka parent (har batch apna naam wala folder). */
    public static final String BATCHES_SUB = "Batches";

    private BulkMirror() {}

    public static final class Plan {
        public final List<String> toCopy;
        public final List<String> toDelete;

        Plan(List<String> toCopy, List<String> toDelete) {
            this.toCopy = toCopy;
            this.toDelete = toDelete;
        }

        public boolean isEmpty() {
            return toCopy.isEmpty() && toDelete.isEmpty();
        }
    }

    /** File ki pehchan: size + last-modified (copy ke baad mirror
     *  file par source ka hi mtime lagta hai, taake agli pass me
     *  wohi file dobara copy na ho). */
    public static String signature(long length, long lastModified) {
        return length + ":" + lastModified;
    }

    public static Plan planSync(Map<String, String> sourceSigs,
                                Map<String, String> mirrorSigs) {
        List<String> copy = new ArrayList<>();
        if (sourceSigs != null) {
            for (Map.Entry<String, String> e : sourceSigs.entrySet()) {
                String cur = mirrorSigs == null ? null : mirrorSigs.get(e.getKey());
                if (cur == null || !cur.equals(e.getValue())) copy.add(e.getKey());
            }
        }
        List<String> delete = new ArrayList<>();
        if (mirrorSigs != null) {
            for (String name : mirrorSigs.keySet()) {
                if (sourceSigs == null || !sourceSigs.containsKey(name)) delete.add(name);
            }
        }
        Collections.sort(copy);
        Collections.sort(delete);
        return new Plan(copy, delete);
    }
}
