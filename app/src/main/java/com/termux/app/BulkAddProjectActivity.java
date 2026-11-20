package com.termux.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
 * Termux Mod — "Naya Project Add Karo (ZIP)" (v124).
 *
 * ZIP select karo — baqi sab khud (BulkProjectInstaller): unzip, project
 * number + main script pehchanna, /storage/emulated/0/mockup_project_<N>
 * me lagana, ghayab subfolders, aliases or projects.conf line (sirf jab
 * pehle se na hon). Folder pehle se ho to ruk jata hai — overwrite nahi.
 * Screen par sirf short labels / buttons.
 */
public class BulkAddProjectActivity extends Activity {

    private static final String LOG_TAG = "BulkAddProject";
    private static final int REQ_PICK_ZIP = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Naya Project Add Karo");

        if (!hasStorageAccess()) {
            showMessage("Storage permission chahiye.", true);
            return;
        }
        launchPicker();
    }

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                return Environment.isExternalStorageManager();
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

    private void launchPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            startActivityForResult(intent, REQ_PICK_ZIP);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "ZIP picker nahi khula: " + e.getMessage());
            showMessage("Picker nahi khul saka.", false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_ZIP) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            finish();
            return;
        }
        installFromZip(data.getData());
    }

    private void installFromZip(final Uri zipUri) {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Project add ho raha hai...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            String error = null;
            int addedNumber = -1;
            File tempDir = null;
            try {
                tempDir = new File(getCacheDir(), "zipadd_" + System.currentTimeMillis());
                File zipCopy = new File(tempDir, "project.zip");
                copyUri(zipUri, zipCopy);

                // 1) Entries parho — project folder + number pehchano
                List<String> entries = BulkProjectInstaller.listEntries(zipCopy);
                String rootFolder = BulkProjects.findProjectRoot(entries);
                if (rootFolder == null) {
                    throw new Exception("ZIP me mockup_project_<N> folder nahi mila.");
                }
                int n = BulkProjects.parseFolderNumber(rootFolder);
                if (n <= 0) {
                    throw new Exception("Project number nahi mila.");
                }

                // 2) Main script pehchano (root ki files se)
                List<String> rootFiles = new ArrayList<>();
                String prefix = rootFolder + "/";
                for (String e : entries) {
                    String norm = e.replace('\\', '/');
                    if (norm.startsWith(prefix)) {
                        String rest = norm.substring(prefix.length());
                        if (!rest.isEmpty() && !rest.contains("/")) rootFiles.add(rest);
                    }
                }
                String script = BulkProjects.detectMainScript(rootFiles, n);
                if (script == null) {
                    throw new Exception("Project ki main script nahi mili.");
                }

                // 3) Extract (sirf project folder, zip-slip guard ke sath)
                File extractDir = new File(tempDir, "extract");
                BulkProjectInstaller.extractProject(zipCopy, prefix, extractDir);
                File extractedProject = new File(extractDir, rootFolder);
                if (!extractedProject.isDirectory()) {
                    throw new Exception("ZIP khul nahi saka.");
                }

                // 4) Asal jagah lagao + subfolders + aliases + conf line
                //    (target pehle se ho to installProject khud ruk jata hai)
                BulkProjectInstaller.installProject(this, extractedProject, n, script);

                addedNumber = n;
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Project add fail: " + e.getMessage());
                error = e.getMessage();
            } finally {
                if (tempDir != null) BulkProjectInstaller.deleteRecursive(tempDir);
            }
            final String fError = error;
            final int fNumber = addedNumber;
            runOnUiThread(() -> {
                try { progress.dismiss(); } catch (Exception ignored) {}
                if (fError == null) {
                    showMessage("Project " + fNumber + " tayyar hai — list me add ho gaya", false);
                } else {
                    showMessage(fError != null ? fError : "Project add nahi ho saka.", false);
                }
            });
        }).start();
    }

    // ------------------------------------------------------------ UI

    private void showMessage(String text, boolean showPermissionsButton) {
        int dp = (int) (getResources().getDisplayMetrics().density + 0.5f);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16 * dp, 16 * dp, 16 * dp, 16 * dp);

        TextView msg = new TextView(this);
        msg.setText(text);
        msg.setTextSize(16);
        msg.setPadding(0, 0, 0, 12 * dp);
        root.addView(msg);

        if (showPermissionsButton) {
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
        }

        Button close = new Button(this);
        close.setText("Band Karo");
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    // ------------------------------------------------------------ file helpers

    private void copyUri(Uri uri, File dest) throws Exception {
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) throw new Exception("ZIP khul nahi saka.");
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }
}
