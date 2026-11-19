package com.salah.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** بوصلة القبلة: السهم الأخضر يشاور على اتجاه الكعبة. */
public class QiblaActivity extends BaseActivity implements SensorEventListener {

    private SensorManager sensors;
    private Sensor rotation;
    private CompassView compass;
    private TextView info;
    private double qibla;
    private float declination;
    private float azimuth;
    private int arrowColor;
    private boolean hasReading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        double lat = Prefs.lat(this), lng = Prefs.lng(this);
        qibla = Qibla.bearing(lat, lng);
        declination = new GeomagneticField((float) lat, (float) lng, 0f,
                System.currentTimeMillis()).getDeclination();

        setContentView(R.layout.activity_qibla);
        arrowColor = ContextCompat.getColor(this, R.color.green_700);

        info = findViewById(R.id.qiblaInfo);
        compass = new CompassView(this);
        FrameLayout holder = findViewById(R.id.compassHolder);
        holder.addView(compass, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotation = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        updateInfo();
    }

    private void updateInfo() {
        String base = "اتجاه القبلة من " + Prefs.city(this) + ": " + Ui.arabicDigits(Math.round(qibla)) + "° من الشمال";
        if (rotation == null) {
            info.setText(base + "\nالموبايل ده مفيهوش حساس بوصلة");
        } else {
            info.setText(base);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotation != null) {
            sensors.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        if (sensors != null) sensors.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] matrix = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(matrix, event.values);
        SensorManager.getOrientation(matrix, orientation);
        float deg = (float) Math.toDegrees(orientation[0]) + declination;
        deg = (deg + 360f) % 360f;
        if (!hasReading) {
            azimuth = deg;
            hasReading = true;
        } else {
            // تنعيم الحركة مع مراعاة اللفة عند 360
            float diff = ((deg - azimuth + 540f) % 360f) - 180f;
            azimuth = (azimuth + diff * 0.15f + 360f) % 360f;
        }
        compass.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /** رسم البوصلة. */
    private class CompassView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path arrow = new Path();

        CompassView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float r = Math.min(cx, cy) * 0.85f;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(r * 0.03f);
            paint.setColor(Color.LTGRAY);
            canvas.drawCircle(cx, cy, r, paint);

            // علامة الشمال
            canvas.save();
            canvas.rotate(-azimuth, cx, cy);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.RED);
            paint.setTextSize(r * 0.16f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ش", cx, cy - r + r * 0.2f, paint);
            canvas.restore();

            // سهم القبلة
            float angle = (float) qibla - azimuth;
            canvas.save();
            canvas.rotate(angle, cx, cy);
            paint.setColor(arrowColor);
            arrow.reset();
            arrow.moveTo(cx, cy - r * 0.78f);
            arrow.lineTo(cx - r * 0.14f, cy + r * 0.1f);
            arrow.lineTo(cx, cy - r * 0.02f);
            arrow.lineTo(cx + r * 0.14f, cy + r * 0.1f);
            arrow.close();
            canvas.drawPath(arrow, paint);
            canvas.restore();

            paint.setColor(Color.DKGRAY);
            canvas.drawCircle(cx, cy, r * 0.04f, paint);
        }
    }
}
