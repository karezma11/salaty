package com.salah.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** ينزّل تلاوة السور في الخلفية مع إشعار بالتقدّم. */
public class DownloadService extends Service {

    public static final String ACTION_SURA = "com.salah.app.DL_SURA";
    public static final String ACTION_ALL = "com.salah.app.DL_ALL";
    public static final String ACTION_CANCEL = "com.salah.app.DL_CANCEL";
    /** المصحف كامل لكل القراء. */
    public static final String ACTION_EVERYONE = "com.salah.app.DL_EVERYONE";
    public static final String EXTRA_WIFI_ONLY = "wifi_only";
    public static final String EXTRA_SURA = "sura";

    private static final int NOTIF_ID = 6;

    public interface Listener {
        void onProgress(int sura, int done, int total);

        void onFinished(int sura, boolean ok);
    }

    private static Listener listener;
    private static final AtomicBoolean cancelled = new AtomicBoolean(false);
    private static volatile int activeSura = 0;

    public static void setListener(Listener l) {
        listener = l;
    }

    public static int activeSura() {
        return activeSura;
    }

    public static void start(Context c, int sura) {
        Intent i = new Intent(c, DownloadService.class)
                .setAction(ACTION_SURA)
                .putExtra(EXTRA_SURA, sura);
        try {
            c.startForegroundService(i);
        } catch (RuntimeException ignored) {
        }
    }

    public static void startAll(Context c) {
        try {
            c.startForegroundService(new Intent(c, DownloadService.class).setAction(ACTION_ALL));
        } catch (RuntimeException ignored) {
        }
    }

    public static void startEveryone(Context c, boolean wifiOnly) {
        try {
            c.startForegroundService(new Intent(c, DownloadService.class)
                    .setAction(ACTION_EVERYONE).putExtra(EXTRA_WIFI_ONLY, wifiOnly));
        } catch (RuntimeException ignored) {
        }
    }

    public static void cancel(Context c) {
        cancelled.set(true);
    }

    private ExecutorService pool;
    private Thread worker;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SURA : intent.getAction();
        if (ACTION_CANCEL.equals(action)) {
            cancelled.set(true);
            return START_NOT_STICKY;
        }
        if (worker != null && worker.isAlive()) {
            return START_NOT_STICKY; // تحميل شغال بالفعل
        }

        cancelled.set(false);
        notify(0, 0, "جاري التحضير...");

        final boolean all = ACTION_ALL.equals(action);
        final int one = intent == null ? 1 : intent.getIntExtra(EXTRA_SURA, 1);
        final boolean everyone = ACTION_EVERYONE.equals(action);
        final boolean wifiOnly = intent != null && intent.getBooleanExtra(EXTRA_WIFI_ONLY, false);

