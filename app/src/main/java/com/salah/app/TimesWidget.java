package com.salah.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

/** ويدجت الشاشة الرئيسية بشكل الإشعار الدائم. */
public class TimesWidget extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.salah.app.WIDGET_REFRESH";

    /** تحدّث كل نسخ الويدجت. */
    public static void refresh(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, TimesWidget.class));
            if (ids == null || ids.length == 0) return;
            for (int id : ids) {
                m.updateAppWidget(id, build(c));
            }
        } catch (Exception ignored) {
        }
    }

    /** الشكل «أ» (3.19): نفس الإشعار الدائم — التاريخ الهجري والصلوات برموزها والعدّاد والدعاء و«صلّيت». */
    private static RemoteViews build(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_times);
        try {
            double[] t = Scheduler.timesFor(c, 0);
            long[] next = Scheduler.nextPrayer(c);
            int idx = (int) next[0];

            v.setTextViewText(R.id.wHijri, TimesCard.hijriLine(c));
            v.setTextViewText(R.id.wCity, Prefs.city(c));
            v.setTextViewText(R.id.wNext, PrayerTimes.NAMES[idx] + " بعد");

            // العدّاد بيعدّ لوحده كل ثانية من غير ما يصحّي التطبيق
            long msLeft = Math.max(0, next[1] - System.currentTimeMillis());
            v.setChronometer(R.id.wCount, SystemClock.elapsedRealtime() + msLeft, null, true);
            v.setChronometerCountDown(R.id.wCount, true);

            // «صلّيت»: الصلاة اللي وقتها دخل (العشاء بعد نص الليل = بتاعة امبارح)
            int[] cur = PrayerLog.current(idx, PrayerLog.isToday(next[1]));
            String curDay = Progress.dayKey(cur[1]);
            boolean pending = PrayerLog.stateOn(c, curDay, cur[0]) == PrayerLog.NOT_PRAYED;

            try {
                v.setImageViewBitmap(R.id.wStrip, TimesCard.renderStrip(c, t, idx, cur[1]));
            } catch (Throwable ignored) {
            }
            v.setTextViewText(R.id.wDua, TimesCard.dua(idx));

            if (pending) {
                v.setViewVisibility(R.id.wPrayed, View.VISIBLE);
                v.setViewVisibility(R.id.wJamaa, View.VISIBLE);
                v.setTextViewText(R.id.wPrayed, "صلّيت " + PrayerTimes.NAMES[cur[0]]);
                v.setOnClickPendingIntent(R.id.wPrayed,
                        PrayedReceiver.intent(c, cur[0], curDay, PrayerLog.ALONE, 62));
                v.setOnClickPendingIntent(R.id.wJamaa,
                        PrayedReceiver.intent(c, cur[0], curDay, PrayerLog.JAMAA, 63));
            } else {
                v.setViewVisibility(R.id.wPrayed, View.GONE);
                v.setViewVisibility(R.id.wJamaa, View.GONE);
            }
        } catch (Exception e) {
            v.setTextViewText(R.id.wHijri, "صلاتي");
            v.setTextViewText(R.id.wCity, "افتح التطبيق لتحديد المدينة");
        }
        v.setOnClickPendingIntent(R.id.wRoot, open(c));
        return v;
    }

    private static PendingIntent open(Context c) {
        return PendingIntent.getActivity(c, 40, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) {
            m.updateAppWidget(id, build(c));
        }
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        super.onReceive(c, intent);
        String a = intent == null ? null : intent.getAction();
        if (ACTION_REFRESH.equals(a)
                || Intent.ACTION_TIME_TICK.equals(a)
                || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            refresh(c);
        }
    }
}
