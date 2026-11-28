package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Termux Mod v156 — Move ke pending originals ki DURABLE list.
 *
 * Ye list sirf copy-verify hue move originals rakhti hai —
 * is ka ek hi writer hai: commit ke baad
 * BulkStageActivity.queuePendingDeletes. Koi item list
 * se sirf tab nikalta hai jab us ka asal delete kamyab
 * ho jaye, ya wo confirmed-gone ho (file + gallery
 * record dono gayab — isGhost). Jo reh gaya wahi
 * "Pending" hai.
 *
 * List stage wali prefs file me rehti hai — app band ho
 * jaye ya process mar jaye, list mehfooz rehti hai, or
 * More Settings ki Pending row / Home ka badge icon
 * wahi se parhte hain. Process shuru hone par list
 * pehle se saaf NAHI hoti (purana rawaiya khatam) —
 * dohri launch ki hifazat in-memory flag
 * (mDeletingOriginals) se hoti hai.
 *
 * Encode/decode BulkStageActivity ke purane
 * encodeUriSet/decodeUriSet wali hi surat hai (newline
 * se juda set), taake purani saved list bhi chalti rahe.
 * Set hisaab (plus/minus/count/isGhost) PURE hai.
 */
public final class BulkPendingDeletes {

    /** Stage wali hi prefs file (BulkStageActivity.PREFS_STAGE). */
    public static final String PREFS = "bulk_wallpapers_stage";
    /** Stage wali hi key (purana KEY_PENDING_DELETES). */
    public static final String KEY = "pending_move_deletes";

    private BulkPendingDeletes() {}

    // ------------------------------------------------------------ pure

    public static Set<String> decode(String data) {
        Set<String> out = new LinkedHashSet<>();
        if (data == null || data.isEmpty()) return out;
        for (String line : data.split("\n", -1)) {
            String s = line.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    public static String encode(Set<String> uris) {
        StringBuilder sb = new StringBuilder();
        if (uris != null) {
            for (String u : uris) {
                if (u == null || u.isEmpty()) continue;
                if (sb.length() > 0) sb.append('\n');
                sb.append(u);
            }
        }
        return sb.toString();
    }

    /** Naya set: base + add (null/khali entries shamil nahi hotin). */
    public static Set<String> plus(Set<String> base, Collection<String> add) {
        Set<String> out = new LinkedHashSet<>();
        if (base != null) out.addAll(base);
        if (add != null) {
            for (String s : add) {
                if (s != null && !s.isEmpty()) out.add(s);
            }
        }
        return out;
    }

    /** Naya set: base − remove. */
    public static Set<String> minus(Set<String> base, Collection<String> remove) {
        Set<String> out = new LinkedHashSet<>();
        if (base != null) out.addAll(base);
        if (remove != null) out.removeAll(remove);
        return out;
    }

    public static int count(Set<String> set) {
        return set == null ? 0 : set.size();
    }

    /** List ki surat (saved tartib me hi). */
    public static List<String> listOf(Set<String> set) {
        List<String> out = new ArrayList<>();
        if (set != null) out.addAll(set);
        return out;
    }

    /**
     * Ghost faisla (pure): file bhi gayab or gallery
     * record bhi gayab — item ab delete karne ke qabil
     * hi nahi raha, list se khamoshi se nikal sakta hai.
     * Koi ek bhi mojood ho to item pending rehta hai.
     */
    public static boolean isGhost(boolean fileExists, boolean rowExists) {
        return !fileExists && !rowExists;
    }

    // ------------------------------------------------------------ prefs

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized Set<String> load(Context ctx) {
        try {
            return decode(prefs(ctx).getString(KEY, ""));
        } catch (Exception e) {
            return new LinkedHashSet<>();
        }
    }

    public static synchronized void save(Context ctx, Set<String> set) {
        try {
            prefs(ctx).edit().putString(KEY, encode(set)).apply();
        } catch (Exception ignored) {}
    }

    public static synchronized void addAll(Context ctx, Collection<String> uris) {
        if (uris == null || uris.isEmpty()) return;
        save(ctx, plus(load(ctx), uris));
    }

    public static synchronized void removeAll(Context ctx, Collection<String> uris) {
        if (uris == null || uris.isEmpty()) return;
        Set<String> now = load(ctx);
        Set<String> next = minus(now, uris);
        if (next.size() != now.size()) save(ctx, next);
    }

    public static int countNow(Context ctx) {
        return count(load(ctx));
    }
}
