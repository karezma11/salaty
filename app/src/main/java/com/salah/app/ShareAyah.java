package com.salah.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

/** مشاركة الآية كصورة مصممة، ولو فشل كنص. */
public final class ShareAyah {

    private static final int W = 1080;

    private ShareAyah() {
    }

    public static void share(Context c, int sura, String suraName, int ayah, String text) {
        try {
            File f = render(c, suraName, ayah, text);
            Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".files", f);
            Intent i = new Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            c.startActivity(Intent.createChooser(i, "مشاركة الآية"));
        } catch (Throwable t) {
            shareText(c, suraName, ayah, text);
        }
    }

    public static void shareText(Context c, String suraName, int ayah, String text) {
        try {
            String body = text + "\n\n[سورة " + suraName + " — الآية " + Ui.arabicDigits(ayah) + "]";
            c.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, body), "مشاركة الآية"));
        } catch (Throwable t) {
            Toast.makeText(c, "تعذّرت المشاركة", Toast.LENGTH_SHORT).show();
        }
    }

    private static File render(Context c, String suraName, int ayah, String text) throws Exception {
        TextPaint ayahPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        ayahPaint.setColor(Color.WHITE);
        ayahPaint.setTextSize(58f);
        Typeface quran = Ui.quranTypeface(c);
        ayahPaint.setTypeface(quran != null ? quran : Typeface.SERIF);

        int pad = 80;
        int maxWidth = W - pad * 2;
        StaticLayout layout = StaticLayout.Builder
                .obtain(text, 0, text.length(), ayahPaint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(22f, 1.0f)
                .build();

        TextPaint metaPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        metaPaint.setColor(Color.parseColor("#D7C27A"));
        metaPaint.setTextSize(36f);
        metaPaint.setTextAlign(Paint.Align.CENTER);
        Typeface naskh = Ui.naskhTypeface(c);
        if (naskh != null) metaPaint.setTypeface(naskh);

        int height = Math.max(640, layout.getHeight() + pad * 2 + 150);
        Bitmap bmp = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new LinearGradient(0, 0, W, height,
                Color.parseColor("#0B5D3B"), Color.parseColor("#04231A"), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, height, bg);

        Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
        frame.setStyle(Paint.Style.STROKE);
        frame.setStrokeWidth(3f);
        frame.setColor(Color.parseColor("#55C8A951"));
        canvas.drawRoundRect(new RectF(34, 34, W - 34, height - 34), 28, 28, frame);

        canvas.save();
        canvas.translate(pad, pad + 20);
        layout.draw(canvas);
        canvas.restore();

        canvas.drawText("سورة " + suraName + " · الآية " + Ui.arabicDigits(ayah),
                W / 2f, height - 70, metaPaint);

        File dir = new File(c.getCacheDir(), "share");
        dir.mkdirs();
        File out = new File(dir, "ayah.png");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bmp.recycle();
        return out;
    }
}
