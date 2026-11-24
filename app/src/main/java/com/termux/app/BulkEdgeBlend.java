package com.termux.app;

/**
 * Termux Mod (v133) — Edge Blend ki PURE logic (unit tests ke liye).
 *
 * Owner ka approved design (pv_edge_d5): app ka background status bar
 * aur navigation bar ke peeche tak jata hai (edge-to-edge), aur SIRF
 * un bar zones ke andar ek soft mist (dhund) ka gradient hota hai —
 * kinare par sab se gehra, andar ki taraf bilkul khatam. Koi hard
 * line / seam kahin nahi; andar ka UI bilkul nahi badalta.
 *
 * Mist ka rang:
 *   light themes -> safed (white), jesa owner ki spec me tha
 *     (#FFFFFFF5 se transparent wali dhund);
 *   dark themes  -> theme ke apne bg rang ki dhund, taake bar ke
 *     icons parhne ke qabil rahen aur design apne rang me ghule.
 *
 * Gradient 5 stops ka hai — alpha fractions smoothstep jesi ghulti
 * hui tartib me (kinara -> andar): 1.00, 0.78, 0.50, 0.24, 0.00.
 * Stops EDGE se INNER END ki taraf hain; drawable ki orientation
 * (TOP_BOTTOM / BOTTOM_TOP) tay karti hai ke edge kis taraf hai.
 */
public final class BulkEdgeBlend {

    /** Kinare par mist ki zyada se zyada alpha (242/255 — halki si
     *  frosted jhalak ke sath takreeban opaque). */
    public static final int EDGE_ALPHA = 242;

    /** Alpha fractions, edge (index 0) se inner end (aakhri) tak. */
    public static final float[] ALPHA_FRACTIONS = {1f, 0.78f, 0.50f, 0.24f, 0f};

    /** Polish: gradient bare inset se thora sa zyada lamba hai taake
     *  fade aur naram ho (alpha is hisse me boht kam rehta hai). */
    public static final int TOP_EXTENSION_DP = 6;
    public static final int BOTTOM_EXTENSION_DP = 10;

    private BulkEdgeBlend() {}

    /** Mist ka RGB (alpha ke baghair): light = safed, dark = theme bg. */
    public static int mistRgb(BulkPalettes.Palette p) {
        if (p == null) return 0xFFFFFF;
        return p.light ? 0xFFFFFF : (p.bg & 0x00FFFFFF);
    }

    /**
     * Gradient ke ARGB stops, EDGE se INNER END ki taraf.
     * Pehle stop ki alpha = EDGE_ALPHA, aakhri ki = 0 (bilkul
     * transparent), beech wali sakht ghulti hui (monotonic kam).
     */
    public static int[] edgeStops(BulkPalettes.Palette p) {
        int rgb = mistRgb(p);
        int[] out = new int[ALPHA_FRACTIONS.length];
        for (int i = 0; i < out.length; i++) {
            int a = Math.round(EDGE_ALPHA * ALPHA_FRACTIONS[i]);
            if (a < 0) a = 0;
            if (a > 255) a = 255;
            out[i] = (a << 24) | rgb;
        }
        return out;
    }

    /** Uper wale mist zone ki kul unchai (px): status inset + extension. */
    public static int topZonePx(int insetPx, int extensionPx) {
        return Math.max(0, insetPx) + Math.max(0, extensionPx);
    }

    /** Neeche wale mist zone ki kul unchai (px): nav inset + extension. */
    public static int bottomZonePx(int insetPx, int extensionPx) {
        return Math.max(0, insetPx) + Math.max(0, extensionPx);
    }
}
