package com.termux.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Termux Mod — Permissions Terminal ke steps (v126).
 *
 * Agar pehli-dafa wale permission popups kisi wajah se kaam na karen to
 * user Permissions screen ke "Terminal" option se ye commands EK EK
 * KARKE terminal me chala sakta hai. Tartib wohi hai jo first-run setup
 * ki hai — sab se pehle storage:
 *
 *  1. termux-setup-storage — storage permission mangta hai or
 *     ~/storage links banata hai (script TermuxActivity ko
 *     ACTION_REQUEST_PERMISSIONS bhejti hai — bilkul wohi flow jo app
 *     khud istemal karta hai: PermissionUtils legacy-or-manage).
 *  2. ls /storage/emulated/0 — check: storage ki list ayi to permission
 *     kaam kar rahi hai.
 *  3. termux-wake-lock — app ki apni built-in command (BulkApiServer);
 *     har project run yehi pehle chalata hai.
 *  4. termux-wake-unlock — wake lock wapas chhorna.
 *
 * Ye class pure Java hai — unit tests ke liye.
 */
public final class BulkPermissionCommands {

    /** Ek step: terminal command + strings-table keys (title/hint). */
    public static final class Step {
        public final String command;
        public final String titleKey;
        public final String hintKey;

        public Step(String command, String titleKey, String hintKey) {
            this.command = command;
            this.titleKey = titleKey;
            this.hintKey = hintKey;
        }
    }

    private static final List<Step> STEPS = Collections.unmodifiableList(Arrays.asList(
        new Step("termux-setup-storage",
            BulkStrings.CMD_STORAGE_TITLE, BulkStrings.CMD_STORAGE_HINT),
        new Step("ls /storage/emulated/0",
            BulkStrings.CMD_STORAGE_CHECK_TITLE, BulkStrings.CMD_STORAGE_CHECK_HINT),
        new Step("termux-wake-lock",
            BulkStrings.CMD_WAKELOCK_TITLE, BulkStrings.CMD_WAKELOCK_HINT),
        new Step("termux-wake-unlock",
            BulkStrings.CMD_WAKEUNLOCK_TITLE, BulkStrings.CMD_WAKEUNLOCK_HINT)
    ));

    private BulkPermissionCommands() {}

    /** Steps hamesha isi tartib me — pehla hamesha termux-setup-storage. */
    public static List<Step> steps() {
        return STEPS;
    }
}
