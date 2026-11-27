package com.termux.app;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * v154 — Recycle Bin: Remove Project ab project ko hamesha ke
 * liye delete NAHI karta; engine folder yahan ($HOME ke andar
 * ".bulk_recycle_bin/mockup_project_<N>") chala jata hai, jahan
 * se wahi Projects popup use RESTORE (engine folder wapas +
 * conf/alias dobara register) ya DELETE FOREVER kar sakta hai.
 *
 * Har entry ke sath ek chhoti meta file (project_<N>.meta)
 * rehti hai jis me sirf display naam hota hai — popup ki list
 * usi se banti hai. TermuxMod ke outputs (Pairs/Batches) ko
 * bin kabhi hath nahi lagata — wo hamesha mehfooz.
 */
public final class BulkRecycleBin {

    private BulkRecycleBin() {}

    /** Bin ka root folder ($HOME ke andar — shared storage par kabhi nahi). */
    public static File binRoot() {
        return new File(BulkMockupSetup.homeDir(), ".bulk_recycle_bin");
    }

    /** Ek entry ke folder ka naam (pure). */
    public static String binFolderName(int n) {
        return "mockup_project_" + n;
    }

    /** Folder naam se project number (pure); bin folder na ho to -1. */
    public static int parseBinFolder(String name) {
        if (name == null) return -1;
        String prefix = "mockup_project_";
        if (!name.startsWith(prefix)) return -1;
        try {
            int n = Integer.parseInt(name.substring(prefix.length()).trim());
            return n > 0 ? n : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Entry ka folder. */
    public static File entryDir(int n) {
        return new File(binRoot(), binFolderName(n));
    }

    /** Entry ki meta file (display naam). */
    public static File metaFile(int n) {
        return new File(binRoot(), "project_" + n + ".meta");
    }

    /** Meta file me display naam likho (khaali ho to "Project N"). */
    public static void writeMeta(int n, String displayName) {
        try {
            //noinspection ResultOfMethodCallIgnored
            binRoot().mkdirs();
            String name = displayName != null && !displayName.trim().isEmpty()
                ? displayName.trim() : "Project " + n;
            try (FileOutputStream out = new FileOutputStream(metaFile(n))) {
                out.write(name.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    /** Entry ka display naam (meta se; na mile to "Project N"). */
    public static String displayName(int n) {
        try {
            File f = metaFile(n);
            if (f.isFile()) {
                byte[] buf = new byte[(int) Math.min(f.length(), 512)];
                int read;
                try (FileInputStream in = new FileInputStream(f)) {
                    read = in.read(buf);
                }
                if (read > 0) {
                    String s = new String(buf, 0, read, StandardCharsets.UTF_8).trim();
                    if (!s.isEmpty()) return s;
                }
            }
        } catch (Exception ignored) {}
        return "Project " + n;
    }

    /** Bin me mojood project numbers, chhote se bare. */
    public static List<Integer> entries() {
        List<Integer> out = new ArrayList<>();
        File[] kids = binRoot().listFiles();
        if (kids != null) {
            for (File k : kids) {
                if (!k.isDirectory()) continue;
                int n = parseBinFolder(k.getName());
                if (n > 0) out.add(n);
            }
        }
        Collections.sort(out);
        return out;
    }

    /** Entry bin me hai ya nahi. */
    public static boolean hasEntry(int n) {
        return n > 0 && entryDir(n).isDirectory();
    }

    /**
     * Entry ko RESTORE karo: engine folder wapas apni jagah,
     * phir conf line + aliases dobara register or drop folder
     * dobara provision. Bundled project agar Dashboard ki
     * only-if-missing extraction se pehle hi wapas aa chuka ho
     * (engine folder mojood) to bin wali copy phenk di jati hai
     * or sirf registration pakki hoti hai — data kabhi do jagah
     * nahi rehta.
     * @return restore ho gaya ya nahi
     */
    public static boolean restoreProject(Context ctx, int n) {
        if (ctx == null || n <= 0 || !hasEntry(n)) return false;
        try {
            File entry = entryDir(n);
            File target = BulkProjects.projectDir(n);
            if (target.isDirectory()) {
                BulkProjectInstaller.deleteRecursive(entry);
            } else {
                File parent = target.getParentFile();
                if (parent != null) {
                    //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                }
                if (!entry.renameTo(target)) {
                    BulkProjectInstaller.copyDir(entry, target);
                    BulkProjectInstaller.deleteRecursive(entry);
                }
            }
            BulkProjectInstaller.appendConfLine(ctx, n);
            String script = BulkProjects.resolveScript(n);
            if (script != null) BulkProjectInstaller.appendAliases(n, script);
            BulkDropFolders.ensureForProject(ctx, n);
            //noinspection ResultOfMethodCallIgnored
            metaFile(n).delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Entry ko hamesha ke liye delete karo (folder + meta). */
    public static void deleteForever(int n) {
        if (n <= 0) return;
        BulkProjectInstaller.deleteRecursive(entryDir(n));
        //noinspection ResultOfMethodCallIgnored
        metaFile(n).delete();
    }
}
