package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** تذكير دوري بالصلاة على النبي، مع وقت هدوء من 11 بالليل لـ 6 الصبح. */
public class SalawatReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean quiet = hour >= 23 || hour < 6;
        if (!quiet && Prefs.salawatMinutes(context) > 0) {
            Notifications.show(context, Notifications.CH_REMINDER, 30,
                    "صلِّ على النبي ﷺ", "اللهم صلِّ وسلم وبارك على سيدنا محمد");
        }
        Scheduler.scheduleSalawat(context);
    }
}
