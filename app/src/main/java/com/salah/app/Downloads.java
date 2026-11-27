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
    private static String[] mirrors(String reciter) {
        String folder = Reciters.byId(reciter).folder;
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
        return target(c, Prefs.reciter(c), sura, ayah);
    }

    /** ملف آية لقارئ معيّن (مش لازم يكون المختار) — تحميل المصحف لكل القراء. */
    public static File target(Context c, String reciter, int sura, int ayah) {
        File dir = new File(Recitation.folderOf(c, reciter),
                String.format(java.util.Locale.US, "%03d", sura));
        return new File(dir, String.format(java.util.Locale.US, "%03d%03d.mp3", sura, ayah));
    }

    /** ينزّل آية واحدة. يرجع true لو الملف بقى موجود وسليم. */
    public static boolean fetchAyah(Context c, int sura, int ayah) {
        return fetchAyah(c, Prefs.reciter(c), sura, ayah);
    }

    public static boolean fetchAyah(Context c, String reciter, int sura, int ayah) {
        File out = target(c, reciter, sura, ayah);
        if (out.exists() && out.length() > 500) return true;
        // التحميل القديم لعبد الباسط يُحتسب كأنه موجود
        if (reciter.equals(Prefs.reciter(c)) && Recitation.externalFile(c, sura, ayah) != null) {
            return true;
        }
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        String name = String.format(java.util.Locale.US, "%03d%03d.mp3", sura, ayah);
        File tmp = new File(out.getAbsolutePath() + ".part");

        for (String mirror : mirrors(reciter)) {
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
        return haveCount(c, Prefs.reciter(c), sura, ayas);
    }

    public static int haveCount(Context c, String reciter, int sura, int ayas) {
        int n = 0;
        for (int a = 1; a <= ayas; a++) {
            File f = target(c, reciter, sura, a);
            if (f.exists() && f.length() > 500) n++;
        }
        return n;
    }

    /** الحجم التقريبي للمصحف كامل بكل القراء (٦٤ ك.ب ≈ ٥٥٠ م.ب، ١٢٨ ≈ ١١٠٠، ٤٠ ≈ ٣٥٠). */
    static long everyoneBytes() {
        long mb = 0;
        for (Reciters.Reciter r : Reciters.ALL) {
            mb += r.folder.contains("128") ? 1100 : r.folder.contains("40kbps") ? 350 : 550;
        }
        return mb * 1024L * 1024L;
    }

    /** «حمّل المصحف كامل لكل القراء»: تأكيد بالحجم والمساحة، وبعدين التحميل في الخلفية. */
    static void askEveryone(android.app.Activity a) {
        long need = everyoneBytes();
        long free = Recitation.folder(a).getParentFile() != null
                ? Recitation.folder(a).getParentFile().getUsableSpace() : 0;
        StringBuilder names = new StringBuilder();
        for (Reciters.Reciter r : Reciters.ALL) {
            if (names.length() > 0) names.append("، ");
            names.append(r.name);
        }
        String gb = gb(need);
        String msg = "هيتحمّل المصحف كامل (١١٤ سورة) بأصوات القراء الـ"
                + Ui.arabicDigits(Reciters.ALL.length) + ": " + names + ".\n\n"
                + "الحجم حوالي " + gb + " جيجا، وبياخد وقت طويل. المحمّل قبل كده مش بيتحمّل تاني، "
                + "ولو وقف تدوس تاني ويكمّل من مكانه.\n\n"
                + "المساحة الفاضية على الموبايل: " + gb(free) + " جيجا.";
        if (free > 0 && free < need + 500L * 1024 * 1024) {
            msg += "\n\n⚠ المساحة مش كفاية للكل: هيحمّل لحد ما المساحة تخلص.";
        }
        new androidx.appcompat.app.AlertDialog.Builder(a)
                .setTitle("تحميل المصحف لكل القراء")
                .setMessage(msg)
                .setPositiveButton("على الواي فاي بس", (d, w) -> startEveryone(a, true))
                .setNeutralButton("بأي نت", (d, w) -> startEveryone(a, false))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private static void startEveryone(android.app.Activity a, boolean wifiOnly) {
        if (!online(a)) {
            android.widget.Toast.makeText(a, "محتاج نت", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (wifiOnly && !onWifi(a)) {
            android.widget.Toast.makeText(a, "اتصل بواي فاي الأول، أو اختار «بأي نت»",
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        DownloadService.startEveryone(a, wifiOnly);
        android.widget.Toast.makeText(a, "بدأ التحميل — تقدر تتابعه من الإشعار",
                android.widget.Toast.LENGTH_LONG).show();
    }

    private static String gb(long bytes) {
        long tenths = Math.round(bytes / (1024.0 * 1024 * 1024) * 10);
        return Ui.arabicDigits(tenths / 10) + (tenths % 10 == 0 ? "" : "٫" + Ui.arabicDigits(tenths % 10));
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
