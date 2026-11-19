package com.salah.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

/** الأذكار: أقسام ← أبواب ← أذكار بالنص والصوت. */
public class AdhkarActivity extends BaseActivity implements Audio.Listener {

    private LinearLayout list;
    private TextView title, sub;

    /** 0 = الأقسام، 1 = الأبواب، 2 = الأذكار. */
    private int level = 0;
    private int categoryId = 0;
    private String categoryName = "";
    private int chapterId = 0;

    private final java.util.Map<String, TextView> badges = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adhkar);
        setupBottomNav(R.id.navAdhkar);
        list = findViewById(R.id.list);
        title = findViewById(R.id.screenTitle);
        sub = findViewById(R.id.screenSub);
        Ui.applyNaskh(title, true);
        showCategories();
    }

    // ---------- المستوى الأول ----------

    private void showCategories() {
        level = 0;
        title.setText("الأذكار");
        List<Dhikr.Category> cats = Dhikr.categories(this);
        sub.setText(Ui.arabicDigits(cats.size()) + " قسم · اختر القسم");
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        badges.clear();
        for (Dhikr.Category cat : cats) {
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText(cat.name);
            Ui.applyNaskh(label, true);
            ((TextView) row.findViewById(R.id.value)).setText(
                    Ui.arabicDigits(Dhikr.chapters(this, cat.id).size()) + " باب · "
                            + Ui.arabicDigits(Dhikr.countIn(this, cat.id)) + " ذكر");
            row.setOnClickListener(v -> showChapters(cat.id, cat.name));
            list.addView(row);
        }
    }

    /** بحث في كل الأذكار. */
    private void askSearch() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("مثال: الاستخارة، السفر، النوم");
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);
        new AlertDialog.Builder(this)
                .setTitle("بحث في الأذكار")
                .setView(box)
                .setPositiveButton("ابحث", (d, w) ->
                        showResults(input.getText().toString().trim()))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showResults(String query) {
        java.util.List<Dhikr.Item> hits = Dhikr.search(this, query, 80);
        if (hits.isEmpty()) {
            Toast.makeText(this, "مفيش نتائج", Toast.LENGTH_SHORT).show();
            return;
        }
        level = 2;
        title.setText("نتائج البحث");
        sub.setText(Ui.arabicDigits(hits.size()) + " نتيجة لـ \u201c" + query + "\u201d");
        list.removeAllViews();
        badges.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Dhikr.Item it : hits) {
            View row = inflater.inflate(R.layout.item_dhikr, list, false);
            TextView text = row.findViewById(R.id.text);
            text.setText(it.text);
            Ui.applyQuranFont(text);
            TextView counter = row.findViewById(R.id.counter);
            boolean audio = Dhikr.hasAudio(this, it);
            counter.setVisibility(View.VISIBLE);
            counter.setText(audio ? "استمع" : "المصدر");
            if (audio) badges.put(idOf(it), counter);
            counter.setOnClickListener(v -> {
                if (audio) toggle(it, counter);
                else showReference(it);
            });
            row.setOnClickListener(v -> {
                if (audio) toggle(it, counter);
                else showReference(it);
            });
            row.setOnLongClickListener(v -> {
                showReference(it);
                return true;
            });
            list.addView(row);
        }
        refreshBadges();
    }

    // ---------- المستوى التاني ----------

    private void showChapters(int catId, String catName) {
        level = 1;
        categoryId = catId;
        categoryName = catName;
        title.setText(catName);
        List<Dhikr.Chapter> chs = Dhikr.chapters(this, catId);
        sub.setText(Ui.arabicDigits(chs.size()) + " باب · ارجع للخلف للأقسام");
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        badges.clear();
        for (Dhikr.Chapter ch : chs) {
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText(ch.name);
            Ui.applyNaskh(label, true);
            ((TextView) row.findViewById(R.id.value)).setText(
                    Ui.arabicDigits(Dhikr.items(this, ch.id).size()) + " ذكر");
            row.setOnClickListener(v -> showItems(ch.id, ch.name));
            list.addView(row);
        }
    }

    // ---------- المستوى التالت ----------

    private void showItems(int chId, String chName) {
        level = 2;
        chapterId = chId;
        title.setText(chName);
        List<Dhikr.Item> its = Dhikr.items(this, chId);
        sub.setText(Ui.arabicDigits(its.size()) + " ذكر · اضغط على الذكر للاستماع");
        list.removeAllViews();
        badges.clear();
        LayoutInflater inflater = LayoutInflater.from(this);

        // تشغيل الباب كله ورا بعض
        java.util.List<Dhikr.Item> withAudio = new java.util.ArrayList<>();
        for (Dhikr.Item it : its) {
            if (Dhikr.hasAudio(this, it)) withAudio.add(it);
        }
        if (withAudio.size() > 1) {
            View all = inflater.inflate(R.layout.item_setting, list, false);
            TextView l = all.findViewById(R.id.label);
            l.setText("شغّل الباب كله");
            Ui.applyNaskh(l, true);
            ((TextView) all.findViewById(R.id.value)).setText(
                    Ui.arabicDigits(withAudio.size()) + " ذكر ورا بعض");
            all.setOnClickListener(v -> playAll(withAudio, chName));
            list.addView(all);
        }
        for (Dhikr.Item it : its) {
            View row = inflater.inflate(R.layout.item_dhikr, list, false);
            TextView text = row.findViewById(R.id.text);
            text.setText(it.text);
            Ui.applyQuranFont(text);
            TextView counter = row.findViewById(R.id.counter);
            boolean audio = Dhikr.hasAudio(this, it);
            counter.setVisibility(View.VISIBLE);
            counter.setText(audio ? "استمع" : "المصدر");
            if (audio) badges.put(idOf(it), counter);
            View.OnClickListener play = v -> {
                if (audio) {
                    toggle(it, counter);
                } else {
                    showReference(it);
                }
            };
            counter.setOnClickListener(v -> {
                if (audio) toggle(it, counter);
                else showReference(it);
            });
            row.setOnClickListener(play);
            row.setOnLongClickListener(v -> {
                showReference(it);
                return true;
            });
            list.addView(row);
        }
    }

    private void showReference(Dhikr.Item it) {
        String ref = it.reference == null || it.reference.trim().isEmpty()
                ? "مفيش تخريج مسجّل للذكر ده." : it.reference;
        final String key = "dhikr:" + it.id;
        final boolean fav = Bookmarks.hasFav(this, key);
        new AlertDialog.Builder(this)
                .setTitle("التخريج")
                .setMessage(ref)
                .setPositiveButton("تمام", null)
                .setNegativeButton(fav ? "شيل من المفضلة" : "ضيف للمفضلة", (d, w) -> {
                    Bookmarks.toggleFav(this, key);
                    Toast.makeText(this, fav ? "اتشال من المفضلة" : "اتضاف للمفضلة",
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // ---------- الصوت ----------

    private String idOf(Dhikr.Item it) {
        return "dhikr-" + it.id;
    }

    private void playAll(java.util.List<Dhikr.Item> its, String chapterName) {
        java.util.List<String> assets = new java.util.ArrayList<>();
        java.util.List<String> ids = new java.util.ArrayList<>();
        java.util.List<String> titles = new java.util.ArrayList<>();
        for (Dhikr.Item it : its) {
            assets.add(Dhikr.asset(it));
            ids.add(idOf(it));
            titles.add(chapterName);
        }
        if (!Audio.playQueue(this, assets, ids, titles, chapterName)) {
            Toast.makeText(this, "تعذّر التشغيل", Toast.LENGTH_SHORT).show();
        }
        refreshBadges();
    }

    private void toggle(Dhikr.Item it, TextView badge) {
        String id = idOf(it);
        if (id.equals(Audio.currentId())) {
            Audio.stop();
            return;
        }
        boolean ok = Audio.playAsset(this, Dhikr.asset(it), id, "ذكر", title.getText().toString());
        if (!ok) Toast.makeText(this, "تعذّر تشغيل الصوت", Toast.LENGTH_SHORT).show();
        refreshBadges();
    }

    /** يحدّث نص الأزرار حسب اللي شغال. */
    private void refreshBadges() {
        String playing = Audio.currentId();
        for (java.util.Map.Entry<String, TextView> e : badges.entrySet()) {
            e.getValue().setText(e.getKey().equals(playing)
                    ? (Audio.isPlaying() ? "إيقاف ⏸" : "استكمال ▶") : "استمع");
        }
    }

    @Override
    public void onAudioChanged() {
        runOnUiThread(this::refreshBadges);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Audio.attach(this);
        refreshBadges();
    }

    @Override
    protected void onPause() {
        // الصوت بيكمّل في الخلفية: بنفصل الشاشة بس
        Audio.detach(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (level == 2) {
            showChapters(categoryId, categoryName);
        } else if (level == 1) {
            showCategories();
        } else {
            super.onBackPressed();
        }
    }
}
