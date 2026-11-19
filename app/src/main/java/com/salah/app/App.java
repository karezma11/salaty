package com.salah.app;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

/** يطبّق المظهر المختار قبل فتح أي شاشة. */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashLog.install(this);
        applyTheme(this);
    }

    public static void applyTheme(Context c) {
        int mode;
        switch (Prefs.theme(c)) {
            case 1:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case 2:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
