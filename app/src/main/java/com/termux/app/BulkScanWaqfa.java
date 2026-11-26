package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Termux Mod v149 revision — Scan Waqfa setting (More Settings).
 *
 * Runner-side improvement: gallery copy ke per-file scan ke
 * baad 0.25s ka waqfa, taake MediaStore ka record sahi tartib
 * me lage or app ke Order Fix ki zaroorat kam se kam pare.
 * Runners ye halat khud nahi parh sakte (wo alag process me
 * chalte hain) — is liye app ek MARKER file rakhta hai:
 * TermuxMod/.scan_waqfa, content "on"/"off". Har runner run
 * ke shuru me marker sirf ek dafa parhta hai; marker na ho
 * ya content kuch aur ho to waqfa ON (default).
 *
 * Switch More Settings me hai (default ON). OFF karne par
 * terminal apni purani tez speed par chalta hai — owner ki
 * pasand par order fix phir app (Order Fix) karta hai.
 * Marker switch badalne par bhi likha jata hai or app start
 * par bhi (syncMarker), taake phone par maujooda runners ko
 * hamesha taaza halat mile. Marker likhna best-effort hai —
 * fail ho jaye to bhi kuch crash nahi hota; row phir bhi
 * saved pref hi dikhati hai.
 *
 * Is class ka faisla hissa (default/content) PURE hai.
 */
public final class BulkScanWaqfa {

    private static final String PREFS = "bulk_scan_waqfa";
    private static final String KEY_ENABLED = "scan_waqfa_enabled";

    /** Marker file ka naam (TermuxMod root me). */
    public static final String MARKER_NAME = ".scan_waqfa";

    private BulkScanWaqfa() {}

    // ------------------------------------------------------------ pure

    /**
     * Switch ka faisla (pure): saved value na ho (pehli dafa)
     * to ON — waqfa is version ka default hai.
     */
    public static boolean resolveEnabled(Boolean stored) {
        return stored == null || stored;
    }

    /** Marker file ka content (pure): ON → "on", OFF → "off". */
    public static String markerContent(boolean on) {
        return on ? "on" : "off";
    }

    /** Marker file (TermuxMod root ke andar). */
    public static File markerFile() {
        return new File(BulkDropFolders.rootDir(), MARKER_NAME);
    }

    // ------------------------------------------------------------ Android glue

    /** Scan Waqfa switch abhi ON hai? (default ON) */
    public static boolean isEnabled(Context ctx) {
        if (ctx == null) return true;
        try {
            SharedPreferences prefs =
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Boolean stored = prefs.contains(KEY_ENABLED)
                ? prefs.getBoolean(KEY_ENABLED, true) : null;
            return resolveEnabled(stored);
        } catch (Exception e) {
            return true;
        }
    }

    /** Switch save karo + marker foran taaza karo (More Settings row). */
    public static void setEnabled(Context ctx, boolean on) {
        if (ctx == null) return;
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, on).apply();
        } catch (Exception ignored) {}
        writeMarker(on);
    }

    /** App start par: marker ko saved pref ke mutabiq taaza karo. */
    public static void syncMarker(Context ctx) {
        if (ctx == null) return;
        writeMarker(isEnabled(ctx));
    }

    /**
     * Marker likho — best-effort: TermuxMod root na ho to sirf
     * root dir banane ki koshish (project folders kabhi nahi);
     * koi bhi fail khamoshi se nigal jata hai.
     */
    private static void writeMarker(boolean on) {
        try {
            File root = BulkDropFolders.rootDir();
            if (!root.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                root.mkdirs();
            }
            File marker = new File(root, MARKER_NAME);
            try (FileOutputStream out = new FileOutputStream(marker)) {
                out.write(markerContent(on).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }
}
