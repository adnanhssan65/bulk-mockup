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
import android.view.WindowManager;
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
    // v133: theme ab BulkPalettes ki table se ata hai — "ui_theme" pref
    // me theme id persist hoti hai; purani "ui_light_mode" boolean sirf
    // pehli mapping ke liye parhi jati hai (false -> Dark). Factory
    // default = Default theme (purana Light, byte-for-byte wahi rang).

    /** Is waqt muntakhab theme ki palette. */
    public static BulkPalettes.Palette currentPalette(Context c) {
        try {
            SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String saved = sp.getString(BulkPalettes.PREF_KEY, null);
            Boolean legacy = sp.contains(KEY_LIGHT)
                ? sp.getBoolean(KEY_LIGHT, true) : null;
            return BulkPalettes.resolve(saved, legacy);
        } catch (Exception e) {
            return BulkPalettes.defaultPalette();
        }
    }

    /** Theme select karke persist karo (id ghalat ho to kuch nahi hota). */
    public static void setTheme(Context c, String themeId) {
        if (BulkPalettes.byId(themeId) == null) return;
        try {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(BulkPalettes.PREF_KEY, themeId)
                .apply();
        } catch (Exception ignored) {}
    }

    public static boolean isLight(Context c) {
        return currentPalette(c).light;
    }

    public static void setLight(Context c, boolean light) {
        setTheme(c, light ? BulkPalettes.ID_DEFAULT : BulkPalettes.ID_DARK);
    }

    /** Toggle karke naya mode wapas karo (true = light). */
    public static boolean toggle(Context c) {
        boolean now = !isLight(c);
        setLight(c, now);
        return now;
    }

    // ------------------------------------------------------------ colors

    public static int appBg(Context c)        { return currentPalette(c).bg; }

    // v135: Control Panel ka Accent override — set ho to palette ke
    // accent ki jagah wahi rang (shades palette wale alpha se);
    // override na ho to palette values byte-for-byte waisa hi.
    public static int accent(Context c) {
        BulkPalettes.Palette p = currentPalette(c);
        return BulkPanelSettings.effectiveAccent(p.accent, BulkPanelSettings.accentOverride(c));
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static int shadeOr(Context c, int paletteValue) {
        int ov = BulkPanelSettings.accentOverride(c);
        if (ov == BulkPanelSettings.ACCENT_NONE) return paletteValue;
        return withAlpha(ov, Color.alpha(paletteValue));
    }

    public static int accentSoft(Context c)   { return shadeOr(c, currentPalette(c).accentSoft); }
    public static int accentBorder(Context c) { return shadeOr(c, currentPalette(c).border); }
    public static int accentStrong(Context c) { return shadeOr(c, currentPalette(c).accentStrong); }
    public static int textMain(Context c)     { return currentPalette(c).text; }
    public static int textSub(Context c)      { return currentPalette(c).textSub; }
    public static int textMuted(Context c)    { return currentPalette(c).muted; }

    public static int onAccent(Context c) {
        BulkPalettes.Palette p = currentPalette(c);
        int ov = BulkPanelSettings.accentOverride(c);
        if (ov != BulkPanelSettings.ACCENT_NONE) return BulkPanelSettings.onAccentFor(ov);
        return p.onAccent;
    }

    public static int glassBg(Context c)      { return currentPalette(c).glassBg; }
    public static int iconBoxColor(Context c) { return shadeOr(c, currentPalette(c).iconBox); }
    public static int cardStart(Context c)    { return currentPalette(c).surface; }
    public static int cardEnd(Context c)      { return currentPalette(c).surfaceMuted; }
    public static int drawerBg(Context c)     { return currentPalette(c).drawerBg; }
    public static int modalColor(Context c)   { return currentPalette(c).modal; }
    public static int danger(Context c)       { return currentPalette(c).danger; }
    public static int dangerSoft(Context c)   { return currentPalette(c).dangerSoft; }
    public static int dangerBorder(Context c) { return currentPalette(c).dangerBorder; }

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

    /**
     * Theme wala gol button — dark me sun, light me moon (design jesa).
     * v133: click ab toggle NAHI karta — Themes popup khulta hai
     * (Default / Dark / 5 naye themes); select foran lagu hota hai.
     */
    public static LinearLayout themeButton(final Activity a) {
        int res = isLight(a) ? R.drawable.ic_bulk_moon : R.drawable.ic_bulk_sun;
        return circleButton(a, res, 34, v -> showThemesPopup(a));
    }

    /**
     * v133 — Themes popup (design style): har theme ek row —
     * palette ke mini swatches (bg / surface / accent) + naam +
     * mojooda theme par ✓. Tap = persist + foran apply (activity
     * recreate, purane toggle wala hi rasta).
     */
    public static void showThemesPopup(final Activity a) {
        final Dialog dialog = new Dialog(a);
        LinearLayout panel = modalPanel(a, BulkStrings.t(a, BulkStrings.THEMES), dialog);
        String currentId = currentPalette(a).id;
        for (final BulkPalettes.Palette p : BulkPalettes.all()) {
            panel.addView(themeRow(a, p, p.id.equals(currentId), v -> {
                dialog.dismiss();
                if (!p.id.equals(currentPalette(a).id)) {
                    setTheme(a, p.id);
                    a.recreate();
                }
            }));
        }
        showPanelDialog(a, panel);
    }

    private static View themeRow(Context c, BulkPalettes.Palette p, boolean selected,
                                 View.OnClickListener click) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(selected ? glassPanelStrong(c) : glassPanel(c));
        row.setPadding(dp(c, 12), dp(c, 11), dp(c, 12), dp(c, 11));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(c, 8);
        row.setLayoutParams(rlp);

        // Mini swatches: bg / surface / accent ke chhote gol rang
        int[] swatches = {p.bg, p.surface, p.accent};
        for (int color : swatches) {
            View dot = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            g.setStroke(dp(c, 1), accentBorder(c));
            dot.setBackground(g);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(c, 16), dp(c, 16));
            dlp.rightMargin = dp(c, 5);
            dot.setLayoutParams(dlp);
            row.addView(dot);
        }

        TextView name = new TextView(c);
        name.setText(p.name);
        name.setTextSize(13.5f);
        name.setTextColor(textMain(c));
        if (selected) name.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nlp.leftMargin = dp(c, 6);
        name.setLayoutParams(nlp);
        row.addView(name);

        if (selected) {
            TextView check = new TextView(c);
            check.setText("✓");
            check.setTextSize(14);
            check.setTypeface(Typeface.DEFAULT_BOLD);
            check.setTextColor(accent(c));
            row.addView(check);
        }
        row.setClickable(true);
        row.setOnClickListener(click);
        return row;
    }

    /** Sub-screens ka mushtarika header: back gol button + title/subtitle + theme button, neeche border line. */
    public static LinearLayout subHeader(final Activity a, String title, String subtitle) {
        return subHeader(a, title, subtitle, null);
    }

    /** subHeader ka variant: dayein taraf theme button ki jagah apna
     *  button (null ho to theme button hi lagta hai). */
    public static LinearLayout subHeader(final Activity a, String title, String subtitle,
                                         View rightButton) {
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

        bar.addView(rightButton != null ? rightButton : themeButton(a));
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

    /**
     * v133 — Edge Blend (owner ka approved design): window ab
     * edge-to-edge hai — status/navigation bars transparent, app ka
     * background un ke peeche tak jata hai. Bar icons ka tint palette
     * ke hisab se (light theme = gehre icons, dark = roshan).
     * Asal content bilkul waisa hi rehta hai: installEdgeBlend asal
     * root ko utni hi padding deta hai jitni system pehle deta tha,
     * aur SIRF bar zones me mist gradient lagata hai (BulkEdgeBlend).
     */
    public static void applyWindow(Activity a) {
        try {
            Window w = a.getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(Color.TRANSPARENT);
            w.setNavigationBarColor(Color.TRANSPARENT);
            View decor = w.getDecorView();
            int flags = decor.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (isLight(a)) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags |= SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags &= ~SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
            installEdgeBlend(a);
        } catch (Exception ignored) {}
    }

    /** SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR (API 26+) — purane Android
     *  par ye bit khamoshi se ignore ho jata hai. */
    private static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 0x10;

    private static final String EDGE_ROOT_TAG = "bulk_edge_root";

    /**
     * Content ke asal root ko ek full-window FrameLayout (edge root)
     * me lapet ta hai: edge root ka background = appBg (poori window,
     * bar zones samet), asal root ko insets jitni padding milti hai
     * (content ki jagah/size bilkul pehle jesi), aur uper/neeche
     * mist overlays lagte hain. Activities jo setContentView badalti
     * hain (Stage/Gallery/AddProject) un ke naye root ko bhi yehi
     * listener khud lapet deta hai; toast jese baad ke adds par
     * edge root pehle se mojood hota hai, is liye kuch nahi hota.
     */
    private static void installEdgeBlend(final Activity a) {
        try {
            View decor = a.getWindow().getDecorView();
            decor.post(() -> {
                try {
                    final ViewGroup content = a.findViewById(android.R.id.content);
                    if (content == null) return;
                    content.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                        @Override
                        public void onChildViewAdded(View parent, View child) {
                            wrapEdgeChild(a, content);
                        }

                        @Override
                        public void onChildViewRemoved(View parent, View child) {}
                    });
                    wrapEdgeChild(a, content);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    private static void wrapEdgeChild(final Activity a, final ViewGroup content) {
        try {
            for (int i = 0; i < content.getChildCount(); i++) {
                if (EDGE_ROOT_TAG.equals(content.getChildAt(i).getTag())) return;
            }
            if (content.getChildCount() == 0) return;
            final View root = content.getChildAt(0);
            if (root == null) return;

            final FrameLayout edge = new FrameLayout(a);
            edge.setTag(EDGE_ROOT_TAG);
            edge.setBackgroundColor(appBg(a));
            content.removeView(root);
            edge.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            final View topMist = new View(a);
            topMist.setBackground(mistDrawable(a, true));
            topMist.setClickable(false);
            topMist.setFocusable(false);
            FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                BulkEdgeBlend.topZonePx(systemDimenPx(a, "status_bar_height"),
                    dp(a, BulkEdgeBlend.TOP_EXTENSION_DP)), Gravity.TOP);
            edge.addView(topMist, topLp);

            final View bottomMist = new View(a);
            bottomMist.setBackground(mistDrawable(a, false));
            bottomMist.setClickable(false);
            bottomMist.setFocusable(false);
            FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                BulkEdgeBlend.bottomZonePx(systemDimenPx(a, "navigation_bar_height"),
                    dp(a, BulkEdgeBlend.BOTTOM_EXTENSION_DP)), Gravity.BOTTOM);
            edge.addView(bottomMist, bottomLp);

            content.addView(edge, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            final int baseL = root.getPaddingLeft();
            final int baseT = root.getPaddingTop();
            final int baseR = root.getPaddingRight();
            final int baseB = root.getPaddingBottom();
            edge.setOnApplyWindowInsetsListener((v, insets) -> {
                try {
                    root.setPadding(baseL + insets.getSystemWindowInsetLeft(),
                        baseT + insets.getSystemWindowInsetTop(),
                        baseR + insets.getSystemWindowInsetRight(),
                        baseB + insets.getSystemWindowInsetBottom());
                    FrameLayout.LayoutParams tlp = (FrameLayout.LayoutParams) topMist.getLayoutParams();
                    tlp.height = BulkEdgeBlend.topZonePx(insets.getSystemWindowInsetTop(),
                        dp(a, BulkEdgeBlend.TOP_EXTENSION_DP));
                    topMist.setLayoutParams(tlp);
                    FrameLayout.LayoutParams blp = (FrameLayout.LayoutParams) bottomMist.getLayoutParams();
                    blp.height = BulkEdgeBlend.bottomZonePx(insets.getSystemWindowInsetBottom(),
                        dp(a, BulkEdgeBlend.BOTTOM_EXTENSION_DP));
                    bottomMist.setLayoutParams(blp);
                } catch (Exception ignored) {}
                return insets;
            });
            edge.requestApplyInsets();
        } catch (Exception ignored) {}
    }

    /** Mist gradient: edge par gehra, andar ki taraf ghulta hua —
     *  koi hard line nahi (stops BulkEdgeBlend se, orientation se
     *  edge ki taraf tay hoti hai). */
    private static GradientDrawable mistDrawable(Activity a, boolean top) {
        GradientDrawable g = new GradientDrawable(
            top ? GradientDrawable.Orientation.TOP_BOTTOM
                : GradientDrawable.Orientation.BOTTOM_TOP,
            BulkEdgeBlend.edgeStops(currentPalette(a)));
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return g;
    }

    private static int systemDimenPx(Activity a, String name) {
        try {
            int id = a.getResources().getIdentifier(name, "dimen", "android");
            return id > 0 ? a.getResources().getDimensionPixelSize(id) : 0;
        } catch (Exception e) {
            return 0;
        }
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
        // v135: About ka title Control Panel wale App Name se
        title.setText(BulkStrings.t(a, BulkStrings.ABOUT_TITLE)
            .replace("Termux Mod", BulkPanelSettings.appName(a)));
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
        // v135: number Control Panel ki Contact setting se
        devContact.setText("Contact: " + BulkPanelSettings.contactNumber(a));
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
