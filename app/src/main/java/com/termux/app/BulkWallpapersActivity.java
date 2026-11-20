package com.termux.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Termux Mod — "Wallpapers Add Karo" (v124).
 *
 * Saada flow (user ki design): pehle Project dropdown, phir "Choose Files".
 * Chuni hui images usi order me project ke <N>_wallpapers folder me copy
 * hoti hain (har file ka time pichli se 1 second aage, taake projects ki
 * time-order wali sorting tumhara order na tore). Chalana alag step hai —
 * copy ke baad "Abhi Chalao" button se.
 * Screen par sirf short labels / buttons — koi lambi description nahi.
 */
public class BulkWallpapersActivity extends Activity {

    private static final String LOG_TAG = "BulkWallpapers";
    private static final int REQ_PICK = 41;

    private List<BulkProjects.ProjectInfo> mProjects = new ArrayList<>();
    private Spinner mProjectSpinner;
    private TextView mStatus;
    private Button mRunButton;
    private BulkProjects.ProjectInfo mLastProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Wallpapers Add Karo");

        if (!hasStorageAccess()) {
            showPermissionScreen();
            return;
        }
        buildUi();
    }

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                return Environment.isExternalStorageManager();
            } catch (Exception e) {
                return true; // check fail ho to rokna nahi — copy khud bata degi
            }
        }
        return true;
    }

    private void buildUi() {
        try {
            mProjects = BulkProjects.mergeProjects(ProjectsLauncher.readProjectsList(), BulkProjects.scanDiskProjects());
        } catch (Exception e) {
            mProjects = new ArrayList<>();
        }

        int dp = (int) (getResources().getDisplayMetrics().density + 0.5f);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16 * dp, 16 * dp, 16 * dp, 16 * dp);

        TextView projectLabel = new TextView(this);
        projectLabel.setText("Project");
        projectLabel.setTextSize(15);
        root.addView(projectLabel);

        mProjectSpinner = new Spinner(this);
        List<String> names = new ArrayList<>();
        for (BulkProjects.ProjectInfo p : mProjects) names.add(p.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, names);
        mProjectSpinner.setAdapter(adapter);
        mProjectSpinner.setEnabled(!mProjects.isEmpty());
        LinearLayout.LayoutParams spinnerLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spinnerLp.setMargins(0, 4 * dp, 0, 12 * dp);
        mProjectSpinner.setLayoutParams(spinnerLp);
        root.addView(mProjectSpinner);

        Button choose = new Button(this);
        choose.setText("Choose Files");
        choose.setTextSize(16);
        choose.setEnabled(!mProjects.isEmpty());
        choose.setOnClickListener(v -> launchPicker());
        root.addView(choose, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mStatus = new TextView(this);
        mStatus.setTextSize(15);
        mStatus.setPadding(0, 12 * dp, 0, 8 * dp);
        mStatus.setText(mProjects.isEmpty() ? "Koi project nahi." : "");
        root.addView(mStatus);

        mRunButton = new Button(this);
        mRunButton.setText("Abhi Chalao");
        mRunButton.setTextSize(16);
        mRunButton.setVisibility(View.GONE);
        mRunButton.setOnClickListener(v -> {
            if (mLastProject == null || mLastProject.workCmd == null) return;
            try {
                Intent intent = new Intent(this, TermuxActivity.class);
                intent.putExtra(BulkDashboardActivity.EXTRA_RUN_COMMAND, mLastProject.workCmd);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Terminal nahi khul saka.", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        root.addView(mRunButton, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button close = new Button(this);
        close.setText("Band Karo");
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void showPermissionScreen() {
        int dp = (int) (getResources().getDisplayMetrics().density + 0.5f);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16 * dp, 16 * dp, 16 * dp, 16 * dp);

        TextView msg = new TextView(this);
        msg.setText("Storage permission chahiye.");
        msg.setTextSize(16);
        msg.setPadding(0, 0, 0, 12 * dp);
        root.addView(msg);

        Button perm = new Button(this);
        perm.setText("Permissions");
        perm.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, BulkPermissionsActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Permissions screen nahi khul saki.", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        root.addView(perm, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button close = new Button(this);
        close.setText("Band Karo");
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void launchPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, REQ_PICK);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Image picker nahi khula: " + e.getMessage());
            mStatus.setText("Picker nahi khul saka.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK) return;
        if (resultCode != RESULT_OK || data == null) return; // cancel — screen wesi ki wesi

        List<Uri> picked = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri u = clip.getItemAt(i).getUri();
                if (u != null) picked.add(u);
            }
        } else if (data.getData() != null) {
            picked.add(data.getData());
        }
        if (picked.isEmpty()) return;

        int pos = mProjectSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= mProjects.size()) return;
        if (mProjects.get(pos).number <= 0) {
            mStatus.setText("Project number nahi mila.");
            return;
        }
        copyIntoProject(mProjects.get(pos), picked);
    }

    private void copyIntoProject(final BulkProjects.ProjectInfo project, final List<Uri> picked) {
        final File targetDir = new File(BulkProjects.projectDir(project.number), project.number + "_wallpapers");
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Copy ho raha hai...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            int copied = 0;
            int failed = 0;
            try {
                if (!targetDir.exists()) targetDir.mkdirs();
                // Time ka anchor: mojooda files ke sab se aakhri time se aage,
                // warna abhi ka waqt — phir har file +1 second.
                long anchor = System.currentTimeMillis();
                File[] existing = targetDir.listFiles();
                if (existing != null) {
                    for (File f : existing) {
                        anchor = Math.max(anchor, f.lastModified());
                    }
                }
                long step = 0;
                for (Uri uri : picked) {
                    step += 1000;
                    try {
                        File dest = uniqueFile(targetDir, displayName(uri));
                        copyUri(uri, dest);
                        //noinspection ResultOfMethodCallIgnored
                        dest.setLastModified(anchor + step);
                        copied++;
                    } catch (Exception e) {
                        failed++;
                        Logger.logError(LOG_TAG, "Copy fail (" + uri + "): " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Copy flow fail: " + e.getMessage());
                failed = picked.size() - copied;
            }
            final int fCopied = copied;
            final int fFailed = failed;
            runOnUiThread(() -> {
                try { progress.dismiss(); } catch (Exception ignored) {}
                if (fCopied > 0) {
                    mLastProject = project;
                    mStatus.setText(fCopied + " wallpapers add ho gaye."
                        + (fFailed > 0 ? " (" + fFailed + " fail)" : ""));
                    mRunButton.setVisibility(project.workCmd != null ? View.VISIBLE : View.GONE);
                } else {
                    mStatus.setText("Copy fail ho gaya.");
                    mRunButton.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    // ------------------------------------------------------------ copy helpers

    private String displayName(Uri uri) {
        String name = null;
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}
        if (name == null || name.trim().isEmpty()) {
            name = "wallpaper_" + System.currentTimeMillis() + ".jpg";
        }
        // Path separators hatao — naam sirf file ka naam rahe
        name = name.replace('/', '_').replace('\\', '_').trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            name = "wallpaper_" + System.currentTimeMillis() + ".jpg";
        }
        return name;
    }

    /** Naam pehle se ho to _1, _2... laga kar alag naam banao (koi file skip nahi hoti). */
    private static File uniqueFile(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        int i = 1;
        while (true) {
            f = new File(dir, base + "_" + i + ext);
            if (!f.exists()) return f;
            i++;
        }
    }

    private void copyUri(Uri uri, File dest) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) throw new Exception("File khul nahi saki");
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }
}
