package com.salah.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** البحث في المصحف. */
public class QuranSearchActivity extends BaseActivity {

    private EditText input;
    private LinearLayout results;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        input = findViewById(R.id.input);
        results = findViewById(R.id.results);
        status = findViewById(R.id.status);

        findViewById(R.id.go).setOnClickListener(v -> run());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                run();
                return true;
            }
            return false;
        });
    }

    private void run() {
        String q = input.getText().toString().trim();
        results.removeAllViews();
        if (q.length() < 2) {
            status.setText("اكتب كلمتين على الأقل للبحث.");
            return;
        }
        List<Quran.Hit> hits = Quran.search(this, q, 200);
        if (hits.isEmpty()) {
            status.setText("مفيش نتائج لـ \"" + q + "\".");
            return;
        }
        status.setText("عدد النتائج: " + Ui.arabicDigits(hits.size()));

        List<Quran.Sura> suras = Quran.suras(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Quran.Hit h : hits) {
            View row = inflater.inflate(R.layout.item_result, results, false);
            String name = h.sura >= 1 && h.sura <= suras.size() ? suras.get(h.sura - 1).name : "";
            TextView where = row.findViewById(R.id.where);
            where.setText("سورة " + name + " · الآية " + Ui.arabicDigits(h.aya));
            Ui.applyNaskh(where, true);
            TextView aya = row.findViewById(R.id.aya);
            aya.setText(h.text);
            Ui.applyQuranFont(aya);
            row.setOnClickListener(v -> {
                Intent i = new Intent(this, SuraActivity.class);
                i.putExtra("sura", h.sura);
                i.putExtra("aya", h.aya);
                startActivity(i);
            });
            results.addView(row);
        }
    }
}
