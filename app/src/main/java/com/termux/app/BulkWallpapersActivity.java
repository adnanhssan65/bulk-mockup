package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.R;

/**
 * Termux Mod (v127, v129 me dobara pakka) — "Add Wallpapers" HUB.
 * Teen chhoti pill rows:
 *   1) Add Wallpapers        → BulkStageActivity (staging / Drop Box)
 *   2) Duplicate Check       → BulkDuplicatesActivity (amber badge sirf
 *                              jab count > 0 ho)
 *   3) Preview Added Wallpapers → BulkPreviewActivity
 * Asal staging engine BulkStageActivity me hai; ye screen sirf rasta hai.
 * Duplicates expiry (TTL) hub khulne par chalti hai, badge refresh hota hai.
 *
 * v129: user ke phone par hub sirf pehli row dikha raha tha (baqi
 * options ghayab) — is liye ab har row EXPLICIT fixed height (56dp)
 * ke sath ScrollView ke andar banti hai; koi row measure me dab
 * nahi sakti, teeno hamesha nazar ati hain.
 */
public class BulkWallpapersActivity extends Activity {

    private TextView mDupBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BulkTheme.applyWindow(this);
        if (!BulkStorageAccess.hasAccess(this)) {
            setContentView(buildPermissionUi());
            BulkFonts.apply(this);
            return;
        }
        setContentView(buildUi());
        BulkFonts.apply(this);
        new Thread(() -> {
            BulkDuplicatesStore.runExpiry(this);
            runOnUiThread(this::refreshDupBadge);
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDupBadge();
    }

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

        TextView permBtn = BulkTheme.primaryButton(this, BulkStrings.t(this, BulkStrings.PERMISSIONS), null);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = BulkTheme.dp(this, 14);
        permBtn.setLayoutParams(pp);
        permBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, BulkPermissionsActivity.class));
            finish();
        });
        panel.addView(permBtn);

        TextView closeBtn = BulkTheme.secondaryButton(this, BulkStrings.t(this, BulkStrings.CLOSE), null);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = BulkTheme.dp(this, 10);
        closeBtn.setLayoutParams(cp);
        closeBtn.setOnClickListener(v -> finish());
        panel.addView(closeBtn);

        root.addView(panel);
        return root;
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BulkTheme.appBg(this));

        root.addView(BulkTheme.subHeader(this, BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS),
            BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS_SUBTITLE)));

        // v129: ScrollView ke andar body — teeno rows hamesha poori
        // height ke sath, koi measure quirk inhe daba nahi sakta.
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BulkTheme.dp(this, 18), BulkTheme.dp(this, 14),
            BulkTheme.dp(this, 18), BulkTheme.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll);

        body.addView(hubRow(R.drawable.ic_bulk_image,
            BulkStrings.t(this, BulkStrings.ADD_WALLPAPERS), null,
            () -> startActivity(new Intent(this, BulkStageActivity.class))),
            rowLp(0));

        mDupBadge = new TextView(this);
        body.addView(hubRow(R.drawable.ic_bulk_copy,
            BulkStrings.t(this, BulkStrings.DUPLICATE_CHECK), mDupBadge,
            () -> startActivity(new Intent(this, BulkDuplicatesActivity.class))),
            rowLp(12));

        body.addView(hubRow(R.drawable.ic_bulk_eye,
            BulkStrings.t(this, BulkStrings.PREVIEW_ADDED), null,
            () -> startActivity(new Intent(this, BulkPreviewActivity.class))),
            rowLp(12));

        refreshDupBadge();
        return root;
    }

    /** Hub row ki pakki height — 56dp, kabhi wrap-measure par nahi. */
    private LinearLayout.LayoutParams rowLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, BulkTheme.dp(this, 56));
        lp.topMargin = BulkTheme.dp(this, topDp);
        return lp;
    }

    /** Design ki pill row: icon box + divider + label + (badge) + arrow circle. */
    private LinearLayout hubRow(int iconRes, String label, TextView badge, final Runnable onTap) {
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
        ImageView iv = BulkTheme.icon(this, iconRes, BulkTheme.accent(this), 17);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
            BulkTheme.dp(this, 17), BulkTheme.dp(this, 17));
        ip.gravity = Gravity.CENTER;
        iv.setLayoutParams(ip);
        iconBox.addView(iv);
        row.addView(iconBox);

        View divider = new View(this);
        divider.setBackgroundColor(BulkTheme.accentBorder(this));
        LinearLayout.LayoutParams dv = new LinearLayout.LayoutParams(
            BulkTheme.dp(this, 1), BulkTheme.dp(this, 22));
        dv.leftMargin = BulkTheme.dp(this, 11);
        divider.setLayoutParams(dv);
        row.addView(divider);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(BulkTheme.textMain(this));
        tv.setTextSize(14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = BulkTheme.dp(this, 11);
        tv.setLayoutParams(lp);
        row.addView(tv);

        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (badge != null) {
            badge.setTextSize(11);
            badge.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            badge.setTextColor(0xFF1A1200);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(BulkTheme.roundedBg(this, 0xFFFFB300, 9, 0, 0));
            badge.setPadding(BulkTheme.dp(this, 7), BulkTheme.dp(this, 2),
                BulkTheme.dp(this, 7), BulkTheme.dp(this, 2));
            badge.setVisibility(View.GONE);
            row.addView(badge);
        }

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

        row.setOnClickListener(v -> { if (onTap != null) onTap.run(); });
        return row;
    }

    private void refreshDupBadge() {
        if (mDupBadge == null) return;
        int n = BulkDuplicatesStore.count(this);
        mDupBadge.setText(String.valueOf(n));
        // Design: amber badge sirf jab duplicates hon
        mDupBadge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
    }
}
