package com.salah.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * لو التطبيق وقع، بيحفظ سبب الوقوع في ملف، وأول ما يتفتح تاني بيعرضه مع زرار نسخ.
 * مفيش كابل ولا adb عند المستخدم، فده الطريقة الوحيدة نعرف السبب بدل التخمين.
 */
final class CrashLog {

    private CrashLog() {
    }

    private static File file(Context c) {
        return new File(c.getFilesDir(), "last-crash.txt");
    }

    static void install(Context c) {
        Context app = c.getApplicationContext();
        Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String head = "صلاتي " + Updater.installedVersion(app)
                        + " · أندرويد " + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")"
                        + " · " + Build.MANUFACTURER + " " + Build.MODEL + "\n\n";
                try (FileOutputStream out = new FileOutputStream(file(app))) {
                    out.write((head + sw).getBytes(StandardCharsets.UTF_8));
                }
            } catch (Throwable ignored) {
            }
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    /** لو فيه وقوع محفوظ: يعرضه مرة واحدة ويمسحه. */
    static void showIfAny(Activity a) {
        File f = file(a);
        if (!f.exists()) return;
        String text;
        try {
            text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            text = String.valueOf(e);
        }
        f.delete();
        if (text.length() > 4000) text = text.substring(0, 4000);
        final String report = text;
        new AlertDialog.Builder(a)
                .setTitle("التطبيق وقف المرة اللي فاتت")
                .setMessage("انسخ التفاصيل دي وابعتها عشان المشكلة تتصلّح:\n\n" + report)
                .setPositiveButton("نسخ", (d, w) -> {
                    ClipboardManager cm = a.getSystemService(ClipboardManager.class);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("crash", report));
                    Toast.makeText(a, "اتنسخ", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("قفل", null)
                .show();
    }
}
