package com.salah.app;

import android.content.Context;

import java.io.File;

/**
 * ملفات التلاوة داخل التطبيق: ملف لكل آية.
 * المسار داخل التطبيق: <سورة بثلاث خانات>/<سورة><آية>.mp3
 * ويقدر المستخدم يحط تلاوة بديلة في مجلد التطبيق على الموبايل.
 */
public final class Recitation {

    private Recitation() {
    }

    /** المجلد الأساسي لكل التلاوات. */
    public static File folder(Context c) {
        return new File(c.getExternalFilesDir(null), "audio");
    }

    /** مجلد قارئ معيّن. */
    public static File folderOf(Context c, String reciterId) {
        return new File(folder(c), reciterId);
    }

    public static String assetName(int sura, int ayah) {
        return String.format("%03d/%03d%03d.mp3", sura, sura, ayah);
    }

    /** ملف الآية للقارئ المختار، أو null. */
    public static File externalFile(Context c, int sura, int ayah) {
        String name = String.format("%03d%03d.mp3", sura, ayah);
        String sub = String.format("%03d", sura);
        String reciter = Prefs.reciter(c);

        File own = new File(new File(folderOf(c, reciter), sub), name);
        if (ok(own)) return own;

        // التحميلات القديمة كانت لعبد الباسط من غير مجلد باسمه
        if ("basit".equals(reciter)) {
            File legacy = new File(new File(folder(c), sub), name);
            if (ok(legacy)) return legacy;
            File flat = new File(folder(c), name);
            if (ok(flat)) return flat;
        }
        return null;
    }

    private static boolean ok(File f) {
        return f.exists() && f.length() > 500;
    }

    /**
     * هل آية معينة متاحة صوتياً؟
     * التلاوة بقت بتتحمّل على الموبايل، فبنفحص الملفات بس من غير ما نسأل الأصول
     * (السؤال ده كان بيرمي استثناء في كل مرة وبيبطّئ القوائم).
     */
    public static boolean ayahAvailable(Context c, int sura, int ayah) {
        return externalFile(c, sura, ayah) != null;
    }

    // ذاكرة مؤقتة: فحص 114 مجلد في كل مرة كان بيعطّل فتح قائمة السور
    private static final boolean[] cache = new boolean[115];
    private static boolean cacheReady;
    private static String cacheReciter = "";

    /** يلغي الذاكرة المؤقتة بعد تحميل أو مسح أو تغيير قارئ. */
    public static void invalidate() {
        cacheReady = false;
    }

    private static void fill(Context c) {
        String reciter = Prefs.reciter(c);
        if (cacheReady && reciter.equals(cacheReciter)) return;
        for (int i = 1; i <= 114; i++) {
            cache[i] = ayahAvailable(c, i, 1);
        }
        cacheReciter = reciter;
        cacheReady = true;
    }

    /** السورة متاحة لو أول آية فيها موجودة. */
    public static boolean available(Context c, int sura) {
        if (sura < 1 || sura > 114) return false;
        fill(c);
        return cache[sura];
    }

    public static int count(Context c) {
        fill(c);
        int n = 0;
        for (int i = 1; i <= 114; i++) {
            if (cache[i]) n++;
        }
        return n;
    }

    public static String path(Context c) {
        return folder(c).getAbsolutePath();
    }
}
