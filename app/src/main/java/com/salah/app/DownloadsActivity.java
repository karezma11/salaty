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

    // قسم القراء: كام سورة نزلت لكل قارئ، والحالة لايف للي بيتحمّل
    private final java.util.Map<String, TextView> reciterValues = new java.util.HashMap<>();
    private final java.util.Map<String, long[]> reciterStatus = new java.util.HashMap<>();
    private final android.os.Handler ticker = new android.os.Handler(android.os.Looper.getMainLooper());
    private String lastBusy;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            String busy = DownloadService.busyReciter();
            // التحميل خلص أو اتنقل لقارئ تاني: نعدّ تاني
            if (lastBusy != null && !lastBusy.equals(busy)) countReciters();
            lastBusy = busy;
            showReciterValues();
            ticker.postDelayed(this, 1000);
        }
    };

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

        // المصحف كامل لكل القراء بضغطة واحدة
        View everyone = inflater.inflate(R.layout.item_setting, list, false);
        TextView el = everyone.findViewById(R.id.label);
        el.setText("حمّل المصحف كامل لكل القراء");
        Ui.applyNaskh(el, true);
        ((TextView) everyone.findViewById(R.id.value)).setText(
                Ui.arabicDigits(Reciters.ALL.length) + " قراء · ١١٤ سورة لكل قارئ · بيكمّل من مكانه");
        everyone.setOnClickListener(v -> Downloads.askEveryone(this));
        list.addView(everyone);

        TextView head = new TextView(this);
        head.setText("القراء · اضغط على أي قارئ لتحميل المصحف كامل بصوته");
        head.setTextSize(13);
        head.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ink_soft));
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        head.setPadding(pad, pad / 2, pad, pad / 4);
        list.addView(head);
        reciterValues.clear();
        for (Reciters.Reciter r : Reciters.ALL) {
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView rl = row.findViewById(R.id.label);
            rl.setText(r.name + (r.id.equals(Prefs.reciter(this)) ? "  ✓" : ""));
            Ui.applyNaskh(rl, true);
            TextView rv = row.findViewById(R.id.value);
            rv.setText(r.quality + " · بيعدّ…");
            reciterValues.put(r.id, rv);
            row.setOnClickListener(v -> onReciter(r));
            list.addView(row);
        }
        countReciters();

        TextView head2 = new TextView(this);
        head2.setText("سور " + Reciters.current(this).name);
        head2.setTextSize(13);
        head2.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ink_soft));
        head2.setPadding(pad, pad / 2, pad, pad / 4);
        list.addView(head2);

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

    /** يعدّ السور لكل قارئ في خيط جانبي (آلاف الملفات). */
    private void countReciters() {
        android.content.Context app = getApplicationContext();
        new Thread(() -> {
            java.util.Map<String, long[]> m = new java.util.HashMap<>();
            for (Reciters.Reciter r : Reciters.ALL) m.put(r.id, Downloads.reciterStatus(app, r.id));
            runOnUiThread(() -> {
                reciterStatus.clear();
                reciterStatus.putAll(m);
                showReciterValues();
            });
        }).start();
    }

    private void showReciterValues() {
        String busy = DownloadService.busyReciter();
        for (Reciters.Reciter r : Reciters.ALL) {
            TextView v = reciterValues.get(r.id);
            if (v == null) continue;
            long[] st = reciterStatus.get(r.id);
            String count = st == null ? "بيعدّ…"
                    : st[0] >= 114 ? "المصحف كامل ✓ · " + mb(st[2])
                    : Ui.arabicDigits(st[0]) + " من ١١٤ سورة"
                      + (st[1] > 0 ? " (و" + Ui.arabicDigits(st[1]) + " ناقصة)" : "")
                      + (st[2] > 0 ? " · " + mb(st[2]) : "");
            if (r.id.equals(busy)) {
                v.setText("⬇ بيتحمّل الآن: " + DownloadService.busyText() + "\n" + count);
            } else {
                v.setText(r.quality + " · " + count);
            }
        }
    }

    private void onReciter(Reciters.Reciter r) {
        String busy = DownloadService.busyReciter();
        long[] st = reciterStatus.get(r.id);
        if (r.id.equals(busy)) {
            new AlertDialog.Builder(this)
                    .setTitle(r.name)
                    .setMessage("بيتحمّل الآن: " + DownloadService.busyText()
                            + "\n\nلو وقّفته، تدوس تاني ويكمّل من مكانه.")
                    .setPositiveButton("وقّف التحميل", (d, w) -> DownloadService.cancel(this))
                    .setNegativeButton("سيبه", null)
                    .show();
            return;
        }
        boolean complete = st != null && st[0] >= 114;
        long have = st == null ? 0 : st[2];
        AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle(r.name + " · " + r.quality);
        if (complete) {
            b.setMessage("المصحف كامل بصوت " + r.name + " على الموبايل (" + mb(have) + ").");
        } else {
            b.setMessage("محمّل " + (st == null ? "…" : Ui.arabicDigits(st[0])) + " من ١١٤ سورة"
                    + (have > 0 ? " (" + mb(have) + ")" : "") + ".");
            b.setPositiveButton("حمّل المصحف كامل", (d, w) -> Downloads.askReciter(this, r, have));
        }
        if (have > 0) {
            b.setNeutralButton("امسح", (d, w) -> new AlertDialog.Builder(this)
                    .setTitle("مسح " + r.name)
                    .setMessage("هتتمسح كل تلاوة " + r.name + " (" + mb(have)
                            + "). تقدر تحمّلها تاني في أي وقت.")
                    .setPositiveButton("امسح", (d2, w2) -> {
                        Downloads.deleteReciter(this, r.id);
                        render();
                    })
                    .setNegativeButton("إلغاء", null)
                    .show());
        }
        if (!r.id.equals(Prefs.reciter(this))) {
            b.setNegativeButton("خليه القارئ", (d, w) -> {
                Prefs.setReciter(this, r.id);
                Recitation.invalidate();
                render();
            });
        }
        b.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        ticker.removeCallbacks(tick);
        ticker.post(tick);
    }

    @Override
    protected void onPause() {
        ticker.removeCallbacks(tick);
        super.onPause();
    }
}
