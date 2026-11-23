package com.termux.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod (v125) — "Duplicates" screen.
 *
 * Staging me add hote waqt jo wallpapers duplicate nikle (SHA-256 same),
 * wo yahan holding folder me rehte hain. TTL (default 24 ghante) guzarne
 * par khud delete — TTL isi screen ki chhoti setting se badla ja sakta
 * hai. Manual delete: Select mode (Select All ya ek ek chun kar) → Delete.
 */
public class BulkDuplicatesActivity extends Activity {

    private static final int[] TTL_VALUES = {6, 12, 24, 48, 72, 168, 0};
    private static final String[] TTL_LABELS = {
        "6 hours", "12 hours", "24 hours", "48 hours", "72 hours", "1 week", "Never"};

    private List<BulkDuplicates.Entry> mEntries = new ArrayList<>();
    private final Map<String, Bitmap> mThumbs = new HashMap<>();
    private int mThumbGen = 0;

    private boolean mSelectMode = false;
    private final Set<String> mChecked = new HashSet<>(); // fileNames

    private TextView mSubtitle;
    private TextView mTtlValue;
    private LinearLayout mToolbarNormal;
    private LinearLayout mToolbarSelect;
    private TextView mSelectCount;
    private GridLayout mGrid;
    private TextView mEmptyText;
    private TextView mHeaderCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        setContentView(buildUi());
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    // ------------------------------------------------------------ UI

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));

        // Header (subHeader jesa, lekin subtitle dynamic hai)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(BulkTheme.dp(this, 16), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 16), 0);
        LinearLayout back = BulkTheme.circleButton(this, R.drawable.ic_bulk_back,
            34, v -> finish());
        topRow.addView(back);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = BulkTheme.dp(this, 12);
        titles.setLayoutParams(tp);
        TextView title = new TextView(this);
        title.setText("Duplicates");
        title.setTextColor(BulkTheme.textMain(this));
        title.setTextSize(17);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titles.addView(title);
        mSubtitle = new TextView(this);
        mSubtitle.setTextColor(BulkTheme.textMuted(this));
        mSubtitle.setTextSize(11.5f);
        titles.addView(mSubtitle);
        topRow.addView(titles);
        topRow.addView(BulkTheme.themeButton(this));
        header.addView(topRow);
        View line = new View(this);
        line.setBackground(BulkTheme.hLine(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 1));
        lp.topMargin = BulkTheme.dp(this, 10);
        line.setLayoutParams(lp);
        header.addView(line);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 8),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll);

        body.addView(settingRow());
        body.addView(toolbarNormal(), topLp(12));
        body.addView(toolbarSelect(), topLp(12));

        mEmptyText = new TextView(this);
        mEmptyText.setText("No duplicates");
        mEmptyText.setTextColor(BulkTheme.textMuted(this));
        mEmptyText.setTextSize(12.5f);
        mEmptyText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.topMargin = BulkTheme.dp(this, 26);
        mEmptyText.setLayoutParams(ep);
        body.addView(mEmptyText);

        mGrid = new GridLayout(this);
        mGrid.setColumnCount(3);
        body.addView(mGrid, topLp(10));
        return root;
    }

    private LinearLayout.LayoutParams topLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, topDp);
        return lp;
    }

    /** Auto-delete setting — chhoti row: label + value (tap par choices). */
    private View settingRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.glassPanel(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        TextView label = new TextView(this);
        label.setText("Auto-delete after");
        label.setTextColor(BulkTheme.textMain(this));
        label.setTextSize(13);
        row.addView(label);
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        mTtlValue = new TextView(this);
        mTtlValue.setTextColor(BulkTheme.accent(this));
        mTtlValue.setTextSize(13);
        mTtlValue.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.addView(mTtlValue);
        ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 14);
        LinearLayout.LayoutParams chp = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14));
        chp.leftMargin = BulkTheme.dp(this, 7);
        chev.setLayoutParams(chp);
        row.addView(chev);
        row.setOnClickListener(v -> showTtlChoices());
        return row;
    }

    private void showTtlChoices() {
        BulkTheme.showChoice(this, "Auto-delete after", TTL_LABELS, which -> {
            BulkDuplicatesStore.setTtlHours(this, TTL_VALUES[which]);
            // Nayi TTL foran lagu — jo ab expire ho gayi hon wo saaf
            new Thread(() -> {
                BulkDuplicatesStore.runExpiry(this);
                runOnUiThread(this::refresh);
            }).start();
            refreshHeader();
        });
    }

    private static String ttlLabel(int hours) {
        for (int i = 0; i < TTL_VALUES.length; i++) {
            if (TTL_VALUES[i] == hours) return TTL_LABELS[i];
        }
        return hours + " hours";
    }

    // ------------------------------------------------------------ toolbars

    private LinearLayout toolbarBase() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        return row;
    }

    private View toolbarNormal() {
        mToolbarNormal = toolbarBase();
        mHeaderCount = new TextView(this);
        mHeaderCount.setTextColor(BulkTheme.textSub(this));
        mHeaderCount.setTextSize(11);
        mHeaderCount.setTypeface(android.graphics.Typeface.MONOSPACE);
        mToolbarNormal.addView(mHeaderCount);
        View spacer = new View(this);
        mToolbarNormal.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView select = toolbarButton(R.drawable.ic_bulk_select, BulkTheme.textSub(this));
        select.setOnClickListener(v -> setSelectMode(true));
        mToolbarNormal.addView(select);
        return mToolbarNormal;
    }

    private View toolbarSelect() {
        mToolbarSelect = toolbarBase();
        mToolbarSelect.setGravity(Gravity.CENTER_VERTICAL);
        mSelectCount = new TextView(this);
        mSelectCount.setTextColor(BulkTheme.accent(this));
        mSelectCount.setTextSize(12);
        mSelectCount.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        mToolbarSelect.addView(mSelectCount);
        View spacer = new View(this);
        mToolbarSelect.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView all = new TextView(this);
        all.setText("Select All");
        all.setTextColor(BulkTheme.textMain(this));
        all.setTextSize(12);
        all.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        all.setBackground(BulkTheme.secondaryBg(this));
        all.setPadding(BulkTheme.dp(this, 10), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 10), BulkTheme.dp(this, 7));
        all.setOnClickListener(v -> {
            for (BulkDuplicates.Entry e : mEntries) mChecked.add(e.fileName);
            refreshGrid();
            refreshToolbars();
        });
        mToolbarSelect.addView(all);

        LinearLayout del = new LinearLayout(this);
        del.setOrientation(LinearLayout.HORIZONTAL);
        del.setGravity(Gravity.CENTER);
        del.setBackground(BulkTheme.dangerBg(this));
        del.setPadding(BulkTheme.dp(this, 10), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 10), BulkTheme.dp(this, 7));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.leftMargin = BulkTheme.dp(this, 8);
        del.setLayoutParams(dlp);
        del.addView(BulkTheme.icon(this, R.drawable.ic_bulk_trash, BulkTheme.danger(this), 13));
        TextView delText = new TextView(this);
        delText.setText("Delete");
        delText.setTextColor(BulkTheme.danger(this));
        delText.setTextSize(12);
        delText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams dtp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dtp.leftMargin = BulkTheme.dp(this, 5);
        delText.setLayoutParams(dtp);
        del.addView(delText);
        del.setOnClickListener(v -> deleteSelected());
        mToolbarSelect.addView(del);

        ImageView done = toolbarButton(R.drawable.ic_bulk_check, BulkTheme.accent(this));
        LinearLayout.LayoutParams dnp = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 30), BulkTheme.dp(this, 30));
        dnp.leftMargin = BulkTheme.dp(this, 8);
        done.setLayoutParams(dnp);
        done.setOnClickListener(v -> setSelectMode(false));
        mToolbarSelect.addView(done);
        return mToolbarSelect;
    }

    private ImageView toolbarButton(int iconRes, int tint) {
        ImageView b = new ImageView(this);
        b.setImageResource(iconRes);
        b.setColorFilter(tint, android.graphics.PorterDuff.Mode.SRC_IN);
        b.setBackground(BulkTheme.circleBg(this));
        b.setPadding(BulkTheme.dp(this, 7), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 7), BulkTheme.dp(this, 7));
        b.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 30), BulkTheme.dp(this, 30)));
        return b;
    }

    // ------------------------------------------------------------ state / actions

    private void refresh() {
        mEntries = BulkDuplicatesStore.loadEntries(this);
        refreshHeader();
        refreshToolbars();
        refreshGrid();
        loadThumbs();
    }

    private void refreshHeader() {
        int ttl = BulkDuplicatesStore.getTtlHours(this);
        if (mSubtitle != null) {
            mSubtitle.setText(ttl <= 0
                ? "Auto-delete off" : "Auto-delete after " + ttlLabel(ttl));
        }
        if (mTtlValue != null) mTtlValue.setText(ttlLabel(ttl));
        if (mHeaderCount != null) {
            mHeaderCount.setText(mEntries.size() + " duplicate(s)");
        }
    }

    private void refreshToolbars() {
        boolean has = !mEntries.isEmpty();
        if (mToolbarNormal != null) {
            mToolbarNormal.setVisibility(has && !mSelectMode ? View.VISIBLE : View.GONE);
        }
        if (mToolbarSelect != null) {
            mToolbarSelect.setVisibility(has && mSelectMode ? View.VISIBLE : View.GONE);
        }
        if (mSelectCount != null) {
            mSelectCount.setText(mChecked.size() + " selected");
        }
        if (mEmptyText != null) {
            mEmptyText.setVisibility(has ? View.GONE : View.VISIBLE);
        }
        if (mGrid != null) {
            mGrid.setVisibility(has ? View.VISIBLE : View.GONE);
        }
    }

    private void setSelectMode(boolean on) {
        mSelectMode = on;
        if (!on) mChecked.clear();
        refreshToolbars();
        refreshGrid();
    }

    private void deleteSelected() {
        if (mChecked.isEmpty()) return;
        List<BulkDuplicates.Entry> toDelete = new ArrayList<>();
        for (BulkDuplicates.Entry e : mEntries) {
            if (mChecked.contains(e.fileName)) toDelete.add(e);
        }
        BulkDuplicatesStore.deleteEntries(this, toDelete);
        for (BulkDuplicates.Entry e : toDelete) mThumbs.remove(e.fileName);
        mChecked.clear();
        mSelectMode = false;
        refresh();
    }

    // ------------------------------------------------------------ grid

    private void refreshGrid() {
        if (mGrid == null) return;
        mGrid.removeAllViews();
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM, HH:mm", Locale.US);
        for (BulkDuplicates.Entry e : mEntries) {
            mGrid.addView(buildTile(e, fmt));
        }
    }

    private View buildTile(final BulkDuplicates.Entry e, SimpleDateFormat fmt) {
        final boolean checked = mSelectMode && mChecked.contains(e.fileName);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setBackground(BulkTheme.tileBg(this, checked));
        tile.setPadding(BulkTheme.dp(this, 6), BulkTheme.dp(this, 6),
            BulkTheme.dp(this, 6), BulkTheme.dp(this, 6));
        int gap = BulkTheme.dp(this, 8);
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        glp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
        tile.setLayoutParams(glp);

        FrameLayout thumbBox = new FrameLayout(this);
        thumbBox.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 64)));
        Bitmap bmp = mThumbs.get(e.fileName);
        if (bmp != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bmp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbBox.addView(iv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            View ph = new View(this);
            ph.setBackground(BulkTheme.iconBoxBg(this));
            thumbBox.addView(ph, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ImageView iv = BulkTheme.icon(this, R.drawable.ic_bulk_image, BulkTheme.textMuted(this), 18);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
                BulkTheme.dp(this, 18), BulkTheme.dp(this, 18));
            ip.gravity = Gravity.CENTER;
            iv.setLayoutParams(ip);
            thumbBox.addView(iv);
        }
        if (mSelectMode) {
            FrameLayout badge = new FrameLayout(this);
            badge.setBackground(checked
                ? BulkTheme.accentCircleBg(this) : BulkTheme.circleBg(this));
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                BulkTheme.dp(this, 18), BulkTheme.dp(this, 18));
            bp.gravity = Gravity.TOP | Gravity.RIGHT;
            bp.topMargin = BulkTheme.dp(this, 4);
            bp.rightMargin = BulkTheme.dp(this, 4);
            badge.setLayoutParams(bp);
            ImageView bi = BulkTheme.icon(this, R.drawable.ic_bulk_check,
                checked ? BulkTheme.onAccent(this) : BulkTheme.textMuted(this), 10);
            FrameLayout.LayoutParams bip = new FrameLayout.LayoutParams(
                BulkTheme.dp(this, 10), BulkTheme.dp(this, 10));
            bip.gravity = Gravity.CENTER;
            bi.setLayoutParams(bip);
            badge.addView(bi);
            thumbBox.addView(badge);
        }
        tile.addView(thumbBox);

        TextView name = new TextView(this);
        name.setText(e.name);
        name.setTextSize(9.5f);
        name.setTextColor(BulkTheme.textMain(this));
        name.setTypeface(android.graphics.Typeface.MONOSPACE);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        np.topMargin = BulkTheme.dp(this, 4);
        name.setLayoutParams(np);
        tile.addView(name);

        TextView when = new TextView(this);
        when.setText(fmt.format(new Date(e.arrivalAt)));
        when.setTextSize(9);
        when.setTextColor(BulkTheme.textMuted(this));
        tile.addView(when);

        if (mSelectMode) {
            tile.setOnClickListener(v -> {
                if (mChecked.contains(e.fileName)) mChecked.remove(e.fileName);
                else mChecked.add(e.fileName);
                refreshToolbars();
                refreshGrid();
            });
        }
        return tile;
    }

    private void loadThumbs() {
        final int gen = ++mThumbGen;
        final List<BulkDuplicates.Entry> snapshot = new ArrayList<>(mEntries);
        final File dir = BulkDuplicatesStore.holdingDir(this);
        new Thread(() -> {
            for (BulkDuplicates.Entry e : snapshot) {
                if (mThumbs.containsKey(e.fileName)) continue;
                Bitmap bmp = decodeFileThumb(new File(dir, e.fileName), 220);
                if (bmp != null) {
                    mThumbs.put(e.fileName, bmp);
                    runOnUiThread(() -> {
                        if (gen == mThumbGen) refreshGrid();
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
            int s = 1;
            while (bounds.outWidth / (s * 2) >= reqSize && bounds.outHeight / (s * 2) >= reqSize) s *= 2;
            opts.inSampleSize = s;
            in = new FileInputStream(f);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
