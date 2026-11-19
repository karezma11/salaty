package com.salah.app;

/**
 * حساب مواقيت الصلاة على الجهاز بدون إنترنت.
 * الناتج: ساعات بالتوقيت المحلي (مثلاً 4.75 = 4:45 ص).
 */
public final class PrayerTimes {

    public static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;

    public static final String[] NAMES = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};

    /** طرق الحساب: {زاوية الفجر، زاوية العشاء} */
    public static final String[] METHOD_NAMES = {
            "الهيئة المصرية العامة للمساحة",
            "رابطة العالم الإسلامي",
            "جامعة العلوم الإسلامية بكراتشي",
            "أمريكا الشمالية (ISNA)"
    };
    private static final double[][] METHOD_ANGLES = {
            {19.5, 17.5},
            {18.0, 17.0},
            {18.0, 18.0},
            {15.0, 15.0}
    };

    private PrayerTimes() {
    }

    /**
     * @param month     من 1 إلى 12
     * @param tzHours   فرق التوقيت عن جرينتش بالساعات
     * @param method    رقم طريقة الحساب من METHOD_NAMES
     * @param asrFactor 1 للشافعي والمالكي والحنبلي، 2 للحنفي
     */
    public static double[] compute(int year, int month, int day, double lat, double lng,
                                   double tzHours, int method, int asrFactor) {
        if (method < 0 || method >= METHOD_ANGLES.length) method = 0;
        double fajrAngle = METHOD_ANGLES[method][0];
        double ishaAngle = METHOD_ANGLES[method][1];
        double jd = julian(year, month, day) - lng / (15.0 * 24.0);

        double[] t = {5 / 24.0, 6 / 24.0, 12 / 24.0, 13 / 24.0, 18 / 24.0, 18 / 24.0};
        // تكرار الحساب مرتين يزيد الدقة
        for (int i = 0; i < 2; i++) {
            double fajr = keep(sunAngleTime(jd, lat, fajrAngle, t[FAJR], true), t[FAJR] * 24.0);
            double sunrise = keep(sunAngleTime(jd, lat, 0.833, t[SUNRISE], true), t[SUNRISE] * 24.0);
            double dhuhr = midDay(jd, t[DHUHR]);
            double asr = keep(asrTime(jd, lat, asrFactor, t[ASR]), t[ASR] * 24.0);
            double maghrib = keep(sunAngleTime(jd, lat, 0.833, t[MAGHRIB], false), t[MAGHRIB] * 24.0);
            double isha = keep(sunAngleTime(jd, lat, ishaAngle, t[ISHA], false), t[ISHA] * 24.0);
            t = new double[]{fajr / 24.0, sunrise / 24.0, dhuhr / 24.0, asr / 24.0,
                    maghrib / 24.0, isha / 24.0};
        }

        // في خطوط العرض العالية الشمس مابتنزلش للزاوية المطلوبة، فالمعادلة مالهاش حل
        // أو بتدّي وقت غير منطقي. الحل المعتمد: نحدد جزء من الليل بنسبة الزاوية.
        double sunriseH = t[SUNRISE] * 24.0;
        double maghribH = t[MAGHRIB] * 24.0;
        double night = 24.0 - (maghribH - sunriseH);
        double portionFajr = night * (fajrAngle / 60.0);
        double portionIsha = night * (ishaAngle / 60.0);

        double fajrH = t[FAJR] * 24.0;
        double ishaH = t[ISHA] * 24.0;
        boolean noFajr = Double.isNaN(sunAngleTime(jd, lat, fajrAngle, t[FAJR], true));
        boolean noIsha = Double.isNaN(sunAngleTime(jd, lat, ishaAngle, t[ISHA], false));

        if (noFajr || sunriseH - fajrH > portionFajr || fajrH >= sunriseH) {
            t[FAJR] = (sunriseH - portionFajr) / 24.0;
        }
        double ishaGap = ishaH - maghribH;
        if (ishaGap < 0) ishaGap += 24.0;
        if (noIsha || ishaGap > portionIsha) {
            t[ISHA] = (maghribH + portionIsha) / 24.0;
        }

        double[] out = new double[6];
        for (int i = 0; i < 6; i++) {
            out[i] = fixHour(t[i] * 24.0 + tzHours - lng / 15.0);
        }
        return out;
    }

    /** لو المعادلة مالهاش حل، نسيب التقدير السابق لحد ما نصلّحه بعدين. */
    private static double keep(double value, double fallback) {
        return Double.isNaN(value) ? fallback : value;
    }

    private static double julian(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    /** يرجع {الميل، معادلة الزمن} */
    private static double[] sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        double ra = fixHour(Math.toDegrees(Math.atan2(dcos(e) * dsin(l), dcos(l))) / 15.0);
        double decl = Math.toDegrees(Math.asin(dsin(e) * dsin(l)));
        double eqt = q / 15.0 - ra;
        return new double[]{decl, eqt};
    }

    private static double midDay(double jd, double t) {
        double eqt = sunPosition(jd + t)[1];
        return fixHour(12 - eqt);
    }

    private static double sunAngleTime(double jd, double lat, double angle, double t, boolean beforeNoon) {
        double decl = sunPosition(jd + t)[0];
        double noon = midDay(jd, t);
        double x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat));
        // |x| > 1 يعني الشمس عمرها ما بتوصل للزاوية دي في اليوم ده
        if (x > 1 || x < -1) return Double.NaN;
        double diff = Math.toDegrees(Math.acos(x)) / 15.0;
        return noon + (beforeNoon ? -diff : diff);
    }

    private static double asrTime(double jd, double lat, int factor, double t) {
        double decl = sunPosition(jd + t)[0];
        double angle = -Math.toDegrees(Math.atan(1.0 / (factor + Math.tan(Math.toRadians(Math.abs(lat - decl))))));
        return sunAngleTime(jd, lat, angle, t, false);
    }

    private static double dsin(double deg) {
        return Math.sin(Math.toRadians(deg));
    }

    private static double dcos(double deg) {
        return Math.cos(Math.toRadians(deg));
    }

    private static double fixAngle(double a) {
        a = a - 360.0 * Math.floor(a / 360.0);
        return a < 0 ? a + 360.0 : a;
    }

    private static double fixHour(double h) {
        h = h - 24.0 * Math.floor(h / 24.0);
        return h < 0 ? h + 24.0 : h;
    }

    /** تحويل الساعات إلى نص مثل 4:45 ص */
    public static String format(double hours) {
        int total = (int) Math.round(hours * 60.0);
        total = ((total % 1440) + 1440) % 1440;
        int h = total / 60;
        int m = total % 60;
        String suffix = h < 12 ? "ص" : "م";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return h12 + ":" + (m < 10 ? "0" + m : String.valueOf(m)) + " " + suffix;
    }
}
