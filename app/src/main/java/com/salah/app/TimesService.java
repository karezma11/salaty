package com.salah.app;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/**
 * خدمة بتثبّت إشعار المواقيت على أندرويد ١٤ وأحدث: هناك الإشعار العادي بيتسحب أو
 * بيتمسح مع «مسح الكل»، وموبايلات كتير بتمسح إشعارات التطبيق لما يتقفل.
 * الخدمة مش بتعمل أي شغل (ولا بتصحّي الموبايل)، دورها إن الإشعار يفضل ثابت.
 * أقدم من ١٤: الإشعار الـ ongoing أصلًا مش بيتسحب، فمفيش خدمة (أأمن).
 */
public class TimesService extends Service {

    private static volatile boolean running;

    /** يشغّل الخدمة لو مش شغالة. لو النظام منع التشغيل من الخلفية، الإشعار العادي بيفضل. */
    static void ensure(Context c) {
        if (running || Build.VERSION.SDK_INT < 34) return;
        try {
            c.startForegroundService(new Intent(c, TimesService.class));
        } catch (Throwable ignored) {
            // ممنوع من الخلفية: هتشتغل أول ما التطبيق يتفتح أو ييجي أذان
        }
    }

    static void stop(Context c) {
        try {
            c.stopService(new Intent(c, TimesService.class));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // أندرويد بيقفل التطبيق لو startForegroundService ما اتبعتش بـ startForeground،
        // حتى لو الخدمة وقفت. فلازم startForeground الأول دايمًا، وبعدين نقرر.
        Notification n = Notifications.lastTimes;
        if (n == null) n = placeholder();
        boolean ok = false;
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(Notifications.timesId(), n,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(Notifications.timesId(), n);
            }
            ok = true;
        } catch (Throwable e) {
            try {
                startForeground(Notifications.timesId(), n);
                ok = true;
            } catch (Throwable ignored) {
            }
        }
        running = ok;
        if (!ok || !Prefs.persistentTimes(this)) {
            running = false;
            stopSelf();
            return START_NOT_STICKY;
        }
        if (Notifications.lastTimes == null) Notifications.updateTimes(this);
        return START_STICKY;
    }

    /** إشعار مؤقت لحد ما المواقيت تتحسب. */
    private Notification placeholder() {
        Notifications.ensureChannels(this);
        return new Notification.Builder(this, Notifications.CH_TIMES)
                .setSmallIcon(R.drawable.ic_notification_times)
                .setContentTitle("مواقيت الصلاة")
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
