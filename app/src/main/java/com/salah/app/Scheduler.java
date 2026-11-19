package com.salah.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.TimeZone;

/** حساب مواقيت اليوم وجدولة تنبيه الأذان وتذكير الصلاة على النبي. */
public final class Scheduler {

    private static final int REQ_ADHAN = 1;
    private static final int REQ_SALAWAT = 2;
    private static final int REQ_SHOW = 3;
    private static final int REQ_IQAMA = 4;
    private static final int REQ_FASTING = 5;
    private static final int REQ_ADHKAR_AM = 6;
    private static final int REQ_ADHKAR_PM = 7;
    private static final int REQ_SILENCE = 8;
    private static final int REQ_PRE = 9;

    private Scheduler() {
    }

    /** مواقيت يوم معين (dayOffset: 0 اليوم، 1 بكرة) بالساعات المحلية. */
    public static double[] timesFor(Context c, int dayOffset) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dayOffset);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        double tz = TimeZone.getDefault().getOffset(cal.getTimeInMillis()) / 3600000.0;
        double[] t = PrayerTimes.compute(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                Prefs.lat(c), Prefs.lng(c), tz, Prefs.method(c), Prefs.asrFactor(c));
        // التعديل اليدوي لكل صلاة
        for (int i = 0; i < t.length; i++) {
            t[i] = (t[i] + Prefs.offset(c, i) / 60.0 + 24.0) % 24.0;
        }
        return t;
    }

    private static long toMillis(int dayOffset, double hours) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dayOffset);
        int minutes = (int) Math.round(hours * 60.0);
        cal.set(Calendar.HOUR_OF_DAY, minutes / 60);
        cal.set(Calendar.MINUTE, minutes % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** الصلاة الجاية: {رقم الصلاة، وقتها بالمللي ثانية}. الشروق مش محسوب. */
    public static long[] nextPrayer(Context c) {
        long now = System.currentTimeMillis() + 30000L;
        for (int day = 0; day <= 1; day++) {
            double[] t = timesFor(c, day);
            for (int i = 0; i < 6; i++) {
                if (i == PrayerTimes.SUNRISE) continue;
                long at = toMillis(day, t[i]);
                if (at > now) return new long[]{i, at};
            }
        }
        double[] t = timesFor(c, 1);
        return new long[]{PrayerTimes.FAJR, toMillis(1, t[PrayerTimes.FAJR])};
    }

    private static int flags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }

    public static boolean canExact(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
    }

    /** يجدول الأذان للصلاة الجاية، أو يلغيه لو الأذان مقفول. */
    public static void scheduleAdhan(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        long[] next = nextPrayer(c);
        Intent i = new Intent(c, AdhanReceiver.class).putExtra("prayer", (int) next[0]);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ_ADHAN, i, flags());
        am.cancel(pi);
        if (!Prefs.adhanEnabled(c)) return;
        if (canExact(c)) {
            PendingIntent show = PendingIntent.getActivity(c, REQ_SHOW,
                    new Intent(c, MainActivity.class), flags());
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(next[1], show), pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next[1], pi);
        }
    }

    /** يجدول التذكير الجاي بالصلاة على النبي، أو يلغيه لو مقفول. */
    public static void scheduleSalawat(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ_SALAWAT,
                new Intent(c, SalawatReceiver.class), flags());
        am.cancel(pi);
        int minutes = Prefs.salawatMinutes(c);
        if (minutes <= 0) return;
        long at = System.currentTimeMillis() + minutes * 60000L;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    /** يجدول تذكير الإقامة بعد صلاة معينة. */
    public static void scheduleIqama(Context c, int prayer) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent i = new Intent(c, IqamaReceiver.class).putExtra("prayer", prayer);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ_IQAMA, i, flags());
        am.cancel(pi);
        int minutes = Prefs.iqamaMinutes(c);
        if (minutes <= 0) return;
        long at = System.currentTimeMillis() + minutes * 60000L;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    /** تذكير الصيام المستحب، الساعة ٨ مساءً كل يوم. */
    public static void scheduleFasting(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent i = new Intent(c, FastingReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ_FASTING, i, flags());
        am.cancel(pi);
        if (!Prefs.fastingReminder(c)) return;
        Calendar at = Calendar.getInstance();
        at.set(Calendar.HOUR_OF_DAY, 20);
        at.set(Calendar.MINUTE, 0);
        at.set(Calendar.SECOND, 0);
        at.set(Calendar.MILLISECOND, 0);
        if (at.getTimeInMillis() <= System.currentTimeMillis()) {
            at.add(Calendar.DAY_OF_MONTH, 1);
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.getTimeInMillis(), pi);
    }

    /** أذكار الصباح بعد الشروق بنص ساعة، وأذكار المساء بعد العصر بنص ساعة. */
    public static void scheduleAdhkar(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        scheduleAdhkarOne(c, am, false, REQ_ADHKAR_AM, PrayerTimes.SUNRISE);
        scheduleAdhkarOne(c, am, true, REQ_ADHKAR_PM, PrayerTimes.ASR);
    }

    private static void scheduleAdhkarOne(Context c, AlarmManager am, boolean evening,
                                          int req, int prayer) {
        Intent i = new Intent(c, AdhkarReceiver.class)
                .putExtra(AdhkarReceiver.EXTRA_EVENING, evening);
        PendingIntent pi = PendingIntent.getBroadcast(c, req, i, flags());
        am.cancel(pi);
        if (!Prefs.adhkarReminder(c)) return;
        try {
            long at = 0;
            for (int day = 0; day <= 1; day++) {
                double[] t = timesFor(c, day);
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, day);
                long when = atHour(cal, t[prayer] + 0.5);
                if (when > System.currentTimeMillis()) {
                    at = when;
                    break;
                }
            }
            if (at > 0) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (Exception ignored) {
            // مفيش موقع محفوظ لسه
        }
    }

    /** يحوّل ساعة عشرية ليوم معيّن إلى توقيت فعلي. */
    private static long atHour(Calendar day, double hours) {
        int total = (int) Math.round(((hours % 24.0) + 24.0) % 24.0 * 60.0);
        Calendar cal = (Calendar) day.clone();
        cal.set(Calendar.HOUR_OF_DAY, total / 60);
        cal.set(Calendar.MINUTE, total % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** يجدول إرجاع الصوت بعد انتهاء مدة الصلاة. */
    public static void scheduleSilenceEnd(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent i = new Intent(c, SilenceEndReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ_SILENCE, i, flags());
        am.cancel(pi);
        int minutes = Math.max(1, Prefs.silenceMinutes(c));
        long at = System.currentTimeMillis() + minutes * 60000L;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    /** تنبيه قبل الصلاة القادمة. */
    public static void schedulePreAdhan(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent i = new Intent(c, PreAdhanReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ_PRE, i, flags());
        am.cancel(pi);
        int before = Prefs.preAdhanMinutes(c);
        if (before <= 0) return;
        try {
            long[] next = nextPrayer(c);
            long at = next[1] - before * 60000L;
            if (at <= System.currentTimeMillis()) return;
            Intent withPrayer = new Intent(c, PreAdhanReceiver.class)
                    .putExtra("prayer", (int) next[0]);
            PendingIntent pi2 = PendingIntent.getBroadcast(c, REQ_PRE, withPrayer, flags());
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi2);
        } catch (Exception ignored) {
        }
    }

    public static void scheduleAll(Context c) {
        scheduleAdhan(c);
        scheduleSalawat(c);
        scheduleFasting(c);
        scheduleAdhkar(c);
        schedulePreAdhan(c);
        Notifications.updateTimes(c);
        TimesWidget.refresh(c);
    }
}
