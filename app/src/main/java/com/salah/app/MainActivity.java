package com.salah.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** الشاشة الرئيسية: الصلاة القادمة، مواقيت اليوم، والأقسام. */
public class MainActivity extends BaseActivity {

    private static final int REQ_LOCATION = 100;
    private static final int REQ_NOTIF = 101;

    private TextView cityView, dateView, nextName, nextTime, countdown, warning, hint;
    private LinearLayout timesRow;
    private final TextView[] timeViews = new TextView[6];
    private final View[] markers = new View[6];
    private final View[] rows = new View[6];

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notifications.ensureChannels(this);
        if (!Prefs.seenWelcome(this)) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        cityView = findViewById(R.id.cityView);
        dateView = findViewById(R.id.dateView);
        nextName = findViewById(R.id.nextName);
        nextTime = findViewById(R.id.nextTime);
        countdown = findViewById(R.id.countdown);
        warning = findViewById(R.id.warning);
        hint = findViewById(R.id.hint);
        timesRow = findViewById(R.id.timesRow);

        Ui.applyNaskh(cityView, true);
        Ui.applyNaskh(nextName, true);

        findViewById(R.id.settingsBtn).setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class)));

        setupBottomNav(R.id.navHome);
        buildTimesRow();
        buildActions();
        CrashLog.showIfAny(this);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
        if (getIntent().getBooleanExtra("gps", false)) {
            useGps(true);
        } else {
            askLocationIfNeeded();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        ticker.run();
        Scheduler.scheduleAll(this);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    // ---------- بناء العناصر ----------

    private void buildTimesRow() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < 6; i++) {
            View item = inflater.inflate(R.layout.item_prayer, timesRow, false);
            TextView name = item.findViewById(R.id.name);
            name.setText(PrayerTimes.NAMES[i]);
            Ui.applyNaskh(name, false);
            timeViews[i] = item.findViewById(R.id.time);
            markers[i] = item.findViewById(R.id.marker);
            rows[i] = item;
            timesRow.addView(item);
        }
    }

    private void buildActions() {
        GridLayout grid = findViewById(R.id.actionsGrid);
        addAction(grid, R.drawable.ic_quran, "القرآن الكريم", "قراءة وتلاوة",
                v -> startActivity(new Intent(this, QuranActivity.class)));
        addAction(grid, R.drawable.ic_adhkar, "الأذكار", "الصباح والمساء",
                v -> startActivity(new Intent(this, AdhkarActivity.class)));
        addAction(grid, R.drawable.ic_ibtihal, "ابتهالات وتواشيح", "استماع بدون نت",
                v -> startActivity(new Intent(this, IbtihalatActivity.class)));
        addAction(grid, R.drawable.ic_radio, "إذاعات القرآن", "بث مباشر",
                v -> startActivity(new Intent(this, RadioActivity.class)));
        addAction(grid, R.drawable.ic_qibla, "القبلة", "بوصلة الاتجاه",
                v -> startActivity(new Intent(this, QiblaActivity.class)));
        addAction(grid, R.drawable.ic_tasbih, "المسبحة", "عدّاد التسبيح",
                v -> startActivity(new Intent(this, TasbihActivity.class)));
        addAction(grid, R.drawable.ic_wird, "الورد والختمة", "متابعة يومية",
                v -> startActivity(new Intent(this, WirdActivity.class)));
        addAction(grid, R.drawable.ic_log, "سجل الصلوات", "تابع صلواتك",
                v -> startActivity(new Intent(this, PrayerLogActivity.class)));
        addAction(grid, R.drawable.ic_settings, "الإعدادات", "المدينة والأذان",
                v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void addAction(GridLayout grid, int icon, String label, String sub,
                           View.OnClickListener click) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_action, grid, false);
        ((ImageView) card.findViewById(R.id.icon)).setImageResource(icon);
        TextView title = card.findViewById(R.id.label);
        title.setText(label);
        Ui.applyNaskh(title, true);
        ((TextView) card.findViewById(R.id.sub)).setText(sub);
        card.setOnClickListener(click);
        // لازم ناخد المقاسات من الـ XML، وإلا الكروت بتلزق في بعضها
        ViewGroup.MarginLayoutParams from = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = from.height;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(from.leftMargin, from.topMargin, from.rightMargin, from.bottomMargin);
        lp.setMarginStart(from.leftMargin);
        lp.setMarginEnd(from.rightMargin);
        card.setLayoutParams(lp);
        grid.addView(card);
    }

    // ---------- التحديث ----------

    private void refresh() {
        cityView.setText(Prefs.city(this));
        String greg = new SimpleDateFormat("EEEE d MMMM", new Locale("ar")).format(new Date());
        dateView.setText(greg + "  ·  " + Hijri.today(this).text());

        double[] t = Scheduler.timesFor(this, 0);
        long[] next = Scheduler.nextPrayer(this);
        int nextIdx = (int) next[0];

        for (int i = 0; i < 6; i++) {
            timeViews[i].setText(PrayerTimes.format(t[i]));
            boolean active = i == nextIdx;
            markers[i].setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            View row = rows[i];
            int pl = row.getPaddingLeft(), pt = row.getPaddingTop();
            int pr = row.getPaddingRight(), pb = row.getPaddingBottom();
            row.setBackgroundResource(active ? R.drawable.row_active : 0);
            row.setPadding(pl, pt, pr, pb);
        }

        nextName.setText(PrayerTimes.NAMES[nextIdx]);
        nextTime.setText(PrayerTimes.format(t[nextIdx]));

        long left = Math.max(0, (next[1] - System.currentTimeMillis()) / 1000L);
        long h = left / 3600, m = (left % 3600) / 60, s = left % 60;
        countdown.setText(h > 0
                ? "باقي " + h + " ساعة و " + m + " دقيقة"
                : "باقي " + m + " دقيقة و " + s + " ثانية");

        String fasting = Hijri.fastingReason(this);
        int toRamadan = Hijri.daysToRamadan(this);
        if (toRamadan >= 0 && toRamadan <= 30) {
            hint.setVisibility(View.VISIBLE);
            hint.setText(toRamadan == 0
                    ? "النهاردة أول رمضان — كل سنة وانت طيب"
                    : "باقي " + Ui.arabicDigits(toRamadan) + " يوم على رمضان");
        } else if (fasting != null) {
            hint.setVisibility(View.VISIBLE);
            hint.setText("صيام مستحب النهاردة: " + fasting);
        } else {
            hint.setVisibility(View.GONE);
        }

        if (!Scheduler.canExact(this)) {
            warning.setVisibility(View.VISIBLE);
            warning.setText("التنبيهات الدقيقة غير مفعّلة، والأذان قد يتأخر. افتح الإعدادات لتفعيلها.");
            warning.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        } else if (Prefs.adhanEnabled(this) && !AdhanCheck.batteryFree(this)) {
            // من غير كده موبايلات كتير بتوقف التطبيق في الخلفية والأذان مابيشتغلش
            warning.setVisibility(View.VISIBLE);
            warning.setText("عشان الأذان يشتغل والموبايل مقفول: اضغط هنا واسمح لصلاتي تشتغل في الخلفية");
            warning.setOnClickListener(v -> AdhanCheck.askBattery(this));
        } else {
            warning.setVisibility(View.GONE);
        }
    }

    // ---------- الموقع ----------

    private void askLocationIfNeeded() {
        if (Prefs.hasLocation(this)) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        } else {
            useGps(false);
        }
    }

    /** يحدّث الموقع من الـ GPS. */
    void useGps(boolean loud) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;
        try {
            Location best = null;
            List<String> providers = lm.getProviders(true);
            for (String p : providers) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            if (best != null) {
                applyLocation(best);
                return;
            }
            String provider = providers.contains(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER
                    : providers.contains(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : null;
            if (provider == null) {
                if (loud) {
                    Toast.makeText(this, "شغّل الموقع أو اختر مدينة من الإعدادات", Toast.LENGTH_LONG).show();
                }
                return;
            }
            if (loud) Toast.makeText(this, "جاري تحديد الموقع...", Toast.LENGTH_SHORT).show();
            lm.requestSingleUpdate(provider, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    applyLocation(location);
                }

                @Override
                public void onStatusChanged(String p, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String p) {
                }

                @Override
                public void onProviderDisabled(String p) {
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            if (loud) Toast.makeText(this, "مفيش إذن للموقع", Toast.LENGTH_LONG).show();
        }
    }

    private void applyLocation(Location l) {
        Prefs.setLocation(this, "موقعي الحالي", l.getLatitude(), l.getLongitude());
        Scheduler.scheduleAdhan(this);
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) {
                    useGps(false);
                    return;
                }
            }
        }
    }
}
