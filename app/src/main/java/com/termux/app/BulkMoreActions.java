package com.termux.app;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Termux Mod (v145) — More Settings screen ke peeche wale asal flows.
 *
 * Ye wahi flows hain jo pehle sidebar (drawer) ki Backup aur
 * Export Project rows se chalte thay — v145 me un rows ki jagah
 * "More Settings" screen ah gayi hai, is liye code yahan shared
 * static shakl me hai taake screen inhe bilkul wese hi chalaye
 * jese drawer chalata tha. Behavior me koi tabdeeli nahi.
 */
public class BulkMoreActions {

    // ------------------------------------------------------------ Backup popup

    /** Backup popup: do chhoti rows — Settings Save / Wapas Lao. */
    public static void showBackupPopup(final Activity a) {
        final android.app.Dialog dialog = new android.app.Dialog(a);
        LinearLayout panel = BulkTheme.modalPanel(a,
            BulkStrings.t(a, BulkStrings.BACKUP), dialog);

        TextView saveBtn = BulkTheme.primaryButton(a,
            BulkStrings.t(a, BulkStrings.BACKUP_SAVE), v -> {
                dialog.dismiss();
                new Thread(() -> {
                    final boolean ok = BulkSettingsBackup.save(a);
                    a.runOnUiThread(() -> {
                        if (a.isFinishing()) return;
                        Toast.makeText(a, BulkStrings.t(a, ok
                            ? BulkStrings.BACKUP_SAVED
                            : BulkStrings.BACKUP_SAVE_FAILED),
                            Toast.LENGTH_LONG).show();
                    });
                }).start();
            });
        panel.addView(saveBtn);

        TextView loadBtn = BulkTheme.secondaryButton(a,
            BulkStrings.t(a, BulkStrings.BACKUP_LOAD), v -> {
                dialog.dismiss();
                new Thread(() -> {
                    final int res = BulkSettingsBackup.load(a);
                    a.runOnUiThread(() -> {
                        if (a.isFinishing()) return;
                        if (res == BulkSettingsBackup.LOAD_OK) {
                            BulkControlPanelActivity.applyIconAliases(a,
                                BulkPanelSettings.iconChoice(a));
                            Toast.makeText(a,
                                BulkStrings.t(a, BulkStrings.BACKUP_LOADED),
                                Toast.LENGTH_LONG).show();
                            a.recreate();
                        } else {
                            Toast.makeText(a, BulkStrings.t(a,
                                res == BulkSettingsBackup.LOAD_NOT_FOUND
                                    ? BulkStrings.CP_IMPORT_NOT_FOUND
                                    : BulkStrings.CP_IMPORT_FAILED),
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }).start();
            });
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = BulkTheme.dp(a, 10);
        loadBtn.setLayoutParams(llp);
        panel.addView(loadBtn);

        BulkTheme.showPanelDialog(a, panel);
    }

    // ------------------------------------------------------------ Export Project

    /** Project chuno → FOLDER copy Download/TermuxMod me (ZIP nahi). */
    public static void startExportFlow(final Activity a) {
        final java.util.List<BulkProjects.ProjectInfo> projects = new java.util.ArrayList<>();
        java.util.List<BulkProjects.ProjectInfo> all;
        try {
            all = BulkProjects.mergeProjects(
                ProjectsLauncher.readProjectsList(), BulkProjects.scanDiskProjects());
        } catch (Exception e) {
            all = new java.util.ArrayList<>();
        }
        for (BulkProjects.ProjectInfo p : all) {
            if (p.number > 0) projects.add(p);
        }
        if (projects.isEmpty()) {
            BulkTheme.showToast(a, BulkStrings.t(a, BulkStrings.NO_PROJECTS));
            return;
        }
        String[] names = new String[projects.size()];
        for (int i = 0; i < projects.size(); i++) names[i] = projects.get(i).name;
        BulkTheme.showChoice(a, BulkStrings.t(a, BulkStrings.EXPORT_PROJECT), names,
            idx -> exportProject(a, projects.get(idx)));
    }

    private static void exportProject(final Activity a, final BulkProjects.ProjectInfo p) {
        final java.io.File src = BulkProjects.projectDir(p.number);
        final java.io.File dst = new java.io.File(BulkFileManager.EXPORT_ROOT, src.getName());

        final android.app.Dialog dialog = new android.app.Dialog(a);
        LinearLayout panel = BulkTheme.modalPanel(a,
            BulkStrings.t(a, BulkStrings.EXPORT_PROJECT), dialog);
        TextView label = new TextView(a);
        label.setText(BulkStrings.t(a, BulkStrings.EXPORTING) + "  " + p.name);
        label.setTextColor(BulkTheme.textSub(a));
        label.setTextSize(12.5f);
        panel.addView(label);
        final LinearLayout track = BulkTheme.progressTrack(a);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(a, 4));
        tp.topMargin = BulkTheme.dp(a, 10);
        track.setLayoutParams(tp);
        panel.addView(track);
        dialog.setCancelable(false);
        BulkTheme.showPanelDialog(a, panel);

        new Thread(() -> {
            BulkFileManager.deleteRecursive(dst);
            final boolean ok = BulkFileManager.copyFolder(src, dst,
                pct -> a.runOnUiThread(() -> BulkTheme.setProgress(track, pct)));
            a.runOnUiThread(() -> {
                try { dialog.dismiss(); } catch (Exception ignored) {}
                BulkLog.add((ok ? "✔ Export: " : "✖ Export fail: ") + p.name);
                if (ok) showExportDone(a, dst);
                else BulkTheme.showToast(a, "Export fail ho gaya.");
            });
        }).start();
    }

    private static void showExportDone(final Activity a, final java.io.File dst) {
        final android.app.Dialog dialog = new android.app.Dialog(a);
        LinearLayout panel = BulkTheme.modalPanel(a,
            BulkStrings.t(a, BulkStrings.EXPORT_PROJECT), dialog);
        TextView body = new TextView(a);
        body.setText(BulkStrings.format(a, BulkStrings.EXPORT_DONE, dst.getAbsolutePath()));
        body.setTextColor(BulkTheme.textSub(a));
        body.setTextSize(12.5f);
        panel.addView(body);

        TextView open = BulkTheme.primaryButton(a,
            BulkStrings.t(a, BulkStrings.OPEN_FILE_MANAGER), v -> {
                dialog.dismiss();
                BulkFileManager.openFolder(a, dst);
            });
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        op.topMargin = BulkTheme.dp(a, 14);
        open.setLayoutParams(op);
        panel.addView(open);

        TextView okBtn = BulkTheme.secondaryButton(a, BulkStrings.t(a, BulkStrings.OK),
            v -> dialog.dismiss());
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        kp.topMargin = BulkTheme.dp(a, 8);
        okBtn.setLayoutParams(kp);
        panel.addView(okBtn);

        BulkTheme.showPanelDialog(a, panel);
    }
}
