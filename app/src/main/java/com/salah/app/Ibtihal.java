package com.salah.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.util.ArrayList;
import java.util.List;

/** الابتهالات والتواشيح المضمّنة داخل التطبيق. */
public final class Ibtihal {

    public static final class Track {
        public final String file;
        public final String title;
        public final String kind;

        Track(String file, String title, String kind) {
            this.file = file;
            this.title = title;
            this.kind = kind;
        }
    }

    private static final String IBTIHAL = "ابتهال";
    private static final String TAWSHEEH = "توشيح";

    private static final Track[] ALL = {
            new Track("006", "وازدانت الدنيا بنور محمدٍ", TAWSHEEH),
            new Track("011", "سيدي يا رسول الله", TAWSHEEH),
            new Track("012", "يا رسول الله يا صاحب الخلق العظيم", TAWSHEEH),
            new Track("017", "خير خلق الله طه", TAWSHEEH),
            new Track("022", "وجهت وجهي لله", IBTIHAL),
            new Track("026", "سبحانك ما أعظم شانك", IBTIHAL),
            new Track("030", "سبحان من عنت الوجوه لوجهه", IBTIHAL),
            new Track("037", "مولاي عفوًا إن أسأت", IBTIHAL),
            new Track("038", "سبحان من حمل الملائكة عرشه", IBTIHAL),
            new Track("039", "الحب في الله نورٌ يشرح الصدر", IBTIHAL),
            new Track("040", "تباركت ربنا سبحانك", IBTIHAL),
            new Track("041", "سر الوصول", IBTIHAL),
            new Track("042", "ضياؤك يا إمام", IBTIHAL),
            new Track("043", "مقامك فوق قدر الرسل طُرًّا", IBTIHAL),
            new Track("044", "يا نسيمًا من رياض المصطفى", IBTIHAL),
            new Track("045", "يا رب قلبي", IBTIHAL),
            new Track("046", "لما بدا في الأفق نور", TAWSHEEH),
            new Track("048", "النفس تشكو", IBTIHAL),
            new Track("049", "النقشبندي — مدح رسول الله", TAWSHEEH),
            new Track("050", "توشيح في نسب النبي الشريف", TAWSHEEH),
            new Track("051", "جلّ المنادي ينادي", IBTIHAL),
            new Track("055", "سال دمعي يا إلهي", IBTIHAL),
            new Track("056", "يا مؤنسي في وحدتي", IBTIHAL)
    };

    private static List<Track> cache;

    private Ibtihal() {
    }

    public static String asset(Track t) {
        return "ibtihalat/" + t.file + ".mp3";
    }

    /** الموجود فعليًا داخل التطبيق. */
    public static List<Track> available(Context c) {
        if (cache != null) return cache;
        List<Track> list = new ArrayList<>();
        for (Track t : ALL) {
            try (AssetFileDescriptor afd = c.getAssets().openFd(asset(t))) {
                if (afd.getLength() > 0) list.add(t);
            } catch (Exception ignored) {
            }
        }
        cache = list;
        return list;
    }
}
