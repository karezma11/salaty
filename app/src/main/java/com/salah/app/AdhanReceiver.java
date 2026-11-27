package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** يشتغل وقت الصلاة: يبدأ تشغيل الأذان ويجدول الصلاة اللي بعدها. */
public class AdhanReceiver extends BroadcastReceiver {

    /** «جرّب الأذان والشاشة مقفولة» من الإعدادات: نفس الطريق من غير جدولة ولا كتم. */
    public static final String EXTRA_TEST = "test";

    @Override
    public void onReceive(Context context, Intent intent) {
        int prayer = intent.getIntExtra("prayer", PrayerTimes.FAJR);
        boolean test = intent.getBooleanExtra(EXTRA_TEST, false);
        // سجل «فحص الأذان»: لو السطر ده مش موجود يبقى النظام ماصحّاش التطبيق خالص
        AdhanCheck.record(context, (test ? "تجربة: " : "")
                + "وصل تنبيه " + PrayerTimes.NAMES[prayer]);
        if (test || Prefs.adhanEnabled(context)) {
            Intent service = new Intent(context, AdhanService.class)
                    .putExtra("prayer", prayer)
                    .putExtra(EXTRA_TEST, test);
            try {
                context.startForegroundService(service);
            } catch (RuntimeException e) {
                // النظام منع التشغيل في الخلفية: نكتفي بإشعار مكتوب
                AdhanCheck.record(context, "النظام منع تشغيل الأذان في الخلفية ("
                        + e.getClass().getSimpleName() + ")");
                Notifications.show(context, Notifications.CH_REMINDER, 20,
                        "حان الآن موعد صلاة " + PrayerTimes.NAMES[prayer], Prefs.city(context));
            }
        }
        if (test) return;
        Silence.start(context);
        Scheduler.scheduleIqama(context, prayer);
        Scheduler.scheduleAdhan(context);
        Scheduler.schedulePreAdhan(context);
        Notifications.updateTimes(context);
        TimesWidget.refresh(context);
    }
}
