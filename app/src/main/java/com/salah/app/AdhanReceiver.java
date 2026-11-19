package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** يشتغل وقت الصلاة: يبدأ تشغيل الأذان ويجدول الصلاة اللي بعدها. */
public class AdhanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int prayer = intent.getIntExtra("prayer", PrayerTimes.FAJR);
        if (Prefs.adhanEnabled(context)) {
            Intent service = new Intent(context, AdhanService.class).putExtra("prayer", prayer);
            try {
                context.startForegroundService(service);
            } catch (RuntimeException e) {
                // النظام منع التشغيل في الخلفية: نكتفي بإشعار مكتوب
                Notifications.show(context, Notifications.CH_REMINDER, 20,
                        "حان الآن موعد صلاة " + PrayerTimes.NAMES[prayer], Prefs.city(context));
            }
        }
        Silence.start(context);
        Scheduler.scheduleIqama(context, prayer);
        Scheduler.scheduleAdhan(context);
        Scheduler.schedulePreAdhan(context);
        Notifications.updateTimes(context);
        TimesWidget.refresh(context);
    }
}
