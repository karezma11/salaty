package com.salah.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** ويدجت الشاشة الرئيسية: الصلاة القادمة ومواقيت اليوم. */
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

    private static RemoteViews build(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_times);
        try {
            double[] t = Scheduler.timesFor(c, 0);
            long[] next = Scheduler.nextPrayer(c);
            int idx = (int) next[0];

            v.setTextViewText(R.id.wName, PrayerTimes.NAMES[idx]);
            v.setTextViewText(R.id.wTime, PrayerTimes.format(t[idx]));

            long left = Math.max(0, (next[1] - System.currentTimeMillis()) / 60000L);
            v.setTextViewText(R.id.wLeft, left >= 60
                    ? "بعد " + Ui.arabicDigits(left / 60) + " س و " + Ui.arabicDigits(left % 60) + " د"
                    : "بعد " + Ui.arabicDigits(left) + " دقيقة");

            v.setTextViewText(R.id.wCity, Prefs.city(c));

            StringBuilder all = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                if (i == PrayerTimes.SUNRISE) continue;
                if (all.length() > 0) all.append("   ");
                all.append(PrayerTimes.NAMES[i]).append(' ').append(PrayerTimes.format(t[i]));
            }
            v.setTextViewText(R.id.wAll, all.toString());
        } catch (Exception e) {
            v.setTextViewText(R.id.wName, "صلاتي");
            v.setTextViewText(R.id.wLeft, "افتح التطبيق لتحديد المدينة");
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
