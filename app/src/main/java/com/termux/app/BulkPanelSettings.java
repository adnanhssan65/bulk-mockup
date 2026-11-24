package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Termux Mod (v135) — Control Panel ki tamam settings ka ek hi ghar.
 *
 * Panel (BulkControlPanelActivity) inhi keys ko parhta/likhta hai or
 * baqi screens (Dashboard, BulkTheme, BulkFonts) bhi yahin se values
 * leti hain — har setting ka EK hi source of truth. Kuch keys purani
 * classes ki hain (Run Settings, Recreate, Scroll, Duplicates TTL,
 * theme/font/language) — wo wahin rehti hain jahan thi; ye class un
 * ko registry me shamil karke Backup / Reset / Defaults ko mukammal
 * karti hai.
 *
 * Password: sirf salted SHA-256 hash persist hota hai
 * ("salt$hash" format), plaintext kabhi nahi. Default password
 * owner ne khud tay kiya hai (DEFAULT_PASSWORD); Change Password
 * ke baad stored hash usi se verify hota hai.
 *
 * Pure hissa (password hash, home-rows model, font-scale, accent,
 * backup JSON, defaults registry) bina Android ke hai — JVM tests
 * me hai. Context wala hissa sirf prefs/file IO hai.
 */
public final class BulkPanelSettings {

    public static final String PREFS = "bulk_mockup";
    public static final String GALLERY_PREFS = "bulk_gallery";
    public static final String DUP_PREFS = "bulk_duplicates";

    // ------------------------------------------------------------ keys

    public static final String KEY_PW_HASH = "panel_pw_hash";
    public static final String KEY_APP_LOCK = "panel_app_lock";
    public static final String KEY_APP_NAME = "panel_app_name";
    public static final String KEY_SLOGAN = "panel_slogan";
    public static final String KEY_HOME_ORDER = "panel_home_order";
    public static final String KEY_HOME_HIDDEN = "panel_home_hidden";
    public static final String KEY_FONT_SIZE = "panel_font_size";
    public static final String KEY_ACCENT = "panel_accent";
    public static final String KEY_CONTACT = "panel_contact";
    public static final String KEY_LOGO_ZOOM = "panel_logo_zoom";
    public static final String KEY_ICON = "panel_icon";

    /** Backup ke andar custom logo (base64 PNG) ki synthetic key. */
    public static final String LOGO_BACKUP_KEY = PREFS + "|panel_logo_png";

    // ------------------------------------------------------------ defaults

    /** Owner ka tay karda default panel password (sirf hash banta hai). */
    public static final String DEFAULT_PASSWORD = "Adnan001";
    private static final String DEFAULT_SALT = "bulkpanel135";

    public static final String DEFAULT_NAME = "Termux Pro";
    public static final String DEFAULT_SLOGAN = "Powerful Terminal Experience";
    public static final String DEFAULT_CONTACT = "+44 7404 032136";
    public static final boolean DEFAULT_APP_LOCK = false;

    /** Process-scope: App Lock ek process me ek dafa khulta hai. */
    public static boolean sAppUnlocked = false;

    private BulkPanelSettings() {}

    // ------------------------------------------------------------ password (pure)

    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Stored format: "salt$hash" — hash = sha256(salt + ":" + password). */
    public static String makeStored(String salt, String password) {
        if (salt == null) salt = "";
        if (password == null) password = "";
        return salt + "$" + sha256Hex(salt + ":" + password);
    }

    /** Default password ka stored roop (jab kuch save na ho). */
    public static String defaultStored() {
        return makeStored(DEFAULT_SALT, DEFAULT_PASSWORD);
    }

    /** Candidate ka stored hash se muqabla; stored khaali ho to default. */
    public static boolean verify(String stored, String candidate) {
        String eff = (stored == null || stored.isEmpty()) ? defaultStored() : stored;
        int cut = eff.indexOf('$');
        if (cut < 0) return false;
        String salt = eff.substring(0, cut);
        String want = eff.substring(cut + 1);
        String got = sha256Hex(salt + ":" + (candidate == null ? "" : candidate));
        if (want.length() != got.length()) return false;
        int diff = 0;
        for (int i = 0; i < want.length(); i++) diff |= want.charAt(i) ^ got.charAt(i);
        return diff == 0;
    }

