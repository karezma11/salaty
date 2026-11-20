package com.salah.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

/**
 * تشغيل التلاوة آية بعد آية.
 * بيفضل شغال حتى لو الشاشة اتقفلت، وبيخطر الشاشة والخدمة بالآية الحالية.
 */
public final class RecitationPlayer {

    public interface Listener {
        /** تغيّرت الآية الجارية. */
        void onAyah(int sura, int ayah);

        /** خلصت السورة أو وقف التشغيل. */
        void onStopped();
    }

    private static MediaPlayer player;
    private static Context app;
    /** شاشة السورة، بتروح وتيجي. */
    private static Listener listener;
    /** الخدمة اللي بتعرض الإشعار، بتفضل طول التشغيل. */
    private static Listener watcher;
    private static int sura;
    private static int ayah;
    private static int lastAyah;
    private static boolean paused;

    /** تكرار كل آية (1 = مرة واحدة). */
    private static int repeatEach = 1;
    private static int playedTimes;
    /** نطاق التكرار داخل السورة، و0 يعني السورة كلها. */
    private static int rangeFrom, rangeTo;
    private static int rangeRepeats = 1;
    private static int rangeDone;
    /** يكمّل للسورة اللي بعدها لوحده. */
    private static boolean continuous;
    private static float speed = 1.0f;
    /** صوت التلاوة (مؤقت النوم بيوطّيه في آخر آية). */
    private static float volume = 1f;

    private RecitationPlayer() {
    }

    public static int currentSura() {
        return sura;
    }

    public static int currentAyah() {
        return ayah;
    }

    public static int lastAyah() {
        return lastAyah;
    }

