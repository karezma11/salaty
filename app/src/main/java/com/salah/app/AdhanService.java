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

/**
 * خدمة تشغيل الأذان كامل في الخلفية.
 * بتشغل ملف الأذان المختار من assets/adhan، ولو مش موجود بتشغل نغمة المنبه.
 */
public class AdhanService extends Service {

    public static final String ACTION_STOP = "com.salah.app.STOP_ADHAN";
    private static final int NOTIF_ID = 1;

    private MediaPlayer player;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int prayer = intent == null ? PrayerTimes.FAJR : intent.getIntExtra("prayer", PrayerTimes.FAJR);
        boolean stop = intent != null && ACTION_STOP.equals(intent.getAction());

        Notification n = buildNotification(prayer);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, n);
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
        return new Notification.Builder(this, Notifications.CH_ADHAN)
                .setSmallIcon(R.drawable.ic_notification)
                // لون غامق ثابت: مع setColorized أندرويد بيختار لون النص حسب
                // الخلفية، والأخضر الفاتح كان بيخليه يختار نص غامق صعب القراءة
                .setColor(androidx.core.content.ContextCompat.getColor(
                        this, R.color.notif_adhan_bg))
                .setColorized(true)
                .setSubText(Prefs.city(this))
                .setContentTitle("حان الآن موعد صلاة " + PrayerTimes.NAMES[prayer])
                .setContentText(Adhans.NAMES[Prefs.adhanVoice(this)])
                .setContentIntent(Notifications.openApp(this))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setOngoing(true)
                .addAction(stopAction)
                .build();
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
                finish();
                return true;
            });
            mp.prepare();
            mp.start();
            player = mp;
        } catch (Exception e) {
            mp.release();
            finish();
        }
    }

    private void releasePlayer() {
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
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
