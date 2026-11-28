package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Termux Mod v156 — Move Delete setting (More Settings).
 *
 * Add Wallpapers ke Move me copy verify hue originals kis
 * order se delete hon, is ka faisla — sirf DO choices
 * (owner ka hukm: "Pop zrorat hy ya ni option rkh dena
 * phr sy update na krwani pry"):
 *
 * AUTO (default): pehle MediaStore delete ki EK koshish —
 * file or gallery ka record ek sath, usi waqt khatam
 * (v130 wala asal faida, popup ke baghair). Na ho to
 * seedha File.delete + reh gayi entry ki safai, or jo
 * file phir bhi na hate sirf us ke liye aakhir me EK
 * batch popup. Har step har file par EK hi dafa — koi
 * retry loop nahi, move kabhi atak nahi sakta.
 *
 * POPUP: pre-v133 wala rawaiya — seedha purana rasta
 * (API 30+ par poore batch ka EK system popup).
 *
 * Halat sirf SharedPreferences me hai — ye app-side
 * setting hai, runners isay kabhi nahi parhte, is liye
 * koi marker file nahi.
 *
 * Is class ka faisla hissa (mode resolve, step routing,
 * summary line) PURE hai.
 */
public final class BulkMoveDelete {

    private static final String PREFS = "bulk_move_delete";
    private static final String KEY_MODE = "move_delete_mode";

    public static final String MODE_AUTO = "auto";
    public static final String MODE_POPUP = "popup";

    // MediaStore delete (step 2) ke nateejay
    public static final int MS_OK = 0;
    public static final int MS_ZERO = 1;
    public static final int MS_SECURITY = 2;
    public static final int MS_ERROR = 3;

    // Step 2 ke baad ka rasta
    public static final int ROUTE_DONE = 0;
    public static final int ROUTE_FALLBACK = 1;
    public static final int ROUTE_FALLBACK_BLOCK = 2;

    private BulkMoveDelete() {}

    // ------------------------------------------------------------ pure

    /**
     * Mode ka faisla (pure): sirf "popup" asal dusri value
     * hai — baqi har cheez (null, khaali, ghalat) AUTO hai,
     * jo is version ka default hai.
     */
    public static String resolveMode(String stored) {
        if (MODE_POPUP.equals(stored)) {
            return MODE_POPUP;
        }
        return MODE_AUTO;
    }

    /** Popup mode hai? (stored/resolved dono chalega) */
    public static boolean isPopup(String mode) {
        return MODE_POPUP.equals(resolveMode(mode));
    }

    /**
     * Step 2 (MediaStore delete) ki koshish karni hai ya
     * nahi (pure): pehli SecurityException ke baad baqi
     * files seedha fallback par jati hain — har file par
     * wahi exception dobara nahi uthaya jata.
     */
    public static boolean shouldTryMediaStore(boolean mediaStoreBlocked) {
        return !mediaStoreBlocked;
    }

    /**
     * Step 2 ke nateeje ka rasta (pure): OK = ho gaya;
     * SECURITY = fallback + aage ke liye step 2 band;
     * baqi (ZERO / ERROR / anjaan) = sirf is file ka
     * fallback. Har surat me step 2 dobara NAHI chalta.
     */
    public static int routeForMsResult(int msResult) {
        if (msResult == MS_OK) return ROUTE_DONE;
        if (msResult == MS_SECURITY) return ROUTE_FALLBACK_BLOCK;
        return ROUTE_FALLBACK;
    }

    /**
     * Ek delete run ki saboot line (pure) — BulkLog ke
     * 90-char cap ke andar rakhi gayi hai.
     */
    public static String runSummary(String mode, int viaMs, int viaDirect,
                                    int viaPopup, int failed) {
        return "Move delete (" + resolveMode(mode) + "): store " + viaMs
            + ", direct " + viaDirect + ", popup " + viaPopup
            + ", fail " + failed;
    }

    // ------------------------------------------------------------ prefs

    /** Mojooda mode (hamesha resolved — auto ya popup). */
    public static String currentMode(Context ctx) {
        try {
            SharedPreferences sp =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return resolveMode(sp.getString(KEY_MODE, null));
        } catch (Exception e) {
            return MODE_AUTO;
        }
    }

    /** Mode save karo (resolved value hi save hoti hai). */
    public static void setMode(Context ctx, String mode) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_MODE, resolveMode(mode)).apply();
        } catch (Exception ignored) {}
    }
}
