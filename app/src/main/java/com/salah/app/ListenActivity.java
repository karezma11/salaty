package com.salah.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * «استمع للقرآن» (الشكل «ب» اللي اختاره المستخدم): صوت بس من غير المصحف. فوق كروت ملوّنة
 * لقوائم جاهزة بضغطة، وتحت كل السور، ومشغّل صغير ثابت تحت الشاشة. السور بتكمّل ورا بعض،
 * واللي مش متحمّلة بتتحمّل لوحدها (الأولى قبل التشغيل، واللي بعدها وانت بتسمع).
 */
public class ListenActivity extends BaseActivity
        implements RecitationPlayer.Listener, DownloadService.Listener {

    private static final int GOLD = 0xFFE2B04A;

    private float dp;
    private List<Quran.Sura> suras;
    private LinearLayout rowsBox;
    private final List<TextView> statuses = new ArrayList<>();
    private TextView reciterView, miniTitle, miniSub;
    private ImageView miniPlay, miniMoon;
    private SleepStrip sleepStrip;

    /** القايمة اللي مستنية تحميل أول سورة فيها قبل ما تشتغل. */
    private int[] pendingQueue;
    private int pendingFrom, pendingAyah;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dp = getResources().getDisplayMetrics().density;
        suras = Quran.suras(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.bg));
        setContentView(root);

        // الرأس
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setBackgroundResource(R.drawable.bg_hero);
        hero.setPadding(px(20), px(20), px(20), px(20));
        TextView title = text("استمع للقرآن", 22, Color.WHITE, true);
        Ui.applyNaskh(title, true);
        hero.addView(title);
        reciterView = text("", 13, 0xFFCFEBDD, false);
        reciterView.setOnClickListener(v -> pickReciter());
        hero.addView(reciterView);
        TextView everyone = text("⬇  حمّل المصحف كامل لكل القراء", 13, 0xFFF3D27A, true);
        everyone.setPadding(0, px(8), 0, 0);
        everyone.setOnClickListener(v -> Downloads.askEveryone(this));
        hero.addView(everyone);
        root.addView(hero);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(12), px(12), px(12), px(16));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        sleepStrip = SleepStrip.insert(this, root, 1);

        buildTiles(content);
        TextView all = text("كل السور", 14, ContextCompat.getColor(this, R.color.ink_soft), true);
        all.setPadding(px(4), px(14), px(4), px(6));
        content.addView(all);
        rowsBox = new LinearLayout(this);
        rowsBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(rowsBox);
        buildRows(0);

        root.addView(buildMini());
        refresh();
    }

    // ---------- القوائم الجاهزة ----------

    private void buildTiles(LinearLayout content) {
        int last = Math.max(1, Prefs.listenSura(this));
        String lastName = last <= suras.size() ? suras.get(last - 1).name : "";
        addTileRow(content,
                tile("كمّل من آخر مرة", "سورة " + lastName + " · من الآية "
                                + Ui.arabicDigits(Prefs.listenAyah(this)),
                        0xFFB8862E, 0xFFE2B04A, R.drawable.ic_quran,
                        v -> play(range(last, 114), 0, Prefs.listenAyah(this))),
                tile("الختمة كاملة", "١١٤ سورة ورا بعض", 0xFF0B5D3B, 0xFF07402A,
                        R.drawable.ic_wird, v -> play(range(1, 114), 0, 1)));
        addTileRow(content,
                tile("سور الصباح", "يس · الواقعة · الملك", 0xFFC9743E, 0xFFE29A5A,
                        R.drawable.ic_adhkar, v -> play(new int[]{36, 56, 67}, 0, 1)),
                tile("قبل النوم", "السجدة · الملك · المعوذات", 0xFF1D2C5E, 0xFF3B4A8C,
                        R.drawable.ic_notification, v -> play(new int[]{32, 67, 112, 113, 114}, 0, 1)));
        addTileRow(content,
                tile("جزء عمّ", "من النبأ للناس · ٣٧ سورة", 0xFF5E3B8C, 0xFF7E5BB0,
                        R.drawable.ic_quran, v -> play(range(78, 114), 0, 1)),
                tile("سورة الكهف", "يوم الجمعة", 0xFF0E6B5C, 0xFF14907A,
                        R.drawable.ic_notification, v -> play(new int[]{18}, 0, 1)));
    }

    private void addTileRow(LinearLayout content, View a, View b) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams la = new LinearLayout.LayoutParams(0, px(78), 1f);
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(0, px(78), 1f);
        lb.setMarginStart(px(8));
        row.addView(a, la);
        row.addView(b, lb);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = px(8);
        content.addView(row, lp);
    }

    private View tile(String t, String s, int c1, int c2, int icon, View.OnClickListener click) {
        FrameLayout f = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{c1, c2});
        bg.setCornerRadius(px(14));
        f.setBackground(bg);
        f.setClipToOutline(true);
        f.setForeground(ContextCompat.getDrawable(this, android.R.drawable.list_selector_background));
        // رسمة باهتة في الركن
        ImageView art = new ImageView(this);
        art.setImageResource(icon);
        art.setColorFilter(0xFFF3D27A);
        art.setAlpha(0.28f);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(px(62), px(62),
                Gravity.BOTTOM | Gravity.START);
        ap.setMarginStart(-px(10));
        ap.bottomMargin = -px(14);
        f.addView(art, ap);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(12), px(10), px(12), px(8));
        TextView tt = text(t, 15, Color.WHITE, true);
        Ui.applyNaskh(tt, true);
        col.addView(tt);
        TextView ss = text(s, 11, 0xE6FFFFFF, false);
        col.addView(ss);
        f.addView(col);
        f.setOnClickListener(click);
        return f;
    }

    private static int[] range(int from, int to) {
        int[] q = new int[Math.max(0, to - from + 1)];
        for (int i = 0; i < q.length; i++) q[i] = from + i;
        return q;
    }

    // ---------- كل السور (على دفعات عشان الشاشة تفتح بسرعة) ----------

    private void buildRows(int start) {
        int end = Math.min(suras.size(), start + 20);
        for (int i = start; i < end; i++) {
            final Quran.Sura s = suras.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_row_card);
            row.setPadding(px(10), px(8), px(10), px(8));
            row.addView(new StarNumber(this, s.index), new LinearLayout.LayoutParams(px(36), px(36)));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cp.setMarginStart(px(10));
            TextView name = text("سورة " + s.name, 18, ContextCompat.getColor(this, R.color.ink), false);
            Ui.applyQuranFont(name);
            col.addView(name);
            col.addView(text(Ui.arabicDigits(s.ayas) + " آية · " + s.type, 11,
                    ContextCompat.getColor(this, R.color.ink_soft), false));
            row.addView(col, cp);
            TextView st = text("", 12, ContextCompat.getColor(this, R.color.green_700), false);
            row.addView(st);
            statuses.add(st);
            row.setOnClickListener(v -> {
                if (RecitationPlayer.isActive() && RecitationPlayer.currentSura() == s.index) {
                    toggle();
                } else {
                    play(range(s.index, 114), 0, 1);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = px(6);
            rowsBox.addView(row, lp);
        }
        refreshRows();
        if (end < suras.size()) rowsBox.post(() -> buildRows(end));
    }

    // ---------- المشغّل الصغير ----------

    private View buildMini() {
        LinearLayout mini = new LinearLayout(this);
        mini.setOrientation(LinearLayout.HORIZONTAL);
        mini.setGravity(Gravity.CENTER_VERTICAL);
        mini.setBackgroundColor(0xFF07402A);
        mini.setPadding(px(14), px(10), px(14), px(10));
        miniPlay = icon(R.drawable.ic_play, GOLD, 34);
        miniPlay.setOnClickListener(v -> toggle());
        mini.addView(miniPlay);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cp.setMarginStart(px(12));
        miniTitle = text("", 16, Color.WHITE, false);
        Ui.applyQuranFont(miniTitle);
        miniSub = text("", 11, 0xFFCFEBDD, false);
        col.addView(miniTitle);
        col.addView(miniSub);
        mini.addView(col, cp);
        miniMoon = icon(R.drawable.ic_sleep, 0xFFCFEBDD, 26);
        miniMoon.setOnClickListener(v -> {
            SleepTimer.cycle(true);
            String l = SleepTimer.shortLabel();
            Toast.makeText(this, l.isEmpty() ? "اتلغى مؤقت النوم" : "مؤقت النوم: " + l,
                    Toast.LENGTH_SHORT).show();
            sleepStrip.resume();
            refresh();
        });
        mini.addView(miniMoon);
        ImageView next = icon(R.drawable.ic_next, Color.WHITE, 26);
        next.setOnClickListener(v -> skipSura());
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(px(40), px(40));
        np.setMarginStart(px(6));
        mini.addView(next, np);
        return mini;
    }

    private ImageView icon(int res, int tint, int size) {
        ImageView i = new ImageView(this);
        i.setImageResource(res);
        i.setColorFilter(tint);
        i.setPadding(px(6), px(6), px(6), px(6));
        i.setLayoutParams(new LinearLayout.LayoutParams(px(size + 12), px(size + 12)));
        return i;
    }

    // ---------- التشغيل ----------

    /** يشغّل قايمة من السورة رقم from؛ لو مش متحمّلة يحمّلها الأول ويشغّلها لوحده. */
    private void play(int[] queue, int from, int ayah) {
        if (queue.length == 0) return;
        int s = queue[from];
        if (Recitation.available(this, s)) {
            pendingQueue = null;
            if (RecitationPlayer.startQueue(this, queue, from, Math.max(1, ayah), this)) {
                startService();
            } else {
                Toast.makeText(this, "تعذّر التشغيل", Toast.LENGTH_SHORT).show();
            }
        } else if (Downloads.online(this)) {
            pendingQueue = queue;
            pendingFrom = from;
            pendingAyah = ayah;
            DownloadService.start(this, s);
            miniTitle.setText("سورة " + suras.get(s - 1).name);
            miniSub.setText("بتتحمّل الأول وهتشتغل لوحدها…");
        } else {
            Toast.makeText(this, "السورة دي مش متحمّلة، ومحتاجة نت أول مرة",
                    Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    private void toggle() {
        if (!RecitationPlayer.isActive()) {
            int last = Math.max(1, Prefs.listenSura(this));
            play(range(last, 114), 0, Prefs.listenAyah(this));
            return;
        }
        try {
            startService(new Intent(this, RecitationService.class)
                    .setAction(RecitationService.ACTION_TOGGLE));
        } catch (RuntimeException e) {
            if (RecitationPlayer.isPlaying()) RecitationPlayer.pause();
            else RecitationPlayer.resume();
        }
        miniPlay.postDelayed(this::refresh, 150);
    }

    private void skipSura() {
        int next = RecitationPlayer.nextSura();
        if (!RecitationPlayer.isActive() || next <= 0) return;
        int[] q = RecitationPlayer.queue();
        if (q != null) {
            play(q, RecitationPlayer.queuePos() + 1, 1);
        } else {
            play(range(next, 114), 0, 1);
        }
    }

    private void startService() {
        try {
            startForegroundService(new Intent(this, RecitationService.class)
                    .setAction(RecitationService.ACTION_SHOW));
        } catch (RuntimeException ignored) {
        }
    }

    private void pickReciter() {
        new AlertDialog.Builder(this)
                .setTitle("القارئ")
                .setItems(Reciters.names(), (d, w) -> {
                    Prefs.setReciter(this, Reciters.ALL[w].id);
                    Recitation.invalidate();
                    refresh();
                })
                .show();
    }

    // ---------- التحديث ----------

    private void refresh() {
        reciterView.setText("القارئ: " + Reciters.current(this).name + " · اضغط للتغيير");
        boolean active = RecitationPlayer.isActive();
        miniPlay.setImageResource(RecitationPlayer.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        miniMoon.setImageResource(SleepTimer.isActive() ? R.drawable.ic_sleep_on : R.drawable.ic_sleep);
        miniMoon.setColorFilter(SleepTimer.isActive() ? GOLD : 0xFFCFEBDD);
        if (active) {
            int s = RecitationPlayer.currentSura();
            miniTitle.setText(s >= 1 && s <= suras.size() ? "سورة " + suras.get(s - 1).name : "");
            int n = RecitationPlayer.nextSura();
            miniSub.setText("الآية " + Ui.arabicDigits(RecitationPlayer.currentAyah()) + " من "
                    + Ui.arabicDigits(RecitationPlayer.lastAyah())
                    + (n > 0 && n <= suras.size() ? " · بعدها: " + suras.get(n - 1).name : ""));
        } else if (pendingQueue == null) {
            int last = Prefs.listenSura(this);
            miniTitle.setText(last > 0 ? "سورة " + suras.get(last - 1).name : "اختار سورة أو قايمة");
            miniSub.setText(last > 0 ? "اضغط تشغيل وكمّل من الآية "
                    + Ui.arabicDigits(Prefs.listenAyah(this)) : "السور بتكمّل ورا بعض لوحدها");
        }
        refreshRows();
    }

    private void refreshRows() {
        int cur = RecitationPlayer.isActive() ? RecitationPlayer.currentSura() : 0;
        for (int i = 0; i < statuses.size(); i++) {
            int s = i + 1;
            TextView st = statuses.get(i);
            if (s == cur) {
                st.setText(RecitationPlayer.isPlaying() ? "بتتسمع الآن" : "متوقفة");
                st.setTextColor(0xFFB8862E);
            } else if (Recitation.available(this, s)) {
                st.setText("✓ متحمّلة");
                st.setTextColor(ContextCompat.getColor(this, R.color.green_700));
            } else {
                st.setText("↓ هتتحمّل لوحدها");
                st.setTextColor(ContextCompat.getColor(this, R.color.ink_soft));
            }
        }
    }

    @Override
    public void onAyah(int sura, int ayah) {
        runOnUiThread(this::refresh);
    }

    @Override
    public void onStopped() {
        runOnUiThread(this::refresh);
    }

    @Override
    public void onProgress(int sura, int done, int total) {
        if (pendingQueue == null || pendingQueue[pendingFrom] != sura) return;
        runOnUiThread(() -> miniSub.setText("بتتحمّل… " + Ui.arabicDigits(done) + " من "
                + Ui.arabicDigits(total) + " آية، وهتشتغل لوحدها"));
    }

    @Override
    public void onFinished(int sura, boolean ok) {
        runOnUiThread(() -> {
            if (pendingQueue != null && pendingQueue[pendingFrom] == sura) {
                int[] q = pendingQueue;
                pendingQueue = null;
                if (ok && Recitation.available(this, sura)) {
                    play(q, pendingFrom, pendingAyah);
                } else {
                    miniSub.setText("التحميل ما كملش — اضغط تاني");
                }
            }
            refreshRows();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DownloadService.setListener(this);
        if (RecitationPlayer.isActive()) RecitationPlayer.attach(this);
        sleepStrip.resume();
        refresh();
    }

    @Override
    protected void onPause() {
        sleepStrip.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        RecitationPlayer.detach(this);
        DownloadService.setListener(null);
        super.onDestroy();
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    /** رقم السورة جوه نجمة ثمانية دهبي. */
    static final class StarNumber extends View {
        private final String n;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        StarNumber(Context c, int number) {
            super(c);
            n = Ui.arabicDigits(number);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) * 0.33f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1.5f, w / 28f));
            p.setColor(0xFFC9A24A);
            cv.save();
            cv.translate(w / 2, h / 2);
            cv.drawRect(-r, -r, r, r, p);
            cv.rotate(45);
            cv.drawRect(-r, -r, r, r, p);
            cv.restore();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF0B5D3B);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(n.length() > 2 ? w * 0.26f : w * 0.32f);
            cv.drawText(n, w / 2, h / 2 - (p.descent() + p.ascent()) / 2, p);
        }
    }
}
