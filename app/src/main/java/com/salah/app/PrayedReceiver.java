package com.salah.app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * زرار «صلّيت» و«في جماعة» في إشعار الأذان والإشعار الدائم: بيسجّل الصلاة في سجل الصلاة
 * من غير ما يفتح التطبيق، وبيحدّث الإشعارين (الزراير بتختفي وعلامة ✓ بتظهر).
 */
public class PrayedReceiver extends BroadcastReceiver {

    private static final String EXTRA_PRAYER = "prayer";
    private static final String EXTRA_DAY = "day";
    private static final String EXTRA_STATE = "state";

    /** الزرار. req لازم يختلف لكل زرار عشان الـ extras ماتتلخبطش. */
    static PendingIntent intent(Context c, int prayer, String day, int state, int req) {
        Intent i = new Intent(c, PrayedReceiver.class)
                .putExtra(EXTRA_PRAYER, prayer)
                .putExtra(EXTRA_DAY, day)
                .putExtra(EXTRA_STATE, state);
        return PendingIntent.getBroadcast(c, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int prayer = intent.getIntExtra(EXTRA_PRAYER, -1);
        String day = intent.getStringExtra(EXTRA_DAY);
        int state = intent.getIntExtra(EXTRA_STATE, PrayerLog.ALONE);
        if (prayer < 0 || prayer > 5 || prayer == PrayerTimes.SUNRISE || day == null) return;

        PrayerLog.set(context, day, prayer, state);
        AdhanService.refreshIfRunning();
        Notifications.updateTimes(context);
        try {
            Toast.makeText(context, "✓ اتسجّلت صلاة " + PrayerTimes.NAMES[prayer]
                    + " " + PrayerLog.label(state), Toast.LENGTH_SHORT).show();
        } catch (RuntimeException ignored) {
        }
    }
}
