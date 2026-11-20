package com.salah.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

/**
 * مشغّل عام لأي صوت في التطبيق (الأذكار، الابتهالات، الأذان).
 * بيفضل شغال لما الشاشة تتقفل، وبيتحكم فيه من الإشعار.
 */
public final class Audio {

    public interface Listener {
        void onAudioChanged();
    }

    private static MediaPlayer player;
    private static Context app;
    private static Listener screen;
    private static Listener watcher;

    /** معرّف المقطع الشغال، عشان الشاشة تعرف أنهي سطر تلوّنه. */
    private static String currentId = "";
    private static java.util.List<String> queueAssets;
    private static java.util.List<String> queueIds;
    private static java.util.List<String> queueTitles;
    private static int queuePos = -1;
    private static String title = "";
    private static String subtitle = "";
    private static boolean paused;
    /** يتحط لما مقطع يفشل، والشاشة بتقراه وتمسحه. */
    private static boolean failed;

    private Audio() {
    }

    public static String currentId() {
        return currentId;
    }

    public static String title() {
        return title;
    }

    public static String subtitle() {
        return subtitle;
    }

    public static boolean isActive() {
        // وقت إعادة الاتصال مفيش player، بس الخدمة والإشعار لازم يفضلوا
        return player != null || reconnecting;
    }

    /** هل آخر تشغيل فشل؟ القراءة بتصفّر العلامة. */
    public static boolean consumeFailure() {
        boolean f = failed;
        failed = false;
        return f;
    }

