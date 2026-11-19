package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** يعيد جدولة التنبيهات بعد إعادة تشغيل الموبايل أو تغيير الوقت. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Scheduler.scheduleAll(context);
        Notifications.updateTimes(context);
        // بعد تثبيت تحديث: نمسح ملف الـ APK الكبير اللي اتنزّل
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Updater.afterUpdate(context);
        }
    }
}
