package com.salah.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

/** العلامات المرجعية في المصحف. */
public class BookmarksActivity extends BaseActivity {

    private LinearLayout list;
    private TextView sub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("العلامات المرجعية");
        Ui.applyNaskh(title, true);
        sub = findViewById(R.id.screenSub);
        list = findViewById(R.id.list);
        render();
    }

    private void render() {
        list.removeAllViews();
        List<Bookmarks.Mark> marks = Bookmarks.all(this);
        List<String> favs = Bookmarks.favs(this);
        if (marks.isEmpty() && favs.isEmpty()) {
            sub.setText("مفيش علامات لسه — افتح أي آية واضغط “ضع علامة”");
            return;
        }
        sub.setText(Ui.arabicDigits(marks.size()) + " آية و " + Ui.arabicDigits(favs.size())
                + " مفضلة · اضغط للفتح، ومطوّلًا للحذف");

        List<Quran.Sura> suras = Quran.suras(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Bookmarks.Mark m : marks) {
            String name = m.sura >= 1 && m.sura <= suras.size() ? suras.get(m.sura - 1).name : "";
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView label = row.findViewById(R.id.label);
            label.setText("سورة " + name);
            Ui.applyNaskh(label, true);
            TextView value = row.findViewById(R.id.value);
            String aya = Quran.ayaText(this, m.sura, m.ayah);
            value.setText(aya == null
                    ? "الآية " + Ui.arabicDigits(m.ayah)
                    : "الآية " + Ui.arabicDigits(m.ayah) + " · " + shorten(aya));
            row.setOnClickListener(v -> {
                Intent i = new Intent(this, SuraActivity.class);
                i.putExtra(SuraActivity.EXTRA_SURA, m.sura);
                i.putExtra(SuraActivity.EXTRA_AYA, m.ayah);
                startActivity(i);
            });
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("حذف العلامة")
                        .setMessage("سورة " + name + " — الآية " + Ui.arabicDigits(m.ayah))
                        .setPositiveButton("احذف", (d, w) -> {
                            Bookmarks.remove(this, m.sura, m.ayah);
                            render();
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
                return true;
            });
            list.addView(row);
        }

        addFavourites(inflater, favs);

        View clear = inflater.inflate(R.layout.item_setting, list, false);
        TextView label = clear.findViewById(R.id.label);
        label.setText("مسح كل العلامات");
        Ui.applyNaskh(label, true);
        ((TextView) clear.findViewById(R.id.value)).setText("لا يمكن التراجع");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("مسح كل العلامات")
                .setMessage("هتتمسح كل العلامات ومش هينفع ترجّعها. متأكد؟")
                .setPositiveButton("امسح الكل", (d, w) -> {
                    Bookmarks.clear(this);
                    Bookmarks.clearFavs(this);
                    render();
                })
                .setNegativeButton("إلغاء", null)
                .show());
        list.addView(clear);
    }

    /** الأذكار والابتهالات المضافة للمفضلة. */
    private void addFavourites(LayoutInflater inflater, List<String> favs) {
        for (String key : favs) {
            int colon = key.indexOf(':');
            if (colon <= 0) continue;
            String kind = key.substring(0, colon);
            String id = key.substring(colon + 1);
            String label = null, value = null;
            Runnable open = null;

            if ("ibtihal".equals(kind)) {
                for (Ibtihal.Track t : Ibtihal.available(this)) {
                    if (t.file.equals(id)) {
                        label = t.title;
                        value = t.kind + " · مفضلة";
                        open = () -> startActivity(new Intent(this, IbtihalatActivity.class));
                        break;
                    }
                }
            } else if ("dhikr".equals(kind)) {
                try {
                    int n = Integer.parseInt(id);
                    for (Dhikr.Category cat : Dhikr.categories(this)) {
                        for (Dhikr.Chapter ch : Dhikr.chapters(this, cat.id)) {
                            for (Dhikr.Item it : Dhikr.items(this, ch.id)) {
                                if (it.id == n) {
                                    label = shorten(it.text);
                                    value = ch.name;
                                    open = () -> startActivity(new Intent(this, AdhkarActivity.class));
                                }
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (label == null) continue;

            final Runnable go = open;
            View row = inflater.inflate(R.layout.item_setting, list, false);
            TextView l = row.findViewById(R.id.label);
            l.setText(label);
            Ui.applyNaskh(l, true);
            ((TextView) row.findViewById(R.id.value)).setText(value);
            row.setOnClickListener(v -> {
                if (go != null) go.run();
            });
            row.setOnLongClickListener(v -> {
                Bookmarks.removeFav(this, key);
                render();
                return true;
            });
            list.addView(row);
        }
    }

    private String shorten(String s) {
        s = s.trim();
        return s.length() <= 40 ? s : s.substring(0, 40) + "…";
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }
}
