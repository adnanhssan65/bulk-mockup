package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.termux.shared.termux.TermuxConstants;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Termux Mod (v128) — live popup run (DEFECT A fix) ke pure helpers:
 * Termux environment tayyari aur output tail katai.
 */
public class BulkRunHelperTest {

    @Test
    public void runEnvironmentSetsTermuxPaths() {
        Map<String, String> base = new HashMap<>();
        base.put("PATH", "/system/bin");
        base.put("CUSTOM", "rakho");
        Map<String, String> env = BulkRunHelper.runEnvironment(base);
        String prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH;
        assertEquals(prefix, env.get("PREFIX"));
        assertEquals(TermuxConstants.TERMUX_HOME_DIR_PATH, env.get("HOME"));
        assertEquals(prefix + "/bin:/system/bin", env.get("PATH"));
        assertEquals(prefix + "/lib", env.get("LD_LIBRARY_PATH"));
        assertEquals(prefix + "/tmp", env.get("TMPDIR"));
        assertEquals("rakho", env.get("CUSTOM")); // base ki cheezein mehfooz
    }

    @Test
    public void runEnvironmentHandlesMissingPath() {
        Map<String, String> env = BulkRunHelper.runEnvironment(new HashMap<>());
        assertEquals(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin", env.get("PATH"));
        Map<String, String> envNull = BulkRunHelper.runEnvironment(null);
        assertEquals(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin", envNull.get("PATH"));
    }

    @Test
    public void runEnvironmentSetsPythonUnbuffered() {
        // v131 (Item 3): pipe par Python block-buffer na ho
        Map<String, String> env = BulkRunHelper.runEnvironment(new HashMap<>());
        assertEquals("1", env.get("PYTHONUNBUFFERED"));
    }

    @Test
    public void trimTailOnlyTrimsPastThreshold() {
        // v131 (Item 3): katai amortized hai — 12000 tak koi katai nahi
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11_999; i++) sb.append('a');
        BulkRunHelper.trimTail(sb);
        assertEquals(11_999, sb.length());
        sb.append('b');
        BulkRunHelper.trimTail(sb);
        assertEquals(12_000, sb.length());
        sb.append('c');
        BulkRunHelper.trimTail(sb);
        // Hadd se barhte hi aakhri 6000 reh jate hain
        assertEquals(6_000, sb.length());
        assertTrue(sb.toString().endsWith("c"));
    }

    @Test
    public void keepTailKeepsEndOnly() {
        assertEquals("abc", BulkRunHelper.keepTail("abc", 10));
        assertEquals("def", BulkRunHelper.keepTail("abcdef", 3));
        assertEquals("", BulkRunHelper.keepTail(null, 5));
        assertEquals("", BulkRunHelper.keepTail("abc", 0));
        assertEquals("abc", BulkRunHelper.keepTail("abc", 3));
    }
}
