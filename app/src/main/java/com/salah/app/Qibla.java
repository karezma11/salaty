package com.salah.app;

/** حساب اتجاه القبلة بالدرجات من الشمال الحقيقي، في اتجاه عقارب الساعة. */
public final class Qibla {

    private static final double KAABA_LAT = 21.4225;
    private static final double KAABA_LNG = 39.8262;

    private Qibla() {
    }

    public static double bearing(double lat, double lng) {
        double phi = Math.toRadians(lat);
        double phiK = Math.toRadians(KAABA_LAT);
        double dLng = Math.toRadians(KAABA_LNG - lng);
        double y = Math.sin(dLng);
        double x = Math.cos(phi) * Math.tan(phiK) - Math.sin(phi) * Math.cos(dLng);
        double deg = Math.toDegrees(Math.atan2(y, x));
        return (deg + 360.0) % 360.0;
    }
}