    // ------------------------------------------------------------ app name (pure)

    /** Naam saaf karo: trim + darmiyan ki extra spaces ek. Khaali -> default. */
    public static String sanitizeName(String raw) {
        if (raw == null) return DEFAULT_NAME;
        String s = raw.trim().replaceAll("\\s+", " ");
        return s.isEmpty() ? DEFAULT_NAME : s;
    }

    /**
     * Two-tone title ka split: AKHRI lafz accent, baqi dark.
     * Return: accent hissa kahan se shuru hota hai (index), ya -1
     * jab naam ek hi lafz ka ho (poora dark render hota hai).
     */
    public static int accentStart(String name) {
        if (name == null) return -1;
        int sp = name.lastIndexOf(' ');
        if (sp < 0 || sp >= name.length() - 1) return -1;
        return sp + 1;
    }

    // ------------------------------------------------------------ home rows (pure)

    public static final String ROW_ADD = "add";
    public static final String ROW_RUN = "run";
    public static final String ROW_PROJECTS = "projects";
    public static final String ROW_PREVIEW = "preview";
    public static final String ROW_TERMINAL = "terminal";

    public static final List<String> DEFAULT_ORDER = Arrays.asList(
        ROW_ADD, ROW_RUN, ROW_PROJECTS, ROW_PREVIEW, ROW_TERMINAL);
    public static final String DEFAULT_ORDER_CSV = "add,run,projects,preview,terminal";

