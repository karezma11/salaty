package com.salah.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/** اختبارات التقويم الهجري. */
public class HijriTest {

    private void expect(int y, int m, int d, int hDay, int hMonth, String what) {
        Hijri.Date h = Hijri.of(y, m, d, 0);
        assertEquals(what + " — اليوم", hDay, h.day);
        assertEquals(what + " — الشهر", hMonth, h.month);
    }

    @Test
    public void knownDates() {
        expect(2024, 4, 10, 1, 10, "عيد الفطر 1445");
        expect(2025, 3, 1, 1, 9, "أول رمضان 1446");
        expect(2026, 2, 18, 1, 9, "أول رمضان 1447");
    }

    @Test
    public void monthAndDayStayInRange() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 1);
        for (int i = 0; i < 400; i++) {
            Hijri.Date h = Hijri.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH), 0);
            assertTrue("الشهر " + h.month, h.month >= 1 && h.month <= 12);
            assertTrue("اليوم " + h.day, h.day >= 1 && h.day <= 30);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    @Test
    public void daysAdvanceOneByOne() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 1);
        int previous = -1;
        for (int i = 0; i < 400; i++) {
            Hijri.Date h = Hijri.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH), 0);
            if (previous != -1) {
                assertTrue("قفزة من " + previous + " إلى " + h.day,
                        h.day == previous + 1 || h.day == 1);
            }
            previous = h.day;
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    @Test
    public void offsetShiftsByWholeDays() {
        Hijri.Date base = Hijri.of(2026, 6, 15, 0);
        Hijri.Date plus = Hijri.of(2026, 6, 15, 1);
        assertTrue("التعديل بيزحزح يوم", plus.day == base.day + 1 || plus.day == 1);
    }

    /** أول رمضان 1447 يوافق 18 فبراير، فبعده بـ12 يوم يبقى 13 رمضان = يوم أبيض. */
    @Test
    public void whiteDaysAreThirteenToFifteen() {
        Hijri.Date thirteenth = Hijri.of(2026, 3, 2, 0);
        assertEquals(13, thirteenth.day);
        assertTrue("13 يوم أبيض", Hijri.isWhiteDay(thirteenth));
        assertTrue("14 يوم أبيض", Hijri.isWhiteDay(Hijri.of(2026, 3, 3, 0)));
        assertTrue("15 يوم أبيض", Hijri.isWhiteDay(Hijri.of(2026, 3, 4, 0)));
        assertFalse("12 مش يوم أبيض", Hijri.isWhiteDay(Hijri.of(2026, 3, 1, 0)));
        assertFalse("16 مش يوم أبيض", Hijri.isWhiteDay(Hijri.of(2026, 3, 5, 0)));
    }
}
