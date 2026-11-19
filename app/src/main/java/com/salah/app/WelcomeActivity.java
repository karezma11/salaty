package com.salah.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/** شاشة أول تشغيل: المدينة والصوت والقارئ والأذونات. */
public class WelcomeActivity extends BaseActivity {

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("أهلاً بك في صلاتي");
        Ui.applyNaskh(title, true);
        ((TextView) findViewById(R.id.screenSub)).setText(
                "أربع خطوات سريعة وتبقى جاهز — كله يشتغل بدون إنترنت");
        list = findViewById(R.id.list);
        render();
    }

    private View row(String label, String value, View.OnClickListener click) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_setting, list, false);
        TextView l = v.findViewById(R.id.label);
        l.setText(label);
        Ui.applyNaskh(l, true);
        ((TextView) v.findViewById(R.id.value)).setText(value);
        if (click != null) v.setOnClickListener(click);
        list.addView(v);
        return v;
    }

    private boolean notificationsOn() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void render() {
        list.removeAllViews();

        row("١ · مدينتك", Prefs.city(this), v -> new AlertDialog.Builder(this)
                .setTitle("اختر المدينة")
                .setItems(Cities.NAMES, (d, w) -> {
                    Prefs.setLocation(this, Cities.NAMES[w],
                            Cities.COORDS[w][0], Cities.COORDS[w][1]);
                    Scheduler.scheduleAll(this);
                    render();
                })
                .show());

        row("٢ · صوت الأذان", Adhans.NAMES[Prefs.adhanVoice(this)],
                v -> new AlertDialog.Builder(this)
                        .setTitle("صوت الأذان")
                        .setItems(Adhans.NAMES, (d, w) -> {
                            Prefs.setAdhanVoice(this, w);
                            render();
                        })
                        .show());

        row("٣ · القارئ", Reciters.current(this).name,
                v -> new AlertDialog.Builder(this)
                        .setTitle("اختر القارئ")
                        .setItems(Reciters.names(), (d, w) -> {
                            Prefs.setReciter(this, Reciters.ALL[w].id);
                            render();
                        })
                        .show());

        row("٤ · الإشعارات", notificationsOn() ? "مفعّلة ✓" : "اضغط للتفعيل", v -> {
            if (Build.VERSION.SDK_INT >= 33 && !notificationsOn()) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
            } else {
                render();
            }
        });

        if (!Scheduler.canExact(this)) {
            row("التنبيهات الدقيقة", "مطلوبة عشان الأذان ييجي في وقته", v -> {
                try {
                    startActivity(new Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            android.net.Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {
                }
            });
        }

        View go = row("يلا نبدأ", "تقدر تغيّر أي حاجة من الإعدادات بعدين", v -> finishSetup());
        go.setContentDescription("ابدأ");
    }

    private void finishSetup() {
        Prefs.setSeenWelcome(this, true);
        Scheduler.scheduleAll(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }
}
