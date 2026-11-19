package com.salah.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** اختبارات حساب مواقيت الصلاة. */
public class PrayerTimesTest {

    private static final double CAIRO_LAT = 30.0444, CAIRO_LNG = 31.2357, CAIRO_TZ = 2;

    @Test
    public void cairoMatchesKnownTimes() {
        double[] t = PrayerTimes.compute(2026, 11, 13, CAIRO_LAT, CAIRO_LNG, CAIRO_TZ, 0, 1);
        assertEquals("الفجر", 4.82, t[PrayerTimes.FAJR], 0.08);
        assertEquals("الظهر", 11.65, t[PrayerTimes.DHUHR], 0.08);
        assertEquals("العصر", 14.65, t[PrayerTimes.ASR], 0.08);
        assertEquals("المغرب", 17.00, t[PrayerTimes.MAGHRIB], 0.08);
        assertEquals("العشاء", 18.33, t[PrayerTimes.ISHA], 0.10);
    }

    @Test
    public void allTimesAreValidEverywhere() {
        double[][] places = {
                {30.0444, 31.2357, 2}, {24.7136, 46.6753, 3}, {51.5074, -0.1278, 0},
                {-33.8688, 151.2093, 10}, {64.1466, -21.9426, 0}, {1.3521, 103.8198, 8}
        };
        for (double[] p : places) {
            for (int month = 1; month <= 12; month++) {
                for (int method = 0; method < PrayerTimes.METHOD_NAMES.length; method++) {
                    double[] t = PrayerTimes.compute(2026, month, 15, p[0], p[1], p[2], method, 1);
                    for (int i = 0; i < 6; i++) {
                        assertFalse("NaN عند " + p[0] + " شهر " + month, Double.isNaN(t[i]));
                        assertTrue("خارج اليوم", t[i] >= 0 && t[i] < 24);
                    }
                }
            }
        }
    }

    @Test
    public void prayersStayInOrder() {
        double[][] places = {{30.0444, 31.2357, 2}, {24.7136, 46.6753, 3}, {51.5074, -0.1278, 0}};
        for (double[] p : places) {
            for (int month = 1; month <= 12; month++) {
                double[] t = PrayerTimes.compute(2026, month, 15, p[0], p[1], p[2], 1, 1);
                assertTrue("الفجر قبل الشروق", t[0] < t[1]);
                assertTrue("الشروق قبل الظهر", t[1] < t[2]);
                assertTrue("الظهر قبل العصر", t[2] < t[3]);
                assertTrue("العصر قبل المغرب", t[3] < t[4]);
                double gap = t[5] - t[4];
                if (gap < 0) gap += 24;
                assertTrue("العشاء بعد المغرب بمدة معقولة", gap > 0 && gap < 6);
            }
        }
    }

    /** في الصيف الشمس مابتنزلش كفاية في لندن — لازم قاعدة خطوط العرض العالية تشتغل. */
    @Test
    public void londonSummerIshaIsReasonable() {
        double[] t = PrayerTimes.compute(2026, 6, 21, 51.5074, -0.1278, 1, 1, 1);
        assertTrue("العشاء بعد المغرب", t[PrayerTimes.ISHA] > t[PrayerTimes.MAGHRIB]);
        assertTrue("العشاء مش بعد منتصف الليل", t[PrayerTimes.ISHA] < 24);
        assertTrue("الفجر قبل الشروق", t[PrayerTimes.FAJR] < t[PrayerTimes.SUNRISE]);
    }

    @Test
    public void hanafiAsrIsAlwaysLater() {
        for (int month = 1; month <= 12; month++) {
            double[] shafi = PrayerTimes.compute(2026, month, 15, CAIRO_LAT, CAIRO_LNG, CAIRO_TZ, 0, 1);
            double[] hanafi = PrayerTimes.compute(2026, month, 15, CAIRO_LAT, CAIRO_LNG, CAIRO_TZ, 0, 2);
            assertTrue("العصر الحنفي بعد الشافعي، شهر " + month,
                    hanafi[PrayerTimes.ASR] > shafi[PrayerTimes.ASR]);
        }
    }

    @Test
    public void formatsHoursCorrectly() {
        assertEquals("4:49 ص", PrayerTimes.format(4.8167));
        assertEquals("12:00 م", PrayerTimes.format(12.0));
        assertEquals("12:00 ص", PrayerTimes.format(0.0));
        assertEquals("11:30 م", PrayerTimes.format(23.5));
    }
}
