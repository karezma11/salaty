package com.salah.app;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/** اختبارات اختيار الصلاة اللي زرار «صلّيت» بيسجّلها. */
public class PrayerLogTest {

    private static int[] cur(int next, boolean nextIsToday) {
        return PrayerLog.current(next, nextIsToday);
    }

    @Test
    public void previousPrayerOfTheDay() {
        assertArrayEquals(new int[]{PrayerTimes.DHUHR, 0}, cur(PrayerTimes.ASR, true));
        assertArrayEquals(new int[]{PrayerTimes.ASR, 0}, cur(PrayerTimes.MAGHRIB, true));
        assertArrayEquals(new int[]{PrayerTimes.MAGHRIB, 0}, cur(PrayerTimes.ISHA, true));
    }

    @Test
    public void sunriseIsSkipped() {
        // بين الشروق والظهر الزرار لسه للفجر
        assertArrayEquals(new int[]{PrayerTimes.FAJR, 0}, cur(PrayerTimes.DHUHR, true));
        assertArrayEquals(new int[]{PrayerTimes.FAJR, 0}, cur(PrayerTimes.SUNRISE, true));
    }

    @Test
    public void ishaBeforeAndAfterMidnight() {
        // بعد العشاء وقبل نص الليل: الفجر الجاي بكرة، والعشاء بتاعة النهارده
        assertArrayEquals(new int[]{PrayerTimes.ISHA, 0}, cur(PrayerTimes.FAJR, false));
        // بعد نص الليل: الفجر النهارده، والعشاء بتاعة امبارح
        assertArrayEquals(new int[]{PrayerTimes.ISHA, 1}, cur(PrayerTimes.FAJR, true));
    }
}