    public static boolean isPlaying() {
        try {
            return player != null && player.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static void attach(Listener l) {
        screen = l;
    }

    public static void detach(Listener l) {
        if (screen == l) screen = null;
    }

    public static void setWatcher(Listener w) {
        watcher = w;
    }

    private static void fire() {
        Listener s = screen;
        Listener w = watcher;
        if (s != null) s.onAudioChanged();
        if (w != null) w.onAudioChanged();
    }

    /** يشغّل ملف واحد من أصول التطبيق. يرجع false لو فشل. */
    public static boolean playAsset(Context c, String assetPath, String id,
                                    String title_, String subtitle_) {
        clearQueue();
        return playOne(c, assetPath, id, title_, subtitle_);
    }

    private static boolean playOne(Context c, String assetPath, String id,
                                   String title_, String subtitle_) {
        stopLive();
        release();
        app = c.getApplicationContext();
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            AssetFileDescriptor afd = app.getAssets().openFd(assetPath);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setOnCompletionListener(p -> onFinishedTrack());
            mp.setOnErrorListener((p, what, extra) -> {
                stop();
                return true;
            });
            mp.prepare();
            mp.start();
            player = mp;
            currentId = id;
            title = title_;
            subtitle = subtitle_;
            paused = false;
            startService(app);
            fire();
            return true;
        } catch (Exception e) {
            mp.release();
            return false;
        }
    }

    // ===== البث المباشر: إعادة اتصال تلقائي =====
    // البث بيقطع لوحده أحيانًا (النت أو السيرفر). طول ما المستخدم ماوقّفش، بنحاول نوصّل
    // تاني بفواصل بتزيد، وبنراقب لو الصوت علّق من غير ما يطلع خطأ.

    private static final android.os.Handler main =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private static String liveUrl;
    private static boolean reconnecting;
    private static int retries;
    private static long lastPos = -1;
    private static long stuckSince;
    private static long bufferingSince;
    private static android.net.wifi.WifiManager.WifiLock wifiLock;
    /** نوقف المحاولة بعد حوالي ٢٠ دقيقة من غير نجاح. */
    private static final int MAX_RETRIES = 60;

    private static final Runnable retry = () -> {
        if (liveUrl == null || app == null) return;
        connect(app, false);
    };

    /** كل ١٠ ثواني: لو الصوت مش بيتقدّم ٢٠ ثانية، أو بيحمّل ٢٠ ثانية، نوصّل تاني. */
    private static final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            if (liveUrl == null) return;
            main.postDelayed(this, 10000);
            MediaPlayer p = player;
            if (p == null || paused || reconnecting) return;
            long now = android.os.SystemClock.elapsedRealtime();
            if (bufferingSince > 0 && now - bufferingSince > 20000) {
                dropped();
                return;
            }
            try {
                if (!p.isPlaying()) return;
                long pos = p.getCurrentPosition();
                if (pos != lastPos) {
                    lastPos = pos;
                    stuckSince = now;
                } else if (stuckSince > 0 && now - stuckSince > 20000) {
                    dropped();
                }
            } catch (IllegalStateException e) {
                dropped();
            }
        }
    };

    public static boolean isReconnecting() {
        return reconnecting;
    }

    /** يشغّل بث مباشر من رابط. */
    public static boolean playUrl(Context c, String url, String id,
                                  String title_, String subtitle_) {
        clearQueue();
        stopLive();
        app = c.getApplicationContext();
        liveUrl = url;
        currentId = id;
        title = title_;
        subtitle = subtitle_;
        retries = 0;
        boolean ok = connect(app, true);
        main.removeCallbacks(watchdog);
        if (ok) main.postDelayed(watchdog, 10000);
        return ok;
    }

    private static boolean connect(Context c, boolean first) {
        release();
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            // المعالج والواي فاي مايناموش والشاشة مقفولة، وده كان من أسباب القطع
            mp.setWakeMode(c, android.os.PowerManager.PARTIAL_WAKE_LOCK);
            holdWifi(c);
            mp.setDataSource(liveUrl);
            mp.setOnPreparedListener(p -> {
                if (player != p) return;
                try {
                    p.start();
                } catch (IllegalStateException ignored) {
                }
                reconnecting = false;
                retries = 0;
                bufferingSince = 0;
                lastPos = -1;
                stuckSince = android.os.SystemClock.elapsedRealtime();
                subtitle = "بث مباشر";
                fire();
            });
            mp.setOnInfoListener((p, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    bufferingSince = android.os.SystemClock.elapsedRealtime();
                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    bufferingSince = 0;
                }
                return false;
            });
            mp.setOnErrorListener((p, what, extra) -> {
                if (player == p) dropped();
                return true;
            });
            mp.setOnCompletionListener(p -> {
                // البث المباشر مالوش نهاية: لو "خلص" يبقى قطع
                if (player == p) dropped();
            });
            player = mp;
            paused = false;
            bufferingSince = android.os.SystemClock.elapsedRealtime();
            // البث بيحتاج وقت للاتصال، فالتحضير بيحصل في الخلفية
            mp.prepareAsync();
            // وقت إعادة الاتصال الخدمة شغالة أصلًا، وتشغيلها من الخلفية ممنوع
            if (first) startService(c);
            fire();
            return true;
        } catch (Exception e) {
            mp.release();
            player = null;
            if (first) {
                failed = true;
                stopLive();
                return false;
            }
            dropped();
            return false;
        }
    }

    /** البث وقع: نحاول تاني بعد شوية (٢، ٥، ١٠، وبعدين كل ٢٠ ثانية). */
    private static void dropped() {
        if (liveUrl == null) return;
        release();
        if (retries >= MAX_RETRIES) {
            failed = true;
            stop();
            return;
        }
        reconnecting = true;
        retries++;
        long wait = retries == 1 ? 2000 : retries == 2 ? 5000 : retries == 3 ? 10000 : 20000;
        subtitle = "انقطع البث — بيعيد الاتصال…";
        main.removeCallbacks(retry);
        main.postDelayed(retry, wait);
        fire();
    }

    private static void holdWifi(Context c) {
        try {
            if (wifiLock == null) {
                android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                        c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm == null) return;
                int mode = android.os.Build.VERSION.SDK_INT >= 29
                        ? android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                        : android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF;
                wifiLock = wm.createWifiLock(mode, "salaty:radio");
                wifiLock.setReferenceCounted(false);
            }
            if (!wifiLock.isHeld()) wifiLock.acquire();
        } catch (Exception ignored) {
        }
    }

    private static void stopLive() {
        liveUrl = null;
        reconnecting = false;
        retries = 0;
        bufferingSince = 0;
        main.removeCallbacks(retry);
        main.removeCallbacks(watchdog);
        try {
            if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
        } catch (Exception ignored) {
        }
    }

    /** يشغّل قائمة مقاطع ورا بعض. */
    public static boolean playQueue(Context c, java.util.List<String> assets,
                                    java.util.List<String> ids,
                                    java.util.List<String> titles, String subtitle_) {
        if (assets == null || assets.isEmpty()) return false;
        queueAssets = assets;
        queueIds = ids;
        queueTitles = titles;
        queuePos = 0;
        return playFromQueue(c, subtitle_);
    }

    private static boolean playFromQueue(Context c, String subtitle_) {
        if (queueAssets == null || queuePos < 0 || queuePos >= queueAssets.size()) {
            clearQueue();
            return false;
        }
        String t = queueTitles != null && queuePos < queueTitles.size()
                ? queueTitles.get(queuePos) : "تشغيل";
        String id = queueIds != null && queuePos < queueIds.size()
                ? queueIds.get(queuePos) : String.valueOf(queuePos);
        String sub = subtitle_ + " · " + (queuePos + 1) + "/" + queueAssets.size();
        java.util.List<String> a = queueAssets, i = queueIds, ti = queueTitles;
        int pos = queuePos;
        String keepSub = subtitle_;
        boolean ok = playOne(c, a.get(pos), id, t, sub);
        // playOne بيمسح القائمة، فنرجّعها
        queueAssets = a;
        queueIds = i;
        queueTitles = ti;
        queuePos = pos;
        queueSubtitle = keepSub;
        return ok;
    }

    private static String queueSubtitle = "";

    private static void onFinishedTrack() {
        if (queueAssets != null && queuePos >= 0 && queuePos + 1 < queueAssets.size()) {
            queuePos++;
            playFromQueue(app, queueSubtitle);
        } else {
            stop();
        }
    }

    private static void clearQueue() {
        queueAssets = null;
        queueIds = null;
        queueTitles = null;
        queuePos = -1;
        queueSubtitle = "";
    }

    private static void startService(Context c) {
        try {
            c.startForegroundService(new Intent(c, AudioService.class)
                    .setAction(AudioService.ACTION_SHOW));
        } catch (RuntimeException ignored) {
            // النظام منع الخدمة: الصوت يفضل شغال والشاشة مفتوحة
        }
    }

    public static void pause() {
        try {
            if (player != null && player.isPlaying()) {
                player.pause();
                paused = true;
                fire();
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static void resume() {
        try {
            if (player != null && paused) {
                player.start();
                paused = false;
                fire();
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static void toggle() {
        // إيقاف من الإشعار وقت إعادة الاتصال = المستخدم مش عايز يكمّل
        if (reconnecting) {
            stop();
            return;
        }
        if (isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    private static void release() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
    }

    public static void stop() {
        stopLive();
        release();
        clearQueue();
        paused = false;
        currentId = "";
        title = "";
        subtitle = "";
        fire();
    }
}
