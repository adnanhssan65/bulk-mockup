package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
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
 * Termux Mod (v127) — Preview Ready Assets (drawer se khulti hai).
 * Sirf wo projects jin ke <N>_Mockups me kuch hai. Project tap → us ke
 * asal folders: Mockups/ tab = <N>_Mockups, Wallpapers/ tab =
 * <N>_Ready_Wallpapers (2-col grid). Kisi item par tap → full-screen
 * inspector (bara preview + naam + asal path). Sab read-only — asal
 * folders ka asal data hi dikhta hai.
 *
 * v129: upar "Drop Wallpapers" ka alag option — projects ki list,
 * project kholne par us ke <N>_wallpapers (drop kiye hue) wallpapers
 * grid me. Counts har resume par FRESH ginne jate hain (koi cache
 * nahi).
 *
 * v130 (owner ki hidayat): Drop Wallpapers ki grid me dropped images
 * SELECT (tap toggle) karke DELETE ho sakte hain — gallery wala
 * toolbar (Sort Newest/Oldest/Name cycle, Grid 2–6 cycle, Select All,
 * Delete + design-style confirm). Mockups / Ready Wallpapers tabs
 * read-only hi rehte hain.
 */
public class BulkReadyAssetsActivity extends Activity {

    private static final String LOG_TAG = "BulkReadyAssets";

    private List<BulkProjects.ProjectInfo> mProjects = new ArrayList<>();
    private final Map<String, Bitmap> mThumbs = new HashMap<>();
    private int mThumbGen = 0;

