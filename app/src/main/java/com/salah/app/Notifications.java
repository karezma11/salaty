package com.salah.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** قنوات الإشعارات والإشعار الدائم بمواقيت اليوم. */
public final class Notifications {

    public static final String CH_ADHAN = "adhan";
    public static final String CH_REMINDER = "reminder";
    // أندرويد مابيسمحش بتعديل قناة بعد إنشائها، فأي تغيير في إعداداتها
    // محتاج معرّف جديد — وإلا يفضل التطبيق شغال بإعدادات أول تثبيت
    public static final String CH_RECITATION = "recitation_v2";
    public static final String CH_TIMES = "times_v3";
    public static final String CH_UPDATES = "updates";

    private static final int ID_TIMES = 5;

    private Notifications() {
    }

    /** لون التطبيق في الإشعارات. */
    static int accent(Context c) {
        try {
            return androidx.core.content.ContextCompat.getColor(c, R.color.green_700);
        } catch (Exception e) {
            return 0xFF0B5D3B;
        }
    }

    public static void ensureChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;

        // تنظيف القنوات القديمة
        try {
            nm.deleteNotificationChannel("recitation");
            nm.deleteNotificationChannel("times");
            nm.deleteNotificationChannel("times_v2");
        } catch (Exception ignored) {
        }

        NotificationChannel adhan = new NotificationChannel(CH_ADHAN, "الأذان",
                NotificationManager.IMPORTANCE_HIGH);
        adhan.setSound(null, null); // الصوت بيشغله التطبيق نفسه
        nm.createNotificationChannel(adhan);

