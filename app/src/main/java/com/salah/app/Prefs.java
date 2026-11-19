package com.salah.app;

import android.content.Context;
import android.content.SharedPreferences;

/** إعدادات التطبيق المحفوظة على الجهاز. */
public final class Prefs {

    private static final String FILE = "salah_prefs";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static double lat(Context c) {
        return Double.longBitsToDouble(sp(c).getLong("lat", Double.doubleToLongBits(30.0444)));
    }

    public static double lng(Context c) {
        return Double.longBitsToDouble(sp(c).getLong("lng", Double.doubleToLongBits(31.2357)));
    }

    public static String city(Context c) {
        return sp(c).getString("city", "القاهرة");
    }

    public static void setLocation(Context c, String city, double lat, double lng) {
        sp(c).edit()
                .putString("city", city)
                .putLong("lat", Double.doubleToLongBits(lat))
                .putLong("lng", Double.doubleToLongBits(lng))
                .apply();
    }

    public static boolean hasLocation(Context c) {
        return sp(c).contains("lat");
    }

    public static int method(Context c) {
        return sp(c).getInt("method", 0);
    }

    public static void setMethod(Context c, int method) {
        sp(c).edit().putInt("method", method).apply();
    }

    /** 1 = شافعي ومالكي وحنبلي، 2 = حنفي */
    public static int asrFactor(Context c) {
        return sp(c).getInt("asr", 1);
    }

    public static void setAsrFactor(Context c, int factor) {
        sp(c).edit().putInt("asr", factor).apply();
    }

    public static boolean adhanEnabled(Context c) {
        return sp(c).getBoolean("adhan", true);
    }

    public static void setAdhanEnabled(Context c, boolean on) {
        sp(c).edit().putBoolean("adhan", on).apply();
    }

    /** آخر سورة فتحها المستخدم، و0 يعني لا شيء. */
    public static int lastSura(Context c) {
        return sp(c).getInt("last_sura", 0);
    }

    public static void setLastSura(Context c, int sura) {
        sp(c).edit().putInt("last_sura", sura).apply();
    }

    /** حجم خط المصحف. */
    public static int quranFontSize(Context c) {
        return sp(c).getInt("quran_font", 24);
    }

    public static void setQuranFontSize(Context c, int size) {
        sp(c).edit().putInt("quran_font", size).apply();
    }

    /** الفاصل بالدقائق بين تذكيرات الصلاة على النبي، و0 يعني مقفول. */
    public static int salawatMinutes(Context c) {
        return sp(c).getInt("salawat", 0);
    }

    public static void setSalawatMinutes(Context c, int minutes) {
        sp(c).edit().putInt("salawat", minutes).apply();
    }

    /** صوت الأذان المختار من Adhans.NAMES. */
    public static int adhanVoice(Context c) {
        return sp(c).getInt("adhan_voice", 0);
    }

    public static void setAdhanVoice(Context c, int index) {
        sp(c).edit().putInt("adhan_voice", index).apply();
    }

    /** استخدام أذان الفجر الخاص (الصلاة خير من النوم). */
    public static boolean fajrAdhan(Context c) {
        return sp(c).getBoolean("adhan_fajr", true);
    }

    public static void setFajrAdhan(Context c, boolean on) {
        sp(c).edit().putBoolean("adhan_fajr", on).apply();
    }

    /** تعديل يدوي بالدقائق لكل صلاة (من -٣٠ إلى +٣٠). */
    public static int offset(Context c, int prayer) {
        return sp(c).getInt("offset_" + prayer, 0);
    }

    public static void setOffset(Context c, int prayer, int minutes) {
        sp(c).edit().putInt("offset_" + prayer, minutes).apply();
    }

    /** دقائق الإقامة بعد الأذان، و0 يعني مقفول. */
    public static int iqamaMinutes(Context c) {
        return sp(c).getInt("iqama", 0);
    }

    public static void setIqamaMinutes(Context c, int minutes) {
        sp(c).edit().putInt("iqama", minutes).apply();
    }

    /** إشعار دائم بمواقيت اليوم. */
    public static boolean persistentTimes(Context c) {
        return sp(c).getBoolean("persistent", true);
    }

