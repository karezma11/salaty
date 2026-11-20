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

    /** حالة صلاة في يوم معيّن (yyyy-MM-dd زي Progress.dayKey). */
    public static int stateOn(Context c, String day, int prayer) {
        return sp(c).getInt("pl_" + day + "_" + prayer, NOT_PRAYED);
    }

    /** يسجّل صلاة في يوم معيّن؛ زرار «صلّيت» بيبعت اليوم اللي الإشعار اتعمل فيه. */
    public static void set(Context c, String day, int prayer, int state) {
        sp(c).edit().putInt("pl_" + day + "_" + prayer, state).apply();
    }

    /**
     * الصلاة اللي وقتها دخل ولسه ممكن تتسجّل، من الصلاة الجاية: قبل الظهر = الفجر
     * (الشروق مش فرض)، وقبل الفجر = العشاء، وبتاعة امبارح لو عدّى نص الليل.
     * بيرجّع {الصلاة، عدد الأيام اللي فاتت}.
     */
    static int[] current(int next, boolean nextIsToday) {
        switch (next) {
            case PrayerTimes.FAJR:
                return new int[]{PrayerTimes.ISHA, nextIsToday ? 1 : 0};
            case PrayerTimes.SUNRISE:
            case PrayerTimes.DHUHR:
                return new int[]{PrayerTimes.FAJR, 0};
            default:
                return new int[]{next - 1, 0};
        }
    }

    /** هل الوقت ده (بالمللي) النهارده؟ */
    static boolean isToday(long millis) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar at = java.util.Calendar.getInstance();
        at.setTimeInMillis(millis);
        return now.get(java.util.Calendar.YEAR) == at.get(java.util.Calendar.YEAR)
                && now.get(java.util.Calendar.DAY_OF_YEAR) == at.get(java.util.Calendar.DAY_OF_YEAR);
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