        worker = new Thread(() -> {
            if (everyone) {
                try {
                    downloadEveryone(wifiOnly);
                } finally {
                    activeSura = 0;
                    stopForeground(true);
                    stopSelf();
                }
                return;
            }
            try {
                Queue<Integer> queue = new ArrayDeque<>();
                if (all) {
                    for (int i = 1; i <= 114; i++) queue.add(i);
                } else {
                    queue.add(one);
                }
                while (!queue.isEmpty() && !cancelled.get()) {
                    downloadSura(queue.poll());
                }
            } finally {
                activeSura = 0;
                stopForeground(true);
                stopSelf();
            }
        });
        worker.start();
        return START_NOT_STICKY;
    }

    /** كل القراء، سورة سورة. بيقف لو الواي فاي اتقطع (لو «على الواي فاي بس») أو المساحة خلصت. */
    private void downloadEveryone(boolean wifiOnly) {
        String stopReason = null;
        int n = Reciters.ALL.length;
        outer:
        for (int r = 0; r < n; r++) {
            Reciters.Reciter rec = Reciters.ALL[r];
            for (int s = 1; s <= 114; s++) {
                if (cancelled.get()) {
                    stopReason = "اتلغى";
                    break outer;
                }
                if (wifiOnly && !Downloads.onWifi(this)) {
                    stopReason = "وقف: الواي فاي اتقطع";
                    break outer;
                }
                if (Recitation.folder(this).getParentFile() != null
                        && Recitation.folder(this).getParentFile().getUsableSpace() < 200L * 1024 * 1024) {
                    stopReason = "وقف: مساحة الموبايل خلصت";
                    break outer;
                }
                downloadSura(rec.id, s, "القارئ " + Ui.arabicDigits(r + 1) + " من "
                        + Ui.arabicDigits(n) + " · " + rec.name + " · ");
            }
        }
        Recitation.invalidate();
        Notifications.show(this, Notifications.CH_UPDATES, 81,
                stopReason == null ? "اتحمّل المصحف كامل لكل القراء ✓" : "تحميل المصحف لكل القراء — " + stopReason,
                stopReason == null ? "التلاوة كلها بقت تشتغل من غير نت"
                        : "ادخل «إدارة التحميلات» ودوس «حمّل المصحف كامل لكل القراء» ويكمّل من مكانه");
    }

    private void downloadSura(int sura) {
        downloadSura(Prefs.reciter(this), sura, "");
    }

    private void downloadSura(String reciter, int sura, String prefix) {
        List<Quran.Sura> all = Quran.suras(this);
        if (sura < 1 || sura > all.size()) return;
        Quran.Sura s = all.get(sura - 1);
        final int total = s.ayas;
        final boolean mine = reciter.equals(Prefs.reciter(this));
        if (mine) activeSura = sura;

        if (Downloads.haveCount(this, reciter, sura, total) == total) {
            if (mine) {
                report(sura, total, total);
                finished(sura, true);
            }
            return;
        }

        pool = Executors.newFixedThreadPool(4);
        final AtomicInteger done = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);

        for (int a = 1; a <= total; a++) {
            final int ayah = a;
            pool.submit(() -> {
                if (cancelled.get()) return;
                boolean ok = Downloads.fetchAyah(DownloadService.this, reciter, sura, ayah);
                if (!ok) failed.incrementAndGet();
                int d = done.incrementAndGet();
                if (d % 3 == 0 || d == total) {
                    notify(d, total, prefix + "سورة " + s.name);
                    if (mine) report(sura, d, total);
                }
            });
        }
        pool.shutdown();
        try {
            while (!pool.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                if (cancelled.get()) {
                    pool.shutdownNow();
                    break;
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }
        if (mine) finished(sura, failed.get() == 0 && !cancelled.get());
    }

    private void report(int sura, int done, int total) {
        Listener l = listener;
        if (l != null) l.onProgress(sura, done, total);
    }

    private void finished(int sura, boolean ok) {
        Listener l = listener;
        if (l != null) l.onFinished(sura, ok);
    }

    private void notify(int done, int total, String text) {
        Notifications.ensureChannels(this);
        Intent cancelIntent = new Intent(this, DownloadService.class).setAction(ACTION_CANCEL);
        PendingIntent cancelPi = PendingIntent.getService(this, 31, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = new Notification.Builder(this, Notifications.CH_RECITATION)
                .setSmallIcon(R.drawable.ic_notification_download)
                .setColor(Notifications.accent(this))
                .setContentTitle("تحميل التلاوة")
                .setContentText(total > 0
                        ? text + " — " + Ui.arabicDigits(done) + " من " + Ui.arabicDigits(total)
                        : text)
                .setContentIntent(Notifications.openApp(this))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null, "إلغاء", cancelPi).build());
        if (total > 0) b.setProgress(total, done, false);

        Notification n = b.build();
        if (NOTIF_ID == 6) {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIF_ID, n);
            }
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, n);
    }

    @Override
    public void onDestroy() {
        cancelled.set(true);
        if (pool != null) pool.shutdownNow();
        super.onDestroy();
    }
}
