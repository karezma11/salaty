package com.salah.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import java.util.List;

/** تخلّي التلاوة شغالة في الخلفية، وتعرض تحكّم في الإشعارات وشاشة القفل. */
public class RecitationService extends Service implements RecitationPlayer.Listener {

    public static final String ACTION_SHOW = "com.salah.app.REC_SHOW";
    public static final String ACTION_TOGGLE = "com.salah.app.REC_TOGGLE";
    public static final String ACTION_NEXT = "com.salah.app.REC_NEXT";
    public static final String ACTION_STOP = "com.salah.app.REC_STOP";

    private static final int NOTIF_ID = 2;

    private String suraName = "";
    private MediaNotif media;

    // التحكم من شاشة القفل أو سماعة البلوتوث بيعدّي بنفس طريق أزرار الإشعار
    private final MediaNotif.Controls controls = new MediaNotif.Controls() {
        @Override
        public void toggle() {
            startService(new Intent(RecitationService.this, RecitationService.class)
                    .setAction(ACTION_TOGGLE));
        }

        @Override
        public void next() {
            startService(new Intent(RecitationService.this, RecitationService.class)
                    .setAction(ACTION_NEXT));
        }

        @Override
        public void stop() {
            startService(new Intent(RecitationService.this, RecitationService.class)
                    .setAction(ACTION_STOP));
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SHOW : intent.getAction();
        if (action == null) action = ACTION_SHOW;

        RecitationPlayer.setWatcher(this);
        startInForeground();

        if (ACTION_TOGGLE.equals(action)) {
            if (RecitationPlayer.isPlaying()) {
                RecitationPlayer.pause();
            } else {
                RecitationPlayer.resume();
            }
            update();
        } else if (ACTION_NEXT.equals(action)) {
            RecitationPlayer.jumpTo(RecitationPlayer.currentAyah() + 1);
        } else if (ACTION_STOP.equals(action)) {
            RecitationPlayer.stop();
            return START_NOT_STICKY;
        }
        if (!RecitationPlayer.isActive()) {
            finish();
        }
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

    private String name() {
        int s = RecitationPlayer.currentSura();
        if (s < 1) return suraName;
        List<Quran.Sura> all = Quran.suras(this);
        if (s <= all.size()) suraName = all.get(s - 1).name;
        return suraName;
    }

    private PendingIntent self(String action, int req) {
        Intent i = new Intent(this, RecitationService.class).setAction(action);
        return PendingIntent.getService(this, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification build() {
        Notifications.ensureChannels(this);
        if (media == null) media = new MediaNotif(this, "salaty-recitation", controls);
        return media.build(this, Notifications.CH_RECITATION,
                "سورة " + name(),
                "الآية " + Ui.arabicDigits(RecitationPlayer.currentAyah())
                        + " من " + Ui.arabicDigits(RecitationPlayer.lastAyah())
                        + " · " + Reciters.current(this).name,
                RecitationPlayer.isPlaying(),
                Notifications.openSura(this, RecitationPlayer.currentSura()),
                self(ACTION_TOGGLE, 21), self(ACTION_NEXT, 22), self(ACTION_STOP, 23));
    }

    private void update() {
        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, build());
    }

    private void finish() {
        RecitationPlayer.setWatcher(null);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onAyah(int sura, int ayah) {
        update();
    }

    @Override
    public void onStopped() {
        finish();
    }

    @Override
    public void onDestroy() {
        RecitationPlayer.setWatcher(null);
        if (media != null) media.release();
        super.onDestroy();
    }
}
