package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Termux Mod v155 — Time Mode setting (More Settings).
 *
 * Mockup files ki mtime ladder kis zamane me lage, is ka
 * faisla: PAST (v154 wala proven default — ladder abhi se
 * peeche, tracker ke baad), CURRENT (tracker baghair, files
 * abhi tak barhti hui 1s step par) ya FUTURE (purana pre-v154
 * tareeqa — max(abhi, tracker) se +60s/file aage).
 *
 * Runners alag process me chalte hain, is liye halat MARKER
 * file se jati hai: TermuxMod/.time_mode, content "past" /
 * "current" / "future". Har runner run ke shuru me marker
 * ek dafa parhta hai; marker na ho ya content kuch aur ho
 * to PAST (default). Setting badalte hi marker taaza hota
 * hai or app start par bhi (syncMarker — Dashboard), taake
 * phone par maujooda runners ko hamesha taaza halat mile.
 * Marker likhna best-effort hai — fail ho to crash nahi;
 * row phir bhi saved pref hi dikhati hai.
 *
 * Sath me Time Reset: shared tracker file
 * (/storage/emulated/0/.mockup_time_tracker.txt) delete —
 * agli ladder bilkul fresh shuru hoti hai.
 *
 * Is class ka faisla hissa (default/content) PURE hai.
 */
public final class BulkTimeMode {

    private static final String PREFS = "bulk_time_mode";
    private static final String KEY_MODE = "time_mode";

    public static final String MODE_PAST = "past";
    public static final String MODE_CURRENT = "current";
    public static final String MODE_FUTURE = "future";

    /** Marker file ka naam (TermuxMod root me). */
    public static final String MARKER_NAME = ".time_mode";

    /** Shared tracker file ka naam (storage root me — runners wali hi). */
    public static final String TRACKER_NAME = ".mockup_time_tracker.txt";

    private BulkTimeMode() {}

    // ------------------------------------------------------------ pure

    /**
     * Mode ka faisla (pure): sirf teen asal values qabool
     * hain — baqi har cheez (null, khaali, ghalat) PAST hai,
     * jo is version ka default hai.
     */
    public static String resolveMode(String stored) {
        if (MODE_CURRENT.equals(stored) || MODE_FUTURE.equals(stored)) {
            return stored;
        }
        return MODE_PAST;
    }

    /** Marker file ka content (pure): hamesha resolved mode. */
    public static String markerContent(String mode) {
        return resolveMode(mode);
    }

    /** Marker file (TermuxMod root ke andar). */
    public static File markerFile() {
        return new File(BulkDropFolders.rootDir(), MARKER_NAME);
    }

    /** Shared tracker file (storage root — runners bhi yahi parhte/likhte hain). */
    public static File trackerFile() {
        return new File(BulkProjects.STORAGE_ROOT, TRACKER_NAME);
    }

    // ------------------------------------------------------------ Android glue

    /** Abhi wala mode (saved pref; pehli dafa PAST). */
    public static String currentMode(Context ctx) {
        if (ctx == null) return MODE_PAST;
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return resolveMode(prefs.getString(KEY_MODE, null));
        } catch (Exception e) {
            return MODE_PAST;
        }
    }

    /** Mode save karo + marker foran taaza karo (More Settings popup). */
    public static void setMode(Context ctx, String mode) {
        if (ctx == null) return;
        String resolved = resolveMode(mode);
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_MODE, resolved).apply();
        } catch (Exception ignored) {}
        writeMarker(resolved);
    }

    /** App start par: marker ko saved pref ke mutabiq taaza karo. */
    public static void syncMarker(Context ctx) {
        if (ctx == null) return;
        writeMarker(currentMode(ctx));
    }

    /**
     * Time Reset: shared tracker file delete kar do. File pehle
     * se na ho to bhi reset hi hai (halat wahi: tracker ghayab).
     * @return reset ke baad tracker file ghayab hai ya nahi
     */
    public static boolean resetTracker() {
        try {
            File tracker = trackerFile();
            if (tracker.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tracker.delete();
            }
            return !tracker.exists();
        } catch (Exception e) {
            return false;
        }
    }

    /** Marker likho — best-effort (Scan Waqfa wali hi surat). */
    private static void writeMarker(String mode) {
        try {
            File root = BulkDropFolders.rootDir();
            if (!root.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                root.mkdirs();
            }
            File marker = new File(root, MARKER_NAME);
            try (FileOutputStream out = new FileOutputStream(marker)) {
                out.write(markerContent(mode).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }
}
