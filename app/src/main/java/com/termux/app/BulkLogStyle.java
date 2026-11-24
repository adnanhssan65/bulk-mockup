package com.termux.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Termux Mod (v133) — Home live-log card ki line styling ka PURE
 * parser. BulkLog ki stored lines (stamp "HH:mm:ss  " ke sath) aur
 * status line (bina stamp) ko pehchan kar renderer ke liye model
 * banata hai. Jo line kisi known pattern par na utre wo PLAIN rehti
 * hai — poori line default halki rang me dikhti hai; styling kabhi
 * koi line tor'ti ya chhupati nahi.
 *
 * Known formats (BulkStrings / BulkRunHelper / BulkStageActivity):
 *   "✔ Project 13 — run mukammal — 18/18 • 00:15"   (RUN_DONE)
 *   "✔ Project 13 — run mukammal • 00:15"           (RUN_DONE, count nahi)
 *   "✔ Project 13 — run complete — 18/18 • 00:15"   (RUN_DONE, English)
 *   "✖ Project 13 — run fail — 3/18 • 00:07"        (RUN_FAIL)
 *   "▶ Project 13 — run shuru"                      (RUN_START)
 *   "Project 13 — 37/100 • 02:15"                   (RUN_PROGRESS, status line)
 *   "18 original(s) deleted from gallery (moved)"   (MOVED)
 */
public final class BulkLogStyle {

    public enum Kind { RUN_DONE, RUN_FAIL, RUN_START, RUN_PROGRESS, MOVED, PLAIN }

    /** Styled card ke fixed rang (owner ki WHITE variant pasand —
     *  card ka apna halka background hi rehta hai, ye rang usi par
     *  parhne ke liye hain). ARGB ints. */
    public static final int COLOR_TEXT = 0xFF2E4730;    // default gehri line
    public static final int COLOR_STAMP = 0xFF8A9A8A;   // muted gray time
    public static final int COLOR_PROJECT = 0xFF12240F; // bold gehra naam
    public static final int COLOR_SUB = 0xFF5A6E5A;     // "run mukammal • 00:15" darmiyani
    public static final int COLOR_GREEN = 0xFF3D9E35;   // check / count
    public static final int COLOR_RED = 0xFFD32F2F;     // fail ka nishan
    public static final int COLOR_DONE_BG = 0xFF3E9E2F; // DONE pill (solid green)
    public static final int COLOR_FAIL_BG = 0xFFD32F2F; // FAIL pill
    public static final int COLOR_MOVED_BG = 0xFFF7E3AC;   // MOVED pill (halki amber)
    public static final int COLOR_MOVED_TEXT = 0xFF8A6D1A; // MOVED pill text (gehri amber)
    public static final int COLOR_PILL_TEXT = 0xFFFFFFFF;
    public static final int COLOR_DIVIDER = 0xFFD8E4D8; // entries ke beech ki hairline
    public static final int BAR_TRACK = 0xFFDDEBD8;
    public static final int BAR_START = 0xFF7ED957;
    public static final int BAR_END = 0xFF2E9E00;

    private static final Pattern STAMP =
        Pattern.compile("^(\\d{2}:\\d{2}:\\d{2})\\s+(.*)$", Pattern.DOTALL);
    private static final Pattern COUNT_TAIL =
        Pattern.compile("^ — (\\d+)/(\\d+) • (.+)$");
    private static final Pattern TIME_TAIL =
        Pattern.compile("^ • (.+)$");
    private static final Pattern PROGRESS =
        Pattern.compile("^(Project .+?) — (\\d+)/(\\d+) • (.+)$");
    private static final Pattern MOVED =
        Pattern.compile("^(\\d+) original\\(s\\) deleted from gallery \\(moved\\)$");

    private static final String DASH = " — ";
    private static final String DONE_MARK = "✔ ";
    private static final String FAIL_MARK = "✖ ";
    private static final String START_MARK = "▶ ";
    // v134: parser DONO zubanoon ke alfaaz pehchanta hai — line jis
    // language setting me bani thi, usi ki styling milegi (Home par
    // English select ho to "run complete", warna "run mukammal").
    private static final String[] DONE_WORDS = {"run mukammal", "run complete"};
    private static final String[] FAIL_WORDS = {"run failed", "run fail"};
    private static final String[] START_WORDS = {"run shuru", "run started"};

    private BulkLogStyle() {}

    /** Ek parse ki hui line ka model — renderer sirf yehi parhta hai. */
    public static final class Parsed {
        public final String stamp;     // "12:30:18" ya null
        public final Kind kind;
        public final String project;   // run kinds ke liye "Project 13", warna null
        public final String mainText;  // PLAIN: poori body; RUN: "run mukammal" waghera; MOVED: count ke baad wala hissa
        public final String countText; // "18/18" (RUN) ya "18" (MOVED), warna null
        public final int cur;          // RUN progress ka pehla hissa, na ho to -1
        public final int total;        // RUN progress ka dusra hissa, na ho to -1
        public final String timeText;  // "00:15" ya null
        public final String pill;      // "DONE" / "FAIL" / "MOVED" ya null

