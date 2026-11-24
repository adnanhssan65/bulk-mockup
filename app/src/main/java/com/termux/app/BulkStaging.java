package com.termux.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Termux Mod — Add Wallpapers ki STAGING (drop) list ki pure logic (v125).
 *
 * User images pick karke staging me rakhta hai — ye kisi project me nahi
 * jate jab tak project select karke "Add" na dabaye. Staging SharedPrefs
 * me ek string ki shakal me persist hoti hai (har line:
 * uri \t name \t size \t addedAt) taake screen band karke wapas ane par
 * bhi staged wallpapers mehfooz rahen.
 *
 * Ye class bilkul pure Java hai (Android dependency nahi) — unit tests
 * isi liye chal sakte hain.
 */
public final class BulkStaging {

    /** Staging persistence ki jagah (BulkStageActivity ke sath shared). */
    public static final String PREFS_NAME = "bulk_wallpapers_stage";
    public static final String KEY_ITEMS = "items";

    public static final int SORT_NAME_ASC = 0;
    public static final int SORT_NAME_DESC = 1;
    public static final int SORT_DATE_ASC = 2;
    public static final int SORT_DATE_DESC = 3;
    public static final int SORT_SIZE_ASC = 4;
    public static final int SORT_SIZE_DESC = 5;

    private BulkStaging() {}

    /** Ek staged wallpaper. uri = content URI ki string shakal. */
    public static final class Item {
        public final String uri;
        public final String name;
        public final long size;     // bytes; -1 = maloom nahi
        public final long addedAt;  // millis — "date added" sort ke liye
        public final String hash;   // SHA-256 hex; "" = abhi hisaab nahi hua (duplicates check ke liye)

        public Item(String uri, String name, long size, long addedAt) {
            this(uri, name, size, addedAt, "");
        }

        public Item(String uri, String name, long size, long addedAt, String hash) {
            this.uri = uri;
            this.name = name == null ? "" : name;
            this.size = size;
            this.addedAt = addedAt;
            this.hash = hash == null ? "" : hash;
        }

        /** Hash ke sath nayi copy (items immutable hain). */
        public Item withHash(String newHash) {
            return new Item(uri, name, size, addedAt, newHash);
        }
    }

    // ------------------------------------------------------------ persist (encode / decode)

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    /** Staging list ko ek string me badlo (SharedPreferences ke liye). */
    public static String encode(List<Item> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Item it : items) {
            if (it == null || it.uri == null || it.uri.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(sanitize(it.uri)).append('\t')
              .append(sanitize(it.name)).append('\t')
              .append(it.size).append('\t')
              .append(it.addedAt).append('\t')
              .append(sanitize(it.hash));
        }
        return sb.toString();
    }

    /** Persist ki hui string se staging list wapas banao. Kharab lines skip. */
    public static List<Item> decode(String data) {
        List<Item> out = new ArrayList<>();
        if (data == null || data.isEmpty()) return out;
        for (String line : data.split("\n", -1)) {
            if (line == null || line.trim().isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length < 1 || parts[0].trim().isEmpty()) continue;
            String uri = parts[0].trim();
            String name = parts.length > 1 ? parts[1] : "";
            long size = -1;
            long addedAt = 0;
            if (parts.length > 2) {
                try { size = Long.parseLong(parts[2].trim()); } catch (Exception ignored) {}
            }
            if (parts.length > 3) {
                try { addedAt = Long.parseLong(parts[3].trim()); } catch (Exception ignored) {}
            }
            // 5th field (hash) purani saved lines me nahi hota — khaali chalega
            String hash = parts.length > 4 ? parts[4].trim() : "";
            out.add(new Item(uri, name, size, addedAt, hash));
        }
        return out;
    }

    // ------------------------------------------------------------ list ops

    public static boolean containsUri(List<Item> items, String uri) {
        if (items == null || uri == null) return false;
        for (Item it : items) {
            if (uri.equals(it.uri)) return true;
        }
        return false;
    }

