package com.nimo.game.engine;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Lê o acelerômetro e converte a inclinação lateral do aparelho em um fator de
 * movimento de -1 (esquerda) a 1 (direita), com suavização, zona morta e curva
 * de resposta. Se não houver acelerômetro, usa o toque como alternativa.
 */
public class TiltController implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private volatile float filteredX;
    private volatile boolean hasSample;
    private volatile boolean touching;
    private volatile float touchX;
    private float sensitivity = Config.TILT_SENSITIVITY;
    private boolean registered;

    public TiltController(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    public static boolean deviceHasAccelerometer(Context context) {
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sm != null && sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
    }

    public boolean hasAccelerometer() {
        return accelerometer != null;
    }

    public boolean isTouchControlEnabled() {
        return accelerometer == null || Config.ALWAYS_ALLOW_TOUCH;
    }

    public void register() {
        if (accelerometer != null && !registered) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            registered = true;
        }
    }

    public void unregister() {
        if (registered) {
            sensorManager.unregisterListener(this);
            registered = false;
        }
        touching = false;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float raw = event.values[0];
        if (!hasSample) {
            filteredX = raw;
            hasSample = true;
        } else {
            filteredX += Config.TILT_SMOOTHING * (raw - filteredX);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Não utilizado.
    }

    public void setTouch(boolean active, float x) {
        touching = active;
        touchX = x;
    }

    /** Fator de movimento entre -1 e 1. */
    public float getMoveFactor(float playerX, float screenWidth) {
        if (touching && isTouchControlEnabled()) {
            float distance = touchX - playerX;
            if (Math.abs(distance) < screenWidth * 0.01f) return 0f;
            float factor = distance / (screenWidth * Config.TOUCH_FULL_SPEED_DISTANCE);
            return Math.max(-1f, Math.min(1f, factor));
        }
        if (accelerometer == null || !hasSample) return 0f;
        // Inclinar para a esquerda gera valores positivos no eixo X do acelerômetro.
        float tilt = -filteredX * sensitivity;
        float abs = Math.abs(tilt);
        if (abs < Config.TILT_DEAD_ZONE) return 0f;
        float f = (abs - Config.TILT_DEAD_ZONE) / (Config.TILT_FULL_SPEED - Config.TILT_DEAD_ZONE);
        if (f > 1f) f = 1f;
        f = (float) Math.pow(f, Config.TILT_CURVE);
        return Math.signum(tilt) * f;
    }
}
