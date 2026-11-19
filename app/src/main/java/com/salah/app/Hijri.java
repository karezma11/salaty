package com.salah.app;

import android.content.Context;

import java.util.Calendar;

/** التقويم الهجري الحسابي، مع إمكانية تعديل يوم للرؤية. */
public final class Hijri {

    public static final String[] MONTHS = {
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };

    public static final class Date {
        public final int year, month, day;

        Date(int y, int m, int d) {
            year = y;
            month = m;
            day = d;
        }

        public String text() {
            return Ui.arabicDigits(day) + " " + MONTHS[month - 1] + " " + Ui.arabicDigits(year) + " هـ";
        }
    }

    private Hijri() {
    }

    private static long gregorianToJd(int y, int m, int d) {
        long a = (14 - m) / 12;
        long yy = y + 4800 - a;
        long mm = m + 12 * a - 3;
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045;
    }

    public static Date of(int y, int m, int d, int offsetDays) {
        long jd = gregorianToJd(y, m, d) + offsetDays;
        long l = jd - 1948440 + 10632;
        long n = (l - 1) / 10631;
        l = l - 10631 * n + 354;
        long j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238);
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29;
        long month = (24 * l) / 709;
        long day = l - (709 * month) / 24;
        long year = 30 * n + j - 30;
        return new Date((int) year, (int) month, (int) day);
    }

    /** تاريخ اليوم الهجري حسب إعداد المستخدم. */
    public static Date today(Context c) {
        Calendar cal = Calendar.getInstance();
        return of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH), Prefs.hijriOffset(c));
    }

    public static Date plusDays(Context c, int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH), Prefs.hijriOffset(c));
    }

    /** الأيام البيض: 13 و14 و15 من كل شهر. */
    public static boolean isWhiteDay(Date h) {
        return h.day == 13 || h.day == 14 || h.day == 15;
    }

    /** الاثنين أو الخميس. */
    public static boolean isMondayOrThursday() {
        int d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return d == Calendar.MONDAY || d == Calendar.THURSDAY;
    }

    /** سبب الصيام المستحب النهاردة، أو null. */
    public static String fastingReason(Context c) {
        Date h = today(c);
        if (h.month == 9) return null; // رمضان، فرض مش نافلة
        if (isWhiteDay(h)) return "من الأيام البيض (" + Ui.arabicDigits(h.day) + " " + MONTHS[h.month - 1] + ")";
        if (isMondayOrThursday()) {
            int d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            return d == Calendar.MONDAY ? "يوم الاثنين" : "يوم الخميس";
        }
        if (h.month == 1 && h.day == 10) return "يوم عاشوراء";
        if (h.month == 12 && h.day == 9) return "يوم عرفة";
        return null;
    }

    /** كام يوم فاضل على رمضان، أو -1 لو احنا فيه أو بعيد. */
    public static int daysToRamadan(Context c) {
        for (int i = 0; i <= 60; i++) {
            Date h = plusDays(c, i);
            if (h.month == 9 && h.day == 1) return i;
        }
        return -1;
    }
}
