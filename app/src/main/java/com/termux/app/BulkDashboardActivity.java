package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.shared.logger.Logger;

import java.util.List;

/**
 * Termux Mod — Dashboard (v124). App ki front screen (LAUNCHER).
 *
 * Bilkul saada screen: bare buttons, projects ki plain list (naam, counts,
 * Run / Delete / Batch), or neeche Terminal / Permissions / Updates / About.
 * Koi fancy styling nahi — standard Android look.
 *
 * Pehli setup mukammal na ho to ye screen TermuxActivity ki taraf bhej
 * deti hai (wahan bootstrap + packages setup v123 ki tarah chalta hai);
 * setup ke baad app khud yahin wapas le ata hai.
 */
public class BulkDashboardActivity extends Activity {

    private static final String LOG_TAG = "BulkDashboard";

    /** TermuxActivity ko diya jane wala extra: ye command terminal me chalao. */
    public static final String EXTRA_RUN_COMMAND = "bulk_run_command";
    /** TermuxActivity ko diya jane wala extra: setup ke baad dashboard khol do. */
    public static final String EXTRA_OPEN_DASHBOARD_AFTER_SETUP = "bulk_open_dashboard_after_setup";

    private boolean mRoutedToSetup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Termux Mod");

        if (!BulkMockupSetup.isSetupDone()) {
            // Setup baqi hai — terminal ki taraf bhejo; setup ke baad wapas yahin ana hai.
            mRoutedToSetup = true;
            try {
                Intent intent = new Intent(this, TermuxActivity.class);
                intent.putExtra(EXTRA_OPEN_DASHBOARD_AFTER_SETUP, true);
                startActivity(intent);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Setup ke liye TermuxActivity nahi khula: " + e.getMessage());
                Toast.makeText(this, "Terminal nahi khul saka.", Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRoutedToSetup) return;
        if (!BulkMockupSetup.isSetupDone()) return; // setup abhi chal raha hoga
        buildUi(); // har dafa fresh — counts taaza rehte hain
        // App ke andar wale projects: jo folder na ho, peeche lag jata hai
        BulkProjectBundles.installInBackground(this, () -> runOnUiThread(() -> {
            if (!isFinishing()) buildUi();
        }));
    }

    private void buildUi() {
        int dp = (int) (getResources().getDisplayMetrics().density + 0.5f);
        int pad = 16 * dp;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Termux Mod");
        title.setTextSize(22);
        title.setPadding(0, 0, 0, 12 * dp);
        root.addView(title);

        Button addWallpapers = new Button(this);
        addWallpapers.setText("Wallpapers Add Karo");
        addWallpapers.setTextSize(17);
        addWallpapers.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, BulkWallpapersActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Wallpapers wali screen nahi khul saki.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(addWallpapers, fullWidth());

        Button addProject = new Button(this);
        addProject.setText("Naya Project Add Karo (ZIP)");
        addProject.setTextSize(17);
        addProject.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, BulkAddProjectActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Project add wali screen nahi khul saki.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(addProject, fullWidth());

        TextView projectsHeader = new TextView(this);
        projectsHeader.setText("PROJECTS");
        projectsHeader.setTextSize(16);
        projectsHeader.setPadding(0, 14 * dp, 0, 6 * dp);
        root.addView(projectsHeader);

        List<BulkProjects.ProjectInfo> projects = loadProjects();
        if (projects.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Koi project nahi.");
            empty.setTextSize(14);
            root.addView(empty);
        }
        for (final BulkProjects.ProjectInfo p : projects) {
            TextView name = new TextView(this);
            name.setText(p.name);
            name.setTextSize(16);
            name.setPadding(0, 10 * dp, 0, 0);
            root.addView(name);

            if (p.number > 0) {
                TextView counts = new TextView(this);
                counts.setText(BulkProjects.countsLineFor(p.number));
                counts.setTextSize(13);
                root.addView(counts);
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Button run = new Button(this);
            run.setText("Run");
            run.setEnabled(p.workCmd != null);
            run.setOnClickListener(v -> runInTerminal(p.workCmd));
            row.addView(run, thirdWidth());
            Button batch = new Button(this);
            batch.setText("Batch");
            batch.setEnabled(p.batchCmd != null);
            batch.setOnClickListener(v -> runInTerminal(p.batchCmd));
            row.addView(batch, thirdWidth());
            Button delete = new Button(this);
            delete.setText("Delete");
            delete.setEnabled(p.deleteCmd != null);
            delete.setOnClickListener(v -> runInTerminal(p.deleteCmd));
            row.addView(delete, thirdWidth());
            root.addView(row, fullWidth());

            if (p.workCmd == null) {
                TextView note = new TextView(this);
                note.setText("Script nahi mila");
                note.setTextSize(12);
                root.addView(note);
            }
        }

        Button terminal = new Button(this);
        terminal.setText("Terminal");
        terminal.setTextSize(17);
        LinearLayout.LayoutParams tlp = fullWidth();
        tlp.setMargins(0, 14 * dp, 0, 0);
        terminal.setLayoutParams(tlp);
        terminal.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, TermuxActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Terminal nahi khul saka.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(terminal);

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        Button permissions = new Button(this);
        permissions.setText("Permissions");
        permissions.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, BulkPermissionsActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Permissions screen nahi khul saki.", Toast.LENGTH_SHORT).show();
            }
        });
        bottomRow.addView(permissions, thirdWidth());
        Button updates = new Button(this);
        updates.setText("Updates");
        updates.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, BulkUpdateActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Update screen nahi khul saki.", Toast.LENGTH_SHORT).show();
            }
        });
        bottomRow.addView(updates, thirdWidth());
        Button about = new Button(this);
        about.setText("About");
        about.setOnClickListener(v -> showAbout());
        bottomRow.addView(about, thirdWidth());
        root.addView(bottomRow, fullWidth());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private List<BulkProjects.ProjectInfo> loadProjects() {
        try {
            return BulkProjects.mergeProjects(ProjectsLauncher.readProjectsList(), BulkProjects.scanDiskProjects());
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Projects parhne me masla: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /** Command ko terminal me chalao — TermuxActivity khol kar wahan type ho jati hai. */
    private void runInTerminal(String command) {
        if (command == null) return;
        try {
            Intent intent = new Intent(this, TermuxActivity.class);
            intent.putExtra(EXTRA_RUN_COMMAND, command);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Terminal nahi khul saka.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
            .setTitle("Termux Mod")
            .setMessage("Termux Mod\nVersion: " + versionName() + "\n\n"
                + "Tamam mockup projects ke liye — packages pehle se andar hain, koi setup nahi.\n\n"
                + "Owner: Mehar Adnan")
            .setPositiveButton("OK", null)
            .show();
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams thirdWidth() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }
}
