package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Termux Mod (v125) — Duplicates ka asal storage.
 *
 * Duplicate copies app ke internal folder (filesDir/bulk_duplicates) me
 * file ban kar rehti hain; un ka index (hash / naam / arrival time /
 * file naam) SharedPreferences me persist hota hai — is liye expiry
 * app band karke kholne par bhi sahi chalta hai. Faisle (dedupe /
 * expiry) BulkDuplicates (pure) me hote hain; ye class sirf IO hai.
 */
public final class BulkDuplicatesStore {

    private static final String LOG_TAG = "BulkDuplicatesStore";
    private static final String PREFS = "bulk_duplicates";
    private static final String KEY_INDEX = "index";
    private static final String KEY_TTL = "ttl_hours";
    private static final String DIR_NAME = "bulk_duplicates";

    private BulkDuplicatesStore() {}

    // ------------------------------------------------------------ prefs / dir

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static File holdingDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), DIR_NAME);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static synchronized List<BulkDuplicates.Entry> loadEntries(Context ctx) {
        return BulkDuplicates.decode(prefs(ctx).getString(KEY_INDEX, ""));
    }

    private static synchronized void saveEntries(Context ctx, List<BulkDuplicates.Entry> entries) {
        prefs(ctx).edit().putString(KEY_INDEX, BulkDuplicates.encode(entries)).apply();
    }

    public static int count(Context ctx) {
        return loadEntries(ctx).size();
    }

    public static int getTtlHours(Context ctx) {
        return prefs(ctx).getInt(KEY_TTL, BulkDuplicates.DEFAULT_TTL_HOURS);
    }

    public static void setTtlHours(Context ctx, int hours) {
        prefs(ctx).edit().putInt(KEY_TTL, hours).apply();
    }

    // ------------------------------------------------------------ add / delete / expiry

    /**
     * Ek duplicate copy holding me rakho: bytes file me copy + index entry.
     * @return bani hui entry, ya fail par null
     */
    public static synchronized BulkDuplicates.Entry addDuplicate(
            Context ctx, InputStream in, String name, String hash, long arrivalAt) {
        if (in == null) return null;
        String safeName = sanitizeFileName(name);
        String fileName = arrivalAt + "_" + safeName;
        File dest = new File(holdingDir(ctx), fileName);
        int i = 1;
        while (dest.exists()) {
            dest = new File(holdingDir(ctx), arrivalAt + "_" + i + "_" + safeName);
            i++;
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            out.flush();
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Duplicate copy fail: " + e);
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
            return null;
        } finally {
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { in.close(); } catch (Exception ignored) {}
        }
        BulkDuplicates.Entry entry =
            new BulkDuplicates.Entry(hash, name, arrivalAt, dest.getName());
        List<BulkDuplicates.Entry> entries = loadEntries(ctx);
        entries.add(entry);
        saveEntries(ctx, entries);
        return entry;
    }

    /** Di hui entries ko holding se delete karo (file + index dono). */
    public static synchronized void deleteEntries(Context ctx, List<BulkDuplicates.Entry> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) return;
        List<BulkDuplicates.Entry> entries = loadEntries(ctx);
        List<BulkDuplicates.Entry> keep = new ArrayList<>();
        for (BulkDuplicates.Entry e : entries) {
            boolean drop = false;
            for (BulkDuplicates.Entry d : toDelete) {
                if (d.fileName.equals(e.fileName)) { drop = true; break; }
            }
            if (drop) {
                //noinspection ResultOfMethodCallIgnored
                new File(holdingDir(ctx), e.fileName).delete();
            } else {
                keep.add(e);
            }
        }
        saveEntries(ctx, keep);
    }

    /**
     * Expiry check — Add Wallpapers / Dashboard khulne par chalta hai.
     * TTL guzar chuki copies delete. @return kitni delete huin
     */
    public static synchronized int runExpiry(Context ctx) {
        List<BulkDuplicates.Entry> entries = loadEntries(ctx);
        if (entries.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        int ttl = getTtlHours(ctx);
        List<BulkDuplicates.Entry> expired = BulkDuplicates.expiredEntries(entries, now, ttl);
        if (expired.isEmpty()) return 0;
        deleteEntries(ctx, expired);
        Logger.logInfo(LOG_TAG, "Duplicates expiry: " + expired.size() + " delete huin");
        return expired.size();
    }

    // ------------------------------------------------------------ hashing

    /** Stream ka SHA-256 hex. Stream band nahi hota (caller ki zimmedari). */
    public static String sha256Hex(InputStream in) {
        if (in == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) md.update(buf, 0, r);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Hash fail: " + e);
            return "";
        }
    }

    private static String sanitizeFileName(String name) {
        String n = name == null ? "" : name;
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) n = n.substring(slash + 1);
        n = n.replaceAll("[^A-Za-z0-9._-]", "_").trim();
        return n.isEmpty() ? "image.jpg" : n;
    }
}
