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
 * Delete + design-style confirm).
 *
 * v132 (owner ki hidayat): folder view me TEEsra tab "Sequence"
 * (mockup + us ka ready jora tartib se), teeno tabs par Sort + Grid
 * aur SELECT + DELETE (toolbar Drop ke sath shared hai). Toolbar ki
 * halat: default me sirf "Select" button; selection mode me
 * Select All + Done; Delete sirf kuch selected ho tab. Tile par
 * default me tap = bara preview (inspector), selection mode me
 * tap = select toggle.
 */
public class BulkReadyAssetsActivity extends Activity {

    private static final String LOG_TAG = "BulkReadyAssets";

    private List<BulkProjects.ProjectInfo> mProjects = new ArrayList<>();
    private final Map<String, Bitmap> mThumbs = new HashMap<>();
    private int mThumbGen = 0;

    private LinearLayout mBody;
    private BulkProjects.ProjectInfo mOpenProject;
    // v132: folder view ke 3 tabs — Mockups / Wallpapers / Sequence
    private static final int TAB_MOCKUPS = 0;
    private static final int TAB_WALLPAPERS = 1;
    private static final int TAB_SEQUENCE = 2;
    private int mTab = TAB_MOCKUPS;
    // v132: folder tabs ki selection (absolute paths) + toolbar halat
    private final Set<String> mAssetSelected = new HashSet<>();
    private int mAssetSort = BulkGalleryState.SORT_NEWEST;
    private int mAssetCols = BulkGalleryState.DEFAULT_GRID;
    // v132: Preview surfaces ka selection mode — Select button se ON,
    // Done se OFF (selection saaf); surface badalne par reset
    private boolean mSelecting = false;