    public static void setPersistentTimes(Context c, boolean on) {
        sp(c).edit().putBoolean("persistent", on).apply();
    }

    /** 0 حسب النظام، 1 فاتح، 2 غامق. */
    public static int theme(Context c) {
        return sp(c).getInt("theme", 0);
    }

    public static void setTheme(Context c, int mode) {
        sp(c).edit().putInt("theme", mode).apply();
    }

    /** آخر آية وقف عندها المستخدم في سورة معينة. */
    public static int lastAyah(Context c, int sura) {
        return sp(c).getInt("last_ayah_" + sura, 1);
    }

    public static void setLastAyah(Context c, int sura, int ayah) {
        if (ayah < 1) return;
        sp(c).edit().putInt("last_ayah_" + sura, ayah).apply();
    }

    /** تعديل التقويم الهجري بالأيام (من -2 إلى +2) حسب الرؤية المحلية. */
    public static int hijriOffset(Context c) {
        return sp(c).getInt("hijri_offset", 0);
    }

    public static void setHijriOffset(Context c, int days) {
        sp(c).edit().putInt("hijri_offset", days).apply();
    }

    public static boolean fastingReminder(Context c) {
        return sp(c).getBoolean("fasting_reminder", true);
    }

    public static void setFastingReminder(Context c, boolean on) {
        sp(c).edit().putBoolean("fasting_reminder", on).apply();
    }

    /** القارئ المختار. */
    public static String reciter(Context c) {
        return sp(c).getString("reciter", "basit");
    }

    public static void setReciter(Context c, String id) {
        sp(c).edit().putString("reciter", id).apply();
    }

    /** تكبير خط التطبيق كله: 1.0 عادي، لحد 1.4. */
    public static float fontScale(Context c) {
        return sp(c).getFloat("font_scale", 1.0f);
    }

    public static void setFontScale(Context c, float scale) {
        sp(c).edit().putFloat("font_scale", scale).apply();
    }

    public static boolean adhkarReminder(Context c) {
        return sp(c).getBoolean("adhkar_reminder", true);
    }

    public static void setAdhkarReminder(Context c, boolean on) {
        sp(c).edit().putBoolean("adhkar_reminder", on).apply();
    }

    public static boolean keepScreenOn(Context c) {
        return sp(c).getBoolean("keep_screen_on", true);
    }

    public static void setKeepScreenOn(Context c, boolean on) {
        sp(c).edit().putBoolean("keep_screen_on", on).apply();
    }

    public static boolean seenWelcome(Context c) {
        return sp(c).getBoolean("seen_welcome", false);
    }

    public static void setSeenWelcome(Context c, boolean v) {
        sp(c).edit().putBoolean("seen_welcome", v).apply();
    }

    /** كتاب التفسير المختار. */
    public static String tafsirBook(Context c) {
        return sp(c).getString("tafsir_book", "mokhtasar");
    }

    public static void setTafsirBook(Context c, String id) {
        sp(c).edit().putString("tafsir_book", id).apply();
    }

    /** كتم الموبايل أثناء الصلاة. */
    public static boolean silencePrayer(Context c) {
        return sp(c).getBoolean("silence_prayer", false);
    }

    public static void setSilencePrayer(Context c, boolean on) {
        sp(c).edit().putBoolean("silence_prayer", on).apply();
    }

    /** مدة الكتم بالدقائق. */
    public static int silenceMinutes(Context c) {
        return sp(c).getInt("silence_minutes", 15);
    }

    public static void setSilenceMinutes(Context c, int m) {
        sp(c).edit().putInt("silence_minutes", m).apply();
    }

    /** وضع الإزعاج الأصلي قبل الكتم، عشان نرجّعه. */
    public static int savedFilter(Context c) {
        return sp(c).getInt("saved_filter", 0);
    }

    public static void setSavedFilter(Context c, int f) {
        sp(c).edit().putInt("saved_filter", f).apply();
    }

    /** تنبيه قبل الأذان بكام دقيقة، و0 يعني مقفول. */
    public static int preAdhanMinutes(Context c) {
        return sp(c).getInt("pre_adhan", 0);
    }

    public static void setPreAdhanMinutes(Context c, int m) {
        sp(c).edit().putInt("pre_adhan", m).apply();
    }
}
