package com.salah.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** متابعة الورد اليومي وختم القرآن. */
public final class Progress {

    public static final int TOTAL_AYAHS = 6236;

    private Progress() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("salah", Context.MODE_PRIVATE);
    }

    static String dayKey(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    /** الرقم المتسلسل للآية في المصحف كله (1..6236). */
    public static int absolute(Context c, int sura, int ayah) {
        List<Quran.Sura> all = Quran.suras(c);
        int n = 0;
        for (Quran.Sura s : all) {
            if (s.index == sura) return n + Math.min(ayah, s.ayas);
            n += s.ayas;
        }
        return n;
    }

    /** يسجّل إن المستخدم وصل للآية دي. */
    public static void record(Context c, int sura, int ayah) {
        int abs = absolute(c, sura, ayah);
        if (abs <= 0) return;
        SharedPreferences p = sp(c);
        int furthest = p.getInt("abs_furthest", 0);
        String today = dayKey(0);
        String lastDay = p.getString("abs_day", "");
        int startOfDay = p.getInt("abs_day_start", abs);

        SharedPreferences.Editor e = p.edit();
        if (!today.equals(lastDay)) {
            e.putString("abs_day", today);
            e.putInt("abs_day_start", abs);
            startOfDay = abs;
        }
        if (abs > furthest) e.putInt("abs_furthest", abs);
        int readToday = Math.max(0, abs - startOfDay);
        e.putInt("read_" + today, Math.max(readToday, p.getInt("read_" + today, 0)));
        e.apply();
    }

    public static int readOn(Context c, int daysAgo) {
        return sp(c).getInt("read_" + dayKey(daysAgo), 0);
    }

    public static int readToday(Context c) {
        return readOn(c, 0);
    }

    public static int furthest(Context c) {
        return sp(c).getInt("abs_furthest", 0);
    }

    public static int dailyTarget(Context c) {
        return sp(c).getInt("daily_target", 0);
    }

    public static void setDailyTarget(Context c, int ayahs) {
        sp(c).edit().putInt("daily_target", ayahs).apply();
    }

    /** عدد الأيام المتواصلة اللي اتحقق فيها الهدف، لحد امبارح أو النهاردة. */
    public static int streak(Context c) {
        int target = dailyTarget(c);
        if (target <= 0) return 0;
        int n = 0;
        for (int i = 0; i < 400; i++) {
            if (readOn(c, i) >= target) {
                n++;
            } else if (i > 0) {
                break;
            }
        }
        return n;
    }

    /** نسبة الختمة من 0 إلى 100. */
    public static int khatmaPercent(Context c) {
        return Math.min(100, furthest(c) * 100 / TOTAL_AYAHS);
    }

    /** كام يوم باقي على الختمة بالمعدل الحالي، أو -1 لو مش معروف. */
    public static int daysLeft(Context c) {
        int target = dailyTarget(c);
        if (target <= 0) return -1;
        int left = TOTAL_AYAHS - furthest(c);
        if (left <= 0) return 0;
        return (left + target - 1) / target;
    }

    public static void reset(Context c) {
        SharedPreferences.Editor e = sp(c).edit();
        e.remove("abs_furthest").remove("abs_day").remove("abs_day_start");
        for (int i = 0; i < 400; i++) e.remove("read_" + dayKey(i));
        e.apply();
    }

    /** تاريخ اليوم بصيغة مقروءة. */
    public static String todayText() {
        return new SimpleDateFormat("EEEE d MMMM", new Locale("ar")).format(new Date());
    }
}
