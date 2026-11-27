package com.salah.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** اختبارات الإمساك ونسبة الصيام وبداية الثلث الأخير من الليل. */
public class RamadanTest {

    private static final double MIN = 1 / 60.0;

    @Test
    public void imsakTenMinutesBeforeFajr() {
        assertEquals(4 + 50 * MIN, Ramadan.imsak(5.0), 1e-9);
        // فجر بعد نص الليل بشوية (بلاد في الشمال صيفًا)
        assertEquals(24 - 4 * MIN, Ramadan.imsak(6 * MIN), 1e-9);
    }

    @Test
    public void lastThirdOfTheNight() {
        // ليل ١٢ ساعة: من ٦ المغرب لـ ٦ الفجر، والثلث الأخير من ٢
        assertEquals(2.0, Ramadan.qiyamStart(18.0, 6.0), 1e-9);
        // ليل ٨ ساعات: ٧:٣٠ م لـ ٣:٣٠ ص، الثلث الأخير ١٢:٥٠ ص
        assertEquals(50 * MIN, Ramadan.qiyamStart(19.5, 3.5), 1e-9);
        // ليل شتا طويل: ٤ العصر لـ ٨:٣٠ الصبح، الثلث الأخير من ٣
        assertEquals(3.0, Ramadan.qiyamStart(16.0, 8.5), 1e-9);
        // ليل صيف قصير جدًا: الثلث الأخير بيبدأ قبل نص الليل
        assertEquals(24 - 10 * MIN, Ramadan.qiyamStart(21.5, 1.0), 1e-9);
    }

    @Test
    public void fastProgress() {
        assertEquals(0.0, Ramadan.fastProgress(5, 18, 4), 1e-9);
        assertEquals(0.5, Ramadan.fastProgress(5, 18, 11.5), 1e-9);
        assertEquals(1.0, Ramadan.fastProgress(5, 18, 20), 1e-9);
    }
}