    /** Selection mode + dono selections reset (surface change par). */
    private void resetSelectionUi() {
        mSelecting = false;
        mAssetSelected.clear();
        mDropSelected.clear();
    }
    // v129: Drop Wallpapers wale hissay ki halat
    // v133: Drop Wallpapers ab LIVE staging dikhata hai (neeche dekho)
    private boolean mDropList = false;
    // v130: Drop Wallpapers selection (staged URIs) + toolbar halat
    private final Set<String> mDropSelected = new HashSet<>();
    private int mDropSort = BulkGalleryState.SORT_NEWEST;
    private int mDropCols = BulkGalleryState.DEFAULT_GRID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        // v133: Sort/Grid shared persisted prefs (gallery wali hi keys)
        android.content.SharedPreferences gp = getSharedPreferences(
            BulkGalleryState.PREFS_NAME, MODE_PRIVATE);
        int savedSort = BulkGalleryState.resolveSortPref(
            gp.getInt(BulkGalleryState.KEY_SORT, BulkGalleryState.PREF_ABSENT));
        int savedGrid = BulkGalleryState.resolveGridPref(
            gp.getInt(BulkGalleryState.KEY_GRID, BulkGalleryState.PREF_ABSENT));
        mAssetSort = savedSort;
        mDropSort = savedSort;
        mAssetCols = savedGrid;
        mDropCols = savedGrid;
        mProjects = loadProjects();
        setContentView(buildUi());
        BulkFonts.apply(this);
        showList();
    }

    private void saveSortPref(int mode) {
        getSharedPreferences(BulkGalleryState.PREFS_NAME, MODE_PRIVATE).edit()
            .putInt(BulkGalleryState.KEY_SORT, mode).apply();
    }

    private void saveGridPref(int cols) {
        getSharedPreferences(BulkGalleryState.PREFS_NAME, MODE_PRIVATE).edit()
            .putInt(BulkGalleryState.KEY_GRID, cols).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // v129: counts FRESH — bahar se folders badle hon to bhi yahan
        // ke number purane na rahen (koi cached count nahi)
        mProjects = loadProjects();
        if (mDropList) showStagedDrop();
        else if (mOpenProject != null) showFolder();
        else showList();
    }

    @Override
    public void onBackPressed() {
        // Folder view khula ho to pehle list par wapas
        if (mDropList) {
            mDropList = false;
            resetSelectionUi();
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

    /**
     * Main list ke sab se upar wali entry row — tap par staged
     * images ki list. v133: count = LIVE staging size (owner ki
     * shikayat: staged images yahan nazar hi nahi ate thay).
     */
    private View dropEntryRow() {
        int total = stagedItems().size();
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
            resetSelectionUi();
            showStagedDrop();
        });
        return row;
    }

    // ------------------------------------------------------------ v133 staged Drop Wallpapers

    /** LIVE staging (BulkStageActivity ke sath shared prefs se, hamesha taaza). */
    private java.util.List<BulkStaging.Item> stagedItems() {
        try {
            String data = getSharedPreferences(BulkStaging.PREFS_NAME, MODE_PRIVATE)
                .getString(BulkStaging.KEY_ITEMS, "");
            return BulkStaging.decode(data);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void persistStaged(java.util.List<BulkStaging.Item> items) {
        getSharedPreferences(BulkStaging.PREFS_NAME, MODE_PRIVATE).edit()
            .putString(BulkStaging.KEY_ITEMS, BulkStaging.encode(items))
            .apply();
    }

    /** Gallery wala sort mode -> staging wala sort mode. */
    private int stagingSortMode() {
        switch (mDropSort) {
            case BulkGalleryState.SORT_OLDEST: return BulkStaging.SORT_DATE_ASC;
            case BulkGalleryState.SORT_NAME: return BulkStaging.SORT_NAME_ASC;
            default: return BulkStaging.SORT_DATE_DESC;
        }
    }

    /**
     * Drop Wallpapers ka andar wala view (v133): TAMAM staged
     * images — kisi project selection ki pabandi NAHI (yehi asal
     * defect tha: staged images yahan dikhte hi nahi thay).
     * Toolbar/grid ki shakal bilkul wohi jo folder tabs ki hai;
     * Delete yahan staging se hatata hai (asal photo mehfooz
     * rehti hai — staging sirf link hai, Stage screen wala usool).
     */
    private void showStagedDrop() {
        mBody.removeAllViews();

        TextView back = new TextView(this);
        back.setText("‹  " + BulkStrings.t(this, BulkStrings.PREVIEW_READY));
        back.setTextColor(BulkTheme.accent(this));
        back.setTextSize(13.5f);
        back.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        back.setPadding(0, 0, 0, BulkTheme.dp(this, 8));
        back.setOnClickListener(v -> { mDropList = false; resetSelectionUi(); showList(); });
        mBody.addView(back);

        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, BulkStrings.STAGED_DROP_SUB));
        title.setTextColor(BulkTheme.textMuted(this));
        title.setTextSize(11.5f);
        title.setPadding(0, 0, 0, BulkTheme.dp(this, 8));
        mBody.addView(title);

        final java.util.List<BulkStaging.Item> items =
            BulkStaging.sorted(stagedItems(), stagingSortMode());
        Set<String> existing = new HashSet<>();
        for (BulkStaging.Item it : items) existing.add(it.uri);
        mDropSelected.retainAll(existing);

        if (items.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(BulkStrings.t(this, BulkStrings.NO_WALLPAPERS));
            none.setTextColor(BulkTheme.textMuted(this));
            none.setTextSize(12);
            none.setPadding(0, BulkTheme.dp(this, 12), 0, 0);
            mBody.addView(none);
        } else {
            mBody.addView(stagedToolbar(items));
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(mDropCols);
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gp.topMargin = BulkTheme.dp(this, 8);
            grid.setLayoutParams(gp);
            for (BulkStaging.Item it : items) {
                grid.addView(stagedThumb(it));
            }
            mBody.addView(grid);
            loadStagedThumbs(items);
        }
        BulkFonts.applyToTree(this, mBody);
    }

    /** Staged view ka toolbar — folder tabs wale selectToolbar ki hum-shakal. */
    private View stagedToolbar(final java.util.List<BulkStaging.Item> items) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = BulkTheme.dp(this, 10);
        bar.setLayoutParams(blp);

        int selCount = 0;
        for (BulkStaging.Item it : items) if (mDropSelected.contains(it.uri)) selCount++;
        BulkGalleryState.SelectionBar state =
            BulkGalleryState.selectionBar(mSelecting, selCount);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        TextView count = new TextView(this);
        count.setText(mSelecting
            ? BulkStrings.format(this, BulkStrings.SELECTED_COUNT, selCount)
            : BulkStrings.format(this, BulkStrings.ITEMS_COUNT, items.size()));
        count.setTextColor(BulkTheme.accent(this));
        count.setTextSize(12.5f);
        count.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row1.addView(count);
        row1.addView(new View(this), new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (state.showSelect) {
            TextView selectBtn = pillText(BulkStrings.t(this, BulkStrings.SELECT), false);
            selectBtn.setOnClickListener(v -> { mSelecting = true; showStagedDrop(); });
            row1.addView(selectBtn);
        }
        if (state.showSelectAll) {
            final int fSelCount = selCount;
            TextView selectAll = pillText(BulkStrings.t(this,
                BulkGalleryState.isAllSelected(selCount, items.size())
                    ? BulkStrings.UNSELECT_ALL : BulkStrings.SELECT_ALL), false);
            selectAll.setOnClickListener(v -> {
                if (BulkGalleryState.isAllSelected(fSelCount, items.size())) {
                    for (BulkStaging.Item it : items) mDropSelected.remove(it.uri);
                } else {
                    for (BulkStaging.Item it : items) mDropSelected.add(it.uri);
                }
                showStagedDrop();
            });
            row1.addView(selectAll);
        }
        if (state.showDone) {
            TextView done = pillText(BulkStrings.t(this, BulkStrings.DONE_BTN), false);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.leftMargin = BulkTheme.dp(this, 6);
            done.setLayoutParams(dlp);
            done.setOnClickListener(v -> {
                mSelecting = false;
                mDropSelected.clear();
                showStagedDrop();
            });
            row1.addView(done);
        }
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
            saveSortPref(mDropSort);
            showStagedDrop();
        });
        row2.addView(sortPill);
        LinearLayout gridPill = pillIcon(R.drawable.ic_bulk_grid, mDropCols + "x");
        gridPill.setOnClickListener(v -> {
            mDropCols = BulkGalleryState.nextColumns(mDropCols);
            saveGridPref(mDropCols);
            showStagedDrop();
        });
        LinearLayout.LayoutParams gp2 = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp2.leftMargin = BulkTheme.dp(this, 6);
        gridPill.setLayoutParams(gp2);
        row2.addView(gridPill);
        if (state.showDelete) {
            row2.addView(new View(this), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView del = pillText(BulkStrings.t(this, BulkStrings.DELETE)
                + " (" + selCount + ")", true);
            del.setOnClickListener(v -> confirmUnstage(items));
            row2.addView(del);
        }
        bar.addView(row2);
        return bar;
    }

    /** Select kiye hue staged items ko staging se hatao (confirm ke baad). */
    private void confirmUnstage(final java.util.List<BulkStaging.Item> shown) {
        final java.util.List<BulkStaging.Item> sel = new ArrayList<>();
        for (BulkStaging.Item it : shown) {
            if (mDropSelected.contains(it.uri)) sel.add(it);
        }
        if (sel.isEmpty()) return;
        BulkTheme.showConfirm(this, BulkStrings.t(this, BulkStrings.DELETE_DROPPED_TITLE),
            BulkStrings.format(this, BulkStrings.DELETE_DROPPED_MSG, sel.size()),
            BulkStrings.t(this, BulkStrings.DELETE), true, () -> {
                Set<String> remove = new HashSet<>();
                for (BulkStaging.Item it : sel) remove.add(it.uri);
                java.util.List<BulkStaging.Item> keep = new ArrayList<>();
                for (BulkStaging.Item it : stagedItems()) {
                    if (!remove.contains(it.uri)) keep.add(it);
                }
                persistStaged(keep);
                for (BulkStaging.Item it : sel) mThumbs.remove(it.uri);
                mDropSelected.clear();
                String msg = BulkStrings.format(this, BulkStrings.DELETED_TOAST, sel.size());
                BulkTheme.showToast(this, msg);
                BulkLog.add(msg);
                showStagedDrop();
            });
    }

    /** Staged grid ki tile — selectThumb ki hum-shakal (URI thumb ke sath). */
    private View stagedThumb(final BulkStaging.Item item) {
        final FrameLayout box = new FrameLayout(this);
        final String key = item.uri;
        final boolean checked = mDropSelected.contains(key);
        box.setBackground(BulkTheme.tileBg(this, checked));
        int gap = BulkTheme.dp(this, 8);
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = BulkTheme.dp(this, Math.max(56, Math.min(150, 300 / mDropCols)));
        glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        glp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
        box.setLayoutParams(glp);
        Bitmap bmp = mThumbs.get(key);
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
            if (mSelecting) {
                if (mDropSelected.contains(key)) mDropSelected.remove(key);
                else mDropSelected.add(key);
                showStagedDrop();
            }
        });
        return box;
    }

    /** Staged items ke thumbs content URIs se (Stage screen wala hi tareeqa). */
    private void loadStagedThumbs(final java.util.List<BulkStaging.Item> items) {
        final int gen = ++mThumbGen;
        new Thread(() -> {
            for (BulkStaging.Item it : items) {
                if (mThumbs.containsKey(it.uri)) continue;
                Bitmap bmp = decodeUriThumb(it.uri, 300);
                if (bmp != null) {
                    mThumbs.put(it.uri, bmp);
                    runOnUiThread(() -> {
                        if (gen == mThumbGen && mDropList) showStagedDrop();
                    });
                }
            }
        }).start();
    }

    private Bitmap decodeUriThumb(String uriStr, int reqSize) {
        try {
            android.net.Uri uri = android.net.Uri.parse(uriStr);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, bounds);
            in.close();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, reqSize);
            in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------ select toolbar (shared)

    /**
     * Preview surfaces ka selection toolbar (compact, v132):
     *   Default: (kul items | Select) / (Sort | Grid)
     *   Selection mode: (N selected | Select All | Done) /
     *   (Sort | Grid | Delete (n) — sirf jab kuch selected ho)
     * Halat BulkGalleryState.selectionBar (pure) se ati hai. Drop
     * Wallpapers aur folder tabs dono ke liye EK implementation.
     */
    private View selectToolbar(final List<File> files, final Set<String> selected,
                               int sortMode, int cols, boolean selecting,
                               final Runnable onSortCycle, final Runnable onGridCycle,
                               final Runnable onRefresh, final Runnable onDelete,
                               final Runnable onEnterSelect, final Runnable onDone) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = BulkTheme.dp(this, 10);
        bar.setLayoutParams(blp);

        int selCount = 0;
        for (File f : files) if (selected.contains(f.getAbsolutePath())) selCount++;
        BulkGalleryState.SelectionBar state =
            BulkGalleryState.selectionBar(selecting, selCount);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        TextView count = new TextView(this);
        count.setText(selecting
            ? BulkStrings.format(this, BulkStrings.SELECTED_COUNT, selCount)
            : BulkStrings.format(this, BulkStrings.ITEMS_COUNT, files.size()));
        count.setTextColor(BulkTheme.accent(this));
        count.setTextSize(12.5f);
        count.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row1.addView(count);
        row1.addView(new View(this), new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (state.showSelect) {
            TextView selectBtn = pillText(BulkStrings.t(this, BulkStrings.SELECT), false);
            selectBtn.setOnClickListener(v -> onEnterSelect.run());
            row1.addView(selectBtn);
        }
        if (state.showSelectAll) {
            final int fSelCount = selCount;
            TextView selectAll = pillText(BulkStrings.t(this,
                BulkGalleryState.isAllSelected(selCount, files.size())
                    ? BulkStrings.UNSELECT_ALL : BulkStrings.SELECT_ALL), false);
            selectAll.setOnClickListener(v -> {
                if (BulkGalleryState.isAllSelected(fSelCount, files.size())) {
                    for (File f : files) selected.remove(f.getAbsolutePath());
                } else {
                    for (File f : files) selected.add(f.getAbsolutePath());
                }
                onRefresh.run();
            });
            row1.addView(selectAll);
        }
        if (state.showDone) {
            TextView done = pillText(BulkStrings.t(this, BulkStrings.DONE_BTN), false);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.leftMargin = BulkTheme.dp(this, 6);
            done.setLayoutParams(dlp);
            done.setOnClickListener(v -> onDone.run());
            row1.addView(done);
        }
        bar.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r2p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r2p.topMargin = BulkTheme.dp(this, 8);
        row2.setLayoutParams(r2p);
        LinearLayout sortPill = pillIcon(R.drawable.ic_bulk_sort, BulkStrings.t(this,
            sortMode == BulkGalleryState.SORT_OLDEST ? BulkStrings.SORT_OLDEST
                : sortMode == BulkGalleryState.SORT_NAME ? BulkStrings.SORT_BY_NAME
                : BulkStrings.SORT_NEWEST));
        sortPill.setOnClickListener(v -> onSortCycle.run());
        row2.addView(sortPill);
        LinearLayout gridPill = pillIcon(R.drawable.ic_bulk_grid, cols + "x");
        gridPill.setOnClickListener(v -> onGridCycle.run());
        LinearLayout.LayoutParams gp2 = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp2.leftMargin = BulkTheme.dp(this, 6);
        gridPill.setLayoutParams(gp2);
        row2.addView(gridPill);
        if (state.showDelete) {
            row2.addView(new View(this), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView del = pillText(BulkStrings.t(this, BulkStrings.DELETE)
                + " (" + selCount + ")", true);
            del.setOnClickListener(v -> onDelete.run());
            row2.addView(del);
        }
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

    /** Design-style confirm ke baad select ki hui files delete (Drop + folder tabs shared). */
    private void confirmDeleteFiles(final List<File> files, final Set<String> selected,
                                    String titleKey, String msgKey, final Runnable onRefresh) {
        final List<File> sel = new ArrayList<>();
        for (File f : files) {
            if (selected.contains(f.getAbsolutePath())) sel.add(f);
        }
        if (sel.isEmpty()) return;
        BulkTheme.showConfirm(this, BulkStrings.t(this, titleKey),
            BulkStrings.format(this, msgKey, sel.size()),
            BulkStrings.t(this, BulkStrings.DELETE), true, () -> {
                int deleted = 0;
                for (File f : sel) {
                    if (f.delete()) {
                        deleted++;
                        mThumbs.remove(f.getAbsolutePath());
                    }
                    selected.remove(f.getAbsolutePath());
                }
                if (deleted > 0) {
                    String msg = BulkStrings.format(this, BulkStrings.DELETED_TOAST, deleted);
                    BulkTheme.showToast(this, msg);
                    BulkLog.add(msg);
                }
                onRefresh.run();
            });
    }

    /**
     * Selectable grid tile (Drop + folder tabs shared): selection
     * mode me tap = select toggle; default mode me tap = inspector
     * (agar diya ho). Long-press = inspector dono halton me.
     */
    private View selectThumb(final File f, final Set<String> selected, int cols,
                             boolean selecting,
                             final Runnable onRefresh, final Runnable onInspect) {
        final FrameLayout box = new FrameLayout(this);
        final String path = f.getAbsolutePath();
        final boolean checked = selected.contains(path);
        box.setBackground(BulkTheme.tileBg(this, checked));
        int gap = BulkTheme.dp(this, 8);
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = BulkTheme.dp(this, Math.max(56, Math.min(150, 300 / cols)));
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
            if (selecting) {
                if (selected.contains(path)) selected.remove(path);
                else selected.add(path);
                onRefresh.run();
            } else if (onInspect != null) {
                onInspect.run();
            }
        });
        if (onInspect != null) {
            box.setOnLongClickListener(v -> {
                onInspect.run();
                return true;
            });
        }
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
            mTab = TAB_MOCKUPS;
            resetSelectionUi();
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
        back.setOnClickListener(v -> { mOpenProject = null; resetSelectionUi(); showList(); });
        mBody.addView(back);

        // Asal path header (Sequence par project folder)
        File dir = mTab == TAB_MOCKUPS ? BulkProjects.mockupsDir(p.number)
            : mTab == TAB_WALLPAPERS ? BulkProjects.readyDir(p.number)
            : BulkProjects.projectDir(p.number);
        TextView path = new TextView(this);
        path.setText(dir.getAbsolutePath());
        path.setTextColor(BulkTheme.textMuted(this));
        path.setTextSize(10.5f);
        path.setTypeface(android.graphics.Typeface.MONOSPACE);
        mBody.addView(path);

        TextView hint = new TextView(this);
        hint.setText(BulkStrings.t(this, BulkStrings.TAP_SELECT_HINT));
        hint.setTextColor(BulkTheme.textMuted(this));
        hint.setTextSize(11);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = BulkTheme.dp(this, 4);
        hint.setLayoutParams(hp);
        mBody.addView(hint);

        // Tabs: Mockups/ | Wallpapers/ | Sequence
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = BulkTheme.dp(this, 10);
        tabs.setLayoutParams(tp);
        TextView tabM = tabButton(BulkStrings.t(this, BulkStrings.MOCKUPS_TAB), mTab == TAB_MOCKUPS,
            () -> { mTab = TAB_MOCKUPS; showFolder(); });
        tabM.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabs.addView(tabM);
        TextView tabW = tabButton(BulkStrings.t(this, BulkStrings.WALLPAPERS_TAB), mTab == TAB_WALLPAPERS,
            () -> { mTab = TAB_WALLPAPERS; showFolder(); });
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wlp.leftMargin = BulkTheme.dp(this, 8);
        tabW.setLayoutParams(wlp);
        tabs.addView(tabW);
        TextView tabS = tabButton(BulkStrings.t(this, BulkStrings.SEQUENCE_TAB), mTab == TAB_SEQUENCE,
            () -> { mTab = TAB_SEQUENCE; showFolder(); });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = BulkTheme.dp(this, 8);
        tabS.setLayoutParams(slp);
        tabs.addView(tabS);
        mBody.addView(tabs);

        // Is tab ki display list (sort teeno tabs par lagta hai)
        final List<File> files = mTab == TAB_MOCKUPS
            ? BulkGalleryState.sortFiles(imageFiles(BulkProjects.mockupsDir(p.number)), mAssetSort)
            : mTab == TAB_WALLPAPERS
            ? BulkGalleryState.sortFiles(imageFiles(BulkProjects.readyDir(p.number)), mAssetSort)
            : sequenceFiles(p);
        // Selection sirf un files ki rakho jo dono folders me ab bhi hain
        Set<String> existing = new HashSet<>();
        for (File f : imageFiles(BulkProjects.mockupsDir(p.number))) existing.add(f.getAbsolutePath());
        for (File f : imageFiles(BulkProjects.readyDir(p.number))) existing.add(f.getAbsolutePath());
        mAssetSelected.retainAll(existing);

        if (files.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(BulkStrings.t(this, BulkStrings.NO_WALLPAPERS));
            none.setTextColor(BulkTheme.textMuted(this));
            none.setTextSize(12);
            none.setPadding(0, BulkTheme.dp(this, 12), 0, 0);
            mBody.addView(none);
        } else {
            mBody.addView(selectToolbar(files, mAssetSelected, mAssetSort, mAssetCols, mSelecting,
                () -> { mAssetSort = BulkGalleryState.nextSort(mAssetSort); saveSortPref(mAssetSort); showFolder(); },
                () -> { mAssetCols = BulkGalleryState.nextColumns(mAssetCols); saveGridPref(mAssetCols); showFolder(); },
                this::showFolder,
                () -> confirmDeleteFiles(files, mAssetSelected,
                    BulkStrings.DELETE_ASSETS_TITLE, BulkStrings.DELETE_ASSETS_MSG,
                    this::showFolder),
                () -> { mSelecting = true; showFolder(); },
                () -> { mSelecting = false; mAssetSelected.clear(); showFolder(); }));
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(mAssetCols);
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gp.topMargin = BulkTheme.dp(this, 8);
            grid.setLayoutParams(gp);
            for (final File f : files) {
                grid.addView(selectThumb(f, mAssetSelected, mAssetCols, mSelecting,
                    this::showFolder, () -> showInspector(f)));
            }
            mBody.addView(grid);
            loadThumbs(files);
        }
        BulkFonts.applyToTree(this, mBody);
    }

    /**
     * Sequence tab ki list: jora = folders ki NAAM tartib me nth
     * mockup + nth ready (BulkGalleryState.interleavePairs). Sort
     * Newest/Oldest ho to jore mockup ki mtime se hilte hain — jora
     * kabhi nahi tootta; Name par canonical tartib hi rehti hai.
     */
    private List<File> sequenceFiles(BulkProjects.ProjectInfo p) {
        List<File> mockups = BulkGalleryState.sortFiles(
            imageFiles(BulkProjects.mockupsDir(p.number)), BulkGalleryState.SORT_NAME);
        List<File> ready = BulkGalleryState.sortFiles(
            imageFiles(BulkProjects.readyDir(p.number)), BulkGalleryState.SORT_NAME);
        if (mAssetSort == BulkGalleryState.SORT_NAME) {
            return BulkGalleryState.interleavePairs(mockups, ready);
        }
        List<File[]> pairs = new ArrayList<>();
        int n = Math.max(mockups.size(), ready.size());
        for (int i = 0; i < n; i++) {
            pairs.add(new File[]{
                i < mockups.size() ? mockups.get(i) : null,
                i < ready.size() ? ready.get(i) : null});
        }
        final boolean newest = mAssetSort == BulkGalleryState.SORT_NEWEST;
        pairs.sort((a, b) -> {
            File ka = a[0] != null ? a[0] : a[1];
            File kb = b[0] != null ? b[0] : b[1];
            long ta = ka != null ? ka.lastModified() : 0L;
            long tb = kb != null ? kb.lastModified() : 0L;
            return newest ? Long.compare(tb, ta) : Long.compare(ta, tb);
        });
        List<File> flat = new ArrayList<>();
        for (File[] pair : pairs) {
            if (pair[0] != null) flat.add(pair[0]);
            if (pair[1] != null) flat.add(pair[1]);
        }
        return flat;
    }

    private TextView tabButton(String text, boolean active, final Runnable onTap) {
        TextView t = active
            ? BulkTheme.primaryButton(this, text, null)
            : BulkTheme.secondaryButton(this, text, null);
        t.setOnClickListener(v -> { if (onTap != null) onTap.run(); });
        return t;
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
                        if (mOpenProject != null) showFolder();
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
