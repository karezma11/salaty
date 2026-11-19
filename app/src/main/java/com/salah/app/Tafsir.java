package com.salah.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/** التفاسير المضمّنة داخل التطبيق. */
public final class Tafsir {

    public static final class Book {
        public final String id;
        public final String name;
        public final String author;
        public final String asset;

        Book(String id, String name, String author, String asset) {
            this.id = id;
            this.name = name;
            this.author = author;
            this.asset = asset;
        }
    }

    public static final Book[] BOOKS = {
            new Book("mokhtasar", "المختصر في التفسير", "مركز تفسير", "tafsir.txt"),
            new Book("jalalain", "تفسير الجلالين", "المحلّي والسيوطي", "tafsir_jalalain.txt")
    };

    public static final String SOURCE =
            "المختصر في التفسير (مركز تفسير) · تفسير الجلالين";

    private static Map<Integer, String> cache;
    private static String cachedBook = "";

    private Tafsir() {
    }

    public static Book book(String id) {
        for (Book b : BOOKS) {
            if (b.id.equals(id)) return b;
        }
        return BOOKS[0];
    }

    public static Book current(Context c) {
        return book(Prefs.tafsirBook(c));
    }

    public static String[] names() {
        String[] n = new String[BOOKS.length];
        for (int i = 0; i < BOOKS.length; i++) {
            n[i] = BOOKS[i].name + "  ·  " + BOOKS[i].author;
        }
        return n;
    }

    /** تفسير آية من الكتاب المختار، أو null. */
    public static String of(Context c, int sura, int ayah) {
        Book b = current(c);
        if (cache == null || !b.id.equals(cachedBook)) {
            cache = load(c, b);
            cachedBook = b.id;
        }
        return cache.get(sura * 1000 + ayah);
    }

    /** يلغي الذاكرة المؤقتة بعد تغيير الكتاب. */
    public static void invalidate() {
        cache = null;
        cachedBook = "";
    }

    private static Map<Integer, String> load(Context c, Book b) {
        Map<Integer, String> map = new HashMap<>(7000);
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(
                    c.getAssets().open(b.asset), "UTF-8"), 65536);
            String line;
            while ((line = r.readLine()) != null) {
                int b1 = line.indexOf('|');
                if (b1 <= 0) continue;
                int b2 = line.indexOf('|', b1 + 1);
                if (b2 <= b1) continue;
                try {
                    int sura = Integer.parseInt(line.substring(0, b1));
                    int ayah = Integer.parseInt(line.substring(b1 + 1, b2));
                    map.put(sura * 1000 + ayah, line.substring(b2 + 1));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            return map;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {
                }
            }
        }
        return map;
    }
}
