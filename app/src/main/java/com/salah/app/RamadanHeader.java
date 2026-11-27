package com.salah.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * وضع رمضان (الشكل «ط» اللي اختاره المستخدم): الرأس الأخضر في الرئيسية بيتزيّن في رمضان،
 * حبل فوانيس ونجوم فوق، وفانوس بيتملى نور مع يوم الصيام وجنبه العدّ للإفطار (أو للإمساك
 * بالليل)، وتحته الإمساك والإفطار. بعد رمضان الرأس بيرجع زي ما كان.
 */
final class RamadanHeader {

    private static final int GOLD = 0xFFE2B04A;
    private static final int GOLD_DARK = 0xFFB88A2E;
    private static final int GOLD_LIGHT = 0xFFFFE7A0;
    private static final int CORAL = 0xFFD9734E;
    private static final int MINT = 0xFFCFEBDD;

    private final LinearLayout hero;
    private final View[] normal;
    private final Drawable normalBg;
    private final int normalTop;
    private final View garland;
    private final LinearLayout block;
    private final LanternView lantern;
    private final TextView label, count, sub, pair;
    private boolean shown;
    /** «شوف وضع رمضان دلوقتي» من الإعدادات: الرأس بس، من غير منبّهات. */
    static boolean preview;

    /** normal: عناصر الرأس العادية اللي بتستخبى في رمضان (الصلاة القادمة والعدّ). */
    RamadanHeader(Context c, LinearLayout hero, View... normal) {
        this.hero = hero;
        this.normal = normal;
        normalBg = hero.getBackground();
        normalTop = hero.getPaddingTop();
        float d = c.getResources().getDisplayMetrics().density;

        garland = new GarlandView(c);
        garland.setVisibility(View.GONE);
        hero.addView(garland, 0, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(46 * d)));
        LinearLayout.LayoutParams glp = (LinearLayout.LayoutParams) garland.getLayoutParams();
        glp.topMargin = -normalTop;
        glp.setMarginStart(-hero.getPaddingStart());
        glp.setMarginEnd(-hero.getPaddingEnd());

        block = new LinearLayout(c);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setVisibility(View.GONE);

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        lantern = new LanternView(c);
        row.addView(lantern, new LinearLayout.LayoutParams(Math.round(58 * d), Math.round(84 * d)));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.setMarginStart(Math.round(14 * d));
        row.addView(col, clp);

        label = text(c, 13, 0xFFBFE3D2, false);
        count = text(c, 40, GOLD_LIGHT, true);
        count.setFontFeatureSettings("tnum");
        sub = text(c, 12, MINT, false);
        col.addView(label);
        col.addView(count);
        col.addView(sub);
        block.addView(row);

        pair = text(c, 15, Color.WHITE, false);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = Math.round(8 * d);
        block.addView(pair, plp);

