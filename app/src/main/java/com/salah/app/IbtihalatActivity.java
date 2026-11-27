package com.salah.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** الابتهالات والتواشيح: تشغيل من داخل التطبيق، ويكمّل والشاشة مقفولة. */
public class IbtihalatActivity extends BaseActivity implements Audio.Listener {

    private final List<TextView> values = new ArrayList<>();
    private List<Ibtihal.Track> tracks;
    private SleepStrip sleepStrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        TextView title = findViewById(R.id.screenTitle);
        title.setText("ابتهالات وتواشيح");
        Ui.applyNaskh(title, true);
        TextView sub = findViewById(R.id.screenSub);

        LinearLayout list = findViewById(R.id.list);
        sleepStrip = SleepStrip.aboveList(this);
        tracks = Ibtihal.available(this);

        if (tracks.isEmpty()) {
            sub.setText("مفيش ابتهالات متاحة في هذه النسخة");
            return;
        }
        sub.setText(Ui.arabicDigits(tracks.size()) + " ابتهال وتوشيح · بيكمّلوا ورا بعض والشاشة مقفولة");

        LayoutInflater inflater = LayoutInflater.from(this);
        // كله ورا بعض من الأول، أو بترتيب عشوائي
        View all = inflater.inflate(R.layout.item_setting, list, false);
        TextView allLabel = all.findViewById(R.id.label);
        allLabel.setText("شغّل الكل");
        Ui.applyNaskh(allLabel, true);
        ((TextView) all.findViewById(R.id.value)).setText(
                Ui.arabicDigits(tracks.size()) + " ابتهال ورا بعض");
        all.setOnClickListener(v -> playFrom(order(0, false)));
        list.addView(all);
        View shuffle = inflater.inflate(R.layout.item_setting, list, false);
        TextView shLabel = shuffle.findViewById(R.id.label);
        shLabel.setText("ترتيب عشوائي");
        Ui.applyNaskh(shLabel, true);
        ((TextView) shuffle.findViewById(R.id.value)).setText("كل مرة بترتيب مختلف");
        shuffle.setOnClickListener(v -> playFrom(order(0, true)));
        list.addView(shuffle);
        for (int i = 0; i < tracks.size(); i++) {
            final int index = i;
            Ibtihal.Track t = tracks.get(i);
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText(t.title);
            Ui.applyNaskh(label, true);
            TextView value = row.findViewById(R.id.value);
            value.setText(t.kind);
            values.add(value);
            row.setOnClickListener(v -> toggle(index));
            row.setOnLongClickListener(v -> {
                String key = "ibtihal:" + t.file;
                boolean fav = Bookmarks.hasFav(this, key);
                Bookmarks.toggleFav(this, key);
                Toast.makeText(this, fav ? "اتشال من المفضلة" : "اتضاف للمفضلة",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
            list.addView(row);
        }
        refresh();
    }

    private String idOf(int index) {
        return "ibtihal-" + tracks.get(index).file;
    }

    /** الضغط على ابتهال: هو واللي بعده لحد الآخر، وبعدين اللي قبله، فالكل بيتسمع ورا بعض. */
    private void toggle(int index) {
        if (idOf(index).equals(Audio.currentId())) {
            Audio.stop();
            return;
        }
        playFrom(order(index, false));
    }

    /** ترتيب التشغيل: من start ولفّة كاملة، أو عشوائي. */
    private java.util.List<Integer> order(int start, boolean shuffle) {
        java.util.List<Integer> o = new java.util.ArrayList<>();
        for (int i = 0; i < tracks.size(); i++) o.add((start + i) % tracks.size());
        if (shuffle) java.util.Collections.shuffle(o);
        return o;
    }

    private void playFrom(java.util.List<Integer> order) {
        java.util.List<String> assets = new java.util.ArrayList<>();
        java.util.List<String> ids = new java.util.ArrayList<>();
        java.util.List<String> titles = new java.util.ArrayList<>();
        for (int i : order) {
            assets.add(Ibtihal.asset(tracks.get(i)));
            ids.add(idOf(i));
            titles.add(tracks.get(i).title);
        }
        if (!Audio.playQueue(this, assets, ids, titles, "ابتهالات وتواشيح")) {
            Toast.makeText(this, "تعذّر تشغيل هذا المقطع", Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    private void refresh() {
        if (tracks == null) return;
        String playing = Audio.currentId();
        for (int i = 0; i < tracks.size() && i < values.size(); i++) {
            values.get(i).setText(idOf(i).equals(playing)
                    ? (Audio.isPlaying() ? "جاري التشغيل ⏸" : "متوقف مؤقتًا ▶")
                    : tracks.get(i).kind);
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
        sleepStrip.resume();
    }

    @Override
    protected void onPause() {
        // الصوت بيكمّل في الخلفية
        Audio.detach(this);
        sleepStrip.pause();
        super.onPause();
    }
}
