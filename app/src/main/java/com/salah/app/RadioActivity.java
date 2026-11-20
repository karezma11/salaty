package com.salah.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** إذاعات القرآن: القرآن الكريم وكل القرّاء. */
public class RadioActivity extends BaseActivity implements Audio.Listener {

    private LinearLayout list;
    private TextView sub;
    private final Map<String, TextView> badges = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("إذاعات القرآن");
        Ui.applyNaskh(title, true);
        sub = findViewById(R.id.screenSub);
        list = findViewById(R.id.list);
        render(Radio.all(this), null);
    }

    private void render(List<Radio.Station> stations, String query) {
        list.removeAllViews();
        badges.clear();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (stations.isEmpty()) {
            sub.setText(query == null
                    ? "قائمة الإذاعات مش متاحة في هذه النسخة"
                    : "مفيش إذاعة بالاسم ده");
        } else {
            sub.setText(query == null
                    ? Ui.arabicDigits(stations.size()) + " إذاعة · بث مباشر، محتاج إنترنت"
                    : Ui.arabicDigits(stations.size()) + " نتيجة");
        }

        View search = inflater.inflate(R.layout.item_setting, list, false);
        TextView sl = search.findViewById(R.id.label);
        sl.setText(query == null ? "ابحث عن قارئ" : "بحث: " + query);
        Ui.applyNaskh(sl, true);
        ((TextView) search.findViewById(R.id.value)).setText(
                query == null ? "اكتب اسم القارئ" : "اضغط لبحث جديد أو عرض الكل");
        search.setOnClickListener(v -> askSearch());
        list.addView(search);

        for (Radio.Station s : stations) {
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText(s.name);
            Ui.applyNaskh(label, true);
            TextView value = row.findViewById(R.id.value);
            value.setText("بث مباشر");
            badges.put(id(s), value);
            row.setOnClickListener(v -> toggle(s));
            list.addView(row);
        }
        refresh();
    }

    private String id(Radio.Station s) {
        return "radio-" + s.id;
    }

    private void askSearch() {
        final EditText input = new EditText(this);
        input.setHint("اسم القارئ");
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);
        new AlertDialog.Builder(this)
                .setTitle("بحث في الإذاعات")
                .setView(box)
                .setPositiveButton("ابحث", (d, w) -> {
                    String q = input.getText().toString().trim();
                    render(Radio.search(this, q), q.isEmpty() ? null : q);
                })
                .setNeutralButton("اعرض الكل", (d, w) -> render(Radio.all(this), null))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void toggle(Radio.Station s) {
        if (id(s).equals(Audio.currentId())) {
            Audio.stop();
            return;
        }
        if (!Downloads.online(this)) {
            Toast.makeText(this, "البث المباشر محتاج إنترنت", Toast.LENGTH_LONG).show();
            return;
        }
        Audio.playUrl(this, s.url, id(s), s.name, "بث مباشر");
        Toast.makeText(this, "جاري الاتصال بالبث...", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void refresh() {
        if (Audio.consumeFailure()) {
            Toast.makeText(this, "الإذاعة دي مش متاحة دلوقتي — جرّب واحدة تانية",
                    Toast.LENGTH_LONG).show();
        }
        String playing = Audio.currentId();
        for (Map.Entry<String, TextView> e : badges.entrySet()) {
            e.getValue().setText(e.getKey().equals(playing)
                    ? (Audio.isReconnecting() ? "انقطع — بيعيد الاتصال…"
                    : Audio.isPlaying() ? "شغّال الآن ⏸" : "جاري الاتصال...") : "بث مباشر");
        }
    }

    @Override
    public void onAudioChanged() {
        runOnUiThread(this::refresh);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Audio.attach(this);
        refresh();
    }

    @Override
    protected void onPause() {
        Audio.detach(this);
        super.onPause();
    }
}