        // بعد صف المدينة والتاريخ
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Math.round(14 * d);
        hero.addView(block, 2, blp);
    }

    private static TextView text(Context c, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    /** كل ثانية من الرئيسية. */
    void update(Context c) {
        double[] t = Scheduler.timesFor(c, 0);
        java.util.Calendar now = java.util.Calendar.getInstance();
        double hours = now.get(java.util.Calendar.HOUR_OF_DAY)
                + now.get(java.util.Calendar.MINUTE) / 60.0
                + now.get(java.util.Calendar.SECOND) / 3600.0;
        double imsak = Ramadan.imsak(t[PrayerTimes.FAJR]);
        double maghrib = t[PrayerTimes.MAGHRIB];
        boolean afterIftar = hours >= maghrib;
        // بالنهار: رمضان النهارده. بعد المغرب: لو بكرة رمضان (أول ليلة سحور، ومش ليلة العيد)
        boolean on = preview || (afterIftar ? Ramadan.on(c, 1) : Ramadan.on(c, 0));
        show(on);
        if (!on) return;

        boolean fasting = hours >= imsak && !afterIftar;
        double target;
        if (fasting) {
            target = maghrib;
        } else if (afterIftar) {
            target = Ramadan.imsak(Scheduler.timesFor(c, 1)[PrayerTimes.FAJR]) + 24;
        } else {
            target = imsak;
        }
        long left = Math.max(0, Math.round((target - hours) * 3600));
        label.setText(fasting ? "باقي على الإفطار" : "باقي على الإمساك");
        count.setText(Ui.arabicDigits(left / 3600) + ":" + two(left / 60 % 60) + ":" + two(left % 60));

        double p = fasting ? Ramadan.fastProgress(imsak, maghrib, hours) : 1;
        lantern.setProgress((float) p);
        int day = preview && Ramadan.day(c) == 0 ? 12 : Ramadan.day(c);
        sub.setText(fasting
                ? "اليوم " + Ui.arabicDigits(day) + " · الفانوس بيتملى نور مع اليوم "
                        + Ui.arabicDigits(Math.round(p * 100)) + "٪"
                : afterIftar ? "تقبّل الله صيامكم" : "وقت السحور");
        pair.setText("الإمساك " + PrayerTimes.format(imsak)
                + "     الإفطار " + PrayerTimes.format(maghrib));
    }

    private static String two(long n) {
        return (n < 10 ? "٠" : "") + Ui.arabicDigits(n);
    }

    private void show(boolean on) {
        if (on == shown) return;
        shown = on;
        for (View v : normal) v.setVisibility(on ? View.GONE : View.VISIBLE);
        garland.setVisibility(on ? View.VISIBLE : View.GONE);
        block.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) {
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF07402A, 0xFF0E1B3D});
            float r = 28 * hero.getResources().getDisplayMetrics().density;
            bg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
            hero.setBackground(bg);
        } else {
            hero.setBackground(normalBg);
        }
    }

    /** شكل الفانوس، مرسوم في مربع ٤٠×٥٢ وبيتكبّر على مقاس الـ View. */
    private static Path lanternBody() {
        Path p = new Path();
        p.moveTo(9, 12);
        p.lineTo(31, 12);
        p.lineTo(36, 28);
        p.lineTo(31, 46);
        p.lineTo(9, 46);
        p.lineTo(4, 28);
        p.close();
        return p;
    }

    /** الفانوس اللي بيتملى نور من تحت لفوق مع يوم الصيام. */
    static final class LanternView extends View {
        private float progress;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path body = lanternBody();

        LanternView(Context c) {
            super(c);
        }

        void setProgress(float p) {
            if (Math.abs(p - progress) < 0.002f) return;
            progress = p;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas cv) {
            float s = Math.min(getWidth() / 40f, getHeight() / 58f);
            cv.save();
            cv.translate((getWidth() - 40 * s) / 2f, 0);
            cv.scale(s, s);
            if (progress >= 0.999f) {
                paint.setShader(new RadialGradient(20, 29, 26, 0x99FFE7A0, 0x00FFE7A0,
                        Shader.TileMode.CLAMP));
                cv.drawCircle(20, 29, 26, paint);
                paint.setShader(null);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(GOLD_DARK);
            cv.drawRect(19.2f, 0, 20.8f, 6, paint);
            Path cap = new Path();
            cap.moveTo(10, 6);
            cap.lineTo(30, 6);
            cap.lineTo(27, 12);
            cap.lineTo(13, 12);
            cap.close();
            cv.drawPath(cap, paint);
            // الجسم: غامق، وبعدين النور لحد النسبة
            paint.setColor(0xFF3A4A3F);
            cv.drawPath(body, paint);
            cv.save();
            cv.clipPath(body);
            paint.setColor(0xFFF3D27A);
            cv.drawRect(0, 46 - 34 * progress, 40, 46, paint);
            cv.restore();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(GOLD_DARK);
            cv.drawPath(body, paint);
            paint.setStrokeWidth(1f);
            cv.drawLine(20, 12, 20, 46, paint);
            cv.drawLine(6, 28, 34, 28, paint);
            paint.setStyle(Paint.Style.FILL);
            Path base = new Path();
            base.moveTo(9, 46);
            base.lineTo(31, 46);
            base.lineTo(27, 52);
            base.lineTo(13, 52);
            base.close();
            cv.drawPath(base, paint);
            cv.drawCircle(20, 55, 2, paint);
            cv.restore();
        }
    }

    /** حبل فوانيس صغيرة ونجوم فوق الرأس. */
    static final class GarlandView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path body = lanternBody();

        GarlandView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            float d = getResources().getDisplayMetrics().density;
            // الحبل
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1 * d);
            paint.setColor(0xFFC9A24A);
            Path rope = new Path();
            rope.moveTo(0, 4 * d);
            rope.quadTo(w / 4, 26 * d, w / 2, 8 * d);
            rope.quadTo(w * 3 / 4, 26 * d, w, 4 * d);
            cv.drawPath(rope, paint);
            paint.setStyle(Paint.Style.FILL);
            // نجوم صغيرة
            paint.setColor(0xCCF3D27A);
            for (int i = 0; i < 9; i++) {
                float x = w * (i + 0.5f) / 9f;
                float y = (i % 2 == 0 ? 30 : 36) * d;
                star(cv, x, y, 3.2f * d);
            }
            // ٥ فوانيس متعلقة على الحبل
            float[] xs = {0.11f, 0.30f, 0.5f, 0.70f, 0.89f};
            float[] ys = {12, 19, 9, 19, 12};
            float s = 0.42f * d;
            for (int i = 0; i < xs.length; i++) {
                cv.save();
                cv.translate(w * xs[i] - 20 * s, ys[i] * d);
                cv.scale(s, s);
                paint.setColor(GOLD_DARK);
                cv.drawRect(9, 6, 31, 12, paint);
                paint.setColor(i % 2 == 0 ? GOLD : CORAL);
                cv.drawPath(body, paint);
                paint.setColor(0xFFFFF1BF);
                cv.drawRect(14, 16, 26, 42, paint);
                paint.setColor(GOLD_DARK);
                cv.drawRect(11, 46, 29, 51, paint);
                cv.restore();
            }
        }

        private void star(Canvas cv, float x, float y, float r) {
            Path p = new Path();
            for (int i = 0; i < 10; i++) {
                double a = Math.PI / 5 * i - Math.PI / 2;
                float rr = i % 2 == 0 ? r : r * 0.45f;
                float px = x + (float) (Math.cos(a) * rr), py = y + (float) (Math.sin(a) * rr);
                if (i == 0) p.moveTo(px, py);
                else p.lineTo(px, py);
            }
            p.close();
            cv.drawPath(p, paint);
        }
    }
}
