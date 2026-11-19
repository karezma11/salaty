package com.salah.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** الأذكار: أقسام، ثم أبواب، ثم أذكار بنصها وصوتها. */
public final class Dhikr {

    public static final class Category {
        public final int id;
        public final String name;

        Category(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static final class Chapter {
        public final int id;
        public final int categoryId;
        public final String name;

        Chapter(int id, int categoryId, String name) {
            this.id = id;
            this.categoryId = categoryId;
            this.name = name;
        }
    }

    public static final class Item {
        public final int id;
        public final int chapterId;
        public final String text;
        public final String reference;

        Item(int id, int chapterId, String text, String reference) {
            this.id = id;
            this.chapterId = chapterId;
            this.text = text;
            this.reference = reference;
        }
    }

    private static List<Category> categories;
    private static List<Chapter> chapters;
    private static List<Item> items;

    private Dhikr() {
    }

    private static List<String[]> read(Context c, String name, int parts) {
        List<String[]> rows = new ArrayList<>();
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(
                    c.getAssets().open("adhkar/" + name), "UTF-8"), 32768);
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|", parts);
                if (p.length >= parts - 1) rows.add(p);
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
        return rows;
    }

    public static List<Category> categories(Context c) {
        if (categories != null) return categories;
        List<Category> list = new ArrayList<>();
        for (String[] p : read(c, "categories.txt", 2)) {
            try {
                list.add(new Category(Integer.parseInt(p[0].trim()), p[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        categories = list;
        return list;
    }

    public static List<Chapter> chapters(Context c, int categoryId) {
        if (chapters == null) {
            List<Chapter> list = new ArrayList<>();
            for (String[] p : read(c, "chapters.txt", 3)) {
                try {
                    list.add(new Chapter(Integer.parseInt(p[0].trim()),
                            Integer.parseInt(p[1].trim()), p[2]));
                } catch (NumberFormatException ignored) {
                }
            }
            chapters = list;
        }
        List<Chapter> out = new ArrayList<>();
        for (Chapter ch : chapters) {
            if (ch.categoryId == categoryId) out.add(ch);
        }
        return out;
    }

    public static List<Item> items(Context c, int chapterId) {
        if (items == null) {
            List<Item> list = new ArrayList<>();
            for (String[] p : read(c, "items.txt", 4)) {
                try {
                    list.add(new Item(Integer.parseInt(p[0].trim()),
                            Integer.parseInt(p[1].trim()), p[2],
                            p.length > 3 ? p[3] : ""));
                } catch (NumberFormatException ignored) {
                }
            }
            items = list;
        }
        List<Item> out = new ArrayList<>();
        for (Item it : items) {
            if (it.chapterId == chapterId) out.add(it);
        }
        return out;
    }

    public static int countIn(Context c, int categoryId) {
        int n = 0;
        for (Chapter ch : chapters(c, categoryId)) n += items(c, ch.id).size();
        return n;
    }

    public static String asset(Item it) {
        return String.format("adhkar/%03d.mp3", it.id);
    }

    public static boolean hasAudio(Context c, Item it) {
        AssetFileDescriptor fd = null;
        try {
            fd = c.getAssets().openFd(asset(it));
            return fd.getLength() > 0;
        } catch (IOException e) {
            return false;
        } finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** بحث في نصوص الأذكار. */
    public static List<Item> search(Context c, String query, int limit) {
        List<Item> out = new ArrayList<>();
        String q = Quran.normalize(query);
        if (q.isEmpty()) return out;
        for (Category cat : categories(c)) {
            for (Chapter ch : chapters(c, cat.id)) {
                for (Item it : items(c, ch.id)) {
                    if (Quran.normalize(it.text).contains(q)) {
                        out.add(it);
                        if (out.size() >= limit) return out;
                    }
                }
            }
        }
        return out;
    }

    /** اسم الباب اللي فيه ذكر معيّن. */
    public static String chapterNameOf(Context c, Item item) {
        for (Category cat : categories(c)) {
            for (Chapter ch : chapters(c, cat.id)) {
                if (ch.id == item.chapterId) return ch.name;
            }
        }
        return "";
    }
}
