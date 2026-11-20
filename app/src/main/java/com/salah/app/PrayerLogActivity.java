package com.salah.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** سجل الصلوات اليومي. */
public class PrayerLogActivity extends BaseActivity {

    private LinearLayout list;
    private TextView sub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("سجل الصلوات");
        Ui.applyNaskh(title, true);
        sub = findViewById(R.id.screenSub);
        list = findViewById(R.id.list);
        render();
    }

    private void render() {
        list.removeAllViews();
        sub.setText("اضغط على الصلاة لتسجيلها: منفردًا ← جماعة ← إلغاء");

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int p : PrayerLog.FARD) {
            final int prayer = p;
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText(PrayerTimes.NAMES[prayer]);
            Ui.applyNaskh(label, true);
            TextView value = row.findViewById(R.id.value);
            value.setText(PrayerLog.label(PrayerLog.state(this, 0, prayer)));
            row.setOnClickListener(v -> {
                int st = PrayerLog.cycle(this, 0, prayer);
                value.setText(PrayerLog.label(st));
                updateStats();
                // علامات ✓ وزراير «صلّيت» في الإشعارات تفضل زي السجل
                Notifications.updateTimes(this);
                AdhanService.refreshIfRunning();
            });
            list.addView(row);
        }

        statsView = inflater.inflate(R.layout.item_setting, list, false);
        TextView l = statsView.findViewById(R.id.label);
        l.setText("الإحصائية");
        Ui.applyNaskh(l, true);
        statsView.setClickable(false);
        list.addView(statsView);
        updateStats();

        // آخر سبع أيام
        for (int d = 1; d <= 7; d++) {
            int done = 0, jamaa = 0;
            for (int p : PrayerLog.FARD) {
                int st = PrayerLog.state(this, d, p);
                if (st != PrayerLog.NOT_PRAYED) done++;
                if (st == PrayerLog.JAMAA) jamaa++;
            }
            if (done == 0) continue;
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText("من " + Ui.arabicDigits(d) + " يوم");
            Ui.applyNaskh(label, true);
            ((TextView) row.findViewById(R.id.value)).setText(
                    Ui.arabicDigits(done) + " من ٥ · " + Ui.arabicDigits(jamaa) + " جماعة");
            row.setClickable(false);
            list.addView(row);
        }
    }

    private View statsView;

    private void updateStats() {
        if (statsView == null) return;
        int month = PrayerLog.countIn(this, 30, false);
        int jamaa = PrayerLog.countIn(this, 30, true);
        ((TextView) statsView.findViewById(R.id.value)).setText(
                "آخر ٣٠ يوم: " + Ui.arabicDigits(month) + " صلاة · "
                        + Ui.arabicDigits(jamaa) + " في جماعة");
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }
}
