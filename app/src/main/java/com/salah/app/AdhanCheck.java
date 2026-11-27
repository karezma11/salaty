package com.salah.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * الأذان على موبايلات كتير (شاومي، أوبو، ريلمي، فيفو، إنفينكس، سامسونج...) مابيشتغلش لأن النظام
 * بيوقّف التطبيق في الخلفية. هنا: السماح بالشغل في الخلفية، وشاشة «التشغيل التلقائي» بتاعة الشركة،
 * وتجربة الأذان الحقيقية بعد دقيقة، وسجل آخر محاولات الأذان، وتقرير فحص المستخدم يبعته.
 */
final class AdhanCheck {

    private static final int REQ_TEST = 10;
    private static final String LOG = "adhan_log";
    private static final int LOG_LINES = 12;

    private AdhanCheck() {
    }

    /** التطبيق برّه «توفير البطارية»؟ */
    static boolean batteryFree(Context c) {
        try {
            PowerManager pm = c.getSystemService(PowerManager.class);
            return pm == null || pm.isIgnoringBatteryOptimizations(c.getPackageName());
        } catch (Exception e) {
            return true;
        }
    }

    /** المستخدم (أو النظام) قافل الخلفية للتطبيق خالص من إعدادات البطارية. */
    static boolean backgroundRestricted(Context c) {
        if (Build.VERSION.SDK_INT < 28) return false;
        try {
            ActivityManager am = c.getSystemService(ActivityManager.class);
            return am != null && am.isBackgroundRestricted();
        } catch (Exception e) {
            return false;
        }
    }

