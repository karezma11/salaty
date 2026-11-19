package com.salah.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** DownloadManager بيبعت هنا لما تنزيل التحديث يخلص، حتى لو التطبيق مقفول. */
public class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        Context app = context.getApplicationContext();
        // فحص توقيع ملف ٣٠٠ ميجا بياخد ثواني، فبيتعمل برّه الخيط الرئيسي
        PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                // Updater بيتأكد إن الرقم ده بتاع تنزيلنا قبل ما يعمل أي حاجة
                Updater.onComplete(app, id);
            } finally {
                pending.finish();
            }
        }).start();
    }
}
