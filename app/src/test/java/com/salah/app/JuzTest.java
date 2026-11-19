package com.salah.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** اختبارات بدايات الأجزاء. */
public class JuzTest {

    @Test
    public void thirtyJuz() {
        assertEquals(30, Juz.count());
    }

    @Test
    public void firstAndLast() {
        assertEquals(1, Juz.sura(1));
        assertEquals(1, Juz.ayah(1));
        assertEquals(78, Juz.sura(30));
        assertEquals(1, Juz.ayah(30));
    }

    @Test
    public void startsAlwaysMoveForward() {
        for (int j = 2; j <= Juz.count(); j++) {
            boolean forward = Juz.sura(j) > Juz.sura(j - 1)
                    || (Juz.sura(j) == Juz.sura(j - 1) && Juz.ayah(j) > Juz.ayah(j - 1));
            assertTrue("الجزء " + j + " مش بعد اللي قبله", forward);
        }
    }

    @Test
    public void valuesAreSane() {
        for (int j = 1; j <= Juz.count(); j++) {
            assertTrue("سورة الجزء " + j, Juz.sura(j) >= 1 && Juz.sura(j) <= 114);
            assertTrue("آية الجزء " + j, Juz.ayah(j) >= 1 && Juz.ayah(j) <= 286);
        }
    }

    @Test
    public void clampsOutOfRange() {
        assertEquals(Juz.sura(1), Juz.sura(0));
        assertEquals(Juz.sura(30), Juz.sura(99));
    }
}