    /** نافذة النظام «السماح لصلاتي بالشغل في الخلفية». */
    static void askBattery(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + a.getPackageName())));
            return;
        } catch (Exception ignored) {
        }
        openAppSettings(a);
        Toast.makeText(a, "افتح «البطارية» واختار «بدون قيود»", Toast.LENGTH_LONG).show();
    }

    static void openAppSettings(Context c) {
        try {
            c.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + c.getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
        }
    }

    /** شاشات «التشغيل التلقائي» عند الشركات اللي بتقفل التطبيقات في الخلفية. */
    private static final String[][] AUTOSTART = {
            {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
            {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
            {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
            {"com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"},
            {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
            {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
            {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"},
            {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
            {"com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"},
            {"com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
            {"com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"},
            {"com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"},
            {"com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"},
            {"com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"},
    };

    private static final String[] MAKERS = {
            "xiaomi", "redmi", "poco", "oppo", "realme", "oneplus", "vivo", "iqoo", "huawei",
            "honor", "infinix", "tecno", "itel", "samsung", "asus"};

    /** الشركة من اللي عندها «تشغيل تلقائي» أو «تطبيقات نايمة» بتوقف الأذان؟ */
    static boolean knownMaker() {
        String m = (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase(Locale.US);
        for (String k : MAKERS) {
            if (m.contains(k)) return true;
        }
        return false;
    }

    static String makerName() {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        return m.isEmpty() ? "الموبايل" : Character.toUpperCase(m.charAt(0)) + m.substring(1);
    }

    /** يفتح شاشة التشغيل التلقائي لو لقاها، وإلا إعدادات التطبيق مع شرح. */
    static void openAutostart(Activity a) {
        for (String[] s : AUTOSTART) {
            try {
                a.startActivity(new Intent().setComponent(new ComponentName(s[0], s[1]))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Toast.makeText(a, "فعّل «صلاتي» في القايمة دي", Toast.LENGTH_LONG).show();
                return;
            } catch (Exception ignored) {
                // الشاشة مش موجودة على النسخة دي، نجرّب اللي بعدها
            }
        }
        openAppSettings(a);
        Toast.makeText(a, "دوّر على «التشغيل التلقائي» أو «البطارية ← بدون قيود» وفعّلهم",
                Toast.LENGTH_LONG).show();
    }

    /** تجربة حقيقية: نفس طريق الأذان (منبّه ← AdhanReceiver ← خدمة) بعد دقيقة. */
    static void testInOneMinute(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent i = new Intent(c, AdhanReceiver.class)
                .putExtra("prayer", PrayerTimes.DHUHR)
                .putExtra(AdhanReceiver.EXTRA_TEST, true);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ_TEST, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long at = System.currentTimeMillis() + 60000L;
        if (Scheduler.canExact(c)) {
            PendingIntent show = PendingIntent.getActivity(c, REQ_TEST,
                    new Intent(c, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        record(c, "اتجدولت تجربة أذان بعد دقيقة");
    }

    /** سطر في سجل آخر محاولات الأذان (commit لأن العملية ممكن تتقفل بعدها على طول). */
    static void record(Context c, String what) {
        try {
            SharedPreferences sp = c.getSharedPreferences("salah", Context.MODE_PRIVATE);
            String time = new SimpleDateFormat("d/M HH:mm:ss", Locale.US).format(new Date());
            String all = time + "  " + what;
            String old = sp.getString(LOG, "");
            if (!old.isEmpty()) all += "\n" + old;
            String[] lines = all.split("\n");
            if (lines.length > LOG_LINES) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < LOG_LINES; i++) {
                    if (i > 0) sb.append('\n');
                    sb.append(lines[i]);
                }
                all = sb.toString();
            }
            sp.edit().putString(LOG, all).commit();
        } catch (Exception ignored) {
        }
    }

    /** صوت المنبّه (الأذان بيطلع عليه)، أو -1 لو مش معروف. */
    static int alarmVolume(Context c) {
        try {
            AudioManager am = c.getSystemService(AudioManager.class);
            return am == null ? -1 : am.getStreamVolume(AudioManager.STREAM_ALARM);
        } catch (Exception e) {
            return -1;
        }
    }

    /** تقرير «فحص الأذان»: كل حاجة ممكن توقف الأذان على الموبايل ده، وآخر المحاولات. */
    static String report(Context c) {
        StringBuilder sb = new StringBuilder();
        line(sb, "الأذان في التطبيق", Prefs.adhanEnabled(c) ? "مفعّل" : "متوقف ✗");
        line(sb, "التنبيهات الدقيقة", Scheduler.canExact(c) ? "مسموحة" : "ممنوعة ✗");
        line(sb, "الشغل في الخلفية", batteryFree(c) ? "مسموح" : "عليه توفير بطارية ✗");
        if (backgroundRestricted(c)) line(sb, "تقييد الخلفية", "مفعّل ✗ الأذان مش هيشتغل");
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) {
                line(sb, "الإشعارات", nm.areNotificationsEnabled() ? "مسموحة" : "ممنوعة ✗");
                line(sb, "عدم الإزعاج", dnd(nm.getCurrentInterruptionFilter()));
            }
        } catch (Exception ignored) {
        }
        int vol = alarmVolume(c);
        if (vol >= 0) {
            AudioManager am = c.getSystemService(AudioManager.class);
            int max = am == null ? 0 : am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            line(sb, "صوت المنبّه", vol == 0 ? "صفر ✗ الأذان بيطلع على صوت المنبّه"
                    : Ui.arabicDigits(vol) + " من " + Ui.arabicDigits(max));
        }
        line(sb, "كتم وقت الصلاة", Prefs.silencePrayer(c) ? "مفعّل" : "متوقف");
        try {
            long[] next = Scheduler.nextPrayer(c);
            line(sb, "الأذان الجاي", PrayerTimes.NAMES[(int) next[0]] + " " + clock(next[1]));
            AlarmManager am = c.getSystemService(AlarmManager.class);
            AlarmManager.AlarmClockInfo info = am == null ? null : am.getNextAlarmClock();
            boolean ours = info != null && info.getShowIntent() != null
                    && c.getPackageName().equals(info.getShowIntent().getCreatorPackage());
            line(sb, "أقرب منبّه في النظام", ours ? "بتاع صلاتي ✓ " + clock(info.getTriggerTime())
                    : info == null ? "مفيش ✗" : "منبّه تاني " + clock(info.getTriggerTime()));
        } catch (Exception ignored) {
        }
        line(sb, "الموبايل", Build.MANUFACTURER + " " + Build.MODEL
                + " · أندرويد " + Build.VERSION.RELEASE);
        String log = c.getSharedPreferences("salah", Context.MODE_PRIVATE).getString(LOG, "");
        sb.append("\nآخر محاولات الأذان:\n").append(log.isEmpty() ? "لسه مفيش" : log);
        return sb.toString();
    }

    private static void line(StringBuilder sb, String k, String v) {
        sb.append(k).append(": ").append(v).append('\n');
    }

    private static String clock(long millis) {
        return TimesCard.digits(new SimpleDateFormat("h:mm a", Locale.US).format(new Date(millis))
                .replace("AM", "ص").replace("PM", "م"));
    }

    private static String dnd(int filter) {
        switch (filter) {
            case NotificationManager.INTERRUPTION_FILTER_ALL:
                return "مقفول";
            case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                return "أولوية";
            case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                return "المنبّهات بس";
            case NotificationManager.INTERRUPTION_FILTER_NONE:
                return "صمت تام ✗";
            default:
                return "غير معروف";
        }
    }
}
