package com.salah.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** مقارنة نسخة GitHub بالنسخة المثبّتة. */
public class UpdaterTest {

    @Test
    public void cleanTag() {
        assertEquals("3.5", Updater.clean("v3.5"));
        assertEquals("3.10", Updater.clean("صلاتي 3.10"));
        assertEquals("4", Updater.clean("v4."));
        assertEquals("", Updater.clean("latest"));
        assertEquals("", Updater.clean(null));
    }

    @Test
    public void newerByNumberNotText() {
        assertTrue(Updater.isNewer("v3.5", "3.4"));
        assertTrue(Updater.isNewer("v3.10", "3.9")); // كنص "3.10" أصغر من "3.9"
        assertTrue(Updater.isNewer("v4.0", "3.9"));
        assertTrue(Updater.isNewer("3.4.1", "3.4"));
    }

    @Test
    public void notNewer() {
        assertFalse(Updater.isNewer("v3.4", "3.4"));
        assertFalse(Updater.isNewer("v3.3", "3.4"));
        assertFalse(Updater.isNewer("3.4.0", "3.4"));
        assertFalse(Updater.isNewer("", "3.4"));
        assertFalse(Updater.isNewer("latest", "3.4"));
    }

    @Test
    public void notesWithoutMarkdown() {
        String s = Updater.plainNotes("## الجديد\r\n- زرار **فحص التحديثات**\n* `تصليح`\n\n\n\nآخر");
        assertEquals("الجديد\n• زرار فحص التحديثات\n• تصليح\n\nآخر", s);
    }
}
