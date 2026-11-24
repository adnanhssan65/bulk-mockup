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
 * sab rows default band.
 *
 * v130 (owner ki hidayat — purana "sirf dekhna hai" usool dropped
 * wallpapers ke liye khatam): khule hue project ke dropped wallpapers
 * SELECT (tap toggle) karke DELETE ho sakte hain — gallery wala hi
 * toolbar (Sort Newest/Oldest/Name cycle, Grid 2–6 cycle, Select All,
 * Delete + design-style confirm). Delete asal files ko <N>_wallpapers
 * se hatata hai; counts har refresh par fresh hain.
 */
public class BulkPreviewActivity extends Activity {

    private static final String LOG_TAG = "BulkPreview";

    private List<BulkProjects.ProjectInfo> mProjects = new ArrayList<>();
    private final Set<Integer> mExpanded = new HashSet<>();
    private final Map<String, Bitmap> mThumbs = new HashMap<>();
    private int mThumbGen = 0;
    private LinearLayout mList;
    // v130: dropped wallpapers ki selection (absolute paths) + toolbar halat
    private final Set<String> mSelectedPaths = new HashSet<>();
    private int mSortMode = BulkGalleryState.SORT_NAME;
    private int mCols = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        // v133: Sort/Grid shared persisted prefs (gallery wali hi keys)
        android.content.SharedPreferences gp = getSharedPreferences(
            BulkGalleryState.PREFS_NAME, MODE_PRIVATE);
        mSortMode = BulkGalleryState.resolveSortPref(
            gp.getInt(BulkGalleryState.KEY_SORT, BulkGalleryState.PREF_ABSENT));
        mCols = BulkGalleryState.resolveGridPref(
            gp.getInt(BulkGalleryState.KEY_GRID, BulkGalleryState.PREF_ABSENT));
        mProjects = loadProjects();
        setContentView(buildUi());
        BulkFonts.apply(this);
    }

    private void saveSortPref() {
        getSharedPreferences(BulkGalleryState.PREFS_NAME, MODE_PRIVATE).edit()
            .putInt(BulkGalleryState.KEY_SORT, mSortMode).apply();
    }

    private void saveGridPref() {
        getSharedPreferences(BulkGalleryState.PREFS_NAME, MODE_PRIVATE).edit()
            .putInt(BulkGalleryState.KEY_GRID, mCols).apply();
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
        // v134: scroll handle (right edge) — page-scroll par track
        android.widget.FrameLayout scrollWrap = new android.widget.FrameLayout(this);
        scrollWrap.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        scroll.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollWrap.addView(scroll);
        BulkScrollHandle.forScrollView(this, scrollWrap, scroll);
        root.addView(scrollWrap);

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
            List<File> files = BulkGalleryState.sortFiles(imageFiles(wdir), mSortMode);
            if (files.isEmpty()) {
                TextView none = new TextView(this);
                none.setText(BulkStrings.t(this, BulkStrings.NO_WALLPAPERS));
                none.setTextColor(BulkTheme.textMuted(this));
                none.setTextSize(11);
                none.setPadding(0, BulkTheme.dp(this, 6), 0, BulkTheme.dp(this, 2));
                row.addView(none);
            } else {
                row.addView(dropToolbar(wdir, files));
                GridLayout grid = new GridLayout(this);
                grid.setColumnCount(mCols);
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

    // ------------------------------------------------------------ v130 dropped toolbar

    /** Gallery wala toolbar (compact): (N selected | Select All) / (Sort | Grid | Delete). */
    private View dropToolbar(final File dir, final List<File> files) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = BulkTheme.dp(this, 8);
        bar.setLayoutParams(blp);

        int selCount = 0;
        for (File f : files) if (mSelectedPaths.contains(f.getAbsolutePath())) selCount++;

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView count = new TextView(this);
        count.setText(BulkStrings.format(this, BulkStrings.SELECTED_COUNT, selCount));
        count.setTextColor(BulkTheme.accent(this));
        count.setTextSize(12.5f);
        count.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row1.addView(count);
        row1.addView(new View(this), new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final int fSelCount = selCount;
        TextView selectAll = pillText(BulkStrings.t(this,
            BulkGalleryState.isAllSelected(selCount, files.size())
                ? BulkStrings.UNSELECT_ALL : BulkStrings.SELECT_ALL), false);
        selectAll.setOnClickListener(v -> {
            if (BulkGalleryState.isAllSelected(fSelCount, files.size())) {
                for (File f : files) mSelectedPaths.remove(f.getAbsolutePath());
            } else {
                for (File f : files) mSelectedPaths.add(f.getAbsolutePath());
            }
            refreshList();
        });
        row1.addView(selectAll);
        bar.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r2p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r2p.topMargin = BulkTheme.dp(this, 8);
        row2.setLayoutParams(r2p);
        LinearLayout sortPill = pillIcon(R.drawable.ic_bulk_sort, BulkStrings.t(this,
            mSortMode == BulkGalleryState.SORT_OLDEST ? BulkStrings.SORT_OLDEST
                : mSortMode == BulkGalleryState.SORT_NAME ? BulkStrings.SORT_BY_NAME
                : BulkStrings.SORT_NEWEST));
        sortPill.setOnClickListener(v -> {
            mSortMode = BulkGalleryState.nextSort(mSortMode);
            saveSortPref();
            refreshList();
        });
        row2.addView(sortPill);
        LinearLayout gridPill = pillIcon(R.drawable.ic_bulk_grid, mCols + "x");
        gridPill.setOnClickListener(v -> {
            mCols = BulkGalleryState.nextColumns(mCols);
            saveGridPref();
            refreshList();
        });
        LinearLayout.LayoutParams gp2 = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp2.leftMargin = BulkTheme.dp(this, 6);
        gridPill.setLayoutParams(gp2);
        row2.addView(gridPill);
        row2.addView(new View(this), new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView del = pillText(BulkStrings.t(this, BulkStrings.DELETE)
            + (selCount > 0 ? " (" + selCount + ")" : ""), true);
        del.setEnabled(selCount > 0);
        del.setAlpha(selCount > 0 ? 1f : 0.45f);
        del.setOnClickListener(v -> confirmDeleteDropped(files));
        row2.addView(del);
        bar.addView(row2);
        return bar;
    }

    private TextView pillText(String text, boolean danger) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        b.setTextColor(danger ? BulkTheme.danger(this) : BulkTheme.textMain(this));
        b.setGravity(android.view.Gravity.CENTER);
        b.setBackground(danger ? BulkTheme.dangerBg(this)
            : BulkTheme.roundedBg(this, BulkTheme.glassBg(this), 20,
                BulkTheme.accentBorder(this), 1));
        b.setPadding(BulkTheme.dp(this, 11), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 11), BulkTheme.dp(this, 7));
        b.setClickable(true);
        return b;
    }

    private LinearLayout pillIcon(int iconRes, String label) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(android.view.Gravity.CENTER);
        pill.setBackground(BulkTheme.roundedBg(this, BulkTheme.glassBg(this), 20,
            BulkTheme.accentBorder(this), 1));
        pill.setPadding(BulkTheme.dp(this, 11), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 11), BulkTheme.dp(this, 7));
        pill.setClickable(true);
        pill.addView(BulkTheme.icon(this, iconRes, BulkTheme.textSub(this), 13));
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(12);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(BulkTheme.textMain(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = BulkTheme.dp(this, 5);
        tv.setLayoutParams(lp);
        pill.addView(tv);
        return pill;
    }

    private void confirmDeleteDropped(final List<File> files) {
        final List<File> sel = new ArrayList<>();
        for (File f : files) {
            if (mSelectedPaths.contains(f.getAbsolutePath())) sel.add(f);
        }
        if (sel.isEmpty()) return;
        BulkTheme.showConfirm(this, BulkStrings.t(this, BulkStrings.DELETE_DROPPED_TITLE),
            BulkStrings.format(this, BulkStrings.DELETE_DROPPED_MSG, sel.size()),
            BulkStrings.t(this, BulkStrings.DELETE), true, () -> deleteDroppedFiles(sel));
    }

    private void deleteDroppedFiles(List<File> sel) {
        int deleted = 0;
        for (File f : sel) {
            if (f.delete()) {
                deleted++;
                mThumbs.remove(f.getAbsolutePath());
            }
            mSelectedPaths.remove(f.getAbsolutePath());
        }
        if (deleted > 0) {
            String msg = BulkStrings.format(this, BulkStrings.DELETED_TOAST, deleted);
            BulkTheme.showToast(this, msg);
            BulkLog.add(msg);
        }
        refreshList();
    }

    private View previewThumb(final File f) {
        final FrameLayout box = new FrameLayout(this);
        final String path = f.getAbsolutePath();
        final boolean[] checked = {mSelectedPaths.contains(path)};
        box.setBackground(BulkTheme.tileBg(this, checked[0]));
        int gap = BulkTheme.dp(this, 8);
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = BulkTheme.dp(this, Math.max(52, Math.min(126, 252 / mCols)));
        glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        glp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
        box.setLayoutParams(glp);
        Bitmap bmp = mThumbs.get(path);
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
        // v130: selection badge (gallery jesa) — tap = toggle, phir
        // list refresh (toolbar ke counts bhi taza ho jate hain)
        final TextView badge = new TextView(this);
        badge.setText("✓");
        badge.setTextColor(BulkTheme.onAccent(this));
        badge.setTextSize(11);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setBackground(BulkTheme.accentCircleBg(this));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 20), BulkTheme.dp(this, 20));
        bp.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;
        bp.topMargin = BulkTheme.dp(this, 4);
        bp.rightMargin = BulkTheme.dp(this, 4);
        badge.setLayoutParams(bp);
        badge.setVisibility(checked[0] ? View.VISIBLE : View.GONE);
        box.addView(badge);
        box.setOnClickListener(v -> {
            if (checked[0]) mSelectedPaths.remove(path);
            else mSelectedPaths.add(path);
            refreshList();
        });
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
