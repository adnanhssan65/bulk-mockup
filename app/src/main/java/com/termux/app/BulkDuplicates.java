package com.termux.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Termux Mod (v125) — Duplicates ki pure logic.
 *
 * Staging me add hote waqt har image ka SHA-256 hash banta hai. Jo image
 * pehle se staging me ho (ya usi batch me pehle accept ho chuki ho) wo
 * DUPLICATE hai — wo staging grid me nahi jati, Duplicates holding me
 * jati hai. Duplicates ek waqt (TTL, default 24 ghante) ke baad khud
 * delete ho jate hain; TTL Duplicates screen se badla ja sakta hai
 * (0 = kabhi khud delete nahi).
 *
 * Ye class pure Java hai — dedupe or expiry ke faisle unit tests me
 * cover hote hain. File/prefs ka asal kaam BulkDuplicatesStore karta hai.
 */
public final class BulkDuplicates {

    public static final int DEFAULT_TTL_HOURS = 24;

    private BulkDuplicates() {}

    /** Ek duplicate copy ka record (holding folder ki file ke sath). */
    public static final class Entry {
        public final String hash;
        public final String name;
        public final long arrivalAt; // millis — expiry isi se hisaab hoti hai
        public final String fileName; // holding folder ke andar ki file

        public Entry(String hash, String name, long arrivalAt, String fileName) {
            this.hash = hash == null ? "" : hash;
            this.name = name == null ? "" : name;
            this.arrivalAt = arrivalAt;
            this.fileName = fileName == null ? "" : fileName;
        }
    }

    // ------------------------------------------------------------ dedupe decision

    /**
     * Candidate duplicate hai ya nahi: us ka hash ya to mojooda staging
     * me hai ya is batch me pehle accept hue hashes me. Hash na ho
     * (null/khaali) to dedupe nahi ho sakta — duplicate nahi mana jata.
     */
    public static boolean isDuplicate(Set<String> stagedHashes, Set<String> batchAcceptedHashes,
                                      String candidateHash) {
        if (candidateHash == null || candidateHash.isEmpty()) return false;
        if (stagedHashes != null && stagedHashes.contains(candidateHash)) return true;
        return batchAcceptedHashes != null && batchAcceptedHashes.contains(candidateHash);
    }

    // ------------------------------------------------------------ expiry decision

    /** TTL ghanton me; <= 0 ka matlab auto-delete band (kabhi expire nahi). */
    public static boolean isExpired(long arrivalAt, long now, int ttlHours) {
        if (ttlHours <= 0) return false;
        if (arrivalAt <= 0) return false;
        return now - arrivalAt >= ttlHours * 3600_000L;
    }

    /** Jo entries expire ho gayi hon — delete karne ke liye. */
    public static List<Entry> expiredEntries(List<Entry> entries, long now, int ttlHours) {
        List<Entry> out = new ArrayList<>();
        if (entries == null) return out;
        for (Entry e : entries) {
            if (e != null && isExpired(e.arrivalAt, now, ttlHours)) out.add(e);
        }
        return out;
    }

    /** Expiry ke baad bachne wali entries. */
    public static List<Entry> remainingEntries(List<Entry> entries, long now, int ttlHours) {
        List<Entry> out = new ArrayList<>();
        if (entries == null) return out;
        for (Entry e : entries) {
            if (e != null && !isExpired(e.arrivalAt, now, ttlHours)) out.add(e);
        }
        return out;
    }

    // ------------------------------------------------------------ index persist (encode / decode)

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    /** Index ko ek string me badlo: har line hash \t name \t arrivalAt \t fileName. */
    public static String encode(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Entry e : entries) {
            if (e == null || e.fileName.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(sanitize(e.hash)).append('\t')
              .append(sanitize(e.name)).append('\t')
              .append(e.arrivalAt).append('\t')
              .append(sanitize(e.fileName));
        }
        return sb.toString();
    }

    /** Persist ki hui index string se entries wapas. Kharab lines skip. */
    public static List<Entry> decode(String data) {
        List<Entry> out = new ArrayList<>();
        if (data == null || data.isEmpty()) return out;
        for (String line : data.split("\n", -1)) {
            if (line == null || line.trim().isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length < 4 || parts[3].trim().isEmpty()) continue;
            long arrival = 0;
            try { arrival = Long.parseLong(parts[2].trim()); } catch (Exception ignored) {}
            out.add(new Entry(parts[0].trim(), parts[1], arrival, parts[3].trim()));
        }
        return out;
    }
}
