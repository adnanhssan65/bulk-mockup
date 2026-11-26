package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Termux Mod (v127) — Run ke sawalon (HD + Size) ki GLOBAL settings aur
 * jawab → stdin mapping. Settings ek hi saved set hai (per-project nahi),
 * `bulk_mockup` prefs me mehfooz.
 *
 * VERIFIED PROMPT MAP (2026-11-23: tamam 14 bundled runners ki process
 * branch ka actual code parha gaya + Size prompt har runner me P1 ki
 * implementation ki copy ke tor par add/test kiya gaya):
 *   Har project 1..14 ki `process` run me DO prompts is order me ate hain:
 *     1) Mode: "[1] HD Mode - [DEFAULT] / [2] Normal (Fast KB Pack)"
 *        script: is_hd = (choice != '2')  → "1" (ya Enter) = HD, "2" = Normal
 *     2) Size: "[1] Original Size - [DEFAULT] / [2] Crop 9:16"
 *        script: crop_916 = (choice == '2') → "1" (ya Enter) = Original, "2" = Crop
 *   Is range se bahar ke projects (15/17/18, ZIP se add hue naye) ke
 *   prompts verify NAHI hain — un ke liye koi pre-answer nahi banta;
 *   wo plain terminal run hi karte hain.
 *
 * Mode: AUTO (default, Adnan 2026-11-23 16:51) = saved jawab khamoshi
 * se pipe ho jate hain, koi menu nahi khulta. Factory defaults (Adnan
 * 2026-11-23 16:53): HD = script ka apna default, Size = Crop 9:16
 * (ye Adnan ki standing pasand hai — script ka Enter-default Original
 * hai, lekin app ka factory default Crop hai). MANUAL = har Run par
 * popup me sawal.
 */
public final class BulkRunSettings {

    public static final String MODE_AUTO = "auto";
    public static final String MODE_MANUAL = "manual";

    public static final String ANSWER_HD = "1";
    public static final String ANSWER_NORMAL = "2";
    public static final String ANSWER_ORIGINAL = "1";
    public static final String ANSWER_CROP = "2";

    private static final String PREFS = "bulk_mockup";
    private static final String KEY_MODE = "run_answer_mode";
    private static final String KEY_HD = "run_answer_hd";
    private static final String KEY_SIZE = "run_answer_size";
    private static final String KEY_RUN_VIEW_TERMINAL = "run_view_terminal";

    private BulkRunSettings() {}

    // ------------------------------------------------------------ pure logic

    /** Sirf 1..14 ke prompts verified hain (bundled runners). */
    public static boolean hasVerifiedPrompts(int projectNumber) {
        return projectNumber >= 1 && projectNumber <= 14;
    }

    /**
     * Project ke liye stdin jawab, script ke prompt order me:
     * [hdAnswer, sizeAnswer]. Unverified project → khaali list (koi pipe nahi).
     * Ghalat/unknown jawab hamesha script ke DEFAULT par girta hai
     * (HD / Original) — Enter dabane ke barabar.
     */
    public static List<String> answersFor(int projectNumber, String hdAnswer, String sizeAnswer) {
        List<String> out = new ArrayList<>();
        if (!hasVerifiedPrompts(projectNumber)) return out;
        out.add(ANSWER_NORMAL.equals(hdAnswer) ? ANSWER_NORMAL : ANSWER_HD);
        out.add(ANSWER_CROP.equals(sizeAnswer) ? ANSWER_CROP : ANSWER_ORIGINAL);
        return out;
    }

    // ------------------------------------------------------------ v137 prompt profiles

