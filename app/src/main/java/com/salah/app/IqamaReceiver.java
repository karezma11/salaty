package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** تذكير بوقت الإقامة بعد الأذان. */
public class IqamaReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int prayer = intent.getIntExtra("prayer", PrayerTimes.FAJR);
        Notifications.show(context, Notifications.CH_REMINDER, 30,
                "أُقيمت صلاة " + PrayerTimes.NAMES[prayer],
                "حيّ على الصلاة، حيّ على الفلاح");
    }
}
