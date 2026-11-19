package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** تنبيه قبل الأذان بمدة يحددها المستخدم. */
public class PreAdhanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int prayer = intent.getIntExtra("prayer", PrayerTimes.FAJR);
        Scheduler.schedulePreAdhan(context);
        if (Prefs.preAdhanMinutes(context) <= 0) return;
        Notifications.show(context, Notifications.CH_REMINDER, 70,
                "قرب موعد " + PrayerTimes.NAMES[prayer],
                "باقي " + Ui.arabicDigits(Prefs.preAdhanMinutes(context)) + " دقيقة على الأذان");
    }
}
