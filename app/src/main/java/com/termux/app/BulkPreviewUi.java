package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Termux Mod (v132) — Frame Preview (DP) ka UI hissa.
 *
 * Source ka faisla BulkPreviewSource (pure) karta hai; yahan sirf
 * asal parhna/banana hai: <N>_Mockups ki sab se nayi asal mockup
 * file, ya bundled preview asset (bulk/previews/p<N>.jpg), ya
 * number wala placeholder. Do jagah istemal hota hai:
 *   - Add Wallpapers → Select Project (DP + image icon + Select/Close popup)
 *   - Projects screen (sirf DP; tap par view-only popup)
 *
 * Thumbnails chhote sampled decodes hain (full-size kabhi nahi) or
 * LruCache me rehte hain — Projects screen har expand par rebuild
 * hoti hai, cache ki wajah se dobara decode nahi hota. Cache key me
 * asal file ka lastModified bhi hai — naya mockup bante hi purani
 * DP khud badal jati hai.
 */
public final class BulkPreviewUi {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(48);
    private static final Map<Integer, Boolean> BUNDLED_OK = new HashMap<>();

    private BulkPreviewUi() {}

    // ------------------------------------------------------------ source

    /** Is project ke preview ka mojooda faisla (folder scan + asset check). */
    public static BulkPreviewSource.Choice choiceFor(Context c, int number) {
        if (number <= 0) return new BulkPreviewSource.Choice(
            BulkPreviewSource.KIND_PLACEHOLDER, null);
        List<BulkPreviewSource.Entry> entries = new ArrayList<>();
        File dir = BulkProjects.mockupsDir(number);
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File k : kids) {
                if (k.isFile()) entries.add(
                    new BulkPreviewSource.Entry(k.getName(), k.lastModified()));
            }
        }
        return BulkPreviewSource.choose(entries, number, bundledAvailable(c, number));
    }

    private static boolean bundledAvailable(Context c, int number) {
        if (!BulkPreviewSource.hasBundledRange(number)) return false;
        Boolean cached = BUNDLED_OK.get(number);
        if (cached != null) return cached;
        boolean ok = false;
        InputStream in = null;
        try {
            in = c.getAssets().open(BulkPreviewSource.bundledAssetPath(number));
            ok = in != null;
        } catch (Exception ignored) {
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
        BUNDLED_OK.put(number, ok);
        return ok;
    }

    // ------------------------------------------------------------ bitmap

    /**
     * Preview bitmap (sampled, reqSize tak) — PLACEHOLDER par null.
     * Background thread se call karo (disk parhna hai); cache hit ho
     * to foran wapas.
     */
    public static Bitmap loadBitmap(Context c, int number, int reqSize) {
        BulkPreviewSource.Choice choice = choiceFor(c, number);
        if (choice.kind == BulkPreviewSource.KIND_PLACEHOLDER) return null;
        String key;
        File file = null;
        if (choice.kind == BulkPreviewSource.KIND_REAL) {
            file = new File(BulkProjects.mockupsDir(number), choice.fileName);
            key = "real|" + file.getAbsolutePath() + "|" + file.lastModified() + "|" + reqSize;
        } else {
            key = "bundled|" + number + "|" + reqSize;
        }
        Bitmap hit = CACHE.get(key);
        if (hit != null) return hit;
        Bitmap bmp = choice.kind == BulkPreviewSource.KIND_REAL
            ? decodeFile(file, reqSize)
            : decodeAsset(c, BulkPreviewSource.bundledAssetPath(number), reqSize);
        if (bmp != null) CACHE.put(key, bmp);
        return bmp;
    }

    private static Bitmap decodeFile(File f, int reqSize) {
        if (f == null || !f.exists()) return null;
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

    private static Bitmap decodeAsset(Context c, String path, int reqSize) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in = c.getAssets().open(path);
            BitmapFactory.decodeStream(in, null, bounds);
            in.close();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, reqSize);
            in = c.getAssets().open(path);
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

    /** Bitmap ke konay gol karo (DP ki shakal). */
    public static Bitmap rounded(Bitmap bmp, float radiusPx) {
        if (bmp == null) return null;
        try {
            Bitmap out = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
                Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            Path path = new Path();
            path.addRoundRect(new RectF(0, 0, bmp.getWidth(), bmp.getHeight()),
                radiusPx, radiusPx, Path.Direction.CW);
            canvas.clipPath(path);
            canvas.drawBitmap(bmp, 0, 0, null);
            return out;
        } catch (Exception e) {
            return bmp;
        }
    }

    // ------------------------------------------------------------ DP view

    /**
     * Chhoti DP (profile-style) — gol konay, andar asal preview;
     * jab tak load na ho (ya placeholder ho) project number dikhta
     * hai. Load background thread par hota hai.
     */
    public static View dpView(final Activity a, final int number, int sizeDp) {
        final FrameLayout box = new FrameLayout(a);
        int size = BulkTheme.dp(a, sizeDp);
        box.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        box.setBackground(BulkTheme.roundedBg(a, BulkTheme.iconBoxColor(a), 12,
            BulkTheme.accentBorder(a), 1));

        TextView num = new TextView(a);
        num.setText(number > 0 ? String.valueOf(number) : "?");
        num.setTextColor(BulkTheme.textMuted(a));
        num.setTextSize(sizeDp / 2.6f);
        num.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        num.setGravity(Gravity.CENTER);
        box.addView(num, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final ImageView iv = new ImageView(a);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(iv, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        box.setTag(number);
        final int req = Math.max(96, size * 2);
        new Thread(() -> {
            Bitmap bmp = loadBitmap(a, number, req);
            if (bmp == null) return;
            final Bitmap round = rounded(bmp, bmp.getWidth() * 0.16f);
            a.runOnUiThread(() -> {
                Object tag = box.getTag();
                if (tag instanceof Integer && (Integer) tag == number) {
                    iv.setImageBitmap(round);
                }
            });
        }).start();
        return box;
    }

    // ------------------------------------------------------------ popup

    /**
     * Bara preview popup (design style): title = project naam,
     * neeche EK bari preview image. withSelect ho to Select Project
     * + Close dono button; warna sirf Close (Projects screen wala
     * view-only version). X (modalPanel wala) hamesha kaam karta hai.
     */
    public static void showPopup(final Activity a, final BulkProjects.ProjectInfo p,
                                 boolean withSelect, final Runnable onSelect) {
        final Dialog dialog = new Dialog(a);
        LinearLayout panel = BulkTheme.modalPanel(a, p.name, dialog);

        TextView sub = new TextView(a);
        sub.setText(BulkStrings.t(a, BulkStrings.FRAME_PREVIEW));
        sub.setTextColor(BulkTheme.textMuted(a));
        sub.setTextSize(11.5f);
        panel.addView(sub);

        FrameLayout box = new FrameLayout(a);
        box.setBackground(BulkTheme.roundedBg(a, BulkTheme.iconBoxColor(a), 12,
            BulkTheme.accentBorder(a), 1));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(a, 330));
        bp.topMargin = BulkTheme.dp(a, 10);
        box.setLayoutParams(bp);

        final TextView placeholder = new TextView(a);
        placeholder.setText(p.number > 0 ? String.valueOf(p.number) : "?");
        placeholder.setTextColor(BulkTheme.textMuted(a));
        placeholder.setTextSize(44);
        placeholder.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        placeholder.setGravity(Gravity.CENTER);
        box.addView(placeholder, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final ImageView iv = new ImageView(a);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(iv, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        panel.addView(box);

        new Thread(() -> {
            Bitmap bmp = loadBitmap(a, p.number, 900);
            if (bmp == null) return;
            a.runOnUiThread(() -> {
                if (dialog.isShowing()) {
                    placeholder.setVisibility(View.GONE);
                    // v134 fit rule (preview pvB2_popup): image
                    // POORI asal aspect par — box ki unchai image
                    // ke hisab se badalti hai, crop kabhi nahi
                    // (screen ke 78% par cap).
                    iv.setAdjustViewBounds(true);
                    iv.setMaxHeight((int) (a.getResources().getDisplayMetrics().heightPixels * 0.78f));
                    iv.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                    LinearLayout.LayoutParams blp =
                        (LinearLayout.LayoutParams) box.getLayoutParams();
                    blp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    box.setLayoutParams(blp);
                    iv.setImageBitmap(bmp);
                }
            });
        }).start();

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = BulkTheme.dp(a, 14);
        row.setLayoutParams(rp);

        TextView close = BulkTheme.secondaryButton(a, BulkStrings.t(a, BulkStrings.CLOSE),
            v -> dialog.dismiss());
        if (withSelect) {
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            cl.rightMargin = BulkTheme.dp(a, 5);
            close.setLayoutParams(cl);
            row.addView(close);
            TextView select = BulkTheme.primaryButton(a,
                BulkStrings.t(a, BulkStrings.SELECT_PROJECT), v -> {
                    dialog.dismiss();
                    if (onSelect != null) onSelect.run();
                });
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            sl.leftMargin = BulkTheme.dp(a, 5);
            select.setLayoutParams(sl);
            row.addView(select);
        } else {
            close.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(close);
        }
        panel.addView(row);

        BulkTheme.showPanelDialog(a, panel);
    }
}
