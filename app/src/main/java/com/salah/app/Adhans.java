package com.salah.app;

/** أصوات الأذان المضمّنة داخل التطبيق. */
public final class Adhans {

    public static final String[] NAMES = {
            "مشاري العفاسي",
            "إبراهيم الأركاني",
            "منصور الزهراني",
            "ناصر القطامي",
            "حمدان الملكي"
    };

    private static final String[] FILES = {
            "adhan/alafasy.mp3",
            "adhan/arkani.mp3",
            "adhan/zahrani.mp3",
            "adhan/qatami.mp3",
            "adhan/malki.mp3"
    };

    /** أذان الفجر له صيغة مختلفة (الصلاة خير من النوم). */
    public static final String FAJR_FILE = "adhan/fajr.mp3";

    private Adhans() {
    }

    public static String file(int index) {
        if (index < 0 || index >= FILES.length) index = 0;
        return FILES[index];
    }

    /** الملف المناسب للصلاة: أذان الفجر للفجر، والمختار لباقي الصلوات. */
    public static String fileFor(int prayer, int chosen, boolean fajrSpecial) {
        if (prayer == PrayerTimes.FAJR && fajrSpecial) return FAJR_FILE;
        return file(chosen);
    }
}