        Parsed(String stamp, Kind kind, String project, String mainText,
               String countText, int cur, int total, String timeText, String pill) {
            this.stamp = stamp;
            this.kind = kind;
            this.project = project;
            this.mainText = mainText;
            this.countText = countText;
            this.cur = cur;
            this.total = total;
            this.timeText = timeText;
            this.pill = pill;
        }

        public boolean hasProgress() {
            return cur >= 0 && total > 0;
        }

        /** Bar ka bhara hua hissa (0..100) — progress na ho to 0. */
        public int percent() {
            if (!hasProgress()) return 0;
            int pct = (int) Math.round(100.0 * cur / total);
            return Math.max(0, Math.min(100, pct));
        }
    }

    /**
     * v134 — RUN_DONE / RUN_FAIL entry ki line tartib (PURE model,
     * renderer isi par chalta hai): progress bar PEHLI line par,
     * head (stamp + nishan + project) us ke baad, aur DONE/FAIL pill
     * wali sub line SAB SE AAKHIR me. Baqi kinds ke liye khaali list
     * (un ka layout is model se nahi banta).
     */
    public enum LinePart { BAR, HEAD, SUB_WITH_PILL }

    public static java.util.List<LinePart> entryLayout(Parsed p) {
        java.util.List<LinePart> out = new java.util.ArrayList<>();
        if (p == null) return out;
        if (p.kind != Kind.RUN_DONE && p.kind != Kind.RUN_FAIL) return out;
        if (p.hasProgress()) out.add(LinePart.BAR);
        out.add(LinePart.HEAD);
        out.add(LinePart.SUB_WITH_PILL);
        return out;
    }

    private static Parsed plain(String stamp, String body) {
        return new Parsed(stamp, Kind.PLAIN, null, body, null, -1, -1, null, null);
    }

    /** Stored/status line ko parse karo — kabhi null nahi, kabhi throw nahi. */
    public static Parsed parse(String line) {
        if (line == null) return plain(null, "");
        String stamp = null;
        String body = line;
        Matcher sm = STAMP.matcher(line);
        if (sm.matches()) {
            stamp = sm.group(1);
            body = sm.group(2);
        }
        try {
            Parsed run = parseRun(stamp, body);
            if (run != null) return run;
            Matcher mv = MOVED.matcher(body);
            if (mv.matches()) {
                return new Parsed(stamp, Kind.MOVED, null,
                    "original(s) deleted from gallery", mv.group(1), -1, -1, null, "MOVED");
            }
            if (stamp == null) {
                Matcher pg = PROGRESS.matcher(body);
                if (pg.matches()) {
                    int cur = Integer.parseInt(pg.group(2));
                    int total = Integer.parseInt(pg.group(3));
                    return new Parsed(null, Kind.RUN_PROGRESS, pg.group(1), null,
                        cur + "/" + total, cur, total, pg.group(4), null);
                }
            }
        } catch (Exception ignored) {
            // parsing ki koi bhi ghalati line ko chhupa nahi sakti
        }
        return plain(stamp, body);
    }

    /** ✔/✖/▶ wali run lines; pattern na mile to null (caller PLAIN bana dega). */
    private static Parsed parseRun(String stamp, String body) {
        Kind kind;
        String[] words;
        String pill;
        String rest;
        if (body.startsWith(DONE_MARK)) {
            kind = Kind.RUN_DONE; words = DONE_WORDS; pill = "DONE";
            rest = body.substring(DONE_MARK.length());
        } else if (body.startsWith(FAIL_MARK)) {
            kind = Kind.RUN_FAIL; words = FAIL_WORDS; pill = "FAIL";
            rest = body.substring(FAIL_MARK.length());
        } else if (body.startsWith(START_MARK)) {
            kind = Kind.RUN_START; words = START_WORDS; pill = null;
            rest = body.substring(START_MARK.length());
        } else {
            return null;
        }
        String word = null;
        int idx = -1;
        for (String w : words) {
            int i = rest.indexOf(DASH + w);
            if (i > 0) { word = w; idx = i; break; }
        }
        if (word == null) return null;
        String project = rest.substring(0, idx);
        String tail = rest.substring(idx + DASH.length() + word.length());
        if (kind == Kind.RUN_START) {
            if (!tail.isEmpty()) return null;
            return new Parsed(stamp, kind, project, word, null, -1, -1, null, null);
        }
        Matcher cm = COUNT_TAIL.matcher(tail);
        if (cm.matches()) {
            int cur = Integer.parseInt(cm.group(1));
            int total = Integer.parseInt(cm.group(2));
            return new Parsed(stamp, kind, project, word,
                cur + "/" + total, cur, total, cm.group(3), pill);
        }
        Matcher tm = TIME_TAIL.matcher(tail);
        if (tm.matches()) {
            return new Parsed(stamp, kind, project, word, null, -1, -1, tm.group(1), pill);
        }
        return null;
    }
}
