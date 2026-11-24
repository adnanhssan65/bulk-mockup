package com.termux.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Termux Mod (v128) — storage access ka EK shared faisla.
 *
 * v127 tak teen screens (Add Wallpapers / Stage / Add Project) sirf
 * All-Files-Access (isExternalStorageManager) check karti theen, jabke
 * Permissions screen asal file probe se tasdeeq karti thi. Vivo par
 * runtime "Files" permission mili hui thi aur probe pass tha (Permissions
 * screen sab ✔), lekin All-Files-Access off hone se baqi screens
 * "Storage permission chahiye." par atak jati theen — aisa loop jahan se
 * nikalna mumkin nahi tha. Ab tamam jagah yahi ek helper istemal hota
 * hai: koi BHI ek rasta khula ho (all-files, legacy runtime permission,
 * ya asal probe) to access hai — Permissions screen wala mayaar hi
 * asal mayaar hai.
 */
public final class BulkStorageAccess {

    private BulkStorageAccess() {}

    /**
     * Pure faisla — device ke bager bhi test ho sakta hai.
     * Koi ek bhi rasta khula ho to access hai.
     */
    public static boolean decide(boolean allFilesAccess, boolean legacyWriteGranted,
                                 boolean legacyReadGranted, boolean probeOk) {
        return allFilesAccess || legacyWriteGranted || legacyReadGranted || probeOk;
    }

    /**
     * v133 — Move ke originals ka delete faisla (pure): All Files
     * Access (hasAccess) ho or asal path maloom ho to original
     * seedha java.io.File se delete hoga — system ka "Allow delete"
     * dialog NAHI ayega. Warna purana MediaStore wala rasta
     * (createDeleteRequest) fallback hai.
     */
    public static boolean shouldDeleteDirectly(boolean hasAccess, String path) {
        return hasAccess && path != null && !path.isEmpty();
    }

    /** Device par asal check: pehle flags, akhir me asal file probe. */
    public static boolean hasAccess(Context context) {
        boolean allFiles = false;
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                allFiles = Environment.isExternalStorageManager();
            } catch (Exception e) {
                // Purani check wali rawaiya: is call ke fail hone par
                // user ko block nahi karna.
                allFiles = true;
            }
        }
        boolean write = hasRuntimePermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean read = hasRuntimePermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (decide(allFiles, write, read, false)) return true;
        return probeStorage();
    }

    /**
     * Storage ka ASAL test: /storage/emulated/0 me file likho, parho,
     * delete karo. Permissions screen bhi yahi istemal karti hai —
     * probe ki sirf EK hi copy hai taake dono jagah kabhi alag na hon.
     */
    public static boolean probeStorage() {
        try {
            File dir = Environment.getExternalStorageDirectory();
            File test = new File(dir, ".bulk_mockup_perm_test");
            try (FileOutputStream out = new FileOutputStream(test)) {
                out.write(42);
            }
            boolean ok;
            try (FileInputStream in = new FileInputStream(test)) {
                ok = in.read() == 42;
            }
            //noinspection ResultOfMethodCallIgnored
            test.delete();
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasRuntimePermission(Context context, String permission) {
        try {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }
}