        NotificationChannel reminder = new NotificationChannel(CH_REMINDER, "التذكير",
                NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(reminder);

        NotificationChannel recitation = new NotificationChannel(CH_RECITATION, "التلاوة",
                NotificationManager.IMPORTANCE_LOW);
        recitation.setSound(null, null);
        recitation.setShowBadge(false);
        nm.createNotificationChannel(recitation);

        // IMPORTANCE_LOW كان بيحطّه في قسم "Silent" آخر الشريط فمحدش بيشوفه.
        // DEFAULT بيخليه مع الإشعارات العادية، وبنقفل الصوت والاهتزاز يدويًا
        // عشان يفضل هادي.
        NotificationChannel times = new NotificationChannel(CH_TIMES, "مواقيت اليوم",
                NotificationManager.IMPORTANCE_DEFAULT);
        times.setSound(null, null);
        times.enableVibration(false);
        times.enableLights(false);
        times.setShowBadge(false);
        nm.createNotificationChannel(times);

        NotificationChannel updates = new NotificationChannel(CH_UPDATES, "تحديثات التطبيق",
                NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(updates);
    }

    public static PendingIntent openApp(Context c) {
        return PendingIntent.getActivity(c, 10, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent openSura(Context c, int sura) {
        Intent i = new Intent(c, SuraActivity.class)
                .putExtra(SuraActivity.EXTRA_SURA, sura)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 12, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void showWith(Context c, String channel, int id, String title, String text,
                                PendingIntent intent) {
        ensureChannels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        Notification n = new Notification.Builder(c, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(accent(c))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(intent != null ? intent : openApp(c))
                .setAutoCancel(true)
                .setShowWhen(false)
                .build();
        nm.notify(id, n);
    }

    public static void show(Context c, String channel, int id, String title, String text) {
        ensureChannels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        Notification n = new Notification.Builder(c, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(accent(c))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(openApp(c))
                .setAutoCancel(true)
                .setShowWhen(false)
                .build();
        nm.notify(id, n);
    }

    private static final int[] NAME_IDS = {
            R.id.pName0, R.id.pName1, R.id.pName2, R.id.pName3, R.id.pName4, R.id.pName5};
    private static final int[] TIME_IDS = {
            R.id.pTime0, R.id.pTime1, R.id.pTime2, R.id.pTime3, R.id.pTime4, R.id.pTime5};
    private static final int[] DOT_IDS = {
            R.id.pDot0, R.id.pDot1, R.id.pDot2, R.id.pDot3, R.id.pDot4, R.id.pDot5};

    /** إشعار دائم فيه الصلاة القادمة ومواقيت اليوم كلها، بتصميم خاص بالتطبيق. */
    public static void updateTimes(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (!Prefs.persistentTimes(c)) {
            nm.cancel(ID_TIMES);
            return;
        }
        ensureChannels(c);
        try {
            double[] t = Scheduler.timesFor(c, 0);
            long[] next = Scheduler.nextPrayer(c);
            int idx = (int) next[0];

            long msLeft = Math.max(0, next[1] - System.currentTimeMillis());
            // الـ Chronometer بيعدّ لوحده كل ثانية، فالوقت فضل صح من غير تحديثات
            long base = android.os.SystemClock.elapsedRealtime() + msLeft;
            long mins = msLeft / 60000L;
            String when;
            if (mins >= 60) {
                long h = mins / 60;
                long m = mins % 60;
                when = "بعد " + Ui.arabicDigits(h) + (h == 1 ? " ساعة" : " ساعات");
                if (m > 0) when += " و" + Ui.arabicDigits(m) + " دقيقة";
            } else {
                when = "بعد " + Ui.arabicDigits(mins) + " دقيقة";
            }

            String name = PrayerTimes.NAMES[idx];
            String time = PrayerTimes.format(t[idx]);

            // التصميم المخصص اختياري: لو فشل لأي سبب بنكمّل بالتصميم العادي
            // بدل ما الإشعار يختفي خالص
            android.widget.RemoteViews small = null;
            android.widget.RemoteViews big = null;
            try {
                small = new android.widget.RemoteViews(
                        c.getPackageName(), R.layout.notif_times_small);
                small.setTextViewText(R.id.nName, name);
                small.setTextViewText(R.id.nTime, time);
                small.setChronometerCountDown(R.id.nLeft, true);
                small.setChronometer(R.id.nLeft, base, "باقي %s", true);

                big = new android.widget.RemoteViews(
                        c.getPackageName(), R.layout.notif_times_big);
                big.setTextViewText(R.id.nName, name);
                big.setTextViewText(R.id.nTime, time);
                big.setChronometerCountDown(R.id.nLeft, true);
                big.setChronometer(R.id.nLeft, base, "باقي %s", true);
                for (int i = 0; i < 6; i++) {
                    big.setTextViewText(NAME_IDS[i], PrayerTimes.NAMES[i]);
                    big.setTextViewText(TIME_IDS[i], PrayerTimes.format(t[i]));
                    // نقطة صغيرة تحت الصلاة القادمة بدل تمييز صارخ
                    big.setViewVisibility(DOT_IDS[i],
                            i == idx ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
                }
            } catch (Throwable e) {
                small = null;
                big = null;
            }

            // نص بديل للأجهزة أو الواجهات اللي مش بتعرض التصميم المخصص
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                if (i > 0) all.append("   ");
                all.append(PrayerTimes.NAMES[i]).append(' ').append(PrayerTimes.format(t[i]));
            }

            Notification.Builder b = new Notification.Builder(c, CH_TIMES)
                    .setSmallIcon(R.drawable.ic_notification_times)
                    .setColor(accent(c))
                    .setSubText(Hijri.today(c).text())
                    .setContentTitle(name + "  " + time)
                    .setContentText(when + "  ·  " + all)
                    .setContentIntent(openApp(c))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false);

            if (small != null && big != null) {
                b.setCustomContentView(small)
                        .setCustomBigContentView(big)
                        .setStyle(new Notification.DecoratedCustomViewStyle());
            } else {
                b.setStyle(new Notification.BigTextStyle().bigText(all.toString()));
            }

            try {
                post(c, nm, b.build());
            } catch (Throwable e) {
                // آخر خط دفاع: إشعار بسيط مضمون يظهر
                post(c, nm, new Notification.Builder(c, CH_TIMES)
                        .setSmallIcon(R.drawable.ic_notification_times)
                        .setColor(accent(c))
                        .setContentTitle(name + "  " + time)
                        .setContentText(all.toString())
                        .setStyle(new Notification.BigTextStyle().bigText(all.toString()))
                        .setContentIntent(openApp(c))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setShowWhen(false)
                        .build());
            }
        } catch (Throwable e) {
            // بدل ما الإشعار يختفي من غير سبب، نعرض الخطأ نفسه
            try {
                post(c, nm, new Notification.Builder(c, CH_TIMES)
                        .setSmallIcon(R.drawable.ic_notification_times)
                        .setContentTitle("تعذّر عرض المواقيت")
                        .setContentText(String.valueOf(e))
                        .setStyle(new Notification.BigTextStyle()
                                .bigText(String.valueOf(e) + "\n" + trace(e)))
                        .setContentIntent(openApp(c))
                        .setOnlyAlertOnce(true)
                        .setShowWhen(false)
                        .build());
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * بيعرض إشعار المواقيت. أندرويد ١٤+ بيسيب الإشعار ده يتسحب أو يتمسح مع «مسح الكل»،
     * فالـ deleteIntent بيرجّعه على طول (من غير خدمة foreground: 3.8 كانت بتقع بيها).
     */
    private static void post(Context c, NotificationManager nm, Notification n) {
        n.deleteIntent = PendingIntent.getBroadcast(c, 13,
                new Intent(c, TimesRepostReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        nm.notify(ID_TIMES, n);
    }

    /** تشخيص: بيقول إذا كان الإشعار اتسجّل فعلًا في النظام ولا لأ. */
    public static String diagnose(Context c) {
        StringBuilder sb = new StringBuilder();
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return "مفيش NotificationManager";
        sb.append("الإشعارات مسموحة: ")
                .append(nm.areNotificationsEnabled() ? "نعم" : "لا").append('\n');
        sb.append("الخيار في التطبيق: ")
                .append(Prefs.persistentTimes(c) ? "مفعّل" : "متوقف").append('\n');
        ensureChannels(c);
        NotificationChannel ch = nm.getNotificationChannel(CH_TIMES);
        sb.append("القناة: ").append(ch == null ? "غير موجودة"
                : ("أهمية " + ch.getImportance()
                   + (ch.getImportance() == NotificationManager.IMPORTANCE_NONE
                        ? " (مغلقة)" : ""))).append('\n');

        updateTimes(c);

        boolean found = false;
        String style = "-";
        try {
            for (android.service.notification.StatusBarNotification sbn
                    : nm.getActiveNotifications()) {
                if (sbn.getId() == ID_TIMES) {
                    found = true;
                    Notification n = sbn.getNotification();
                    style = String.valueOf(n.extras.getString(Notification.EXTRA_TEMPLATE));
                    break;
                }
            }
        } catch (Throwable e) {
            style = "تعذّر الفحص: " + e;
        }
        sb.append("الإشعار مسجّل في النظام: ").append(found ? "نعم" : "لا").append('\n');
        sb.append("القالب: ").append(style);
        return sb.toString();
    }

    private static String trace(Throwable e) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] st = e.getStackTrace();
        for (int i = 0; i < Math.min(4, st.length); i++) {
            sb.append(st[i].toString()).append('\n');
        }
        return sb.toString();
    }
}
