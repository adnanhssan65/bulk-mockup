package com.termux.app;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Termux Mod (v129) — Home ke hero card par "TV ki tarah" LIVE LOGS.
 *
 * Ek chhota sa in-app ring buffer: app ke actions (run start/finish,
 * add, export waghera) aur run output ki aakhri lines yahan jama hoti
 * hain; Home ka terminal card is ki aakhri ~6 lines live dikhata hai
 * (dashboard har second taaza karta hai, nayi line sab se neeche —
 * auto-scroll ki shakal yahi hai). Buffer sirf memory me hai — app
 * band to logs saaf (ye asal terminal ki history nahi hai).
 *
 * Pure Java (Android dependency nahi) taake unit test ho sake.
 */
public final class BulkLog {

    /** Buffer me zyada se zyada itni lines — card ko sirf tail chahiye. */
    public static final int MAX_LINES = 60;
    /** Ek line is se lambi ho to kaat do (card chhota hai). */
    public static final int MAX_LINE_CHARS = 90;

    private static final List<String> LINES = new ArrayList<>();

    private BulkLog() {}

    /** Ek line jama karo (time stamp ke sath mehfooz hoti hai). */
    public static synchronized void add(String line) {
        if (line == null) return;
        String t = line.trim();
        if (t.isEmpty()) return;
        if (t.length() > MAX_LINE_CHARS) t = t.substring(0, MAX_LINE_CHARS);
        String stamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        LINES.add(stamp + "  " + t);
        while (LINES.size() > MAX_LINES) LINES.remove(0);
    }

    /** Jama shuda lines ki copy (purani → nayi tartib me). */
    public static synchronized List<String> lines() {
        return new ArrayList<>(LINES);
    }

    public static synchronized int size() {
        return LINES.size();
    }

    public static synchronized void clear() {
        LINES.clear();
    }

    /**
     * Card ke liye aakhri {@code max} lines ek text block me (nayi line
     * sab se neeche). Buffer khaali ho to idle line wapas hoti hai.
     */
    public static synchronized String tailText(int max, String idleLine) {
        if (LINES.isEmpty()) return idleLine == null ? "" : idleLine;
        int from = Math.max(0, LINES.size() - Math.max(1, max));
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < LINES.size(); i++) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(LINES.get(i));
        }
        return sb.toString();
    }
}
