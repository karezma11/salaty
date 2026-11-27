package com.salah.app;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

/**
 * أسماء ملفات الصوت لازم تفضل بأرقام إنجليزي حتى لو لغة الموبايل عربي:
 * String.format من غير Locale كان بيكتب «٠٠١.mp3» فالأذكار ماكانتش بتلاقي صوتها
 * وتحميل التلاوة كان بيطلب روابط مش موجودة ويقول «ما كملش».
 */
public class FileNamesTest {

    private Locale saved;

    @Before
    public void arabicPhone() {
        saved = Locale.getDefault();
        Locale.setDefault(new Locale("ar", "EG"));
    }

    @After
    public void restore() {
        Locale.setDefault(saved);
    }

    @Test
    public void adhkarAudioUsesLatinDigits() {
        assertEquals("adhkar/007.mp3", Dhikr.asset(new Dhikr.Item(7, 1, "", "")));
        assertEquals("adhkar/287.mp3", Dhikr.asset(new Dhikr.Item(287, 1, "", "")));
    }

    @Test
    public void recitationAssetUsesLatinDigits() {
        assertEquals("002/002255.mp3", Recitation.assetName(2, 255));
    }
}
