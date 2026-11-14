package com.termux.app;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.PowerManager;

import com.termux.shared.logger.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Bulk Mockup ka built-in API server (Termux:API app ka replacement — sirf EK APK).
 *
 * $PREFIX/bin ke wrapper scripts (termux-media-scan / termux-wake-lock /
 * termux-wake-unlock) ek unix socket ke zariye is server se baat karte hain.
 * Socket Linux ke ABSTRACT namespace me hai (naam: com.termux.bulkapi) —
 * filesystem socket ke liye darkaar hidden API (LocalSocketImpl) public SDK
 * me mojood nahi, is liye yehi wahid public-API tareeqa hai. Commands bilkul
 * be-zarar hain (di gayi file ko media scan karna, wake lock lena/chorna),
 * is liye abstract naam ka chhota sa tradeoff qabool hai.
 *
 * Protocol (ek line = ek command, har command ka jawab ek line "OK"/"ERR ..."):
 *   SCAN <path>    — ek file ko MediaScanner se scan karo, mukammal hone tak ruko
 *   SCANR <path>   — poora folder recursively scan karo (mtime order me)
 *   WAKELOCK       — wake lock lo (script ke unlock tak)
 *   WAKEUNLOCK     — wake lock chhor do
 *
 * Connections ek ke baad ek handle hote hain taake scan order hamesha
 * wahi rahe jo script ne bheja tha (gallery order ke liye zaroori hai).
 */
public final class BulkApiServer {

    private static final String LOG_TAG = "BulkApiServer";
    public static final String SOCKET_NAME = "com.termux.bulkapi";

    private static volatile boolean sRunning = false;
    private static LocalServerSocket sServer;
    private static Context sContext;
    private static PowerManager.WakeLock sWakeLock;

    private BulkApiServer() {}

    public static String socketName() {
        return SOCKET_NAME;
    }

    public static synchronized void start(Context context) {
        if (sRunning) return;
        sRunning = true;
        sContext = context.getApplicationContext();
        new Thread(BulkApiServer::serveLoop, "BulkApiServer").start();
        Logger.logInfo(LOG_TAG, "BulkApiServer starting at " + socketName());
    }

    public static synchronized void stop() {
        sRunning = false;
        releaseWakeLock();
        try { if (sServer != null) sServer.close(); } catch (Exception ignored) {}
        sServer = null;
    }

    private static void serveLoop() {
        LocalServerSocket server;
        try {
            server = new LocalServerSocket(socketName());
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "BulkApiServer bind failed", e);
            sRunning = false;
            return;
        }
        sServer = server;
        Logger.logInfo(LOG_TAG, "BulkApiServer listening");

        while (sRunning) {
            LocalSocket client;
            try {
                client = server.accept();
            } catch (Exception e) {
                if (sRunning) Logger.logError(LOG_TAG, "accept failed: " + e.getMessage());
                break;
            }
            try {
                handleClient(client);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "client handling failed: " + e.getMessage());
            } finally {
                try { client.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static void handleClient(LocalSocket client) throws Exception {
        client.setSoTimeout(60_000);
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String reply;
            try {
                reply = execute(line);
            } catch (Exception e) {
                reply = "ERR " + e.getMessage();
            }
            out.write(reply);
            out.write("\n");
            out.flush();
        }
    }

    private static String execute(String line) {
        if (line.startsWith("SCANR ")) {
            scanRecursive(line.substring(6).trim());
            return "OK";
        }
        if (line.startsWith("SCAN ")) {
            scanOne(line.substring(5).trim());
            return "OK";
        }
        if (line.equals("WAKELOCK")) {
            acquireWakeLock();
            return "OK";
        }
        if (line.equals("WAKEUNLOCK")) {
            releaseWakeLock();
            return "OK";
        }
        return "ERR unknown command";
    }

    // ------------------------------------------------------------- media scan

    /** Ek file scan karo aur onScanCompleted tak ruko (15s safety timeout — hang kabhi nahi). */
    private static void scanOne(final String path) {
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            MediaScannerConnection.scanFile(sContext, new String[]{path}, null,
                (scannedPath, uri) -> latch.countDown());
            if (!latch.await(15, TimeUnit.SECONDS)) {
                Logger.logError(LOG_TAG, "scan timeout (continuing): " + path);
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "scan failed for " + path + ": " + e.getMessage());
        }
    }

    /** Folder ke andar ki tamam files, purani-se-nayi (mtime) order me, ek ek karke scan. */
    private static void scanRecursive(String dirPath) {
        List<File> files = new ArrayList<>();
        collectFiles(new File(dirPath), files);
        Collections.sort(files, Comparator
            .comparingLong(File::lastModified)
            .thenComparing(File::getName));
        for (File f : files) {
            scanOne(f.getAbsolutePath());
        }
    }

    private static void collectFiles(File dir, List<File> out) {
        File[] entries = dir.listFiles();
        if (entries == null) return;
        for (File e : entries) {
            if (e.isDirectory()) {
                if (!e.getName().startsWith(".")) collectFiles(e, out);
            } else if (e.isFile()) {
                out.add(e);
            }
        }
    }

    // ------------------------------------------------------------- wake lock

    private static synchronized void acquireWakeLock() {
        try {
            if (sWakeLock == null) {
                PowerManager pm = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);
                if (pm == null) return;
                sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BulkMockup:ApiWakeLock");
                sWakeLock.setReferenceCounted(false);
            }
            if (!sWakeLock.isHeld()) sWakeLock.acquire();
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "wake lock acquire failed: " + e.getMessage());
        }
    }

    private static synchronized void releaseWakeLock() {
        try {
            if (sWakeLock != null && sWakeLock.isHeld()) sWakeLock.release();
        } catch (Exception ignored) {}
    }
}
