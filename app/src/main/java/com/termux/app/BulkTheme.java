package com.termux.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.R;

/**
 * Termux Mod — v125 design system (approved HTML prototype ki native shakal).
 *
 * Dark neon-green (#76FF03 / black) + Light mint-emerald — dono themes,
 * toggle SharedPreferences me persist hota hai ("bulk_mockup" prefs,
 * key "ui_light_mode"). Tamam Bulk screens isi class se colors, rounded
 * backgrounds, icon buttons, sub-headers, toast banner or About modal
 * leti hain taake har screen design jesi hi dikhe.
 */
public final class BulkTheme {

    private static final String PREFS = "bulk_mockup";
    private static final String KEY_LIGHT = "ui_light_mode";

    /** Wave SVGs ke hardcoded neon rang — dono themes me yahi rehte hain (design bhi yahi karta hai). */
    public static final int NEON = 0xFF76FF03;
    public static final int NEON_LIGHT = 0xFFB2FF59;

    private BulkTheme() {}

    // ------------------------------------------------------------ theme state

    public static boolean isLight(Context c) {
        try {
            // v128: factory default LIGHT — sirf tab jab user ne kabhi
            // theme choose na kiya ho (saved choice hamesha respected).
            return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LIGHT, true);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setLight(Context c, boolean light) {
        try {
            SharedPreferences.Editor ed = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            ed.putBoolean(KEY_LIGHT, light);
            ed.apply();
        } catch (Exception ignored) {}
    }

    /** Toggle karke naya mode wapas karo (true = light). */
    public static boolean toggle(Context c) {
        boolean now = !isLight(c);
        setLight(c, now);
        return now;
    }

    // ------------------------------------------------------------ colors

    public static int appBg(Context c)        { return isLight(c) ? 0xFFF3F8F3 : 0xFF030603; }
    public static int accent(Context c)       { return isLight(c) ? 0xFF2E9E00 : 0xFF76FF03; }
    public static int accentSoft(Context c)   { return isLight(c) ? 0x1C2E9E00 : 0x1F76FF03; }
    public static int accentBorder(Context c) { return isLight(c) ? 0x592E9E00 : 0x5C76FF03; }
    public static int accentStrong(Context c) { return isLight(c) ? 0xB82E9E00 : 0xB876FF03; }
    public static int textMain(Context c)     { return isLight(c) ? 0xFF0D1B0E : 0xFFFFFFFF; }
    public static int textSub(Context c)      { return isLight(c) ? 0xFF2E4730 : 0xFFA5B8A6; }
    public static int textMuted(Context c)    { return isLight(c) ? 0xFF556E57 : 0xFF6E8270; }
    public static int onAccent(Context c)     { return isLight(c) ? 0xFFFFFFFF : 0xFF030803; }
    public static int glassBg(Context c)      { return isLight(c) ? 0xE0FFFFFF : 0xD60B140C; }
    public static int iconBoxColor(Context c) { return isLight(c) ? 0x1C2E9E00 : 0x1776FF03; }
    public static int cardStart(Context c)    { return isLight(c) ? 0xFFFFFFFF : 0xFF0D180E; }
    public static int cardEnd(Context c)      { return isLight(c) ? 0xFFE7F3E8 : 0xFF050905; }
    public static int drawerBg(Context c)     { return isLight(c) ? 0xFAF3F8F3 : 0xF7070D08; }
    public static int modalColor(Context c)   { return isLight(c) ? 0xFAFFFFFF : 0xF50A120B; }
    public static int danger(Context c)       { return isLight(c) ? 0xFFD32F2F : 0xFFFF5252; }
    public static int dangerSoft(Context c)   { return isLight(c) ? 0x1FD32F2F : 0x24FF5252; }
    public static int dangerBorder(Context c) { return isLight(c) ? 0x59D32F2F : 0x59FF5252; }

    // ------------------------------------------------------------ metrics

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    // ------------------------------------------------------------ drawables

    private static GradientDrawable rounded(Context c, int fill, float radiusDp, int strokeColor, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) g.setStroke(dp(c, strokeDp), strokeColor);
        return g;
    }

    /** Umum rounded background (custom fill / radius / stroke ke liye). */
    public static Drawable roundedBg(Context c, int fill, float radiusDp, int strokeColor, float strokeDp) {
        return rounded(c, fill, radiusDp, strokeColor, strokeDp);
    }

