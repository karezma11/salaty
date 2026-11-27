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
        row("الأذان يعلى بالتدريج", Prefs.adhanFade(this) ? "مفعّل" : "متوقف", v -> {
            Prefs.setAdhanFade(this, !Prefs.adhanFade(this));
            render();
        });
        row("قلب الموبايل يوقف الأذان", Prefs.adhanFlip(this) ? "مفعّل" : "متوقف", v -> {
            Prefs.setAdhanFlip(this, !Prefs.adhanFlip(this));
            render();
        });
        row("تجربة الأذان", "اضغط للسماع · بيكمّل والشاشة مقفولة", v -> preview());
        // الأذان مابيشتغلش على موبايلات كتير لأن النظام بيوقّف التطبيق في الخلفية
        row("جرّب الأذان والشاشة مقفولة", "بعد دقيقة، بنفس طريق الأذان الحقيقي", v -> {
            AdhanCheck.testInOneMinute(this);
            new AlertDialog.Builder(this)
                    .setTitle("تجربة الأذان")
                    .setMessage("اقفل الشاشة وسيب الموبايل دقيقة.\n\nلو الأذان مااشتغلش، "
                            + "اسمح للتطبيق بالشغل في الخلفية والتشغيل التلقائي من هنا، "
                            + "وبعدين افتح «فحص الأذان» وابعت الصورة.")
                    .setPositiveButton("تمام", null)
                    .show();
        });
        row("السماح بالشغل في الخلفية", AdhanCheck.batteryFree(this)
                ? "مسموح ✓" : "مهم للأذان — اضغط للسماح", v -> AdhanCheck.askBattery(this));
        if (AdhanCheck.knownMaker()) {
            row("التشغيل التلقائي", "من إعدادات " + AdhanCheck.makerName()
                    + ": اسمح لصلاتي تشتغل لوحدها", v -> AdhanCheck.openAutostart(this));
        }
        row("فحص الأذان", "لو الأذان مابيشتغلش: اعرف السبب", v -> showAdhanReport());
        row("تنبيه قبل الأذان", preLabel(), v -> pickPre());
        row("وقت الإقامة", iqamaLabel(), v -> pickIqama());
        row("تعديل المواقيت يدويًا", offsetsLabel(), v -> pickPrayerForOffset());
        row("كتم الموبايل وقت الصلاة", silenceLabel(), v -> toggleSilence());
        row("شوف وضع رمضان دلوقتي", RamadanHeader.preview ? "شغّال — اضغط للإيقاف"
                : "معاينة شكل الرئيسية في رمضان", v -> {
            RamadanHeader.preview = !RamadanHeader.preview;
            if (RamadanHeader.preview) {
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } else {
                render();
            }
        });
        row("منبّه السحور في رمضان", suhoorLabel(), v -> pickSuhoor());
        row("إشعار الإفطار في رمضان", Prefs.iftarNotify(this) ? "مفعّل · بدعاء الإفطار" : "متوقف",
                v -> {
                    Prefs.setIftarNotify(this, !Prefs.iftarNotify(this));
                    Scheduler.scheduleExtras(this);
                    render();
                });
        row("تذكير سورة الكهف يوم الجمعة", kahfLabel(), v -> pickKahf());
        row("تنبيه قيام الليل", qiyamLabel(), v -> {
            Prefs.setQiyamAlarm(this, !Prefs.qiyamAlarm(this));
            Scheduler.scheduleExtras(this);
            render();
        });
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
        updateValue = row("فحص التحديثات", updateLabel(), v -> onUpdateClick())
                .findViewById(R.id.value);
        row("فحص الإشعار الدائم", "اضغط لمعرفة سبب عدم ظهوره", v ->
                new AlertDialog.Builder(this)
                        .setTitle("فحص الإشعار")
                        .setMessage(Notifications.diagnose(this))
                        .setPositiveButton("تمام", null)
                        .show());
    }

    private void showAdhanReport() {
        String report = AdhanCheck.report(this);
        new AlertDialog.Builder(this)
                .setTitle("فحص الأذان")
                .setMessage(report)
                .setPositiveButton("تمام", null)
                .setNeutralButton("نسخ", (d, w) -> {
                    android.content.ClipboardManager cm =
                            getSystemService(android.content.ClipboardManager.class);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("فحص الأذان", report));
                        Toast.makeText(this, "اتنسخ", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
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

    private View row(String label, String value, View.OnClickListener click) {
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
        return v;
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

    private String suhoorLabel() {
        int m = Prefs.suhoorMinutes(this);
        if (m <= 0) return "متوقف";
        return (m == 60 ? "ساعة" : m == 90 ? "ساعة ونص" : Ui.arabicDigits(m) + " دقيقة")
                + " قبل الإمساك";
    }

    private void pickSuhoor() {
        final int[] choices = {0, 30, 45, 60, 90};
        String[] labels = {"مقفول", "نص ساعة قبل الإمساك", "٤٥ دقيقة قبل الإمساك",
                "ساعة قبل الإمساك", "ساعة ونص قبل الإمساك"};
        new AlertDialog.Builder(this)
                .setTitle("منبّه السحور")
                .setItems(labels, (d, w) -> {
                    Prefs.setSuhoorMinutes(this, choices[w]);
                    Scheduler.scheduleExtras(this);
                    render();
                })
                .show();
    }

    private String kahfLabel() {
        int h = Prefs.kahfHour(this);
        if (h <= 0) return "متوقف";
        return "الساعة " + Ui.arabicDigits(h > 12 ? h - 12 : h) + (h < 12 ? " الصبح" : " الضهر");
    }

    private void pickKahf() {
        final int[] hours = {0, 7, 8, 9, 10, 11};
        String[] labels = new String[hours.length];
        for (int i = 0; i < hours.length; i++) {
            labels[i] = hours[i] == 0 ? "مقفول" : "الساعة " + Ui.arabicDigits(hours[i]) + " الصبح";
        }
        new AlertDialog.Builder(this)
                .setTitle("تذكير سورة الكهف")
                .setItems(labels, (d, w) -> {
                    Prefs.setKahfHour(this, hours[w]);
                    Scheduler.scheduleExtras(this);
                    render();
                })
                .show();
    }

    private String qiyamLabel() {
        String when = "";
        try {
            long at = Scheduler.qiyamMillis(this);
            if (at > 0) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(at);
                when = "الثلث الأخير " + PrayerTimes.format(cal.get(java.util.Calendar.HOUR_OF_DAY)
                        + cal.get(java.util.Calendar.MINUTE) / 60.0);
            }
        } catch (Exception ignored) {
        }
        return (Prefs.qiyamAlarm(this) ? "مفعّل" : "متوقف") + (when.isEmpty() ? "" : " · " + when);
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
        // لو إشعار انتهاء التنزيل ضاع، نتأكد بنفسنا (في الخلفية: فحص التوقيع تقيل)
        if (Updater.state(this) == Updater.DOWNLOADING) {
            android.content.Context app = getApplicationContext();
            new Thread(() -> Updater.refresh(app)).start();
        }
        ticker.removeCallbacks(tick);
        ticker.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ticker.removeCallbacks(tick);
    }

    // ===== فحص التحديثات =====

    private TextView updateValue;
    private final android.os.Handler ticker =
            new android.os.Handler(android.os.Looper.getMainLooper());

    /** بيحدّث نسبة التنزيل في السطر كل ثانيتين طول ما الصفحة مفتوحة. */
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (updateValue != null) updateValue.setText(updateLabel());
            if (Updater.state(SettingsActivity.this) == Updater.DOWNLOADING) {
                ticker.postDelayed(this, 2000);
            }
        }
    };

    private String updateLabel() {
        String v = Updater.version(this);
        switch (Updater.state(this)) {
            case Updater.DOWNLOADING: {
                int p = Updater.progress(this);
                return "جاري تنزيل " + v + (p >= 0 ? " — " + Ui.arabicDigits(p) + "٪" : "");
            }
            case Updater.READY:
                return "نسخة " + v + " جاهزة — اضغط للتثبيت";
            case Updater.OTHER_KEY:
                return "نسخة " + v + " محتاجة خطوة منك — اضغط";
            case Updater.FAILED:
                return "التنزيل ما كملش — اضغط للتفاصيل";
            default:
                return "اضغط عشان تشوف فيه نسخة أحدث (محتاج نت)";
        }
    }

    private void onUpdateClick() {
        switch (Updater.state(this)) {
            case Updater.DOWNLOADING:
                showDownloading();
                break;
            case Updater.READY:
                install();
                break;
            case Updater.OTHER_KEY:
                Updater.cancel(this);
                showOtherKey();
                render();
                break;
            case Updater.FAILED:
                new AlertDialog.Builder(this)
                        .setTitle("التنزيل ما كملش")
                        .setMessage(Updater.error(this))
                        .setPositiveButton("جرّب تاني", (d, w) -> {
                            Updater.cancel(this);
                            checkUpdate();
                        })
                        .setNegativeButton("إلغاء", (d, w) -> {
                            Updater.cancel(this);
                            render();
                        })
                        .show();
                break;
            default:
                checkUpdate();
        }
    }

    private void checkUpdate() {
        Toast.makeText(this, "بيدوّر على نسخة أحدث…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            Updater.Release found = null;
            String err = null;
            try {
                found = Updater.fetchLatest();
            } catch (Updater.Problem e) {
                err = e.getMessage();
            } catch (Exception e) {
                err = "تعذّر الاتصال بـ GitHub. اتأكد إن النت شغال وجرّب تاني.";
            }
            final Updater.Release r = found;
            final String error = err;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (error != null) {
                    new AlertDialog.Builder(this)
                            .setTitle("فحص التحديثات")
                            .setMessage(error)
                            .setPositiveButton("تمام", null)
                            .show();
                } else {
                    showRelease(r);
                }
            });
        }).start();
    }

    private void showRelease(Updater.Release r) {
        String installed = Updater.installedVersion(this);
        if (!Updater.isNewer(r.version, installed)) {
            new AlertDialog.Builder(this)
                    .setTitle("فحص التحديثات")
                    .setMessage("عندك آخر نسخة (" + installed + ").")
                    .setPositiveButton("تمام", null)
                    .show();
            return;
        }
        if (Updater.isDebugSigned(this)) {
            showOtherKey();
            return;
        }
        if (r.url == null) {
            new AlertDialog.Builder(this)
                    .setTitle("فيه نسخة جديدة: " + r.version)
                    .setMessage("النسخة منشورة بس من غير ملف تثبيت لسه. جرّب كمان شوية.")
                    .setPositiveButton("تمام", null)
                    .show();
            return;
        }
        StringBuilder msg = new StringBuilder();
        msg.append("عندك ").append(installed).append("، والجديدة ").append(r.version).append('.');
        if (r.size > 0) {
            msg.append("\nحجم الملف: ").append(Ui.arabicDigits(r.size / 1048576)).append(" ميجا");
        }
        if (!r.notes.isEmpty()) msg.append("\n\n").append(r.notes);
        msg.append("\n\nإعداداتك وعلاماتك وسجل صلواتك هيفضلوا زي ما هما.");
        new AlertDialog.Builder(this)
                .setTitle("فيه نسخة جديدة: " + r.version)
                .setMessage(msg.toString())
                .setPositiveButton("نزّل دلوقتي", (d, w) -> startDownload(r, false))
                .setNeutralButton("على الواي فاي بس", (d, w) -> startDownload(r, true))
                .setNegativeButton("مش دلوقتي", null)
                .show();
    }

    private void startDownload(Updater.Release r, boolean wifiOnly) {
        try {
            Updater.download(this, r, wifiOnly);
            Toast.makeText(this, wifiOnly
                    ? "هيتنزّل أول ما تتوصل بالواي فاي، وهيظهر إشعار لما يخلص"
                    : "بدأ التنزيل — هيظهر إشعار لما يخلص", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذّر بدء التنزيل", Toast.LENGTH_SHORT).show();
        }
        render();
        ticker.removeCallbacks(tick);
        ticker.post(tick);
    }

    private void showDownloading() {
        int p = Updater.progress(this);
        new AlertDialog.Builder(this)
                .setTitle("جاري تنزيل " + Updater.version(this))
                .setMessage((p >= 0 ? "خلص " + Ui.arabicDigits(p) + "٪. " : "")
                        + "تقدر تقفل التطبيق، التنزيل هيكمّل وهيظهر إشعار لما يخلص.")
                .setPositiveButton("تمام", null)
                .setNegativeButton("إلغاء التنزيل", (d, w) -> {
                    Updater.cancel(this);
                    render();
                })
                .show();
    }

    private void install() {
        java.io.File f = Updater.file(this);
        if (f == null || !f.exists()) {
            Updater.cancel(this);
            render();
            Toast.makeText(this, "ملف التحديث مش موجود، دوّر تاني", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Updater.needsInstallPermission(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("اسمح بتثبيت التحديث")
                    .setMessage("أندرويد محتاج تسمح لصلاتي تثبّت التحديث. هتفتحلك صفحة: فعّل "
                            + "«السماح من هذا المصدر» وارجع اضغط على السطر تاني.")
                    .setPositiveButton("افتح", (d, w) -> {
                        try {
                            startActivity(Updater.installPermissionIntent(this));
                        } catch (Exception e) {
                            Toast.makeText(this, "افتح إعدادات الموبايل ← التطبيقات ← صلاتي",
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
            return;
        }
        try {
            startActivity(Updater.installIntent(this, f));
        } catch (Exception e) {
            Toast.makeText(this, "تعذّر فتح المثبّت", Toast.LENGTH_SHORT).show();
        }
    }

    /** النسخة المثبّتة بمفتاح مختلف: لازم نسخة احتياطية ومسح وتثبيت من جديد. */
    private void showOtherKey() {
        new AlertDialog.Builder(this)
                .setTitle("التحديث محتاج خطوة منك")
                .setMessage("النسخة اللي على موبايلك موقّعة بمفتاح مختلف عن النسخة الرسمية "
                        + "(غالبًا نسخة تجريبية)، فأندرويد مش هيركّب الجديدة فوقها.\n\n"
                        + "١. اضغط «نسخة احتياطية» وابعت الملف لنفسك.\n"
                        + "٢. نزّل النسخة الجديدة من صفحة GitHub.\n"
                        + "٣. امسح صلاتي وثبّت الجديدة.\n"
                        + "٤. من الإعدادات ← استرجاع نسخة.\n\n"
                        + "ده بيحصل مرة واحدة بس، والتحديثات بعد كده هتتركّب من هنا عادي.")
                .setPositiveButton("نسخة احتياطية", (d, w) -> doExport())
                .setNeutralButton("صفحة التنزيل", (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Updater.PAGE)));
                    } catch (Exception e) {
                        Toast.makeText(this, Updater.PAGE, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
}
