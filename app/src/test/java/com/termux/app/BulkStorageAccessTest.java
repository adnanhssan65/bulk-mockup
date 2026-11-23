package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Termux Mod (v128) — shared storage faisle (decide) ki tests.
 * v127 ka bug: teen screens sirf All-Files-Access mangti theen, jabke
 * asal kaam runtime permission / probe se bhi ho jata tha — user
 * Permissions screen par sab ✔ dekh kar bhi atak jata tha.
 */
public class BulkStorageAccessTest {

    @Test
    public void nothingGrantedNoAccess() {
        assertFalse(BulkStorageAccess.decide(false, false, false, false));
    }

    @Test
    public void allFilesAccessAloneIsEnough() {
        assertTrue(BulkStorageAccess.decide(true, false, false, false));
    }

    @Test
    public void legacyWriteAloneIsEnough() {
        // Vivo wala asal case: All-Files-Access off, runtime "Files"
        // (WRITE) permission mili hui — access hona chahiye.
        assertTrue(BulkStorageAccess.decide(false, true, false, false));
    }

    @Test
    public void legacyReadAloneIsEnough() {
        assertTrue(BulkStorageAccess.decide(false, false, true, false));
    }

    @Test
    public void probeAloneIsEnough() {
        // Koi flag na mile lekin asal file likhna/parhna ho jaye —
        // Permissions screen ke mayaar par access hai.
        assertTrue(BulkStorageAccess.decide(false, false, false, true));
    }

    @Test
    public void anyCombinationIsEnough() {
        assertTrue(BulkStorageAccess.decide(true, true, true, true));
        assertTrue(BulkStorageAccess.decide(false, true, false, true));
        assertTrue(BulkStorageAccess.decide(true, false, false, false));
    }
}
