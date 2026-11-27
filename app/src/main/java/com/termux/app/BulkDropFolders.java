package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

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

    /**
     * v136: TermuxMod root + har known project ka drop folder us ke
     * output subfolders (Mockups / Ready Wallpapers) ke SATH banao.
     * Sirf do jagah istemal hota hai: v136 ki pehli-dafa setup
     * (BulkHomeMigration) or Recreate Folders (manual) — aam app
     * flow me kabhi nahi.
     * @return int[]{created, alreadyPresent}
     */
    public static int[] ensureDropFoldersFull(Set<Integer> knownNumbers) {
        int[] counts = ensureDropFolders(knownNumbers);
        int created = counts[0], present = counts[1];
        if (knownNumbers != null) {
            File root = rootDir();
            for (int n : knownNumbers) {
                if (n <= 0) continue;
                File dir = new File(root, dropFolderName(n));
                String[] subs = {BulkMirror.PAIRS_SUB, BulkMirror.BATCHES_SUB};
                for (String sub : subs) {
                    File s = new File(dir, sub);
                    if (s.isDirectory()) {
                        present++;
                    } else if (s.mkdirs() || s.isDirectory()) {
                        created++;
                    }
                }
            }
        }
        return new int[]{created, present};
    }

    /**
     * v155 (Auto Recreate — owner ka hukm: "folder ni tu ye
     * bnaie khud", DEFAULT par): EK project ka poora drop set
     * (root + Project N + Pairs + Batches) jo na ho bana do —
     * sirf create, kabhi delete nahi. Run / scan / restore se
     * pehle har jagah yahi check lagta hai, taake ghayab folder
     * ki wajah se copies ya registration khamoshi se skip na
     * ho (v154 wala phone masla).
     * @return int[]{created, alreadyPresent}
     */
    public static int[] ensureProjectFolders(int n) {
        if (n <= 0) return new int[]{0, 0};
        Set<Integer> one = new TreeSet<>();
        one.add(n);
        return ensureDropFoldersFull(one);
    }

    /**
     * Ek project ka drop folder (ZIP se naya project ane par).
     * v138: poora set (Mockups / Ready Wallpapers ke sath) banta
     * hai or provision record bhi lag jata hai — sync ki provision
     * pass ise dobara kabhi nahi chhedti.
     */
    public static void ensureForProject(Context ctx, int n) {
        if (n <= 0) return;
        Set<Integer> one = new TreeSet<>();
        one.add(n);
        ensureDropFoldersFull(one);
        markProvisioned(ctx, n);
    }

    // ------------------------------------------------------------ v138: one-time provision

    private static final String PREFS = "bulk_mockup";
    private static final String KEY_PROVISION_PREFIX = "dropfolder_provisioned_";

    /**
     * v138 (PURE): drop folder ki one-time provision SIRF tab jab
     * us project ka provision record mojood na ho. Record lag jane
     * ke baad app wo folder dobara KABHI nahi banati — owner delete
     * kare to delete hi rehta hai (wapas lane ka wahid rasta
     * Permissions -> Recreate Folders hai, jo is record ko nahi
     * chhedta).
     */
    public static boolean shouldProvision(boolean provisionRecorded) {
        return !provisionRecorded;
    }

    /** Project ka provision record laga hai? */
    public static boolean isProvisionRecorded(Context c, int n) {
        if (c == null || n <= 0) return false;
        try {
            return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_PROVISION_PREFIX + n, false);
        } catch (Exception e) {
            return false;
        }
    }

    private static void markProvisioned(Context c, int n) {
        if (c == null || n <= 0) return;
        try {
            SharedPreferences.Editor e = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            e.putBoolean(KEY_PROVISION_PREFIX + n, true).apply();
        } catch (Exception ignored) {}
    }

    /**
     * v138 — Remove Project par provision record saaf karo:
     * project dobara add ho to us ka TermuxMod folder phir se
     * ek dafa provision ho sakta hai (warna purana record use
     * hamesha ke liye rok deta). Folder khud is se kabhi nahi
     * hatta — TermuxMod/Project N or us ki images Remove
     * Project me hamesha mehfooz rehti hain.
     */
    public static void clearProvisionRecord(Context c, int n) {
        if (c == null || n <= 0) return;
        try {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(KEY_PROVISION_PREFIX + n).apply();
        } catch (Exception ignored) {}
    }

    /** Bundled project numbers: assets ke "bulk/projects" zips se. */
    public static Set<Integer> bundledNumbers(Context ctx) {
        Set<Integer> out = new TreeSet<>();
        if (ctx == null) return out;
        try {
            String[] names = ctx.getAssets().list(BulkProjectBundles.ASSETS_DIR);
            if (names != null) {
                for (String name : names) {
                    String base = name;
                    if (base.toLowerCase().endsWith(".zip")) {
                        base = base.substring(0, base.length() - 4);
                    }
                    int n = BulkProjects.parseFolderNumber(base);
                    if (n > 0) out.add(n);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * v138: v136 ki pehli setup (dropfolders_initialized) BUNDLED
     * projects ke folders bana chuki thi — un ke provision records
     * pehle se lagao, taake owner ka delete kiya hua bundled
     * folder provision pass me dobara na ban jaye. Sirf record
     * lagta hai; is step me folder kabhi nahi banta. ZIP-added
     * projects is seeding me NAHI ate (un ka record asal
     * provision ya ZIP-add se hi lagta hai) — yahi wajah hai ke
     * purani ZIP-add wale 15/16/17, jin ke folders v136 init me
     * nahi bane thay, pehli v138 pass me provision ho jate hain.
     */
    public static void seedProvisionRecordsForBundled(Context ctx, Set<Integer> numbers) {
        if (ctx == null || numbers == null) return;
        if (!BulkHomeMigration.isDropInitialized(ctx)) return;
        Set<Integer> bundled = bundledNumbers(ctx);
        for (int n : numbers) {
            if (n > 0 && bundled.contains(n) && !isProvisionRecorded(ctx, n)) {
                markProvisioned(ctx, n);
            }
        }
    }

    /**
     * v138: jin discovered projects ka provision record nahi, un
     * ke liye TermuxMod/Project N (+ Mockups / Ready Wallpapers)
     * EK dafa banao + record lagao. Folder pehle se mojood ho
     * (ZIP-add / Recreate se bana ho) to sirf record lagta hai —
     * kuch dobara nahi banta. Folder ban hi na sake (storage
     * fail) to record NAHI lagta; agli pass me dobara koshish
     * hogi. Record wala project is pass me kabhi nahi chheda
     * jata — owner ka delete kiya folder waisa hi rehta hai.
     * @return is pass me asal me bane (pehle ghayab) folders
     */
    public static int provisionMissing(Context ctx, Set<Integer> numbers) {
        if (ctx == null || numbers == null) return 0;
        int created = 0;
        for (int n : numbers) {
            if (n <= 0 || !shouldProvision(isProvisionRecorded(ctx, n))) continue;
            File dir = new File(rootDir(), dropFolderName(n));
            boolean existed = dir.isDirectory();
            try {
                Set<Integer> one = new TreeSet<>();
                one.add(n);
                ensureDropFoldersFull(one);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Provision fail Project " + n + ": " + e.getMessage());
                continue;
            }
            if (dir.isDirectory()) {
                if (!existed) created++;
                markProvisioned(ctx, n);
            }
        }
        return created;
    }

    /**
     * Recreate Folders (Permissions): drop folders ka poora set
     * (root + har project ka Project N / Pairs / Batches) —
     * sirf create, kabhi delete nahi.
     * v155 COUNT FIX: pehle yahan ek dusra loop TermuxMod
     * folders ke andar ENGINE-naam wale subfolders (N_wallpapers
     * waghera) bhi gin leta tha — wahi "121 already present"
     * wali phuli hui ghalat count thi (phone par folders asal
     * me ghayab thay, number kuch aur kehta tha). Ab count
     * SIRF wahi hai jo owner ko dikhta hai: is call ke
     * mkdirs/isDirectory ke asal nataij.
     * @return int[]{created, alreadyPresent}
     */
    public static int[] recreateAll(Set<Integer> knownNumbers) {
        return ensureDropFoldersFull(knownNumbers);
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
            File dropDir = new File(root, dropFolderName(n));
            // v136: video SIRF mojooda drop folder me — folder khud
            // kabhi nahi banta (owner ka delete kiya hua waisa hi)
            if (!dropDir.isDirectory()) continue;
            File dest = new File(dropDir, previewFileName(n));
            if (dest.exists()) continue;
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
     * Ek pass: folder check + preview videos (sirf mojooda
     * folders me), phir fetch + registration. Dashboard / Add /
     * Projects ke khulne par background thread se chalaya
     * jata hai.
     * v155 LIFECYCLE (owner ka naya final rule — v136/v138 wala
     * "delete kiya folder wapas nahi banta" usool khatam):
     * AUTO RECREATE DEFAULT hai — jahan folder chahiye wahan
     * pehle check hota hai, na ho to app KHUD bana leti hai
     * (start/sync, run, scan — har jagah). Is liye ye pass ab
     * BILA SHART tamam known projects ka poora drop set
     * (Project N + Pairs + Batches) ensure karti hai; provision
     * records ab folder banne ko NAHI rokte (wo sirf purani
     * pehchan ke liye rehte hain). Sirf create hota hai —
     * delete kabhi nahi.
     * @return project me move hui kul files
     */
    public static int syncNow(Context ctx) {
        Set<Integer> known = knownNumbers();
        if (known.isEmpty()) return 0;
        // v155: Auto Recreate — ghayab drop folders khud bante
        // hain (provision record ka gate khatam)
        ensureDropFoldersFull(known);
        // v135: Preview Video ka master switch (Control Panel) OFF ho
        // to video kabhi copy nahi hoti — Auto ON ho tab bhi nahi.
        // v136: videos bhi sirf MOJOOD drop folders me jati hain.
        if (BulkRecreateSettings.mayCopyVideos(BulkRecreateSettings.videosAuto(ctx),
                BulkRecreateSettings.videosEnabled(ctx))) {
            ensurePreviewVideos(ctx, known);
        }
        int fetched = fetchAll(ctx, known);
        // v154: output mirror BAND — runners apni gallery copies
        // khud Pairs folder me dete hain. Ab har pass me sirf
        // PLAIN REGISTRATION hota hai (BulkMediaRegister: Pairs
        // ki sirf nayi files, Auto Scan switch ke neeche).
        BulkMediaRegister.registerAllProjects(ctx, known, false);
        return fetched;
    }

    // ------------------------------------------------------------ v154: manual gallery scan

    /**
     * v154 — More Settings ke "Gallery Scan" button ka ek-dafa
     * ka manual pass: tamam known projects ki Pairs files jo
     * abhi register nahi huin, ek dafa register ho jati hain.
     * Auto Scan OFF ho tab bhi chalta hai (yehi is ka maqsad
     * hai). Caller background thread par bulaye.
     * @return register hui kul files
     */
    public static int galleryScanNow(Context ctx) {
        if (ctx == null) return 0;
        return BulkMediaRegister.registerAllProjects(ctx, knownNumbers(), true);
    }
}
