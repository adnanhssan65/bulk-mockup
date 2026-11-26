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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Termux Mod ka built-in API server (Termux:API app ka replacement — sirf EK APK).
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
 *   SCAN <path>    — ek file ko MediaScanner se scan karo
 *   SCANR <path>   — poora folder recursively scan karo (mtime order me)
 *   WAKELOCK       — wake lock lo (script ke unlock tak)
 *   WAKEUNLOCK     — wake lock chhor do
 *
 * v150 — SERVER REDESIGN (owner-approved 2026-11-26; stall ka pakka hal):
 * purana server EK hi thread par har connection serial handle karta
 * tha or har scan ke callback par 15s tak ruk jata tha. App-private
 * paths (/data/data/com.termux/files/home/... engine outputs) ka
 * callback MediaProvider se KABHI nahi ata (wo jagah provider parh
 * hi nahi sakta) — ek aisi darkhwast poori line ko zehar kar deti
 * thi: peeche ki har scan runner ke 5s timeout par mar jati thi
 * (phone saboot: 6 images = 68 second).
 *
 * Naya design:
 *  - Har connection apni thread par; WAKELOCK/WAKEUNLOCK foran,
 *    kabhi line me nahi.
 *  - Scan ALLOWLIST: canonical path sirf /storage/ ya /sdcard ke
 *    neeche ho to asal scan; baqi sab (app-private waghera) par
 *    foran OK — MediaScannerConnection bulaya hi nahi jata.
 *    SCANR me bhi har file par yahi filter.
 *  - Asal scans EK FIFO worker thread par — darkhwast ki tartib
 *    sakhti se mehfooz. Worker har scan ke callback ka zyada se
 *    zyada 4s intezar karta hai.
 *  - Connection handler jawab tab deta hai jab scan mukammal ho
 *    YA 3.5s guzar jayen — jo pehle ho. Is liye runner ka 5s
 *    subprocess timeout wrapper ko kabhi nahi marta; worker
 *    peeche apni tartib se scans khatam karta rehta hai.
 *  - CIRCUIT BREAKER: 2 lagatar callback miss hon to worker 30s
 *    ke liye no-wait mode me chala jata hai (scans FIFO tartib
 *    me submit hote hain, callback ka intezar nahi hota), phir
 *    khud waiting mode par wapas.
 *  - awaitIdle(ms)/pendingScanCount(): BulkMediaRegister ki
 *    pass is queue ke khali hone ka intezar kar sakti hai.
 */
public final class BulkApiServer {

    private static final String LOG_TAG = "BulkApiServer";
    public static final String SOCKET_NAME = "com.termux.bulkapi";

    /** Worker ek scan ke callback ka zyada se zyada intezar. */
    static final long SCAN_CALLBACK_WAIT_MS = 4000L;
    /** Handler jawab se pehle zyada se zyada intezar (runner ka
     *  5s timeout is se hamesha peeche rahe). */
    static final long REPLY_WAIT_MS = 3500L;
    /** Itne lagatar callback miss → circuit breaker khul jata hai. */
    static final int BREAKER_MISS_LIMIT = 2;
    /** Breaker khulne par no-wait mode ki muddat. */
    static final long BREAKER_NOWAIT_MS = 30_000L;

    private static volatile boolean sRunning = false;
    private static LocalServerSocket sServer;
    private static Context sContext;
    private static PowerManager.WakeLock sWakeLock;

    // ------------------------------------------------------------ scan queue

    /** Ek scan darkhwast: worker is ka kaam khatam kare to done girta hai. */
    static final class ScanTask {
        final String path;
        final CountDownLatch done = new CountDownLatch(1);

        ScanTask(String path) {
            this.path = path;
        }
    }

    private static final BlockingQueue<ScanTask> SCAN_QUEUE =
        new LinkedBlockingQueue<>();
    private static final AtomicInteger PENDING = new AtomicInteger(0);
    private static volatile boolean sWorkerRunning = false;
    private static Thread sWorker;

    private BulkApiServer() {}

    public static String socketName() {
        return SOCKET_NAME;
    }

    public static synchronized void start(Context context) {
        if (sRunning) return;
        sRunning = true;
        sContext = context.getApplicationContext();
        sWorkerRunning = true;
        sWorker = new Thread(BulkApiServer::workerLoop, "BulkApiScanWorker");
        sWorker.setDaemon(true);
        sWorker.start();
        new Thread(BulkApiServer::serveLoop, "BulkApiServer").start();
        Logger.logInfo(LOG_TAG, "BulkApiServer starting at " + socketName());
    }

    public static synchronized void stop() {
        sRunning = false;
        sWorkerRunning = false;
        if (sWorker != null) sWorker.interrupt();
        // Line me phansi darkhwastein chor do — un ke handlers ko
        // jawab mil jaye, pending ginna bhi durust rahe.
        ScanTask leftover;
        while ((leftover = SCAN_QUEUE.poll()) != null) {
            leftover.done.countDown();
            PENDING.decrementAndGet();
        }
        releaseWakeLock();
        try { if (sServer != null) sServer.close(); } catch (Exception ignored) {}
        sServer = null;
    }

    /** Abhi line me (queue + chalta hua) kitne scans baqi hain. */
    public static int pendingScanCount() {
        return PENDING.get();
    }

