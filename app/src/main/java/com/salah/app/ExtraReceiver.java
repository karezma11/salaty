package com.salah.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** تنبيهات 3.17: السحور، والإفطار، وسورة الكهف يوم الجمعة، وقيام الليل. */
public class ExtraReceiver extends BroadcastReceiver {

    static final String EXTRA_KIND = "kind";
    static final int SUHOOR = 1, IFTAR = 2, KAHF = 3, QIYAM = 4, STOP_SUHOOR = 5;

    static final int ID_SUHOOR = 90, ID_IFTAR = 91, ID_KAHF = 92, ID_QIYAM = 93;

    @Override
    public void onReceive(Context c, Intent intent) {
        int kind = intent.getIntExtra(EXTRA_KIND, 0);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (kind == STOP_SUHOOR) {
            if (nm != null) nm.cancel(ID_SUHOOR);
            return;
        }
        try {
            switch (kind) {
                case SUHOOR:
                    suhoor(c, nm);
                    break;
                case IFTAR:
                    Notifications.show(c, Notifications.CH_REMINDER, ID_IFTAR,
                            "حان وقت الإفطار", Ramadan.IFTAR_DUA);
                    break;
                case KAHF:
                    kahf(c, nm);
                    break;
                case QIYAM:
                    Notifications.show(c, Notifications.CH_REMINDER, ID_QIYAM,
                            "بدأ الثلث الأخير من الليل",
                            "«ينزل ربنا تبارك وتعالى كل ليلة إلى السماء الدنيا حين يبقى ثلث الليل الآخر»");
                    break;
                default:
                    break;
            }
        } finally {
            Scheduler.scheduleExtras(c);
        }
    }

    /** منبّه السحور: بيرن بصوت المنبّه لحد ما تدوس «إيقاف» أو تفتحه. */
    private static void suhoor(Context c, NotificationManager nm) {
        if (nm == null) return;
        Notifications.ensureChannels(c);
        double[] t = Scheduler.timesFor(c, 0);
        String imsak = PrayerTimes.format(Ramadan.imsak(t[PrayerTimes.FAJR]));
        int mins = Prefs.suhoorMinutes(c);
        String left = mins >= 60 && mins % 60 == 0
                ? (mins == 60 ? "ساعة" : Ui.arabicDigits(mins / 60) + " ساعة")
                : mins == 90 ? "ساعة ونص" : Ui.arabicDigits(mins) + " دقيقة";
        PendingIntent stop = PendingIntent.getBroadcast(c, 95,
                new Intent(c, ExtraReceiver.class).putExtra(EXTRA_KIND, STOP_SUHOOR),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, Notifications.CH_SUHOOR)
                .setSmallIcon(Notifications.smallIcon(c))
                .setColor(Notifications.accent(c))
                .setContentTitle("السحور · باقي " + left + " على الإمساك")
                .setContentText("الإمساك " + imsak + " · الفجر "
                        + PrayerTimes.format(t[PrayerTimes.FAJR]))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(Notifications.openApp(c))
                .setDeleteIntent(stop)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(null, "إيقاف", stop).build())
                .build();
        // يفضل يرن لحد ما المستخدم يوقفه، زي المنبّه
        n.flags |= Notification.FLAG_INSISTENT;
        nm.notify(ID_SUHOOR, n);
    }

    private static void kahf(Context c, NotificationManager nm) {
        if (nm == null) return;
        Notifications.ensureChannels(c);
        Intent read = new Intent(c, SuraActivity.class)
                .putExtra(SuraActivity.EXTRA_SURA, 18)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent play = new Intent(read).putExtra(SuraActivity.EXTRA_PLAY, true);
        PendingIntent readPi = PendingIntent.getActivity(c, 96, read,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playPi = PendingIntent.getActivity(c, 97, play,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = "«من قرأ سورة الكهف في يوم الجمعة أضاء له من النور ما بين الجمعتين»";
        Notification.Style style = new Notification.BigTextStyle().bigText(text);
        try {
            // الشكل «أ»: صورة خضرا فيها مدخل الكهف وآية، زي صورة إشعار المواقيت
            style = new Notification.BigPictureStyle().bigPicture(kahfCard(c))
                    .setSummaryText("نور ما بين الجمعتين");
        } catch (Throwable ignored) {
        }
        Notification n = new Notification.Builder(c, Notifications.CH_REMINDER)
                .setSmallIcon(Notifications.smallIcon(c))
                .setColor(Notifications.accent(c))
                .setSubText("الجمعة")
                .setContentTitle("سورة الكهف")
                .setContentText(text)
                .setLargeIcon(Notifications.appIcon(c))
                .setStyle(style)
                .setContentIntent(readPi)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(null, "اقرأ", readPi).build())
                .addAction(new Notification.Action.Builder(null, "شغّل التلاوة", playPi).build())
                .build();
        nm.notify(ID_KAHF, n);
    }

    /** صورة إشعار الكهف: مدخل كهف على أخضر غامق، واسم السورة دهبي، وآية تحت. */
    static android.graphics.Bitmap kahfCard(Context c) {
        int w = 960, h = 400;
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h,
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas cv = new android.graphics.Canvas(bmp);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF0B3B2C);
        cv.drawRoundRect(new android.graphics.RectF(0, 0, w, h), 40, 40, p);
        // الجبل ومدخل الكهف
        android.graphics.Path hill = new android.graphics.Path();
        hill.moveTo(60, h);
        hill.quadTo(120, 90, w / 2f, 80);
        hill.quadTo(w - 120, 90, w - 60, h);
        hill.close();
        p.setColor(0xFF13513D);
        cv.drawPath(hill, p);
        android.graphics.Path cave = new android.graphics.Path();
        cave.moveTo(w / 2f - 130, h);
        cave.quadTo(w / 2f - 110, 190, w / 2f, 180);
        cave.quadTo(w / 2f + 110, 190, w / 2f + 130, h);
        cave.close();
        p.setColor(0xFF06261C);
        cv.drawPath(cave, p);

        android.graphics.Typeface font = null, bold = null;
        try {
            font = androidx.core.content.res.ResourcesCompat.getFont(c, R.font.amiri_regular);
            bold = androidx.core.content.res.ResourcesCompat.getFont(c, R.font.amiri_bold);
        } catch (Exception ignored) {
        }
        android.graphics.Paint t = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        t.setTextAlign(android.graphics.Paint.Align.CENTER);
        t.setColor(0xFFE2B04A);
        t.setTextSize(50);
        if (bold != null) t.setTypeface(bold);
        cv.drawText("سورة الكهف", w / 2f, 62, t);
        t.setColor(0xFF9FE1CB);
        t.setTextSize(38);
        if (font != null) t.setTypeface(font);
        cv.drawText("إِنَّهُمْ فِتْيَةٌ آمَنُوا بِرَبِّهِمْ وَزِدْنَاهُمْ هُدًى", w / 2f, h - 40, t);
        return bmp;
    }
}
