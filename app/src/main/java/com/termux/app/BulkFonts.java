package com.termux.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.termux.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Termux Mod — Font option (v127, owner ki addition).
 *
 * Sidebar ke upar wale Font icon se popup khulta hai: 10 bold/stylish
 * lekin saaf parhe jane wale fonts (Google Fonts, OFL license) +
 * "Default" (system font). Choice "bulk_mockup" prefs (key "ui_font")
 * me persist hoti hai or tamam Bulk screens ke text par lagti hai —
 * har screen apni view tree par {@link #apply(Activity)} bulati hai.
 *
 * Catalog wala hissa pure Java hai (unit tests ke liye); font files
 * assets/bulk/fonts/&lt;file&gt; par hain.
 */
public final class BulkFonts {

    private static final String PREFS = "bulk_mockup";
    private static final String KEY_FONT = "ui_font";
    public static final String DEFAULT_ID = "default";
    /** v128: factory default font — sirf tab jab saved choice na ho. */
    public static final String FACTORY_ID = "poppins";
    public static final String ASSET_DIR = "bulk/fonts/";

    /** Ek font ki maloomat: stable id, dikhai dene wala naam, asset file. */
    public static final class FontInfo {
        public final String id;
        public final String name;
        public final String assetFile; // null = system default

        FontInfo(String id, String name, String assetFile) {
            this.id = id;
            this.name = name;
            this.assetFile = assetFile;
        }

        /** APK ke andar ka poora asset path; default ke liye null. */
        public String assetPath() {
            return assetFile == null ? null : ASSET_DIR + assetFile;
        }
    }

    private static final List<FontInfo> FONTS = new ArrayList<>();

    static {
        FONTS.add(new FontInfo(DEFAULT_ID, "Default", null));
        FONTS.add(new FontInfo("poppins", "Poppins", "poppins.ttf"));
        FONTS.add(new FontInfo("montserrat", "Montserrat", "montserrat.ttf"));
        FONTS.add(new FontInfo("oswald", "Oswald", "oswald.ttf"));
        FONTS.add(new FontInfo("raleway", "Raleway", "raleway.ttf"));
        FONTS.add(new FontInfo("rubik", "Rubik", "rubik.ttf"));
        FONTS.add(new FontInfo("quicksand", "Quicksand", "quicksand.ttf"));
        FONTS.add(new FontInfo("comfortaa", "Comfortaa", "comfortaa.ttf"));
        FONTS.add(new FontInfo("bebasneue", "Bebas Neue", "bebasneue.ttf"));
        FONTS.add(new FontInfo("orbitron", "Orbitron", "orbitron.ttf"));
        FONTS.add(new FontInfo("playfairdisplay", "Playfair Display", "playfairdisplay.ttf"));
    }

    private BulkFonts() {}

    // ------------------------------------------------------------ pure catalog

    /** Tamam fonts (Default pehle) — popup ki list isi order me banti hai. */
    public static List<FontInfo> fonts() {
        return Collections.unmodifiableList(FONTS);
    }

    /** Sirf asal (bundled) fonts — Default ke baghair. */
    public static int bundledCount() {
        int n = 0;
        for (FontInfo f : FONTS) if (f.assetFile != null) n++;
        return n;
    }

    public static FontInfo byId(String id) {
        if (id != null) {
            for (FontInfo f : FONTS) if (f.id.equals(id)) return f;
        }
        return FONTS.get(0);
    }

    public static boolean isValidId(String id) {
        if (id == null) return false;
        for (FontInfo f : FONTS) if (f.id.equals(id)) return true;
        return false;
    }

    // ------------------------------------------------------------ prefs glue

    public static String currentId(Context c) {
        try {
            // Saved choice na ho to factory default (Poppins); saved
            // choice — chahe "Default" hi kyun na ho — hamesha respected.
            String id = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_FONT, FACTORY_ID);
            return isValidId(id) ? id : DEFAULT_ID;
        } catch (Exception e) {
            return FACTORY_ID;
        }
    }

    public static void setFont(Context c, String id) {
        try {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_FONT, isValidId(id) ? id : DEFAULT_ID).apply();
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------ typeface

    private static final Map<String, Typeface> CACHE = new HashMap<>();

    /** Mojooda font ka Typeface; Default (ya load fail) par null. */
    public static Typeface typeface(Context c) {
        FontInfo f = byId(currentId(c));
        if (f.assetFile == null) return null;
        synchronized (CACHE) {
            Typeface tf = CACHE.get(f.id);
            if (tf == null) {
                try {
                    tf = Typeface.createFromAsset(c.getAssets(), f.assetPath());
                } catch (Exception e) {
                    tf = null;
                }
                CACHE.put(f.id, tf);
            }
            return tf;
        }
    }

    // ------------------------------------------------------------ apply

    /** v135: Control Panel ka font-size multiplier (1.0 = Normal). */
    public static float scaleFactor(Context c) {
        try {
            return BulkPanelSettings.fontScale(c);
        } catch (Exception e) {
            return 1f;
        }
    }

    /**
     * Activity ki poori view tree ke har TextView par mojooda font lagao
     * (bold style mehfooz rehta hai).
     * v135: font-size scale HAR surat lagta hai — Default (system)
     * font par bhi; typeface ho to font + size dono sath lagte hain.
     */
    public static void apply(Activity a) {
        if (a == null) return;
        Typeface tf = typeface(a);
        float scale = scaleFactor(a);
        if (tf == null && scale == 1f) return;
        try {
            View root = a.findViewById(android.R.id.content);
            if (root != null) applyToTree(root, tf, scale);
        } catch (Exception ignored) {}
    }

    /** Kisi bhi view tree (dialogs samet) par font lagao. */
    public static void applyToTree(View root, Typeface tf) {
        applyToTree(root, tf, 1f);
    }

    /**
     * Font + optional size scale. Scale ek TextView par SIRF ek dafa
     * lagta hai (tag guard) — dobara apply double-scale nahi karta.
     * tf null ho to sirf size scale lagta hai.
     */
    public static void applyToTree(View root, Typeface tf, float scale) {
        if (root == null) return;
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            try {
                if (tf != null) {
                    Typeface cur = tv.getTypeface();
                    int style = cur != null ? cur.getStyle() : Typeface.NORMAL;
                    tv.setTypeface(tf, style);
                }
                if (scale != 1f && tv.getTag(R.id.bulk_font_scaled) == null) {
                    tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                        tv.getTextSize() * scale);
                    tv.setTag(R.id.bulk_font_scaled, Boolean.TRUE);
                }
            } catch (Exception ignored) {}
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyToTree(vg.getChildAt(i), tf, scale);
            }
        }
    }

    /** Dialog waghera ke liye: activity ke current font se tree par lagao. */
    public static void applyToTree(Context c, View root) {
        Typeface tf = typeface(c);
        float scale = scaleFactor(c);
        if (tf == null && scale == 1f) return;
        applyToTree(root, tf, scale);
    }
}
