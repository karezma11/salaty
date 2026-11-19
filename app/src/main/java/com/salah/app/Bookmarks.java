package com.salah.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** العلامات المرجعية: سورة وآية، محفوظة على الجهاز. */
public final class Bookmarks {

    public static final class Mark implements Comparable<Mark> {
        public final int sura;
        public final int ayah;

        Mark(int sura, int ayah) {
            this.sura = sura;
            this.ayah = ayah;
        }

        String key() {
            return sura + ":" + ayah;
        }

        @Override
        public int compareTo(Mark o) {
            return sura != o.sura ? Integer.compare(sura, o.sura) : Integer.compare(ayah, o.ayah);
        }
    }

    private static final String KEY = "bookmarks";

    private Bookmarks() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("salah", Context.MODE_PRIVATE);
    }

    private static Set<String> raw(Context c) {
        return new HashSet<>(sp(c).getStringSet(KEY, new HashSet<>()));
    }

    public static boolean has(Context c, int sura, int ayah) {
        return raw(c).contains(sura + ":" + ayah);
    }

    public static void toggle(Context c, int sura, int ayah) {
        Set<String> set = raw(c);
        String k = sura + ":" + ayah;
        if (!set.remove(k)) set.add(k);
        sp(c).edit().putStringSet(KEY, set).apply();
    }

    public static void remove(Context c, int sura, int ayah) {
        Set<String> set = raw(c);
        set.remove(sura + ":" + ayah);
        sp(c).edit().putStringSet(KEY, set).apply();
    }

    public static void clear(Context c) {
        sp(c).edit().remove(KEY).apply();
    }

    /** كل العلامات مرتبة بترتيب المصحف. */
    public static List<Mark> all(Context c) {
        List<Mark> list = new ArrayList<>();
        for (String s : raw(c)) {
            int colon = s.indexOf(':');
            if (colon <= 0) continue;
            try {
                list.add(new Mark(Integer.parseInt(s.substring(0, colon)),
                        Integer.parseInt(s.substring(colon + 1))));
            } catch (NumberFormatException ignored) {
            }
        }
        Collections.sort(list);
        return list;
    }

    public static int count(Context c) {
        return raw(c).size();
    }

    // ---------- المفضلة العامة: أذكار وابتهالات ----------

    private static final String FAV = "favourites";

    private static Set<String> rawFav(Context c) {
        return new HashSet<>(sp(c).getStringSet(FAV, new HashSet<>()));
    }

    public static boolean hasFav(Context c, String key) {
        return rawFav(c).contains(key);
    }

    public static void toggleFav(Context c, String key) {
        Set<String> set = rawFav(c);
        if (!set.remove(key)) set.add(key);
        sp(c).edit().putStringSet(FAV, set).apply();
    }

    public static void removeFav(Context c, String key) {
        Set<String> set = rawFav(c);
        set.remove(key);
        sp(c).edit().putStringSet(FAV, set).apply();
    }

    public static List<String> favs(Context c) {
        List<String> list = new ArrayList<>(rawFav(c));
        Collections.sort(list);
        return list;
    }

    public static int favCount(Context c) {
        return rawFav(c).size();
    }

    public static void clearFavs(Context c) {
        sp(c).edit().remove(FAV).apply();
    }
}