    public static boolean isPlaying() {
        try {
            return player != null && player.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static boolean isActive() {
        return player != null;
    }

    public static int repeatEach() {
        return repeatEach;
    }

    public static void setRepeatEach(int n) {
        repeatEach = Math.max(1, n);
        playedTimes = 0;
    }

    public static int rangeFrom() {
        return rangeFrom;
    }

    public static int rangeTo() {
        return rangeTo;
    }

    public static int rangeRepeats() {
        return rangeRepeats;
    }

    /** يحدد مقطع للتكرار؛ from=0 يلغي النطاق. */
    public static void setRange(int from, int to, int repeats) {
        rangeFrom = Math.max(0, from);
        rangeTo = Math.max(0, to);
        rangeRepeats = Math.max(1, repeats);
        rangeDone = 0;
    }

    public static boolean continuous() {
        return continuous;
    }

    public static void setContinuous(boolean on) {
        continuous = on;
    }

    public static float speed() {
        return speed;
    }

    /** يغيّر سرعة التلاوة فورًا. */
    public static void setSpeed(float s) {
        speed = Math.max(0.5f, Math.min(2.0f, s));
        applySpeed();
    }

    private static void applySpeed() {
        try {
            if (player != null && player.isPlaying()) {
                player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
            }
        } catch (Exception ignored) {
            // بعض الأجهزة مش بتدعم تغيير السرعة
        }
    }

    /** يغيّر صوت التلاوة (من ٠ لـ ١)، ومؤقت النوم بيستخدمه للتوطية. */
    static void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        try {
            if (player != null) player.setVolume(volume, volume);
        } catch (IllegalStateException ignored) {
        }
    }

    /** الوقت الباقي في الآية الشغالة، أو 0 لو مش معروف. */
    static long ayahRemainingMs() {
        try {
            if (player == null || !player.isPlaying()) return 0;
            int d = player.getDuration();
            if (d <= 0) return 0;
            float sp = speed > 0 ? speed : 1f;
            return (long) (Math.max(0, d - player.getCurrentPosition()) / sp);
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    /** تربط الشاشة بالتشغيل الجاري من غير ما تبدأ من الأول. */
    public static void attach(Listener l) {
        listener = l;
    }

    /** تفصل الشاشة من غير ما توقف التلاوة. */
    public static void detach(Listener l) {
        if (listener == l) listener = null;
    }

    public static void setWatcher(Listener w) {
        watcher = w;
    }

    private static void fireAyah() {
        Listener l = listener;
        Listener w = watcher;
        if (l != null) l.onAyah(sura, ayah);
        if (w != null) w.onAyah(sura, ayah);
    }

    private static void fireStopped() {
        Listener l = listener;
        Listener w = watcher;
        if (l != null) l.onStopped();
        if (w != null) w.onStopped();
    }

    /** يبدأ سورة من آية معينة. يرجع false لو الصوت مش متاح. */
    public static boolean start(Context c, int suraIndex, int fromAyah, int ayahCount, Listener l) {
        releaseOnly();
        app = c.getApplicationContext();
        listener = l;
        sura = suraIndex;
        ayah = Math.max(1, fromAyah);
        lastAyah = ayahCount;
        paused = false;
        playedTimes = 0;
        rangeDone = 0;
        return playCurrent();
    }

    private static boolean playCurrent() {
        if (app == null || ayah > lastAyah) {
            finish();
            return false;
        }
        MediaPlayer mp = new MediaPlayer();
        AssetFileDescriptor fd = null;
        try {
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            File external = Recitation.externalFile(app, sura, ayah);
            if (external != null) {
                mp.setDataSource(external.getAbsolutePath());
            } else {
                fd = app.getAssets().openFd(Recitation.assetName(sura, ayah));
                mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            }
            mp.setOnCompletionListener(p -> next());
            mp.setOnErrorListener((p, what, extra) -> {
                finish();
                return true;
            });
            // prepare() كان بيقفل الواجهة لحد ما الملف يتقري؛ دلوقتي بيحصل في الخلفية
            final int startedSura = sura;
            final int startedAyah = ayah;
            mp.setOnPreparedListener(p -> {
                if (player != p || sura != startedSura || ayah != startedAyah) {
                    return;
                }
                try {
                    p.setVolume(volume, volume);
                    p.start();
                    applySpeed();
                } catch (IllegalStateException ignored) {
                }
                fireAyah();
                prefetchNext();
            });
            releaseOnly();
            player = mp;
            mp.prepareAsync();
            fireAyah();
            return true;
        } catch (IOException | IllegalStateException e) {
            mp.release();
            finish();
            return false;
        } finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** يجهّز الآية اللي بعدها وهي لسه بتتقري، عشان مفيش فاصل. */
    private static void prefetchNext() {
        final int s = sura, a = ayah + 1, last = lastAyah;
        final Context c = app;
        if (c == null || a > last) return;
        // في خيط منفصل، عشان ما نعطّلش الواجهة
        new Thread(() -> readAhead(c, s, a)).start();
    }

    private static void readAhead(Context app, int sura, int ayah) {
        try {
            File f = Recitation.externalFile(app, sura, ayah);
            if (f == null) return;
            // قراءة أول بايتات الملف بتخلي النظام يحطّه في الذاكرة المؤقتة
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                byte[] buf = new byte[8192];
                int left = 64 * 1024, read;
                while (left > 0 && (read = in.read(buf)) > 0) left -= read;
            }
        } catch (Exception ignored) {
        }
    }

    private static void next() {
        // مؤقت النوم: الوقت خلص والآية اللي كانت شغالة خلصت
        if (SleepTimer.expired()) {
            finish();
            return;
        }

        // تكرار نفس الآية
        playedTimes++;
        if (playedTimes < repeatEach) {
            playCurrent();
            return;
        }
        playedTimes = 0;

        int end = rangeFrom > 0 && rangeTo >= rangeFrom ? rangeTo : lastAyah;

        if (ayah < end) {
            ayah++;
            playCurrent();
            return;
        }

        // وصلنا لآخر المقطع
        if (rangeFrom > 0) {
            rangeDone++;
            if (rangeDone < rangeRepeats) {
                ayah = rangeFrom;
                playCurrent();
                return;
            }
            rangeDone = 0;
            if (rangeTo < lastAyah) {
                // المقطع خلص، نكمّل باقي السورة عادي
                setRange(0, 0, 1);
                ayah++;
                playCurrent();
                return;
            }
        }

        // آخر السورة
        if (SleepTimer.isEndOfSura()) {
            finish();
            return;
        }
        if (continuous && sura < 114 && app != null) {
            int nextSura = sura + 1;
            java.util.List<Quran.Sura> all = Quran.suras(app);
            if (nextSura <= all.size() && Recitation.available(app, nextSura)) {
                sura = nextSura;
                ayah = 1;
                lastAyah = all.get(nextSura - 1).ayas;
                playCurrent();
                return;
            }
        }
        finish();
    }

    public static void pause() {
        try {
            if (player != null && player.isPlaying()) {
                player.pause();
                paused = true;
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static void resume() {
        try {
            if (player != null && paused) {
                player.start();
                paused = false;
            }
        } catch (IllegalStateException ignored) {
        }
    }

    /** ينتقل لآية معينة في نفس السورة. */
    public static void jumpTo(int targetAyah) {
        if (player == null || targetAyah < 1 || targetAyah > lastAyah) return;
        ayah = targetAyah;
        playCurrent();
    }

    private static void releaseOnly() {
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
        releaseOnly();
        setRange(0, 0, 1);
        repeatEach = 1;
        playedTimes = 0;
        paused = false;
        sura = 0;
        ayah = 0;
        volume = 1f;
        fireStopped();
        listener = null;
        SleepTimer.playbackEnded();
    }

    private static void finish() {
        releaseOnly();
        paused = false;
        volume = 1f;
        fireStopped();
        SleepTimer.playbackEnded();
    }
}