    /** CSV -> saaf tartib: sirf valid ids, dedup, ghayab default order se akhir me. */
    public static List<String> parseOrder(String csv) {
        List<String> out = new ArrayList<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String id = part.trim();
                if (DEFAULT_ORDER.contains(id) && !out.contains(id)) out.add(id);
            }
        }
        for (String id : DEFAULT_ORDER) if (!out.contains(id)) out.add(id);
        return out;
    }

    public static String serializeOrder(List<String> order) {
        StringBuilder sb = new StringBuilder();
        if (order != null) {
            for (String id : order) {
                if (!DEFAULT_ORDER.contains(id)) continue;
                if (sb.length() > 0) sb.append(',');
                sb.append(id);
            }
        }
        return sb.length() == 0 ? DEFAULT_ORDER_CSV : sb.toString();
    }

    public static Set<String> parseHidden(String csv) {
        Set<String> out = new LinkedHashSet<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String id = part.trim();
                if (DEFAULT_ORDER.contains(id)) out.add(id);
            }
        }
        return out;
    }

    public static String serializeHidden(Set<String> hidden) {
        StringBuilder sb = new StringBuilder();
        if (hidden != null) {
            for (String id : DEFAULT_ORDER) {
                if (hidden.contains(id)) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(id);
                }
            }
        }
        return sb.toString();
    }

    /** Ek row ko dir (-1 = upar, +1 = neeche) hila kar nayi list; kinare par clamp. */
    public static List<String> moved(List<String> order, int index, int dir) {
        List<String> out = new ArrayList<>(order);
        int j = index + dir;
        if (index < 0 || index >= out.size() || j < 0 || j >= out.size()) return out;
        String tmp = out.get(index);
        out.set(index, out.get(j));
        out.set(j, tmp);
        return out;
    }

    /** Jo rows dikhani hain — tartib ke sath, hidden hata kar. */
    public static List<String> visibleRows(List<String> order, Set<String> hidden) {
        List<String> out = new ArrayList<>();
        for (String id : order) {
            if (hidden == null || !hidden.contains(id)) out.add(id);
        }
        return out;
    }

    // ------------------------------------------------------------ font size (pure)

    public static final String SIZE_SMALL = "small";
    public static final String SIZE_NORMAL = "normal";
    public static final String SIZE_LARGE = "large";
    public static final String SIZE_XLARGE = "xlarge";
    public static final List<String> SIZE_IDS = Arrays.asList(
        SIZE_SMALL, SIZE_NORMAL, SIZE_LARGE, SIZE_XLARGE);

    public static float fontScaleFor(String id) {
        if (SIZE_SMALL.equals(id)) return 0.85f;
        if (SIZE_LARGE.equals(id)) return 1.15f;
        if (SIZE_XLARGE.equals(id)) return 1.30f;
        return 1.0f;
    }

    public static String sanitizeSizeId(String id) {
        return SIZE_IDS.contains(id) ? id : SIZE_NORMAL;
    }

    // ------------------------------------------------------------ accent (pure)

    /** 0 = koi override nahi (theme ka accent). */
    public static final int ACCENT_NONE = 0;

    /** Swatches: 7 theme accents + Pink. (Theme Default alag row hai = 0.) */
    public static final int[] ACCENT_SWATCHES = {
        0xFF2E9E00, // Default theme green
        0xFF76FF03, // Dark theme lime
        0xFF38BDF8, // Midnight sky
        0xFF7C3AED, // Royal Purple
        0xFF0D9488, // Ocean teal
        0xFFEA580C, // Sunset orange
        0xFFE11D63, // Rose pink
        0xFFE91E63, // Pink
    };

    public static int effectiveAccent(int paletteAccent, int override) {
        return override != ACCENT_NONE ? override : paletteAccent;
    }

    /** Accent ke upar parha jane wala rang: roshan accent -> gehra text, warna safed. */
    public static int onAccentFor(int argb) {
        double lum = 0.299 * ((argb >> 16) & 0xFF)
            + 0.587 * ((argb >> 8) & 0xFF)
            + 0.114 * (argb & 0xFF);
        return lum > 150 ? 0xFF0A1408 : 0xFFFFFFFF;
    }

    // ------------------------------------------------------------ launcher icon (pure)

    public static final String ICON_TILE = "tile";   // Termux Mod — full tile
    public static final String ICON_GLYPH = "glyph"; // Bulk Mockup — glyph on navy
    public static final String ICON_LIGHT = "light"; // Mockup Studio — light
    public static final String ICON_PRO = "pro";     // Termux Pro — glyph+glow (DEFAULT)
    public static final List<String> ICON_IDS = Arrays.asList(
        ICON_TILE, ICON_GLYPH, ICON_LIGHT, ICON_PRO);
    public static final String DEFAULT_ICON = ICON_PRO;

    /** Har icon id ki activity-alias class (manifest se match lazmi). */
    public static final String[] ICON_ALIASES = {
        "com.termux.app.BulkIconTermuxMod",
        "com.termux.app.BulkIconBulkMockup",
        "com.termux.app.BulkIconMockupStudio",
        "com.termux.app.BulkIconTermuxPro",
    };

    public static String sanitizeIconId(String id) {
        return ICON_IDS.contains(id) ? id : DEFAULT_ICON;
    }

    public static int iconIndex(String id) {
        int i = ICON_IDS.indexOf(sanitizeIconId(id));
        return i < 0 ? ICON_IDS.indexOf(DEFAULT_ICON) : i;
    }

    // ------------------------------------------------------------ contact (pure)

    /** wa.me ke liye sirf digits (pehle wala '+' waghera hata kar). */
    public static String digitsOf(String number) {
        if (number == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char ch = number.charAt(i);
            if (ch >= '0' && ch <= '9') sb.append(ch);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ logo crop (pure)

    /**
     * Cover-crop ka markazi square: size = chhoti side / zoom
     * (zoom 1 = poori chhoti side, zyada zoom = aur qareeb).
     * Return [x, y, size].
     */
    public static int[] coverCropRect(int srcW, int srcH, float zoom) {
        if (srcW <= 0 || srcH <= 0) return new int[]{0, 0, 0};
        float z = zoom < 1f ? 1f : zoom;
        int base = Math.min(srcW, srcH);
        int size = Math.round(base / z);
        if (size < 16) size = Math.min(16, base);
        if (size > base) size = base;
        return new int[]{(srcW - size) / 2, (srcH - size) / 2, size};
    }

    // ------------------------------------------------------------ registry + defaults

    /** Ek registry entry: kis prefs file ki key, type (s/b/i/f), factory default (string roop). */
    public static final class Entry {
        public final String prefs;
        public final String key;
        public final char type;
        public final String def;

        Entry(String prefs, String key, char type, String def) {
            this.prefs = prefs;
            this.key = key;
            this.type = type;
            this.def = def;
        }

        public String composite() {
            return prefs + "|" + key;
        }
    }

    private static final List<Entry> REGISTRY = new ArrayList<>();

    static {
        // bulk_mockup — language/theme/font (purani keys, yahin rehti hain)
        REGISTRY.add(new Entry(PREFS, "ui_language", 's', "en"));
        REGISTRY.add(new Entry(PREFS, "ui_theme", 's', "default"));
        REGISTRY.add(new Entry(PREFS, "ui_font", 's', "poppins"));
        // Run Settings (BulkRunSettings ki keys)
        REGISTRY.add(new Entry(PREFS, "run_answer_mode", 's', "auto"));
        REGISTRY.add(new Entry(PREFS, "run_answer_hd", 's', "1"));
        REGISTRY.add(new Entry(PREFS, "run_answer_size", 's', "2"));
        // Recreate (BulkRecreateSettings ki keys)
        REGISTRY.add(new Entry(PREFS, "recreate_folders_auto", 'b', "false"));
        REGISTRY.add(new Entry(PREFS, "recreate_videos_auto", 'b', "false"));
        REGISTRY.add(new Entry(PREFS, "recreate_videos_enabled", 'b', "true"));
        // Panel ki apni keys
        REGISTRY.add(new Entry(PREFS, KEY_PW_HASH, 's', ""));
        REGISTRY.add(new Entry(PREFS, KEY_APP_LOCK, 'b', "false"));
        REGISTRY.add(new Entry(PREFS, KEY_APP_NAME, 's', DEFAULT_NAME));
        REGISTRY.add(new Entry(PREFS, KEY_SLOGAN, 's', DEFAULT_SLOGAN));
        REGISTRY.add(new Entry(PREFS, KEY_HOME_ORDER, 's', DEFAULT_ORDER_CSV));
        REGISTRY.add(new Entry(PREFS, KEY_HOME_HIDDEN, 's', ""));
        REGISTRY.add(new Entry(PREFS, KEY_FONT_SIZE, 's', SIZE_NORMAL));
        REGISTRY.add(new Entry(PREFS, KEY_ACCENT, 'i', "0"));
        REGISTRY.add(new Entry(PREFS, KEY_CONTACT, 's', DEFAULT_CONTACT));
        REGISTRY.add(new Entry(PREFS, KEY_LOGO_ZOOM, 'f', "1.0"));
        REGISTRY.add(new Entry(PREFS, KEY_ICON, 's', DEFAULT_ICON));
        // bulk_gallery — scroll button (BulkScrollSettings ki key)
        REGISTRY.add(new Entry(GALLERY_PREFS, "scroll_button_show", 'b', "true"));
        // bulk_duplicates — TTL (BulkDuplicatesStore ki key)
        REGISTRY.add(new Entry(DUP_PREFS, "ttl_hours", 'i', "24"));
    }

    public static List<Entry> registry() {
        return new ArrayList<>(REGISTRY);
    }

    public static Entry entryFor(String composite) {
        for (Entry e : REGISTRY) if (e.composite().equals(composite)) return e;
        return null;
    }

    /** Typed value ka string roop: "s:..." / "b:..." / "i:..." / "f:...". */
    public static String encodeTyped(char type, String raw) {
        return type + ":" + (raw == null ? "" : raw);
    }

    /** Factory defaults ka poora map (composite key -> typed value) — logo khaali (delete). */
    public static Map<String, String> factoryDefaults() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Entry e : REGISTRY) out.put(e.composite(), encodeTyped(e.type, e.def));
        out.put(LOGO_BACKUP_KEY, encodeTyped('s', ""));
        return out;
    }

    // ------------------------------------------------------------ backup JSON (pure)

    public static final String BACKUP_KIND = "termuxmod-settings";

    /** Flat JSON object: kind + version + tamam typed values. */
    public static String buildBackup(Map<String, String> typedValues) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendPair(sb, "kind", BACKUP_KIND);
        sb.append(',');
        appendPair(sb, "version", "1");
        if (typedValues != null) {
            List<String> keys = new ArrayList<>(typedValues.keySet());
            java.util.Collections.sort(keys);
            for (String k : keys) {
                sb.append(',');
                appendPair(sb, k, typedValues.get(k));
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static void appendPair(StringBuilder sb, String key, String value) {
        sb.append('"').append(escapeJson(key)).append("\":\"")
            .append(escapeJson(value == null ? "" : value)).append('"');
    }

    public static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Backup JSON -> typed values map (kind/version hata kar).
     * Ghalat/kind-mismatch par null.
     */
    public static Map<String, String> parseBackup(String json) {
        Map<String, String> flat = parseFlatJsonObject(json);
        if (flat == null) return null;
        if (!BACKUP_KIND.equals(flat.get("kind"))) return null;
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : flat.entrySet()) {
            if ("kind".equals(e.getKey()) || "version".equals(e.getKey())) continue;
            String v = e.getValue();
            if (v == null || v.length() < 2 || v.charAt(1) != ':') return null;
            char t = v.charAt(0);
            if (t != 's' && t != 'b' && t != 'i' && t != 'f') return null;
            out.put(e.getKey(), v);
        }
        return out.isEmpty() ? null : out;
    }

    /** Sirf string-values wala flat JSON object parser; ghalti par null. */
    public static Map<String, String> parseFlatJsonObject(String json) {
        if (json == null) return null;
        int[] pos = {0};
        Map<String, String> out = new LinkedHashMap<>();
        skipWs(json, pos);
        if (!eat(json, pos, '{')) return null;
        skipWs(json, pos);
        if (eat(json, pos, '}')) return out;
        while (true) {
            skipWs(json, pos);
            String key = parseJsonString(json, pos);
            if (key == null) return null;
            skipWs(json, pos);
            if (!eat(json, pos, ':')) return null;
            skipWs(json, pos);
            String value = parseJsonString(json, pos);
            if (value == null) return null;
            out.put(key, value);
            skipWs(json, pos);
            if (eat(json, pos, ',')) continue;
            if (eat(json, pos, '}')) break;
            return null;
        }
        return out;
    }

    private static void skipWs(String s, int[] pos) {
        while (pos[0] < s.length()) {
            char ch = s.charAt(pos[0]);
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') pos[0]++;
            else break;
        }
    }

    private static boolean eat(String s, int[] pos, char ch) {
        if (pos[0] < s.length() && s.charAt(pos[0]) == ch) {
            pos[0]++;
            return true;
        }
        return false;
    }

    private static String parseJsonString(String s, int[] pos) {
        if (pos[0] >= s.length() || s.charAt(pos[0]) != '"') return null;
        pos[0]++;
        StringBuilder sb = new StringBuilder();
        while (pos[0] < s.length()) {
            char ch = s.charAt(pos[0]++);
            if (ch == '"') return sb.toString();
            if (ch == '\\') {
                if (pos[0] >= s.length()) return null;
                char esc = s.charAt(pos[0]++);
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (pos[0] + 4 > s.length()) return null;
                        try {
                            sb.append((char) Integer.parseInt(s.substring(pos[0], pos[0] + 4), 16));
                        } catch (Exception e) {
                            return null;
                        }
                        pos[0] += 4;
                        break;
                    default: return null;
                }
            } else {
                sb.append(ch);
            }
        }
        return null;
    }

    // ------------------------------------------------------------ prefs glue

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences prefsFor(Context c, String name) {
        return c.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public static boolean verifyPassword(Context c, String candidate) {
        try {
            return verify(prefs(c).getString(KEY_PW_HASH, ""), candidate);
        } catch (Exception e) {
            return verify("", candidate);
        }
    }

    public static void setPassword(Context c, String salt, String newPassword) {
        try {
            prefs(c).edit().putString(KEY_PW_HASH, makeStored(salt, newPassword)).apply();
        } catch (Exception ignored) {}
    }

    public static boolean appLockEnabled(Context c) {
        try {
            return prefs(c).getBoolean(KEY_APP_LOCK, DEFAULT_APP_LOCK);
        } catch (Exception e) {
            return DEFAULT_APP_LOCK;
        }
    }

    public static void setAppLock(Context c, boolean on) {
        try {
            prefs(c).edit().putBoolean(KEY_APP_LOCK, on).apply();
        } catch (Exception ignored) {}
    }

    public static String appName(Context c) {
        try {
            return sanitizeName(prefs(c).getString(KEY_APP_NAME, DEFAULT_NAME));
        } catch (Exception e) {
            return DEFAULT_NAME;
        }
    }

    public static void setAppName(Context c, String name) {
        try {
            prefs(c).edit().putString(KEY_APP_NAME, sanitizeName(name)).apply();
        } catch (Exception ignored) {}
    }

    public static String slogan(Context c) {
        try {
            String s = prefs(c).getString(KEY_SLOGAN, DEFAULT_SLOGAN);
            return (s == null || s.trim().isEmpty()) ? DEFAULT_SLOGAN : s;
        } catch (Exception e) {
            return DEFAULT_SLOGAN;
        }
    }

    public static void setSlogan(Context c, String slogan) {
        try {
            String s = slogan == null ? "" : slogan.trim();
            prefs(c).edit().putString(KEY_SLOGAN, s.isEmpty() ? DEFAULT_SLOGAN : s).apply();
        } catch (Exception ignored) {}
    }

    public static List<String> homeOrder(Context c) {
        try {
            return parseOrder(prefs(c).getString(KEY_HOME_ORDER, DEFAULT_ORDER_CSV));
        } catch (Exception e) {
            return new ArrayList<>(DEFAULT_ORDER);
        }
    }

    public static void setHomeOrder(Context c, List<String> order) {
        try {
            prefs(c).edit().putString(KEY_HOME_ORDER, serializeOrder(order)).apply();
        } catch (Exception ignored) {}
    }

    public static Set<String> homeHidden(Context c) {
        try {
            return parseHidden(prefs(c).getString(KEY_HOME_HIDDEN, ""));
        } catch (Exception e) {
            return new LinkedHashSet<>();
        }
    }

    public static void setHomeHidden(Context c, Set<String> hidden) {
        try {
            prefs(c).edit().putString(KEY_HOME_HIDDEN, serializeHidden(hidden)).apply();
        } catch (Exception ignored) {}
    }

    public static String fontSizeId(Context c) {
        try {
            return sanitizeSizeId(prefs(c).getString(KEY_FONT_SIZE, SIZE_NORMAL));
        } catch (Exception e) {
            return SIZE_NORMAL;
        }
    }

    public static void setFontSizeId(Context c, String id) {
        try {
            prefs(c).edit().putString(KEY_FONT_SIZE, sanitizeSizeId(id)).apply();
        } catch (Exception ignored) {}
    }

    public static float fontScale(Context c) {
        return fontScaleFor(fontSizeId(c));
    }

    public static int accentOverride(Context c) {
        try {
            return prefs(c).getInt(KEY_ACCENT, ACCENT_NONE);
        } catch (Exception e) {
            return ACCENT_NONE;
        }
    }

    public static void setAccentOverride(Context c, int argb) {
        try {
            prefs(c).edit().putInt(KEY_ACCENT, argb).apply();
        } catch (Exception ignored) {}
    }

    public static String contactNumber(Context c) {
        try {
            String s = prefs(c).getString(KEY_CONTACT, DEFAULT_CONTACT);
            return (s == null || s.trim().isEmpty()) ? DEFAULT_CONTACT : s;
        } catch (Exception e) {
            return DEFAULT_CONTACT;
        }
    }

    public static void setContactNumber(Context c, String number) {
        try {
            String s = number == null ? "" : number.trim();
            prefs(c).edit().putString(KEY_CONTACT, s.isEmpty() ? DEFAULT_CONTACT : s).apply();
        } catch (Exception ignored) {}
    }

    public static String iconChoice(Context c) {
        try {
            return sanitizeIconId(prefs(c).getString(KEY_ICON, DEFAULT_ICON));
        } catch (Exception e) {
            return DEFAULT_ICON;
        }
    }

    public static void setIconChoice(Context c, String id) {
        try {
            prefs(c).edit().putString(KEY_ICON, sanitizeIconId(id)).apply();
        } catch (Exception ignored) {}
    }

    public static float logoZoom(Context c) {
        try {
            float z = prefs(c).getFloat(KEY_LOGO_ZOOM, 1f);
            return z < 1f ? 1f : (z > 3f ? 3f : z);
        } catch (Exception e) {
            return 1f;
        }
    }

    public static void setLogoZoom(Context c, float zoom) {
        try {
            prefs(c).edit().putFloat(KEY_LOGO_ZOOM, zoom < 1f ? 1f : (zoom > 3f ? 3f : zoom)).apply();
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------ custom logo file

    public static File logoFile(Context c) {
        return new File(c.getFilesDir(), "bulk_custom_logo.png");
    }

    public static boolean hasCustomLogo(Context c) {
        try {
            File f = logoFile(c);
            return f.isFile() && f.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Custom logo ka bitmap; na ho / parha na jaye to null (default artwork). */
    public static Bitmap loadLogo(Context c) {
        try {
            if (!hasCustomLogo(c)) return null;
            return BitmapFactory.decodeFile(logoFile(c).getAbsolutePath());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean saveLogo(Context c, Bitmap square) {
        try {
            File f = logoFile(c);
            try (FileOutputStream out = new FileOutputStream(f)) {
                return square.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static void clearLogo(Context c) {
        try {
            File f = logoFile(c);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------ backup / reset glue

    /** Tamam registry values mojooda prefs se parh kar typed map banao (+ logo base64). */
    public static Map<String, String> exportValues(Context c) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Entry e : REGISTRY) {
            out.put(e.composite(), encodeTyped(e.type, readRaw(c, e)));
        }
        String b64 = "";
        try {
            File f = logoFile(c);
            if (f.isFile() && f.length() > 0) {
                byte[] bytes = new byte[(int) f.length()];
                try (InputStream in = new FileInputStream(f)) {
                    int off = 0;
                    int r;
                    while (off < bytes.length && (r = in.read(bytes, off, bytes.length - off)) > 0) {
                        off += r;
                    }
                }
                b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
            }
        } catch (Exception ignored) {}
        out.put(LOGO_BACKUP_KEY, encodeTyped('s', b64));
        return out;
    }

    private static String readRaw(Context c, Entry e) {
        try {
            SharedPreferences sp = prefsFor(c, e.prefs);
            switch (e.type) {
                case 'b':
                    return String.valueOf(sp.getBoolean(e.key, Boolean.parseBoolean(e.def)));
                case 'i':
                    return String.valueOf(sp.getInt(e.key, Integer.parseInt(e.def)));
                case 'f':
                    return String.valueOf(sp.getFloat(e.key, Float.parseFloat(e.def)));
                default:
                    String v = sp.getString(e.key, e.def);
                    return v == null ? e.def : v;
            }
        } catch (Exception ex) {
            return e.def;
        }
    }

    /** Typed values apply karo; pehchani hui entries hi lagti hain. Apply shuda count. */
    public static int applyValues(Context c, Map<String, String> typedValues) {
        int applied = 0;
        if (typedValues == null) return 0;
        for (Map.Entry<String, String> kv : typedValues.entrySet()) {
            String composite = kv.getKey();
            String typed = kv.getValue();
            if (typed == null || typed.length() < 2 || typed.charAt(1) != ':') continue;
            char type = typed.charAt(0);
            String raw = typed.substring(2);
            if (LOGO_BACKUP_KEY.equals(composite)) {
                applyLogoBackup(c, raw);
                applied++;
                continue;
            }
            Entry e = entryFor(composite);
            if (e == null || e.type != type) continue;
            try {
                SharedPreferences.Editor ed = prefsFor(c, e.prefs).edit();
                switch (type) {
                    case 'b': ed.putBoolean(e.key, Boolean.parseBoolean(raw)); break;
                    case 'i': ed.putInt(e.key, Integer.parseInt(raw)); break;
                    case 'f': ed.putFloat(e.key, Float.parseFloat(raw)); break;
                    default: ed.putString(e.key, raw);
                }
                ed.apply();
                applied++;
            } catch (Exception ignored) {}
        }
        return applied;
    }

    private static void applyLogoBackup(Context c, String base64) {
        try {
            if (base64 == null || base64.isEmpty()) {
                clearLogo(c);
                return;
            }
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) saveLogo(c, bmp);
        } catch (Exception ignored) {}
    }

    /** Factory defaults par wapas: tamam registry values + custom logo saaf. */
    public static void resetAll(Context c) {
        applyValues(c, factoryDefaults());
    }
}