    /** Naya item sirf tab add karo jab us ki URI pehle se staging me na ho. @return add hua ya nahi */
    public static boolean addIfAbsent(List<Item> items, Item item) {
        if (items == null || item == null || item.uri == null) return false;
        if (containsUri(items, item.uri)) return false;
        items.add(item);
        return true;
    }

    /** Sort ki hui NAYI list wapas karo (asal list nahi badalti). */
    public static List<Item> sorted(List<Item> items, int mode) {
        List<Item> out = new ArrayList<>();
        if (items != null) out.addAll(items);
        Comparator<Item> cmp;
        switch (mode) {
            case SORT_NAME_DESC:
                cmp = (a, b) -> nameKey(b).compareTo(nameKey(a));
                break;
            case SORT_DATE_ASC:
                cmp = (a, b) -> Long.compare(a.addedAt, b.addedAt);
                break;
            case SORT_DATE_DESC:
                cmp = (a, b) -> Long.compare(b.addedAt, a.addedAt);
                break;
            case SORT_SIZE_ASC:
                cmp = (a, b) -> Long.compare(sizeKey(a), sizeKey(b));
                break;
            case SORT_SIZE_DESC:
                cmp = (a, b) -> Long.compare(sizeKey(b), sizeKey(a));
                break;
            case SORT_NAME_ASC:
            default:
                cmp = (a, b) -> nameKey(a).compareTo(nameKey(b));
                break;
        }
        Collections.sort(out, cmp);
        return out;
    }

    private static String nameKey(Item it) {
        return it.name == null ? "" : it.name.toLowerCase();
    }

    private static long sizeKey(Item it) {
        return it.size < 0 ? Long.MAX_VALUE : it.size; // size na maloom ho to aakhir me
    }

    // ------------------------------------------------------------ lock / commit rules

    /**
     * Commit (project me copy) or Run Now sirf tab: project select ho
     * AUR staging me kam az kam ek wallpaper ho. Pick karna hamesha
     * khula hai — lock sirf commit par hai.
     */
    public static boolean canCommit(int selectedProjectNumber, int stagedCount) {
        return selectedProjectNumber > 0 && stagedCount > 0;
    }

    /** Run Now ke liye commit ke sath project ki run command bhi honi chahiye. */
    public static boolean canRun(int selectedProjectNumber, int stagedCount, boolean hasWorkCommand) {
        return canCommit(selectedProjectNumber, stagedCount) && hasWorkCommand;
    }

    // ------------------------------------------------------------ grid columns

    /**
     * v134 — Select Project screen har dafa BINA selection ke khulti
     * hai (owner ki hidayat): persisted last-selection kabhi restore
     * nahi hoti, user khud tap karke select karta hai. Isi liye ye
     * function hamesha -1 (koi selection nahi) deta hai — persisted
     * value sirf purani halat ki nishani hai, istemal nahi hoti.
     */
    public static int restoredSelection(int persistedNumber) {
        return -1;
    }

    /**
     * v134 — Drop Box compact row ke khaali slots: row hamesha 4
     * slots ki hoti hai (max 3 thumbs + View-all tile); staged kam
     * hon to baqi spacer slots taake ek image bhi poori chaudi
     * jagah na le. n<=0 par 0.
     */
    public static int compactSpacerCount(int stagedCount) {
        if (stagedCount <= 0) return 0;
        int thumbs = Math.min(3, stagedCount);
        return Math.max(0, 3 - thumbs);
    }

    /** Grid columns sirf 2 / 3 / 4 — is ke ilawa jo bhi ho, 2 par wapas. */
    public static int clampColumns(int cols) {
        return (cols == 3 || cols == 4) ? cols : 2;
    }

    /** Grid icon ka cycle: 2 -> 3 -> 4 -> 2. */
    public static int nextColumns(int current) {
        switch (clampColumns(current)) {
            case 2: return 3;
            case 3: return 4;
            default: return 2;
        }
    }
}
