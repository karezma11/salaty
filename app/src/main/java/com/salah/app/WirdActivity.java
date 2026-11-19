package com.salah.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/** الورد اليومي وختم القرآن. */
public class WirdActivity extends BaseActivity {

    private LinearLayout list;
    private TextView sub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("الورد والختمة");
        Ui.applyNaskh(title, true);
        sub = findViewById(R.id.screenSub);
        list = findViewById(R.id.list);
        render();
    }

    private void row(String label, String value, View.OnClickListener click) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_setting, list, false);
        TextView l = v.findViewById(R.id.label);
        l.setText(label);
        Ui.applyNaskh(l, true);
        ((TextView) v.findViewById(R.id.value)).setText(value);
        if (click != null) v.setOnClickListener(click);
        else v.setClickable(false);
        list.addView(v);
    }

    private void render() {
        list.removeAllViews();
        sub.setText(Progress.todayText() + " · " + Hijri.today(this).text());

        int target = Progress.dailyTarget(this);
        int today = Progress.readToday(this);
        int percent = Progress.khatmaPercent(this);
        int furthest = Progress.furthest(this);

        row("ورد النهاردة", target > 0
                ? Ui.arabicDigits(today) + " من " + Ui.arabicDigits(target) + " آية"
                  + (today >= target ? "  ✓ تم" : "")
                : Ui.arabicDigits(today) + " آية", null);

        row("الهدف اليومي", target > 0 ? Ui.arabicDigits(target) + " آية" : "مش محدد",
                v -> pickTarget());

        if (target > 0) {
            row("أيام متواصلة", Ui.arabicDigits(Progress.streak(this)) + " يوم", null);
        }

        row("تقدّم الختمة", Ui.arabicDigits(percent) + "٪ · "
                + Ui.arabicDigits(furthest) + " من " + Ui.arabicDigits(Progress.TOTAL_AYAHS) + " آية", null);

        int left = Progress.daysLeft(this);
        if (left > 0) {
            row("باقي على الختمة", Ui.arabicDigits(left) + " يوم بالمعدل ده", null);
        } else if (left == 0) {
            row("باقي على الختمة", "تمت الختمة — تقبّل الله", null);
        }

        StringBuilder week = new StringBuilder();
        for (int i = 6; i >= 0; i--) {
            if (week.length() > 0) week.append("   ");
            week.append(Ui.arabicDigits(Progress.readOn(this, i)));
        }
        row("آخر ٧ أيام", week.toString(), null);

        row("صفّر الختمة", "ابدأ من أول المصحف", v -> new AlertDialog.Builder(this)
                .setTitle("تصفير الختمة")
                .setMessage("هيتمسح تقدّم الختمة وسجل الأيام. العلامات المرجعية مش هتتأثر. متأكد؟")
                .setPositiveButton("صفّر", (d, w) -> {
                    Progress.reset(this);
                    render();
                })
                .setNegativeButton("إلغاء", null)
                .show());
    }

    private void pickTarget() {
        final int[] choices = {0, 10, 20, 50, 100, 150, 208, 300};
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            if (choices[i] == 0) {
                labels[i] = "بدون هدف";
            } else {
                int days = (Progress.TOTAL_AYAHS + choices[i] - 1) / choices[i];
                labels[i] = Ui.arabicDigits(choices[i]) + " آية يوميًا · ختمة في "
                        + Ui.arabicDigits(days) + " يوم";
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("الهدف اليومي")
                .setItems(labels, (d, w) -> {
                    Progress.setDailyTarget(this, choices[w]);
                    render();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }
}
