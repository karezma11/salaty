package com.salah.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.util.List;

/** إدارة تلاوة السور المحمّلة على الجهاز. */
public class DownloadsActivity extends BaseActivity {

    private LinearLayout list;
    private TextView sub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("إدارة التحميلات");
        Ui.applyNaskh(title, true);
        sub = findViewById(R.id.screenSub);
        list = findViewById(R.id.list);
        render();
    }

    /** حجم مجلد سورة بالميجا. */
    private long sizeOf(int sura) {
        File dir = new File(Recitation.folderOf(this, Prefs.reciter(this)),
                String.format(java.util.Locale.US, "%03d", sura));
        File[] files = dir.listFiles();
        if (files == null) return 0;
        long total = 0;
        for (File f : files) total += f.length();
        return total;
    }

    private String mb(long bytes) {
        if (bytes <= 0) return "";
        double m = bytes / (1024.0 * 1024.0);
        return m < 1
                ? Ui.arabicDigits(Math.round(bytes / 1024.0)) + " ك.ب"
                : Ui.arabicDigits(Math.round(m)) + " م.ب";
    }

    private void render() {
        list.removeAllViews();
        List<Quran.Sura> suras = Quran.suras(this);
        LayoutInflater inflater = LayoutInflater.from(this);

        int complete = 0, partial = 0;
        long total = 0;
        for (Quran.Sura s : suras) {
            long bytes = sizeOf(s.index);
            if (bytes <= 0) continue;
            total += bytes;
            int have = Downloads.haveCount(this, s.index, s.ayas);
            if (have >= s.ayas) complete++;
            else partial++;
        }

        String who = Reciters.current(this).name;
        if (total == 0) {
            sub.setText("مفيش تلاوة محمّلة لسه بصوت " + who);
        } else {
            sub.setText(who + " · محمّل " + Ui.arabicDigits(complete) + " سورة كاملة"
                    + (partial > 0 ? " و " + Ui.arabicDigits(partial) + " ناقصة" : "")
                    + " · " + mb(total));
        }

        final long totalBytes = total;
        if (totalBytes > 0) {
            View all = inflater.inflate(R.layout.item_setting, list, false);
            TextView l = all.findViewById(R.id.label);
            l.setText("مسح كل التلاوة المحمّلة");
            Ui.applyNaskh(l, true);
            ((TextView) all.findViewById(R.id.value)).setText("هيفضي " + mb(totalBytes));
            all.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("مسح كل التلاوة")
                    .setMessage("هتتمسح كل ملفات التلاوة من الموبايل (" + mb(totalBytes)
                            + "). تقدر تحمّلها تاني في أي وقت. متأكد؟")
                    .setPositiveButton("امسح الكل", (d, w) -> {
                        for (Quran.Sura s : suras) Downloads.delete(this, s.index, s.ayas);
                        render();
                    })
                    .setNegativeButton("إلغاء", null)
                    .show());
            list.addView(all);
        }

        for (Quran.Sura s : suras) {
            final long bytes = sizeOf(s.index);
            if (bytes <= 0) continue;
            final int have = Downloads.haveCount(this, s.index, s.ayas);
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText("سورة " + s.name);
            Ui.applyNaskh(label, true);
            TextView value = row.findViewById(R.id.value);
            value.setText(have >= s.ayas
                    ? "كاملة · " + mb(bytes)
                    : "ناقصة: " + Ui.arabicDigits(have) + " من " + Ui.arabicDigits(s.ayas)
                      + " · " + mb(bytes));
            row.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("سورة " + s.name)
                    .setMessage(have >= s.ayas
                            ? "التلاوة كاملة (" + mb(bytes) + ")."
                            : "التلاوة ناقصة: " + Ui.arabicDigits(have) + " من "
                              + Ui.arabicDigits(s.ayas) + " آية.")
                    .setPositiveButton("امسح", (d, w) -> {
                        Downloads.delete(this, s.index, s.ayas);
                        render();
                    })
                    .setNeutralButton(have >= s.ayas ? null : "أكمل التحميل",
                            have >= s.ayas ? null : (d, w) -> {
                                DownloadService.start(this, s.index);
                            })
                    .setNegativeButton("إلغاء", null)
                    .show());
            list.addView(row);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }
}
