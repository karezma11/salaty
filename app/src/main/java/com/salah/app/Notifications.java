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
    /** منبّه السحور: أهمية عالية وصوت المنبّه، عشان يصحّي. */
    public static final String CH_SUHOOR = "suhoor";

    private static final int ID_TIMES = 5;

    private Notifications() {
    }

    /**
     * أيقونة شريط الحالة (جنب الساعة): الهلال الأبيض. جرّبنا في 3.12 أيقونة ملوّنة على vivo
     * وطلعت دايرة بيضا: النظام بياخد شكلها بس ويلوّنه، زي أندرويد العادي.
     */
    static int smallIcon(Context c) {
        return R.drawable.ic_notification;
    }

    /** أيقونة التطبيق بألوانها (هلال دهبي على أخضر) لجنب الإشعار. */
    private static android.graphics.Bitmap appIcon;

    static synchronized android.graphics.Bitmap appIcon(Context c) {
        if (appIcon != null) return appIcon;
        try {
            android.graphics.drawable.Drawable d = c.getPackageManager()
                    .getApplicationIcon(c.getPackageName());
            int size = 192;
            appIcon = android.graphics.Bitmap.createBitmap(size, size,
                    android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas cv = new android.graphics.Canvas(appIcon);
            d.setBounds(0, 0, size, size);
            d.draw(cv);
        } catch (Throwable e) {
            appIcon = null;
        }
        return appIcon;
    }

    /** لون التطبيق في الإشعارات. */
    static int accent(Context c) {
        try {
            return androidx.core.content.ContextCompat.getColor(c, R.color.ic_launcher_background);
        } catch (Exception e) {
            return 0xFF073D28;
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

        NotificationChannel suhoor = new NotificationChannel(CH_SUHOOR, "منبّه السحور",
                NotificationManager.IMPORTANCE_HIGH);
        android.net.Uri alarm = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_ALARM);
        if (alarm == null) {
            alarm = android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_NOTIFICATION);
        }
        suhoor.setSound(alarm, new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        suhoor.enableVibration(true);
        nm.createNotificationChannel(suhoor);
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
                .setSmallIcon(Notifications.smallIcon(c))
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
                .setSmallIcon(Notifications.smallIcon(c))
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

            // «صلّيت»: الصلاة اللي وقتها دخل (العشاء بعد نص الليل = بتاعة امبارح)
            int[] cur = PrayerLog.current(idx, PrayerLog.isToday(next[1]));
            String curDay = Progress.dayKey(cur[1]);
            boolean curPending = PrayerLog.stateOn(c, curDay, cur[0]) == PrayerLog.NOT_PRAYED;

            // تصميم أندرويد العادي، من غير RemoteViews: على vivo (Funtouch 15) الإشعار
            // بالتصميم المخصص كان بيتسجّل وبعدين واجهة النظام بتشيله ومابيظهرش خالص.
            Notification.InboxStyle inbox = new Notification.InboxStyle()
                    .setBigContentTitle("الصلاة القادمة: " + name + "  " + time);
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                String line = PrayerTimes.NAMES[i] + "   " + PrayerTimes.format(t[i]);
                inbox.addLine(i == idx ? "◂ " + line : "    " + line);
                if (i > 0) all.append("   ");
                all.append(PrayerTimes.NAMES[i]).append(' ').append(PrayerTimes.format(t[i]));
            }
            inbox.setSummaryText(Hijri.today(c).text());

            Notification.Builder b = new Notification.Builder(c, CH_TIMES)
                    .setSmallIcon(Notifications.smallIcon(c))
                    .setColor(accent(c))
                    .setContentTitle(name + "  " + time)
                    .setContentText(when + "  ·  " + TimesCard.hijriLine(c))
                    .setLargeIcon(appIcon(c))
                    .setStyle(inbox)
                    .setContentIntent(openApp(c))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    // عدّاد تنازلي لحد الصلاة الجاية في رأس الإشعار، بيعدّ لوحده
                    .setWhen(next[1])
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true);

            // زرارين للصلاة اللي لسه ماتسجّلتش، وبيختفوا بعد التسجيل لحد الصلاة اللي بعدها
            if (curPending) {
                b.addAction(new Notification.Action.Builder(null,
                        "صلّيت " + PrayerTimes.NAMES[cur[0]],
                        PrayedReceiver.intent(c, cur[0], curDay, PrayerLog.ALONE, 60)).build());
                b.addAction(new Notification.Action.Builder(null, "في جماعة",
                        PrayedReceiver.intent(c, cur[0], curDay, PrayerLog.JAMAA, 61)).build());
            }

            // الشكل «س»: صورة بالتاريخ الهجري والصلوات برموزها ودعاء، لما الإشعار يتفتح.
            // لو الرسم فشل لأي سبب بتفضل القائمة العادية.
            try {
                android.graphics.Bitmap card = TimesCard.render(c, t, idx, cur[1]);
                b.setStyle(new Notification.BigPictureStyle()
                        .bigPicture(card)
                        .bigLargeIcon((android.graphics.Bitmap) null)
                        .setBigContentTitle(name + "  " + time)
                        .setSummaryText(when));
            } catch (Throwable ignored) {
            }

            try {
                post(c, nm, b.build());
            } catch (Throwable e) {
                // آخر خط دفاع: إشعار بسيط مضمون يظهر
                post(c, nm, new Notification.Builder(c, CH_TIMES)
                        .setSmallIcon(Notifications.smallIcon(c))
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
                        .setSmallIcon(Notifications.smallIcon(c))
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