    /**
     * v137 — runner ke prompt khandan (families), asal scripts parh
     * kar verify shuda (bundled 1..14 + ZIP wale 15..18 ki copies):
     *
     *   ADNAN  (adnan_core.py, adnan_core_4.py, dual_core.py,
     *           project3_core.py, w<N>.py):
     *           Mode [1] HD (DEFAULT) / [2] Normal — is_hd = jawab != '2'
     *           Size [1] Original (DEFAULT) / [2] Crop — crop = jawab == '2'
     *   SYSTEM (system_core.py):
     *           Mode [1] Normal (DEFAULT) / [2] HD — is_hd = jawab == '2'
     *           Size ADNAN wali hi tartib.
     *   SINGLE (w20.py — v147 me bundled):
     *           Sirf EK sawal: Mode [1] HD (DEFAULT) / [2] Normal.
     *           Size wala sawal is runner me hai hi nahi — jawab
     *           sirf mode wala pipe hota hai.
     *   NONE (w21.py — v147 me bundled):
     *           Koi sawal nahi — runner poori tarah automatic hai
     *           (video → Live Photo). Kuch pipe nahi hota.
     *   UNKNOWN (koi or script): app ko sawal maloom nahi — sirf
     *           Enter pipe hota hai (script ke apne defaults lagu).
     *
     * Ye profiles is liye zaroori hain ke ZIP-added projects bhi ab
     * console popup me chalte hain (terminal kabhi nahi khulta —
     * owner ki sakht hidayat, v137).
     */
    public enum PromptProfile { ADNAN, SYSTEM, SINGLE, NONE, UNKNOWN }

    /** Script ke naam se prompt khandan pehchano (pure). */
    public static PromptProfile profileForScript(String script) {
        if (script == null) return PromptProfile.UNKNOWN;
        String s = script.trim();
        if (s.equals("system_core.py")) return PromptProfile.SYSTEM;
        // v147: w20/w21 ke apne khandan aam w<N> wale ADNAN rule se
        // PEHLE — un ke sawal ginti me mukhtalif hain (1 / 0).
        if (s.equals("w20.py")) return PromptProfile.SINGLE;
        if (s.equals("w21.py")) return PromptProfile.NONE;
        if (s.equals("adnan_core.py") || s.equals("adnan_core_4.py")
            || s.equals("dual_core.py") || s.equals("project3_core.py")
            || s.matches("w\\d+\\.py")) {
            return PromptProfile.ADNAN;
        }
        return PromptProfile.UNKNOWN;
    }

    /**
     * Project ka profile: bundled 1..14 ke prompts pehle se verified
     * hain (sab ADNAN khandan ke main scripts) — baqi projects ka
     * faisla un ki asal (persisted/detected) script se. v139: 15–18
     * bhi bundled hain; un ke profiles bhi script se hi bante hain
     * (w15/w17/w18 = ADNAN, P16 ki system_core.py = SYSTEM) — dono
     * khandan VM par asal runs se verify shuda hain. v147: 20/21 bhi
     * bundled — w20.py = SINGLE (sirf Mode sawal), w21.py = NONE
     * (koi sawal nahi), dono VM par asal runs se verify shuda.
     */
    public static PromptProfile profileFor(int projectNumber, String script) {
        if (hasVerifiedPrompts(projectNumber)) return PromptProfile.ADNAN;
        return profileForScript(script);
    }

    /**
     * Profile ke hisab se stdin jawab [mode, size]. UNKNOWN par do
     * khaali Enter — script apne defaults par chalti hai, lekin run
     * phir bhi console popup ke andar hi rehta hai.
     * SINGLE par sirf mode jawab [mode]; NONE par khaali list —
     * buildPipedCommand bina pipe ke asal command chalata hai.
     * Ghalat/unknown saved jawab hamesha script ke DEFAULT par
     * girta hai (ADNAN: HD/Original; SYSTEM: Normal/Original).
     */
    public static List<String> answersForProfile(PromptProfile profile,
                                                 String hdAnswer, String sizeAnswer) {
        List<String> out = new ArrayList<>();
        if (profile == null || profile == PromptProfile.UNKNOWN) {
            out.add("");
            out.add("");
            return out;
        }
        if (profile == PromptProfile.NONE) {
            return out; // koi sawal hi nahi — kuch pipe nahi hota
        }
        boolean normal = ANSWER_NORMAL.equals(hdAnswer);
        if (profile == PromptProfile.SINGLE) {
            out.add(normal ? "2" : "1"); // ADNAN wali tartib: '1'=HD (default), '2'=Normal
            return out;
        }
        boolean crop = ANSWER_CROP.equals(sizeAnswer);
        if (profile == PromptProfile.SYSTEM) {
            out.add(normal ? "1" : "2"); // SYSTEM khandan: '1'=Normal (default), '2'=HD
        } else {
            out.add(normal ? "2" : "1"); // ADNAN khandan: '1'=HD (default), '2'=Normal
        }
        out.add(crop ? "2" : "1"); // dono khandan: '1'=Original (default), '2'=Crop
        return out;
    }

