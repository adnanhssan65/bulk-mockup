package com.termux.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.termux.R;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod (v127) — Preview Added Wallpapers (hub se khulti hai).
 * Tamam projects + un ke <N>_wallpapers folder ka count; row tap karne
 * par us project ke asal thumbnails khulte hain. Design: accordion —
 * sab rows default band. Sirf dekhna hai (delete/edit nahi) — ye
 * Adnan ka v125 wala usool hai.
 */
public class BulkPreviewActivity extends Activity {

    private static final String LOG_TAG = "BulkPreview";

    private List<BulkProjects.ProjectInfo> mProjects = new ArrayList<>();
    private final Set<Integer> mExpanded = new HashSet<>();
    private final Map<String, Bitmap> mThumbs = new HashMap<>();
    private int mThumbGen = 0;
    private LinearLayout mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        mProjects = loadProjects();
        setContentView(buildUi());
        BulkFonts.apply(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Counts bahar se bhi badal sakte hain (terminal run / Add)
        mProjects = loadProjects();
        refreshList();
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
        root.addView(BulkTheme.subHeader(this, BulkStrings.t(this, BulkStrings.PREVIEW_ADDED),
            BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS_SUBTITLE)));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(BulkTheme.glassPanel(this));
        panel.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 8));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.setMargins(BulkTheme.dp(this, 18), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        panel.setLayoutParams(pp);
        mList = new LinearLayout(this);
        mList.setOrientation(LinearLayout.VERTICAL);
        panel.addView(mList);
        scroll.addView(panel);
        root.addView(scroll);

        refreshList();
        return root;
    }

    private void refreshList() {
        if (mList == null) return;
        mList.removeAllViews();
        boolean any = false;
        for (BulkProjects.ProjectInfo p : mProjects) {
            if (p.number <= 0) continue;
            any = true;
            mList.addView(previewRow(p));
        }
        if (!any) {
            TextView empty = new TextView(this);
            empty.setText(BulkStrings.t(this, BulkStrings.NO_PROJECTS));
            empty.setTextColor(BulkTheme.textMuted(this));
            empty.setTextSize(12);
            empty.setPadding(0, BulkTheme.dp(this, 6), 0, BulkTheme.dp(this, 6));
            mList.addView(empty);
        }
        BulkFonts.applyToTree(this, mList);
    }

    private View previewRow(final BulkProjects.ProjectInfo p) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        final boolean expanded = mExpanded.contains(p.number);
        final File wdir = BulkProjects.wallpapersDir(p.number);
        int count = BulkProjects.countFiles(wdir);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(0, BulkTheme.dp(this, 8), 0, BulkTheme.dp(this, 8));
        TextView name = new TextView(this);
        name.setText(p.name + " · " + count);
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(13.5f);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.addView(name);
        View spacer = new View(this);
        head.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 14);
        LinearLayout.LayoutParams chp = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14));
        chev.setLayoutParams(chp);
        chev.setRotation(expanded ? 90 : 0);
        head.addView(chev);
        head.setOnClickListener(v -> {
            if (expanded) mExpanded.remove(p.number);
            else mExpanded.add(p.number);
            refreshList();
        });
        row.addView(head);

        View line = new View(this);
        line.setBackground(BulkTheme.hLine(this));
        row.addView(line, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 1)));

        if (expanded) {
            List<File> files = imageFiles(wdir);
            if (files.isEmpty()) {
                TextView none = new TextView(this);
                none.setText(BulkStrings.t(this, BulkStrings.NO_WALLPAPERS));
                none.setTextColor(BulkTheme.textMuted(this));
                none.setTextSize(11);
                none.setPadding(0, BulkTheme.dp(this, 6), 0, BulkTheme.dp(this, 2));
                row.addView(none);
            } else {
                GridLayout grid = new GridLayout(this);
                grid.setColumnCount(3);
                LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                gp.topMargin = BulkTheme.dp(this, 6);
                grid.setLayoutParams(gp);
                for (File f : files) grid.addView(previewThumb(f));
                row.addView(grid);
                loadThumbs(files);
            }
        }
        return row;
    }

    private View previewThumb(File f) {
        FrameLayout box = new FrameLayout(this);
        box.setBackground(BulkTheme.tileBg(this, false));
        int gap = BulkTheme.dp(this, 8);
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = BulkTheme.dp(this, 84);
        glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        glp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
        box.setLayoutParams(glp);
        Bitmap bmp = mThumbs.get(f.getAbsolutePath());
        if (bmp != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bmp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(BulkTheme.dp(this, 3), BulkTheme.dp(this, 3),
                BulkTheme.dp(this, 3), BulkTheme.dp(this, 3));
            box.addView(iv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            ImageView iv = BulkTheme.icon(this, R.drawable.ic_bulk_image, BulkTheme.textMuted(this), 18);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
                BulkTheme.dp(this, 18), BulkTheme.dp(this, 18));
            ip.gravity = android.view.Gravity.CENTER;
            iv.setLayoutParams(ip);
            box.addView(iv);
        }
        return box;
    }

    private void loadThumbs(final List<File> files) {
        final int gen = ++mThumbGen;
        new Thread(() -> {
            for (File f : files) {
                String key = f.getAbsolutePath();
                if (mThumbs.containsKey(key)) continue;
                Bitmap bmp = decodeFileThumb(f, 220);
                if (bmp != null) {
                    mThumbs.put(key, bmp);
                    runOnUiThread(() -> {
                        if (gen == mThumbGen) refreshList();
                    });
                }
            }
        }).start();
    }

    private Bitmap decodeFileThumb(File f, int reqSize) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in = new FileInputStream(f);
            BitmapFactory.decodeStream(in, null, bounds);
            in.close();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, reqSize);
            in = new FileInputStream(f);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private static int sampleSize(int w, int h, int req) {
        int s = 1;
        while (w / (s * 2) >= req && h / (s * 2) >= req) s *= 2;
        return s;
    }

    private static List<File> imageFiles(File dir) {
        List<File> out = new ArrayList<>();
        File[] kids = dir.listFiles();
        if (kids == null) return out;
        for (File k : kids) {
            // v129: count wala usool — hidden files grid me bhi nahi
            if (k.isFile() && BulkProjects.isCountableWallpaper(k.getName())) out.add(k);
        }
        java.util.Collections.sort(out, (a, b) ->
            a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
        return out;
    }
}
