package com.termux.app;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.R;
import com.termux.shared.logger.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod (v127, v130 me toolbar dobara) — App ki apni Gallery
 * (picking ka primary route). MediaStore se device ki images, pehle
 * FOLDER list, folder tap karne par us ki images grid me ("All Images"
 * view bhi hai).
 *
 * v130 TOOLBAR (owner ke apne HTML design — All Images Gallery
 * Manager — ke mutabiq, 2 rows + hint bar):
 *   Row 1: "N selected" | Select All / Unselect All | Add/Move CTA
 *          (0 select par disabled; Copy mode me "Add (n)", Move mode
 *          me "Move (n)")
 *   Row 2: Sort (Newest -> Oldest -> Name cycle) | Grid (ek line me
 *          2 -> 3 -> 4 -> 5 -> 6 images cycle) | Copy / Move toggle
 *   Hint bar: swipe tip + kul items count.
 * Selection IMAGE ID par hoti hai (sort badalne se selection nahi
 * tutti). Move mode confirm par EXTRA_MOVE=true jata hai — asal
 * original-delete BulkStageActivity commit (copy verify) ke baad
 * karti hai.
 *
 * Selection FAST hai: tap = ek toggle; SWIPE (paint-style, v129) ya
 * LONG-PRESS + DRAG = poora swept range select (BulkGallerySelect);
 * drag ke doran kinare par grid auto-scroll bhi hota hai. Confirm par
 * tamam selected URIs BulkStageActivity ko wapas — wahan se wohi
 * purana staging + Duplicates SHA-256 flow chalta hai.
 *
 * Performance: grid me sirf chhote sampled thumbnails (LruCache +
 * background decode, full-size kabhi nahi).
 *
 * Permission: app ke paas first-run chain se storage/media access hota
 * hai; MediaStore parhna na ho sake to screen band ho jati hai or
 * Stage par System Picker secondary option ke tor par mojood rehta hai.
 */
public class BulkGalleryActivity extends Activity {

    private static final String LOG_TAG = "BulkGallery";
    public static final String EXTRA_URIS = "gallery_uris";
    /** v130: confirm ke waqt mode Move tha — Stage in URIs par move-intent lagati hai. */
    public static final String EXTRA_MOVE = "gallery_move";

    private static final class Bucket {
        final String id;
        final String name;
        int count;
        long coverId;
        Bucket(String id, String name) { this.id = id; this.name = name; }
    }

    private final List<Bucket> mBuckets = new ArrayList<>();
    private int mTotalCount = 0;

    // Grid state — entries DISPLAY order me; selection image ID par
    private boolean mInGrid = false;
    private String mGridTitle = "";
    private List<BulkGalleryState.Entry> mEntries = new ArrayList<>();
    private final Set<Long> mSelectedIds = new HashSet<>();
    private GridView mGrid;
    private GalleryAdapter mAdapter;
    private int mCols = BulkGalleryState.MIN_COLUMNS;
    private int mSortMode = BulkGalleryState.SORT_NEWEST;
    private int mMode = BulkGalleryState.MODE_COPY;

    // Toolbar refs (refresh ke liye)
    private TextView mCountLabel;
    private TextView mSelectAllBtn;
    private TextView mConfirmBtn;
    private TextView mSortLabel;
    private TextView mGridLabel;
    private LinearLayout mCopyPill;
    private LinearLayout mMovePill;
    private ImageView mCopyIcon;
    private ImageView mMoveIcon;
    private TextView mCopyLabel;
    private TextView mMoveLabel;
    private TextView mTotalLabel;

    // Drag-select state (positions — display order par hisaab)
    private boolean mDragging = false;
    private int mDragAnchor = -1;
    private int mDragCurrent = -1;
    private Set<Integer> mDragBase = new HashSet<>();
    // v129: paint-style swipe select — ungli ghaseetne ki shuruaat
    private int mDownPos = -1;
    private float mDownX = -1;
    private float mDownY = -1;

