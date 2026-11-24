package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.termux.R;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Termux Mod (v125) — "New Project (ZIP)" screen (approved design).
 *
 * ZIP select karo — baqi sab khud (BulkProjectInstaller): unzip, project
 * number + main script pehchanna, /storage/emulated/0/mockup_project_<N>
 * me lagana, ghayab subfolders, aliases or projects.conf line (sirf jab
 * pehle se na hon). Folder pehle se ho to ruk jata hai — overwrite nahi.
 * Engine v124 wala hi hai; sirf screen design ke states me hai:
 * idle ("Install Project Package") → installing (progress) → done.
 */
public class BulkAddProjectActivity extends Activity {

    private static final String LOG_TAG = "BulkAddProject";
    private static final int REQ_PICK_ZIP = 42;

    private LinearLayout mIdlePanel;
    private LinearLayout mBusyPanel;
    private LinearLayout mDonePanel;
    private TextView mBusyFile;
    private LinearLayout mBusyBar;
    private TextView mDoneName;
    private TextView mDoneFrom;
    private final AtomicBoolean mBusyAnim = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        if (!BulkStorageAccess.hasAccess(this)) {
            setContentView(buildMessageUi("Storage permission chahiye.", true));
            return;
        }
        setContentView(buildUi());
        BulkFonts.apply(this);
        showState(0);
    }

    // ------------------------------------------------------------ UI

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this, BulkStrings.t(this, BulkStrings.NEW_PROJECT_ZIP),
            BulkStrings.t(this, BulkStrings.ADD_PROJECT_SUBTITLE)));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        // v131 (Item 5 audit): fillViewport KHATAM — Stage/Duplicates
        // wali hi khaandani bimari (ek hi visible glass panel poori
        // height par phail sakta tha). Body ab content height par.
        scroll.setFillViewport(false);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll);

        body.addView(idlePanel());
        body.addView(busyPanel(), topLp(12));
        body.addView(donePanel(), topLp(12));
        return root;
    }

    private LinearLayout.LayoutParams topLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, topDp);
        return lp;
    }

    private LinearLayout designPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(BulkTheme.glassPanel(this));
        panel.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 22),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 22));
        return panel;
    }

    private View idlePanel() {
        mIdlePanel = designPanel();
        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, BulkStrings.INSTALL_PACKAGE));
        title.setTextColor(BulkTheme.textMain(this));
        title.setTextSize(15);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        mIdlePanel.addView(title);

        TextView sub = new TextView(this);
        sub.setText(BulkStrings.t(this, BulkStrings.INSTALL_PACKAGE_SUB));
        sub.setTextColor(BulkTheme.textMuted(this));
        sub.setTextSize(12);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = BulkTheme.dp(this, 5);
        sub.setLayoutParams(sp);
        mIdlePanel.addView(sub);

        TextView btn = BulkTheme.primaryButton(this, BulkStrings.t(this, BulkStrings.SELECT_ZIP), null);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = BulkTheme.dp(this, 16);
        btn.setLayoutParams(bp);
        btn.setOnClickListener(v -> launchPicker());
        mIdlePanel.addView(btn);

        return mIdlePanel;
    }

    private View busyPanel() {
        mBusyPanel = designPanel();
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.INSTALLING));
        label.setTextColor(BulkTheme.accent(this));
        label.setTextSize(13);
        label.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        mBusyPanel.addView(label);

        mBusyFile = new TextView(this);
        mBusyFile.setTextColor(BulkTheme.textSub(this));
        mBusyFile.setTextSize(12.5f);
        mBusyFile.setTypeface(android.graphics.Typeface.MONOSPACE);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = BulkTheme.dp(this, 8);
        mBusyFile.setLayoutParams(fp);
        mBusyPanel.addView(mBusyFile);

        mBusyBar = BulkTheme.progressTrack(this);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 4));
        bp.topMargin = BulkTheme.dp(this, 12);
        mBusyBar.setLayoutParams(bp);
        mBusyPanel.addView(mBusyBar);
        return mBusyPanel;
    }

    private View donePanel() {
        mDonePanel = designPanel();
        mDonePanel.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout circle = new FrameLayout(this);
        circle.setBackground(BulkTheme.accentCircleBg(this));
        circle.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 46), BulkTheme.dp(this, 46)));
        ImageView check = BulkTheme.icon(this, R.drawable.ic_bulk_check, BulkTheme.onAccent(this), 22);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 22), BulkTheme.dp(this, 22));
        cp.gravity = Gravity.CENTER;
        check.setLayoutParams(cp);
        circle.addView(check);
        mDonePanel.addView(circle);

        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, BulkStrings.INSTALLED_TITLE));
        title.setTextColor(BulkTheme.textMain(this));
        title.setTextSize(16);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = BulkTheme.dp(this, 12);
        title.setLayoutParams(tp);
        mDonePanel.addView(title);

        mDoneName = new TextView(this);
        mDoneName.setTextColor(BulkTheme.accent(this));
        mDoneName.setTextSize(13);
        mDoneName.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        np.topMargin = BulkTheme.dp(this, 4);
        mDoneName.setLayoutParams(np);
        mDonePanel.addView(mDoneName);

        mDoneFrom = new TextView(this);
        mDoneFrom.setTextColor(BulkTheme.textMuted(this));
        mDoneFrom.setTextSize(11.5f);
        mDoneFrom.setTypeface(android.graphics.Typeface.MONOSPACE);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = BulkTheme.dp(this, 3);
        mDoneFrom.setLayoutParams(fp);
        mDonePanel.addView(mDoneFrom);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = BulkTheme.dp(this, 16);
        row.setLayoutParams(rp);
        TextView another = BulkTheme.secondaryButton(this, BulkStrings.t(this, BulkStrings.ADD_ANOTHER), null);
        another.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        another.setOnClickListener(v -> showState(0));
        row.addView(another);
        TextView open = BulkTheme.primaryButton(this, BulkStrings.t(this, BulkStrings.OPEN_PROJECTS), null);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        op.leftMargin = BulkTheme.dp(this, 10);
        open.setLayoutParams(op);
        open.setOnClickListener(v -> {
            startActivity(new Intent(this, BulkProjectsActivity.class));
            finish();
        });
        row.addView(open);
        mDonePanel.addView(row);
        return mDonePanel;
    }

    /** 0 = idle, 1 = installing, 2 = done */
    private void showState(int state) {
        if (mIdlePanel == null) return;
        mIdlePanel.setVisibility(state == 0 ? View.VISIBLE : View.GONE);
        mBusyPanel.setVisibility(state == 1 ? View.VISIBLE : View.GONE);
        mDonePanel.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
        if (state == 1) startBusyAnim();
        else mBusyAnim.set(false);
    }

    private void startBusyAnim() {
        mBusyAnim.set(true);
        new Thread(() -> {
            int pct = 8;
            while (mBusyAnim.get()) {
                final int p = pct;
                runOnUiThread(() -> BulkTheme.setProgress(mBusyBar, p));
                pct += 4;
                if (pct > 92) pct = 8;
                try { Thread.sleep(120); } catch (InterruptedException e) { return; }
            }
        }).start();
    }

    private View buildMessageUi(String text, boolean showPermissionsButton) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.setPadding(BulkTheme.dp(this, 16), BulkTheme.dp(this, 16),
            BulkTheme.dp(this, 16), BulkTheme.dp(this, 16));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(BulkTheme.glassPanelStrong(this));
        panel.setPadding(BulkTheme.dp(this, 16), BulkTheme.dp(this, 16),
            BulkTheme.dp(this, 16), BulkTheme.dp(this, 16));

        TextView msg = new TextView(this);
        msg.setText(text);
        msg.setTextColor(BulkTheme.textMain(this));
        msg.setTextSize(15);
        panel.addView(msg);

        if (showPermissionsButton) {
            TextView perm = BulkTheme.primaryButton(this, BulkStrings.t(this, BulkStrings.PERMISSIONS), null);
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pp.topMargin = BulkTheme.dp(this, 14);
            perm.setLayoutParams(pp);
            perm.setOnClickListener(v -> {
                startActivity(new Intent(this, BulkPermissionsActivity.class));
                finish();
            });
            panel.addView(perm);
        }

        TextView close = BulkTheme.secondaryButton(this, BulkStrings.t(this, BulkStrings.CLOSE), null);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = BulkTheme.dp(this, 10);
        close.setLayoutParams(cp);
        close.setOnClickListener(v -> finish());
        panel.addView(close);

        root.addView(panel);
        return root;
    }

    private void showError(String text) {
        setContentView(buildMessageUi(text, false));
    }

    // ------------------------------------------------------------ picker + install (v124 engine)

    private void launchPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            startActivityForResult(intent, REQ_PICK_ZIP);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "ZIP picker nahi khula: " + e.getMessage());
            showError("Picker nahi khul saka.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_ZIP) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            showState(0); // cancel — idle screen par wapas
            return;
        }
        installFromZip(data.getData());
    }

    private String zipDisplayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = c.getString(idx);
                    if (name != null && !name.isEmpty()) return name;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        String seg = uri.getLastPathSegment();
        return seg != null ? seg : "project.zip";
    }

    private void installFromZip(final Uri zipUri) {
        final String zipName = zipDisplayName(zipUri);
        mBusyFile.setText(zipName);
        showState(1);

        new Thread(() -> {
            String error = null;
            int addedNumber = -1;
            File tempDir = null;
            try {
                tempDir = new File(getCacheDir(), "zipadd_" + System.currentTimeMillis());
                File zipCopy = new File(tempDir, "project.zip");
                copyUri(zipUri, zipCopy);

                // 1) Entries parho — project folder + number pehchano.
                //    v136 self-setting: folder na mile to FALLBACK —
                //    number ZIP ke file naam se (mockup_project_<N>.zip)
                //    or files seedha root par mani jati hain. Number
                //    dono tarah na mile to saaf bilingual error or
                //    kuch register NAHI hota.
                List<String> entries = BulkProjectInstaller.listEntries(zipCopy);
                String rootFolder = BulkProjects.findProjectRoot(entries);
                final boolean flatMode = rootFolder == null;
                final int n;
                final String prefix;
                if (!flatMode) {
                    n = BulkProjects.parseFolderNumber(rootFolder);
                    prefix = rootFolder + "/";
                } else {
                    n = BulkProjects.parseZipBaseNumber(zipName);
                    rootFolder = "mockup_project_" + n;
                    prefix = "";
                }
                if (n <= 0) {
                    throw new Exception(BulkStrings.t(BulkAddProjectActivity.this,
                        BulkStrings.ZIP_NO_FOLDER));
                }

                // 2) Main script pehchano (root ki files se)
                List<String> rootFiles = new ArrayList<>();
                for (String e : entries) {
                    String norm = e.replace('\\', '/');
                    if (norm.startsWith(prefix)) {
                        String rest = norm.substring(prefix.length());
                        if (!rest.isEmpty() && !rest.contains("/")) rootFiles.add(rest);
                    }
                }
                String script = BulkProjects.detectMainScript(rootFiles, n);
                if (script == null) {
                    throw new Exception(BulkStrings.t(BulkAddProjectActivity.this,
                        BulkStrings.ZIP_NO_SCRIPT));
                }

                // 3) Extract (sirf project folder, zip-slip guard ke sath;
                //    flat mode me saari files project folder ke andar)
                File extractDir = new File(tempDir, "extract");
                File extractedProject = flatMode
                    ? new File(extractDir, rootFolder) : extractDir;
                BulkProjectInstaller.extractProject(zipCopy, prefix, extractedProject);
                if (!flatMode) extractedProject = new File(extractDir, rootFolder);
                if (!extractedProject.isDirectory()) {
                    throw new Exception("ZIP khul nahi saka.");
                }

                // 4) Asal jagah lagao + subfolders + aliases + conf line
                //    (target pehle se ho to installProject khud ruk jata hai)
                BulkProjectInstaller.installProject(this, extractedProject, n, script);

                // v132: naye project ka manual-move drop folder bhi banao
                BulkDropFolders.ensureForProject(n);

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
                mBusyAnim.set(false);
                if (fError == null) {
                    mDoneName.setText(BulkStrings.format(BulkAddProjectActivity.this,
                        BulkStrings.PROJECT_N, fNumber));
                    mDoneFrom.setText(BulkStrings.format(BulkAddProjectActivity.this,
                        BulkStrings.EXTRACTED_FROM, zipName));
                    showState(2);
                } else {
                    showError(fError != null ? fError : "Project add nahi ho saka.");
                }
            });
        }).start();
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
