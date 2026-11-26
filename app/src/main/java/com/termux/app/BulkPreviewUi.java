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
import java.io.FileOutputStream;
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

    // ------------------------------------------------------------ v149 saved preview

    /** App ke andar save shuda persistent preview ki file (bulk_previews/p<N>.jpg). */
    public static File savedPreviewFile(Context c, int number) {
        if (c == null || number <= 0) return null;
        try {
            return new File(new File(c.getFilesDir(), "bulk_previews"),
                "p" + number + ".jpg");
        } catch (Exception e) {
            return null;
        }
    }

    /** Asal mockup file se saved preview banao (width ≤720, JPEG q85). Kabhi throw nahi karta. */
    private static void writeSavedPreview(Context c, int number, File mockFile) {
        File saved = savedPreviewFile(c, number);
        if (saved == null || mockFile == null || !mockFile.isFile()) return;
        Bitmap bmp = decodeFile(mockFile, 720);
        if (bmp == null) return;
        saveScaledJpeg(bmp, saved);
    }

    /** Bitmap ko width ≤720 par scale karke JPEG q85 me save karo (temp + rename). */
    private static void saveScaledJpeg(Bitmap bmp, File dest) {
        try {
            Bitmap out = bmp;
            if (bmp.getWidth() > 720) {
                int h = Math.max(1, Math.round(
                    bmp.getHeight() * (720f / bmp.getWidth())));
                out = Bitmap.createScaledBitmap(bmp, 720, h, true);
            }
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            File tmp = new File(parent, dest.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                out.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            }
            if (!tmp.renameTo(dest)) {
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    out.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                }
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        } catch (Exception ignored) {}
    }

    /**
     * ZIP-add ke baad: saved preview na ho to project folder ke
     * template asset (mockup.png / mockup.jpg / template.jpg /
     * template.png — root pehle, phir assets/) se bana do, taake
     * preview pehle run se pehle hi nazar aye. Template na mile
     * to khali rehne do — pehla run rule 2 se bana de ga. Koi
     * bhi fail import ko kabhi nahi rokta.
     */
    public static void ensureSavedPreviewFromTemplate(Context c, int number, File projectDir) {
        try {
            if (c == null || number <= 0
                || projectDir == null || !projectDir.isDirectory()) return;
            File saved = savedPreviewFile(c, number);
            if (saved == null || saved.isFile()) return;
            List<String> rootNames = fileNames(projectDir);
            File assetsDir = new File(projectDir, "assets");
            List<String> assetsNames = assetsDir.isDirectory()
                ? fileNames(assetsDir) : null;
            String rel = BulkPreviewSource.pickTemplate(rootNames, assetsNames);
            if (rel == null) return;
            File template = new File(projectDir, rel);
            if (!template.isFile()) return;
            Bitmap bmp = decodeFile(template, 720);
            if (bmp == null) return;
            saveScaledJpeg(bmp, saved);
        } catch (Exception ignored) {}
    }

    private static List<String> fileNames(File dir) {
        List<String> out = new ArrayList<>();
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File k : kids) if (k.isFile()) out.add(k.getName());
        }
        return out;
    }

    // ------------------------------------------------------------ bitmap

    /**
     * Preview bitmap (sampled, reqSize tak) — PLACEHOLDER par null.
     * Background thread se call karo (disk parhna hai); cache hit ho
     * to foran wapas.
     *
     * v149 tartib: SAVED → REAL → BUNDLED → placeholder. Kisi
     * source ka decode fail ho to agli source par girte hain —
     * placeholder sirf tab jab waqai kuch na mile. REAL istemal
     * ho to saved preview (dobara) likhi jati hai taake asal
     * mockups delete hone ke baad bhi DP mehfooz rahe.
     */
    public static Bitmap loadBitmap(Context c, int number, int reqSize) {
        File saved = savedPreviewFile(c, number);
        boolean savedExists = saved != null && saved.isFile();
        BulkPreviewSource.Choice choice = choiceFor(c, number);
        List<Integer> order = BulkPreviewSource.loadOrder(savedExists, choice);
        for (int kind : order) {
            Bitmap bmp = null;
            if (kind == BulkPreviewSource.KIND_SAVED) {
                String key = "saved|" + saved.getAbsolutePath() + "|"
                    + saved.lastModified() + "|" + reqSize;
                bmp = CACHE.get(key);
                if (bmp == null) {
                    bmp = decodeFile(saved, reqSize);
                    if (bmp != null) {
                        CACHE.put(key, bmp);
                    } else {
                        // Kharab saved file — hata do (agli source
                        // par jao; dobara ban bhi sake gi)
                        //noinspection ResultOfMethodCallIgnored
                        saved.delete();
                    }
                }
            } else if (kind == BulkPreviewSource.KIND_REAL) {
                File file = new File(BulkProjects.mockupsDir(number), choice.fileName);
                String key = "real|" + file.getAbsolutePath() + "|"
                    + file.lastModified() + "|" + reqSize;
                bmp = CACHE.get(key);
                if (bmp == null) {
                    bmp = decodeFile(file, reqSize);
                    if (bmp != null) {
                        CACHE.put(key, bmp);
                        writeSavedPreview(c, number, file);
                    }
                } else if (saved != null && !saved.isFile()) {
                    writeSavedPreview(c, number, file);
                }
            } else if (kind == BulkPreviewSource.KIND_BUNDLED) {
                String key = "bundled|" + number + "|" + reqSize;
                bmp = CACHE.get(key);
                if (bmp == null) {
                    bmp = decodeAsset(c,
                        BulkPreviewSource.bundledAssetPath(number), reqSize);
                    if (bmp != null) CACHE.put(key, bmp);
                }
            }
            if (bmp != null) return bmp;
        }
        return null;
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
