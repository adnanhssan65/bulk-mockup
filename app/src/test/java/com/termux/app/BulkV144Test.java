package com.termux.app;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * v144 — Sidebar Backup: settings save/load ka file round-trip,
 * fresh-install auto-load ki gating (flag unset + file mojood),
 * or sidebar row/popup ki wiring (source scan).
 */
public class BulkV144Test {

    // ------------------------------------------------------------ file round-trip

    @Test
    public void backupFileRoundTripsThroughDisk() throws Exception {
        File dir = Files.createTempDirectory("bulkbk").toFile();
        File f = new File(dir, "settings-backup.json");
        Map<String, String> values = BulkPanelSettings.factoryDefaults();
        values.put("bulk_mockup|panel_app_name", "s:Adnan Ka App \"Pro\"");
        values.put("bulk_mockup|panel_contact", "s:+44 7400 000000");

        assertTrue(BulkSettingsBackup.writeBackupFile(f, values));
        assertTrue(f.isFile() && f.length() > 0);

        Map<String, String> back = BulkSettingsBackup.readBackupFile(f);
        assertEquals(values, back);
    }

    @Test
    public void writtenBackupUsesControlPanelFormat() throws Exception {
        // Wahi kind/version jo Control Panel ka Settings Backup likhta
        // hai — sidebar wali file CP import se bhi parhi ja sakti hai.
        File dir = Files.createTempDirectory("bulkbk").toFile();
        File f = new File(dir, "settings-backup.json");
        assertTrue(BulkSettingsBackup.writeBackupFile(
            f, BulkPanelSettings.factoryDefaults()));
        String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"kind\":\"" + BulkPanelSettings.BACKUP_KIND + "\""));
        assertEquals(BulkPanelSettings.factoryDefaults(),
            BulkPanelSettings.parseBackup(json));
    }

    @Test
    public void readBackupFileRejectsMissingAndGarbage() throws Exception {
        File dir = Files.createTempDirectory("bulkbk").toFile();
        assertNull(BulkSettingsBackup.readBackupFile(new File(dir, "nope.json")));
        assertNull(BulkSettingsBackup.readBackupFile(null));
        File bad = new File(dir, "bad.json");
        Files.write(bad.toPath(), "not json at all".getBytes(StandardCharsets.UTF_8));
        assertNull(BulkSettingsBackup.readBackupFile(bad));
        File foreign = new File(dir, "foreign.json");
        Files.write(foreign.toPath(),
            "{\"kind\":\"other\",\"version\":\"1\"}".getBytes(StandardCharsets.UTF_8));
        assertNull(BulkSettingsBackup.readBackupFile(foreign));
    }

    @Test
    public void backupFileIsTheControlPanelFile() {
        // Ek hi file — do formats/makaam bilkul nahi.
        assertEquals(new File(BulkFileManager.EXPORT_ROOT, "settings-backup.json"),
            BulkSettingsBackup.backupFile());
        assertEquals("settings-backup.json", BulkSettingsBackup.BACKUP_FILE_NAME);
    }

    // ------------------------------------------------------------ auto-load gating

    @Test
    public void autoRestoreOnlyWhenFlagUnsetAndFileExists() {
        assertTrue(BulkSettingsBackup.shouldAutoRestore(false, true));
        assertFalse(BulkSettingsBackup.shouldAutoRestore(true, true));
        assertFalse(BulkSettingsBackup.shouldAutoRestore(false, false));
        assertFalse(BulkSettingsBackup.shouldAutoRestore(true, false));
    }

    @Test
    public void restoredFlagStaysOutOfBackupRegistry() {
        // Flag per-install hai — backup file ke andar safar nahi karti,
        // warna dost ki fresh install par auto-load kabhi na chale.
        assertNull(BulkPanelSettings.entryFor(
            BulkPanelSettings.PREFS + "|" + BulkSettingsBackup.KEY_RESTORED));
        assertFalse(BulkPanelSettings.factoryDefaults().containsKey(
            BulkPanelSettings.PREFS + "|" + BulkSettingsBackup.KEY_RESTORED));
    }

    // ------------------------------------------------------------ strings

    @Test
    public void backupStringsExistInBothLanguages() {
        String[] keys = {
            BulkStrings.BACKUP, BulkStrings.BACKUP_SAVE, BulkStrings.BACKUP_LOAD,
            BulkStrings.BACKUP_SAVED, BulkStrings.BACKUP_LOADED,
            BulkStrings.BACKUP_SAVE_FAILED
        };
        for (String k : keys) {
            assertTrue("dono zubanen: " + k, BulkStrings.hasBothLanguages(k));
            assertFalse(BulkStrings.get(k, false).trim().isEmpty());
            assertFalse(BulkStrings.get(k, true).trim().isEmpty());
        }
        assertEquals("Backup", BulkStrings.get(BulkStrings.BACKUP, false));
        assertEquals("Settings Wapas Lao (Load)", BulkStrings.get(BulkStrings.BACKUP_LOAD, true));
    }

    // ------------------------------------------------------------ wiring (source scan)

    private static String dashboardSource() throws Exception {
        String rel = "app/src/main/java/com/termux/app/BulkDashboardActivity.java";
        // cwd se upar chalte hue dono layouts try karo (repo root /
        // workspace root) — harness jis jagah se bhi chale, source mile.
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 8 && dir != null; i++) {
            File a = new File(dir, rel);
            if (a.isFile()) {
                return new String(Files.readAllBytes(a.toPath()), StandardCharsets.UTF_8);
            }
            File b = new File(dir, "bulk_mockup_fork/termux-app/" + rel);
            if (b.isFile()) {
                return new String(Files.readAllBytes(b.toPath()), StandardCharsets.UTF_8);
            }
            File c = new File(dir, "workspace/bulk_mockup_fork/termux-app/" + rel);
            if (c.isFile()) {
                return new String(Files.readAllBytes(c.toPath()), StandardCharsets.UTF_8);
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    @Test
    public void sidebarBackupRowAndPopupAreWired() throws Exception {
        String src = dashboardSource();
        assertTrue("BulkDashboardActivity source milna chahiye", src != null);
        // Sidebar ki chhoti row
        assertTrue(src.contains("drawerRow(BulkStrings.t(this, BulkStrings.BACKUP)"));
        // Popup + dono actions helper ke zariye
        assertTrue(src.contains("showBackupPopup()"));
        assertTrue(src.contains("BulkSettingsBackup.save(this)"));
        assertTrue(src.contains("BulkSettingsBackup.load(this)"));
        assertTrue(src.contains("BulkStrings.BACKUP_SAVE"));
        assertTrue(src.contains("BulkStrings.BACKUP_LOAD"));
    }

    @Test
    public void autoRestoreHookRunsInOnCreate() throws Exception {
        String src = dashboardSource();
        assertTrue("BulkDashboardActivity source milna chahiye", src != null);
        int onCreate = src.indexOf("protected void onCreate");
        int hook = src.indexOf("BulkSettingsBackup.autoRestoreIfNeeded(this)");
        assertTrue("onCreate milna chahiye", onCreate >= 0);
        assertTrue("auto-restore hook onCreate ke andar ho", hook > onCreate);
        // UI banne (buildUi) se pehle — settings pehle, shakal baad me
        int buildUiCall = src.indexOf("buildUi();", onCreate);
        assertTrue(hook < buildUiCall);
    }
}
