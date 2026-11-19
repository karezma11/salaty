package com.salah.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

/** قائمة السور. */
public class QuranActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quran);
        setupBottomNav(R.id.navQuran);

        findViewById(R.id.search).setOnClickListener(
                v -> startActivity(new Intent(this, QuranSearchActivity.class)));
        findViewById(R.id.searchTextBtn).setOnClickListener(
                v -> startActivity(new Intent(this, QuranSearchActivity.class)));
        findViewById(R.id.searchTextBtn).setOnLongClickListener(v -> {
            startActivity(new Intent(this, BookmarksActivity.class));
            return true;
        });

        List<Quran.Sura> suras = Quran.suras(this);
        TextView audioStatus = findViewById(R.id.audioStatus);
        int have = Recitation.count(this);
        audioStatus.setText(have >= 114
                ? "المصحف كامل بصوت " + Reciters.current(this).name + " — جاهز بدون نت"
                : "محمّل " + Ui.arabicDigits(have) + " من ١١٤ سورة · اضغط هنا لتحميل الباقي");
        audioStatus.setOnClickListener(v -> downloadAll(have));

        findViewById(R.id.search).setOnLongClickListener(v -> {
            startActivity(new Intent(this, JuzActivity.class));
            return true;
        });

        View resumeCard = findViewById(R.id.resumeCard);
        int last = Prefs.lastSura(this);
        if (last >= 1 && last <= suras.size()) {
            Quran.Sura s = suras.get(last - 1);
            TextView t = findViewById(R.id.resumeText);
            int at = Prefs.lastAyah(this, last);
            t.setText(at > 1
                    ? "متابعة القراءة: سورة " + s.name + " · الآية " + Ui.arabicDigits(at)
                    : "متابعة القراءة: سورة " + s.name);
            Ui.applyNaskh(t, true);
            resumeCard.setVisibility(View.VISIBLE);
            resumeCard.setOnClickListener(v -> open(last));
        } else {
            resumeCard.setVisibility(View.GONE);
        }

        LinearLayout list = findViewById(R.id.list);
        // بناء الـ114 صف مرة واحدة كان بياخد وقت محسوس، فبنعرض أول شاشة
        // فورًا وبنكمّل الباقي على دفعات بين إطارات الرسم
        buildRows(list, suras, 0);
    }

    private void buildRows(LinearLayout list, List<Quran.Sura> suras, int from) {
        LayoutInflater inflater = LayoutInflater.from(this);
        int end = Math.min(suras.size(), from + (from == 0 ? 12 : 24));
        for (int i = from; i < end; i++) {
            Quran.Sura s = suras.get(i);
            View row = inflater.inflate(R.layout.item_sura, list, false);
            ((TextView) row.findViewById(R.id.num)).setText(Ui.arabicDigits(s.index));
            TextView name = row.findViewById(R.id.name);
            name.setText("سورة " + s.name);
            Ui.applyNaskh(name, true);
            ((TextView) row.findViewById(R.id.meta)).setText(
                    s.type + " · "
                            + Ui.arabicDigits(s.ayas) + " آية");
            row.findViewById(R.id.hasAudio).setVisibility(
                    Recitation.available(this, s.index) ? View.VISIBLE : View.GONE);
            row.setOnClickListener(v -> open(s.index));
            list.addView(row);
        }
        if (end < suras.size()) {
            final int next = end;
            list.post(() -> buildRows(list, suras, next));
        }
    }

    /** تحميل المصحف كامل مرة واحدة. */
    private void downloadAll(int have) {
        if (have >= 114) {
            Toast.makeText(this, "المصحف كامل متاح بالفعل", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Downloads.online(this)) {
            Toast.makeText(this, "محتاج اتصال بالإنترنت للتحميل", Toast.LENGTH_LONG).show();
            return;
        }
        String net = Downloads.onWifi(this)
                ? "أنت على واي فاي."
                : "تنبيه: أنت على بيانات الموبايل، والحجم كبير.";
        new AlertDialog.Builder(this)
                .setTitle("تحميل المصحف كامل")
                .setMessage("الحجم حوالي ٨٦٠ ميجا، والتحميل بياخد وقت.\n" + net
                        + "\n\nالتحميل بيكمّل في الخلفية، وتقدر توقفه من الإشعار في أي وقت.")
                .setPositiveButton("ابدأ التحميل", (d, w) -> {
                    DownloadService.startAll(this);
                    Toast.makeText(this, "بدأ التحميل — تابعه من الإشعارات", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void open(int sura) {
        Intent i = new Intent(this, SuraActivity.class);
        i.putExtra("sura", sura);
        startActivity(i);
    }
}
