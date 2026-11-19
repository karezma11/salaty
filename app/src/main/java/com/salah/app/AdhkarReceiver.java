package com.salah.app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** تنبيه أذكار الصباح والمساء. */
public class AdhkarReceiver extends BroadcastReceiver {

    public static final String EXTRA_EVENING = "evening";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean evening = intent.getBooleanExtra(EXTRA_EVENING, false);
        Scheduler.scheduleAdhkar(context);
        if (!Prefs.adhkarReminder(context)) return;

        Intent open = new Intent(context, AdhkarActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, evening ? 61 : 60, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notifications.showWith(context, Notifications.CH_REMINDER, evening ? 61 : 60,
                evening ? "أذكار المساء" : "أذكار الصباح",
                evening ? "حصّن نفسك بأذكار المساء" : "ابدأ يومك بأذكار الصباح",
                pi);
    }
}
