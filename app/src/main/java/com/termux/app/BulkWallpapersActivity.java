package com.termux.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.termux.R;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod (v125) — "Add Wallpapers" screen (approved design + staging).
 *
 * Flow:
 *  - Step 1: project select (dropdown). Project select hue baghair bhi
 *    images pick ho sakti hain — lock sirf COMMIT par hai.
 *  - Step 2: STAGING (drop) page — picked images yahan thumbnails me
 *    dikhte hain; "Select Images" se mazeed add hote hain. Staging
 *    SharedPreferences me persist hoti hai: screen se bahar ja kar
 *    wapas ao to staged wallpapers mehfooz rehte hain. Toolbar: Sort,
 *    Grid (2/3/4 columns), Select (multi-select + delete), Clear (sab).
 *    Har tile par x badge se ek image remove ho jata hai.
 *  - Preview Added Images: tamam projects + un ke <N>_wallpapers folder
 *    ka count; tap karne par us project ke asal thumbnails khulte hain
 *    (sirf dekhna — delete/edit nahi).
 *  - Step 3: "Add" = staged images selected project ke <N>_wallpapers
 *    me copy (picker order, +1 second mtime steps) — kamyabi par sirf
 *    copy hue items staging se saaf. "Run Now" = Add + terminal me
 *    project run. Dono tab tak locked jab tak project select na ho
 *    or staging khaali ho.
 */
public class BulkWallpapersActivity extends Activity {

    private static final String LOG_TAG = "BulkWallpapers";
    private static final int REQ_PICK = 41;

    // Staging persistence (screen band hone par bhi mehfooz)
    private static final String PREFS_STAGE = "bulk_wallpapers_stage";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_COLS = "cols";

    private List<BulkProjects.ProjectInfo> mProjects = new ArrayList<>();
    private int mSelectedNumber = -1;

    private List<BulkStaging.Item> mStaged = new ArrayList<>();
    private final Map<String, Bitmap> mThumbs = new HashMap<>();
    private int mThumbGen = 0;
    private int mCols = 2;

    private boolean mSelectMode = false;
    private final Set<String> mChecked = new HashSet<>();
    private static final Set<Integer> sPreviewExpanded = new HashSet<>();
    private final Map<String, Bitmap> mPreviewThumbs = new HashMap<>();
    private int mPreviewGen = 0;

    private boolean mCopying = false;
    private final Object mStageLock = new Object();
    private TextView mDupCount;

    // View refs (refresh ke liye)
    private Spinner mSpinner;
    private TextView mStep1Status;
    private TextView mStagedCount;
    private LinearLayout mToolbarNormal;
    private LinearLayout mToolbarSelect;
    private TextView mSelectCount;
    private GridLayout mStagedGrid;
    private TextView mStep3Count;
    private TextView mAddBtn;
    private TextView mRunBtn;
    private LinearLayout mStep3Buttons;
    private LinearLayout mStep3Progress;
    private TextView mProgressLabel;
    private LinearLayout mProgressBar;
    private LinearLayout mPreviewList;

