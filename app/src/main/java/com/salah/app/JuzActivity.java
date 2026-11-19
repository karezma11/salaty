package com.salah.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** التنقّل بالأجزاء. */
public class JuzActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("الأجزاء");
        Ui.applyNaskh(title, true);
        ((TextView) findViewById(R.id.screenSub)).setText(
                Ui.arabicDigits(Juz.count()) + " جزء · اضغط لفتح بداية الجزء");

        LinearLayout list = findViewById(R.id.list);
        LayoutInflater inflater = LayoutInflater.from(this);
        List<Quran.Sura> suras = Quran.suras(this);

        for (int j = 1; j <= Juz.count(); j++) {
            final int juz = j;
            int s = Juz.sura(j);
            int a = Juz.ayah(j);
            String name = s >= 1 && s <= suras.size() ? suras.get(s - 1).name : "";
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText(Juz.name(j));
            Ui.applyNaskh(label, true);
            ((TextView) row.findViewById(R.id.value)).setText(
                    "يبدأ من سورة " + name + " · الآية " + Ui.arabicDigits(a));
            row.setOnClickListener(v -> {
                Intent i = new Intent(this, SuraActivity.class);
                i.putExtra(SuraActivity.EXTRA_SURA, Juz.sura(juz));
                i.putExtra(SuraActivity.EXTRA_AYA, Juz.ayah(juz));
                startActivity(i);
            });
            list.addView(row);
        }
    }
}
