package com.salah.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/** الإعدادات: المدينة، طريقة الحساب، المذهب، الأذان، وتذكير الصلاة على النبي. */
public class SettingsActivity extends BaseActivity {

    private static final int[] SALAWAT_CHOICES = {0, 60, 120, 180, 240, 360, 720};

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("الإعدادات");
        Ui.applyNaskh(title, true);
        ((TextView) findViewById(R.id.screenSub)).setText("كل الحسابات بتتم على الموبايل بدون إنترنت");
        list = findViewById(R.id.list);
        render();
    }

    private void render() {
        list.removeAllViews();

        row("المدينة", Prefs.city(this), v -> pickCity());
        row("طريقة الحساب", PrayerTimes.METHOD_NAMES[Prefs.method(this)], v -> pickMethod());
        row("حساب العصر", Prefs.asrFactor(this) == 2 ? "الحنفي" : "الشافعي والمالكي والحنبلي",
                v -> pickAsr());
        row("الأذان عند الوقت", Prefs.adhanEnabled(this) ? "مفعّل" : "متوقف", v -> {
            Prefs.setAdhanEnabled(this, !Prefs.adhanEnabled(this));
            Scheduler.scheduleAll(this);
            render();
        });
        row("صوت الأذان", Adhans.NAMES[Prefs.adhanVoice(this)], v -> pickVoice());
        row("أذان الفجر الخاص", Prefs.fajrAdhan(this) ? "مفعّل" : "متوقف", v -> {
            Prefs.setFajrAdhan(this, !Prefs.fajrAdhan(this));
            render();
        });
        row("تجربة الأذان", "اضغط للسماع · بيكمّل والشاشة مقفولة", v -> preview());
        row("تنبيه قبل الأذان", preLabel(), v -> pickPre());
        row("وقت الإقامة", iqamaLabel(), v -> pickIqama());
        row("تعديل المواقيت يدويًا", offsetsLabel(), v -> pickPrayerForOffset());
        row("كتم الموبايل وقت الصلاة", silenceLabel(), v -> toggleSilence());
        row("تذكير الصلاة على النبي", salawatLabel(), v -> pickSalawat());
        row("تعديل التقويم الهجري", hijriLabel(), v -> pickHijri());
        row("تنبيه الصيام المستحب", Prefs.fastingReminder(this) ? "مفعّل" : "متوقف", v -> {
            Prefs.setFastingReminder(this, !Prefs.fastingReminder(this));
            Scheduler.scheduleAll(this);
            render();
        });
        row("حجم الخط", fontLabel(), v -> pickFont());
        row("الشاشة ما تطفيش وانت بتقرا", Prefs.keepScreenOn(this) ? "مفعّل" : "متوقف", v -> {
            Prefs.setKeepScreenOn(this, !Prefs.keepScreenOn(this));
            render();
        });
        row("تنبيه أذكار الصباح والمساء", Prefs.adhkarReminder(this) ? "مفعّل" : "متوقف", v -> {
            Prefs.setAdhkarReminder(this, !Prefs.adhkarReminder(this));
            Scheduler.scheduleAdhkar(this);
            render();
        });
        row("المظهر", themeLabel(), v -> pickTheme());
        row("إشعار دائم بالمواقيت", Prefs.persistentTimes(this) ? "مفعّل" : "متوقف", v -> {
            Prefs.setPersistentTimes(this, !Prefs.persistentTimes(this));
            Notifications.updateTimes(this);
            render();
        });
        row("تحديد موقعي بالـ GPS", "اضغط للتحديد", v -> {
            startActivity(new Intent(this, MainActivity.class)
                    .putExtra("gps", true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });
        if (!Scheduler.canExact(this)) {
            row("تفعيل التنبيهات الدقيقة", "مطلوب للأذان في وقته", v -> openExactAlarmSettings());
        }
        row("القارئ", Reciters.current(this).name, v -> pickReciter());
        row("إدارة التحميلات", Ui.arabicDigits(Recitation.count(this)) + " من ١١٤ سورة",
                v -> startActivity(new Intent(this, DownloadsActivity.class)));
        row("العلامات والمفضلة",
                Ui.arabicDigits(Bookmarks.count(this) + Bookmarks.favCount(this)) + " عنصر",
                v -> startActivity(new Intent(this, BookmarksActivity.class)));
        row("نسخة احتياطية", "احفظ إعداداتك وعلاماتك في ملف", v -> doExport());
        row("استرجاع نسخة", "اختر ملف نسخة احتياطية", v -> pickRestore());
        row("مجلد التلاوة", Recitation.path(this), null);
        row("نص المصحف", "مصحف تنزيل (tanzil.net) — نسخة حرفية", null);
        row("كتاب التفسير", Tafsir.current(this).name, v -> new AlertDialog.Builder(this)
                .setTitle("اختر التفسير")
                .setItems(Tafsir.names(), (d, w) -> {
                    Prefs.setTafsirBook(this, Tafsir.BOOKS[w].id);
                    Tafsir.invalidate();
                    render();
                })
                .show());
        row("مصادر التفسير", Tafsir.SOURCE, null);
        row("الخط", "خط أميري — رخصة SIL OFL", null);
        row("أصوات الأذان", "من مجموعة Athan-MP3 المفتوحة على GitHub", null);
        row("الابتهالات والتواشيح", "من أرشيف إنترنت — Public Domain Mark 1.0", null);
        row("إصدار التطبيق", versionLabel(), null);
        row("فحص الإشعار الدائم", "اضغط لمعرفة سبب عدم ظهوره", v ->
                new AlertDialog.Builder(this)
                        .setTitle("فحص الإشعار")
                        .setMessage(Notifications.diagnose(this))
                        .setPositiveButton("تمام", null)
                        .show());
    }

    /** رقم النسخة المثبّتة — بيفرق لما تتأكد إن التحديث وصل فعلًا. */
    private String versionLabel() {
        try {
            android.content.pm.PackageInfo info =
                    getPackageManager().getPackageInfo(getPackageName(), 0);
            return "صلاتي " + info.versionName
                    + "  (" + Ui.arabicDigits(info.versionCode) + ")";
        } catch (Exception e) {
            return "غير معروف";
        }
    }

    private String salawatLabel() {
        int m = Prefs.salawatMinutes(this);
        if (m <= 0) return "متوقف";
        if (m < 60) return "كل " + Ui.arabicDigits(m) + " دقيقة";
        return "كل " + Ui.arabicDigits(m / 60) + " ساعة";
    }

    private void row(String label, String value, View.OnClickListener click) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_setting, list, false);
        TextView l = v.findViewById(R.id.label);
        l.setText(label);
        Ui.applyNaskh(l, true);
        ((TextView) v.findViewById(R.id.value)).setText(value);
        if (click != null) {
            v.setOnClickListener(click);
        } else {
            v.setClickable(false);
        }
        list.addView(v);
    }

    private void pickCity() {
        new AlertDialog.Builder(this)
                .setTitle("اختر المدينة")
                .setItems(Cities.NAMES, (d, which) -> {
                    Prefs.setLocation(this, Cities.NAMES[which],
                            Cities.COORDS[which][0], Cities.COORDS[which][1]);
                    Scheduler.scheduleAll(this);
                    render();
                })
                .show();
    }

    private void pickMethod() {
        new AlertDialog.Builder(this)
                .setTitle("طريقة الحساب")
                .setItems(PrayerTimes.METHOD_NAMES, (d, which) -> {
                    Prefs.setMethod(this, which);
                    Scheduler.scheduleAll(this);
                    render();
                })
                .show();
    }

    private void pickAsr() {
        String[] options = {"الشافعي والمالكي والحنبلي", "الحنفي"};
        new AlertDialog.Builder(this)
                .setTitle("حساب العصر")
                .setItems(options, (d, which) -> {
                    Prefs.setAsrFactor(this, which == 0 ? 1 : 2);
                    Scheduler.scheduleAll(this);
                    render();
                })
                .show();
    }

    private String iqamaLabel() {
        int m = Prefs.iqamaMinutes(this);
        return m <= 0 ? "متوقف" : "بعد الأذان بـ " + Ui.arabicDigits(m) + " دقيقة";
    }

    private String themeLabel() {
        switch (Prefs.theme(this)) {
            case 1:
                return "فاتح";
            case 2:
                return "غامق";
            default:
                return "حسب النظام";
        }
    }

    private String offsetsLabel() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int o = Prefs.offset(this, i);
            if (o == 0) continue;
            if (b.length() > 0) b.append("، ");
            b.append(PrayerTimes.NAMES[i]).append(' ')
                    .append(o > 0 ? "+" : "-").append(Ui.arabicDigits(Math.abs(o)));
        }
        return b.length() == 0 ? "بدون تعديل" : b.toString();
    }

    private String preLabel() {
        int m = Prefs.preAdhanMinutes(this);
        return m <= 0 ? "متوقف" : "قبل الأذان بـ " + Ui.arabicDigits(m) + " دقيقة";
    }

    private void pickPre() {
        final int[] choices = {0, 5, 10, 15, 20, 30};
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            labels[i] = choices[i] == 0 ? "متوقف"
                    : "قبل الأذان بـ " + Ui.arabicDigits(choices[i]) + " دقيقة";
        }
        new AlertDialog.Builder(this)
                .setTitle("تنبيه قبل الأذان")
                .setItems(labels, (d, w) -> {
                    Prefs.setPreAdhanMinutes(this, choices[w]);
                    Scheduler.schedulePreAdhan(this);
                    render();
                })
                .show();
    }

    private void pickIqama() {
        final int[] choices = {0, 5, 10, 15, 20, 25, 30};
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            labels[i] = choices[i] == 0 ? "متوقف"
                    : "بعد الأذان بـ " + Ui.arabicDigits(choices[i]) + " دقيقة";
        }
        new AlertDialog.Builder(this)
                .setTitle("وقت الإقامة")
                .setItems(labels, (d, which) -> {
                    Prefs.setIqamaMinutes(this, choices[which]);
                    render();
                })
                .show();
    }

    private void pickPrayerForOffset() {
        String[] labels = new String[6];
        for (int i = 0; i < 6; i++) {
            int o = Prefs.offset(this, i);
            labels[i] = PrayerTimes.NAMES[i] + (o == 0 ? ""
                    : "  (" + (o > 0 ? "+" : "-") + Ui.arabicDigits(Math.abs(o)) + " دقيقة)");
        }
        new AlertDialog.Builder(this)
                .setTitle("اختر الصلاة")
                .setItems(labels, (d, which) -> pickOffset(which))
                .show();
    }

    private void pickOffset(int prayer) {
        final int[] choices = {-10, -8, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 8, 10};
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            int v = choices[i];
            labels[i] = v == 0 ? "بدون تعديل"
                    : (v > 0 ? "متأخر " : "مبكر ") + Ui.arabicDigits(Math.abs(v)) + " دقيقة";
        }
        new AlertDialog.Builder(this)
                .setTitle("تعديل " + PrayerTimes.NAMES[prayer])
                .setItems(labels, (d, which) -> {
                    Prefs.setOffset(this, prayer, choices[which]);
                    Scheduler.scheduleAll(this);
                    render();
                })
                .show();
    }

    private String hijriLabel() {
        int o = Prefs.hijriOffset(this);
        String today = Hijri.today(this).text();
        if (o == 0) return today;
        return today + " (" + (o > 0 ? "+" : "-") + Ui.arabicDigits(Math.abs(o)) + ")";
    }

    private void pickHijri() {
        final int[] choices = {-2, -1, 0, 1, 2};
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            int o = choices[i];
            labels[i] = Hijri.of(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
                    java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH), o).text()
                    + (o == 0 ? "  (الافتراضي)" : "");
        }
        new AlertDialog.Builder(this)
                .setTitle("تعديل التقويم الهجري")
                .setItems(labels, (d, w) -> {
                    Prefs.setHijriOffset(this, choices[w]);
                    render();
                })
                .show();
    }

    private String fontLabel() {
        float f = Prefs.fontScale(this);
        if (f <= 1.0f) return "عادي";
        if (f <= 1.15f) return "كبير";
        if (f <= 1.3f) return "أكبر";
        return "الأكبر";
    }

    private void pickFont() {
        final float[] choices = {1.0f, 1.15f, 1.3f, 1.45f};
        String[] labels = {"عادي", "كبير", "أكبر", "الأكبر"};
        new AlertDialog.Builder(this)
                .setTitle("حجم خط التطبيق")
                .setItems(labels, (d, w) -> {
                    Prefs.setFontScale(this, choices[w]);
                    recreate();
                })
                .show();
    }

    private static final int REQ_RESTORE = 300;

    private void doExport() {
        try {
            java.io.File f = Backup.export(this);
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".files", f);
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "حفظ النسخة الاحتياطية"));
        } catch (Exception e) {
            Toast.makeText(this, "تعذّر عمل النسخة", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickRestore() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*"), REQ_RESTORE);
        } catch (Exception e) {
            Toast.makeText(this, "مفيش مدير ملفات", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_RESTORE || resultCode != RESULT_OK || data == null
                || data.getData() == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("استرجاع نسخة")
                .setMessage("هيتم استبدال إعداداتك وعلاماتك الحالية باللي في الملف. متأكد؟")
                .setPositiveButton("استرجع", (d, w) -> {
                    try (java.io.InputStream in =
                                 getContentResolver().openInputStream(data.getData())) {
                        int n = Backup.restore(this, in);
                        Tafsir.invalidate();
                        Recitation.invalidate();
                        App.applyTheme(this);
                        Scheduler.scheduleAll(this);
                        Toast.makeText(this, "تم استرجاع " + Ui.arabicDigits(n) + " إعداد",
                                Toast.LENGTH_LONG).show();
                        recreate();
                    } catch (Exception e) {
                        Toast.makeText(this, "الملف مش سليم", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void pickTheme() {
        String[] labels = {"حسب النظام", "فاتح", "غامق"};
        new AlertDialog.Builder(this)
                .setTitle("المظهر")
                .setItems(labels, (d, which) -> {
                    Prefs.setTheme(this, which);
                    App.applyTheme(this);
                    recreate();
                })
                .show();
    }

    private void pickReciter() {
        new AlertDialog.Builder(this)
                .setTitle("اختر القارئ")
                .setItems(Reciters.names(), (d, w) -> {
                    String id = Reciters.ALL[w].id;
                    if (id.equals(Prefs.reciter(this))) return;
                    Prefs.setReciter(this, id);
                    Recitation.invalidate();
                    Toast.makeText(this,
                            "اتغيّر القارئ — السور هتتحمّل بصوته أول مرة تشغّلها",
                            Toast.LENGTH_LONG).show();
                    render();
                })
                .show();
    }

    private void pickVoice() {
        new AlertDialog.Builder(this)
                .setTitle("صوت الأذان")
                .setItems(Adhans.NAMES, (d, which) -> {
                    Prefs.setAdhanVoice(this, which);
                    render();
                })
                .show();
    }

    private void preview() {
        String id = "adhan-" + Prefs.adhanVoice(this);
        if (id.equals(Audio.currentId())) {
            Audio.stop();
            return;
        }
        boolean ok = Audio.playAsset(this, Adhans.file(Prefs.adhanVoice(this)), id,
                "تجربة الأذان", Adhans.NAMES[Prefs.adhanVoice(this)]);
        if (!ok) {
            Toast.makeText(this, "تعذّر تشغيل الأذان", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "اضغط مرة تانية للإيقاف", Toast.LENGTH_SHORT).show();
        }
    }

    private String silenceLabel() {
        if (!Prefs.silencePrayer(this)) return "متوقف";
        if (!Silence.allowed(this)) return "محتاج إذن — اضغط للسماح";
        return "مفعّل · " + Ui.arabicDigits(Prefs.silenceMinutes(this)) + " دقيقة";
    }

    private void toggleSilence() {
        if (Prefs.silencePrayer(this) && Silence.allowed(this)) {
            // مفعّل: نعرض المدة أو نقفله
            final int[] choices = {0, 10, 15, 20, 30};
            String[] labels = new String[choices.length];
            for (int i = 0; i < choices.length; i++) {
                labels[i] = choices[i] == 0 ? "اقفل الكتم"
                        : Ui.arabicDigits(choices[i]) + " دقيقة";
            }
            new AlertDialog.Builder(this)
                    .setTitle("مدة الكتم بعد الأذان")
                    .setItems(labels, (d, w) -> {
                        if (choices[w] == 0) {
                            Prefs.setSilencePrayer(this, false);
                            Silence.stop(this);
                        } else {
                            Prefs.setSilenceMinutes(this, choices[w]);
                        }
                        render();
                    })
                    .show();
            return;
        }
        if (!Silence.allowed(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("إذن مطلوب")
                    .setMessage("عشان التطبيق يقدر يكتم الموبايل وقت الصلاة، لازم تسمحله "
                            + "بالتحكم في وضع \u201cعدم الإزعاج\u201d من إعدادات النظام.\n\n"
                            + "التطبيق بيكتم عند الأذان بس، وبيرجّع الصوت لوحده بعد المدة اللي تحددها.")
                    .setPositiveButton("افتح الإعدادات", (d, w) -> {
                        Prefs.setSilencePrayer(this, true);
                        Silence.askPermission(this);
                    })
                    .setNegativeButton("مش دلوقتي", null)
                    .show();
            return;
        }
        Prefs.setSilencePrayer(this, true);
        render();
    }

    private void pickSalawat() {
        String[] labels = new String[SALAWAT_CHOICES.length];
        for (int i = 0; i < SALAWAT_CHOICES.length; i++) {
            int m = SALAWAT_CHOICES[i];
            labels[i] = m == 0 ? "متوقف"
                    : m < 60 ? "كل " + Ui.arabicDigits(m) + " دقيقة"
                    : "كل " + Ui.arabicDigits(m / 60) + " ساعة";
        }
        new AlertDialog.Builder(this)
                .setTitle("تذكير الصلاة على النبي ﷺ")
                .setItems(labels, (d, which) -> {
                    Prefs.setSalawatMinutes(this, SALAWAT_CHOICES[which]);
                    Scheduler.scheduleSalawat(this);
                    render();
                })
                .show();
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
                return;
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(this, "افتح إعدادات التطبيق وفعّل التنبيهات والمنبهات", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }
}
