package com.termux.app;

import android.content.Context;

/**
 * Termux Mod (v134) — "Scroll Button" (scroll-jump handle) ki
 * persisted setting. Gallery gear popup ke Sort/Grid sections ke
 * sath hi is ki Show/Hide radio rehti hai; setting USI prefs file
 * (bulk_gallery) me rehti hai jahan sort/grid ki hain, taake har
 * screen wohi ek value parhe.
 *
 * DEFAULT = Show. Hide ho to handle kisi bhi surface par kabhi
 * nahi dikhta.
 */
public final class BulkScrollSettings {

    public static final String PREFS_NAME = "bulk_gallery";
    public static final String KEY_SHOW = "scroll_button_show";
    public static final boolean DEFAULT_SHOW = true;

    private BulkScrollSettings() {}

    /** Pure gate: persisted value hi faisla hai. */
    public static boolean mayShow(boolean prefValue) {
        return prefValue;
    }

    public static boolean isShown(Context context) {
        try {
            return mayShow(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW, DEFAULT_SHOW));
        } catch (Exception e) {
            return DEFAULT_SHOW;
        }
    }

    public static void setShown(Context context, boolean show) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SHOW, show).apply();
        } catch (Exception ignored) {}
    }
}
