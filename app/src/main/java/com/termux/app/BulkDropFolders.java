package com.termux.app;

import android.content.Context;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manual-move drop folders (v132): /storage/emulated/0/TermuxMod/Project N/.
 *
 * User file manager se images "TermuxMod/Project N" me daalta hai; app
 * Dashboard / Add Wallpapers / Projects khulte hi unhe khud
 * mockup_project_N/N_wallpapers me MOVE kar deti hai (naam takraye to
 * staging wala _1 suffix, SHA-256 duplicate ho to Duplicates holding,
 * mtime staging commit jesa +1s stagger). Is class ka planning hissa
 * (folder-naam parse, unique naam, fetch plan) PURE hai — unit tests me.
 * IO hissa kabhi delete nahi karta: drop folder se file ya to project me
 * jati hai ya Duplicates holding me; fail ho to wahin rehti hai.
 */
public final class BulkDropFolders {

    private static final String LOG_TAG = "BulkDropFolders";

    public static final String ROOT_NAME = "TermuxMod";

    private static final Pattern DROP_NAME =
        Pattern.compile("(?i)^project\\s+(\\d+)$");

    private BulkDropFolders() {}

    // ------------------------------------------------------------ pure

    /** Drop folder ka naam: "Project 5". */
    public static String dropFolderName(int n) {
        return "Project " + n;
    }

    /** v133: har drop folder me project ki pehchan ke liye preview video ka naam. */
    public static String previewFileName(int n) {
        return "Preview - Project " + n + ".mp4";
    }

    /** Preview video ka bundled asset path. */
    public static String previewAssetPath(int n) {
        return "bulk/previews/p" + n + ".mp4";
    }

    /**
     * "Project N" (case-insensitive, extra spaces ok) se number — sirf
     * jab N known projects me ho, warna -1. Naam kuch aur ho (backup,
     * random folder) to -1: woh folder kabhi nahi chheda jata.
     */
    public static int parseDropFolderName(String name, Set<Integer> knownNumbers) {
        if (name == null || knownNumbers == null) return -1;
        Matcher m = DROP_NAME.matcher(name.trim());
        if (!m.matches()) return -1;
        final int n;
        try {
            n = Integer.parseInt(m.group(1));
        } catch (Exception e) {
            return -1;
        }
        if (n <= 0) return -1;
        return knownNumbers.contains(n) ? n : -1;
    }

    /**
     * Staging commit wala collision rule: naam free ho to wahi, warna
     * base_1.ext, base_2.ext ... (BulkStageActivity.uniqueFile jesa).
     */
    public static String uniqueDestName(String name, Set<String> takenNames) {
        if (name == null || name.isEmpty()) name = "image.jpg";
        if (takenNames == null || !takenNames.contains(name)) return name;
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        int i = 1;
        while (true) {
            String candidate = base + "_" + i + ext;
            if (!takenNames.contains(candidate)) return candidate;
            i++;
        }
    }

    /** Fetch plan ki ek entry: source naam -> MOVE (destName) ya DUPLICATE. */
    public static final class FetchAction {
        public static final int MOVE = 0;
        public static final int DUPLICATE = 1;
        public final String sourceName;
        public final int kind;
        public final String destName; // DUPLICATE par null
        FetchAction(String sourceName, int kind, String destName) {
            this.sourceName = sourceName;
            this.kind = kind;
            this.destName = destName;
        }
    }

    /**
     * Fetch plan (pure): source naam naam-order me diye jayen. Jiski
     * SHA-256 destination me pehle se ho -> DUPLICATE (holding me
     * jayegi). Warna MOVE — naam takraye (destination se ya is plan ke
     * pehle wale move se) to uniqueDestName se naya naam. Khaali hash
     * (hash fail) kabhi duplicate nahi gina jata. Inputs mutate nahi hote.
     */
    public static List<FetchAction> planFetch(List<String> sourceNamesInOrder,
                                              Map<String, String> sourceHashes,
                                              Set<String> destNames,
                                              Set<String> destHashes) {
        List<FetchAction> plan = new ArrayList<>();
        if (sourceNamesInOrder == null) return plan;
        Set<String> taken = new HashSet<>();
        if (destNames != null) taken.addAll(destNames);
        Set<String> knownHashes = destHashes != null ? destHashes : new HashSet<>();
        for (String name : sourceNamesInOrder) {
            String hash = sourceHashes != null ? sourceHashes.get(name) : null;
            if (hash != null && !hash.isEmpty() && knownHashes.contains(hash)) {
                plan.add(new FetchAction(name, FetchAction.DUPLICATE, null));
            } else {
                String dest = uniqueDestName(name, taken);
                taken.add(dest);
                plan.add(new FetchAction(name, FetchAction.MOVE, dest));
            }
        }
        return plan;
    }

