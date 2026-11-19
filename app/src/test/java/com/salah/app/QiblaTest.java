package com.salah.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** اختبارات اتجاه القبلة. */
public class QiblaTest {

    @Test
    public void knownCities() {
        assertEquals("القاهرة", 136.1, Qibla.bearing(30.0444, 31.2357), 1.0);
        assertEquals("لندن", 119.0, Qibla.bearing(51.5074, -0.1278), 1.5);
        assertEquals("كوالالمبور", 292.5, Qibla.bearing(3.139, 101.687), 2.0);
        assertEquals("جاكرتا", 295.1, Qibla.bearing(-6.2088, 106.8456), 2.0);
    }

    @Test
    public void alwaysWithinACircle() {
        for (double lat = -80; lat <= 80; lat += 10) {
            for (double lng = -180; lng <= 180; lng += 15) {
                double b = Qibla.bearing(lat, lng);
                assertTrue("خارج المدى عند " + lat + "," + lng, b >= 0 && b < 360);
                assertTrue("NaN عند " + lat + "," + lng, !Double.isNaN(b));
            }
        }
    }

    @Test
    public void northOfMeccaPointsSouth() {
        double b = Qibla.bearing(30.0, 39.8262);
        assertTrue("من الشمال تمامًا القبلة جنوب", b > 170 && b < 190);
    }
}