    private LinearLayout mBody;
    private BulkProjects.ProjectInfo mOpenProject;
    private boolean mMockupsTab = true;
    // v129: Drop Wallpapers wale hissay ki halat
    private boolean mDropList = false;
    private BulkProjects.ProjectInfo mDropProject;
    // v130: Drop Wallpapers selection (absolute paths) + toolbar halat
    private final Set<String> mDropSelected = new HashSet<>();
    private int mDropSort = BulkGalleryState.SORT_NAME;
    private int mDropCols = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        mProjects = loadProjects();
        setContentView(buildUi());
        BulkFonts.apply(this);
        showList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // v129: counts FRESH — bahar se folders badle hon to bhi yahan
        // ke number purane na rahen (koi cached count nahi)
        mProjects = loadProjects();
        if (mDropProject != null) showDropFolder();
        else if (mDropList) showDropList();
        else if (mOpenProject != null) showFolder();
        else showList();
    }

    @Override
    public void onBackPressed() {
        // Folder view khula ho to pehle list par wapas
        if (mDropProject != null) {
            mDropProject = null;
            showDropList();
            return;
        }
        if (mDropList) {
            mDropList = false;
            showList();
            return;
        }
        if (mOpenProject != null) {
            mOpenProject = null;
            showList();
            return;
        }
        super.onBackPressed();
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
        root.addView(BulkTheme.subHeader(this, BulkStrings.t(this, BulkStrings.PREVIEW_READY),
            BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS_SUBTITLE)));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        mBody = new LinearLayout(this);
        mBody.setOrientation(LinearLayout.VERTICAL);
        mBody.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(mBody);
        root.addView(scroll);
        return root;
    }

    // ------------------------------------------------------------ project list

    private void showList() {
        mBody.removeAllViews();
        // v129: sab se upar Drop Wallpapers ka option (owner ki demand)
        mBody.addView(dropEntryRow());
        boolean any = false;
        for (BulkProjects.ProjectInfo p : mProjects) {
            if (p.number <= 0) continue;
            int mockups = BulkProjects.countFiles(BulkProjects.mockupsDir(p.number));
            if (mockups <= 0) continue;
            any = true;
            int ready = BulkProjects.countFiles(BulkProjects.readyDir(p.number));
            mBody.addView(projectRow(p, mockups, ready), listLp(any ? 0 : 0));
        }
        if (!any) {
            TextView hint = new TextView(this);
            hint.setText(BulkStrings.t(this, BulkStrings.NO_READY_PROJECTS));
            hint.setTextColor(BulkTheme.textMuted(this));
            hint.setTextSize(12.5f);
            hint.setPadding(0, BulkTheme.dp(this, 10), 0, 0);
            mBody.addView(hint);
        }
        BulkFonts.applyToTree(this, mBody);
    }

    private LinearLayout.LayoutParams listLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, topDp);
        return lp;
    }

    // ------------------------------------------------------------ Drop Wallpapers (v129)

    /** Main list ke sab se upar wali entry row — tap par drop projects ki list. */
    private View dropEntryRow() {
        int total = 0;
        for (BulkProjects.ProjectInfo p : mProjects) {
            if (p.number > 0) total += BulkProjects.countFiles(BulkProjects.wallpapersDir(p.number));
        }
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.glassPanelStrong(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 11),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 11));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = BulkTheme.dp(this, 14);
        row.setLayoutParams(rlp);

        FrameLayout iconBox = new FrameLayout(this);
        iconBox.setBackground(BulkTheme.iconBoxBg(this));
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 34), BulkTheme.dp(this, 34)));
        ImageView iv = BulkTheme.icon(this, R.drawable.ic_bulk_image, BulkTheme.accent(this), 17);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 17), BulkTheme.dp(this, 17));
        ip.gravity = Gravity.CENTER;
        iv.setLayoutParams(ip);
        iconBox.addView(iv);
        row.addView(iconBox);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = BulkTheme.dp(this, 11);
        texts.setLayoutParams(tp);
        TextView name = new TextView(this);
        name.setText(BulkStrings.t(this, BulkStrings.DROP_WALLPAPERS));
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(14);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        texts.addView(name);
        TextView sub = new TextView(this);
        sub.setText(BulkStrings.format(this, BulkStrings.WALLPAPERS_COUNT, total));
        sub.setTextColor(BulkTheme.textMuted(this));
        sub.setTextSize(11);
        texts.addView(sub);
        row.addView(texts);

        FrameLayout arrow = new FrameLayout(this);
        arrow.setBackground(BulkTheme.arrowCircleBg(this));
        arrow.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 24), BulkTheme.dp(this, 24)));
        ImageView av = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 12);
        FrameLayout.LayoutParams avp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 12));
        avp.gravity = Gravity.CENTER;
        av.setLayoutParams(avp);
        arrow.addView(av);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            mDropList = true;
            showDropList();
        });
        return row;
    }

    /** Drop Wallpapers: jin projects ke wallpapers folder me kuch hai, un ki list. */
    private void showDropList() {
        mBody.removeAllViews();
        TextView back = new TextView(this);
        back.setText("‹  " + BulkStrings.t(this, BulkStrings.PREVIEW_READY));
        back.setTextColor(BulkTheme.accent(this));
        back.setTextSize(13.5f);
        back.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        back.setPadding(0, 0, 0, BulkTheme.dp(this, 8));
        back.setOnClickListener(v -> { mDropList = false; showList(); });
        mBody.addView(back);

        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, BulkStrings.DROP_WALLPAPERS_SUB));
        title.setTextColor(BulkTheme.textMuted(this));
        title.setTextSize(11.5f);
        title.setPadding(0, 0, 0, BulkTheme.dp(this, 8));
        mBody.addView(title);

        boolean any = false;
        for (BulkProjects.ProjectInfo p : mProjects) {
            if (p.number <= 0) continue;
            int count = BulkProjects.countFiles(BulkProjects.wallpapersDir(p.number));
            if (count <= 0) continue;
            any = true;
            mBody.addView(dropProjectRow(p, count));
        }
        if (!any) {
            TextView hint = new TextView(this);
            hint.setText(BulkStrings.t(this, BulkStrings.NO_WALLPAPERS));
            hint.setTextColor(BulkTheme.textMuted(this));
            hint.setTextSize(12.5f);
            hint.setPadding(0, BulkTheme.dp(this, 10), 0, 0);
            mBody.addView(hint);
        }
        BulkFonts.applyToTree(this, mBody);
    }

    private View dropProjectRow(final BulkProjects.ProjectInfo p, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 11),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 11));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = BulkTheme.dp(this, 10);
        row.setLayoutParams(rlp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        texts.setLayoutParams(tp);
        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(14);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        texts.addView(name);
        TextView cnt = new TextView(this);
        cnt.setText(BulkStrings.format(this, BulkStrings.WALLPAPERS_COUNT, count));
        cnt.setTextColor(BulkTheme.textMuted(this));
        cnt.setTextSize(11);
        texts.addView(cnt);
        row.addView(texts);

        FrameLayout arrow = new FrameLayout(this);
        arrow.setBackground(BulkTheme.arrowCircleBg(this));
        arrow.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 24), BulkTheme.dp(this, 24)));
        ImageView av = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 12);
        FrameLayout.LayoutParams avp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 12));
        avp.gravity = Gravity.CENTER;
        av.setLayoutParams(avp);
        arrow.addView(av);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            mDropProject = p;
            showDropFolder();
        });
        return row;
    }

    /**
     * Ek project ke drop wallpapers (us ke <N>_wallpapers folder ki
     * images). v130: select + delete toolbar ke sath (upar dekho);
     * Mockups/Ready tabs wali read-only grid is se alag hai.
     */
    private void showDropFolder() {
        final BulkProjects.ProjectInfo p = mDropProject;
        if (p == null) { showDropList(); return; }
        mBody.removeAllViews();

        TextView back = new TextView(this);
        back.setText("‹  " + p.name);
        back.setTextColor(BulkTheme.accent(this));
        back.setTextSize(13.5f);
        back.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        back.setPadding(0, 0, 0, BulkTheme.dp(this, 8));
        back.setOnClickListener(v -> { mDropProject = null; showDropList(); });
        mBody.addView(back);

        File dir = BulkProjects.wallpapersDir(p.number);
        TextView path = new TextView(this);
        path.setText(dir.getAbsolutePath());
        path.setTextColor(BulkTheme.textMuted(this));
        path.setTextSize(10.5f);
        path.setTypeface(android.graphics.Typeface.MONOSPACE);
        mBody.addView(path);

        List<File> files = BulkGalleryState.sortFiles(imageFiles(dir), mDropSort);
        // Selection me sirf maujood files rakho (delete ke baad purani entries saaf)
        Set<String> existing = new HashSet<>();
        for (File f : files) existing.add(f.getAbsolutePath());
        mDropSelected.retainAll(existing);
        if (files.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(BulkStrings.t(this, BulkStrings.NO_WALLPAPERS));
            none.setTextColor(BulkTheme.textMuted(this));
            none.setTextSize(12);
            none.setPadding(0, BulkTheme.dp(this, 12), 0, 0);
            mBody.addView(none);
        } else {
            mBody.addView(dropToolbar(files));
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(mDropCols);
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gp.topMargin = BulkTheme.dp(this, 8);
            grid.setLayoutParams(gp);
            for (File f : files) grid.addView(dropThumb(f));
            mBody.addView(grid);
            loadThumbs(files);
        }
        BulkFonts.applyToTree(this, mBody);
    }

    // ------------------------------------------------------------ v130 drop toolbar

    /** Gallery wala toolbar (compact): (N selected | Select All) / (Sort | Grid | Delete). */
    private View dropToolbar(final List<File> files) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = BulkTheme.dp(this, 10);
        bar.setLayoutParams(blp);

        int selCount = 0;
        for (File f : files) if (mDropSelected.contains(f.getAbsolutePath())) selCount++;

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
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
                for (File f : files) mDropSelected.remove(f.getAbsolutePath());
            } else {
                for (File f : files) mDropSelected.add(f.getAbsolutePath());
            }
            showDropFolder();
        });
        row1.addView(selectAll);
        bar.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r2p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r2p.topMargin = BulkTheme.dp(this, 8);
        row2.setLayoutParams(r2p);
        LinearLayout sortPill = pillIcon(R.drawable.ic_bulk_sort, BulkStrings.t(this,
            mDropSort == BulkGalleryState.SORT_OLDEST ? BulkStrings.SORT_OLDEST
                : mDropSort == BulkGalleryState.SORT_NAME ? BulkStrings.SORT_BY_NAME
                : BulkStrings.SORT_NEWEST));
        sortPill.setOnClickListener(v -> {
            mDropSort = BulkGalleryState.nextSort(mDropSort);
            showDropFolder();
        });
        row2.addView(sortPill);
        LinearLayout gridPill = pillIcon(R.drawable.ic_bulk_grid, mDropCols + "x");
        gridPill.setOnClickListener(v -> {
            mDropCols = BulkGalleryState.nextColumns(mDropCols);
            showDropFolder();
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
        b.setGravity(Gravity.CENTER);
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
        pill.setGravity(Gravity.CENTER);
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
            if (mDropSelected.contains(f.getAbsolutePath())) sel.add(f);
        }
        if (sel.isEmpty()) return;
        BulkTheme.showConfirm(this, BulkStrings.t(this, BulkStrings.DELETE_DROPPED_TITLE),
            BulkStrings.format(this, BulkStrings.DELETE_DROPPED_MSG, sel.size()),
            BulkStrings.t(this, BulkStrings.DELETE), true, () -> {
                int deleted = 0;
                for (File f : sel) {
                    if (f.delete()) {
                        deleted++;
                        mThumbs.remove(f.getAbsolutePath());
                    }
                    mDropSelected.remove(f.getAbsolutePath());
                }
                if (deleted > 0) {
                    String msg = BulkStrings.format(this, BulkStrings.DELETED_TOAST, deleted);
                    BulkTheme.showToast(this, msg);
                    BulkLog.add(msg);
                }
                showDropFolder();
            });
    }

    /** Drop grid ka tile: tap = select toggle (inspector NAHI — ye sirf dropped view hai). */
    private View dropThumb(final File f) {
        final FrameLayout box = new FrameLayout(this);
        final String path = f.getAbsolutePath();
        final boolean checked = mDropSelected.contains(path);
        box.setBackground(BulkTheme.tileBg(this, checked));
        int gap = BulkTheme.dp(this, 8);
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = BulkTheme.dp(this, Math.max(56, Math.min(150, 300 / mDropCols)));
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
            ImageView iv = BulkTheme.icon(this, R.drawable.ic_bulk_image, BulkTheme.textMuted(this), 20);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
                BulkTheme.dp(this, 20), BulkTheme.dp(this, 20));
            ip.gravity = Gravity.CENTER;
            iv.setLayoutParams(ip);
            box.addView(iv);
        }
        TextView badge = new TextView(this);
        badge.setText("✓");
        badge.setTextColor(BulkTheme.onAccent(this));
        badge.setTextSize(11);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(BulkTheme.accentCircleBg(this));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 20), BulkTheme.dp(this, 20));
        bp.gravity = Gravity.TOP | Gravity.RIGHT;
        bp.topMargin = BulkTheme.dp(this, 4);
        bp.rightMargin = BulkTheme.dp(this, 4);
        badge.setLayoutParams(bp);
        badge.setVisibility(checked ? View.VISIBLE : View.GONE);
        box.addView(badge);
        box.setOnClickListener(v -> {
            if (mDropSelected.contains(path)) mDropSelected.remove(path);
            else mDropSelected.add(path);
            showDropFolder();
        });
        return box;
    }

    private View projectRow(final BulkProjects.ProjectInfo p, int mockups, int ready) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 11),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 11));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = BulkTheme.dp(this, 10);
        row.setLayoutParams(rlp);

        FrameLayout iconBox = new FrameLayout(this);
        iconBox.setBackground(BulkTheme.iconBoxBg(this));
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 34), BulkTheme.dp(this, 34)));
        ImageView iv = BulkTheme.icon(this, R.drawable.ic_bulk_phone, BulkTheme.accent(this), 17);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 17), BulkTheme.dp(this, 17));
        ip.gravity = Gravity.CENTER;
        iv.setLayoutParams(ip);
        iconBox.addView(iv);
        row.addView(iconBox);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = BulkTheme.dp(this, 11);
        texts.setLayoutParams(tp);
        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(14);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        texts.addView(name);
        TextView counts = new TextView(this);
        counts.setText(mockups + " Mockups · " + ready + " Ready");
        counts.setTextColor(BulkTheme.textMuted(this));
        counts.setTextSize(11);
        texts.addView(counts);
        row.addView(texts);

        FrameLayout arrow = new FrameLayout(this);
        arrow.setBackground(BulkTheme.arrowCircleBg(this));
        arrow.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 24), BulkTheme.dp(this, 24)));
        ImageView av = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 12);
        FrameLayout.LayoutParams avp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 12));
        avp.gravity = Gravity.CENTER;
        av.setLayoutParams(avp);
        arrow.addView(av);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            mOpenProject = p;
            mMockupsTab = true;
            showFolder();
        });
        return row;
    }

    // ------------------------------------------------------------ folder view

    private void showFolder() {
        final BulkProjects.ProjectInfo p = mOpenProject;
        if (p == null) { showList(); return; }
        mBody.removeAllViews();

        // Wapas row
        TextView back = new TextView(this);
        back.setText("‹  " + p.name);
        back.setTextColor(BulkTheme.accent(this));
        back.setTextSize(13.5f);
        back.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        back.setPadding(0, 0, 0, BulkTheme.dp(this, 8));
        back.setOnClickListener(v -> { mOpenProject = null; showList(); });
        mBody.addView(back);

        // Asal path header
        File dir = mMockupsTab ? BulkProjects.mockupsDir(p.number) : BulkProjects.readyDir(p.number);
        TextView path = new TextView(this);
        path.setText(dir.getAbsolutePath());
        path.setTextColor(BulkTheme.textMuted(this));
        path.setTextSize(10.5f);
        path.setTypeface(android.graphics.Typeface.MONOSPACE);
        mBody.addView(path);

        TextView hint = new TextView(this);
        hint.setText(BulkStrings.t(this, BulkStrings.TAP_TO_INSPECT));
        hint.setTextColor(BulkTheme.textMuted(this));
        hint.setTextSize(11);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = BulkTheme.dp(this, 4);
        hint.setLayoutParams(hp);
        mBody.addView(hint);

        // Tabs: Mockups/ | Wallpapers/
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = BulkTheme.dp(this, 10);
        tabs.setLayoutParams(tp);
        TextView tabM = tabButton(BulkStrings.t(this, BulkStrings.MOCKUPS_TAB), mMockupsTab,
            () -> { mMockupsTab = true; showFolder(); });
        tabM.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabs.addView(tabM);
        TextView tabW = tabButton(BulkStrings.t(this, BulkStrings.WALLPAPERS_TAB), !mMockupsTab,
            () -> { mMockupsTab = false; showFolder(); });
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wlp.leftMargin = BulkTheme.dp(this, 8);
        tabW.setLayoutParams(wlp);
        tabs.addView(tabW);
        mBody.addView(tabs);

        List<File> files = imageFiles(dir);
        if (files.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(BulkStrings.t(this, BulkStrings.NO_WALLPAPERS));
            none.setTextColor(BulkTheme.textMuted(this));
            none.setTextSize(12);
            none.setPadding(0, BulkTheme.dp(this, 12), 0, 0);
            mBody.addView(none);
        } else {
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(2);
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gp.topMargin = BulkTheme.dp(this, 10);
            grid.setLayoutParams(gp);
            for (File f : files) grid.addView(assetThumb(f));
            mBody.addView(grid);
            loadThumbs(files);
        }
        BulkFonts.applyToTree(this, mBody);
    }

    private TextView tabButton(String text, boolean active, final Runnable onTap) {
        TextView t = active
            ? BulkTheme.primaryButton(this, text, null)
            : BulkTheme.secondaryButton(this, text, null);
        t.setOnClickListener(v -> { if (onTap != null) onTap.run(); });
        return t;
    }

    private View assetThumb(final File f) {
        FrameLayout box = new FrameLayout(this);
        box.setBackground(BulkTheme.tileBg(this, false));
        int gap = BulkTheme.dp(this, 8);
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = BulkTheme.dp(this, 150);
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
            ImageView iv = BulkTheme.icon(this, R.drawable.ic_bulk_image, BulkTheme.textMuted(this), 20);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
                BulkTheme.dp(this, 20), BulkTheme.dp(this, 20));
            ip.gravity = Gravity.CENTER;
            iv.setLayoutParams(ip);
            box.addView(iv);
        }
        box.setOnClickListener(v -> showInspector(f));
        return box;
    }

    // ------------------------------------------------------------ inspector

    private void showInspector(final File f) {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this, f.getName(), dialog);

        final ImageView big = new ImageView(this);
        big.setScaleType(ImageView.ScaleType.FIT_CENTER);
        big.setAdjustViewBounds(true);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 380));
        big.setLayoutParams(bp);
        panel.addView(big);

        TextView path = new TextView(this);
        path.setText(f.getAbsolutePath());
        path.setTextColor(BulkTheme.textMuted(this));
        path.setTextSize(10.5f);
        path.setTypeface(android.graphics.Typeface.MONOSPACE);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = BulkTheme.dp(this, 8);
        path.setLayoutParams(pp);
        panel.addView(path);

        TextView ok = BulkTheme.primaryButton(this, BulkStrings.t(this, BulkStrings.OK),
            v -> dialog.dismiss());
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        op.topMargin = BulkTheme.dp(this, 12);
        ok.setLayoutParams(op);
        panel.addView(ok);

        BulkTheme.showPanelDialog(this, panel);
        new Thread(() -> {
            Bitmap bmp = decodeFileThumb(f, 900);
            if (bmp != null) runOnUiThread(() -> big.setImageBitmap(bmp));
        }).start();
    }

    // ------------------------------------------------------------ helpers

    private void loadThumbs(final List<File> files) {
        final int gen = ++mThumbGen;
        new Thread(() -> {
            for (File f : files) {
                String key = f.getAbsolutePath();
                if (mThumbs.containsKey(key)) continue;
                Bitmap bmp = decodeFileThumb(f, 300);
                if (bmp != null) {
                    mThumbs.put(key, bmp);
                    runOnUiThread(() -> {
                        if (gen != mThumbGen) return;
                        if (mDropProject != null) showDropFolder();
                        else if (mOpenProject != null) showFolder();
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
            // v129: count wala usool hi yahan bhi — hidden files grid
            // me bhi nahi (count aur grid kabhi alag na hon)
            if (k.isFile() && BulkProjects.isCountableWallpaper(k.getName())) out.add(k);
        }
        java.util.Collections.sort(out, (a, b) ->
            a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
        return out;
    }
}
