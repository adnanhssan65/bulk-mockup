package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Termux Mod (v134) — Recreate ki auto/manual setting.
 *
 * Owner ki shikayat: jo drop folder / preview video wo delete
 * karta tha, app khulte hi wapas ban jata tha. Ab usool:
 *   - DEFAULT = MANUAL (dono toggles OFF).
 *   - Manual: Dashboard / Add / Projects khulne par missing drop
 *     folders NAHI bante or missing preview videos copy NAHI
 *     hotin. Sirf Permissions -> Recreate Folders dabane se
 *     (on-demand) sab dobara banta hai.
 *   - Auto (toggle ON): usi hisse ka v133 wala rawaiya wapas —
 *     folders ke liye folders, videos ke liye videos, alag alag.
 * Faisla (gate) PURE hai — unit tests me hai; ye class sirf
 * persisted values parhti/likhti hai or gate ko expose karti hai.
 */
public final class BulkRecreateSettings {

    private static final String PREFS = "bulk_mockup";
    public static final String KEY_FOLDERS_AUTO = "recreate_folders_auto";
    public static final String KEY_VIDEOS_AUTO = "recreate_videos_auto";

    /** Factory defaults: dono MANUAL. */
    public static final boolean DEFAULT_FOLDERS_AUTO = false;
    public static final boolean DEFAULT_VIDEOS_AUTO = false;

    private BulkRecreateSettings() {}

    // ------------------------------------------------------------ pure gate

    /** Drop folders khud-ba-khud banein? Sirf auto ON ho to. */
    public static boolean mayCreateFolders(boolean foldersAuto) {
        return foldersAuto;
    }

    /** Preview videos khud-ba-khud copy hon? Sirf auto ON ho to. */
    public static boolean mayCopyVideos(boolean videosAuto) {
        return videosAuto;
    }

    // ------------------------------------------------------------ persisted

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean foldersAuto(Context c) {
        try {
            return prefs(c).getBoolean(KEY_FOLDERS_AUTO, DEFAULT_FOLDERS_AUTO);
        } catch (Exception e) {
            return DEFAULT_FOLDERS_AUTO;
        }
    }

    public static boolean videosAuto(Context c) {
        try {
            return prefs(c).getBoolean(KEY_VIDEOS_AUTO, DEFAULT_VIDEOS_AUTO);
        } catch (Exception e) {
            return DEFAULT_VIDEOS_AUTO;
        }
    }

    public static void setFoldersAuto(Context c, boolean on) {
        try {
            prefs(c).edit().putBoolean(KEY_FOLDERS_AUTO, on).apply();
        } catch (Exception ignored) {}
    }

    public static void setVideosAuto(Context c, boolean on) {
        try {
            prefs(c).edit().putBoolean(KEY_VIDEOS_AUTO, on).apply();
        } catch (Exception ignored) {}
    }
}
