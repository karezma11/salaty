package com.salah.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import java.util.Calendar;

/**
 * صورة الإشعار الدائم (الشكل «س» اللي اختاره المستخدم): التاريخ الهجري فوق، والصلوات
 * الست جنب بعض برمز لكل صلاة والجاية متعلّمة بالدهبي، ودعاء تحت.
 * بتتحط كـ BigPictureStyle: صورة في إشعار عادي، لأن vivo بيشيل أي تصميم مخصص (RemoteViews).
 */
final class TimesCard {

    static final String[] DUAS = {
            "اللهم أعني على ذكرك وشكرك وحسن عبادتك",
            "ربنا آتنا في الدنيا حسنة وفي الآخرة حسنة وقنا عذاب النار",
            "اللهم إني أسألك العفو والعافية في الدنيا والآخرة",
            "يا مقلب القلوب ثبّت قلبي على دينك",
            "رب اجعلني مقيم الصلاة ومن ذريتي ربنا وتقبل دعاء",
            "اللهم إنك عفو تحب العفو فاعف عني",
            "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم",
            "رب اغفر لي ولوالدي وللمؤمنين يوم يقوم الحساب",
    };

    private static final String[] DAYS = {
            "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};

    private static final int BG = 0xFF0B3B2C;
    private static final int ON_BG = 0xFF13513D;
    private static final int MINT = 0xFF9FE1CB;
    private static final int GOLD = 0xFFE2B04A;
    private static final int WHITE = 0xFFFFFFFF;
    /** علامة ✓ تحت الصلاة اللي اتسجّلت. */
    private static final int DONE = 0xFF2E9E78;

    private TimesCard() {
    }

    /** الدعاء بيتغيّر مع كل صلاة وكل يوم. */
    static String dua(int nextIdx) {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        return DUAS[Math.abs(day * 6 + nextIdx) % DUAS.length];
    }

    static String hijriLine(Context c) {
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        return DAYS[dow] + "  ·  " + Hijri.today(c).text();
    }

    /** أرقام عربي للوقت من غير ص/م (الصورة صغيرة). */
    static String digits(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            sb.append(ch >= '0' && ch <= '9' ? (char) ('٠' + (ch - '0')) : ch);
        }
        return sb.toString();
    }

    /** logDaysAgo: يوم علامات ✓ (١ لو عدّى نص الليل والعشاء لسه بتاعة امبارح). */
    static Bitmap render(Context c, double[] t, int next, int logDaysAgo) {
        int w = 960, h = 470;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(BG);
        cv.drawRoundRect(new RectF(0, 0, w, h), 40, 40, p);

        Typeface font = null;
        Typeface bold = null;
        try {
            font = ResourcesCompat.getFont(c, R.font.amiri_regular);
            bold = ResourcesCompat.getFont(c, R.font.amiri_bold);
        } catch (Exception ignored) {
        }
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTextAlign(Paint.Align.CENTER);
        if (font != null) text.setTypeface(font);

        // التاريخ الهجري فوق
        text.setColor(GOLD);
        text.setTextSize(38);
        if (bold != null) text.setTypeface(bold);
        cv.drawText(hijriLine(c), w / 2f, 62, text);
        if (font != null) text.setTypeface(font);

        // الصلوات الست من اليمين للشمال
        float colW = (w - 40) / 6f;
        float top = 92, bottom = 362;
        for (int i = 0; i < 6; i++) {
            float cx = w - 20 - colW * i - colW / 2f;
            boolean on = i == next;
            int col = on ? GOLD : MINT;
            if (on) {
                p.setColor(ON_BG);
                cv.drawRoundRect(new RectF(cx - colW / 2f + 8, top, cx + colW / 2f - 8, bottom),
                        28, 28, p);
            }
            // اتطلعوا لفوق شوية عشان علامة ✓ تيجي تحتهم جوه المربع
            drawIcon(cv, i, cx, top + 64, col);
            text.setColor(col);
            text.setTextSize(34);
            cv.drawText(PrayerTimes.NAMES[i], cx, top + 168, text);
            text.setColor(on ? GOLD : WHITE);
            text.setTextSize(38);
            String time = PrayerTimes.format(t[i]);
            int sp = time.indexOf(' ');
            cv.drawText(digits(sp > 0 ? time.substring(0, sp) : time), cx, top + 222, text);
            if (i != PrayerTimes.SUNRISE
                    && PrayerLog.state(c, logDaysAgo, i) != PrayerLog.NOT_PRAYED) {
                drawCheck(cv, cx, top + 252);
            }
        }

        // الدعاء تحت
        text.setColor(MINT);
        text.setTextSize(fit(text, dua(next), w - 60, 34));
        cv.drawText(dua(next), w / 2f, 428, text);
        return bmp;
    }

