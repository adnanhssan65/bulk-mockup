package com.termux.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Termux Mod (v133) — Themes ki palette table (PURE Java, unit tests
 * ke liye). Har theme = poori palette: bg, surface (card), surface-muted,
 * text, muted, border, accent, on-accent + BulkTheme ke baqi shades
 * (accent soft/strong, glass, drawer, modal, danger family).
 *
 * Pehle do themes v132 wale hi rang hain, byte-for-byte:
 *   "default" = purana Light (factory default)
 *   "dark"    = purana Dark
 * Baqi paanch v133 ke naye professional themes hain (owner ki pasand):
 *   Midnight (dark), Royal Purple, Ocean, Sunset, Rose (light).
 *
 * Persisted value "bulk_mockup" prefs ki key "ui_theme" me theme ki
 * id hoti hai. Saved value na ho to purani "ui_light_mode" boolean se
 * map hota hai (false -> dark, warna default) — purani choice kabhi
 * zaya nahi hoti.
 */
public final class BulkPalettes {

    public static final String PREF_KEY = "ui_theme";

    public static final String ID_DEFAULT = "default";
    public static final String ID_DARK = "dark";
    public static final String ID_MIDNIGHT = "midnight";
    public static final String ID_ROYAL = "royal";
    public static final String ID_OCEAN = "ocean";
    public static final String ID_SUNSET = "sunset";
    public static final String ID_ROSE = "rose";

    private BulkPalettes() {}

    /** Ek theme ki poori palette. Tamam rang ARGB ints hain. */
    public static final class Palette {
        public final String id;
        public final String name;      // dono zubano me English hi rehta hai
        public final boolean light;
        public final int bg;           // appBg
        public final int surface;      // card start (solid card)
        public final int surfaceMuted; // card end
        public final int text;         // textMain
        public final int textSub;
        public final int muted;        // textMuted
        public final int border;       // accentBorder
        public final int accent;
        public final int onAccent;
        public final int accentSoft;
        public final int accentStrong;
        public final int glassBg;
        public final int iconBox;
        public final int drawerBg;
        public final int modal;
        public final int danger;
        public final int dangerSoft;
        public final int dangerBorder;

        Palette(String id, String name, boolean light,
                int bg, int surface, int surfaceMuted, int text, int textSub, int muted,
                int border, int accent, int onAccent, int accentSoft, int accentStrong,
                int glassBg, int iconBox, int drawerBg, int modal,
                int danger, int dangerSoft, int dangerBorder) {
            this.id = id;
            this.name = name;
            this.light = light;
            this.bg = bg;
            this.surface = surface;
            this.surfaceMuted = surfaceMuted;
            this.text = text;
            this.textSub = textSub;
            this.muted = muted;
            this.border = border;
            this.accent = accent;
            this.onAccent = onAccent;
            this.accentSoft = accentSoft;
            this.accentStrong = accentStrong;
            this.glassBg = glassBg;
            this.iconBox = iconBox;
            this.drawerBg = drawerBg;
            this.modal = modal;
            this.danger = danger;
            this.dangerSoft = dangerSoft;
            this.dangerBorder = dangerBorder;
        }
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    // Danger family — light themes ki aik, dark themes ki dusri (v132 se)
    private static final int[] DANGER_LIGHT = {0xFFD32F2F, 0x1FD32F2F, 0x59D32F2F};
    private static final int[] DANGER_DARK = {0xFFFF5252, 0x24FF5252, 0x59FF5252};

    private static Palette make(String id, String name, boolean light,
                                int bg, int surface, int surfaceMuted,
                                int text, int textSub, int muted,
                                int accent, int onAccent,
                                int glassBg, int drawerBg, int modal) {
        int softAlpha = light ? 0x1C : 0x1F;
        int[] danger = light ? DANGER_LIGHT : DANGER_DARK;
        return new Palette(id, name, light,
            bg, surface, surfaceMuted, text, textSub, muted,
            withAlpha(accent, 0x59), accent, onAccent,
            withAlpha(accent, softAlpha), withAlpha(accent, 0xB8),
            glassBg, withAlpha(accent, light ? 0x1C : 0x17), drawerBg, modal,
            danger[0], danger[1], danger[2]);
    }

    private static final List<Palette> THEMES = new ArrayList<>();

