package com.salah.app;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * فحص التحديثات: بيقرا آخر إصدار منشور على GitHub، وينزّل الـ APK بـ DownloadManager
 * (بيكمّل لو التطبيق اتقفل)، وبعد التنزيل بيتأكد إن الملف سليم وموقّع بنفس المفتاح
 * قبل ما يعرض التثبيت. مفيش أي اتصال بالنت غير لما المستخدم يضغط الزرار.
 */
public final class Updater {

    static final String API = "https://api.github.com/repos/karezma11/salaty/releases/latest";
    static final String PAGE = "https://github.com/karezma11/salaty/releases/latest";
    static final String MIME = "application/vnd.android.package-archive";
    static final int NOTIF_ID = 80;

    // حالات التحديث المحفوظة
    static final int IDLE = 0;
    static final int DOWNLOADING = 1;
    static final int READY = 2;
    static final int FAILED = 3;
    static final int OTHER_KEY = 4;

    private Updater() {
    }

    /** خطأ برسالة جاهزة للمستخدم (أي IOException تاني = مشكلة في النت). */
    static final class Problem extends IOException {
        Problem(String msg) {
            super(msg);
        }
    }

    /** بيانات آخر إصدار على GitHub. */
    static final class Release {
        String version;
        String notes;
        String url;
        long size;
    }

    // ===== مقارنة النسخ (من غير أندرويد، عشان تتختبر) =====

    /** "v3.10" أو "صلاتي 3.5" ← "3.10" / "3.5". */
    static String clean(String tag) {
        if (tag == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (char ch : tag.trim().toCharArray()) {
            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
                started = true;
            } else if (ch == '.' && started) {
                sb.append(ch);
            } else if (started) {
                break;
            }
        }
        String s = sb.toString();
        while (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** هل نسخة GitHub أحدث من المثبّتة؟ بيقارن رقم رقم (3.10 أحدث من 3.9). */
    static boolean isNewer(String remote, String local) {
        String r = clean(remote);
        if (r.isEmpty()) return false;
        String[] a = r.split("\\.");
        String[] b = clean(local).split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length ? toInt(a[i]) : 0;
            int y = i < b.length ? toInt(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int toInt(String s) {
        try {
            return s.isEmpty() ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** ملاحظات الإصدار من غير رموز Markdown، وبحد أقصى للطول. */
    static String plainNotes(String md) {
        if (md == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : md.replace("\r", "").split("\n")) {
            String l = line.trim();
            while (l.startsWith("#")) l = l.substring(1);
            l = l.replace("**", "").replace("__", "").replace("`", "").trim();
            if (l.startsWith("- ") || l.startsWith("* ")) l = "• " + l.substring(2).trim();
            sb.append(l).append('\n');
        }
        String s = sb.toString().replaceAll("\n{3,}", "\n\n").trim();
        return s.length() > 1500 ? s.substring(0, 1500) + "…" : s;
    }

    // ===== الفحص =====

    /** النسخة المثبّتة دلوقتي. */
    static String installedVersion(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    static long installedCode(Context c) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    /** بيقرا آخر إصدار من GitHub. لازم يتنادي من خيط في الخلفية. */
    static Release fetchLatest() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(API).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "Salaty-Android");
        try {
            int code = conn.getResponseCode();
            if (code == 404) throw new Problem("مفيش إصدارات منشورة لسه");
            if (code == 403 || code == 429) {
                throw new Problem("GitHub موقّف الطلبات مؤقتًا، جرّب كمان ساعة");
            }
            if (code != 200) throw new Problem("رد غير متوقع من GitHub (" + code + ")");
            String body = read(conn.getInputStream());
            org.json.JSONObject j = new org.json.JSONObject(body);
            Release r = new Release();
            r.version = clean(j.optString("tag_name", j.optString("name", "")));
            r.notes = plainNotes(j.optString("body", ""));
            org.json.JSONArray assets = j.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    org.json.JSONObject a = assets.getJSONObject(i);
                    if (a.optString("name", "").toLowerCase().endsWith(".apk")) {
                        r.url = a.optString("browser_download_url", null);
                        r.size = a.optLong("size", 0);
                        break;
                    }
                }
            }
            return r;
        } catch (org.json.JSONException e) {
            throw new Problem("رد GitHub مش مفهوم");
        } finally {
            conn.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    // ===== التنزيل =====

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("updates", Context.MODE_PRIVATE);
    }

    private static File dir(Context c) {
        File d = c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (d != null && !d.exists()) d.mkdirs();
        return d;
    }

    /** يمسح أي APK اتنزّل قبل كده (بياخد مساحة كبيرة). */
    static void cleanup(Context c) {
        File d = dir(c);
        if (d == null) return;
        File[] files = d.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().endsWith(".apk")) f.delete();
        }
    }

    /** بعد تثبيت التحديث: يمسح الملف والحالة. */
    static void afterUpdate(Context c) {
        String v = sp(c).getString("version", null);
        if (v != null && !isNewer(v, installedVersion(c))) {
            cleanup(c);
            sp(c).edit().clear().apply();
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIF_ID);
        }
    }

    /** يبدأ التنزيل. wifiOnly = يستنى الواي فاي. */
    static void download(Context c, Release r, boolean wifiOnly) {
        cleanup(c);
        String name = "salaty-" + r.version + ".apk";
        File target = new File(dir(c), name);
        DownloadManager dm = c.getSystemService(DownloadManager.class);
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(r.url))
                .setTitle("تحديث صلاتي " + r.version)
                .setDescription(wifiOnly ? "هيتنزّل لما يتوصل بالواي فاي" : "جاري التنزيل")
                .setMimeType(MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(c, Environment.DIRECTORY_DOWNLOADS, name)
                .setAllowedOverRoaming(false);
        if (wifiOnly) {
            req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        } else {
            req.setAllowedOverMetered(true);
        }
        req.addRequestHeader("User-Agent", "Salaty-Android");
        long id = dm.enqueue(req);
        sp(c).edit()
                .putLong("id", id)
                .putString("version", r.version)
                .putLong("size", r.size)
                .putString("file", target.getAbsolutePath())
                .putInt("state", DOWNLOADING)
                .putString("error", null)
                .apply();
    }

    static void cancel(Context c) {
        long id = sp(c).getLong("id", -1);
        if (id >= 0) {
            DownloadManager dm = c.getSystemService(DownloadManager.class);
            if (dm != null) dm.remove(id);
        }
        cleanup(c);
        sp(c).edit().clear().apply();
    }

    static int state(Context c) {
        return sp(c).getInt("state", IDLE);
    }

    static String version(Context c) {
        return sp(c).getString("version", "");
    }

    static String error(Context c) {
        return sp(c).getString("error", "");
    }

    static File file(Context c) {
        String f = sp(c).getString("file", null);
        return f == null ? null : new File(f);
    }

    /** نسبة التنزيل (٠–١٠٠)، أو -١ لو مش معروفة. */
    static int progress(Context c) {
        long id = sp(c).getLong("id", -1);
        DownloadManager dm = c.getSystemService(DownloadManager.class);
        if (id < 0 || dm == null) return -1;
        try (Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id))) {
            if (cur == null || !cur.moveToFirst()) return -1;
            long done = cur.getLong(cur.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = cur.getLong(cur.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            if (total <= 0) total = sp(c).getLong("size", 0);
            return total > 0 ? (int) (done * 100 / total) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** لو التنزيل اتمسح من برّه (من إشعار النظام مثلًا) نرجع للحالة العادية. */
    static void refresh(Context c) {
        if (state(c) != DOWNLOADING) return;
        long id = sp(c).getLong("id", -1);
        DownloadManager dm = c.getSystemService(DownloadManager.class);
        if (dm == null) return;
        try (Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id))) {
            if (cur == null || !cur.moveToFirst()) {
                sp(c).edit().clear().apply();
                return;
            }
            int status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL
                    || status == DownloadManager.STATUS_FAILED) {
                onComplete(c, id);
            }
        } catch (Exception ignored) {
        }
    }

