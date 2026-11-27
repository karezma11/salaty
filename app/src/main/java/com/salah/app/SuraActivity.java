package com.salah.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/** عرض سورة كاملة، مع تلاوة آية بآية وتظليل الآية الجارية. */
public class SuraActivity extends BaseActivity
        implements RecitationPlayer.Listener, DownloadService.Listener {

    public static final String EXTRA_SURA = "sura";
    public static final String EXTRA_AYA = "aya";
    /** تذكير الكهف: «شغّل التلاوة» بيفتح السورة ويدوس تشغيل. */
    public static final String EXTRA_PLAY = "play";

    private ScrollView scroll;
    private TextView body, playerTitle, playerSub;
    private FloatingActionButton playBtn;

    private int suraIndex;
    private String suraTitle = "";
    private int ayahCount;
    private int[] ayahStart;
    private int[] ayahEnd;
    private SpannableStringBuilder text;
    private BackgroundColorSpan highlight;
    private int green, highlightColor;
    private float touchX, touchY;
    private boolean hideText;
    private boolean bodyReady;
    private int pendingJump;
    private SleepStrip sleepStrip;

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sura);

        // شريط مؤقت النوم فوق شريط التشغيل
        android.view.View card = findViewById(R.id.playerCard);
        android.view.ViewGroup cardParent = (android.view.ViewGroup) card.getParent();
        sleepStrip = SleepStrip.insert(this, cardParent, cardParent.indexOfChild(card));

        green = ContextCompat.getColor(this, R.color.green_700);
        highlightColor = ContextCompat.getColor(this, R.color.highlight);

        suraIndex = getIntent().getIntExtra(EXTRA_SURA, 1);
        List<Quran.Sura> all = Quran.suras(this);
        Quran.Sura sura = (suraIndex >= 1 && suraIndex <= all.size())
                ? all.get(suraIndex - 1) : null;
        ayahCount = sura == null ? 0 : sura.ayas;

        suraTitle = sura == null ? "" : sura.name;
        TextView name = findViewById(R.id.suraName);
        name.setText(sura == null ? "سورة" : "سورة " + sura.name);
        Ui.applyNaskh(name, true);
        ((TextView) findViewById(R.id.suraMeta)).setText(sura == null ? ""
                : sura.type + " · "
                + Ui.arabicDigits(sura.ayas) + " آية");

        findViewById(R.id.suraName).setOnLongClickListener(v -> {
            askAyahNumber();
            return true;
        });
        scroll = findViewById(R.id.scroll);
        body = findViewById(R.id.body);
        playerTitle = findViewById(R.id.playerTitle);
        playerSub = findViewById(R.id.playerSub);
        playBtn = findViewById(R.id.playBtn);

        Ui.applyQuranFont(body);
        Ui.applyNaskh(playerTitle, true);
        body.setTextSize(Prefs.quranFontSize(this));
        loadBodyAsync();

        playBtn.setOnClickListener(v -> togglePlay());
        playBtn.setOnLongClickListener(v -> {
            showPlaybackOptions();
            return true;
        });
        playerTitle.setOnClickListener(v -> showPlaybackOptions());
        body.setOnClickListener(v -> {
            if (hideText) {
                toggleHideText();
            } else {
                showTafsirAtTouch();
            }
        });
        body.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                touchX = ev.getX();
                touchY = ev.getY();
            }
            return false;
        });
        findViewById(R.id.fontPlus).setOnClickListener(v -> changeFont(2));
        findViewById(R.id.fontMinus).setOnClickListener(v -> changeFont(-2));

        keepScreenOn(Prefs.keepScreenOn(this));
        Prefs.setLastSura(this, suraIndex);
        syncControls();

        int jump = getIntent().getIntExtra(EXTRA_AYA, 0);
        if (jump <= 0) jump = Prefs.lastAyah(this, suraIndex);
        pendingJump = jump;
        if (getIntent().getBooleanExtra(EXTRA_PLAY, false)
                && !(RecitationPlayer.isActive() && RecitationPlayer.currentSura() == suraIndex)) {
            playBtn.post(this::togglePlay);
        }
    }

    /** خيارات التلاوة والحفظ. */
    private void showPlaybackOptions() {
        String[] items = {
                "تكرار كل آية: " + Ui.arabicDigits(RecitationPlayer.repeatEach()) + "×",
                "تكرار مقطع (من آية لآية)",
                RecitationPlayer.continuous() ? "يكمّل للسورة اللي بعدها ✓" : "يقف عند آخر السورة",
                "سرعة التلاوة: " + speedLabel(),
                hideText ? "أظهر النص (وضع الحفظ شغّال)" : "وضع الحفظ — إخفاء النص",
                SleepTimer.isActive() ? "مؤقت النوم: " + SleepTimer.shortLabel() : "مؤقت النوم"
        };
        new AlertDialog.Builder(this)
                .setTitle("التلاوة والحفظ")
                .setItems(items, (d, w) -> {
                    switch (w) {
                        case 0: pickRepeat(); break;
                        case 1: pickRange(); break;
                        case 2:
                            RecitationPlayer.setContinuous(!RecitationPlayer.continuous());
                            Toast.makeText(this, RecitationPlayer.continuous()
                                    ? "هيكمّل للسورة اللي بعدها" : "هيقف عند آخر السورة",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 3: pickSpeed(); break;
                        case 4: toggleHideText(); break;
                        default: pickSleep();
                    }
                })
                .show();
    }

    private String speedLabel() {
        float sp = RecitationPlayer.speed();
        if (sp <= 0.76f) return "بطيئة";
        if (sp <= 1.01f) return "عادية";
        if (sp <= 1.26f) return "سريعة";
        return "أسرع";
    }

    private void pickRepeat() {
        final int[] choices = {1, 2, 3, 5, 7, 10};
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            labels[i] = choices[i] == 1 ? "مرة واحدة"
                    : Ui.arabicDigits(choices[i]) + " مرات لكل آية";
        }
        new AlertDialog.Builder(this)
                .setTitle("تكرار الآية")
                .setItems(labels, (d, w) -> {
                    RecitationPlayer.setRepeatEach(choices[w]);
                    syncControls();
                })
                .show();
    }

    private void pickRange() {
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        final android.widget.EditText from = new android.widget.EditText(this);
        from.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        from.setHint("من آية");
        final android.widget.EditText to = new android.widget.EditText(this);
        to.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        to.setHint("إلى آية");
        box.addView(from);
        box.addView(to);

        new AlertDialog.Builder(this)
                .setTitle("تكرار مقطع ٣ مرات")
                .setView(box)
                .setPositiveButton("ابدأ", (d, w) -> {
                    try {
                        int a = Integer.parseInt(from.getText().toString().trim());
                        int b = Integer.parseInt(to.getText().toString().trim());
                        if (a < 1 || b < a || b > ayahCount) {
                            Toast.makeText(this, "أرقام غير صحيحة", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        RecitationPlayer.setRange(a, b, 3);
                        if (!Recitation.available(this, suraIndex)) {
                            offerDownload();
                            return;
                        }
                        RecitationPlayer.start(this, suraIndex, a, ayahCount, this);
                        startService();
                        syncControls();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "اكتب أرقام", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void pickSpeed() {
        final float[] choices = {0.75f, 1.0f, 1.25f, 1.5f};
        String[] labels = {"بطيئة (للحفظ)", "عادية", "سريعة", "أسرع"};
        new AlertDialog.Builder(this)
                .setTitle("سرعة التلاوة")
                .setItems(labels, (d, w) -> RecitationPlayer.setSpeed(choices[w]))
                .show();
    }

    private void pickSleep() {
        SleepStrip.pick(this, true, () -> sleepStrip.resume());
    }

    /** وضع الحفظ: يخفي النص ويسيب الصوت. */
    private void toggleHideText() {
        hideText = !hideText;
        body.setTextColor(hideText
                ? 0x00000000
                : androidx.core.content.ContextCompat.getColor(this, R.color.ink));
        Toast.makeText(this, hideText
                ? "النص مخفي — اضغط عليه لإظهاره"
                : "ظهر النص", Toast.LENGTH_SHORT).show();
    }

    /** انتقال سريع لرقم آية. */
    private void askAyahNumber() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("رقم الآية (١ - " + Ui.arabicDigits(ayahCount) + ")");
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);
        new AlertDialog.Builder(this)
                .setTitle("انتقال لآية")
                .setView(box)
                .setPositiveButton("اذهب", (d, w) -> {
                    try {
                        int n = Integer.parseInt(input.getText().toString().trim());
                        if (n >= 1 && n <= ayahCount) {
                            scrollToAyah(n);
                        } else {
                            Toast.makeText(this, "رقم خارج السورة", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "اكتب رقم صحيح", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void changeFont(int delta) {
        int size = Prefs.quranFontSize(this) + delta;
        if (size < 16) size = 16;
        if (size > 46) size = 46;
        Prefs.setQuranFontSize(this, size);
        body.setTextSize(size);
        bodyReady = false;
        loadBodyAsync();
    }

    /**
     * البقرة لوحدها 55 ألف حرف. بناء النص وقياسه في الخيط الرئيسي كان بيجمّد
     * الشاشة ثواني، فبنعملهم في الخلفية وبنحط النص جاهز مقاس.
     */
    private void loadBodyAsync() {
        body.setText("جاري التحميل...");
        final android.text.TextPaint paint = new android.text.TextPaint(body.getPaint());
        new Thread(() -> {
            build();
            final androidx.core.text.PrecomputedTextCompat.Params params =
                    new androidx.core.text.PrecomputedTextCompat.Params.Builder(paint)
                            .setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE)
                            .setHyphenationFrequency(
                                    android.text.Layout.HYPHENATION_FREQUENCY_NONE)
                            .setTextDirection(
                                    android.text.TextDirectionHeuristics.FIRSTSTRONG_RTL)
                            .build();
            Object pre = null;
            try {
                pre = androidx.core.text.PrecomputedTextCompat.create(text, params);
            } catch (Exception ignored) {
            }
            final Object precomputed = pre;
            runOnUiThread(() -> {
                boolean applied = false;
                if (precomputed instanceof androidx.core.text.PrecomputedTextCompat) {
                    try {
                        androidx.core.widget.TextViewCompat.setTextMetricsParams(body, params);
                        androidx.core.widget.TextViewCompat.setPrecomputedText(body,
                                (androidx.core.text.PrecomputedTextCompat) precomputed);
                        applied = true;
                    } catch (Exception ignored) {
                    }
                }
                if (!applied) {
                    body.setText(text, android.widget.TextView.BufferType.SPANNABLE);
                }
                bodyReady = true;
                int jump = pendingJump;
                if (jump > 1) body.post(() -> scrollToAyah(jump));
                if (RecitationPlayer.isActive() && RecitationPlayer.currentSura() == suraIndex) {
                    highlightAyah(RecitationPlayer.currentAyah());
                }
            });
        }).start();
    }

    /** يبني نص السورة ويحفظ بداية ونهاية كل آية عشان التظليل. */
    private void build() {
        text = new SpannableStringBuilder();
        ayahStart = new int[ayahCount + 2];
        ayahEnd = new int[ayahCount + 2];
        for (Quran.Aya aya : Quran.ayas(this, suraIndex)) {
            int start = text.length();
            text.append(aya.text);
            int markStart = text.length();
            text.append(Quran.ayaMark(aya.number));
            text.setSpan(new ForegroundColorSpan(green), markStart, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new RelativeSizeSpan(0.8f), markStart, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (aya.number < ayahStart.length) {
                ayahStart[aya.number] = start;
                ayahEnd[aya.number] = text.length();
            }
        }
    }

    // ---------- التشغيل ----------

    private void togglePlay() {
        if (RecitationPlayer.currentSura() == suraIndex && RecitationPlayer.isActive()) {
            if (RecitationPlayer.isPlaying()) {
                RecitationPlayer.pause();
            } else {
                RecitationPlayer.resume();
            }
            syncControls();
            return;
        }
        if (!Recitation.available(this, suraIndex)) {
            offerDownload();
            return;
        }
        RecitationPlayer.start(this, suraIndex, 1, ayahCount, this);
        startService();
        syncControls();
    }

    /** تشغّل الخدمة اللي بتخلّي التلاوة مكمّلة بعد قفل الشاشة. */
    private void startService() {
        try {
            startForegroundService(new android.content.Intent(this, RecitationService.class)
                    .setAction(RecitationService.ACTION_SHOW));
        } catch (RuntimeException ignored) {
            // النظام منع تشغيل الخدمة: التلاوة تفضل شغالة والشاشة مفتوحة
        }
    }

    /** يحدد الآية اللي المستخدم ضغط عليها ويعرض تفسيرها. */
    private void showTafsirAtTouch() {
        android.text.Layout layout = body.getLayout();
        if (layout == null) return;
        int line = layout.getLineForVertical((int) touchY);
        int offset = layout.getOffsetForHorizontal(line, touchX);
        int found = -1;
        for (int a = 1; a < ayahStart.length; a++) {
            if (ayahEnd[a] == 0) continue;
            if (offset >= ayahStart[a] && offset <= ayahEnd[a]) {
                found = a;
                break;
            }
        }
        if (found < 1) return;
        showTafsir(found);
    }

    private void showTafsir(int ayah) {
        String text = Tafsir.of(this, suraIndex, ayah);
        String verse = Quran.ayaText(this, suraIndex, ayah);
        if (text == null || text.isEmpty()) {
            text = "مفيش تفسير متاح للآية دي.";
        }

        TextView view = new TextView(this);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        view.setPadding(pad, pad / 2, pad, 0);
        view.setTextSize(16);
        view.setLineSpacing(0f, 1.25f);
        view.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ink));
        view.setText(text);
        Ui.applyNaskh(view, false);

        android.widget.ScrollView box = new android.widget.ScrollView(this);
        box.addView(view);

        final boolean marked = Bookmarks.has(this, suraIndex, ayah);
        final String body = verse;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("الآية " + Ui.arabicDigits(ayah) + " · " + Tafsir.current(this).name)
                .setView(box)
                .setPositiveButton("تشغيل من هنا", (d, w) -> {
                    if (Recitation.ayahAvailable(this, suraIndex, ayah)) {
                        RecitationPlayer.start(this, suraIndex, ayah, ayahCount, this);
                        startService();
                        syncControls();
                    } else {
                        offerDownload();
                    }
                })
                .setNeutralButton("شارك", (d, w) -> {
                    if (body != null) ShareAyah.share(this, suraIndex, suraTitle, ayah, body);
                })
                .setNegativeButton(marked ? "شيل العلامة" : "ضع علامة", (d, w) -> {
                    Bookmarks.toggle(this, suraIndex, ayah);
                    Toast.makeText(this,
                            marked ? "اتشالت العلامة"
                                   : "اتحطت علامة على الآية " + Ui.arabicDigits(ayah),
                            Toast.LENGTH_SHORT).show();
                })
                .create();
        dialog.show();
    }

    /** يعرض تحميل السورة لو مش متاحة. */
    private void offerDownload() {
        if (DownloadService.activeSura() == suraIndex) {
            new AlertDialog.Builder(this)
                    .setTitle("جاري التحميل")
                    .setMessage("تلاوة السورة بتتحمّل دلوقتي، استنى شوية.")
                    .setPositiveButton("تمام", null)
                    .show();
            return;
        }
        if (!Downloads.online(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("مفيش اتصال بالإنترنت")
                    .setMessage("تلاوة السورة دي لسه مش متحمّلة على الموبايل.\n\n"
                            + "اتصل بالإنترنت مرة واحدة عشان تحمّلها، وبعدها هتشتغل أوفلاين للأبد.\n\n"
                            + "أو ضع ملفات الآيات يدويًا في:\n" + Recitation.path(this))
                    .setPositiveButton("تمام", null)
                    .show();
            return;
        }
        String net = Downloads.onWifi(this) ? "أنت على واي فاي." : "تنبيه: أنت على بيانات الموبايل.";
        new AlertDialog.Builder(this)
                .setTitle("تحميل تلاوة السورة")
                .setMessage("السورة فيها " + Ui.arabicDigits(ayahCount) + " آية.\n" + net
                        + "\n\nالتحميل مرة واحدة بس، وبعدها التلاوة تشتغل من غير نت.")
                .setPositiveButton("حمّل", (d, w) -> {
                    DownloadService.setListener(this);
                    DownloadService.start(this, suraIndex);
                    playerTitle.setText("جاري التحميل...");
                    playerSub.setText("استنى شوية");
                })
                .setNegativeButton("مش دلوقتي", null)
                .show();
    }

    private void syncControls() {
        boolean mine = RecitationPlayer.currentSura() == suraIndex && RecitationPlayer.isActive();
        boolean playing = mine && RecitationPlayer.isPlaying();
        playBtn.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        if (mine) {
            playerTitle.setText(playing ? "جاري التلاوة" : "التلاوة متوقفة مؤقتًا");
            playerSub.setText("الآية " + Ui.arabicDigits(RecitationPlayer.currentAyah())
                    + " من " + Ui.arabicDigits(ayahCount));
        } else if (DownloadService.activeSura() == suraIndex) {
            playerTitle.setText("جاري التحميل...");
        } else if (Recitation.available(this, suraIndex)) {
            playerTitle.setText("تلاوة السورة");
            playerSub.setText("بصوت " + Reciters.current(this).name);
        } else {
            playerTitle.setText("تحميل التلاوة");
            playerSub.setText("اضغط للتحميل مرة واحدة · " + Reciters.current(this).name);
        }
    }

    private void scrollToAyah(int ayah) {
        if (ayah < 1 || ayah >= ayahStart.length) return;
        android.text.Layout layout = body.getLayout();
        if (layout == null) return;
        int line = layout.getLineForOffset(ayahStart[ayah]);
        int y = layout.getLineTop(line) + body.getTop() - scroll.getHeight() / 3;
        scroll.smoothScrollTo(0, Math.max(0, y));
    }

    /** يظلل الآية الجارية ويمرّر الشاشة ناحيتها. */
    private void highlightAyah(int ayah) {
        if (!bodyReady || ayah < 1 || ayah >= ayahStart.length) return;
        CharSequence shown = body.getText();
        if (!(shown instanceof android.text.Spannable)) return;
        android.text.Spannable sp = (android.text.Spannable) shown;
        // تعديل العلامة على نفس النص بيعيد الرسم بس، من غير إعادة قياس السورة كلها
        if (highlight != null) {
            try {
                sp.removeSpan(highlight);
            } catch (Exception ignored) {
            }
        }
        highlight = new BackgroundColorSpan(highlightColor);
        try {
            sp.setSpan(highlight, ayahStart[ayah], ayahEnd[ayah],
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } catch (Exception ignored) {
            return;
        }
        body.post(() -> scrollToAyah(ayah));
    }

    @Override
    public void onAyah(int sura, int ayah) {
        main.post(() -> {
            if (sura != suraIndex) return;
            highlightAyah(ayah);
            syncControls();
        });
    }

    @Override
    public void onStopped() {
        main.post(() -> {
            if (highlight != null && body.getText() instanceof android.text.Spannable) {
                try {
                    ((android.text.Spannable) body.getText()).removeSpan(highlight);
                } catch (Exception ignored) {
                }
            }
            highlight = null;
            syncControls();
        });
    }

    @Override
    public void onProgress(int sura, int done, int total) {
        if (sura != suraIndex) return;
        runOnUiThread(() -> {
            playerTitle.setText("جاري التحميل...");
            playerSub.setText(Ui.arabicDigits(done) + " من " + Ui.arabicDigits(total) + " آية");
        });
    }

    @Override
    public void onFinished(int sura, boolean ok) {
        if (sura != suraIndex) return;
        runOnUiThread(() -> {
            syncControls();
            if (ok && Recitation.available(this, suraIndex)) {
                RecitationPlayer.start(this, suraIndex, 1, ayahCount, this);
                startService();
                syncControls();
            } else {
                playerSub.setText("التحميل ما كملش — جرّب تاني");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DownloadService.setListener(this);
        if (RecitationPlayer.isActive() && RecitationPlayer.currentSura() == suraIndex) {
            RecitationPlayer.attach(this);
            highlightAyah(RecitationPlayer.currentAyah());
        }
        syncControls();
        sleepStrip.resume();
    }

    @Override
    protected void onPause() {
        savePosition();
        sleepStrip.pause();
        super.onPause();
    }

    /** يحفظ أول آية ظاهرة على الشاشة عشان يرجّعه لها بعدين. */
    private void savePosition() {
        if (RecitationPlayer.isActive() && RecitationPlayer.currentSura() == suraIndex) {
            Prefs.setLastAyah(this, suraIndex, RecitationPlayer.currentAyah());
            Progress.record(this, suraIndex, RecitationPlayer.currentAyah());
            return;
        }
        android.text.Layout layout = body.getLayout();
        if (layout == null) return;
        int top = scroll.getScrollY() - body.getTop();
        if (top < 0) top = 0;
        int line = layout.getLineForVertical(top);
        int offset = layout.getLineStart(line);
        for (int a = 1; a < ayahStart.length; a++) {
            if (ayahEnd[a] == 0) continue;
            if (offset <= ayahEnd[a]) {
                Prefs.setLastAyah(this, suraIndex, a);
                Progress.record(this, suraIndex, a);
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        // التلاوة بتفضل شغالة في الخلفية: بنفصل الشاشة بس
        RecitationPlayer.detach(this);
        DownloadService.setListener(null);
        super.onDestroy();
    }
}
