package com.salah.app;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** كتم الموبايل أثناء الصلاة، بإذن من المستخدم. */
public final class Silence {

    private Silence() {
    }

    /** هل التطبيق مسموح له يغيّر وضع عدم الإزعاج؟ */
    public static boolean allowed(Context c) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            return nm != null && nm.isNotificationPolicyAccessGranted();
        } catch (Exception e) {
            return false;
        }
    }

    /** يفتح شاشة النظام عشان المستخدم يدّي الإذن بنفسه. */
    public static void askPermission(Context c) {
        try {
            c.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
        }
    }

    /** يكتم الموبايل ويجدول إرجاعه بعد المدة المحددة. */
    public static void start(Context c) {
        if (!Prefs.silencePrayer(c) || !allowed(c)) return;
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;
            // نحفظ الوضع الحالي عشان نرجّعه زي ما كان
            Prefs.setSavedFilter(c, nm.getCurrentInterruptionFilter());
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            Scheduler.scheduleSilenceEnd(c);
        } catch (Exception ignored) {
        }
    }

    /** يرجّع الموبايل لوضعه الأصلي. */
    public static void stop(Context c) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null || !allowed(c)) return;
            int saved = Prefs.savedFilter(c);
            if (saved <= 0) saved = NotificationManager.INTERRUPTION_FILTER_ALL;
            nm.setInterruptionFilter(saved);
            Prefs.setSavedFilter(c, 0);
        } catch (Exception ignored) {
        }
    }
}
