package com.termux.app;

/**
 * Termux Mod (v134) — project folders ki WAPSI (move-back).
 *
 * v136: RETIRED. Move-back pass (TermuxMod -> storage root) ab
 * kabhi nahi chalta — koi caller baqi nahi. Owner ka final faisla
 * ulta hai: project folders shared storage par KAHIN nahi rehte;
 * engine BulkHomeMigration se $HOME ke andar chala gaya hai.
 * Sirf PURE faisla {@link #decide(boolean, boolean, boolean)} reh
 * gaya hai (tests + tareekhi record ke liye).
 */
public final class BulkMigrationBack {

    private BulkMigrationBack() {}

    // ------------------------------------------------------------ pure

    /** Ek project ke liye faisla. */
    public enum Decision {
        /** TermuxMod -> root move karo. */
        MOVE,
        /** Root par pehle se folder hai — copy wahin rahe, kabhi merge nahi. */
        STAY_CONFLICT,
        /** Runner purana/hardcoded hai — copy wahin rahe, wese hi chalegi. */
        STAY_UNSAFE,
        /** TermuxMod me kuch hai hi nahi — karne ko kuch nahi. */
        NOTHING
    }

    /**
     * Faisla (pure): TermuxMod wali copy ho, root par na ho, or
     * runner safe ho — tabhi MOVE. Root par pehle se ho to
     * STAY_CONFLICT (safety check se pehle — conflict hamesha jeet-ta
     * hai, kyun ke merge/overwrite har haal me mana hai).
     */
    public static Decision decide(boolean inModExists, boolean rootExists,
                                  boolean runnerSafe) {
        if (!inModExists) return Decision.NOTHING;
        if (rootExists) return Decision.STAY_CONFLICT;
        if (!runnerSafe) return Decision.STAY_UNSAFE;
        return Decision.MOVE;
    }

    /** Ek pass ka natija (tareekhi — v136 me istemal nahi hota). */
    public static final class Result {
        public int moved;
        public int stayed;
        public int evaluated; // moved + stayed (jin par asal faisla hua)
    }
}