    private static void drawCheck(Canvas cv, float x, float y) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(DONE);
        cv.drawCircle(x, y, 14, p);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(WHITE);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(4);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        Path ck = new Path();
        ck.moveTo(x - 6.5f, y + 0.5f);
        ck.lineTo(x - 1.5f, y + 5.5f);
        ck.lineTo(x + 7f, y - 4.5f);
        cv.drawPath(ck, line);
    }

    /** يصغّر الخط لحد ما السطر ييجي في العرض. */
    private static float fit(Paint p, String s, float max, float size) {
        p.setTextSize(size);
        while (size > 20 && p.measureText(s) > max) {
            size -= 1;
            p.setTextSize(size);
        }
        return size;
    }

    /** رمز كل صلاة: فجر، شروق، ظهر، عصر، مغرب، عشاء. */
    private static void drawIcon(Canvas cv, int i, float x, float y, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(color);
        line.setStrokeWidth(5);
        line.setStrokeCap(Paint.Cap.ROUND);
        float r = 26;
        switch (i) {
            case 0: { // نص شمس تحت الأفق
                cv.save();
                cv.clipRect(x - 50, y - 50, x + 50, y + 12);
                cv.drawCircle(x, y + 12, r, p);
                cv.restore();
                cv.drawLine(x - 44, y + 14, x + 44, y + 14, line);
                break;
            }
            case 1: // شمس فوق الأفق
                cv.drawCircle(x, y - 4, r, p);
                cv.drawLine(x - 44, y + 30, x + 44, y + 30, line);
                break;
            case 2: // شمس بأشعة
                cv.drawCircle(x, y, r - 4, p);
                for (int d = 0; d < 8; d++) {
                    double a = Math.toRadians(d * 45);
                    cv.drawLine(x + (float) Math.cos(a) * 34, y + (float) Math.sin(a) * 34,
                            x + (float) Math.cos(a) * 44, y + (float) Math.sin(a) * 44, line);
                }
                break;
            case 3: { // شمس ورا سحابة
                cv.drawCircle(x + 8, y - 6, r - 4, p);
                Paint cloud = new Paint(Paint.ANTI_ALIAS_FLAG);
                cloud.setColor(color);
                cloud.setAlpha(170);
                cv.drawRoundRect(new RectF(x - 42, y + 2, x + 42, y + 30), 14, 14, cloud);
                break;
            }
            case 4: { // شمس بتغرب
                cv.save();
                cv.clipRect(x - 50, y - 50, x + 50, y + 16);
                cv.drawCircle(x, y + 16, r + 2, p);
                cv.restore();
                Paint ground = new Paint(Paint.ANTI_ALIAS_FLAG);
                ground.setColor(0xFF3A1C14);
                cv.drawRect(x - 44, y + 16, x + 44, y + 34, ground);
                break;
            }
            default: { // هلال
                Path moon = new Path();
                moon.addCircle(x, y, r, Path.Direction.CW);
                Path cut = new Path();
                cut.addCircle(x + 14, y - 10, r - 2, Path.Direction.CW);
                moon.op(cut, Path.Op.DIFFERENCE);
                cv.drawPath(moon, p);
            }
        }
    }
}
