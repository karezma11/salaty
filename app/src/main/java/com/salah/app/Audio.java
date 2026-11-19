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
        return player != null;
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

    /** يشغّل بث مباشر من رابط. */
    public static boolean playUrl(Context c, String url, String id,
                                  String title_, String subtitle_) {
        clearQueue();
        release();
        app = c.getApplicationContext();
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            mp.setDataSource(url);
            mp.setOnPreparedListener(p -> {
                if (player != p) return;
                try {
                    p.start();
                } catch (IllegalStateException ignored) {
                }
                fire();
            });
            mp.setOnErrorListener((p, what, extra) -> {
                failed = true;
                stop();
                return true;
            });
            mp.setOnCompletionListener(p -> stop());
            player = mp;
            currentId = id;
            title = title_;
            subtitle = subtitle_;
            paused = false;
            // البث بيحتاج وقت للاتصال، فالتحضير بيحصل في الخلفية
            mp.prepareAsync();
            startService(app);
            fire();
            return true;
        } catch (Exception e) {
            mp.release();
            player = null;
            failed = true;
            return false;
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
        release();
        clearQueue();
        paused = false;
        currentId = "";
        title = "";
        subtitle = "";
        fire();
    }
}