    /** Recreate jin standard subfolders ko pakka karta hai (naam). */
    public static List<String> standardSubfolderNames(int n) {
        List<String> subs = new ArrayList<>();
        subs.add(BulkProjects.wallpapersDir(n).getName());
        subs.add(BulkProjects.mockupsDir(n).getName());
        subs.add(BulkProjects.readyDir(n).getName());
        return subs;
    }

    // ------------------------------------------------------------ known projects

    /** Known project numbers: projects.conf + disk scan (har screen wala source). */
    public static Set<Integer> knownNumbers() {
        Set<Integer> out = new TreeSet<>();
        try {
            List<BulkProjects.ProjectInfo> all = BulkProjects.mergeProjects(
                ProjectsLauncher.readProjectsList(), BulkProjects.scanDiskProjects());
            for (BulkProjects.ProjectInfo p : all) {
                if (p != null && p.number > 0) out.add(p.number);
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "knownNumbers fail: " + e.getMessage());
        }
        return out;
    }

    public static File rootDir() {
        return new File(BulkProjects.STORAGE_ROOT, ROOT_NAME);
    }

    // ------------------------------------------------------------ creation (create-only)

    /**
     * TermuxMod root + har known project ka drop folder banao (jo na ho).
     * @return int[]{created, alreadyPresent}
     */
    public static int[] ensureDropFolders(Set<Integer> knownNumbers) {
        int created = 0, present = 0;
        File root = rootDir();
        if (root.isDirectory()) {
            present++;
        } else if (root.mkdirs() || root.isDirectory()) {
            created++;
        }
        if (knownNumbers != null) {
            for (int n : knownNumbers) {
                if (n <= 0) continue;
                File dir = new File(root, dropFolderName(n));
                if (dir.isDirectory()) {
                    present++;
                } else if (dir.mkdirs() || dir.isDirectory()) {
                    created++;
                }
            }
        }
        return new int[]{created, present};
    }

    /** Ek project ka drop folder (ZIP se naya project ane par). */
    public static void ensureForProject(int n) {
        if (n <= 0) return;
        Set<Integer> one = new TreeSet<>();
        one.add(n);
        ensureDropFolders(one);
    }

    /**
     * Recreate Folders (Permissions): drop folders + mojooda project
     * folders ke standard subfolders — sirf create, kabhi delete nahi.
     * @return int[]{created, alreadyPresent}
     */
    public static int[] recreateAll(Set<Integer> knownNumbers) {
        int[] counts = ensureDropFolders(knownNumbers);
        int created = counts[0], present = counts[1];
        Set<Integer> candidates = new TreeSet<>();
        if (knownNumbers != null) candidates.addAll(knownNumbers);
        try {
            candidates.addAll(BulkProjects.scanDiskProjects().keySet());
        } catch (Exception ignored) {}
        for (int n : candidates) {
            if (n <= 0) continue;
            File projectDir = BulkProjects.projectDir(n);
            if (!projectDir.isDirectory()) continue; // sirf mojooda project folders
            for (String sub : standardSubfolderNames(n)) {
                File dir = new File(projectDir, sub);
                if (dir.isDirectory()) {
                    present++;
                } else if (dir.mkdirs() || dir.isDirectory()) {
                    created++;
                }
            }
        }
        return new int[]{created, present};
    }

    // ------------------------------------------------------------ fetch (auto-import)

