package com.termux.app;

/**
 * Termux Mod v157 — Pre-run delete GATE ka khalis (pure)
 * faisla hissa.
 *
 * Owner ka phone-masla (v156 par sabit): Add Wallpapers
 * ke Move ke baad wo project select karke Run kar deta
 * tha — run foran shuru ho jata, pending originals ka
 * delete kabhi hota hi nahi tha (4 items Pending me
 * atke rahe, koi popup kabhi nahi aya; aakhir me use
 * Pending → Delete Now se hath se saaf karna para).
 * Owner ka hukm: delete ka step Run se PEHLE aye —
 * "pehle ye popup aye, delete kry phr run kry agr
 * move wla hy tu".
 *
 * Gate ka usool:
 *  - Pending list khaali → run foran, koi deri, koi UI.
 *  - Pending hai → pehle wahi v156 delete flow (Move
 *    Delete mode ke order se) foreground me mukammal,
 *    PHIR run. Host BulkStageActivity hai — wahi asal
 *    flow rakhti hai or system consent ka result bhi
 *    wahi leti hai (BulkPendingUi ke Delete Now wala
 *    hi pakka rasta).
 *  - Delete kamyab ho, user popup CANCEL kare ya kuch
 *    items fail hon — run har surat me aage barhta
 *    hai; reh gaye items Pending me rehte hain (badge /
 *    Pending row emergency rasta barqarar). Gate ek
 *    run par sirf EK dafa lagta hai — alreadyGated par
 *    hamesha RUN_NOW, warna cancel-wale items par gate
 *    loop ban jata.
 *  - Gate sirf Run ("process") par hai — Batch or
 *    Delete dispatch par kabhi nahi.
 */
public final class BulkRunGate {

    /** Run foran shuru karo — gate ki zaroorat nahi / gate ho chuka. */
    public static final int RUN_NOW = 0;
    /** Pehle pending deletes mukammal karo, phir run. */
    public static final int DELETE_FIRST = 1;

    private BulkRunGate() {}

    /**
     * Gate ka faisla (pure): pending khaali (ya ginti
     * ghalat) → RUN_NOW; pending hai or ye is run ki
     * pehli koshish hai → DELETE_FIRST; gate ke baad
     * (alreadyGated) hamesha RUN_NOW — cancel/fail par
     * reh gaye items run ko dobara nahi rokte.
     */
    public static int decide(int pendingCount, boolean alreadyGated) {
        if (alreadyGated) return RUN_NOW;
        return pendingCount > 0 ? DELETE_FIRST : RUN_NOW;
    }

    /** Ye action gate ke andar hai? (pure) — sirf Run. */
    public static boolean gatesAction(String action) {
        return "process".equals(action);
    }

    /** Gate lagne ki BulkLog line (pure) — 90-char cap ke andar. */
    public static String gateLogLine(int pendingCount) {
        return "Run gate: " + pendingCount
            + " pending deletes — pehle delete, phir run";
    }

    /**
     * Gate khatam hone ki BulkLog line (pure): run ab
     * shuru hota hai; jo pending baqi hai wo sirf
     * cancel/fail wale hain — Pending me mehfooz.
     */
    public static String gateDoneLine(int remainingPending) {
        return "Run gate khatam: run shuru (pending baqi: "
            + remainingPending + ")";
    }
}
