package com.salah.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/** أساس كل الشاشات: حجم الخط العام وشريط التنقّل السفلي. */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.fontScale = Prefs.fontScale(base);
        super.attachBaseContext(base.createConfigurationContext(cfg));
    }

    /** يربط أزرار الشريط السفلي لو موجود في التخطيط. */
    protected void setupBottomNav(int activeId) {
        View bar = findViewById(R.id.bottomNav);
        if (bar == null) return;
        bind(R.id.navHome, MainActivity.class, activeId);
        bind(R.id.navQuran, QuranActivity.class, activeId);
        bind(R.id.navAdhkar, AdhkarActivity.class, activeId);
        bind(R.id.navMore, SettingsActivity.class, activeId);
    }

    private void bind(int viewId, Class<?> target, int activeId) {
        View v = findViewById(viewId);
        if (v == null) return;
        v.setSelected(viewId == activeId);
        v.setAlpha(viewId == activeId ? 1f : 0.55f);
        if (viewId == activeId) {
            v.setOnClickListener(null);
            return;
        }
        v.setOnClickListener(x -> {
            startActivity(new Intent(this, target)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            overridePendingTransition(0, 0);
        });
    }

    /** تمنع الشاشة من الإطفاء أثناء القراءة أو التشغيل. */
    protected void keepScreenOn(boolean on) {
        if (on) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
