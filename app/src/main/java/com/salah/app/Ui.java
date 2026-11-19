package com.salah.app;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

/** أدوات مشتركة للواجهة: الخطوط والتنسيق. */
public final class Ui {

    private static Typeface quran;
    private static Typeface naskh;
    private static boolean loaded;

    private Ui() {
    }

    private static void load(Context c) {
        if (loaded) return;
        loaded = true;
        quran = byName(c, "amiri_quran");
        naskh = byName(c, "amiri_regular");
    }

    /** يحمّل خط بالاسم، ويرجع null لو الملف مش موجود. */
    private static Typeface byName(Context c, String name) {
        try {
            int id = c.getResources().getIdentifier(name, "font", c.getPackageName());
            return id == 0 ? null : ResourcesCompat.getFont(c, id);
        } catch (Exception e) {
            return null;
        }
    }

    /** خط المصحف، ولو مش موجود بيرجع الخط العادي. */
    public static void applyQuranFont(TextView v) {
        load(v.getContext());
        if (quran != null) v.setTypeface(quran);
    }

    /** خط عربي للعناوين. */
    public static void applyNaskh(TextView v, boolean bold) {
        load(v.getContext());
        if (naskh != null) {
            v.setTypeface(naskh, bold ? Typeface.BOLD : Typeface.NORMAL);
        } else if (bold) {
            v.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    /** تحويل الأرقام إلى أرقام عربية هندية. */
    public static String arabicDigits(long n) {
        String digits = "٠١٢٣٤٥٦٧٨٩";
        StringBuilder sb = new StringBuilder();
        for (char ch : String.valueOf(n).toCharArray()) {
            sb.append(ch >= '0' && ch <= '9' ? digits.charAt(ch - '0') : ch);
        }
        return sb.toString();
    }

    public static Typeface quranTypeface(Context c) {
        load(c);
        return quran;
    }

    public static Typeface naskhTypeface(Context c) {
        load(c);
        return naskh;
    }
}
