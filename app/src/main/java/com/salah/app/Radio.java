package com.salah.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** إذاعات القرآن الكريم (بث مباشر). */
public final class Radio {

    public static final class Station {
        public final String id;
        public final String name;
        public final String url;

        Station(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }

    private static List<Station> cache;

    private Radio() {
    }

    public static List<Station> all(Context c) {
        if (cache != null) return cache;
        List<Station> list = new ArrayList<>();
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(
                    c.getAssets().open("radios.txt"), "UTF-8"), 32768);
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|", 3);
                if (p.length < 3) continue;
                String name = p[1].trim();
                String url = p[2].trim();
                if (name.isEmpty() || url.isEmpty()) continue;
                list.add(new Station(p[0].trim(), name, url));
            }
        } catch (IOException ignored) {
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {
                }
            }
        }
        // إذاعتَي القرآن الكريم (القاهرة ثم السعودية) في أول القائمة
        promote(list, "السعودية");
        promote(list, "من القاهرة");
        cache = list;
        return list;
    }

    /** يرفع إذاعة لأول القائمة. */
    private static void promote(List<Station> list, String needle) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.contains(needle)) {
                list.add(0, list.remove(i));
                return;
            }
        }
    }

    public static List<Station> search(Context c, String query) {
        List<Station> out = new ArrayList<>();
        String q = Quran.normalize(query);
        if (q.isEmpty()) return all(c);
        for (Station s : all(c)) {
            if (Quran.normalize(s.name).contains(q)) out.add(s);
        }
        return out;
    }
}