    /**
     * Jawab terminal command ke stdin me pipe karne wali command:
     *   printf '%s\n' '1' '1' | (cd ... && python adnan_core.py process 1)
     * v129: workCmd hamesha POORI command hoti hai (BulkProjects
     * bare aliases ko merge ke waqt hi resolve kar deta hai) — is
     * command ko kisi bhi shell me alias ki zaroorat nahi. Subshell
     * wrap is liye ke compound command (cd ... && python ...) par
     * pipe sahi lage. Khaali jawab → asal command waisa ka waisa.
     */
    public static String buildPipedCommand(String workCmd, List<String> answers) {
        if (workCmd == null) return null;
        if (answers == null || answers.isEmpty()) return workCmd;
        StringBuilder sb = new StringBuilder("printf '%s\\n'");
        for (String a : answers) {
            sb.append(" '").append(a).append("'");
        }
        sb.append(" | (").append(workCmd).append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------ prefs glue

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Default AUTO (Adnan 2026-11-23) — factory jawab: HD default + Size Crop. */
    public static String mode(Context c) {
        String m = prefs(c).getString(KEY_MODE, MODE_AUTO);
        return MODE_MANUAL.equals(m) ? MODE_MANUAL : MODE_AUTO;
    }

    public static boolean isAuto(Context c) {
        return MODE_AUTO.equals(mode(c));
    }

    public static void setMode(Context c, boolean auto) {
        prefs(c).edit().putString(KEY_MODE, auto ? MODE_AUTO : MODE_MANUAL).apply();
    }

    public static String hdAnswer(Context c) {
        String v = prefs(c).getString(KEY_HD, ANSWER_HD);
        return ANSWER_NORMAL.equals(v) ? ANSWER_NORMAL : ANSWER_HD;
    }

    public static void setHdAnswer(Context c, boolean normal) {
        prefs(c).edit().putString(KEY_HD, normal ? ANSWER_NORMAL : ANSWER_HD).apply();
    }

    /** Factory default CROP 9:16 (Adnan ki standing pasand, 2026-11-23). */
    public static String sizeAnswer(Context c) {
        String v = prefs(c).getString(KEY_SIZE, ANSWER_CROP);
        return ANSWER_ORIGINAL.equals(v) ? ANSWER_ORIGINAL : ANSWER_CROP;
    }

    public static void setSizeAnswer(Context c, boolean crop) {
        prefs(c).edit().putString(KEY_SIZE, crop ? ANSWER_CROP : ANSWER_ORIGINAL).apply();
    }

    // ------------------------------------------------------------ v151: run view

    /**
     * v151 (owner 2026-11-26): Run ka kaam HAR haal me asal
     * terminal session karta hai — ye setting sirf VIEW chunti
     * hai: Run dabane par kya dikhe. ON = terminal samne aye;
     * OFF (default) = maujooda console popup sirf showpiece ke
     * tor par dikhe (display-only — kaam peeche terminal
     * session hi karta hai). Faisla pure hai: stored value na
     * ho (fresh install) to POPUP view; stored true ho tab hi
     * terminal view.
     */
    public static boolean resolveTerminalView(Boolean stored) {
        return stored != null && stored;
    }

    /** Run par terminal samne aye? Default NAHI — popup view (v151). */
    public static boolean isTerminalView(Context c) {
        SharedPreferences p = prefs(c);
        Boolean stored = p.contains(KEY_RUN_VIEW_TERMINAL)
            ? p.getBoolean(KEY_RUN_VIEW_TERMINAL, false) : null;
        return resolveTerminalView(stored);
    }

    public static void setTerminalView(Context c, boolean terminal) {
        prefs(c).edit().putBoolean(KEY_RUN_VIEW_TERMINAL, terminal).apply();
    }
}
