package com.termux.app;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * v145 Item D — Sidebar cleanup + More Settings screen.
 *
 * Drawer se Backup / Export Project / Output Folder rows hat gayi
 * hain; un ki jagah Control Panel ke foran baad ek "More Settings"
 * row hai jo apni poori screen (BulkMoreSettingsActivity) kholti
 * hai. Screen ki teenon rows wahi purane flows chalati hain —
 * Backup popup aur Export flow ab shared BulkMoreActions me hain
 * (Dashboard me un ki doosri copy nahi bachi). Ye tests source
 * scan se wiring pakki karti hain (BulkV145Test wala pattern).
 */
public class BulkV145ItemDTest {

    private static final String DASH =
        "app/src/main/java/com/termux/app/BulkDashboardActivity.java";
    private static final String PAGE =
        "app/src/main/java/com/termux/app/BulkMoreSettingsActivity.java";
    private static final String ACTIONS =
        "app/src/main/java/com/termux/app/BulkMoreActions.java";
    private static final String MANIFEST = "app/src/main/AndroidManifest.xml";
    private static final String GRADLE = "app/build.gradle";

    private static String findSource(String rel) throws Exception {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 8 && dir != null; i++) {
            File a = new File(dir, rel);
            if (a.isFile()) return new String(Files.readAllBytes(a.toPath()), StandardCharsets.UTF_8);
            File b = new File(dir, "bulk_mockup_fork/termux-app/" + rel);
            if (b.isFile()) return new String(Files.readAllBytes(b.toPath()), StandardCharsets.UTF_8);
            File c = new File(dir, "workspace/bulk_mockup_fork/termux-app/" + rel);
            if (c.isFile()) return new String(Files.readAllBytes(c.toPath()), StandardCharsets.UTF_8);
            dir = dir.getParentFile();
        }
        return null;
    }

    // ------------------------------------------------------------ drawer

    @Test
    public void drawerHasMoreSettingsAndNotTheThreeMovedRows() throws Exception {
        String dash = findSource(DASH);
        assertNotNull("dashboard source milna chahiye", dash);

        // Teen moved rows ab drawer me nahi hain
        assertFalse(dash.contains("drawerRow(BulkStrings.t(this, BulkStrings.BACKUP)"));
        assertFalse(dash.contains("drawerRow(BulkStrings.t(this, BulkStrings.EXPORT_PROJECT)"));
        assertFalse(dash.contains("drawerRow(BulkStrings.t(this, BulkStrings.OUTPUT_FOLDER)"));

        // More Settings row hai aur sahi screen kholti hai
        assertTrue(dash.contains("drawerRow(BulkStrings.t(this, BulkStrings.MORE_SETTINGS)"));
        assertTrue(dash.contains("BulkMoreSettingsActivity.class"));

        // Control Panel row wesi ki wesi hai
        assertTrue(dash.contains("drawerRow(BulkStrings.t(this, BulkStrings.CONTROL_PANEL)"));
        assertTrue(dash.contains("BulkControlPanelActivity.class"));

        // Tartib: Control Panel -> More Settings -> Check for Updates
        int cp = dash.indexOf("drawerRow(BulkStrings.t(this, BulkStrings.CONTROL_PANEL)");
        int ms = dash.indexOf("drawerRow(BulkStrings.t(this, BulkStrings.MORE_SETTINGS)");
        int cu = dash.indexOf("drawerRow(BulkStrings.t(this, BulkStrings.CHECK_FOR_UPDATES)");
        assertTrue("More Settings Control Panel ke baad", cp >= 0 && ms > cp);
        assertTrue("More Settings Updates se pehle", cu > ms);
    }

    @Test
    public void dashboardNoLongerHostsBackupOrExportFlows() throws Exception {
        String dash = findSource(DASH);
        assertNotNull(dash);
        // Flows BulkMoreActions me shift ho gaye — Dashboard me copy nahi
        assertFalse(dash.contains("private void showBackupPopup"));
        assertFalse(dash.contains("private void startExportFlow"));
        assertFalse(dash.contains("private void exportProject"));
        assertFalse(dash.contains("private void showExportDone"));
        // Lekin fresh-install auto-restore Dashboard me hi rehta hai
        assertTrue(dash.contains("BulkSettingsBackup.autoRestoreIfNeeded"));
    }

    // ------------------------------------------------------------ page + flows

    @Test
    public void moreSettingsPageWiresTheThreeExistingFlows() throws Exception {
        String page = findSource(PAGE);
        assertNotNull("More Settings screen ka source milna chahiye", page);
        // Header: aam sub-screen wala pattern (back + title)
        assertTrue(page.contains("BulkTheme.subHeader"));
        assertTrue(page.contains("BulkStrings.MORE_SETTINGS"));
        // Teen rows — wahi purane labels, wahi purane flows
        assertTrue(page.contains("BulkStrings.BACKUP"));
        assertTrue(page.contains("BulkStrings.EXPORT_PROJECT"));
        assertTrue(page.contains("BulkStrings.OUTPUT_FOLDER"));
        assertTrue(page.contains("BulkMoreActions.showBackupPopup"));
        assertTrue(page.contains("BulkMoreActions.startExportFlow"));
        assertTrue(page.contains("BulkOutputFolderActivity.class"));
    }

    @Test
    public void moreActionsHoldTheUnchangedFlows() throws Exception {
        String act = findSource(ACTIONS);
        assertNotNull("BulkMoreActions ka source milna chahiye", act);
        // Backup popup: Save / Load dono asal BulkSettingsBackup par
        assertTrue(act.contains("BulkSettingsBackup.save"));
        assertTrue(act.contains("BulkSettingsBackup.load"));
        assertTrue(act.contains("BulkStrings.BACKUP_SAVE"));
        assertTrue(act.contains("BulkStrings.BACKUP_LOAD"));
        // Export flow: project picker -> folder copy (ZIP nahi)
        assertTrue(act.contains("BulkProjects.mergeProjects"));
        assertTrue(act.contains("BulkFileManager.EXPORT_ROOT"));
        assertTrue(act.contains("BulkFileManager.copyFolder"));
        assertTrue(act.contains("BulkFileManager.openFolder"));
    }

    @Test
    public void moreSettingsActivityRegisteredInManifest() throws Exception {
        String man = findSource(MANIFEST);
        assertNotNull(man);
        assertTrue(man.contains(".app.BulkMoreSettingsActivity"));
    }

    // ------------------------------------------------------------ strings + version

    @Test
    public void moreSettingsStringsHaveBothLanguages() {
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.MORE_SETTINGS));
        assertTrue(BulkStrings.hasBothLanguages(BulkStrings.MORE_SETTINGS_SUB));
        assertEquals("More Settings", BulkStrings.get(BulkStrings.MORE_SETTINGS, false));
        assertEquals("More Settings", BulkStrings.get(BulkStrings.MORE_SETTINGS, true));
    }

    @Test
    public void versionIs149() throws Exception {
        String gradle = findSource(GRADLE);
        assertNotNull(gradle);
        assertTrue(gradle.contains("versionCode 153"));
        String dash = findSource(DASH);
        assertNotNull(dash);
        assertTrue(dash.contains("Termux Mod v153"));
    }
}