    // ------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        if (!hasStorageAccess()) {
            setContentView(buildPermissionUi());
            return;
        }
        mProjects = loadProjects();
        restoreStage();
        setContentView(buildUi());
        loadStagedThumbs();
        // Duplicates expiry — screen khulne par (TTL guzri copies delete)
        new Thread(() -> {
            BulkDuplicatesStore.runExpiry(this);
            runOnUiThread(this::refreshDupCount);
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Project folders ke counts bahar se bhi badal sakte hain (terminal run)
        if (mPreviewList != null) refreshPreview();
        refreshDupCount();
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

    /** v124 wali storage check: Android 11+ par all-files access. */
    private boolean hasStorageAccess() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            try {
                return android.os.Environment.isExternalStorageManager();
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

    private BulkProjects.ProjectInfo selectedProject() {
        for (BulkProjects.ProjectInfo p : mProjects) {
            if (p.number == mSelectedNumber) return p;
        }
        return null;
    }

    // ------------------------------------------------------------ staging persistence

    private SharedPreferences stagePrefs() {
        return getSharedPreferences(PREFS_STAGE, MODE_PRIVATE);
    }

    private void restoreStage() {
        SharedPreferences sp = stagePrefs();
        mStaged = BulkStaging.decode(sp.getString(KEY_ITEMS, ""));
        mSelectedNumber = sp.getInt(KEY_PROJECT, -1);
        mCols = BulkStaging.clampColumns(sp.getInt(KEY_COLS, 2));
        if (selectedProject() == null) mSelectedNumber = -1;
    }

    private void persistStage() {
        stagePrefs().edit()
            .putString(KEY_ITEMS, BulkStaging.encode(mStaged))
            .putInt(KEY_PROJECT, mSelectedNumber)
            .putInt(KEY_COLS, mCols)
            .apply();
    }

    // ------------------------------------------------------------ image picker

    private void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        // Persistable read permission — staging app restart ke baad bhi parhi ja sake
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQ_PICK);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Image picker nahi khula: " + e);
            BulkTheme.showToast(this, "Image picker nahi khula.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null) return;
        List<Uri> picked = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri u = clip.getItemAt(i).getUri();
                if (u != null) picked.add(u);
            }
        } else if (data.getData() != null) {
            picked.add(data.getData());
        }
        if (picked.isEmpty()) return;

        // Persistable permission + naam/size yahin (halka kaam); hash or
        // duplicates ka faisla background thread par hota hai
        final List<String[]> infos = new ArrayList<>();
        for (Uri uri : picked) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            infos.add(queryNameSize(uri));
        }
        new Thread(() -> processPicked(picked, infos)).start();
    }

    /**
     * Picked images ka background process: har image ka SHA-256 hash,
     * mojooda staged hashes se muqabla — duplicate ho to staging ki
     * bajaye Duplicates holding me; warna hash ke sath staging me add.
     */
    private void processPicked(List<Uri> picked, List<String[]> infos) {
        // 1) Mojooda staged items ke ghayab hashes bhardo (snapshot par)
        List<BulkStaging.Item> snapshot;
        synchronized (mStageLock) { snapshot = new ArrayList<>(mStaged); }
        Set<String> stagedHashes = new HashSet<>();
        Map<String, String> filledHashes = new HashMap<>();
        for (BulkStaging.Item it : snapshot) {
            String h = it.hash;
            if (h == null || h.isEmpty()) {
                h = hashOfUri(Uri.parse(it.uri));
                if (!h.isEmpty()) filledHashes.put(it.uri, h);
            }
            if (!h.isEmpty()) stagedHashes.add(h);
        }

        // 2) Naye picks classify karo
        Set<String> batchAccepted = new HashSet<>();
        List<BulkStaging.Item> accepted = new ArrayList<>();
        int dupCount = 0;
        long now = System.currentTimeMillis();
        for (int i = 0; i < picked.size(); i++) {
            Uri uri = picked.get(i);
            String name = infos.get(i)[0];
            long size = parseLong(infos.get(i)[1], -1);
            String hash = hashOfUri(uri);
            if (BulkDuplicates.isDuplicate(stagedHashes, batchAccepted, hash)) {
                InputStream in = null;
                try { in = getContentResolver().openInputStream(uri); } catch (Exception ignored) {}
                if (in != null && BulkDuplicatesStore.addDuplicate(
                        this, in, name, hash, System.currentTimeMillis()) != null) {
                    dupCount++;
                }
            } else {
                accepted.add(new BulkStaging.Item(uri.toString(), name, size, now++, hash));
                if (!hash.isEmpty()) batchAccepted.add(hash);
            }
        }

        // 3) Apply — lock ke andar taake is doran hue removals mehfooz rahen
        synchronized (mStageLock) {
            if (!filledHashes.isEmpty()) {
                List<BulkStaging.Item> updated = new ArrayList<>();
                for (BulkStaging.Item it : mStaged) {
                    String fh = filledHashes.get(it.uri);
                    updated.add(fh != null ? it.withHash(fh) : it);
                }
                mStaged = updated;
            }
            for (BulkStaging.Item item : accepted) {
                BulkStaging.addIfAbsent(mStaged, item);
            }
            persistStage();
        }
        final int fDup = dupCount;
        runOnUiThread(() -> {
            refreshStagedViews();
            refreshDupCount();
            loadStagedThumbs();
            if (fDup > 0) {
                BulkTheme.showToast(this, fDup + " duplicate(s) moved to Duplicates");
            }
        });
    }

    private String hashOfUri(Uri uri) {
        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);
            return BulkDuplicatesStore.sha256Hex(in);
        } catch (Exception e) {
            return "";
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }

    private String[] queryNameSize(Uri uri) {
        String name = null;
        String size = null;
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ni >= 0) name = c.getString(ni);
                int si = c.getColumnIndex(OpenableColumns.SIZE);
                if (si >= 0 && !c.isNull(si)) size = String.valueOf(c.getLong(si));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        if (name == null || name.isEmpty()) {
            String seg = uri.getLastPathSegment();
            name = seg != null ? seg : "image";
        }
        return new String[]{name, size};
    }

    private static long parseLong(String s, long fallback) {
        if (s == null) return fallback;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return fallback; }
    }

    // ------------------------------------------------------------ permission screen

    private View buildPermissionUi() {
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
        msg.setText("Storage permission chahiye.");
        msg.setTextColor(BulkTheme.textMain(this));
        msg.setTextSize(16);
        panel.addView(msg);

        TextView permBtn = BulkTheme.primaryButton(this, "Permissions", null);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = BulkTheme.dp(this, 14);
        permBtn.setLayoutParams(pp);
        permBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, BulkPermissionsActivity.class));
            finish();
        });
        panel.addView(permBtn);

        TextView closeBtn = BulkTheme.secondaryButton(this, "Band Karo", null);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = BulkTheme.dp(this, 10);
        closeBtn.setLayoutParams(cp);
        closeBtn.setOnClickListener(v -> finish());
        panel.addView(closeBtn);

        root.addView(panel);
        return root;
    }

    // ------------------------------------------------------------ main UI

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));

        root.addView(BulkTheme.subHeader(this, "Add Wallpapers",
            "Select project first, then pick images"));

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

        body.addView(duplicatesRow());
        body.addView(step1Panel(), panelLp(12));
        body.addView(step2Panel(), panelLp(12));
        body.addView(previewPanel(), panelLp(12));
        body.addView(step3Panel(), panelLp(12));

        refreshStates();
        refreshStagedViews();
        return root;
    }

    private LinearLayout.LayoutParams panelLp(int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, topMarginDp);
        return lp;
    }

    private LinearLayout stepPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(BulkTheme.glassPanel(this));
        panel.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14));
        return panel;
    }

    private LinearLayout headerRow(String label, TextView right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(BulkTheme.stepLabel(this, label));
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (right != null) row.addView(right);
        return row;
    }

    // ------------------------------------------------------------ Duplicates row

    /** Chhoti si row (icon + "Duplicates" + count) — tap par Duplicates screen. */
    private View duplicatesRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 11),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 11));

        FrameLayout iconBox = new FrameLayout(this);
        iconBox.setBackground(BulkTheme.iconBoxBg(this));
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 34), BulkTheme.dp(this, 34)));
        ImageView iv = BulkTheme.icon(this, R.drawable.ic_bulk_copy, BulkTheme.accent(this), 17);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 17), BulkTheme.dp(this, 17));
        ip.gravity = Gravity.CENTER;
        iv.setLayoutParams(ip);
        iconBox.addView(iv);
        row.addView(iconBox);

        View divider = new View(this);
        divider.setBackgroundColor(BulkTheme.accentBorder(this));
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 1), BulkTheme.dp(this, 22));
        dp2.leftMargin = BulkTheme.dp(this, 11);
        divider.setLayoutParams(dp2);
        row.addView(divider);

        TextView label = new TextView(this);
        label.setText("Duplicates");
        label.setTextColor(BulkTheme.textMain(this));
        label.setTextSize(14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = BulkTheme.dp(this, 11);
        label.setLayoutParams(lp);
        row.addView(label);

        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mDupCount = new TextView(this);
        mDupCount.setText("0");
        mDupCount.setTextColor(BulkTheme.accent(this));
        mDupCount.setTextSize(12.5f);
        mDupCount.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        row.addView(mDupCount);

        FrameLayout arrow = new FrameLayout(this);
        arrow.setBackground(BulkTheme.arrowCircleBg(this));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 24), BulkTheme.dp(this, 24));
        ap.leftMargin = BulkTheme.dp(this, 9);
        arrow.setLayoutParams(ap);
        ImageView av = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 12);
        FrameLayout.LayoutParams avp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 12));
        avp.gravity = Gravity.CENTER;
        av.setLayoutParams(avp);
        arrow.addView(av);
        row.addView(arrow);

        row.setOnClickListener(v ->
            startActivity(new Intent(this, BulkDuplicatesActivity.class)));
        return row;
    }

    private void refreshDupCount() {
        if (mDupCount == null) return;
        mDupCount.setText(String.valueOf(BulkDuplicatesStore.count(this)));
    }

    // ------------------------------------------------------------ Step 1 — project

    private View step1Panel() {
        LinearLayout panel = stepPanel();

        mStep1Status = new TextView(this);
        mStep1Status.setTextSize(11);
        panel.addView(headerRow("Step 1 • Select Project", mStep1Status));

        FrameLayout wrap = new FrameLayout(this);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.topMargin = BulkTheme.dp(this, 8);
        wrap.setLayoutParams(wp);

        mSpinner = new Spinner(this);
        mSpinner.setBackground(BulkTheme.spinnerBg(this, false));
        mSpinner.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 11),
            BulkTheme.dp(this, 34), BulkTheme.dp(this, 11));
        wrap.addView(mSpinner, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 16);
        FrameLayout.LayoutParams chp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 16), BulkTheme.dp(this, 16));
        chp.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        chp.rightMargin = BulkTheme.dp(this, 10);
        chev.setLayoutParams(chp);
        wrap.addView(chev);
        panel.addView(wrap);

        List<String> labels = new ArrayList<>();
        labels.add("-- Choose a Project --");
        for (BulkProjects.ProjectInfo p : mProjects) {
            int ready = p.number > 0
                ? BulkProjects.countFiles(BulkProjects.readyDir(p.number)) : 0;
            labels.add(p.name + " (Ready: " + ready + ")");
        }
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
                    ? (TextView) convertView : new TextView(BulkWallpapersActivity.this);
                t.setText(getItem(position));
                t.setTextSize(13.5f);
                t.setTextColor(dropDown || position > 0
                    ? BulkTheme.textMain(BulkWallpapersActivity.this)
                    : BulkTheme.textMuted(BulkWallpapersActivity.this));
                t.setBackgroundColor(dropDown
                    ? BulkTheme.modalColor(BulkWallpapersActivity.this) : Color.TRANSPARENT);
                t.setPadding(BulkTheme.dp(BulkWallpapersActivity.this, 10),
                    BulkTheme.dp(BulkWallpapersActivity.this, 10),
                    BulkTheme.dp(BulkWallpapersActivity.this, 10),
                    BulkTheme.dp(BulkWallpapersActivity.this, 10));
                t.setSingleLine(true);
                t.setEllipsize(TextUtils.TruncateAt.END);
                return t;
            }
        };
        mSpinner.setAdapter(adapter);

        int initial = 0;
        for (int i = 0; i < mProjects.size(); i++) {
            if (mProjects.get(i).number == mSelectedNumber) { initial = i + 1; break; }
        }
        mSpinner.setSelection(initial);
        mSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                mSelectedNumber = position == 0 ? -1 : mProjects.get(position - 1).number;
                persistStage();
                refreshStates();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        return panel;
    }

    // ------------------------------------------------------------ Step 2 — staging

    private View step2Panel() {
        LinearLayout panel = stepPanel();

        mStagedCount = new TextView(this);
        mStagedCount.setTextSize(11);
        mStagedCount.setTextColor(BulkTheme.textSub(this));
        mStagedCount.setTypeface(android.graphics.Typeface.MONOSPACE);
        panel.addView(headerRow("Step 2 • Select Images", mStagedCount));

        // Pick / add-more — hamesha khula (lock sirf commit par)
        LinearLayout pick = new LinearLayout(this);
        pick.setOrientation(LinearLayout.HORIZONTAL);
        pick.setGravity(Gravity.CENTER);
        pick.setBackground(BulkTheme.secondaryBg(this));
        pick.setPadding(BulkTheme.dp(this, 12), BulkTheme.dp(this, 11),
            BulkTheme.dp(this, 12), BulkTheme.dp(this, 11));
        LinearLayout.LayoutParams pkp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pkp.topMargin = BulkTheme.dp(this, 10);
        pick.setLayoutParams(pkp);
        pick.addView(BulkTheme.icon(this, R.drawable.ic_bulk_image, BulkTheme.accent(this), 15));
        TextView pickText = new TextView(this);
        pickText.setText("Select Images");
        pickText.setTextColor(BulkTheme.textMain(this));
        pickText.setTextSize(13.5f);
        pickText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams ptp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ptp.leftMargin = BulkTheme.dp(this, 8);
        pickText.setLayoutParams(ptp);
        pick.addView(pickText);
        pick.setOnClickListener(v -> launchPicker());
        panel.addView(pick);

        // Toolbar — sirf jab staging khaali na ho
        mToolbarNormal = buildToolbarNormal();
        panel.addView(mToolbarNormal);
        mToolbarSelect = buildToolbarSelect();
        panel.addView(mToolbarSelect);

        mStagedGrid = new GridLayout(this);
        mStagedGrid.setColumnCount(mCols);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin = BulkTheme.dp(this, 10);
        mStagedGrid.setLayoutParams(gp);
        panel.addView(mStagedGrid);
        return panel;
    }

    private LinearLayout toolbarRowBase() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, 10);
        row.setLayoutParams(lp);
        return row;
    }

    private LinearLayout buildToolbarNormal() {
        LinearLayout row = toolbarRowBase();
        ImageView sort = toolbarButton(R.drawable.ic_bulk_sort, BulkTheme.textSub(this));
        sort.setOnClickListener(v -> showSortDialog());
        row.addView(sort);
        ImageView grid = toolbarButton(R.drawable.ic_bulk_grid, BulkTheme.textSub(this));
        grid.setOnClickListener(v -> {
            mCols = BulkStaging.nextColumns(mCols);
            persistStage();
            refreshStagedViews();
        });
        row.addView(grid, toolbarBtnLp(8));
        ImageView select = toolbarButton(R.drawable.ic_bulk_select, BulkTheme.textSub(this));
        select.setOnClickListener(v -> setSelectMode(true));
        row.addView(select, toolbarBtnLp(8));
        ImageView clear = toolbarButton(R.drawable.ic_bulk_trash, BulkTheme.danger(this));
        clear.setOnClickListener(v -> confirmClearAll());
        row.addView(clear, toolbarBtnLp(8));
        return row;
    }

    private LinearLayout buildToolbarSelect() {
        LinearLayout row = toolbarRowBase();
        row.setGravity(Gravity.CENTER_VERTICAL);
        mSelectCount = new TextView(this);
        mSelectCount.setTextSize(12);
        mSelectCount.setTextColor(BulkTheme.accent(this));
        mSelectCount.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.addView(mSelectCount);
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout del = new LinearLayout(this);
        del.setOrientation(LinearLayout.HORIZONTAL);
        del.setGravity(Gravity.CENTER);
        del.setBackground(BulkTheme.dangerBg(this));
        del.setPadding(BulkTheme.dp(this, 10), BulkTheme.dp(this, 7),
            BulkTheme.dp(this, 10), BulkTheme.dp(this, 7));
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
        row.addView(del);

        ImageView done = toolbarButton(R.drawable.ic_bulk_check, BulkTheme.accent(this));
        done.setOnClickListener(v -> setSelectMode(false));
        row.addView(done, toolbarBtnLp(8));
        return row;
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

    private LinearLayout.LayoutParams toolbarBtnLp(int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 30), BulkTheme.dp(this, 30));
        lp.leftMargin = BulkTheme.dp(this, leftDp);
        return lp;
    }

    // ------------------------------------------------------------ staging actions

    private void setSelectMode(boolean on) {
        mSelectMode = on;
        if (!on) mChecked.clear();
        refreshStagedViews();
    }

    private void confirmClearAll() {
        if (mStaged.isEmpty()) return;
        BulkTheme.showConfirm(this, "Clear Staging",
            "Remove all " + mStaged.size() + " staged wallpapers?",
            "Clear", true, () -> {
                mStaged.clear();
                mChecked.clear();
                mSelectMode = false;
                mThumbs.clear();
                persistStage();
                refreshStagedViews();
            });
    }

    private void deleteSelected() {
        if (mChecked.isEmpty()) return;
        List<BulkStaging.Item> keep = new ArrayList<>();
        for (BulkStaging.Item it : mStaged) {
            if (mChecked.contains(it.uri)) {
                mThumbs.remove(it.uri);
            } else {
                keep.add(it);
            }
        }
        mStaged = keep;
        mChecked.clear();
        mSelectMode = false;
        persistStage();
        refreshStagedViews();
    }

    private void removeStaged(BulkStaging.Item item) {
        mStaged.remove(item);
        mThumbs.remove(item.uri);
        mChecked.remove(item.uri);
        persistStage();
        refreshStagedViews();
    }

    private void showSortDialog() {
        String[] options = {
            "Name (A to Z)", "Name (Z to A)",
            "Date Added (Oldest First)", "Date Added (Newest First)",
            "Size (Small to Large)", "Size (Large to Small)"};
        BulkTheme.showChoice(this, "Sort Wallpapers", options, which -> {
            mStaged = BulkStaging.sorted(mStaged, which);
            persistStage();
            refreshStagedViews();
        });
    }

    // ------------------------------------------------------------ staging refresh

    private void refreshStagedViews() {
        if (mStagedGrid == null) return;
        boolean has = !mStaged.isEmpty();
        mToolbarNormal.setVisibility(has && !mSelectMode ? View.VISIBLE : View.GONE);
        mToolbarSelect.setVisibility(has && mSelectMode ? View.VISIBLE : View.GONE);
        mStagedGrid.setVisibility(has ? View.VISIBLE : View.GONE);
        if (mSelectMode) {
            mSelectCount.setText(mChecked.size() + " selected");
        }
        mStagedGrid.setColumnCount(mCols);
        mStagedGrid.removeAllViews();
        for (BulkStaging.Item item : mStaged) {
            mStagedGrid.addView(buildTile(item));
        }
        refreshStates();
    }

    private View buildTile(final BulkStaging.Item item) {
        final boolean checked = mSelectMode && mChecked.contains(item.uri);
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
        int thumbH = mCols == 2 ? 76 : (mCols == 3 ? 60 : 48);
        thumbBox.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, thumbH)));
        Bitmap bmp = mThumbs.get(item.uri);
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

        // Badge: normal mode = x (remove), select mode = check state
        FrameLayout badge = new FrameLayout(this);
        if (mSelectMode) {
            badge.setBackground(checked
                ? BulkTheme.accentCircleBg(this) : BulkTheme.circleBg(this));
        } else {
            android.graphics.drawable.GradientDrawable gd =
                new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(0x8C000000);
            badge.setBackground(gd);
        }
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 18));
        bp.gravity = Gravity.TOP | Gravity.RIGHT;
        bp.topMargin = BulkTheme.dp(this, 4);
        bp.rightMargin = BulkTheme.dp(this, 4);
        badge.setLayoutParams(bp);
        ImageView badgeIcon;
        if (mSelectMode) {
            badgeIcon = BulkTheme.icon(this, R.drawable.ic_bulk_check,
                checked ? BulkTheme.onAccent(this) : BulkTheme.textMuted(this), 10);
        } else {
            badgeIcon = BulkTheme.icon(this, R.drawable.ic_bulk_close, 0xFFFFFFFF, 10);
        }
        FrameLayout.LayoutParams bip = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 10), BulkTheme.dp(this, 10));
        bip.gravity = Gravity.CENTER;
        badgeIcon.setLayoutParams(bip);
        badge.addView(badgeIcon);
        thumbBox.addView(badge);
        tile.addView(thumbBox);

        TextView name = new TextView(this);
        name.setText(item.name);
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

        if (mSelectMode) {
            tile.setOnClickListener(v -> {
                if (mChecked.contains(item.uri)) mChecked.remove(item.uri);
                else mChecked.add(item.uri);
                refreshStagedViews();
            });
        } else {
            badge.setOnClickListener(v -> removeStaged(item));
        }
        return tile;
    }

    private void loadStagedThumbs() {
        final int gen = ++mThumbGen;
        final List<BulkStaging.Item> snapshot = new ArrayList<>(mStaged);
        new Thread(() -> {
            for (BulkStaging.Item item : snapshot) {
                if (mThumbs.containsKey(item.uri)) continue;
                Bitmap bmp = decodeUriThumb(Uri.parse(item.uri), 256);
                if (bmp != null) {
                    mThumbs.put(item.uri, bmp);
                    runOnUiThread(() -> {
                        if (gen == mThumbGen) refreshStagedViews();
                    });
                }
            }
        }).start();
    }

    private Bitmap decodeUriThumb(Uri uri, int reqSize) {
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

    // ------------------------------------------------------------ Preview Added Images

    private View previewPanel() {
        LinearLayout panel = stepPanel();
        TextView label = BulkTheme.stepLabel(this, "Preview Added Images");
        panel.addView(label);
        mPreviewList = new LinearLayout(this);
        mPreviewList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BulkTheme.dp(this, 6);
        mPreviewList.setLayoutParams(lp);
        panel.addView(mPreviewList);
        refreshPreview();
        return panel;
    }

    private void refreshPreview() {
        if (mPreviewList == null) return;
        mPreviewList.removeAllViews();
        boolean any = false;
        for (BulkProjects.ProjectInfo p : mProjects) {
            if (p.number <= 0) continue;
            any = true;
            mPreviewList.addView(previewRow(p));
        }
        if (!any) {
            TextView empty = new TextView(this);
            empty.setText("No Projects Installed");
            empty.setTextColor(BulkTheme.textMuted(this));
            empty.setTextSize(12);
            empty.setPadding(0, BulkTheme.dp(this, 6), 0, 0);
            mPreviewList.addView(empty);
        }
    }

    private View previewRow(final BulkProjects.ProjectInfo p) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        final boolean expanded = sPreviewExpanded.contains(p.number);
        final File wdir = BulkProjects.wallpapersDir(p.number);
        int count = BulkProjects.countFiles(wdir);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, BulkTheme.dp(this, 8), 0, BulkTheme.dp(this, 8));
        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextColor(BulkTheme.textMain(this));
        name.setTextSize(13.5f);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.addView(name);
        View spacer = new View(this);
        head.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView cnt = new TextView(this);
        cnt.setText(String.valueOf(count));
        cnt.setTextColor(BulkTheme.accent(this));
        cnt.setTextSize(12);
        cnt.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        head.addView(cnt);
        ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron, BulkTheme.textMuted(this), 14);
        LinearLayout.LayoutParams chp = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14));
        chp.leftMargin = BulkTheme.dp(this, 8);
        chev.setLayoutParams(chp);
        chev.setRotation(expanded ? 90 : 0);
        head.addView(chev);
        head.setOnClickListener(v -> {
            if (expanded) sPreviewExpanded.remove(p.number);
            else sPreviewExpanded.add(p.number);
            refreshPreview();
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
                none.setText("No wallpapers");
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
                loadPreviewThumbs(files);
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
        Bitmap bmp = mPreviewThumbs.get(f.getAbsolutePath());
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
            ip.gravity = Gravity.CENTER;
            iv.setLayoutParams(ip);
            box.addView(iv);
        }
        return box;
    }

    private void loadPreviewThumbs(final List<File> files) {
        final int gen = ++mPreviewGen;
        new Thread(() -> {
            for (File f : files) {
                String key = f.getAbsolutePath();
                if (mPreviewThumbs.containsKey(key)) continue;
                Bitmap bmp = decodeFileThumb(f, 220);
                if (bmp != null) {
                    mPreviewThumbs.put(key, bmp);
                    runOnUiThread(() -> {
                        if (gen == mPreviewGen) refreshPreview();
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

    private static List<File> imageFiles(File dir) {
        List<File> out = new ArrayList<>();
        File[] kids = dir.listFiles();
        if (kids == null) return out;
        for (File k : kids) {
            if (k.isFile() && BulkProjects.isImageFile(k.getName())) out.add(k);
        }
        java.util.Collections.sort(out, (a, b) ->
            a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
        return out;
    }

    // ------------------------------------------------------------ Step 3 — commit / run

    private View step3Panel() {
        LinearLayout panel = stepPanel();

        mStep3Count = new TextView(this);
        mStep3Count.setTextSize(11);
        mStep3Count.setTextColor(BulkTheme.textMuted(this));
        panel.addView(headerRow("Step 3 • Actions", mStep3Count));

        mStep3Buttons = new LinearLayout(this);
        mStep3Buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = BulkTheme.dp(this, 10);
        mStep3Buttons.setLayoutParams(bp);

        mAddBtn = BulkTheme.secondaryButton(this, "Add", null);
        mAddBtn.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        mAddBtn.setOnClickListener(v -> startCommit(false));
        mStep3Buttons.addView(mAddBtn);

        mRunBtn = BulkTheme.primaryButton(this, "Run Now", null);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rp.leftMargin = BulkTheme.dp(this, 10);
        mRunBtn.setLayoutParams(rp);
        mRunBtn.setOnClickListener(v -> startCommit(true));
        mStep3Buttons.addView(mRunBtn);
        panel.addView(mStep3Buttons);

        mStep3Progress = new LinearLayout(this);
        mStep3Progress.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = BulkTheme.dp(this, 10);
        mStep3Progress.setLayoutParams(pp);
        mStep3Progress.setVisibility(View.GONE);
        mProgressLabel = new TextView(this);
        mProgressLabel.setTextSize(12);
        mProgressLabel.setTextColor(BulkTheme.textSub(this));
        mStep3Progress.addView(mProgressLabel);
        mProgressBar = BulkTheme.progressTrack(this);
        LinearLayout.LayoutParams pbp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 4));
        pbp.topMargin = BulkTheme.dp(this, 6);
        mProgressBar.setLayoutParams(pbp);
        mStep3Progress.addView(mProgressBar);
        panel.addView(mStep3Progress);
        return panel;
    }

    /** Lock halat: commit/run sirf project + staged images ke sath. */
    private void refreshStates() {
        BulkProjects.ProjectInfo sel = selectedProject();
        boolean hasProject = sel != null;
        if (mStep1Status != null) {
            mStep1Status.setText(hasProject ? "Unlocked" : "Required");
            mStep1Status.setTextColor(hasProject ? BulkTheme.accent(this) : BulkTheme.textMuted(this));
            mStep1Status.setTypeface(android.graphics.Typeface.DEFAULT,
                hasProject ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        if (mSpinner != null) {
            mSpinner.setBackground(BulkTheme.spinnerBg(this, hasProject));
        }
        if (mStagedCount != null) {
            mStagedCount.setText(mStaged.size() + " selected");
        }
        if (mStep3Count != null) {
            mStep3Count.setText(mStaged.size() + " wallpaper(s)");
        }
        if (mAddBtn != null) {
            boolean canCommit = BulkStaging.canCommit(mSelectedNumber, mStaged.size()) && !mCopying;
            boolean canRun = BulkStaging.canRun(mSelectedNumber, mStaged.size(),
                sel != null && sel.workCmd != null) && !mCopying;
            mStep3Buttons.setAlpha((canCommit || canRun) ? 1f : 0.4f);
            mAddBtn.setEnabled(canCommit);
            mAddBtn.setAlpha(canCommit ? 1f : 0.4f);
            mRunBtn.setEnabled(canRun);
            mRunBtn.setAlpha(canRun ? 1f : 0.4f);
        }
    }

    private void setProgressUi(boolean active, String label, int pct) {
        if (mStep3Progress == null) return;
        mStep3Progress.setVisibility(active ? View.VISIBLE : View.GONE);
        mStep3Buttons.setVisibility(active ? View.GONE : View.VISIBLE);
        if (active) {
            mProgressLabel.setText(label);
            BulkTheme.setProgress(mProgressBar, pct);
        }
    }

    private void startCommit(final boolean runNow) {
        final BulkProjects.ProjectInfo sel = selectedProject();
        if (sel == null || sel.number <= 0) return;
        if (!BulkStaging.canCommit(sel.number, mStaged.size())) return;
        if (runNow && sel.workCmd == null) return;
        final List<BulkStaging.Item> todo = new ArrayList<>(mStaged);
        final File destDir = BulkProjects.wallpapersDir(sel.number);
        mCopying = true;
        refreshStates();
        setProgressUi(true, runNow ? "Rendering phone mockups..." : "Adding wallpapers...", 0);

        new Thread(() -> {
            final CopyResult result = copyStaged(todo, destDir, pct ->
                runOnUiThread(() -> BulkTheme.setProgress(mProgressBar, pct)));
            runOnUiThread(() -> {
                mCopying = false;
                setProgressUi(false, "", 0);
                if (result.copied > 0) {
                    // Sirf copy hue items staging se saaf — fail hue (or copy ke
                    // doran add hue naye) items mehfooz rehte hain
                    Set<String> failedSet = new HashSet<>(result.failedUris);
                    List<BulkStaging.Item> keep = new ArrayList<>();
                    for (BulkStaging.Item it : mStaged) {
                        boolean inTodo = BulkStaging.containsUri(todo, it.uri);
                        if (inTodo && !failedSet.contains(it.uri)) continue; // copied
                        keep.add(it);
                    }
                    mStaged = keep;
                    persistStage();
                }
                refreshStagedViews();
                refreshPreview();
                if (result.copied == 0) {
                    BulkTheme.showToast(BulkWallpapersActivity.this, "Copy fail ho gaya.");
                    return;
                }
                String msg = "Added " + result.copied + " wallpaper(s) to " + sel.name
                    + (result.failed > 0 ? " (" + result.failed + " fail)" : "");
                BulkTheme.showToast(BulkWallpapersActivity.this, msg);
                if (runNow) openTerminalWithCommand(sel.workCmd);
            });
        }).start();
    }

    private interface ProgressCb { void onProgress(int pct); }

    private static final class CopyResult {
        int copied;
        int failed;
        final List<String> failedUris = new ArrayList<>();
    }

    /** Staged images ko picker order me copy karo; har file +1s mtime step. */
    private CopyResult copyStaged(List<BulkStaging.Item> items, File destDir, ProgressCb cb) {
        CopyResult result = new CopyResult();
        if (!destDir.exists()) destDir.mkdirs();
        ContentResolver cr = getContentResolver();
        long anchor = System.currentTimeMillis() / 1000L;
        long step = 0;
        int total = Math.max(1, items.size());
        for (BulkStaging.Item item : items) {
            Uri uri = Uri.parse(item.uri);
            String rawName = item.name != null && !item.name.isEmpty()
                ? item.name : displayName(cr, uri);
            File dest = uniqueFile(destDir, sanitizeFileName(rawName));
            boolean ok = copyUri(cr, uri, dest);
            if (ok) {
                result.copied++;
                //noinspection ResultOfMethodCallIgnored
                dest.setLastModified((anchor + step) * 1000L);
                step++;
            } else {
                result.failed++;
                result.failedUris.add(item.uri);
            }
            if (cb != null) cb.onProgress((result.copied + result.failed) * 100 / total);
        }
        return result;
    }

    private String sanitizeFileName(String name) {
        String n = name == null ? "" : name;
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) n = n.substring(slash + 1);
        n = n.trim();
        return n.isEmpty() ? "image.jpg" : n;
    }

    private String displayName(ContentResolver cr, Uri uri) {
        Cursor c = null;
        try {
            c = cr.query(uri, null, null, null, null);
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
        return seg != null ? seg : "image.jpg";
    }

    private File uniqueFile(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        int i = 1;
        while (true) {
            f = new File(dir, base + "_" + i + ext);
            if (!f.exists()) return f;
            i++;
        }
    }

    private boolean copyUri(ContentResolver cr, Uri uri, File dest) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = cr.openInputStream(uri);
            if (in == null) return false;
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            out.flush();
            return true;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Copy fail " + uri + ": " + e);
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private void openTerminalWithCommand(String command) {
        Intent intent = new Intent(this, TermuxActivity.class);
        intent.putExtra(TermuxActivity.EXTRA_RUN_COMMAND, command);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
