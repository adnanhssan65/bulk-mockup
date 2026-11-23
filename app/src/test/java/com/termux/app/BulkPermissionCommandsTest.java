package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Termux Mod (v126) — Permissions Terminal ke steps: tartib (storage pehle), saaf ek-line commands, or har step ke labels dono zubano me. */
public class BulkPermissionCommandsTest {

    @Test
    public void stepsInSetupOrder() {
        List<BulkPermissionCommands.Step> steps = BulkPermissionCommands.steps();
        assertEquals(4, steps.size());
        // First-run setup ki tartib: storage sab se pehle
        assertEquals("termux-setup-storage", steps.get(0).command);
        assertEquals("ls /storage/emulated/0", steps.get(1).command);
        assertEquals("termux-wake-lock", steps.get(2).command);
        assertEquals("termux-wake-unlock", steps.get(3).command);
    }

    @Test
    public void commandsAreSingleLineAndUnique() {
        Set<String> seen = new HashSet<>();
        for (BulkPermissionCommands.Step s : BulkPermissionCommands.steps()) {
            assertFalse("command khaali", s.command.trim().isEmpty());
            assertFalse("command ek hi line ki: " + s.command,
                s.command.contains("\n") || s.command.contains("\r"));
            assertEquals("command ke aas paas spaces nahi", s.command, s.command.trim());
            assertTrue("duplicate command: " + s.command, seen.add(s.command));
        }
    }

    @Test
    public void everyStepLabelsExistInBothLanguages() {
        for (BulkPermissionCommands.Step s : BulkPermissionCommands.steps()) {
            assertTrue("title key dono zubano me: " + s.titleKey,
                BulkStrings.hasBothLanguages(s.titleKey));
            assertTrue("hint key dono zubano me: " + s.hintKey,
                BulkStrings.hasBothLanguages(s.hintKey));
        }
    }
}
