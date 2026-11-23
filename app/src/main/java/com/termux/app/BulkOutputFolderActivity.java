package com.termux.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.termux.R;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Termux Mod (v127) — Output Folder (drawer se khulti hai).
 * Design me ye screen editable thi (folder rename / path presets),
 * lekin runner scripts ke paths hardcoded hain (/storage/emulated/0/
 * mockup_project_<N>) — rename karne se sara engine toot jayega.
 * Is liye ye screen READ-ONLY hai: asal storage location, selected
 * project ka asal folder, aur live folder structure (asal counts ke
 * sath). Deviation report me darj hai.
 */
public class BulkOutputFolderActivity extends Activity {

    private static final String LOG_TAG = "BulkOutputFolder";

    private List<BulkProjects.ProjectInfo> mProjects = new ArrayList<>();
    private int mSelectedNumber = -1;
    private TextView mMainFolder;
    private TextView mProjectFolder;
    private TextView mTree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        mProjects = loadProjects();
        for (BulkProjects.ProjectInfo p : mProjects) {
            if (p.number > 0) { mSelectedNumber = p.number; break; }
        }
        setContentView(buildUi());
        BulkFonts.apply(this);
        refreshInfo();
    }

    private List<BulkProjects.ProjectInfo> loadProjects() {
        try {
            return BulkProjects.mergeProjects(
                ProjectsLauncher.readProjectsList(), BulkProjects.scanDiskProjects());
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Projects list nahi mili: " + e);
            return new ArrayList<>();
        }
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this, BulkStrings.t(this, BulkStrings.OUTPUT_FOLDER),
            BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS_SUBTITLE)));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll);

        body.addView(infoBox(BulkStrings.t(this, BulkStrings.STORAGE_LOCATION),
            BulkProjects.STORAGE_ROOT));

        body.addView(projectPicker(), boxLp(12));

        mMainFolder = new TextView(this);
        body.addView(labeledBox(BulkStrings.t(this, BulkStrings.MAIN_FOLDER), mMainFolder), boxLp(12));

        mProjectFolder = new TextView(this);
        body.addView(labeledBox(BulkStrings.t(this, BulkStrings.PROJECT_FOLDER), mProjectFolder), boxLp(12));

        mTree = new TextView(this);
        mTree.setTypeface(android.graphics.Typeface.MONOSPACE);
        mTree.setTextSize(11.5f);
        body.addView(labeledBox(BulkStrings.t(this, BulkStrings.LIVE_FOLDER_TREE), mTree), boxLp(12));
        return root;
    }

    private LinearLayout.LayoutParams boxLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, topDp);
        return lp;
    }

    private View infoBox(String label, String value) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTypeface(android.graphics.Typeface.MONOSPACE);
        return labeledBox(label, v);
    }

    private View labeledBox(String label, TextView value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(BulkTheme.glassPanel(this));
        box.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(11);
        l.setTextColor(BulkTheme.textMuted(this));
        l.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        l.setLetterSpacing(0.08f);
        box.addView(l);
        value.setTextColor(BulkTheme.textMain(this));
        value.setTextSize(12.5f);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vp.topMargin = BulkTheme.dp(this, 6);
        value.setLayoutParams(vp);
        box.addView(value);
        return box;
    }

    private View projectPicker() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(BulkTheme.glassPanel(this));
        box.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        TextView l = new TextView(this);
        l.setText(BulkStrings.t(this, BulkStrings.SELECT_PROJECT));
        l.setTextSize(11);
        l.setTextColor(BulkTheme.textMuted(this));
        l.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        l.setLetterSpacing(0.08f);
        box.addView(l);

        FrameLayout wrap = new FrameLayout(this);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.topMargin = BulkTheme.dp(this, 8);
        wrap.setLayoutParams(wp);

        final Spinner spinner = new Spinner(this);
        spinner.setBackground(BulkTheme.spinnerBg(this, false));
        spinner.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 11),
            BulkTheme.dp(this, 34), BulkTheme.dp(this, 11));
        wrap.addView(spinner, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 16);
        FrameLayout.LayoutParams chp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 16), BulkTheme.dp(this, 16));
        chp.gravity = android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL;
        chp.rightMargin = BulkTheme.dp(this, 10);
        chev.setLayoutParams(chp);
        wrap.addView(chev);
        box.addView(wrap);

        List<String> labels = new ArrayList<>();
        for (BulkProjects.ProjectInfo p : mProjects) labels.add(p.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 0, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return spinnerText(position, convertView, false);
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return spinnerText(position, convertView, true);
            }
            private View spinnerText(int position, View convertView, boolean dropDown) {
                TextView t = convertView instanceof TextView
                    ? (TextView) convertView : new TextView(BulkOutputFolderActivity.this);
                t.setText(getItem(position));
                t.setTextSize(13.5f);
                t.setTextColor(BulkTheme.textMain(BulkOutputFolderActivity.this));
                t.setBackgroundColor(dropDown
                    ? BulkTheme.modalColor(BulkOutputFolderActivity.this) : Color.TRANSPARENT);
                t.setPadding(BulkTheme.dp(BulkOutputFolderActivity.this, 10),
                    BulkTheme.dp(BulkOutputFolderActivity.this, 10),
                    BulkTheme.dp(BulkOutputFolderActivity.this, 10),
                    BulkTheme.dp(BulkOutputFolderActivity.this, 10));
                t.setSingleLine(true);
                t.setEllipsize(TextUtils.TruncateAt.END);
                return t;
            }
        };
        spinner.setAdapter(adapter);
        int initial = 0;
        for (int i = 0; i < mProjects.size(); i++) {
            if (mProjects.get(i).number == mSelectedNumber) { initial = i; break; }
        }
        if (!mProjects.isEmpty()) spinner.setSelection(initial);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                if (position >= 0 && position < mProjects.size()) {
                    mSelectedNumber = mProjects.get(position).number;
                    refreshInfo();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        return box;
    }

    private void refreshInfo() {
        if (mSelectedNumber <= 0) {
            if (mMainFolder != null) mMainFolder.setText("—");
            if (mProjectFolder != null) mProjectFolder.setText("—");
            if (mTree != null) mTree.setText(BulkStrings.t(this, BulkStrings.NO_PROJECTS));
            return;
        }
        int n = mSelectedNumber;
        File base = BulkProjects.projectDir(n);
        mMainFolder.setText(base.getAbsolutePath());
        mProjectFolder.setText(base.getName());
        StringBuilder sb = new StringBuilder();
        sb.append(base.getName()).append("/\n");
        sb.append("  ").append(n).append("_wallpapers/  (")
            .append(BulkProjects.countFiles(BulkProjects.wallpapersDir(n))).append(")\n");
        sb.append("  ").append(n).append("_Mockups/  (")
            .append(BulkProjects.countFiles(BulkProjects.mockupsDir(n))).append(")\n");
        sb.append("  ").append(n).append("_Ready_Wallpapers/  (")
            .append(BulkProjects.countFiles(BulkProjects.readyDir(n))).append(")\n");
        sb.append("  Ready_Upload_Batches/  (")
            .append(countSubdirs(new File(base, "Ready_Upload_Batches"))).append(")");
        mTree.setText(sb.toString());
    }

    private static int countSubdirs(File dir) {
        File[] kids = dir.listFiles();
        if (kids == null) return 0;
        int c = 0;
        for (File k : kids) if (k.isDirectory()) c++;
        return c;
    }
}