    public static Drawable glassPanel(Context c)  { return rounded(c, glassBg(c), 16, accentBorder(c), 1); }
    public static Drawable glassPanelStrong(Context c) { return rounded(c, glassBg(c), 16, accentStrong(c), 1); }
    public static Drawable pillBg(Context c)      { return rounded(c, glassBg(c), 14, accentBorder(c), 1); }
    public static Drawable primaryBg(Context c)   { return rounded(c, accent(c), 13, Color.TRANSPARENT, 0); }
    public static Drawable secondaryBg(Context c) { return rounded(c, accentSoft(c), 13, accentStrong(c), 1); }
    public static Drawable dangerBg(Context c)    { return rounded(c, dangerSoft(c), 12, dangerBorder(c), 1); }
    public static Drawable iconBoxBg(Context c)   { return rounded(c, iconBoxColor(c), 10, accentBorder(c), 1); }
    public static Drawable tileBg(Context c)      { return rounded(c, appBg(c), 12, accentBorder(c), 1); }
    /** Tile ka selected (checked) variant — strong accent border. */
    public static Drawable tileBg(Context c, boolean checked) {
        return checked ? rounded(c, appBg(c), 12, accentStrong(c), 1) : tileBg(c);
    }
    public static Drawable modalBg(Context c)     { return rounded(c, modalColor(c), 16, accentStrong(c), 1); }
    public static Drawable drawerRowBg(Context c, boolean active) {
        return active ? rounded(c, accentSoft(c), 12, accentBorder(c), 1)
                      : rounded(c, Color.TRANSPARENT, 12, Color.TRANSPARENT, 0);
    }
    public static Drawable spinnerBg(Context c, boolean strong) {
        return rounded(c, appBg(c), 12, strong ? accentStrong(c) : accentBorder(c), 1);
    }

    public static Drawable circleBg(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(glassBg(c));
        g.setStroke(dp(c, 1), accentBorder(c));
        return g;
    }

    public static Drawable arrowCircleBg(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(accentSoft(c));
        g.setStroke(dp(c, 1.5f), accentStrong(c));
        return g;
    }

    public static Drawable accentCircleBg(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(accent(c));
        return g;
    }

