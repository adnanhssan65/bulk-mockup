package com.termux.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** v141 — ek mirror copy step: kis subfolder ki kaunsi file. */
    public static final class CopyStep {
        public final boolean mockup;
        public final String name;

        CopyStep(boolean mockup, String name) {
            this.mockup = mockup;
            this.name = name;
        }
    }

    /**
     * v141 — dono subfolders ki MIXED copy tartib, jora-war
     * (pair-aware). Jora ki tareef Ready Assets Sequence tab wali
     * hi hai: NAAM tartib me nth mockup + nth ready. Joron ko
     * mockup ki mtime (jora sirf ready ka ho to ready ki mtime)
     * ke barhte hue hisab se lagaya jata hai — yani jis tartib
     * me outputs bane thay, usi me copy honge; caller har copy
     * ke baad foran media scan karta hai, is liye mirror copies
     * ka MediaStore DATE_ADDED bhi mockup -> wallpaper jora
     * rakhta hai (Threads jese pickers jo DATE_ADDED par sort
     * karte hain, wahan grouping nahi banti). Jo file copy list
     * me nahi wo step nahi banti; jora kabhi nahi tootta.
     */
    public static List<CopyStep> interleavedCopyOrder(
            List<String> mockNames, Map<String, Long> mockMtimes,
            List<String> readyNames, Map<String, Long> readyMtimes,
            Set<String> mockToCopy, Set<String> readyToCopy) {
        List<String> m = mockNames == null
            ? new ArrayList<>() : new ArrayList<>(mockNames);
        List<String> r = readyNames == null
            ? new ArrayList<>() : new ArrayList<>(readyNames);
        Collections.sort(m);
        Collections.sort(r);
        int pairs = Math.max(m.size(), r.size());
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < pairs; i++) order.add(i);
        order.sort((a, b) -> Long.compare(
            pairKey(a, m, mockMtimes, r, readyMtimes),
            pairKey(b, m, mockMtimes, r, readyMtimes)));
        List<CopyStep> out = new ArrayList<>();
        Set<String> emittedM = new HashSet<>();
        Set<String> emittedR = new HashSet<>();
        for (int i : order) {
            if (i < m.size() && mockToCopy != null
                    && mockToCopy.contains(m.get(i))) {
                out.add(new CopyStep(true, m.get(i)));
                emittedM.add(m.get(i));
            }
            if (i < r.size() && readyToCopy != null
                    && readyToCopy.contains(r.get(i))) {
                out.add(new CopyStep(false, r.get(i)));
                emittedR.add(r.get(i));
            }
        }
        // Defensive: copy list ki koi file jora-bandish se bahir
        // reh jaye (list/signature mismatch) to aakhir me naam
        // tartib me — koi copy chhootni nahi chahiye.
        if (mockToCopy != null) {
            List<String> left = new ArrayList<>(mockToCopy);
            left.removeAll(emittedM);
            Collections.sort(left);
            for (String name : left) out.add(new CopyStep(true, name));
        }
        if (readyToCopy != null) {
            List<String> left = new ArrayList<>(readyToCopy);
            left.removeAll(emittedR);
            Collections.sort(left);
            for (String name : left) out.add(new CopyStep(false, name));
        }
        return out;
    }

    /** Jora ki tartib wali chabi: mockup ki mtime, warna ready ki. */
    private static long pairKey(int i, List<String> m, Map<String, Long> mm,
                                List<String> r, Map<String, Long> rm) {
        if (i < m.size() && mm != null) {
            Long v = mm.get(m.get(i));
            if (v != null) return v;
        }
        if (i < r.size() && rm != null) {
            Long v = rm.get(r.get(i));
            if (v != null) return v;
        }
        return Long.MAX_VALUE;
    }
}
