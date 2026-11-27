package com.salah.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

/**
 * خدمة تشغيل الأذان كامل في الخلفية.
 * بتشغل ملف الأذان المختار من assets/adhan، ولو مش موجود بتشغل نغمة المنبه.
 */
public class AdhanService extends Service {

    public static final String ACTION_STOP = "com.salah.app.STOP_ADHAN";
    private static final int NOTIF_ID = 1;

    private MediaPlayer player;

    // ===== الأذان بيعلى بالتدريج، وقلب الموبايل على وشه يوقفه =====
    private static final long FADE_MS = 15000L;
    private final Handler main = new Handler(Looper.getMainLooper());
    private long fadeStart;
    private final Runnable fade = new Runnable() {
        @Override
        public void run() {
            MediaPlayer p = player;
            if (p == null) return;
            float f = Math.min(1f, (android.os.SystemClock.elapsedRealtime() - fadeStart)
                    / (float) FADE_MS);
            // من ١٥٪ لـ ١٠٠٪، وبالتربيع عشان الودن تحس إنه بيعلى بانتظام
            float v = 0.15f + 0.85f * f * f;
            try {
                p.setVolume(v, v);
            } catch (IllegalStateException ignored) {
                return;
            }
            if (f < 1f) main.postDelayed(this, 250);
        }
    };
    private SensorManager sensors;
    /** شفنا الموبايل مش مقلوب الأول؟ عشان لو كان مقلوب على الترابيزة من الأول الأذان مايقفش. */
    private boolean seenUp;
    private long downSince;
    private final SensorEventListener flip = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            float z = e.values[2];
            if (z > -3f) {
                seenUp = true;
                downSince = 0;
            } else if (z < -8f && seenUp) {
                long now = android.os.SystemClock.elapsedRealtime();
                if (downSince == 0) {
                    downSince = now;
                } else if (now - downSince > 700) {
                    AdhanCheck.record(AdhanService.this, "الأذان وقف بقلب الموبايل");
                    finish();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private void startExtras(MediaPlayer mp) {
        if (Prefs.adhanFade(this)) {
            try {
                mp.setVolume(0.15f, 0.15f);
            } catch (IllegalStateException ignored) {
            }
            fadeStart = android.os.SystemClock.elapsedRealtime();
            main.post(fade);
        }
        if (Prefs.adhanFlip(this)) {
            try {
                sensors = getSystemService(SensorManager.class);
                Sensor acc = sensors == null ? null
                        : sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (acc != null) {
                    seenUp = false;
                    downSince = 0;
                    sensors.registerListener(flip, acc, SensorManager.SENSOR_DELAY_NORMAL);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void stopExtras() {
        main.removeCallbacks(fade);
        if (sensors != null) {
            try {
                sensors.unregisterListener(flip);
            } catch (Exception ignored) {
            }
            sensors = null;
        }
    }
    /** الصلاة اللي أذانها شغال، واليوم بتاعها (لزرار «صلّيت»). */
    private int prayer = PrayerTimes.FAJR;
    private String day;
    /** تجربة من الإعدادات، مش صلاة بجد. */
    private boolean test;
    private static AdhanService running;

    /** بعد «صلّيت» من أي إشعار: إشعار الأذان (لو شغال) يشيل الزراير ويقول إنها اتسجّلت. */
    static void refreshIfRunning() {
        AdhanService s = running;
        if (s == null || s.player == null) return;
        android.app.NotificationManager nm =
                s.getSystemService(android.app.NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, s.buildNotification(s.prayer));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean stop = intent != null && ACTION_STOP.equals(intent.getAction());
        if (!stop) {
            prayer = intent == null ? PrayerTimes.FAJR
                    : intent.getIntExtra("prayer", PrayerTimes.FAJR);
            test = intent != null && intent.getBooleanExtra(AdhanReceiver.EXTRA_TEST, false);
            day = Progress.dayKey(0);
        }
        if (day == null) day = Progress.dayKey(0);
        running = this;

        Notification n = buildNotification(prayer);
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (RuntimeException e) {
            AdhanCheck.record(this, "النظام رفض إشعار الأذان (" + e.getClass().getSimpleName() + ")");
            throw e;
        }

        if (stop) {
            finish();
            return START_NOT_STICKY;
        }
        play(prayer);
        return START_NOT_STICKY;
    }

    private Notification buildNotification(int prayer) {
        Notifications.ensureChannels(this);
        Intent stopIntent = new Intent(this, AdhanService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 11, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Action stopAction = new Notification.Action.Builder(null, "إيقاف", stopPi).build();

        // «صلّيت» و«في جماعة» جنب «إيقاف»، ولو اتسجّلت الإشعار بيقول كده بدل الزراير
        String text = test ? "اشتغل والشاشة مقفولة ✓" : Adhans.NAMES[Prefs.adhanVoice(this)];
        boolean fard = !test && prayer != PrayerTimes.SUNRISE && prayer >= 0 && prayer <= 5
                && day != null;
        int logged = fard ? PrayerLog.stateOn(this, day, prayer) : PrayerLog.NOT_PRAYED;
        if (fard && logged != PrayerLog.NOT_PRAYED) {
            text = "✓ اتسجّلت " + PrayerLog.label(logged);
        }
        Notification.Builder b = new Notification.Builder(this, Notifications.CH_ADHAN)
                .setSmallIcon(Notifications.smallIcon(this))
                // لون غامق ثابت: مع setColorized أندرويد بيختار لون النص حسب
                // الخلفية، والأخضر الفاتح كان بيخليه يختار نص غامق صعب القراءة
                .setColor(androidx.core.content.ContextCompat.getColor(
                        this, R.color.notif_adhan_bg))
                .setColorized(true)
                .setSubText(Prefs.city(this))
                .setContentTitle(test ? "تجربة الأذان"
                        : "حان الآن موعد صلاة " + PrayerTimes.NAMES[prayer])
                .setContentText(text)
                .setContentIntent(Notifications.openApp(this))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setOngoing(true)
                .addAction(stopAction);
        if (fard && logged == PrayerLog.NOT_PRAYED) {
            b.addAction(new Notification.Action.Builder(null, "صلّيت",
                    PrayedReceiver.intent(this, prayer, day, PrayerLog.ALONE, 62)).build());
            b.addAction(new Notification.Action.Builder(null, "في جماعة",
                    PrayedReceiver.intent(this, prayer, day, PrayerLog.JAMAA, 63)).build());
        }
        return b.build();
    }

    private void play(int prayer) {
        releasePlayer();
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            boolean loaded = false;
            String asset = Adhans.fileFor(prayer, Prefs.adhanVoice(this), Prefs.fajrAdhan(this));
            try (AssetFileDescriptor afd = getAssets().openFd(asset)) {
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                loaded = true;
            } catch (IOException noFile) {
                // ملف الأذان مش موجود: نستخدم نغمة المنبه
            }
            if (!loaded) {
                Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarm == null) alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                mp.setDataSource(this, alarm);
            }
            mp.setOnCompletionListener(p -> finish());
            mp.setOnErrorListener((p, what, extra) -> {
                AdhanCheck.record(this, "الصوت وقف بخطأ " + what + "/" + extra);
                finish();
                return true;
            });
            mp.prepare();
            player = mp;
            startExtras(mp);
            mp.start();
            int vol = AdhanCheck.alarmVolume(this);
            AdhanCheck.record(this, (loaded ? "الأذان اشتغل" : "ملف الأذان مش موجود: اشتغلت نغمة المنبّه")
                    + (vol == 0 ? " — بس صوت المنبّه على صفر" : ""));
        } catch (Exception e) {
            AdhanCheck.record(this, "فشل تشغيل صوت الأذان (" + e.getClass().getSimpleName() + ")");
            stopExtras();
            if (player == mp) player = null;
            mp.release();
            finish();
        }
    }

    private void releasePlayer() {
        stopExtras();
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
    }

    private void finish() {
        releasePlayer();
        if (running == this) running = null;
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (running == this) running = null;
        releasePlayer();
        super.onDestroy();
    }
}
