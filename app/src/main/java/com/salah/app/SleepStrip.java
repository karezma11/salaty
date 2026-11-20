package com.salah.app;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/**
 * شريط مؤقت النوم (الشكل ج اللي اختاره المستخدم): شريط دهبي فيه العدّ بالثانية،
 * و«+١٥ د» لو لسه صاحي، و«إلغاء». بيظهر بس والمؤقت شغّال.
 */
final class SleepStrip {

    private final View view;
    private final TextView text;
    private final TextView time;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            handler.postDelayed(this, 1000);
        }
    };
    /** حاجة الشاشة عايزة تعملها لما المؤقت يتغيّر من الشريط. */
    private Runnable onChange;

    private SleepStrip(View v) {
        view = v;
        text = v.findViewById(R.id.sleepText);
        time = v.findViewById(R.id.sleepTime);
        v.findViewById(R.id.sleepPlus).setOnClickListener(x -> {
            SleepTimer.add(15);
            changed();
        });
        v.findViewById(R.id.sleepCancel).setOnClickListener(x -> {
            SleepTimer.cancel();
            Toast.makeText(v.getContext(), "اتلغى مؤقت النوم", Toast.LENGTH_SHORT).show();
            changed();
        });
    }

    /** يحط الشريط في parent في المكان index. */
    static SleepStrip insert(Activity a, ViewGroup parent, int index) {
        View v = LayoutInflater.from(a).inflate(R.layout.view_sleep_strip, parent, false);
        parent.addView(v, index);
        return new SleepStrip(v);
    }

    /** في الشاشات اللي بتستخدم activity_simple_list: الشريط تحت العنوان وفوق القايمة. */
    static SleepStrip aboveList(Activity a) {
        View scroll = (View) a.findViewById(R.id.list).getParent();
        ViewGroup root = (ViewGroup) scroll.getParent();
        SleepStrip s = insert(a, root, root.indexOfChild(scroll));
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) s.view.getLayoutParams();
        lp.topMargin = Math.round(12 * a.getResources().getDisplayMetrics().density);
        s.view.setLayoutParams(lp);
        return s;
    }

    void setOnChange(Runnable r) {
        onChange = r;
    }

    private void changed() {
        update();
        if (onChange != null) onChange.run();
    }

    void resume() {
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    void pause() {
        handler.removeCallbacks(tick);
    }

    private void update() {
        if (!SleepTimer.isActive()) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        boolean recitation = RecitationPlayer.isActive();
        String what = recitation ? "التلاوة هتقف"
                : Audio.isLive() ? "الإذاعة هتقف" : "الصوت هيقف";
        if (SleepTimer.isEndOfSura()) {
            text.setText(what + " آخر السورة");
            time.setText("");
        } else if (SleepTimer.expired()) {
            text.setText(what + " بعد الآية دي");
            time.setText("");
        } else {
            text.setText(what + " بعد");
            time.setText(clock(SleepTimer.remainingMs()));
        }
    }

    /** ٢٣:١٤ أو ١:٠٥:٠٠ بأرقام عربي. */
    static String clock(long ms) {
        long s = (ms + 999) / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        String out = h > 0
                ? h + ":" + two(m) + ":" + two(sec)
                : m + ":" + two(sec);
        StringBuilder sb = new StringBuilder();
        for (char ch : out.toCharArray()) {
            sb.append(ch >= '0' && ch <= '9' ? (char) ('٠' + (ch - '0')) : ch);
        }
        return sb.toString();
    }

    private static String two(long n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    /** قايمة اختيار المدة. «آخر السورة» للتلاوة بس. */
    static void pick(Activity a, boolean recitation, Runnable after) {
        final int[] mins = recitation
                ? new int[]{15, 30, 45, 60, SleepTimer.END_OF_SURA, SleepTimer.OFF}
                : new int[]{15, 30, 45, 60, SleepTimer.OFF};
        String[] labels = new String[mins.length];
        for (int i = 0; i < mins.length; i++) {
            int m = mins[i];
            labels[i] = m == SleepTimer.END_OF_SURA ? "آخر السورة"
                    : m == SleepTimer.OFF ? "من غير مؤقت"
                    : m == 60 ? "ساعة" : Ui.arabicDigits(m) + " دقيقة";
        }
        new AlertDialog.Builder(a)
                .setTitle("مؤقت النوم")
                .setItems(labels, (d, w) -> {
                    int m = mins[w];
                    String msg;
                    if (m == SleepTimer.END_OF_SURA) {
                        SleepTimer.setEndOfSura();
                        msg = "التلاوة هتقف آخر السورة";
                    } else if (m == SleepTimer.OFF) {
                        SleepTimer.cancel();
                        msg = "اتلغى مؤقت النوم";
                    } else {
                        SleepTimer.set(m);
                        msg = "هيقف بعد " + labels[w];
                    }
                    Toast.makeText(a, msg, Toast.LENGTH_SHORT).show();
                    if (after != null) after.run();
                })
                .show();
    }
}
