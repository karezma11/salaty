package com.salah.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** تحميل ملفات التلاوة من المصادر المتاحة. */
public final class Downloads {

    /** المصادر بالترتيب: لو الأول وقع نجرّب اللي بعده. */
    private static String[] mirrors(Context c) {
        String folder = Reciters.current(c).folder;
        return new String[]{
                "https://everyayah.com/data/" + folder + "/",
                "https://www.everyayah.com/data/" + folder + "/"
        };
    }

    private Downloads() {
    }

    public static boolean online(Context c) {
        try {
            ConnectivityManager cm = c.getSystemService(ConnectivityManager.class);
            if (cm == null) return false;
            NetworkCapabilities n = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return n != null && n.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean onWifi(Context c) {
        try {
            ConnectivityManager cm = c.getSystemService(ConnectivityManager.class);
            if (cm == null) return false;
            NetworkCapabilities n = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return n != null && n.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } catch (Exception e) {
            return false;
        }
    }

    public static File target(Context c, int sura, int ayah) {
        File dir = new File(Recitation.folderOf(c, Prefs.reciter(c)),
                String.format(java.util.Locale.US, "%03d", sura));
        return new File(dir, String.format(java.util.Locale.US, "%03d%03d.mp3", sura, ayah));
    }

    /** ينزّل آية واحدة. يرجع true لو الملف بقى موجود وسليم. */
    public static boolean fetchAyah(Context c, int sura, int ayah) {
        File out = target(c, sura, ayah);
        if (out.exists() && out.length() > 500) return true;
        // التحميل القديم لعبد الباسط يُحتسب كأنه موجود
        if (Recitation.externalFile(c, sura, ayah) != null) return true;
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        String name = String.format(java.util.Locale.US, "%03d%03d.mp3", sura, ayah);
        File tmp = new File(out.getAbsolutePath() + ".part");

        for (String mirror : mirrors(c)) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(mirror + name).openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("User-Agent", "SalatyApp");
                if (conn.getResponseCode() != 200) continue;
                try (InputStream in = conn.getInputStream();
                     FileOutputStream os = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        os.write(buf, 0, n);
                    }
                }
                if (tmp.length() > 500 && tmp.renameTo(out)) {
                    Recitation.invalidate();
                    return true;
                }
                tmp.delete();
            } catch (Exception e) {
                tmp.delete();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return false;
    }

    /** عدد آيات السورة الموجودة على الموبايل. */
    public static int haveCount(Context c, int sura, int ayas) {
        int n = 0;
        for (int a = 1; a <= ayas; a++) {
            File f = target(c, sura, a);
            if (f.exists() && f.length() > 500) n++;
        }
        return n;
    }

    /** يمسح تلاوة سورة من الموبايل. */
    public static void delete(Context c, int sura, int ayas) {
        Recitation.invalidate();
        for (int a = 1; a <= ayas; a++) {
            File f = target(c, sura, a);
            if (f.exists()) f.delete();
        }
    }
}
