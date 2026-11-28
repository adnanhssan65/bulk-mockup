package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Termux Mod v156 — Pending Delete ka SHARED popup:
 * More Settings ki Pending row or Home ke badge icon,
 * dono jagah bilkul ek hi popup.
 *
 * Khulne se pehle ghost entries khamoshi se saaf hoti
 * hain (jin ki file + gallery record dono pehle se
 * gayab hain — BulkPendingDeletes.isGhost), phir list
 * or "Delete Now" button. Delete Now Stage screen
 * kholta hai — wahan wahi asal delete flow
 * (BulkStageActivity.processPendingDeletes, mojooda
 * Move Delete mode ke order se, batch popup aakhir
 * me) inhi persisted items par chalta hai. Is popup
 * se bahir koi item kabhi delete nahi hota.
 *
 * Popup sirf button se band hota hai (showPanelDialog
 * ka lock); band karne se list par koi asar nahi —
 * item sirf asal delete ya ghost-prune se nikalta hai.
 */
public final class BulkPendingUi {

    /** Popup me zyada se zyada itne naam — baqi "+N" ki line. */
    public static final int MAX_LISTED = 30;

    private BulkPendingUi() {}

    // ------------------------------------------------------------ pure

    /**
     * Item ka dikhai dene wala naam (pure): asal path
     * ho to file ka naam, warna URI ka aakhri hissa.
     */
    public static String nameFor(String path, String uriStr) {
        String src = (path != null && !path.isEmpty()) ? path : uriStr;
        if (src == null || src.isEmpty()) return "?";
        int i = src.lastIndexOf('/');
        String name = (i >= 0 && i + 1 < src.length()) ? src.substring(i + 1) : src;
        return name.isEmpty() ? src : name;
    }

    // ------------------------------------------------------------ device

    /** MediaStore DATA se asal path (na mile to null). */
    private static String resolvePath(Activity a, String uriStr) {
        Cursor c = null;
        try {
            c = a.getContentResolver().query(Uri.parse(uriStr),
                new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (c == null || !c.moveToFirst()) return null;
            String p = c.getString(0);
            return (p == null || p.isEmpty()) ? null : p;
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * File + gallery record dono gayab hon to ghost.
     * Shakk wali surat (query fail) me item REHTA hai —
     * list ghalti se kabhi na ghate.
     */
    public static boolean isGhostItem(Activity a, String uriStr) {
        try {
            Uri uri = Uri.parse(uriStr);
            boolean rowExists = false;
            String path = null;
            Cursor c = null;
            try {
                c = a.getContentResolver().query(uri,
                    new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                if (c != null && c.moveToFirst()) {
                    rowExists = true;
                    path = c.getString(0);
                }
            } finally {
                if (c != null) c.close();
            }
            boolean fileExists = path != null && !path.isEmpty()
                && new File(path).exists();
            return BulkPendingDeletes.isGhost(fileExists, rowExists);
        } catch (Exception e) {
            return false;
        }
    }

    /** Ghost entries peeche saaf karo; ho jaye to after UI thread par chale. */
    public static void pruneGhosts(final Activity a, final Runnable after) {
        new Thread(() -> {
            try {
                Set<String> set = BulkPendingDeletes.load(a);
                List<String> ghosts = new ArrayList<>();
                for (String s : set) {
                    if (isGhostItem(a, s)) ghosts.add(s);
                }
                if (!ghosts.isEmpty()) BulkPendingDeletes.removeAll(a, ghosts);
            } catch (Exception ignored) {}
            if (after != null) {
                try {
                    a.runOnUiThread(after);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    /** Wohi popup — More Settings row or Home badge, dono se. */
    public static void showPendingPopup(final Activity a) {
        pruneGhosts(a, () -> {
            if (a.isFinishing()) return;
            final List<String> items =
                BulkPendingDeletes.listOf(BulkPendingDeletes.load(a));
            final Dialog dialog = new Dialog(a);
            LinearLayout panel = BulkTheme.modalPanel(a,
                BulkStrings.t(a, BulkStrings.PENDING_DELETE), dialog);
            if (items.isEmpty()) {
                TextView empty = new TextView(a);
                empty.setText(BulkStrings.t(a, BulkStrings.PENDING_DELETE_EMPTY));
                empty.setTextSize(13);
                empty.setTextColor(BulkTheme.textMuted(a));
                empty.setPadding(0, BulkTheme.dp(a, 4), 0, BulkTheme.dp(a, 6));
                panel.addView(empty);
            } else {
                int shown = Math.min(items.size(), MAX_LISTED);
                for (int i = 0; i < shown; i++) {
                    String uriStr = items.get(i);
                    TextView tv = new TextView(a);
                    tv.setText(nameFor(resolvePath(a, uriStr), uriStr));
                    tv.setTextSize(13);
                    tv.setTextColor(BulkTheme.textMain(a));
                    tv.setPadding(BulkTheme.dp(a, 2), BulkTheme.dp(a, 5),
                        BulkTheme.dp(a, 2), BulkTheme.dp(a, 5));
                    panel.addView(tv);
                }
                if (items.size() > shown) {
                    TextView more = new TextView(a);
                    more.setText("… +" + (items.size() - shown));
                    more.setTextSize(12.5f);
                    more.setTextColor(BulkTheme.textMuted(a));
                    panel.addView(more);
                }
                TextView now = BulkTheme.primaryButton(a,
                    BulkStrings.t(a, BulkStrings.PENDING_DELETE_NOW), v -> {
                        dialog.dismiss();
                        try {
                            a.startActivity(new Intent(a, BulkStageActivity.class));
                        } catch (Exception e) {
                            Toast.makeText(a, "Add Wallpapers screen nahi khul saki.",
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
                nLp.topMargin = BulkTheme.dp(a, 12);
                now.setLayoutParams(nLp);
                panel.addView(now);
            }
            BulkTheme.showPanelDialog(a, panel);
        });
    }
}
