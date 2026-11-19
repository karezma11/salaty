package com.salah.app;

import android.content.Context;

/** القرّاء المتاحون للتحميل من everyayah. */
public final class Reciters {

    public static final class Reciter {
        public final String id;
        public final String name;
        public final String folder;
        public final String quality;

        Reciter(String id, String name, String folder, String quality) {
            this.id = id;
            this.name = name;
            this.folder = folder;
            this.quality = quality;
        }
    }

    public static final Reciter[] ALL = {
            new Reciter("basit", "عبد الباسط عبد الصمد", "Abdul_Basit_Murattal_64kbps", "مرتل · 64"),
            new Reciter("husary", "محمود خليل الحصري", "Husary_64kbps", "مرتل · 64"),
            new Reciter("minshawi", "محمد صديق المنشاوي", "Minshawy_Murattal_128kbps", "مرتل · 128"),
            new Reciter("alafasy", "مشاري العفاسي", "Alafasy_64kbps", "مرتل · 64"),
            new Reciter("sudais", "عبد الرحمن السديس", "Abdurrahmaan_As-Sudais_64kbps", "مرتل · 64"),
            new Reciter("shuraim", "سعود الشريم", "Saood_ash-Shuraym_64kbps", "مرتل · 64"),
            new Reciter("ajamy", "أحمد العجمي", "ahmed_ibn_ali_al_ajamy_128kbps", "مرتل · 128"),
            new Reciter("ghamadi", "سعد الغامدي", "Ghamadi_40kbps", "مرتل · 40")
    };

    private Reciters() {
    }

    public static Reciter byId(String id) {
        for (Reciter r : ALL) {
            if (r.id.equals(id)) return r;
        }
        return ALL[0];
    }

    public static Reciter current(Context c) {
        return byId(Prefs.reciter(c));
    }

    public static int indexOf(String id) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].id.equals(id)) return i;
        }
        return 0;
    }

    public static String[] names() {
        String[] n = new String[ALL.length];
        for (int i = 0; i < ALL.length; i++) {
            n[i] = ALL[i].name + "  ·  " + ALL[i].quality;
        }
        return n;
    }
}
