package com.salah.app;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * مؤقت النوم المشترك للتلاوة وكل أصوات التطبيق (الإذاعة والابتهالات والأذكار).
 * الوقت بيتحسب بـ elapsedRealtime، فنوم الجهاز مابيأخّروش. الإذاعة والأصوات بتوطى بالتدريج
 * في آخر ٣٠ ثانية وبعدين تقف، والتلاوة بتكمّل الآية اللي شغالة وبتوطى في آخرها وبعدين تقف.
 */
public final class SleepTimer {

    /** الخدمات بتسمع عشان تحدّث سطر الإشعار كل دقيقة ولما المؤقت يتغيّر. */
    public interface Listener {
        void onSleepChanged();
    }

    /** المدد اللي زرار الهلال في الإشعار بيلف عليها (بالدقايق). */
    static final int[] STEPS = {15, 30, 60};
    /** نتيجة nextStep: يقفل المؤقت، أو «آخر السورة». */
    static final int OFF = 0;
    static final int END_OF_SURA = -1;

    private static final long FADE_MS = 30000L;

    /** وقت التوقف بـ elapsedRealtime، أو 0. */
    private static long deadline;
    private static boolean endOfSura;
    /** الوقت خلص والتلاوة بتكمّل آخر آية. */
    private static boolean expired;
    /** الوقت الباقي في الآية لحظة ما الوقت خلص، عشان التوطية تبقى ناعمة لو الآية قصيرة. */
    private static long fadeSpan;
    private static long lastMinute = -1;

    private static Handler main;
    private static final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<>();

    private SleepTimer() {
    }

    private static Handler handler() {
        if (main == null) main = new Handler(Looper.getMainLooper());
        return main;
    }

    private static long now() {
        return SystemClock.elapsedRealtime();
    }

    public static void addListener(Listener l) {
        listeners.add(l);
    }

    public static void removeListener(Listener l) {
        listeners.remove(l);
    }

    private static void fire() {
        for (Listener l : listeners) l.onSleepChanged();
    }

    public static boolean isActive() {
        return deadline != 0 || endOfSura;
    }

    public static boolean isEndOfSura() {
        return endOfSura;
    }

    /** الوقت خلص: التلاوة لازم تقف بعد الآية اللي شغالة. */
    public static boolean expired() {
        return expired;
    }

    public static long remainingMs() {
        return deadline == 0 ? 0 : Math.max(0, deadline - now());
    }

    /** يشغّل المؤقت بعدد دقايق (0 يقفله). */
    public static void set(int minutes) {
        clear();
        if (minutes > 0) {
            deadline = now() + minutes * 60000L;
            handler().post(tick);
        }
        fire();
    }

    /** التلاوة تقف لما السورة الشغالة تخلص. */
    public static void setEndOfSura() {
        clear();
        endOfSura = true;
        fire();
    }

    /** «+١٥ د»: يزوّد على الوقت الباقي، أو يبدأ من دلوقتي لو الوقت خلص أو كان «آخر السورة». */
    public static void add(int minutes) {
        if (deadline == 0 || expired) {
            set(minutes);
            return;
        }
        deadline += minutes * 60000L;
        fullVolume();
        fire();
    }

    public static void cancel() {
        clear();
        fire();
    }

    /** زرار الهلال: كل ضغطة مدة أطول، وبعد الساعة «آخر السورة» (للتلاوة) وبعدين يقفل. */
    public static void cycle(boolean recitation) {
        long mins = (remainingMs() + 59999L) / 60000L;
        int step = nextStep(isActive(), mins, endOfSura, recitation);
        if (step == END_OF_SURA) {
            setEndOfSura();
        } else {
            set(step);
        }
    }

    /** منطق زرار الهلال من غير أندرويد، عشان الاختبارات. */
    static int nextStep(boolean active, long remainingMin, boolean endOfSura, boolean recitation) {
        if (endOfSura) return OFF;
        if (!active) return STEPS[0];
        for (int s : STEPS) {
            if (s > remainingMin) return s;
        }
        return recitation ? END_OF_SURA : OFF;
    }

    /** نص قصير لسطر الإشعار، أو "" لو المؤقت مقفول. */
    public static String shortLabel() {
        if (endOfSura) return "يقف آخر السورة";
        if (deadline == 0) return "";
        if (expired) return "يقف بعد الآية دي";
        long mins = (remainingMs() + 59999L) / 60000L;
        return "يقف بعد " + Ui.arabicDigits(mins) + " د";
    }

    /** لما صوت يقف: لو مفيش حاجة تانية شغالة، المؤقت مالوش لازمة. */
    static void playbackEnded() {
        if (!isActive()) return;
        if (RecitationPlayer.isActive() || Audio.isActive()) return;
        cancel();
    }

    private static void clear() {
        boolean hadTime = deadline != 0;
        deadline = 0;
        endOfSura = false;
        expired = false;
        fadeSpan = 0;
        lastMinute = -1;
        if (main != null) main.removeCallbacks(tick);
        if (hadTime) fullVolume();
    }

    private static void fullVolume() {
        Audio.setVolume(1f);
        RecitationPlayer.setVolume(1f);
    }

    private static final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (deadline == 0) return;
            long left = deadline - now();

            if (!expired && left <= 0) {
                expire();
                if (deadline == 0) return;
            }

            if (expired) {
                // التلاوة: بتوطى في آخر الآية اللي شغالة، وRecitationPlayer بيقف لما تخلص
                long rem = RecitationPlayer.ayahRemainingMs();
                if (rem > 0 && fadeSpan > 0) {
                    RecitationPlayer.setVolume(Math.min(1f, rem / (float) fadeSpan));
                }
            } else if (left < FADE_MS) {
                Audio.setVolume(Math.max(0f, left / (float) FADE_MS));
            }

            long minute = (Math.max(0, left) + 59999L) / 60000L;
            if (minute != lastMinute) {
                lastMinute = minute;
                fire();
            }
            handler().postDelayed(this, 1000);
        }
    };

    /** الوقت خلص: الأصوات تقف، والتلاوة تكمّل الآية الشغالة لو بتتقري. */
    private static void expire() {
        if (Audio.isActive()) Audio.stop();
        if (RecitationPlayer.isPlaying()) {
            expired = true;
            long rem = RecitationPlayer.ayahRemainingMs();
            fadeSpan = Math.max(1000L, Math.min(FADE_MS, rem));
            fire();
            return;
        }
        if (RecitationPlayer.isActive()) RecitationPlayer.stop();
        // stop() بيقفل المؤقت عن طريق playbackEnded، ولو مفيش حاجة شغالة نقفله هنا
        if (deadline != 0) cancel();
    }
}
