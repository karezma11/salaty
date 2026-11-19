package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** المستخدم (أو «مسح الكل») شال إشعار المواقيت: نرجّعه طول ما الخيار مفعّل. */
public class TimesRepostReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Notifications.updateTimes(context);
    }
}
