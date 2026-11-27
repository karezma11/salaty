package com.salah.app;

import android.content.Context;

/** حسابات رمضان وقيام الليل: الإمساك، ونسبة الصيام، وبداية الثلث الأخير من الليل. */
public final class Ramadan {

    /** الإمساك قبل الفجر بعشر دقايق، زي إمساكيات مصر. */
    static final int IMSAK_MINUTES = 10;

    static final String IFTAR_DUA = "ذهب الظمأ وابتلّت العروق وثبت الأجر إن شاء الله";

    private Ramadan() {
    }

    /** النهارده (أو بعد كام يوم) في رمضان حسب التقويم الهجري وتعديله؟ */
    public static boolean on(Context c, int dayOffset) {
        return Hijri.plusDays(c, dayOffset).month == 9;
    }

    /** رقم اليوم في رمضان، أو 0. */
    public static int day(Context c) {
        Hijri.Date h = Hijri.today(c);
        return h.month == 9 ? h.day : 0;
    }

    /** الإمساك بالساعات من وقت الفجر. */
    static double imsak(double fajr) {
        return ((fajr - IMSAK_MINUTES / 60.0) % 24.0 + 24.0) % 24.0;
    }

    /** نسبة اللي عدّى من يوم الصيام (من الفجر للمغرب) من ٠ لـ ١. */
    static double fastProgress(double fajr, double maghrib, double now) {
        if (maghrib <= fajr) return 0;
        return Math.max(0, Math.min(1, (now - fajr) / (maghrib - fajr)));
    }

    /**
     * بداية الثلث الأخير من الليل: الليل من المغرب للفجر اللي بعده، والثلث الأخير
     * بيبدأ قبل الفجر بتلت طول الليل. بيرجّع ساعة من ٠ لـ ٢٤.
     */
    static double qiyamStart(double maghrib, double fajrNext) {
        double night = (24.0 - maghrib) + fajrNext;
        return ((fajrNext - night / 3.0) % 24.0 + 24.0) % 24.0;
    }
}
