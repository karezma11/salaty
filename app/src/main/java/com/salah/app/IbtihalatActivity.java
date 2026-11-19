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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        TextView title = findViewById(R.id.screenTitle);
        title.setText("ابتهالات وتواشيح");
        Ui.applyNaskh(title, true);
        TextView sub = findViewById(R.id.screenSub);

        LinearLayout list = findViewById(R.id.list);
        tracks = Ibtihal.available(this);

        if (tracks.isEmpty()) {
            sub.setText("مفيش ابتهالات متاحة في هذه النسخة");
            return;
        }
        sub.setText(Ui.arabicDigits(tracks.size()) + " ابتهال وتوشيح · بيكمّل والشاشة مقفولة");

        LayoutInflater inflater = LayoutInflater.from(this);
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

    private void toggle(int index) {
        if (idOf(index).equals(Audio.currentId())) {
            Audio.stop();
            return;
        }
        Ibtihal.Track t = tracks.get(index);
        boolean ok = Audio.playAsset(this, Ibtihal.asset(t), idOf(index), t.title, t.kind);
        if (!ok) Toast.makeText(this, "تعذّر تشغيل هذا المقطع", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onPause() {
        // الصوت بيكمّل في الخلفية
        Audio.detach(this);
        super.onPause();
    }
}