    static {
        // Default — v132 Light, byte-for-byte (accent #2E9E00 family)
        THEMES.add(new Palette(ID_DEFAULT, "Default", true,
            0xFFF3F8F3, 0xFFFFFFFF, 0xFFE7F3E8,
            0xFF0D1B0E, 0xFF2E4730, 0xFF556E57,
            0x592E9E00, 0xFF2E9E00, 0xFFFFFFFF,
            0x1C2E9E00, 0xB82E9E00,
            0xE0FFFFFF, 0x1C2E9E00, 0xFAF3F8F3, 0xFAFFFFFF,
            DANGER_LIGHT[0], DANGER_LIGHT[1], DANGER_LIGHT[2]));
        // Dark — v132 Dark, byte-for-byte (accent #76FF03 family)
        THEMES.add(new Palette(ID_DARK, "Dark", false,
            0xFF030603, 0xFF0D180E, 0xFF050905,
            0xFFFFFFFF, 0xFFA5B8A6, 0xFF6E8270,
            0x5C76FF03, 0xFF76FF03, 0xFF030803,
            0x1F76FF03, 0xB876FF03,
            0xD60B140C, 0x1776FF03, 0xF7070D08, 0xF50A120B,
            DANGER_DARK[0], DANGER_DARK[1], DANGER_DARK[2]));
        // Midnight — dark: bg #0F172A, card #1E293B, accent #38BDF8
        THEMES.add(make(ID_MIDNIGHT, "Midnight", false,
            0xFF0F172A, 0xFF1E293B, 0xFF0B1120,
            0xFFF8FAFC, 0xFFCBD5E1, 0xFF94A3B8,
            0xFF38BDF8, 0xFF082F49,
            0xD61E293B, 0xF70F172A, 0xF51E293B));
        // Royal Purple — light: bg #F6F2FF, card #FFFFFF, accent #7C3AED
        THEMES.add(make(ID_ROYAL, "Royal Purple", true,
            0xFFF6F2FF, 0xFFFFFFFF, 0xFFEDE9FE,
            0xFF221240, 0xFF4C3673, 0xFF7E6FA3,
            0xFF7C3AED, 0xFFFFFFFF,
            0xE0FFFFFF, 0xFAF6F2FF, 0xFAFFFFFF));
        // Ocean — light: bg #EFFAF9, card #FFFFFF, accent #0D9488
        THEMES.add(make(ID_OCEAN, "Ocean", true,
            0xFFEFFAF9, 0xFFFFFFFF, 0xFFD9F5EF,
            0xFF052E2B, 0xFF24544D, 0xFF5F7F79,
            0xFF0D9488, 0xFFFFFFFF,
            0xE0FFFFFF, 0xFAEFFAF9, 0xFAFFFFFF));
        // Sunset — light: bg #FFF7ED, card #FFFFFF, accent #EA580C
        THEMES.add(make(ID_SUNSET, "Sunset", true,
            0xFFFFF7ED, 0xFFFFFFFF, 0xFFFFEDD5,
            0xFF331A03, 0xFF66452A, 0xFF96765C,
            0xFFEA580C, 0xFFFFFFFF,
            0xE0FFFFFF, 0xFAFFF7ED, 0xFAFFFFFF));
        // Rose — light: bg #FFF5F7, card #FFFFFF, accent #E11D63
        THEMES.add(make(ID_ROSE, "Rose", true,
            0xFFFFF5F7, 0xFFFFFFFF, 0xFFFFE4EC,
            0xFF330613, 0xFF662A3C, 0xFF996A7A,
            0xFFE11D63, 0xFFFFFFFF,
            0xE0FFFFFF, 0xFAFFF5F7, 0xFAFFFFFF));
    }

    /** Tamam themes, popup wali tartib me (Default pehle). */
    public static List<Palette> all() {
        return new ArrayList<>(THEMES);
    }

    /** Factory default theme — Default (purana Light). */
    public static Palette defaultPalette() {
        return byId(ID_DEFAULT);
    }

    /** Id se theme; na mile to null. */
    public static Palette byId(String id) {
        if (id == null) return null;
        for (Palette p : THEMES) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    /**
     * Saved values se asal theme ka faisla (pure):
     *   savedThemeId valid ho to wahi; warna purani boolean choice
     *   (legacyLight == FALSE -> Dark); warna factory Default.
     */
    public static Palette resolve(String savedThemeId, Boolean legacyLight) {
        Palette p = byId(savedThemeId);
        if (p != null) return p;
        if (legacyLight != null && !legacyLight) return byId(ID_DARK);
        return defaultPalette();
    }
}
