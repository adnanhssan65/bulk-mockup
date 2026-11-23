package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Termux Mod (v127, Adnan 2026-11-23 17:08) — manual file-manager route.
 *
 * Owner ka bulk workflow file manager se chalta hai (hazaron wallpapers
 * ek sath select/move) — is liye app ke andar se kisi bhi project ka
 * asal folder seedha phone ke file manager me khul sakta hai, or koi
 * project FOLDER copy ki surat me (ZIP nahi) shared location par
 * export ho sakta hai:
 *   /storage/emulated/0/Download/TermuxMod/<project-folder>/
 *
 * Folder kholne ka sab se compatible tareeqa: ACTION_VIEW +
 * "resource/folder" (Samsung My Files, Mi File Manager, Solid/ES waghera
 * isay samajhte hain); na chale to "inode/directory" try hota hai; wo
 * bhi na chale to design-style dialog me poora path + Copy Path button.
 */
public final class BulkFileManager {

    /** Export ki shared manzil (hamesha dikhne wali jaga). */
    public static final File EXPORT_ROOT = new File("/storage/emulated/0/Download/TermuxMod");

    private BulkFileManager() {}

    // ------------------------------------------------------------ folder kholna

    public static void openFolder(final Activity a, final File dir) {
        if (dir == null) return;
        Uri uri = Uri.fromFile(dir);
        // Koshish 1: resource/folder (sab se zyada file managers)
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "resource/folder");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(i);
            return;
        } catch (Exception ignored) {}
        // Koshish 2: inode/directory
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "inode/directory");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(i);
            return;
        } catch (Exception ignored) {}
        // Fallback: poora path dikhao + copy ki sahulat
        showPathDialog(a, dir);
    }

    private static void showPathDialog(final Activity a, final File dir) {
        final Dialog dialog = new Dialog(a);
        LinearLayout panel = BulkTheme.modalPanel(a,
            BulkStrings.t(a, BulkStrings.NO_FILE_MANAGER), dialog);

        TextView path = new TextView(a);
        path.setText(dir.getAbsolutePath());
        path.setTextColor(BulkTheme.textSub(a));
        path.setTextSize(12);
        path.setTypeface(android.graphics.Typeface.MONOSPACE);
        panel.addView(path);

        TextView copy = BulkTheme.primaryButton(a, BulkStrings.t(a, BulkStrings.COPY_PATH), v -> {
            try {
                ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("path", dir.getAbsolutePath()));
                    BulkTheme.showToast(a, BulkStrings.t(a, BulkStrings.PATH_COPIED));
                }
            } catch (Exception ignored) {}
            dialog.dismiss();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = BulkTheme.dp(a, 14);
        copy.setLayoutParams(cp);
        panel.addView(copy);

        TextView ok = BulkTheme.secondaryButton(a, BulkStrings.t(a, BulkStrings.OK),
            v -> dialog.dismiss());
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        op.topMargin = BulkTheme.dp(a, 8);
        ok.setLayoutParams(op);
        panel.addView(ok);

        BulkTheme.showPanelDialog(a, panel);
    }

    // ------------------------------------------------------------ folder copy (pure)

    public interface CopyProgress {
        void onProgress(int pct);
    }

    /** Poora folder (files + subfolders) dst ke andar copy — dst pehle se ho to andar merge/overwrite. */
    public static boolean copyFolder(File src, File dst, CopyProgress cb) {
        if (src == null || dst == null || !src.isDirectory()) return false;
        long total = totalBytes(src);
        long[] done = {0};
        return copyRec(src, dst, total, done, cb);
    }

    private static boolean copyRec(File src, File dst, long total, long[] done, CopyProgress cb) {
        if (!dst.exists() && !dst.mkdirs()) return false;
        File[] kids = src.listFiles();
        if (kids == null) return false;
        boolean ok = true;
        for (File k : kids) {
            File target = new File(dst, k.getName());
            if (k.isDirectory()) {
                if (!copyRec(k, target, total, done, cb)) ok = false;
            } else {
                if (!copyFile(k, target)) { ok = false; continue; }
                done[0] += k.length();
                if (cb != null && total > 0) {
                    cb.onProgress((int) Math.min(100, done[0] * 100 / total));
                }
            }
        }
        return ok;
    }

    private static long totalBytes(File f) {
        if (f.isFile()) return f.length();
        long sum = 0;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) sum += totalBytes(k);
        return sum;
    }

    private static boolean copyFile(File src, File dst) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            out.flush();
            //noinspection ResultOfMethodCallIgnored
            dst.setLastModified(src.lastModified());
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    /** Folder poora delete (export se pehle purani copy saaf karne ke liye). */
    public static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
