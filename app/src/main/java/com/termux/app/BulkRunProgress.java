package com.termux.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Termux Mod (v131) — Run ki progress ka pure hisaab (Item 4).
 *
 * Runner scripts tqdm se progress deti hain — un ki bar line me
 * "n/total" hota hai (custom bar_format: "{desc} |{bar:20}| {n}/{total}",
 * ya standard tqdm: " 37%|...| 37/100 [00:12<...]"). Har line pehle
 * ANSI rang codes se saaf hoti hai, phir pehla "digits/digits" jora
 * progress mana jata hai. Koi jora na mile to progress NAHI banti —
 * caller timer + aakhri line wala fallback dikhata hai (nakli number
 * kabhi nahi).
 *
 * Ye class bilkul pure Java hai (Android dependency nahi) — unit
 * tests isi liye chal sakte hain. Ek run ke liye ek instance; lines
 * drain thread se offer() hoti hain, UI thread getters parhti hai —
 * is liye tamam methods synchronized hain (lock sirf chhote hisaab
 * par hai, read ke raste me koi rukawat nahi).
 */
public final class BulkRunProgress {

    /** ANSI escape sequence (rang/style codes) — \u001B[ ... letter. */
    private static final Pattern ANSI =
        Pattern.compile("\u001B\\[[0-9;?]*[A-Za-z]");

    /** tqdm ka "n/total" jora. */
    private static final Pattern NTOTAL =
        Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private int mDone = -1;
    private int mTotal = -1;
    private String mLastLine = "";

    /** ANSI rang codes hatao — null par khaali string. */
    public static String stripAnsi(String s) {
        if (s == null) return "";
        return ANSI.matcher(s).replaceAll("");
    }

    /**
     * Ek output line do. Wapas true agar is line se progress (n/total)
     * mili/badi. Progress wali line "aakhri line" nahi banti (wohi bar
     * bar-bar redraw hoti hai); baqi lines aakhri line ban jati hain.
     */
    public synchronized boolean offer(String rawLine) {
        String clean = stripAnsi(rawLine).trim();
        if (clean.isEmpty()) return false;
        Matcher m = NTOTAL.matcher(clean);
        if (m.find()) {
            try {
                int n = Integer.parseInt(m.group(1));
                int total = Integer.parseInt(m.group(2));
                if (total > 0 && n >= 0) {
                    mDone = n;
                    mTotal = total;
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // bohat bare digits — progress nahi mano
            }
        }
        mLastLine = clean;
        return false;
    }

    public synchronized boolean hasProgress() {
        return mTotal > 0 && mDone >= 0;
    }

    public synchronized int done() {
        return Math.max(0, mDone);
    }

    public synchronized int total() {
        return Math.max(0, mTotal);
    }

    /** 0..100 — progress na ho to -1 (caller bar chhupa de). */
    public synchronized int percent() {
        if (!hasProgress()) return -1;
        long pct = (long) mDone * 100L / mTotal;
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        return (int) pct;
    }

    /** Sab se nayi ghair-progress line (fallback display ke liye). */
    public synchronized String lastLine() {
        return mLastLine;
    }

    /** Milliseconds ko "mm:ss" — minutes 59 se barh bhi sakte hain (lambi runs). */
    public static String formatElapsed(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000L;
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        return (min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec;
    }
}
