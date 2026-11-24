package com.termux.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Termux Mod (v133) — Projects screen ke Delete popup ke options
 * (PURE Java, unit tests ke liye).
 *
 * Options asal runner scripts ke apne delete-menu se liye gaye
 * hain — alfaaz dono zubano me wohi (scripts ke apne menu words):
 *
 *   system_core.py (Project 16 wala khandan):
 *     1 Input (Images Only) · 2 Tayaar Maal (Images Only) ·
 *     3 Warehouse
 *   baqi tamam (adnan_core / dual_core / project3_core /
 *   adnan_core_4 / w-family):
 *     1 Tayaar Maal (Mockups + Ready) · 2 Sirf Mockups ·
 *     3 Input Wallpapers · 4 Warehouse · 5 Backup Restore
 *
 * Warehouse wala choice script se batch ka sawal poochta hai —
 * is liye wo piped nahi jata; terminal par interactive chalega.
 */
public final class BulkDeleteMenu {

    private BulkDeleteMenu() {}

    /** Ek delete option: menu digit + label + warehouse hai ya nahi. */
    public static final class Option {
        public final int digit;
        public final String label;
        public final boolean warehouse;

        Option(int digit, String label, boolean warehouse) {
            this.digit = digit;
            this.label = label;
            this.warehouse = warehouse;
        }
    }

    public static boolean isSystemCore(String script) {
        return "system_core.py".equals(script);
    }

    /** Detected main script ke hisab se delete menu (tartib = script ki menu tartib). */
    public static List<Option> optionsFor(String script) {
        List<Option> out = new ArrayList<>();
        if (isSystemCore(script)) {
            out.add(new Option(1, "Input (Images Only)", false));
            out.add(new Option(2, "Tayaar Maal (Images Only)", false));
            out.add(new Option(3, "Warehouse (Ready_Upload_Batches)", true));
        } else {
            out.add(new Option(1, "Tayaar Maal (Mockups + Ready Wallpapers)", false));
            out.add(new Option(2, "Sirf Mockups", false));
            out.add(new Option(3, "Input Wallpapers", false));
            out.add(new Option(4, "Warehouse (Ready_Upload_Batches)", true));
            out.add(new Option(5, "Backup se Wapas Lao (Restore)", false));
        }
        return out;
    }

    /** Digit ka option; na mile to null. */
    public static Option optionFor(String script, int digit) {
        for (Option o : optionsFor(script)) {
            if (o.digit == digit) return o;
        }
        return null;
    }

    /** Ye choice Warehouse hai? (terminal interactive rasta lagega) */
    public static boolean isWarehouse(String script, int digit) {
        Option o = optionFor(script, digit);
        return o != null && o.warehouse;
    }
}
