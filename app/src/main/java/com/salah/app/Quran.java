package com.salah.app;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * قراءة نص القرآن من ملفات التطبيق، بدون إنترنت.
 * المصدر: مشروع تنزيل (tanzil.net)، منسوخ حرفياً بدون تعديل.
 */
public final class Quran {

    /** بيانات سورة واحدة. */
    public static final class Sura {
        public final int index;
        public final String name;
        public final int ayas;
        public final String type;

        Sura(int index, String name, int ayas, String type) {
            this.index = index;
            this.name = name;
            this.ayas = ayas;
            this.type = type;
        }
    }

    /** آية واحدة. */
    public static final class Aya {
        public final int number;
        public final String text;

        Aya(int number, String text) {
            this.number = number;
            this.text = text;
        }
    }

    private static List<Sura> suras;

    private Quran() {
    }

    /** قائمة السور الـ 114، تُقرأ مرة واحدة وتُحفظ في الذاكرة. */
    public static List<Sura> suras(Context c) {
        if (suras != null) return suras;
        List<Sura> list = new ArrayList<>(114);
        AssetManager am = c.getAssets();
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(am.open("quran/suras.txt"), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length < 4) continue;
                list.add(new Sura(Integer.parseInt(p[0]), p[1], Integer.parseInt(p[2]), p[3]));
            }
        } catch (IOException | NumberFormatException e) {
            // ملف ناقص: نرجع ما قرأناه
        } finally {
            close(r);
        }
        suras = list;
        return list;
    }

    private static int cachedSura;
    private static List<Aya> cachedAyas;

    /** آيات سورة معينة (رقم السورة من 1 إلى 114). */
    public static List<Aya> ayas(Context c, int suraIndex) {
        // السورة الواحدة بتتقرا كذا مرة في نفس الشاشة، فبنحفظ آخر واحدة
        synchronized (Quran.class) {
            if (cachedSura == suraIndex && cachedAyas != null) return cachedAyas;
        }
        List<Aya> list = new ArrayList<>();
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(
                    c.getAssets().open("quran/" + suraIndex + ".txt"), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                int bar = line.indexOf('|');
                if (bar <= 0) continue;
                int n = Integer.parseInt(line.substring(0, bar));
                list.add(new Aya(n, line.substring(bar + 1)));
            }
        } catch (IOException | NumberFormatException e) {
            // ملف ناقص: نرجع ما قرأناه
        } finally {
            close(r);
        }
        if (!list.isEmpty()) {
            synchronized (Quran.class) {
                cachedSura = suraIndex;
                cachedAyas = java.util.Collections.unmodifiableList(list);
                return cachedAyas;
            }
        }
        return list;
    }

    /**
     * يبسّط النص للبحث: يشيل التشكيل وعلامات الوقف،
     * ويوحّد الألف والياء والتاء المربوطة.
     */
    public static String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 0x064B && ch <= 0x0652) || ch == 0x0670 || ch == 0x0640
                    || (ch >= 0x06D6 && ch <= 0x06ED)) {
                continue; // تشكيل وعلامات وقف وتطويل
            }
            if (ch == 'أ' || ch == 'إ' || ch == 'آ' || ch == 'ٱ') ch = 'ا';
            else if (ch == 'ى') ch = 'ي';
            else if (ch == 'ة') ch = 'ه';
            sb.append(ch);
        }
        return sb.toString();
    }

    /** نتيجة بحث واحدة. */
    public static final class Hit {
        public final int sura;
        public final int aya;
        public final String text;

        Hit(int sura, int aya, String text) {
            this.sura = sura;
            this.aya = aya;
            this.text = text;
        }
    }

    /** يبحث في المصحف كله، ويتوقف عند حد أقصى للنتائج. */
    public static List<Hit> search(Context c, String query, int limit) {
        List<Hit> hits = new ArrayList<>();
        String q = normalize(query).trim();
        if (q.isEmpty()) return hits;
        for (Sura sura : suras(c)) {
            for (Aya aya : ayas(c, sura.index)) {
                if (normalize(aya.text).contains(q)) {
                    hits.add(new Hit(sura.index, aya.number, aya.text));
                    if (hits.size() >= limit) return hits;
                }
            }
        }
        return hits;
    }

    /** رقم الآية بالأرقام العربية الهندية داخل علامة نهاية الآية. */
    public static String ayaMark(int n) {
        String digits = "٠١٢٣٤٥٦٧٨٩";
        StringBuilder sb = new StringBuilder();
        for (char ch : String.valueOf(n).toCharArray()) {
            sb.append(digits.charAt(ch - '0'));
        }
        return " ۝" + sb + " ";
    }

    private static void close(BufferedReader r) {
        if (r != null) {
            try {
                r.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** نص آية واحدة، أو null لو مش موجودة. */
    public static String ayaText(Context c, int sura, int ayah) {
        for (Aya a : ayas(c, sura)) {
            if (a.number == ayah) return a.text;
        }
        return null;
    }
}
