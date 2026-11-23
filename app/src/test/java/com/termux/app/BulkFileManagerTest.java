package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

/** Termux Mod (v127) — Export Project ki folder copy (pure hisaab) ki tests. */
public class BulkFileManagerTest {

    private static void write(File f, String text) throws Exception {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes("UTF-8"));
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }

    @Test
    public void copyFolderCopiesTreeWithContents() throws Exception {
        File base = Files.createTempDirectory("bulkfm").toFile();
        File src = new File(base, "mockup_project_9");
        write(new File(src, "w9.py"), "print('x')");
        write(new File(src, "9_wallpapers/a.jpg"), "JPEGDATA");
        write(new File(src, "backgrounds/bg.png"), "PNGDATA");
        File dst = new File(base, "out/mockup_project_9");

        final int[] lastPct = {-1};
        boolean ok = BulkFileManager.copyFolder(src, dst, pct -> lastPct[0] = pct);

        assertTrue(ok);
        assertEquals("print('x')", read(new File(dst, "w9.py")));
        assertEquals("JPEGDATA", read(new File(dst, "9_wallpapers/a.jpg")));
        assertEquals("PNGDATA", read(new File(dst, "backgrounds/bg.png")));
        assertEquals(100, lastPct[0]);
    }

    @Test
    public void copyFolderRejectsBadSource() throws Exception {
        File base = Files.createTempDirectory("bulkfm2").toFile();
        assertFalse(BulkFileManager.copyFolder(new File(base, "nope"), new File(base, "out"), null));
        assertFalse(BulkFileManager.copyFolder(null, new File(base, "out"), null));
    }

    @Test
    public void deleteRecursiveRemovesTree() throws Exception {
        File base = Files.createTempDirectory("bulkfm3").toFile();
        File tree = new File(base, "tree");
        write(new File(tree, "sub/f.txt"), "x");
        BulkFileManager.deleteRecursive(tree);
        assertFalse(tree.exists());
    }

    @Test
    public void exportRootIsSharedDownload() {
        assertEquals("/storage/emulated/0/Download/TermuxMod",
            BulkFileManager.EXPORT_ROOT.getAbsolutePath());
    }
}