    /**
     * Har TermuxMod/Project N se images apne project ke wallpapers
     * folder me move karo. Duplicate (SHA-256 destination me mojood) ->
     * Duplicates holding. Har us project ke liye jisne files payin, ek
     * BulkLog line. @return project me move hui kul files.
     */
    public static int fetchAll(Context ctx, Set<Integer> knownNumbers) {
        int totalMoved = 0;
        File root = rootDir();
        if (!root.isDirectory() || knownNumbers == null || knownNumbers.isEmpty()) {
            return 0;
        }
        // Asal drop dirs pehchano (naam case-insensitive mukhtalif ho sakta hai)
        Map<Integer, File> dropDirs = new HashMap<>();
        File[] children = root.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!child.isDirectory()) continue;
                int n = parseDropFolderName(child.getName(), knownNumbers);
                if (n > 0 && !dropDirs.containsKey(n)) dropDirs.put(n, child);
            }
        }
        List<Integer> order = new ArrayList<>(dropDirs.keySet());
        Collections.sort(order);
        long anchor = System.currentTimeMillis() / 1000L;
        for (int n : order) {
            try {
                totalMoved += fetchProject(ctx, n, dropDirs.get(n), anchor);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Fetch fail Project " + n + ": " + e.getMessage());
            }
        }
        return totalMoved;
    }

    private static int fetchProject(Context ctx, int n, File dropDir, long anchor) {
        File[] files = dropDir.listFiles();
        if (files == null || files.length == 0) return 0;
        List<File> sources = new ArrayList<>();
        for (File f : files) {
            // v133: preview video kabhi import nahi hoti (wese bhi
            // video countable nahi — ye check naam se bhi pakka hai)
            if (f.getName().equals(previewFileName(n))) continue;
            if (f.isFile() && BulkProjects.isCountableWallpaper(f.getName())) {
                sources.add(f);
            }
        }
        if (sources.isEmpty()) return 0;
        sources.sort((a, b) -> a.getName().compareTo(b.getName()));

        File destDir = BulkProjects.wallpapersDir(n);
        if (!destDir.exists()) destDir.mkdirs();
        if (!destDir.isDirectory()) return 0;

        Set<String> destNames = new HashSet<>(BulkProjects.listFileNames(destDir));
        Set<String> destHashes = new HashSet<>();
        for (String destName : destNames) {
            if (!BulkProjects.isCountableWallpaper(destName)) continue;
            String h = hashOf(new File(destDir, destName));
            if (!h.isEmpty()) destHashes.add(h);
        }

        List<String> names = new ArrayList<>();
        Map<String, String> sourceHashes = new HashMap<>();
        Map<String, File> byName = new HashMap<>();
        for (File f : sources) {
            names.add(f.getName());
            byName.put(f.getName(), f);
            sourceHashes.put(f.getName(), hashOf(f));
        }

        List<FetchAction> plan = planFetch(names, sourceHashes, destNames, destHashes);
        int moved = 0;
        long step = 0;
        for (FetchAction action : plan) {
            File src = byName.get(action.sourceName);
            if (src == null || !src.exists()) continue;
            if (action.kind == FetchAction.DUPLICATE) {
                // Holding me copy + index; copy pakki ho tab hi source delete
                boolean held = false;
                FileInputStream in = null;
                try {
                    in = new FileInputStream(src);
                    held = BulkDuplicatesStore.addDuplicate(ctx, in,
                        action.sourceName, sourceHashes.get(action.sourceName),
                        System.currentTimeMillis()) != null;
                } catch (Exception e) {
                    Logger.logError(LOG_TAG, "Duplicate divert fail: " + e.getMessage());
                } finally {
                    try { if (in != null) in.close(); } catch (Exception ignored) {}
                }
                if (held) {
                    //noinspection ResultOfMethodCallIgnored
                    src.delete();
                }
            } else {
                File dest = new File(destDir, action.destName);
                if (moveFile(src, dest)) {
                    // Staging commit jesa +1s stagger (order mehfooz rahe)
                    //noinspection ResultOfMethodCallIgnored
                    dest.setLastModified((anchor + step) * 1000L);
                    step++;
                    moved++;
                }
            }
        }
        if (moved > 0) {
            BulkLog.add("Project " + n + " <- " + moved + " wallpapers (manual move)");
        }
        return moved;
    }

    /** renameTo pehle; fail ho to copy + size verify + source delete. */
    private static boolean moveFile(File src, File dest) {
        try {
            if (src.renameTo(dest)) return true;
        } catch (Exception ignored) {}
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            out.flush();
            if (dest.length() != src.length()) {
                //noinspection ResultOfMethodCallIgnored
                dest.delete();
                return false;
            }
            //noinspection ResultOfMethodCallIgnored
            src.delete();
            return true;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Move fail " + src.getName() + ": " + e.getMessage());
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private static String hashOf(File f) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            return BulkDuplicatesStore.sha256Hex(in);
        } catch (Exception e) {
            return "";
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * v133: har drop folder me us project ki ~2s preview video rakho
     * (gallery/file manager me folder ki pehchan ke liye) — SIRF jab
     * file wahan na ho; mojooda file kabhi overwrite nahi hoti.
     * Asset na mile (project bundled na ho) to khamoshi se skip.
     */
    public static void ensurePreviewVideos(Context ctx, Set<Integer> knownNumbers) {
        if (ctx == null || knownNumbers == null) return;
        File root = rootDir();
        for (int n : knownNumbers) {
            if (n <= 0) continue;
            File dest = new File(new File(root, dropFolderName(n)), previewFileName(n));
            if (dest.exists()) continue;
            File parent = dest.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            try (InputStream in = ctx.getAssets().open(previewAssetPath(n));
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[1 << 16];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            } catch (Exception e) {
                // Bundled preview na ho to koi baat nahi — video sirf pehchan ke liye hai
                //noinspection ResultOfMethodCallIgnored
                dest.delete();
            }
        }
    }

    /**
     * Ek pass: (auto ON ho to) drop folders ensure + preview
     * videos, phir fetch. Dashboard / Add / Projects ke khulne par
     * background thread se chalaya jata hai.
     * v134: DEFAULT MANUAL hai — auto toggles OFF hon to missing
     * folders/videos khud NAHI bante (owner jo delete kare wo
     * delete hi rehta hai); fetch sirf mojooda folders par chalta
     * hai. Toggles Permissions -> Recreate Folders ki settings me.
     * @return project me move hui kul files
     */
    public static int syncNow(Context ctx) {
        Set<Integer> known = knownNumbers();
        if (known.isEmpty()) return 0;
        if (BulkRecreateSettings.mayCreateFolders(BulkRecreateSettings.foldersAuto(ctx))) {
            ensureDropFolders(known);
        }
        if (BulkRecreateSettings.mayCopyVideos(BulkRecreateSettings.videosAuto(ctx))) {
            ensurePreviewVideos(ctx, known);
        }
        int fetched = fetchAll(ctx, known);
        // v134: output mirror HAR pass me (recreate toggles se
        // alag — ye outputs hain, drop folders nahi)
        syncAllMirrors(ctx, known);
        return fetched;
    }

    // ------------------------------------------------------------ output mirror (v134)

    /** Har known project ke outputs ka mirror drop folder ke andar. */
    public static void syncAllMirrors(Context ctx, Set<Integer> knownNumbers) {
        if (ctx == null || knownNumbers == null) return;
        for (int n : knownNumbers) {
            if (n > 0) syncMirrorForProject(ctx, n);
        }
    }

    /**
     * Ek project: <N>_Mockups -> "Project N/Mockups" or
     * <N>_Ready_Wallpapers -> "Project N/Ready Wallpapers" ka true
     * mirror (naya/badla copy, ghayab delete — sirf mirror side).
     * Drop folder (Project N) mojood na ho to skip: is sync se koi
     * folder khud nahi banta (manual mode ki izzat). Subfolder tab
     * hi banta hai jab kuch copy karna ho.
     * @return copy hui kul files
     */
    public static int syncMirrorForProject(Context ctx, int n) {
        if (ctx == null || n <= 0) return 0;
        File dropDir = new File(rootDir(), dropFolderName(n));
        if (!dropDir.isDirectory()) return 0;
        int copied = 0;
        copied += syncOneMirror(ctx, n, BulkProjects.mockupsDir(n),
            new File(dropDir, BulkMirror.MOCKUPS_SUB), true);
        copied += syncOneMirror(ctx, n, BulkProjects.readyDir(n),
            new File(dropDir, BulkMirror.READY_SUB), false);
        return copied;
    }

    private static int syncOneMirror(Context ctx, int n, File srcDir, File dstDir,
                                     boolean mockups) {
        Map<String, String> src = mirrorSignatures(srcDir);
        Map<String, String> dst = mirrorSignatures(dstDir);
        BulkMirror.Plan plan = BulkMirror.planSync(src, dst);
        if (plan.isEmpty()) return 0;
        if (!plan.toCopy.isEmpty() && !dstDir.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            dstDir.mkdirs();
        }
        int copied = 0;
        for (String name : plan.toCopy) {
            File s = new File(srcDir, name);
            File d = new File(dstDir, name);
            if (copyFileVerified(s, d)) {
                // Mirror file par source ka mtime — agli pass me
                // signature mil jata hai, dobara copy nahi hoti
                //noinspection ResultOfMethodCallIgnored
                d.setLastModified(s.lastModified());
                copied++;
            }
        }
        for (String name : plan.toDelete) {
            // Delete SIRF mirror side par — project side mehfooz
            //noinspection ResultOfMethodCallIgnored
            new File(dstDir, name).delete();
        }
        if (copied > 0) {
            BulkLog.add(BulkStrings.format(ctx,
                mockups ? BulkStrings.LOG_MIRROR_MOCKUPS : BulkStrings.LOG_MIRROR_READY,
                n, copied));
        }
        return copied;
    }

    /** Dir ki seedhi files (directories/hidden nahi) ke signatures. */
    private static Map<String, String> mirrorSignatures(File dir) {
        Map<String, String> out = new HashMap<>();
        if (dir == null || !dir.isDirectory()) return out;
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            if (!f.isFile()) continue;
            if (f.getName().startsWith(".")) continue;
            out.put(f.getName(), BulkMirror.signature(f.length(), f.lastModified()));
        }
        return out;
    }

    /** Copy + size verify (moveFile wala hi pattern, delete baghair). */
    private static boolean copyFileVerified(File src, File dest) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            out.flush();
            if (dest.length() != src.length()) {
                //noinspection ResultOfMethodCallIgnored
                dest.delete();
                return false;
            }
            return true;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Mirror copy fail " + src.getName() + ": " + e.getMessage());
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }
}
