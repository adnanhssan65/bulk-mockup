package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;

/**
 * Termux Mod (v145) — More Settings screen.
 *
 * Sidebar (drawer) lambi ho gayi thi — owner ke kehne par Backup,
 * Export Project aur Output Folder rows drawer se hat kar is apni
 * screen par ah gayi hain. Drawer me in ki jagah ek hi chhoti row
 * "More Settings" hai jo ye screen kholti hai. Teenon options ka
 * behavior bilkul wahi hai jo drawer rows ka tha — sirf jagah
 * badli hai: Backup → wahi Backup popup (BulkMoreActions),
 * Export Project → wahi export flow (BulkMoreActions),
 * Output Folder → wahi Output Folder screen.
 */
public class BulkMoreSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        setContentView(buildUi());
        BulkFonts.apply(this);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));
        root.addView(BulkTheme.subHeader(this,
            BulkStrings.t(this, BulkStrings.MORE_SETTINGS),
            BulkStrings.t(this, BulkStrings.MORE_SETTINGS_SUB)));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll);

        body.addView(menuRow(BulkStrings.t(this, BulkStrings.BACKUP),
            v -> BulkMoreActions.showBackupPopup(this)));
        body.addView(menuRow(BulkStrings.t(this, BulkStrings.EXPORT_PROJECT),
            v -> BulkMoreActions.startExportFlow(this)));
        body.addView(menuRow(BulkStrings.t(this, BulkStrings.OUTPUT_FOLDER), v -> {
            try {
                startActivity(new Intent(this, BulkOutputFolderActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Output Folder screen nahi khul saki.",
                    Toast.LENGTH_SHORT).show();
            }
        }));
        return root;
    }

    /** Chhoti si tappable row: label + chevron (owner ka UI rule). */
    private LinearLayout menuRow(String label, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(BulkTheme.pillBg(this));
        row.setPadding(BulkTheme.dp(this, 14), BulkTheme.dp(this, 12),
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, BulkTheme.dp(this, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);
        row.setOnClickListener(click);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(14);
        text.setTypeface(medium());
        text.setTextColor(BulkTheme.textMain(this));
        row.addView(text, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        android.widget.ImageView chev = BulkTheme.icon(this, R.drawable.ic_bulk_chevron,
            BulkTheme.textMuted(this), 14);
        chev.setLayoutParams(new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 14), BulkTheme.dp(this, 14)));
        row.addView(chev);
        return row;
    }

    private static Typeface medium() {
        try {
            return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }
}
