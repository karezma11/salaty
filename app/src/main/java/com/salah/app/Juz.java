package com.salah.app;

/** بدايات الأجزاء الثلاثين. */
public final class Juz {

    /** لكل جزء: {السورة، الآية}. */
    private static final int[][] START = {
            {1, 1}, {2, 142}, {2, 253}, {3, 92}, {4, 24}, {4, 148},
            {5, 82}, {6, 111}, {7, 88}, {8, 41}, {9, 93}, {11, 6},
            {12, 53}, {15, 1}, {17, 1}, {18, 75}, {21, 1}, {23, 1},
            {25, 21}, {27, 56}, {29, 46}, {33, 31}, {36, 28}, {39, 32},
            {41, 47}, {46, 1}, {51, 31}, {58, 1}, {67, 1}, {78, 1}
    };

    private Juz() {
    }

    public static int count() {
        return START.length;
    }

    public static int sura(int juz) {
        return START[clamp(juz) - 1][0];
    }

    public static int ayah(int juz) {
        return START[clamp(juz) - 1][1];
    }

    private static int clamp(int juz) {
        if (juz < 1) return 1;
        if (juz > START.length) return START.length;
        return juz;
    }

    public static String name(int juz) {
        return "الجزء " + Ui.arabicDigits(clamp(juz));
    }
}
