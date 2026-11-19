package com.salah.app;

import android.content.Context;
import android.content.SharedPreferences;

/** سجل الصلوات: الخمس فروض لكل يوم، ومعاها هل كانت في جماعة. */
public final class PrayerLog {

    /** الفروض الخمسة فقط (من غير الشروق). */
    public static final int[] FARD = {PrayerTimes.FAJR, PrayerTimes.DHUHR,
            PrayerTimes.ASR, PrayerTimes.MAGHRIB, PrayerTimes.ISHA};

    public static final int NOT_PRAYED = 0;
    public static final int ALONE = 1;
    public static final int JAMAA = 2;

    private PrayerLog() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("salah", Context.MODE_PRIVATE);
    }

    private static String key(int daysAgo, int prayer) {
        return "pl_" + Progress.dayKey(daysAgo) + "_" + prayer;
    }

    public static int state(Context c, int daysAgo, int prayer) {
        return sp(c).getInt(key(daysAgo, prayer), NOT_PRAYED);
    }

    /** يلف بين: لم تُصلَّ ← منفردًا ← جماعة ← لم تُصلَّ. */
    public static int cycle(Context c, int daysAgo, int prayer) {
        int next = (state(c, daysAgo, prayer) + 1) % 3;
        sp(c).edit().putInt(key(daysAgo, prayer), next).apply();
        return next;
    }

    public static String label(int state) {
        switch (state) {
            case JAMAA:
                return "في جماعة";
            case ALONE:
                return "منفردًا";
            default:
                return "لم تُسجّل";
        }
    }

    /** عدد الصلوات المسجّلة في آخر عدد أيام. */
    public static int countIn(Context c, int days, boolean jamaaOnly) {
        int n = 0;
        for (int d = 0; d < days; d++) {
            for (int p : FARD) {
                int st = state(c, d, p);
                if (jamaaOnly ? st == JAMAA : st != NOT_PRAYED) n++;
            }
        }
        return n;
    }
}
