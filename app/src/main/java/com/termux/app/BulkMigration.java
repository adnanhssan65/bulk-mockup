package com.termux.app;

/**
 * Termux Mod (v133) — project folders ki SAFE shift (relocation).
 *
 * v136: RETIRED. Is class ka migration pass (storage root ->
 * TermuxMod/mockup_project_<N>) ab kabhi nahi chalta — koi caller
 * baqi nahi. v136 me engine BulkHomeMigration se $HOME ke andar
 * jata hai or shared storage par mockup_project_<N> bilkul nahi
 * rehta. Sirf PURE helper {@link #isRelocationSafe(String)} reh
 * gaya hai (tests + tareekhi record ke liye).
 */
public final class BulkMigration {

    private BulkMigration() {}

    // ------------------------------------------------------------ pure

    /**
     * Runner ka text relocation-safe hai? Sirf tab jab folder ki
     * jagah hardcoded na ho: text me "MOCKUP_BASE" (env pattern)
     * ya "__file__" (script-relative) mojood ho.
     */
    public static boolean isRelocationSafe(String runnerText) {
        if (runnerText == null) return false;
        return runnerText.contains("MOCKUP_BASE") || runnerText.contains("__file__");
    }

    /** Ek pass ka natija (tareekhi — v136 me istemal nahi hota). */
    public static final class Result {
        public int moved;
        public int stayed;
        public int evaluated; // moved + stayed (jin par asal faisla hua)
    }
}
