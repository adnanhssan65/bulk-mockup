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
import java.util.List;
import java.util.Map;

/**
 * Termux Mod (v127) — Preview Ready Assets (drawer se khulti hai).
 * Sirf wo projects jin ke <N>_Mockups me kuch hai. Project tap → us ke
 * asal folders: Mockups/ tab = <N>_Mockups, Wallpapers/ tab =
 * <N>_Ready_Wallpapers (2-col grid). Kisi item par tap → full-screen
 * inspector (bara preview + naam + asal path). Sab read-only — asal
 * folders ka asal data hi dikhta hai.
 */
public class BulkReadyAssetsActivity extends Activity {

    private static final String LOG_TAG = "BulkReadyAssets";

    private List<BulkProjects.ProjectInfo> mProjects = new ArrayList<>();
    private final Map<String, Bitmap> mThumbs = new HashMap<>();
    private int mThumbGen = 0;

    private LinearLayout mBody;
    private BulkProjects.ProjectInfo mOpenProject;
    private boolean mMockupsTab = true;

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
    public void onBackPressed() {
        // Folder view khula ho to pehle list par wapas
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
                        if (gen == mThumbGen && mOpenProject != null) showFolder();
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
            if (k.isFile() && BulkProjects.isImageFile(k.getName())) out.add(k);
        }
        java.util.Collections.sort(out, (a, b) ->
            a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
        return out;
    }
}
