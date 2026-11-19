package com.salah.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** يرجّع صوت الموبايل بعد انتهاء مدة الصلاة. */
public class SilenceEndReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Silence.stop(context);
    }
}
