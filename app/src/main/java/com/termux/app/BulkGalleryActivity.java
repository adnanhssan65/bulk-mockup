package com.termux.app;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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
 * v131 (Defect 2 fix): loading v129 wale proven raste par wapas —
 * MediaStore query SIRF {_ID} (DATE_ADDED DESC); v130 ka barhaya
 * hua projection (DISPLAY_NAME/DATE_ADDED wali Entry list) owner ke
 * phone par gallery khaali kar gaya tha. Naam ab Name-sort ke liye
 * alag lazy query se ate hain. Toolbar EK block me header ke neeche,
 * har row ki height pakki (46/42/30dp).
 *
 * TOOLBAR (owner ke apne HTML design — All Images Gallery
 * Manager — ke mutabiq, rows + hint bar):
 *   Row 1: "N selected" | Select All / Unselect All | Lock toggle
 *          (on ho to selection se kuch hataya nahi ja sakta) |
 *          Clear (poori selection khaali — lock on ho tab bhi;
 *          yehi unselect ka asal rasta hai)
 *   Row 2: Sort (Newest -> Oldest -> Name cycle) | Grid (ek line me
 *          2 -> 3 -> 4 -> 5 -> 6 images cycle)
 *   Add row (v132 redesign): full-width Add (n) button. Dabane par
 *          persisted Add Mode setting: Manual ho to
 *          Copy/Move popup, Auto–Copy/Auto–Move ho to seedha wohi
 *          mode. Factory default Auto–Move hai. Setting header ke
 *          gear icon se badalti hai (is screen par theme icon ki
 *          jagah gear hai).
 *   Hint bar: swipe tip + kul items count.
 * Selection IMAGE ID par hoti hai (sort badalne se selection nahi
 * tutti). Move confirm par EXTRA_MOVE=true jata hai — asal
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
    // v132: "All Images" row ka cover — gallery ki sab se pehli
    // (sab se nayi) image; buckets query DATE_ADDED DESC hai, is
    // liye pehli row hi asal cover hai
    private long mAllCoverId = -1;

    // Grid state — v131 (Defect 2 fix): ids LOAD order me (Newest
    // pehle) + DISPLAY order ki alag list; selection image ID par.
    // Naam sirf Name-sort ke liye alag se (lazy) ate hain.
    private boolean mInGrid = false;
    private String mGridTitle = "";
    private String mBucketId = null;
    private List<Long> mLoadOrder = new ArrayList<>();
    private List<Long> mIds = new ArrayList<>();
    private java.util.Map<Long, String> mNames = null;
    private final Set<Long> mSelectedIds = new HashSet<>();
    private GridView mGrid;
    private GalleryAdapter mAdapter;
    // v134: scroll handle (right edge, scroll fraction ke sath
    // chalti hui, drag = scrub) — BulkScrollHandle khud sambhalta hai
    private BulkScrollHandle mScrollHandle;
    private int mCols = BulkGalleryState.DEFAULT_GRID;
    private int mSortMode = BulkGalleryState.SORT_NEWEST;
    // v132 redesign: Add Mode setting (persisted) — factory default
    // Auto–Move; gear icon se badalti hai (header, theme ki jagah)
    private static final String PREFS_GALLERY = "bulk_gallery";
    private static final String KEY_ADD_CHOICE = "add_choice";
    private int mAddChoice = BulkGalleryState.CHOICE_AUTO_MOVE;

    // Toolbar refs (refresh ke liye)
    private TextView mCountLabel;
    private TextView mSelectAllBtn;
    private TextView mConfirmBtn;
    private TextView mSortLabel;
    private TextView mGridLabel;
    private TextView mTotalLabel;
    // v132: Lock / Clear (toolbar row 1)
    private LinearLayout mLockPill;
    private ImageView mLockIcon;
    private TextView mLockLabel;

    // Drag-select state (positions — display order par hisaab)
    private boolean mDragging = false;
    private int mDragAnchor = -1;
    private int mDragCurrent = -1;
    private Set<Integer> mDragBase = new HashSet<>();
    // v129: paint-style swipe select — ungli ghaseetne ki shuruaat
    private int mDownPos = -1;
    private float mDownX = -1;
    private float mDownY = -1;
    // v132: paint (swipe) drag hai ya long-press wala range drag;
    // paint ka target state (anchor ki toggled halat, lock-aware);
    // or ye gesture scroll-qarar paya (is me paint kabhi nahi banta)
    private boolean mDragPaint = false;
    private boolean mDragPaintState = true;
    private boolean mGestureScroll = false;
    // v132: LOCK — on ho to selection se kuch hataya nahi ja sakta
    // (tap/drag/Select-All sab par); Clear hi wahid unselect rasta
    // hai. Screen ki had tak — persist nahi hota, default OFF.
    private boolean mLocked = false;

    private final LruCache<Long, Bitmap> mThumbCache = new LruCache<>(240);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        // v132 redesign: Add Mode setting load (factory default Auto–Move)
        mAddChoice = BulkGalleryState.clampChoice(getSharedPreferences(
            PREFS_GALLERY, MODE_PRIVATE).getInt(KEY_ADD_CHOICE,
                BulkGalleryState.CHOICE_AUTO_MOVE));
        // v133: Sort + Grid bhi persisted (factory: Newest / Grid 3) —
        // Preview surfaces bhi yahi keys parhte hain
        android.content.SharedPreferences gp = getSharedPreferences(PREFS_GALLERY, MODE_PRIVATE);
        mSortMode = BulkGalleryState.resolveSortPref(
            gp.getInt(BulkGalleryState.KEY_SORT, BulkGalleryState.PREF_ABSENT));
        mCols = BulkGalleryState.resolveGridPref(
            gp.getInt(BulkGalleryState.KEY_GRID, BulkGalleryState.PREF_ABSENT));
        new Thread(() -> {
            boolean ok = loadBuckets();
            runOnUiThread(() -> {
                if (!ok) {
                    BulkTheme.showToast(this, BulkStrings.t(this, BulkStrings.GALLERY_EMPTY));
                    finish();
                    return;
                }
                // v132: gallery khaali ho tab bhi folder screen
                // dikhti hai — System Picker wala rasta wahan bhi
                // mojood rehta hai (pehle screen band ho jati thi)
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

    @Override
    protected void onPause() {
        super.onPause();
        // v132: gesture beech me ruk jaye (notification shade, call,
        // focus change) to painting state atka na rahe — agli dafa
        // screen par aane par drag saaf halat se shuru hota hai.
        resetDrag();
    }

    /** Har drag/paint state ko saaf karo — stuck "painting" mumkin hi nahi rehta. */
    private void resetDrag() {
        mDragging = false;
        mDragPaint = false;
        mDragAnchor = -1;
        mDragCurrent = -1;
        mDownPos = -1;
        mGestureScroll = false;
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
                // v132: sab se pehli row (DATE_ADDED DESC = sab se
                // nayi image) hi "All Images" ka cover hai
                if (mAllCoverId < 0) mAllCoverId = id;
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

    /**
     * v131 (Defect 2 fix): v129 wala PROVEN query bilkul waisa hi —
     * projection SIRF {_ID}, order DATE_ADDED DESC (load order =
     * Newest pehle). v130 ne projection me DISPLAY_NAME/DATE_ADDED
     * jor kar Entry list bana di thi; owner ke phone par usi tabdeeli
     * ke baad gallery khaali ho gayi (v129 me yahi screen bhari hui
     * chalti thi). Display order BulkGalleryState.orderIds se banta
     * hai; naam Name-sort ke waqt alag query se ate hain (fetchNames).
     */
    private List<Long> loadIds(String bucketId) {
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
                new String[]{MediaStore.Images.Media._ID},
                sel, args, MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    out.add(c.getLong(0));
                }
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "MediaStore images nahi mili: " + e);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    /**
     * Name-sort ke liye display names — alag, lazy query. Fail ho to
     * mNames null rehta hai aur Name-sort load order par gir jata hai
     * (gallery ka main rasta is par munhasir NAHI hai).
     */
    private void fetchNamesAsync() {
        final String bucketId = mBucketId;
        new Thread(() -> {
            java.util.Map<Long, String> names = new java.util.HashMap<>();
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
                        MediaStore.Images.Media.DISPLAY_NAME},
                    sel, args, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        String n = c.getString(1);
                        names.put(c.getLong(0), n != null ? n : "");
                    }
                }
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Naam nahi mile (Name-sort fallback): " + e);
                return;
            } finally {
                if (c != null) c.close();
            }
            final java.util.Map<Long, String> ready = names;
            runOnUiThread(() -> {
                mNames = ready;
                if (mInGrid && mSortMode == BulkGalleryState.SORT_NAME) {
                    mIds = BulkGalleryState.orderIds(mLoadOrder, mSortMode, mNames);
                    refreshGridState();
                }
            });
        }).start();
    }

    private static Uri uriFor(long id) {
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
    }

    // ------------------------------------------------------------ folder list

    private void showFolders() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        // v136 (owner ka FINAL header layout, preview
        // pv_v136_picker_chip): folderListHeader — back + title +
        // System Picker chip EK hi row me TITLE line par centered;
        // subtitle un ke neeche full width, saaf gap ke sath. Gear
        // is header me nahi (settings sirf gallery grids me hain).
        root.addView(folderListHeader());

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll);

        // v132: All Images ka cover ab gallery ki pehli image hai
        // (pehle khaali box ata tha — coverId -1 pass ho raha tha)
        LinearLayout.LayoutParams allLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        allLp.topMargin = 0;
        body.addView(folderRow(null, BulkStrings.t(this, BulkStrings.ALL_IMAGES),
            mTotalCount, mAllCoverId), allLp);
        for (Bucket b : mBuckets) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = BulkTheme.dp(this, 10);
            body.addView(folderRow(b.id, b.name, b.count, b.coverId), lp);
        }
        if (mTotalCount == 0) {
            TextView empty = new TextView(this);
            empty.setText(BulkStrings.t(this, BulkStrings.GALLERY_EMPTY));
            empty.setTextColor(BulkTheme.textMuted(this));
            empty.setTextSize(12);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ep.topMargin = BulkTheme.dp(this, 16);
            empty.setLayoutParams(ep);
            body.addView(empty);
        }
        setContentView(root);
        BulkFonts.apply(this);
    }

    /**
     * v136 folder-list header (owner ka approved layout, preview
     * pv_v136_picker_chip): PEHLI row me back button, title or
     * System Picker chip — teeno TITLE line par vertically
     * centered (ek saaf row). Subtitle is row ke NEECHE full
     * width par ati hai, chip ke bottom se kam az kam 8dp saaf
     * gap ke sath — chip subtitle ko kabhi nahi chhooti.
     * (Aam subHeader yahan istemal nahi hota: wahan right button
     * title+subtitle ke pure block par center hota hai, jis se
     * 40dp chip subtitle se takrati thi.)
     */
    private View folderListHeader() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(BulkTheme.dp(this, 20), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 20), 0);
        bar.addView(BulkTheme.circleButton(this, R.drawable.ic_bulk_back, 34,
            v -> finish()));

        TextView title = new TextView(this);
        title.setText(BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS));
        title.setTextSize(16);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(BulkTheme.textMain(this));
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = BulkTheme.dp(this, 10);
        tlp.rightMargin = BulkTheme.dp(this, 8);
        title.setLayoutParams(tlp);
        bar.addView(title);

        bar.addView(systemPickerChip());
        wrap.addView(bar);

        TextView sub = new TextView(this);
        sub.setText(BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS_SUBTITLE));
        sub.setTextSize(11);
        sub.setTextColor(BulkTheme.textMuted(this));
        sub.setPadding(BulkTheme.dp(this, 20), 0, BulkTheme.dp(this, 20),
            BulkTheme.dp(this, 10));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = BulkTheme.dp(this, 8);
        sub.setLayoutParams(slp);
        wrap.addView(sub);

        View line = new View(this);
        line.setBackground(BulkTheme.hLine(this));
        wrap.addView(line, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BulkTheme.dp(this, 0.6f))));
        return wrap;
    }

    /**
     * System Picker CHIP — v136 FINAL shakal (owner ke alfaaz:
     * "wha pr icon ni, chota sa option bna do, sath lekh do
     * system picker", size MEDIUM rakha jaye; preview
     * pv_v136_picker_chip): folder-list header ke top-right me
     * ek themed pill chip — folder glyph + "System Picker" text,
     * ~40dp unchi (aram-deh tap target), normal chhota label
     * text. Na bare icon, na list row. Tap = wohi system picker
     * (Gallery / File Manager) jo staging flow me jata hai.
     */
    private View systemPickerChip() {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setBackground(BulkTheme.pillBg(this));
        chip.setPadding(BulkTheme.dp(this, 12), 0, BulkTheme.dp(this, 12), 0);
        chip.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, BulkTheme.dp(this, 40)));
        ImageView iv = BulkTheme.icon(this, R.drawable.ic_bulk_folder, BulkTheme.accent(this), 16);
        iv.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 16), BulkTheme.dp(this, 16)));
        chip.addView(iv);
        TextView label = new TextView(this);
        label.setText(BulkStrings.t(this, BulkStrings.SYSTEM_PICKER));
        label.setTextColor(BulkTheme.textMain(this));
        label.setTextSize(12.5f);
        label.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = BulkTheme.dp(this, 6);
        label.setLayoutParams(lp);
        chip.addView(label);
        chip.setClickable(true);
        chip.setOnClickListener(v -> launchSystemPicker());
        return chip;
    }

    private static final int REQ_SYS_PICK = 47;

    /** System picker (ACTION_OPEN_DOCUMENT) — v127 wala secondary route, ab folder screen se bhi. */
    private void launchSystemPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQ_SYS_PICK);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "System picker nahi khula: " + e);
            BulkTheme.showToast(this, BulkStrings.t(this, BulkStrings.GALLERY_EMPTY));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SYS_PICK || resultCode != RESULT_OK || data == null) return;
        ArrayList<Uri> uris = new ArrayList<>();
        android.content.ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri u = clip.getItemAt(i).getUri();
                if (u != null) uris.add(u);
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;
        // Wohi result rasta jo gallery selection ka hai — Stage par
        // ja kar ye picks bhi staging + Duplicates hash flow se
        // guzarte hain (Copy mode; system picker par Move nahi hota)
        Intent out = new Intent();
        out.putParcelableArrayListExtra(EXTRA_URIS, uris);
        out.putExtra(EXTRA_MOVE, false);
        setResult(RESULT_OK, out);
        finish();
    }

    private View folderRow(final String bucketId, final String name, int count, long coverId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 10),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 10));

        // v132 restyle: asal cover thumbnail GOL konay ke sath
        // (pehle bitmap ke konay chaukor the — bg ka radius nazar
        // hi nahi ata tha); size baqi v131 rows se ham-ahang 48dp
        final ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackground(BulkTheme.iconBoxBg(this));
        cover.setClipToOutline(true);
        cover.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                    BulkTheme.dp(BulkGalleryActivity.this, 10));
            }
        });
        cover.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 48), BulkTheme.dp(this, 48)));
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
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(tv);
        TextView cnt = new TextView(this);
        cnt.setText(BulkStrings.format(this, BulkStrings.WALLPAPERS_COUNT, count));
        cnt.setTextColor(BulkTheme.textMuted(this));
        cnt.setTextSize(11);
        texts.addView(cnt);
        row.addView(texts);

        // v134 (owner ki pasand, preview pv_picker4): folder row par
        // Select All control BILKUL nahi — na pill, na icon. Folder
        // select karne ka wahid rasta grid toolbar ka Select All
        // button hai (har folder type par wohi chalta hai).
        row.setOnClickListener(v -> openGrid(bucketId, name));
        return row;
    }

    // ------------------------------------------------------------ grid

    private void openGrid(final String bucketId, final String title) {
        openGrid(bucketId, title, false);
    }

    /**
     * @param selectAll true ho to folder ki TAMAM images pehle se
     *                  selected aati hain (v132 whole-folder select) —
     *                  baqi flow (grid, toolbar, confirm → staging)
     *                  bilkul wohi hai
     */
    private void openGrid(final String bucketId, final String title, final boolean selectAll) {
        mInGrid = true;
        mGridTitle = title;
        mBucketId = bucketId;
        mNames = null;
        mSelectedIds.clear();
        // v132: lock per-visit hai — grid khulte hi hamesha OFF
        mLocked = false;
        new Thread(() -> {
            final List<Long> ids = loadIds(bucketId);
            runOnUiThread(() -> {
                mLoadOrder = ids;
                mIds = BulkGalleryState.orderIds(mLoadOrder, mSortMode, mNames);
                if (selectAll) mSelectedIds.addAll(mIds);
                buildGridUi();
            });
        }).start();
    }

    private void buildGridUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this, mGridTitle,
            BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS_SUBTITLE), gearButton()));

        // v131 (Defect 2 fix): toolbar EK hi block ki surat me header
        // ke foran neeche — kisi scrollable/weighted container ke
        // ANDAR nahi; har row ki height pakki (fixed dp), hub (v129)
        // wala hi usool jo owner ke phone par proven hai. Block ki
        // kul height bhi explicit hai: padding 6 + row1 46 + 4 +
        // row2 42 + 4 + add row 42 (v132 redesign) + 6 + hint 30 +
        // padding 6 = 186dp.
        root.addView(buildToolbarBlock(), new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 186)));

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
            // Long-press wala range drag — hamesha SELECT karta hai
            // (v129 wala proven rawaiya, barqarar)
            mDragging = true;
            mDragPaint = false;
            mDragAnchor = position;
            mDragCurrent = position;
            mDragBase = selectedPositions();
            applyDrag();
            return true;
        });
        mGrid.setOnTouchListener((v, e) -> {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                // Paint-select ki tayyari: kahan se ungli chali — tap
                // abhi bhi aam click hi rehta hai (false wapas).
                // v132: pichli gesture ka atka hua drag state pehle
                // saaf — stuck "painting" ka koi rasta nahi rehta.
                resetDrag();
                mDownPos = mGrid.pointToPosition((int) e.getX(), (int) e.getY());
                mDownX = e.getX();
                mDownY = e.getY();
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (!mDragging) {
                    // PAINT SELECT (v129 rawaiya, v132 me pakka):
                    // baghair long-press ke, ungli ghaseette hi
                    // selection shuru — jis image par se ungli guzre
                    // wo paint hoti jati hai. Seedha neeche scroll
                    // selection NAHI banata (owner ke HTML wala
                    // usool: horizontal swipe = select, vertical =
                    // scroll).
                    if (mDownPos < 0 || mGestureScroll) return false;
                    float dx = Math.abs(e.getX() - mDownX);
                    float dy = Math.abs(e.getY() - mDownY);
                    int slop = BulkTheme.dp(this, 10);
                    if (dx * dx + dy * dy < slop * slop) return false;
                    if (dx < dy * 0.9f) {
                        // v132 FIX: vertical-dominant gesture ab
                        // SCROLL qarar paya — is poori gesture me
                        // paint phir kabhi shuru nahi hoga. Pehle:
                        // scroll ke baad side par ghaseetne se paint
                        // achanak SHURU ho jata tha, or anchor wo
                        // purani (scroll ho chuki) position hoti thi
                        // — bilkul ghalat cells select ho jate thay.
                        mGestureScroll = true;
                        return false;
                    }
                    mDragging = true;
                    mDragPaint = true;
                    mDragAnchor = mDownPos;
                    mDragCurrent = mDownPos;
                    mDragBase = selectedPositions();
                    // Paint ka target: anchor cell ki TOGGLED halat
                    // (selected par shuru -> unselect paint; lock on
                    // ho to hamesha select — BulkGallerySelect)
                    mDragPaintState = BulkGallerySelect.paintTargetState(
                        mDragBase.contains(mDragAnchor), mLocked);
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
                boolean wasDragging = mDragging;
                resetDrag();
                // Drag ke baad click fire na ho; aam tap par false
                // taake click listener apna kaam kare
                return wasDragging;
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                // Dusri ungli chal rahe drag ko kharab na kare
                return mDragging;
            }
            return mDragging;
        });
        // v134: grid ek FrameLayout me — right edge par scroll
        // HANDLE (BulkScrollHandle): list ki fraction ke sath khud
        // uper/neeche chalti hai, ghaseetne par fast-scroll (scrub),
        // chevron tap par bilkul top/bottom. Handle sirf apni jagah
        // par touchable hai; grid ke taps/drag-select neeche bilkul
        // wese hi chalte hain. Gear popup ki "Scroll Button" setting
        // (Show/Hide) se band bhi ho sakti hai.
        FrameLayout gridWrap = new FrameLayout(this);
        gridWrap.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        mGrid.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        gridWrap.addView(mGrid);
        mScrollHandle = BulkScrollHandle.forAbsList(this, gridWrap, mGrid);
        root.addView(gridWrap);

        setContentView(root);
        BulkFonts.apply(this);
        refreshGridState();
    }

    // ------------------------------------------------------------ v130 toolbar

    /**
     * HTML design ki toolbar EK block me: (count | Select All | CTA) /
     * (Sort | Grid | Copy/Move) / hint bar. v131: har row ki height
     * pakki hai (46/42/30dp) — wrap-measure par kuch nahi chhora gaya.
     */
    private View buildToolbarBlock() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 6),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 6));

        // Row 1 — pakki height 46dp
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        row1.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 46)));
        mCountLabel = new TextView(this);
        mCountLabel.setTextColor(BulkTheme.accent(this));
        mCountLabel.setTextSize(14);
        mCountLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row1.addView(mCountLabel);
        row1.addView(new View(this), new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mSelectAllBtn = toolPillText(BulkStrings.t(this, BulkStrings.SELECT_ALL));
        mSelectAllBtn.setOnClickListener(v -> {
            // v132: LOCK on ho to Select All sirf ADD karta hai —
            // unselect ka wahid rasta Clear hai
            if (!mLocked && BulkGalleryState.isAllSelected(mSelectedIds.size(), mIds.size())) {
                mSelectedIds.clear();
            } else {
                mSelectedIds.addAll(mIds);
            }
            refreshGridState();
        });
        row1.addView(mSelectAllBtn);

        // v132 redesign: Lock + Clear bhi Row 1 me hain (Add button
        // neeche Add row me gaya). LOCK: on ho to selection se kuch
        // hataya nahi ja sakta — tap, drag, Select All sab sirf ADD
        // karte hain. CLEAR: poori selection ek tap me khaali — lock
        // on ho tab bhi chalta hai, yehi unselect ka asal rasta hai.
        mLockPill = toolPill(R.drawable.ic_bulk_lock);
        mLockIcon = (ImageView) mLockPill.getChildAt(0);
        mLockLabel = (TextView) mLockPill.getChildAt(1);
        mLockPill.setOnClickListener(v -> {
            mLocked = !mLocked;
            refreshGridState();
        });
        LinearLayout.LayoutParams lkp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lkp.leftMargin = BulkTheme.dp(this, 6);
        mLockPill.setLayoutParams(lkp);
        row1.addView(mLockPill);

        LinearLayout clearPill = toolPill(R.drawable.ic_bulk_trash);
        TextView clearLabel = (TextView) clearPill.getChildAt(1);
        clearLabel.setText(BulkStrings.t(this, BulkStrings.CLEAR));
        clearPill.setOnClickListener(v -> {
            // Clear hamesha chalta hai — lock on ho tab bhi
            mSelectedIds.clear();
            refreshGridState();
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.leftMargin = BulkTheme.dp(this, 6);
        clearPill.setLayoutParams(clp);
        row1.addView(clearPill);
        bar.addView(row1);

        // Row 2 — pakki height 42dp
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r2p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 42));
        r2p.topMargin = BulkTheme.dp(this, 4);
        row2.setLayoutParams(r2p);

        LinearLayout sortPill = toolPill(R.drawable.ic_bulk_sort);
        mSortLabel = (TextView) sortPill.getChildAt(1);
        sortPill.setOnClickListener(v -> {
            mSortMode = BulkGalleryState.nextSort(mSortMode);
            saveSortPref();
            if (mSortMode == BulkGalleryState.SORT_NAME && mNames == null) {
                fetchNamesAsync(); // naam ate hi order khud taaza hoga
            }
            mIds = BulkGalleryState.orderIds(mLoadOrder, mSortMode, mNames);
            refreshGridState();
        });
        row2.addView(sortPill);

        LinearLayout gridPill = toolPill(R.drawable.ic_bulk_grid);
        mGridLabel = (TextView) gridPill.getChildAt(1);
        gridPill.setOnClickListener(v -> {
            mCols = BulkGalleryState.nextColumns(mCols);
            saveGridPref();
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
        bar.addView(row2);

        // Add row — pakki height 42dp (v132 redesign): full-width
        // Add (n) button (Sort/Grid row ke neeche, jahan pehle
        // Copy/Move pills thay). Dabane par persisted Add Mode
        // setting ke hisab se ya seedha preset mode (Auto) lagta
        // hai ya Copy/Move popup khulta hai (Manual).
        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams arp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 42));
        arp.topMargin = BulkTheme.dp(this, 4);
        addRow.setLayoutParams(arp);

        mConfirmBtn = new TextView(this);
        mConfirmBtn.setTextColor(BulkTheme.onAccent(this));
        mConfirmBtn.setTextSize(13.5f);
        mConfirmBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        mConfirmBtn.setGravity(Gravity.CENTER);
        mConfirmBtn.setBackground(BulkTheme.primaryBg(this));
        mConfirmBtn.setClickable(true);
        mConfirmBtn.setOnClickListener(v -> onAddPressed());
        mConfirmBtn.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 42)));
        addRow.addView(mConfirmBtn);
        bar.addView(addRow);

        bar.addView(buildHintBar());
        return bar;
    }

    /** Hint bar (HTML): swipe tip bayein, kul items dayein — accent-soft patti, pakki height 30dp. */
    private View buildHintBar() {
        LinearLayout hint = new LinearLayout(this);
        hint.setOrientation(LinearLayout.HORIZONTAL);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setBackgroundColor(BulkTheme.iconBoxColor(this));
        hint.setPadding(BulkTheme.dp(this, 10), 0, BulkTheme.dp(this, 10), 0);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 30));
        hp.topMargin = BulkTheme.dp(this, 6);
        hint.setLayoutParams(hp);
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
        for (int i = 0; i < mIds.size(); i++) {
            if (mSelectedIds.contains(mIds.get(i))) out.add(i);
        }
        return out;
    }

    private void applyDrag() {
        // Paint (swipe) drag: anchor ki toggled halat poore swept
        // range par; long-press drag: purana union-select rawaiya.
        Set<Integer> pos = mDragPaint
            ? BulkGallerySelect.paintSelection(mDragBase, mDragAnchor, mDragCurrent,
                mDragPaintState)
            : BulkGallerySelect.dragSelection(mDragBase, mDragAnchor, mDragCurrent);
        Set<Long> next = new HashSet<>();
        for (int p : pos) {
            if (p >= 0 && p < mIds.size()) next.add(mIds.get(p));
        }
        // Koi tabdeeli nahi to grid dobara mat bandho — har MOVE par
        // full rebind drag ko bhari or jumpy banata tha
        if (next.equals(mSelectedIds)) return;
        mSelectedIds.clear();
        mSelectedIds.addAll(next);
        refreshGridState();
    }

    private void toggle(int position) {
        if (position < 0 || position >= mIds.size()) return;
        long id = mIds.get(position);
        // v132: LOCK on ho to selected cell tap se unselect nahi hota
        boolean now = BulkGallerySelect.toggledState(mSelectedIds.contains(id), mLocked);
        if (now) mSelectedIds.add(id);
        else mSelectedIds.remove(id);
        refreshGridState();
    }

    private void refreshGridState() {
        int n = mSelectedIds.size();
        if (mCountLabel != null) {
            mCountLabel.setText(BulkStrings.format(this, BulkStrings.SELECTED_COUNT, n));
        }
        if (mSelectAllBtn != null) {
            // v132: lock on ho to button hamesha "Select All" hi
            // kehlata hai (uska click bhi sirf add karta hai)
            mSelectAllBtn.setText(BulkStrings.t(this,
                !mLocked && BulkGalleryState.isAllSelected(n, mIds.size())
                    ? BulkStrings.UNSELECT_ALL : BulkStrings.SELECT_ALL));
        }
        if (mConfirmBtn != null) {
            // v132 redesign: Add button hamesha "Add (n)" — Copy/Move
            // ka faisla Add dabane par hota hai (popup ya Auto preset)
            if (n > 0) {
                mConfirmBtn.setText(BulkStrings.format(this, BulkStrings.ADD_COUNT, n));
            } else {
                mConfirmBtn.setText(BulkStrings.t(this, BulkStrings.ADD));
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
        // v132: Lock pill — on ho to accent style + "Locked" label
        if (mLockPill != null && mLockLabel != null) {
            mLockLabel.setText(BulkStrings.t(this,
                mLocked ? BulkStrings.LOCKED : BulkStrings.LOCK));
            styleModePill(mLockPill, mLockIcon, mLockLabel, mLocked);
        }
        if (mTotalLabel != null) {
            mTotalLabel.setText(BulkStrings.format(this, BulkStrings.ITEMS_COUNT, mIds.size()));
        }
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
        BulkFonts.applyToTree(this, mGrid);
    }

    /**
     * Add dabane par (v132 redesign): persisted Add Mode setting —
     * Auto (Copy/Move) ho to seedha usi mode se confirm; Manual ho
     * to Copy/Move popup. Factory default Auto–Move hai.
     */
    private void onAddPressed() {
        if (mSelectedIds.isEmpty()) return;
        int preset = BulkGalleryState.resolveMode(mAddChoice);
        if (preset != BulkGalleryState.MODE_NONE) {
            doConfirm(preset);
        } else {
            showCopyMovePopup();
        }
    }

    /** Chune hue mode se wohi purana confirm rasta (Stage pipeline). */
    private void doConfirm(int mode) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (Long id : mIds) {
            if (mSelectedIds.contains(id)) uris.add(uriFor(id));
        }
        Intent data = new Intent();
        data.putParcelableArrayListExtra(EXTRA_URIS, uris);
        data.putExtra(EXTRA_MOVE, mode == BulkGalleryState.MODE_MOVE);
        setResult(RESULT_OK, data);
        finish();
    }

    /** Manual mode ka popup: "Add N Wallpapers" + Copy / Move buttons. */
    private void showCopyMovePopup() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.format(this, BulkStrings.ADD_N_WALLPAPERS, mSelectedIds.size()),
            dialog);
        TextView q = new TextView(this);
        q.setText(BulkStrings.t(this, BulkStrings.COPY_OR_MOVE_Q));
        q.setTextColor(BulkTheme.textSub(this));
        q.setTextSize(13.5f);
        panel.addView(q);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams brp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brp.topMargin = BulkTheme.dp(this, 14);
        btnRow.setLayoutParams(brp);
        TextView copyBtn = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.COPY_MODE),
            v -> { dialog.dismiss(); doConfirm(BulkGalleryState.MODE_COPY); });
        copyBtn.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnRow.addView(copyBtn);
        TextView moveBtn = BulkTheme.primaryButton(this,
            BulkStrings.t(this, BulkStrings.MOVE_MODE),
            v -> { dialog.dismiss(); doConfirm(BulkGalleryState.MODE_MOVE); });
        LinearLayout.LayoutParams mbp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mbp.leftMargin = BulkTheme.dp(this, 10);
        moveBtn.setLayoutParams(mbp);
        btnRow.addView(moveBtn);
        panel.addView(btnRow);

        BulkTheme.showPanelDialog(this, panel);
        BulkFonts.applyToTree(this, panel);
    }

    /** v133: Sort choice persist (Preview surfaces bhi yahi key parhte hain). */
    private void saveSortPref() {
        getSharedPreferences(PREFS_GALLERY, MODE_PRIVATE).edit()
            .putInt(BulkGalleryState.KEY_SORT, mSortMode).apply();
    }

    /** v133: Grid choice persist (Preview surfaces bhi yahi key parhte hain). */
    private void saveGridPref() {
        getSharedPreferences(PREFS_GALLERY, MODE_PRIVATE).edit()
            .putInt(BulkGalleryState.KEY_GRID, mCols).apply();
    }

    /**
     * Gear icon ka popup (v132 redesign): Add Mode ki teen choices —
     * Manual / Auto–Copy / Auto–Move; mojooda choice par ✓ + strong
     * background. Tap = save (persisted) + band.
     * v133: neeche Sort (Newest/Oldest/Name) + Grid (2–6) sections —
     * ye bhi persisted hain or grid khula ho to foran lagte hain.
     */
    private void showAddModeMenu() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = BulkTheme.modalPanel(this,
            BulkStrings.t(this, BulkStrings.ADD_MODE), dialog);
        // v136 (owner ki pasand): popup COMPACT — rows ~48dp,
        // Grid choices ek chip row me, or poora content ek
        // ScrollView ke andar taake har section har screen
        // height par pohanch sake (Scroll Button ab neeche
        // dabta nahi). Content/order/prefs bilkul wohi hain.
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.62);
        scroller.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, maxH));
        scroller.addView(content);
        panel.addView(scroller);

        addChoiceRow(content, dialog, BulkGalleryState.CHOICE_MANUAL,
            BulkStrings.t(this, BulkStrings.ADD_CHOICE_MANUAL));
        addChoiceRow(content, dialog, BulkGalleryState.CHOICE_AUTO_COPY,
            BulkStrings.t(this, BulkStrings.ADD_CHOICE_AUTO_COPY));
        addChoiceRow(content, dialog, BulkGalleryState.CHOICE_AUTO_MOVE,
            BulkStrings.t(this, BulkStrings.ADD_CHOICE_AUTO_MOVE));

        content.addView(gearSectionLabel(BulkStrings.t(this, BulkStrings.SORT_SECTION)));
        final int[] sorts = {BulkGalleryState.SORT_NEWEST, BulkGalleryState.SORT_OLDEST,
            BulkGalleryState.SORT_NAME};
        final String[] sortKeys = {BulkStrings.SORT_NEWEST, BulkStrings.SORT_OLDEST,
            BulkStrings.SORT_BY_NAME};
        for (int i = 0; i < sorts.length; i++) {
            final int mode = sorts[i];
            settingRow(content, dialog, BulkStrings.t(this, sortKeys[i]),
                mSortMode == mode, () -> {
                    mSortMode = mode;
                    saveSortPref();
                    if (mInGrid) {
                        if (mSortMode == BulkGalleryState.SORT_NAME && mNames == null) {
                            fetchNamesAsync();
                        }
                        mIds = BulkGalleryState.orderIds(mLoadOrder, mSortMode, mNames);
                        refreshGridState();
                    }
                });
        }

        content.addView(gearSectionLabel(BulkStrings.t(this, BulkStrings.GRID_SECTION)));
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams crp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 40));
        crp.topMargin = BulkTheme.dp(this, 6);
        chipRow.setLayoutParams(crp);
        content.addView(chipRow);
        for (int cols = BulkGalleryState.MIN_COLUMNS;
             cols <= BulkGalleryState.MAX_COLUMNS; cols++) {
            final int c = cols;
            gridChip(chipRow, dialog, c + "x", mCols == c, () -> {
                mCols = c;
                saveGridPref();
                if (mGrid != null) mGrid.setNumColumns(mCols);
                if (mInGrid) refreshGridState();
            });
        }

        // v134: Scroll Button (scroll handle) Show/Hide — persisted.
        // v136: ye setting ab sirf gallery grids wale handle ko
        // govern karti hai (handle kahin or hota hi nahi)
        content.addView(gearSectionLabel(BulkStrings.t(this, BulkStrings.SCROLL_BUTTON)));
        settingRow(content, dialog, BulkStrings.t(this, BulkStrings.SHOW),
            BulkScrollSettings.isShown(this), () -> {
                BulkScrollSettings.setShown(this, true);
                if (mScrollHandle != null) mScrollHandle.refreshGate();
            });
        settingRow(content, dialog, BulkStrings.t(this, BulkStrings.HIDE),
            !BulkScrollSettings.isShown(this), () -> {
                BulkScrollSettings.setShown(this, false);
                if (mScrollHandle != null) mScrollHandle.refreshGate();
            });
        BulkTheme.showPanelDialog(this, panel);
        BulkFonts.applyToTree(this, panel);
    }

    /** v136: Grid section ki chip (2x–6x ek hi row me) — tap = amal + persist + band. */
    private void gridChip(LinearLayout row, final android.app.Dialog dialog,
                          String label, boolean selected, final Runnable action) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(13.5f);
        chip.setGravity(Gravity.CENTER);
        chip.setTextColor(selected ? BulkTheme.accent(this) : BulkTheme.textMain(this));
        if (selected) chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        chip.setBackground(selected ? BulkTheme.glassPanelStrong(this)
            : BulkTheme.glassPanel(this));
        chip.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.leftMargin = BulkTheme.dp(this, 3);
        lp.rightMargin = BulkTheme.dp(this, 3);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> {
            action.run();
            dialog.dismiss();
        });
        row.addView(chip);
    }

    private TextView gearSectionLabel(String text) {
        TextView label = BulkTheme.stepLabel(this, text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, 10);
        label.setLayoutParams(lp);
        return label;
    }

    /** Gear popup ki radio row (addChoiceRow wali hi shakal): tap = amal + persist + band.
     *  v136: COMPACT — pakki 48dp height, tight padding, zara chhota text. */
    private void settingRow(LinearLayout panel, final android.app.Dialog dialog,
                            String label, boolean selected, final Runnable action) {
        TextView row = new TextView(this);
        row.setText(selected ? label + "  ✓" : label);
        row.setTextSize(13.5f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTextColor(selected ? BulkTheme.accent(this) : BulkTheme.textMain(this));
        if (selected) row.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.setBackground(selected ? BulkTheme.glassPanelStrong(this)
            : BulkTheme.glassPanel(this));
        row.setPadding(BulkTheme.dp(this, 14), 0, BulkTheme.dp(this, 14), 0);
        row.setClickable(true);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 48));
        rp.topMargin = BulkTheme.dp(this, 6);
        row.setLayoutParams(rp);
        row.setOnClickListener(v -> {
            action.run();
            dialog.dismiss();
        });
        panel.addView(row);
    }

    private void addChoiceRow(LinearLayout panel, final android.app.Dialog dialog,
                              final int choice, String label) {
        boolean selected = mAddChoice == choice;
        TextView row = new TextView(this);
        row.setText(selected ? label + "  ✓" : label);
        row.setTextSize(13.5f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTextColor(selected ? BulkTheme.accent(this) : BulkTheme.textMain(this));
        if (selected) row.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.setBackground(selected ? BulkTheme.glassPanelStrong(this)
            : BulkTheme.glassPanel(this));
        row.setPadding(BulkTheme.dp(this, 14), 0, BulkTheme.dp(this, 14), 0);
        row.setClickable(true);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 48));
        rp.topMargin = BulkTheme.dp(this, 6);
        row.setLayoutParams(rp);
        row.setOnClickListener(v -> {
            mAddChoice = choice;
            getSharedPreferences(PREFS_GALLERY, MODE_PRIVATE).edit()
                .putInt(KEY_ADD_CHOICE, choice).apply();
            dialog.dismiss();
        });
        panel.addView(row);
    }

    /** Header ke dayein taraf gear button (theme icon ki jagah — sirf is screen par). */
    private View gearButton() {
        return BulkTheme.circleButton(this, R.drawable.ic_bulk_gear, 34,
            v -> showAddModeMenu());
    }



    // ------------------------------------------------------------ adapter + thumbs

    private final class GalleryAdapter extends BaseAdapter {
        @Override public int getCount() { return mIds.size(); }
        @Override public Object getItem(int position) { return mIds.get(position); }
        @Override public long getItemId(int position) { return mIds.get(position); }

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
            long id = mIds.get(position);
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
