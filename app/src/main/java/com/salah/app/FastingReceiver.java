package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** تذكير بالصيام المستحب بكرة. */
public class FastingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Scheduler.scheduleFasting(context);
        if (!Prefs.fastingReminder(context)) return;

        Hijri.Date tomorrow = Hijri.plusDays(context, 1);
        String reason = null;
        if (tomorrow.month != 9) {
            if (Hijri.isWhiteDay(tomorrow)) {
                reason = "من الأيام البيض";
            } else if (tomorrow.month == 1 && tomorrow.day == 10) {
                reason = "يوم عاشوراء";
            } else if (tomorrow.month == 12 && tomorrow.day == 9) {
                reason = "يوم عرفة";
            } else {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
                int d = cal.get(java.util.Calendar.DAY_OF_WEEK);
                if (d == java.util.Calendar.MONDAY) reason = "يوم الاثنين";
                else if (d == java.util.Calendar.THURSDAY) reason = "يوم الخميس";
            }
        }
        if (reason == null) return;
        Notifications.show(context, Notifications.CH_REMINDER, 40,
                "صيام مستحب بكرة", "بكرة " + reason + " — " + tomorrow.text());
    }
}
