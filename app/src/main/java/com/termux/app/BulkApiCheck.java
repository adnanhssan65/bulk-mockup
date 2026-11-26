package com.termux.app;

import android.content.Context;

/**
 * Termux Mod v150 — Permissions screen ki scan row ka check.
 *
 * v149 me ye row Termux:API app ki mojoodgi (PackageManager)
 * parhti thi or missing par use install karne ki hidayat
 * deti thi. Wo dawa ab GHALAT hai: scan app ke APNE built-in
 * server (BulkApiServer) or $PREFIX/bin ke wrapper scripts
 * se chalta hai — Termux:API app ki zaroorat hi nahi. Balke
 * use install karwana ulta nuqsan tha (pkg install kabhi
 * hamari wrapper files ko official walon se badal deta hai).
 *
 * Is liye ab ye check HAMARI apni wrapper files ki asal
 * halat parhta hai: teeno files (termux-media-scan /
 * termux-wake-lock / termux-wake-unlock) mojood hon or un
 * ka content generated script se hu-ba-hu milta ho →
 * "Done". Kharab hon to user ko kuch nahi karna — app agle
 * start par unhe khud theek kar deti hai
 * (BulkMockupSetup.ensureBinWrappers ka content check).
 *
 * Faisla (pure) alag hai taake unit test ho sake; Android
 * wala hissa sirf gulaab (glue) hai.
 */
public final class BulkApiCheck {

    private BulkApiCheck() {}

    // ------------------------------------------------------------ pure

    /**
     * Faisla (pure): wrapper content check ka natija — sirf
     * pakka match (Boolean.TRUE) hi "durust" hai; mismatch
     * (FALSE) ya check fail (null) dono "durust nahi" hain.
     */
    public static boolean resolveOk(Boolean contentMatches) {
        return contentMatches != null && contentMatches;
    }

    // ------------------------------------------------------------ Android glue

    /**
     * ASAL check: teeno wrapper files ki mojoodgi + content
     * match (BulkMockupSetup.wrappersOk). Har call par fresh
     * parha jata hai; exception = durust nahi.
     */
    public static boolean isScanReady(Context ctx) {
        if (ctx == null) return false;
        Boolean ok;
        try {
            ok = BulkMockupSetup.wrappersOk(ctx);
        } catch (Exception e) {
            ok = null;
        }
        return resolveOk(ok);
    }
}
