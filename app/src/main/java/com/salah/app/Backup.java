package com.salah.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/** نسخ احتياطي للإعدادات والعلامات وسجل الصلوات. */
public final class Backup {

    private Backup() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("salah", Context.MODE_PRIVATE);
    }

    /** يكتب كل الإعدادات في ملف JSON ويرجّع مكانه. */
    public static File export(Context c) throws Exception {
        JSONObject root = new JSONObject();
        for (Map.Entry<String, ?> e : sp(c).getAll().entrySet()) {
            Object v = e.getValue();
            if (v instanceof Set) {
                root.put(e.getKey(), new org.json.JSONArray((Set<?>) v));
            } else {
                root.put(e.getKey(), v);
            }
        }
        JSONObject out = new JSONObject();
        out.put("app", "salaty");
        out.put("version", 1);
        out.put("saved", System.currentTimeMillis());
        out.put("data", root);

        File dir = new File(c.getCacheDir(), "share");
        dir.mkdirs();
        File f = new File(dir, "salaty-backup.json");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(out.toString(2).getBytes("UTF-8"));
        }
        return f;
    }

    /** يقرأ ملف نسخة احتياطية ويطبّقه. يرجّع عدد القيم المستوردة. */
    public static int restore(Context c, InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            sb.append(new String(buf, 0, n, "UTF-8"));
        }
        JSONObject out = new JSONObject(sb.toString());
        if (!"salaty".equals(out.optString("app"))) {
            throw new IllegalArgumentException("الملف ده مش نسخة احتياطية من صلاتي");
        }
        JSONObject data = out.getJSONObject("data");
        SharedPreferences.Editor e = sp(c).edit();
        int count = 0;
        java.util.Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            Object v = data.get(k);
            if (v instanceof org.json.JSONArray) {
                org.json.JSONArray arr = (org.json.JSONArray) v;
                java.util.Set<String> set = new java.util.HashSet<>();
                for (int i = 0; i < arr.length(); i++) set.add(arr.getString(i));
                e.putStringSet(k, set);
            } else if (v instanceof Boolean) {
                e.putBoolean(k, (Boolean) v);
            } else if (v instanceof Integer) {
                e.putInt(k, (Integer) v);
            } else if (v instanceof Long) {
                e.putLong(k, (Long) v);
            } else if (v instanceof Double) {
                e.putFloat(k, ((Double) v).floatValue());
            } else {
                e.putString(k, String.valueOf(v));
            }
            count++;
        }
        e.apply();
        return count;
    }
}