    public static Drawable heroCardBg(Context c) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{cardStart(c), cardEnd(c)});
        g.setCornerRadius(dp(c, 15));
        g.setStroke(dp(c, 1.5f), accentStrong(c));
        return g;
    }

    public static Drawable accentBarBg(Context c) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{accent(c), Color.TRANSPARENT});
        g.setCornerRadius(dp(c, 999));
        return g;
    }

    public static Drawable lineBg(Context c, boolean fromAccent) {
        GradientDrawable g = new GradientDrawable(
            fromAccent ? GradientDrawable.Orientation.LEFT_RIGHT : GradientDrawable.Orientation.RIGHT_LEFT,
            new int[]{Color.TRANSPARENT, accent(c)});
        return g;
    }

    public static Drawable hLine(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(accentBorder(c));
        return g;
    }

    // ------------------------------------------------------------ views

    /** Tint kiya hua vector icon. */
    public static ImageView icon(Context c, int resId, int color, int sizeDp) {
        ImageView iv = new ImageView(c);
        try {
            Drawable d = c.getResources().getDrawable(resId, c.getTheme());
            if (d != null) {
                d = d.mutate();
                d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                iv.setImageDrawable(d);
            }
        } catch (Exception ignored) {}
        int s = dp(c, sizeDp);
        iv.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return iv;
    }

    /** 34dp (ya di gayi size) ka gol glass button — andar tint kiya icon. */
    public static LinearLayout circleButton(Context c, int resId, int sizeDp, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(c);
        box.setGravity(Gravity.CENTER);
        box.setBackground(circleBg(c));
        int s = dp(c, sizeDp);
        box.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        ImageView iv = icon(c, resId, accent(c), sizeDp >= 34 ? 16 : 14);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp >= 34 ? 16 : 14), dp(c, sizeDp >= 34 ? 16 : 14)));
        box.addView(iv);
        if (click != null) box.setOnClickListener(click);
        box.setClickable(true);
        return box;
    }

    /** Theme toggle wala gol button — dark me sun, light me moon (design jesa). */
    public static LinearLayout themeButton(final Activity a) {
        int res = isLight(a) ? R.drawable.ic_bulk_moon : R.drawable.ic_bulk_sun;
        return circleButton(a, res, 34, v -> {
            toggle(a);
            a.recreate();
        });
    }

    /** Sub-screens ka mushtarika header: back gol button + title/subtitle + theme button, neeche border line. */
    public static LinearLayout subHeader(final Activity a, String title, String subtitle) {
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(a, 20), dp(a, 14), dp(a, 20), dp(a, 10));

        bar.addView(circleButton(a, R.drawable.ic_bulk_back, 34, v -> a.finish()));

        LinearLayout titles = new LinearLayout(a);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tlp.setMargins(dp(a, 10), 0, dp(a, 8), 0);
        titles.setLayoutParams(tlp);

        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(textMain(a));
        titles.addView(t);

        TextView s = new TextView(a);
        s.setText(subtitle);
        s.setTextSize(11);
        s.setTextColor(textMuted(a));
        titles.addView(s);
        bar.addView(titles);

        bar.addView(themeButton(a));
        wrap.addView(bar);

        View line = new View(a);
        line.setBackground(hLine(a));
        wrap.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(a, 0.6f))));
        return wrap;
    }

    /** Neon primary button (design ki .neon-btn-primary). */
    public static TextView primaryButton(Context c, String text, View.OnClickListener click) {
        TextView b = new TextView(c);
        b.setText(text);
        b.setTextSize(13.5f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(onAccent(c));
        b.setGravity(Gravity.CENTER);
        b.setBackground(primaryBg(c));
        b.setPadding(dp(c, 16), dp(c, 11), dp(c, 16), dp(c, 11));
        if (click != null) b.setOnClickListener(click);
        b.setClickable(true);
        return b;
    }

    /** Accent-outline secondary button (design ki .neon-btn-secondary). */
    public static TextView secondaryButton(Context c, String text, View.OnClickListener click) {
        TextView b = new TextView(c);
        b.setText(text);
        b.setTextSize(13.5f);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(accent(c));
        b.setGravity(Gravity.CENTER);
        b.setBackground(secondaryBg(c));
        b.setPadding(dp(c, 16), dp(c, 11), dp(c, 16), dp(c, 11));
        if (click != null) b.setOnClickListener(click);
        b.setClickable(true);
        return b;
    }

    /** Step panel ka header label: "STEP 1 • SELECT PROJECT" jesa accent bold chhota text. */
    public static TextView stepLabel(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(11);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(accent(c));
        t.setAllCaps(true);
        try { t.setLetterSpacing(0.08f); } catch (Exception ignored) {}
        return t;
    }

    /** Horizontal progress track + fill (design ki progress bar). fill ko tag se pakro: setProgress(track, pct). */
    public static LinearLayout progressTrack(Context c) {
        LinearLayout track = new LinearLayout(c);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(rounded(c, appBg(c), 999, Color.TRANSPARENT, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 8));
        track.setLayoutParams(lp);
        View fill = new View(c);
        fill.setBackground(rounded(c, accent(c), 999, Color.TRANSPARENT, 0));
        fill.setTag("fill");
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0));
        return track;
    }

    public static void setProgress(LinearLayout track, int pct) {
        if (track == null) return;
        View fill = track.findViewWithTag("fill");
        if (fill == null) return;
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) fill.getLayoutParams();
        lp.weight = pct <= 0 ? 0.001f : pct;
        fill.setLayoutParams(lp);
        // spacer taake fill sirf pct hissa le
        if (track.getChildCount() < 2) {
            View spacer = new View(track.getContext());
            spacer.setTag("spacer");
            track.addView(spacer, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100));
        }
        View spacer = track.findViewWithTag("spacer");
        if (spacer != null) {
            LinearLayout.LayoutParams slp = (LinearLayout.LayoutParams) spacer.getLayoutParams();
            slp.weight = 100 - (pct <= 0 ? 0 : pct);
            spacer.setLayoutParams(slp);
        }
        track.requestLayout();
    }

    // ------------------------------------------------------------ window

    public static void applyWindow(Activity a) {
        try {
            Window w = a.getWindow();
            w.setStatusBarColor(appBg(a));
            w.setNavigationBarColor(appBg(a));
            View decor = w.getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (isLight(a)) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(flags);
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------ toast banner (design style)

    private static final Handler TOAST_HANDLER = new Handler(Looper.getMainLooper());
    private static View sToastBanner;
    private static Runnable sToastHide;

    /** Design wali neeche wali toast banner: modal panel + accent dot + text, ~2.8s. */
    public static void showToast(final Activity a, final String message) {
        if (a == null || a.isFinishing()) return;
        a.runOnUiThread(() -> {
            try {
                ViewGroup content = a.findViewById(android.R.id.content);
                if (content == null) return;
                if (sToastHide != null) TOAST_HANDLER.removeCallbacks(sToastHide);
                if (sToastBanner != null && sToastBanner.getParent() != null) {
                    ((ViewGroup) sToastBanner.getParent()).removeView(sToastBanner);
                }

                LinearLayout banner = new LinearLayout(a);
                banner.setOrientation(LinearLayout.HORIZONTAL);
                banner.setGravity(Gravity.CENTER_VERTICAL);
                banner.setBackground(modalBg(a));
                banner.setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 10));

                View dot = new View(a);
                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(accent(a));
                dot.setBackground(dotBg);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(a, 10), dp(a, 10));
                dlp.setMargins(0, 0, dp(a, 9), 0);
                dot.setLayoutParams(dlp);
                banner.addView(dot);

                TextView text = new TextView(a);
                text.setText(message);
                text.setTextSize(12.5f);
                text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                text.setTextColor(textMain(a));
                banner.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                lp.setMargins(dp(a, 18), 0, dp(a, 18), dp(a, 18));
                content.addView(banner, lp);
                banner.setAlpha(0f);
                banner.animate().alpha(1f).setDuration(150).start();
                sToastBanner = banner;

                sToastHide = () -> {
                    try {
                        banner.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                            if (banner.getParent() != null) ((ViewGroup) banner.getParent()).removeView(banner);
                        }).start();
                    } catch (Exception ignored) {}
                };
                TOAST_HANDLER.postDelayed(sToastHide, 2800);
            } catch (Exception ignored) {}
        });
    }

    // ------------------------------------------------------------ confirm / choice modals (design style)

    public interface OnChoice {
        void onChoice(int index);
    }

    public static LinearLayout modalPanel(final Activity a, String title, final Dialog dialog) {
        LinearLayout panel = new LinearLayout(a);
        // v128 fix: panel par caller ka dialog tag kar do — showPanelDialog
        // isi ko dikhayegi. Pehle showPanelDialog apna ALAG dialog bana kar
        // dikhati thi, is liye tamam close/dismiss us phantom (kabhi na
        // dikhne wale) dialog par jate thay aur asal popup screen par
        // ruka rehta tha (Quick Run / Manual / Processing teeno par).
        panel.setTag(dialog);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(modalBg(a));
        panel.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));

        LinearLayout titleRow = new LinearLayout(a);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(accent(a));
        titleRow.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        titleRow.addView(circleButton(a, R.drawable.ic_bulk_close, 28, v -> dialog.dismiss()));
        panel.addView(titleRow);

        View line = new View(a);
        line.setBackground(hLine(a));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(a, 0.6f)));
        lineLp.setMargins(0, dp(a, 10), 0, dp(a, 12));
        line.setLayoutParams(lineLp);
        panel.addView(line);
        return panel;
    }

    public static Dialog showPanelDialog(Activity a, LinearLayout panel) {
        // v128 fix: modalPanel ne caller ka dialog tag kiya ho to WAHI
        // dikhao (us ke close/dismiss handlers usi par lage hain);
        // warna pehle ki tarah naya dialog banao.
        Dialog dialog = (panel.getTag() instanceof Dialog)
            ? (Dialog) panel.getTag() : new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // v127: muntakhab font har popup par bhi lage
        BulkFonts.applyToTree(a, panel);
        dialog.setContentView(panel);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setLayout((int) (a.getResources().getDisplayMetrics().widthPixels * 0.88),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
        return dialog;
    }

    /** Design-style confirm: message + Cancel / Confirm buttons. Confirm danger ho to red. */
    public static void showConfirm(final Activity a, String title, String message,
                                   String confirmText, boolean danger, final Runnable onConfirm) {
        final Dialog dialog = new Dialog(a);
        LinearLayout panel = modalPanel(a, title, dialog);

        TextView body = new TextView(a);
        body.setText(message);
        body.setTextSize(12.5f);
        body.setTextColor(textSub(a));
        body.setLineSpacing(0, 1.35f);
        panel.addView(body);

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(a, 16), 0, 0);
        row.setLayoutParams(rowLp);

        TextView cancel = secondaryButton(a, BulkStrings.t(a, BulkStrings.CANCEL), v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        cLp.setMargins(0, 0, dp(a, 5), 0);
        cancel.setLayoutParams(cLp);
        row.addView(cancel);

        TextView confirm;
        if (danger) {
            confirm = new TextView(a);
            confirm.setText(confirmText);
            confirm.setTextSize(13.5f);
            confirm.setTypeface(Typeface.DEFAULT_BOLD);
            confirm.setTextColor(0xFFFFFFFF);
            confirm.setGravity(Gravity.CENTER);
            confirm.setBackground(rounded(a, danger(a), 13, Color.TRANSPARENT, 0));
            confirm.setPadding(dp(a, 16), dp(a, 11), dp(a, 16), dp(a, 11));
            confirm.setClickable(true);
        } else {
            confirm = primaryButton(a, confirmText, null);
        }
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });
        LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        oLp.setMargins(dp(a, 5), 0, 0, 0);
        confirm.setLayoutParams(oLp);
        row.addView(confirm);
        panel.addView(row);

        showPanelDialog(a, panel);
    }

    /** Design-style choice list (radio jesi rows) — tap par dismiss + callback. */
    public static void showChoice(final Activity a, String title, final String[] options, final OnChoice cb) {
        final Dialog dialog = new Dialog(a);
        LinearLayout panel = modalPanel(a, title, dialog);
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            TextView row = new TextView(a);
            row.setText(options[i]);
            row.setTextSize(13.5f);
            row.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.setTextColor(textMain(a));
            row.setPadding(dp(a, 4), dp(a, 11), dp(a, 4), dp(a, 11));
            row.setClickable(true);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                if (cb != null) cb.onChoice(idx);
            });
            panel.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (i < options.length - 1) {
                View line = new View(a);
                line.setBackground(hLine(a));
                panel.addView(line, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(a, 0.6f))));
            }
        }
        showPanelDialog(a, panel);
    }

    // ------------------------------------------------------------ About modal (design style)

    /** Design ke info-modal jesa About dialog — wahi text jo prototype me hai. */
    public static void showAboutDialog(final Activity a) {
        final Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(modalBg(a));
        panel.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));

        LinearLayout titleRow = new LinearLayout(a);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(a);
        title.setText(BulkStrings.t(a, BulkStrings.ABOUT_TITLE));
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(accent(a));
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        titleRow.addView(circleButton(a, R.drawable.ic_bulk_close, 28, v -> dialog.dismiss()));
        panel.addView(titleRow);

        View line = new View(a);
        line.setBackground(hLine(a));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(a, 0.6f)));
        lineLp.setMargins(0, dp(a, 10), 0, dp(a, 12));
        line.setLayoutParams(lineLp);
        panel.addView(line);

        TextView body = new TextView(a);
        body.setText(BulkStrings.t(a, BulkStrings.ABOUT_BODY));
        body.setTextSize(12.5f);
        body.setTextColor(textSub(a));
        body.setLineSpacing(0, 1.35f);
        panel.addView(body);

        LinearLayout devBox = new LinearLayout(a);
        devBox.setOrientation(LinearLayout.VERTICAL);
        devBox.setBackground(rounded(a, accentSoft(a), 12, accentBorder(a), 1));
        devBox.setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10));
        LinearLayout.LayoutParams devLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        devLp.setMargins(0, dp(a, 8), 0, 0);
        devBox.setLayoutParams(devLp);
        TextView devName = new TextView(a);
        devName.setText("Developer: Mehar Adnan");
        devName.setTextSize(12.5f);
        devName.setTypeface(Typeface.DEFAULT_BOLD);
        devName.setTextColor(textMain(a));
        devBox.addView(devName);
        TextView devContact = new TextView(a);
        devContact.setText("Contact: +44 7404 032136");
        devContact.setTextSize(11.5f);
        devContact.setTypeface(Typeface.MONOSPACE);
        devContact.setTextColor(accent(a));
        devBox.addView(devContact);
        panel.addView(devBox);

        TextView close = primaryButton(a, BulkStrings.t(a, BulkStrings.CLOSE), v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        closeLp.setMargins(0, dp(a, 16), 0, 0);
        close.setLayoutParams(closeLp);
        panel.addView(close);

        BulkFonts.applyToTree(a, panel);
        dialog.setContentView(panel);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setLayout((int) (a.getResources().getDisplayMetrics().widthPixels * 0.88), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }
}
