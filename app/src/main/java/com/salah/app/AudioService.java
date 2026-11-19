package com.salah.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** تخلّي أصوات التطبيق شغالة والشاشة مقفولة، مع تحكّم من الإشعار. */
public class AudioService extends Service implements Audio.Listener {

    public static final String ACTION_SHOW = "com.salah.app.AUDIO_SHOW";
    public static final String ACTION_TOGGLE = "com.salah.app.AUDIO_TOGGLE";
    public static final String ACTION_STOP = "com.salah.app.AUDIO_STOP";

    private static final int NOTIF_ID = 7;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SHOW : intent.getAction();
        if (action == null) action = ACTION_SHOW;

        Audio.setWatcher(this);
        startInForeground();

        if (ACTION_TOGGLE.equals(action)) {
            Audio.toggle();
        } else if (ACTION_STOP.equals(action)) {
            Audio.stop();
            return START_NOT_STICKY;
        }
        if (!Audio.isActive()) finish();
        return START_NOT_STICKY;
    }

    private void startInForeground() {
        Notification n = build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    private PendingIntent self(String action, int req) {
        return PendingIntent.getService(this, req,
                new Intent(this, AudioService.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification build() {
        Notifications.ensureChannels(this);
        boolean playing = Audio.isPlaying();
        return new Notification.Builder(this, Notifications.CH_RECITATION)
                .setSmallIcon(R.drawable.ic_notification_play)
                .setColor(Notifications.accent(this))
                .setContentTitle(Audio.title().isEmpty() ? "صلاتي" : Audio.title())
                .setContentText(Audio.subtitle())
                .setContentIntent(Notifications.openApp(this))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null,
                        playing ? "إيقاف مؤقت" : "تشغيل", self(ACTION_TOGGLE, 51)).build())
                .addAction(new Notification.Action.Builder(null,
                        "إنهاء", self(ACTION_STOP, 52)).build())
                .build();
    }

    private void update() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, build());
    }

    private void finish() {
        Audio.setWatcher(null);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onAudioChanged() {
        if (Audio.isActive()) {
            update();
        } else {
            finish();
        }
    }

    @Override
    public void onDestroy() {
        Audio.setWatcher(null);
        super.onDestroy();
    }
}
