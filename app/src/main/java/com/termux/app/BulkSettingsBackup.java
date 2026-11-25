package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Termux Mod v144 — Sidebar Backup ka shared helper.
 *
 * Owner ki baat (2026-11-25): "Setting save ka option ho — me jo save
 * click kro tu phr he save ho — phr agly ko bhejo usky pas whi aye sb."
 *
 * Save: mojooda tamam settings usi file/format me jo Control Panel ka
 * Settings Backup istemal karta hai — Download/TermuxMod/
 * settings-backup.json (BulkPanelSettings.buildBackup/parseBackup hi
 * asal format hai; doosra format NAHI banaya gaya).
 *
 * Auto-load: fresh install par (is install par kabhi restore na hua
 * ho — KEY_RESTORED flag) agar backup file mojood ho to app start par
 * settings khud wapas ah jati hain. Isi se "APK delete karke dobara
 * lagao → save wali settings" or "dost ko APK + file bhejo → us ke
 * paas wohi settings" sach hota hai. Flag sirf asal restore/save par
 * set hota hai — file baad me aye to bhi agli start par restore ho
 * jayega.
 */
public final class BulkSettingsBackup {

    public static final String BACKUP_FILE_NAME = "settings-backup.json";

    /** Per-install flag (bulk_mockup prefs) — backup file ke ANDAR nahi jata. */
    public static final String KEY_RESTORED = "settings_backup_restored";

    public static final int LOAD_OK = 1;
    public static final int LOAD_NOT_FOUND = 0;
    public static final int LOAD_INVALID = -1;

    private BulkSettingsBackup() {}

    /** Control Panel wali hi file: Download/TermuxMod/settings-backup.json. */
    public static File backupFile() {
        return new File(BulkFileManager.EXPORT_ROOT, BACKUP_FILE_NAME);
    }

    // ------------------------------------------------------------ pure file IO

    /** Typed values -> backup JSON file. Safal ho to true. */
    public static boolean writeBackupFile(File f, Map<String, String> typedValues) {
        try {
            if (f == null) return false;
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            String json = BulkPanelSettings.buildBackup(typedValues);
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(json.getBytes("UTF-8"));
            }
            return f.isFile() && f.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Backup file -> typed values; file na ho ya format ghalat ho to null. */
    public static Map<String, String> readBackupFile(File f) {
        try {
            if (f == null || !f.isFile()) return null;
            byte[] bytes = new byte[(int) f.length()];
            try (InputStream in = new FileInputStream(f)) {
                int off = 0;
                int r;
                while (off < bytes.length
                    && (r = in.read(bytes, off, bytes.length - off)) > 0) {
                    off += r;
                }
            }
            return BulkPanelSettings.parseBackup(new String(bytes, "UTF-8"));
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------ save / load (Context)

    /** Mojooda settings backup file me save karo. */
    public static boolean save(Context c) {
        boolean ok = writeBackupFile(backupFile(), BulkPanelSettings.exportValues(c));
        // Apni hi save ki hui settings par auto-load dobara na chale.
        if (ok) markRestored(c);
        return ok;
    }

    /** Backup file se settings wapas lao. LOAD_* code return hota hai. */
    public static int load(Context c) {
        File f = backupFile();
        if (!f.isFile()) return LOAD_NOT_FOUND;
        Map<String, String> values = readBackupFile(f);
        if (values == null) return LOAD_INVALID;
        BulkPanelSettings.applyValues(c, values);
        markRestored(c);
        return LOAD_OK;
    }

    // ------------------------------------------------------------ auto-load gating

    /** Pure faisla: restore sirf tab jab flag unset ho AUR file mojood ho. */
    public static boolean shouldAutoRestore(boolean restoredFlag, boolean fileExists) {
        return !restoredFlag && fileExists;
    }

    public static boolean isRestored(Context c) {
        try {
            return prefs(c).getBoolean(KEY_RESTORED, false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void markRestored(Context c) {
        try {
            prefs(c).edit().putBoolean(KEY_RESTORED, true).apply();
        } catch (Exception ignored) {}
    }

    /**
     * App start par ek dafa: fresh install + backup file mojood ho to
     * settings khud wapas. Restore asal me hua ho tab hi true.
     */
    public static boolean autoRestoreIfNeeded(Context c) {
        try {
            if (!shouldAutoRestore(isRestored(c), backupFile().isFile())) return false;
            return load(c) == LOAD_OK;
        } catch (Exception e) {
            return false;
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(BulkPanelSettings.PREFS, Context.MODE_PRIVATE);
    }
}
