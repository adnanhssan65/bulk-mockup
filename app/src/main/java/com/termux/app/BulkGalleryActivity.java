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
 * Termux Mod (v127, Adnan 2026-11-23) — App ki apni Gallery (picking ka
 * primary route). MediaStore se device ki images, pehle FOLDER list,
 * folder tap karne par us ki images grid me ("All Images" view bhi hai).
 *
 * Selection FAST hai: tap = ek toggle; SWIPE (paint-style, v129) ya
 * LONG-PRESS + DRAG = poora swept range select (BulkGallerySelect);
 * drag ke doran kinare par grid auto-scroll bhi hota hai; Select All
 * current view ke liye; upar live selected count. Confirm par tamam
 * selected URIs BulkStageActivity ko wapas — wahan se wohi purana
 * staging + Duplicates SHA-256 flow chalta hai.
 *
 * Performance: grid me sirf chhote sampled thumbnails (LruCache +
 * background decode, full-size kabhi nahi). Grid ki density staging
 * wali Rows setting (bulk_wallpapers_stage/cols) se ati hai.
 *
 * Permission: app ke paas first-run chain se storage/media access hota
 * hai; MediaStore parhna na ho sake to screen band ho jati hai or
 * Stage par System Picker secondary option ke tor par mojood rehta hai.
 */
public class BulkGalleryActivity extends Activity {

    private static final String LOG_TAG = "BulkGallery";
    public static final String EXTRA_URIS = "gallery_uris";

    private static final class Bucket {
        final String id;
        final String name;
        int count;
        long coverId;
        Bucket(String id, String name) { this.id = id; this.name = name; }
    }

    private final List<Bucket> mBuckets = new ArrayList<>();
    private int mTotalCount = 0;

    // Grid state
    private boolean mInGrid = false;
    private String mGridTitle = "";
    private final List<Long> mImageIds = new ArrayList<>();
    private final Set<Integer> mSelected = new HashSet<>();
    private GridView mGrid;
    private GalleryAdapter mAdapter;
    private TextView mCountLabel;
    private int mCols = 3;

    // Drag-select state
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
        mCols = BulkStaging.clampColumns(
            getSharedPreferences("bulk_wallpapers_stage", MODE_PRIVATE).getInt("cols", 3));
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
            mSelected.clear();
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

    private List<Long> loadImageIds(String bucketId) {
        List<Long> out = new ArrayList<>();
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
                new String[]{MediaStore.Images.Media._ID}, sel, args,
                MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (c != null) {
                while (c.moveToNext()) out.add(c.getLong(0));
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "MediaStore images nahi mili: " + e);
        } finally {
            if (c != null) c.close();
        }
        return out;
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
        mSelected.clear();
        new Thread(() -> {
            final List<Long> ids = loadImageIds(bucketId);
            runOnUiThread(() -> {
                mImageIds.clear();
                mImageIds.addAll(ids);
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

        // Action row: selected count + Select All + Add (confirm)
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 8));
        mCountLabel = new TextView(this);
        mCountLabel.setTextColor(BulkTheme.accent(this));
        mCountLabel.setTextSize(12.5f);
        mCountLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        bar.addView(mCountLabel, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView selectAll = new TextView(this);
        selectAll.setText(BulkStrings.t(this, BulkStrings.SELECT_ALL));
        selectAll.setTextColor(BulkTheme.textMain(this));
        selectAll.setTextSize(12.5f);
        selectAll.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        selectAll.setBackground(BulkTheme.secondaryBg(this));
        selectAll.setPadding(BulkTheme.dp(this, 10), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 10), BulkTheme.dp(this, 7));
        selectAll.setOnClickListener(v -> {
            if (mSelected.size() == mImageIds.size()) mSelected.clear();
            else for (int i = 0; i < mImageIds.size(); i++) mSelected.add(i);
            refreshGridState();
        });
        bar.addView(selectAll);

        TextView confirm = BulkTheme.primaryButton(this, BulkStrings.t(this, BulkStrings.ADD), null);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.leftMargin = BulkTheme.dp(this, 8);
        confirm.setLayoutParams(cp);
        confirm.setOnClickListener(v -> confirmSelection());
        bar.addView(confirm);
        root.addView(bar);

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
            mDragBase = new HashSet<>(mSelected);
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
                    // v129 PAINT SELECT: baghair long-press ke, ungli
                    // ghaseette hi selection shuru — jis image par se
                    // ungli guzre wo select hoti jati hai (owner ki
                    // demand: "hath sy swipe krty jae, image select ho
                    // jae"). Slop se chhoti harkat = scroll/tap, drag nahi.
                    if (mDownPos < 0) return false;
                    float dx = Math.abs(e.getX() - mDownX);
                    float dy = Math.abs(e.getY() - mDownY);
                    int slop = BulkTheme.dp(this, 10);
                    if (dx * dx + dy * dy < slop * slop) return false;
                    // Seedha neeche scroll karna selection NA ban jaye:
                    // paint-select tab hi shuru jab swipe kam az kam
                    // utna hi side par ho jitna neeche (diagonal sweep
                    // bhi chal jata hai). Ek dafa shuru ho jaye to phir
                    // har direction me range banta hai.
                    if (dx < dy * 0.9f) return false;
                    mDragging = true;
                    mDragAnchor = mDownPos;
                    mDragCurrent = mDownPos;
                    mDragBase = new HashSet<>(mSelected);
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

    private void applyDrag() {
        mSelected.clear();
        mSelected.addAll(BulkGallerySelect.dragSelection(mDragBase, mDragAnchor, mDragCurrent));
        refreshGridState();
    }

    private void toggle(int position) {
        if (mSelected.contains(position)) mSelected.remove(position);
        else mSelected.add(position);
        refreshGridState();
    }

    private void refreshGridState() {
        if (mCountLabel != null) {
            mCountLabel.setText(BulkStrings.format(this, BulkStrings.SELECTED_COUNT, mSelected.size()));
        }
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
        BulkFonts.applyToTree(this, mGrid);
    }

    private void confirmSelection() {
        if (mSelected.isEmpty()) {
            finish();
            return;
        }
        ArrayList<Uri> uris = new ArrayList<>();
        for (int i = 0; i < mImageIds.size(); i++) {
            if (mSelected.contains(i)) uris.add(uriFor(mImageIds.get(i)));
        }
        Intent data = new Intent();
        data.putParcelableArrayListExtra(EXTRA_URIS, uris);
        setResult(RESULT_OK, data);
        finish();
    }

    // ------------------------------------------------------------ adapter + thumbs

    private final class GalleryAdapter extends BaseAdapter {
        @Override public int getCount() { return mImageIds.size(); }
        @Override public Object getItem(int position) { return mImageIds.get(position); }
        @Override public long getItemId(int position) { return mImageIds.get(position); }

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
            long id = mImageIds.get(position);
            boolean sel = mSelected.contains(position);
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
