package com.termux.app;

import android.content.Context;

/**
 * Termux Mod v149 (Item H) — Termux:API app ki ASAL mojoodgi
 * ka check (Permissions screen ki row).
 *
 * Runners ka per-file scan termux-media-scan se hota hai, jo
 * Termux:API app ke baghair kaam hi nahi karta. Is liye
 * Permissions screen par is ki halat ASAL check se dikhti
 * hai: PackageManager se com.termux.api ki lookup — koi
 * cache nahi, koi andaza nahi; screen har dafa khulne par
 * (rebuild) fresh parha jata hai. Lookup fail ho (package
 * na mile, ya koi aur exception) to halat "nahi laga" hai —
 * crash kabhi nahi.
 *
 * Faisla (pure) alag hai taake unit test ho sake; Android
 * wala hissa sirf gulaab (glue) hai.
 */
public final class BulkApiCheck {

    /** Termux:API app ka package naam. */
    public static final String API_PACKAGE = "com.termux.api";

    /** Download page (F-Droid) — missing ho to row ke neeche link yahi khulta hai. */
    public static final String API_STORE_URL = "https://f-droid.org/packages/com.termux.api/";

    private BulkApiCheck() {}

    // ------------------------------------------------------------ pure

    /**
     * Faisla (pure): package lookup ka natija — sirf pakki
     * mojoodgi (Boolean.TRUE) hi "laga hua" hai; na-milna
     * (FALSE) ya lookup fail (null) dono "nahi laga" hain.
     */
    public static boolean resolveInstalled(Boolean packageFound) {
        return packageFound != null && packageFound;
    }

    // ------------------------------------------------------------ Android glue

    /**
     * ASAL check: PackageManager se com.termux.api ki mojoodgi.
     * Har call par fresh lookup; exception = nahi laga.
     */
    public static boolean isApiInstalled(Context ctx) {
        if (ctx == null) return false;
        Boolean found;
        try {
            found = ctx.getPackageManager().getPackageInfo(API_PACKAGE, 0) != null;
        } catch (Exception e) {
            found = null;
        }
        return resolveInstalled(found);
    }
}