    /** بعد ما DownloadManager يخلّص: نتأكد من الملف ونعرض التثبيت. */
    static synchronized void onComplete(Context c, long id) {
        if (id < 0 || id != sp(c).getLong("id", -2)) return;
        if (state(c) != DOWNLOADING) return;
        DownloadManager dm = c.getSystemService(DownloadManager.class);
        int status = -1;
        int reason = 0;
        try (Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id))) {
            if (cur != null && cur.moveToFirst()) {
                status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                reason = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
            }
        } catch (Exception ignored) {
        }

        String ver = version(c);
        File f = file(c);
        if (status != DownloadManager.STATUS_SUCCESSFUL || f == null || !f.exists()) {
            fail(c, reason == DownloadManager.ERROR_INSUFFICIENT_SPACE
                    ? "مفيش مساحة كفاية على الموبايل. فضّي مساحة وجرّب تاني."
                    : "التنزيل ما كملش. اتأكد من النت وجرّب تاني من الإعدادات.");
            return;
        }
        long expected = sp(c).getLong("size", 0);
        if (expected > 0 && f.length() != expected) {
            fail(c, "الملف اتنزّل ناقص. جرّب تاني من الإعدادات.");
            return;
        }

        PackageManager pm = c.getPackageManager();
        PackageInfo apk = pm.getPackageArchiveInfo(f.getAbsolutePath(), 0);
        if (apk == null || !c.getPackageName().equals(apk.packageName)) {
            fail(c, "الملف اللي اتنزّل مش تحديث لصلاتي.");
            return;
        }
        long code = Build.VERSION.SDK_INT >= 28 ? apk.getLongVersionCode() : apk.versionCode;
        if (code <= installedCode(c)) {
            fail(c, "الملف اللي اتنزّل مش أحدث من النسخة المثبّتة.");
            return;
        }

        if (Boolean.FALSE.equals(sameSigner(c, f))) {
            // الملف مش هينفع يتركّب، ولو اتمسح التطبيق هيتمسح معاه، فمالوش لازمة
            f.delete();
            sp(c).edit().putInt("state", OTHER_KEY).apply();
            notifyUser(c, "تحديث صلاتي " + ver + " محتاج خطوة منك",
                    "النسخة اللي على موبايلك موقّعة بمفتاح مختلف. افتح الإعدادات ← فحص التحديثات.",
                    openSettings(c));
            return;
        }

        sp(c).edit().putInt("state", READY).apply();
        notifyUser(c, "تحديث صلاتي " + ver + " جاهز",
                "اضغط هنا عشان تثبّته. إعداداتك وعلاماتك هتفضل زي ما هي.",
                PendingIntent.getActivity(c, 81, installIntent(c, f),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }

    private static void fail(Context c, String why) {
        File f = file(c);
        if (f != null) f.delete();
        sp(c).edit().putInt("state", FAILED).putString("error", why).apply();
        notifyUser(c, "تحديث صلاتي ما اتنزّلش", why, openSettings(c));
    }

    private static PendingIntent openSettings(Context c) {
        return PendingIntent.getActivity(c, 82,
                new Intent(c, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void notifyUser(Context c, String title, String text, PendingIntent pi) {
        Notifications.showWith(c, Notifications.CH_UPDATES, NOTIF_ID, title, text, pi);
    }

    /** Intent التثبيت: الملف بيتبعت لمثبّت أندرويد عن طريق FileProvider. */
    static Intent installIntent(Context c, File f) {
        Uri uri = androidx.core.content.FileProvider.getUriForFile(
                c, c.getPackageName() + ".files", f);
        return new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * هل الملف موقّع بنفس مفتاح النسخة المثبّتة؟
     * null = مش عارفين نحدد (بنسيب المثبّت نفسه يقرر).
     */
    @SuppressWarnings("deprecation")
    static Boolean sameSigner(Context c, File apk) {
        try {
            PackageManager pm = c.getPackageManager();
            Set<String> mine;
            Set<String> theirs;
            if (Build.VERSION.SDK_INT >= 28) {
                int flag = PackageManager.GET_SIGNING_CERTIFICATES;
                mine = signers(pm.getPackageInfo(c.getPackageName(), flag));
                theirs = signers(pm.getPackageArchiveInfo(apk.getAbsolutePath(), flag));
            } else {
                int flag = PackageManager.GET_SIGNATURES;
                mine = sigs(pm.getPackageInfo(c.getPackageName(), flag).signatures);
                PackageInfo a = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flag);
                theirs = a == null ? null : sigs(a.signatures);
            }
            if (mine == null || theirs == null || mine.isEmpty() || theirs.isEmpty()) return null;
            for (String s : theirs) {
                if (mine.contains(s)) return true;
            }
            return false;
        } catch (Exception e) {
            return null;
        }
    }

    private static Set<String> signers(PackageInfo p) {
        if (p == null || Build.VERSION.SDK_INT < 28 || p.signingInfo == null) return null;
        Signature[] s = p.signingInfo.hasMultipleSigners()
                ? p.signingInfo.getApkContentsSigners()
                : p.signingInfo.getSigningCertificateHistory();
        return sigs(s);
    }

    private static Set<String> sigs(Signature[] s) {
        if (s == null) return null;
        Set<String> out = new HashSet<>();
        for (Signature x : s) out.add(x.toCharsString());
        return out;
    }

    /**
     * النسخة المثبّتة موقّعة بمفتاح الـ debug؟ يبقى أي نسخة رسمية مش هتتركّب فوقها،
     * وبنقول للمستخدم قبل ما ينزّل ٣٠٠ ميجا على الفاضي.
     */
    @SuppressWarnings("deprecation")
    static boolean isDebugSigned(Context c) {
        try {
            Signature[] s = c.getPackageManager()
                    .getPackageInfo(c.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
            java.security.cert.CertificateFactory cf =
                    java.security.cert.CertificateFactory.getInstance("X.509");
            for (Signature x : s) {
                java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate)
                        cf.generateCertificate(new java.io.ByteArrayInputStream(x.toByteArray()));
                if (cert.getSubjectX500Principal().getName().contains("Android Debug")) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /** لسه المستخدم ما سمحش للتطبيق يثبّت تطبيقات؟ */
    static boolean needsInstallPermission(Context c) {
        return Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls();
    }

    static Intent installPermissionIntent(Context c) {
        return new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + c.getPackageName()));
    }
}
