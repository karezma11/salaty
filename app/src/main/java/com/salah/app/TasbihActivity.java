package com.salah.app;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/** مسبحة إلكترونية. */
public class TasbihActivity extends BaseActivity {

    private static final String[] PHRASES = {
            "سبحان الله", "الحمد لله", "الله أكبر", "لا إله إلا الله",
            "أستغفر الله", "اللهم صلِّ وسلم على نبينا محمد",
            "سبحان الله وبحمده، سبحان الله العظيم",
            "لا حول ولا قوة إلا بالله"
    };

    private int index = 0;
    private int count = 0;
    private TextView phrase, countView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasbih);

        phrase = findViewById(R.id.phrase);
        countView = findViewById(R.id.count);
        Ui.applyQuranFont(phrase);

        findViewById(R.id.tap).setOnClickListener(v -> {
            count++;
            render();
            if (count % 33 == 0) buzz();
        });
        findViewById(R.id.reset).setOnClickListener(v -> {
            count = 0;
            render();
        });
        findViewById(R.id.change).setOnClickListener(v -> pick());
        render();
    }

    private void pick() {
        new AlertDialog.Builder(this)
                .setTitle("اختر الذكر")
                .setItems(PHRASES, (d, which) -> {
                    index = which;
                    count = 0;
                    render();
                })
                .show();
    }

    private void render() {
        phrase.setText(PHRASES[index]);
        countView.setText(Ui.arabicDigits(count));
    }

    private void buzz() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {
        }
    }
}