    /**
     * Scan queue ke khali hone ka intezar — zyada se zyada ms.
     * @return true agar queue waqt ke andar khali ho gayi
     */
    public static boolean awaitIdle(long ms) {
        long deadline = System.currentTimeMillis() + Math.max(0L, ms);
        while (PENDING.get() > 0) {
            if (System.currentTimeMillis() >= deadline) return false;
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------ allowlist (pure)

    /**
     * Scan allowlist ka faisla (pure): canonical path sirf
     * shared storage (/storage/... ya /sdcard...) ho to asal
     * scan ke qabil hai. App-private paths (/data/data/...)
     * MediaProvider parh hi nahi sakta — un ka callback kabhi
     * nahi ata, is liye un par scan ki koshish hi zehar thi.
     */
    public static boolean isScanAllowed(String canonicalPath) {
        if (canonicalPath == null) return false;
        return canonicalPath.equals("/storage")
            || canonicalPath.startsWith("/storage/")
            || canonicalPath.equals("/sdcard")
            || canonicalPath.startsWith("/sdcard/");
    }

    /**
     * Asal path ka faisla (glue): pehle canonical banao — is se
     * $HOME/storage/... wale symlinks asal shared-storage path
     * ban jate hain or allowlist durust lagu hoti hai.
     * Canonical fail ho to raw path par hi faisla.
     */
    static boolean scanAllowed(String rawPath) {
        if (rawPath == null) return false;
        String canon;
        try {
            canon = new File(rawPath).getCanonicalPath();
        } catch (Exception e) {
            canon = rawPath;
        }
        return isScanAllowed(canon);
    }

    // ------------------------------------------------------------ connections

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
            final LocalSocket client;
            try {
                client = server.accept();
            } catch (Exception e) {
                if (sRunning) Logger.logError(LOG_TAG, "accept failed: " + e.getMessage());
                break;
            }
            // Har connection apni thread par — ek sust client
            // dusron ko (khas taur par WAKELOCK ko) nahi rokta.
            Thread t = new Thread(() -> handleClient(client), "BulkApiClient");
            t.setDaemon(true);
            t.start();
        }
    }

    private static void handleClient(LocalSocket client) {
        try {
            client.setSoTimeout(60_000);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(client.getInputStream(), "UTF-8"));
            BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(client.getOutputStream(), "UTF-8"));
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
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "client handling failed: " + e.getMessage());
        } finally {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private static String execute(String line) {
        if (line.startsWith("SCANR ")) {
            // Folder: har file allowlist se guzar kar line me;
            // jawab aakhri file ke mukammal hone ya 3.5s par —
            // baqi worker peeche tartib se khatam karta rahe ga.
            List<String> files = collectScanFiles(line.substring(6).trim());
            ScanTask last = null;
            for (String f : files) last = enqueueScan(f);
            if (last != null) awaitQuietly(last.done, REPLY_WAIT_MS);
            return "OK";
        }
        if (line.startsWith("SCAN ")) {
            String path = line.substring(5).trim();
            if (!scanAllowed(path)) return "OK";
            ScanTask task = enqueueScan(path);
            awaitQuietly(task.done, REPLY_WAIT_MS);
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

    private static void awaitQuietly(CountDownLatch latch, long ms) {
        try {
            latch.await(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------ scan worker

    private static ScanTask enqueueScan(String path) {
        ScanTask task = new ScanTask(path);
        PENDING.incrementAndGet();
        SCAN_QUEUE.add(task);
        return task;
    }

    /**
     * EK hi worker — scans darkhwast ki tartib me hi submit
     * hote hain (gallery order ke liye lazmi). Waiting mode me
     * har scan ke callback ka 4s tak intezar; breaker khula ho
     * to sirf submit, intezar nahi.
     */
    private static void workerLoop() {
        int consecutiveMisses = 0;
        long noWaitUntilMs = 0;
        while (sWorkerRunning) {
            ScanTask task;
            try {
                task = SCAN_QUEUE.take();
            } catch (InterruptedException e) {
                if (!sWorkerRunning) break;
                continue;
            }
            try {
                boolean noWait = System.currentTimeMillis() < noWaitUntilMs;
                boolean hit = runScan(task.path, noWait);
                if (noWait) {
                    // No-wait mode me miss nahi ginte — mode
                    // apni muddat (30s) par khud khatam hota hai.
                } else if (hit) {
                    consecutiveMisses = 0;
                } else {
                    consecutiveMisses++;
                    if (consecutiveMisses >= BREAKER_MISS_LIMIT) {
                        consecutiveMisses = 0;
                        noWaitUntilMs = System.currentTimeMillis() + BREAKER_NOWAIT_MS;
                        Logger.logInfo(LOG_TAG,
                            "Scan circuit breaker: 30s no-wait mode");
                    }
                }
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "scan worker fail: " + e.getMessage());
            } finally {
                task.done.countDown();
                PENDING.decrementAndGet();
            }
        }
    }

    /**
     * Ek scan asal me chalao.
     * @return waiting mode me callback aya (true) ya miss (false);
     *         no-wait mode me hamesha true (miss nahi gina jata)
     */
    private static boolean runScan(final String path, boolean noWait) {
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            MediaScannerConnection.scanFile(sContext, new String[]{path}, null,
                (scannedPath, uri) -> latch.countDown());
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "scan failed for " + path + ": " + e.getMessage());
            return false;
        }
        if (noWait) return true;
        try {
            boolean fired = latch.await(SCAN_CALLBACK_WAIT_MS, TimeUnit.MILLISECONDS);
            if (!fired) {
                Logger.logError(LOG_TAG, "scan callback miss (continuing): " + path);
            }
            return fired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Folder ki files mtime tartib me, allowlist se guzar kar. */
    private static List<String> collectScanFiles(String dirPath) {
        List<File> files = new ArrayList<>();
        collectFiles(new File(dirPath), files);
        Collections.sort(files, Comparator
            .comparingLong(File::lastModified)
            .thenComparing(File::getName));
        List<String> out = new ArrayList<>();
        for (File f : files) {
            String p = f.getAbsolutePath();
            if (scanAllowed(p)) out.add(p);
        }
        return out;
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
