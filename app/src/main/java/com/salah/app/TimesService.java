package com.salah.app;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/**
 * خدمة بتثبّت إشعار المواقيت: إشعار عادي أندرويد ١٤ بيسيبه يتمسح بالسحب أو «مسح الكل»،
 * وموبايلات كتير بتقفل التطبيق وتمسح إشعاراته لما يتقفل من قايمة التطبيقات.
 * الخدمة مش بتعمل أي شغل (ولا بتصحّي الموبايل)، دورها إن الإشعار يفضل ثابت.
 */
public class TimesService extends Service {

    private static volatile boolean running;

    /** يشغّل الخدمة لو مش شغالة. لو النظام منع التشغيل من الخلفية، الإشعار العادي بيفضل. */
    static void ensure(Context c) {
        if (running) return;
        try {
            c.startForegroundService(new Intent(c, TimesService.class));
        } catch (Exception ignored) {
            // ForegroundServiceStartNotAllowedException: هتشتغل أول ما التطبيق يتفتح أو ييجي أذان
        }
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, TimesService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = Notifications.lastTimes;
        if (n == null || !Prefs.persistentTimes(this)) {
            // لازم startForeground قبل الإيقاف وإلا أندرويد بيقفل التطبيق
            Notifications.updateTimes(this);
            n = Notifications.lastTimes;
        }
        try {
            if (n != null) {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(Notifications.timesId(), n,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else {
                    startForeground(Notifications.timesId(), n);
                }
                running = true;
            }
        } catch (Exception e) {
            running = false;
        }
        if (!running || !Prefs.persistentTimes(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
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
