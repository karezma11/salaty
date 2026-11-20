package com.salah.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** اختبارات زرار الهلال (مؤقت النوم) والعدّ اللي في الشريط. */
public class SleepTimerTest {

    @Test
    public void firstTapIsFifteenMinutes() {
        assertEquals(15, SleepTimer.nextStep(false, 0, false, true));
        assertEquals(15, SleepTimer.nextStep(false, 0, false, false));
    }

    @Test
    public void eachTapIsLonger() {
        assertEquals(30, SleepTimer.nextStep(true, 15, false, true));
        assertEquals(60, SleepTimer.nextStep(true, 30, false, true));
        // بعد «+١٥ د» من الشريط: ٢٣ دقيقة باقية، الضغطة الجاية ٣٠
        assertEquals(30, SleepTimer.nextStep(true, 23, false, false));
        assertEquals(60, SleepTimer.nextStep(true, 45, false, false));
    }

    @Test
    public void afterAnHourEndOfSuraThenOff() {
        assertEquals(SleepTimer.END_OF_SURA, SleepTimer.nextStep(true, 60, false, true));
        assertEquals(SleepTimer.OFF, SleepTimer.nextStep(true, 0, true, true));
        // الإذاعة والأصوات مالهاش «آخر السورة»
        assertEquals(SleepTimer.OFF, SleepTimer.nextStep(true, 60, false, false));
        assertEquals(SleepTimer.OFF, SleepTimer.nextStep(true, 75, false, false));
    }

    @Test
    public void stripClock() {
        assertEquals("٢٣:١٤", SleepStrip.clock((23 * 60 + 14) * 1000L));
        assertEquals("٠:٠٥", SleepStrip.clock(4200));
        assertEquals("١:٠٥:٠٠", SleepStrip.clock(65 * 60 * 1000L));
    }
}