    private final LruCache<Long, Bitmap> mThumbCache = new LruCache<>(240);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        new Thread(() -> {
            boolean ok = loadBuckets();
            runOnUiThread(() -> {
                if (!ok || (mBuckets.isEmpty() && mTotalCount == 0)) {
                    BulkTheme.showToast(this, BulkStrings.t(this, BulkStrings.GALLERY_EMPTY));
                    finish();
                    return;
                }
                showFolders();
            });
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (mInGrid) {
            mInGrid = false;
            mSelectedIds.clear();
            showFolders();
            return;
        }
        super.onBackPressed();
    }

    // ------------------------------------------------------------ MediaStore

    private boolean loadBuckets() {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME},
                null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (c == null) return false;
            Map<String, Bucket> map = new LinkedHashMap<>();
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String bid = c.getString(1);
                String name = c.getString(2);
                if (bid == null) bid = "";
                if (name == null || name.isEmpty()) name = "Images";
                Bucket b = map.get(bid);
                if (b == null) {
                    b = new Bucket(bid, name);
                    b.coverId = id;
                    map.put(bid, b);
                }
                b.count++;
                mTotalCount++;
            }
            mBuckets.addAll(map.values());
            java.util.Collections.sort(mBuckets, (a, b) ->
                a.name.toLowerCase().compareTo(b.name.toLowerCase()));
            return true;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "MediaStore buckets nahi mile: " + e);
            return false;
        } finally {
            if (c != null) c.close();
        }
    }

    /** Ek bucket (ya tamam) ki entries — mojooda sort ke mutabiq display order me. */
    private List<BulkGalleryState.Entry> loadEntries(String bucketId) {
        List<BulkGalleryState.Entry> out = new ArrayList<>();
        Cursor c = null;
        try {
            String sel = null;
            String[] args = null;
            if (bucketId != null) {
                sel = MediaStore.Images.Media.BUCKET_ID + "=?";
                args = new String[]{bucketId};
            }
            c = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED},
                sel, args, MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    String name = c.getString(1);
                    out.add(new BulkGalleryState.Entry(
                        c.getLong(0), name != null ? name : "", c.getLong(2)));
                }
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "MediaStore images nahi mili: " + e);
        } finally {
            if (c != null) c.close();
        }
        return BulkGalleryState.sorted(out, mSortMode);
    }

    private static Uri uriFor(long id) {
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
    }

    // ------------------------------------------------------------ folder list

    private void showFolders() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this, BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS),
            BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS_SUBTITLE)));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll);

        body.addView(folderRow(null, BulkStrings.t(this, BulkStrings.ALL_IMAGES), mTotalCount, -1L));
        for (Bucket b : mBuckets) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = BulkTheme.dp(this, 10);
            body.addView(folderRow(b.id, b.name, b.count, b.coverId), lp);
        }
        setContentView(root);
        BulkFonts.apply(this);
    }

    private View folderRow(final String bucketId, String name, int count, long coverId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 10));

        final ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackground(BulkTheme.iconBoxBg(this));
        cover.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 44), BulkTheme.dp(this, 44)));
        row.addView(cover);
        if (coverId >= 0) loadThumb(coverId, cover);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tp.leftMargin = BulkTheme.dp(this, 11);
        texts.setLayoutParams(tp);
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextColor(BulkTheme.textMain(this));
        tv.setTextSize(14);
        texts.addView(tv);
        TextView cnt = new TextView(this);
        cnt.setText(BulkStrings.format(this, BulkStrings.WALLPAPERS_COUNT, count));
        cnt.setTextColor(BulkTheme.textMuted(this));
        cnt.setTextSize(11);
        texts.addView(cnt);
        row.addView(texts);

        row.setOnClickListener(v -> openGrid(bucketId, name));
        return row;
    }

    // ------------------------------------------------------------ grid

    private void openGrid(final String bucketId, final String title) {
        mInGrid = true;
        mGridTitle = title;
        mSelectedIds.clear();
        new Thread(() -> {
            final List<BulkGalleryState.Entry> entries = loadEntries(bucketId);
            runOnUiThread(() -> {
                mEntries = entries;
                buildGridUi();
            });
        }).start();
    }

    private void buildGridUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this, mGridTitle,
            BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS_SUBTITLE)));

        root.addView(buildToolbar());
        root.addView(buildHintBar());

        mGrid = new GridView(this);
        mGrid.setNumColumns(mCols);
        mGrid.setHorizontalSpacing(BulkTheme.dp(this, 4));
        mGrid.setVerticalSpacing(BulkTheme.dp(this, 4));
        mGrid.setPadding(BulkTheme.dp(this, 8), BulkTheme.dp(this, 4),
            BulkTheme.dp(this, 8), BulkTheme.dp(this, 8));
        mGrid.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        mAdapter = new GalleryAdapter();
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener((parent, view, position, id) -> {
            if (mDragging) return;
            toggle(position);
        });
        mGrid.setOnItemLongClickListener((parent, view, position, id) -> {
            mDragging = true;
            mDragAnchor = position;
            mDragCurrent = position;
            mDragBase = selectedPositions();
            applyDrag();
            return true;
        });
        mGrid.setOnTouchListener((v, e) -> {
            int action = e.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                // Paint-select ki tayyari: kahan se ungli chali — tap
                // abhi bhi aam click hi rehta hai (false wapas)
                mDownPos = mGrid.pointToPosition((int) e.getX(), (int) e.getY());
                mDownX = e.getX();
                mDownY = e.getY();
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (!mDragging) {
                    // PAINT SELECT (v129, barqarar): baghair long-press
                    // ke, ungli ghaseette hi selection shuru — jis image
                    // par se ungli guzre wo select hoti jati hai.
                    // Seedha neeche scroll selection NAHI banata (owner
                    // ke HTML wala usool: horizontal swipe = select,
                    // vertical = scroll).
                    if (mDownPos < 0) return false;
                    float dx = Math.abs(e.getX() - mDownX);
                    float dy = Math.abs(e.getY() - mDownY);
                    int slop = BulkTheme.dp(this, 10);
                    if (dx * dx + dy * dy < slop * slop) return false;
                    if (dx < dy * 0.9f) return false;
                    mDragging = true;
                    mDragAnchor = mDownPos;
                    mDragCurrent = mDownPos;
                    mDragBase = selectedPositions();
                    applyDrag();
                    return true;
                }
                int pos = mGrid.pointToPosition((int) e.getX(), (int) e.getY());
                if (pos >= 0 && pos != mDragCurrent) {
                    mDragCurrent = pos;
                    applyDrag();
                }
                // Kinare par ungli ho to grid khud scroll ho — lambi
                // list me bhi swipe-select rukta nahi
                int edge = BulkTheme.dp(this, 56);
                int step = BulkTheme.dp(this, 28);
                if (e.getY() < edge) mGrid.smoothScrollBy(-step, 0);
                else if (e.getY() > mGrid.getHeight() - edge) mGrid.smoothScrollBy(step, 0);
                return true;
            }
            if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL) {
                mDownPos = -1;
                if (mDragging) {
                    mDragging = false;
                    return true; // drag ke baad click fire na ho
                }
                return false;
            }
            return mDragging;
        });
        root.addView(mGrid);

        setContentView(root);
        BulkFonts.apply(this);
        refreshGridState();
    }

    // ------------------------------------------------------------ v130 toolbar

    /** HTML design ki 2-row toolbar: (count | Select All | CTA) / (Sort | Grid | Copy/Move). */
    private View buildToolbar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 10));

        // Row 1
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        mCountLabel = new TextView(this);
        mCountLabel.setTextColor(BulkTheme.accent(this));
        mCountLabel.setTextSize(14);
        mCountLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row1.addView(mCountLabel);
        row1.addView(new View(this), new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mSelectAllBtn = toolPillText(BulkStrings.t(this, BulkStrings.SELECT_ALL));
        mSelectAllBtn.setOnClickListener(v -> {
            if (BulkGalleryState.isAllSelected(mSelectedIds.size(), mEntries.size())) {
                mSelectedIds.clear();
            } else {
                for (BulkGalleryState.Entry e : mEntries) mSelectedIds.add(e.id);
            }
            refreshGridState();
        });
        row1.addView(mSelectAllBtn);

        mConfirmBtn = new TextView(this);
        mConfirmBtn.setTextColor(BulkTheme.onAccent(this));
        mConfirmBtn.setTextSize(13);
        mConfirmBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        mConfirmBtn.setGravity(Gravity.CENTER);
        mConfirmBtn.setBackground(BulkTheme.primaryBg(this));
        mConfirmBtn.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 9),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 9));
        mConfirmBtn.setClickable(true);
        mConfirmBtn.setOnClickListener(v -> confirmSelection());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.leftMargin = BulkTheme.dp(this, 8);
        mConfirmBtn.setLayoutParams(cp);
        row1.addView(mConfirmBtn);
        bar.addView(row1);

        // Row 2
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r2p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r2p.topMargin = BulkTheme.dp(this, 10);
        row2.setLayoutParams(r2p);

        LinearLayout sortPill = toolPill(R.drawable.ic_bulk_sort);
        mSortLabel = (TextView) sortPill.getChildAt(1);
        sortPill.setOnClickListener(v -> {
            mSortMode = BulkGalleryState.nextSort(mSortMode);
            mEntries = BulkGalleryState.sorted(mEntries, mSortMode);
            refreshGridState();
        });
        row2.addView(sortPill);

        LinearLayout gridPill = toolPill(R.drawable.ic_bulk_grid);
        mGridLabel = (TextView) gridPill.getChildAt(1);
        gridPill.setOnClickListener(v -> {
            mCols = BulkGalleryState.nextColumns(mCols);
            if (mGrid != null) mGrid.setNumColumns(mCols);
            refreshGridState();
        });
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.leftMargin = BulkTheme.dp(this, 6);
        gridPill.setLayoutParams(gp);
        row2.addView(gridPill);

        row2.addView(new View(this), new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mCopyPill = toolPill(R.drawable.ic_bulk_copy);
        mCopyIcon = (ImageView) mCopyPill.getChildAt(0);
        mCopyLabel = (TextView) mCopyPill.getChildAt(1);
        mCopyLabel.setText(BulkStrings.t(this, BulkStrings.COPY_MODE));
        mCopyPill.setOnClickListener(v -> {
            mMode = BulkGalleryState.MODE_COPY;
            refreshGridState();
        });
        row2.addView(mCopyPill);

        mMovePill = toolPill(R.drawable.ic_bulk_chevron);
        mMoveIcon = (ImageView) mMovePill.getChildAt(0);
        mMoveLabel = (TextView) mMovePill.getChildAt(1);
        mMoveLabel.setText(BulkStrings.t(this, BulkStrings.MOVE_MODE));
        mMovePill.setOnClickListener(v -> {
            mMode = BulkGalleryState.MODE_MOVE;
            refreshGridState();
        });
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.leftMargin = BulkTheme.dp(this, 6);
        mMovePill.setLayoutParams(mp);
        row2.addView(mMovePill);
        bar.addView(row2);
        return bar;
    }

    /** Hint bar (HTML): swipe tip bayein, kul items dayein — accent-soft patti. */
    private View buildHintBar() {
        LinearLayout hint = new LinearLayout(this);
        hint.setOrientation(LinearLayout.HORIZONTAL);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setBackgroundColor(BulkTheme.iconBoxColor(this));
        hint.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 6),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 6));
        TextView tip = new TextView(this);
        tip.setText(BulkStrings.t(this, BulkStrings.SWIPE_TIP));
        tip.setTextColor(BulkTheme.accent(this));
        tip.setTextSize(11);
        hint.addView(tip, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        mTotalLabel = new TextView(this);
        mTotalLabel.setTextColor(BulkTheme.accent(this));
        mTotalLabel.setTextSize(11);
        mTotalLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        hint.addView(mTotalLabel);
        return hint;
    }

    /** Chhota pill button sirf text ke sath (Select All jesa). */
    private TextView toolPillText(String text) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        b.setTextColor(BulkTheme.textMain(this));
        b.setGravity(Gravity.CENTER);
        b.setBackground(toolPillBg(false));
        b.setPadding(BulkTheme.dp(this, 11), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 11), BulkTheme.dp(this, 7));
        b.setClickable(true);
        return b;
    }

    /** Chhota pill button icon + label ke sath (Sort / Grid / Copy / Move). Label = child 1. */
    private LinearLayout toolPill(int iconRes) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(toolPillBg(false));
        pill.setPadding(BulkTheme.dp(this, 11), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 11), BulkTheme.dp(this, 7));
        pill.setClickable(true);
        ImageView iv = BulkTheme.icon(this, iconRes, BulkTheme.textSub(this), 13);
        pill.addView(iv);
        TextView tv = new TextView(this);
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

    /** Tool pill ka background: aam = glass + halki border; active (mode) = accent-soft + accent border. */
    private android.graphics.drawable.Drawable toolPillBg(boolean active) {
        return active
            ? BulkTheme.roundedBg(this, BulkTheme.iconBoxColor(this), 20,
                BulkTheme.accent(this), 1)
            : BulkTheme.roundedBg(this, BulkTheme.glassBg(this), 20,
                BulkTheme.accentBorder(this), 1);
    }

    private void styleModePill(LinearLayout pill, ImageView icon, TextView label, boolean active) {
        if (pill == null) return;
        pill.setBackground(toolPillBg(active));
        int fg = active ? BulkTheme.accent(this) : BulkTheme.textMain(this);
        label.setTextColor(fg);
        icon.setColorFilter(active ? BulkTheme.accent(this) : BulkTheme.textSub(this),
            android.graphics.PorterDuff.Mode.SRC_IN);
    }

    // ------------------------------------------------------------ selection state

    /** Mojooda selection ki positions (display order par) — drag ke base ke liye. */
    private Set<Integer> selectedPositions() {
        Set<Integer> out = new HashSet<>();
        for (int i = 0; i < mEntries.size(); i++) {
            if (mSelectedIds.contains(mEntries.get(i).id)) out.add(i);
        }
        return out;
    }

    private void applyDrag() {
        Set<Integer> pos = BulkGallerySelect.dragSelection(mDragBase, mDragAnchor, mDragCurrent);
        mSelectedIds.clear();
        for (int p : pos) {
            if (p >= 0 && p < mEntries.size()) mSelectedIds.add(mEntries.get(p).id);
        }
        refreshGridState();
    }

    private void toggle(int position) {
        if (position < 0 || position >= mEntries.size()) return;
        long id = mEntries.get(position).id;
        if (mSelectedIds.contains(id)) mSelectedIds.remove(id);
        else mSelectedIds.add(id);
        refreshGridState();
    }

    private void refreshGridState() {
        int n = mSelectedIds.size();
        if (mCountLabel != null) {
            mCountLabel.setText(BulkStrings.format(this, BulkStrings.SELECTED_COUNT, n));
        }
        if (mSelectAllBtn != null) {
            mSelectAllBtn.setText(BulkStrings.t(this,
                BulkGalleryState.isAllSelected(n, mEntries.size())
                    ? BulkStrings.UNSELECT_ALL : BulkStrings.SELECT_ALL));
        }
        if (mConfirmBtn != null) {
            boolean move = mMode == BulkGalleryState.MODE_MOVE;
            if (n > 0) {
                mConfirmBtn.setText(BulkStrings.format(this,
                    move ? BulkStrings.MOVE_COUNT : BulkStrings.ADD_COUNT, n));
            } else {
                mConfirmBtn.setText(BulkStrings.t(this,
                    move ? BulkStrings.MOVE : BulkStrings.ADD));
            }
            mConfirmBtn.setEnabled(n > 0);
            mConfirmBtn.setAlpha(n > 0 ? 1f : 0.45f);
        }
        if (mSortLabel != null) {
            mSortLabel.setText(BulkStrings.t(this,
                mSortMode == BulkGalleryState.SORT_OLDEST ? BulkStrings.SORT_OLDEST
                    : mSortMode == BulkGalleryState.SORT_NAME ? BulkStrings.SORT_BY_NAME
                    : BulkStrings.SORT_NEWEST));
        }
        if (mGridLabel != null) {
            mGridLabel.setText(mCols + "x");
        }
        styleModePill(mCopyPill, mCopyIcon, mCopyLabel, mMode == BulkGalleryState.MODE_COPY);
        styleModePill(mMovePill, mMoveIcon, mMoveLabel, mMode == BulkGalleryState.MODE_MOVE);
        if (mTotalLabel != null) {
            mTotalLabel.setText(BulkStrings.format(this, BulkStrings.ITEMS_COUNT, mEntries.size()));
        }
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
        BulkFonts.applyToTree(this, mGrid);
    }

    private void confirmSelection() {
        if (mSelectedIds.isEmpty()) {
            finish();
            return;
        }
        ArrayList<Uri> uris = new ArrayList<>();
        for (BulkGalleryState.Entry e : mEntries) {
            if (mSelectedIds.contains(e.id)) uris.add(uriFor(e.id));
        }
        Intent data = new Intent();
        data.putParcelableArrayListExtra(EXTRA_URIS, uris);
        data.putExtra(EXTRA_MOVE, mMode == BulkGalleryState.MODE_MOVE);
        setResult(RESULT_OK, data);
        finish();
    }

    // ------------------------------------------------------------ adapter + thumbs

    private final class GalleryAdapter extends BaseAdapter {
        @Override public int getCount() { return mEntries.size(); }
        @Override public Object getItem(int position) { return mEntries.get(position); }
        @Override public long getItemId(int position) { return mEntries.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout cell;
            ImageView iv;
            TextView check;
            if (convertView instanceof FrameLayout) {
                cell = (FrameLayout) convertView;
                iv = (ImageView) cell.getChildAt(0);
                check = (TextView) cell.getChildAt(1);
            } else {
                cell = new FrameLayout(BulkGalleryActivity.this);
                iv = new ImageView(BulkGalleryActivity.this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackground(BulkTheme.iconBoxBg(BulkGalleryActivity.this));
                cell.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                check = new TextView(BulkGalleryActivity.this);
                check.setText("✓");
                check.setTextColor(BulkTheme.onAccent(BulkGalleryActivity.this));
                check.setTextSize(13);
                check.setGravity(Gravity.CENTER);
                check.setBackground(BulkTheme.accentCircleBg(BulkGalleryActivity.this));
                FrameLayout.LayoutParams chp = new FrameLayout.LayoutParams(
                    BulkTheme.dp(BulkGalleryActivity.this, 22), BulkTheme.dp(BulkGalleryActivity.this, 22));
                chp.gravity = Gravity.TOP | Gravity.RIGHT;
                chp.topMargin = BulkTheme.dp(BulkGalleryActivity.this, 5);
                chp.rightMargin = BulkTheme.dp(BulkGalleryActivity.this, 5);
                check.setLayoutParams(chp);
                cell.addView(check);
            }
            int cellSize = gridCellSize();
            cell.setLayoutParams(new GridView.LayoutParams(cellSize, cellSize));
            long id = mEntries.get(position).id;
            boolean sel = mSelectedIds.contains(id);
            check.setVisibility(sel ? View.VISIBLE : View.GONE);
            cell.setAlpha(sel ? 0.82f : 1f);
            loadThumb(id, iv);
            return cell;
        }
    }

    private int gridCellSize() {
        int w = getResources().getDisplayMetrics().widthPixels;
        int gaps = BulkTheme.dp(this, 16) + BulkTheme.dp(this, 4) * (mCols - 1);
        return Math.max(BulkTheme.dp(this, 40), (w - gaps) / mCols);
    }

    /** Sampled chhota thumbnail — background decode + cache; full-size kabhi nahi. */
    private void loadThumb(final long id, final ImageView target) {
        Bitmap cached = mThumbCache.get(id);
        if (cached != null) {
            target.setImageBitmap(cached);
            target.setTag(id);
            return;
        }
        target.setImageBitmap(null);
        target.setTag(id);
        new Thread(() -> {
            Bitmap bmp = decodeThumb(uriFor(id), 256);
            if (bmp != null) {
                mThumbCache.put(id, bmp);
                runOnUiThread(() -> {
                    Object tag = target.getTag();
                    if (tag instanceof Long && (Long) tag == id) target.setImageBitmap(bmp);
                });
            }
        }).start();
    }

    private Bitmap decodeThumb(Uri uri, int reqSize) {
        try {
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

    private static int sampleSize(int w, int h, int req) {
        int s = 1;
        while (w / (s * 2) >= req && h / (s * 2) >= req) s *= 2;
        return s;
    }
}
