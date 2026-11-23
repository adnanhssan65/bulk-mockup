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
}
