package com.nimo.game.engine;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.Random;

/**
 * Efeitos visuais leves: partículas, confetes, textos flutuantes, flash de tela e tremor.
 * Usa arrays de tamanho fixo para não criar objetos durante a partida.
 */
public class EffectsManager {

    private static final int MAX_PARTICLES = 260;
    private static final int MAX_TEXTS = 12;
    private static final int[] CONFETTI_COLORS = {
            0xFFFF5FA2, 0xFFFFD54F, 0xFF4FC3F7, 0xFF81C784, 0xFFBA68C8, 0xFFFF8A65
    };

    private final float[] px = new float[MAX_PARTICLES];
    private final float[] py = new float[MAX_PARTICLES];
    private final float[] pvx = new float[MAX_PARTICLES];
    private final float[] pvy = new float[MAX_PARTICLES];
    private final float[] life = new float[MAX_PARTICLES];
    private final float[] maxLife = new float[MAX_PARTICLES];
    private final float[] psize = new float[MAX_PARTICLES];
    private final float[] gravity = new float[MAX_PARTICLES];
    private final float[] rotation = new float[MAX_PARTICLES];
    private final float[] spin = new float[MAX_PARTICLES];
    private final int[] color = new int[MAX_PARTICLES];
    private final boolean[] square = new boolean[MAX_PARTICLES];
    private int cursor;

    private final String[] texts = new String[MAX_TEXTS];
    private final float[] tx = new float[MAX_TEXTS];
    private final float[] ty = new float[MAX_TEXTS];
    private final float[] tlife = new float[MAX_TEXTS];
    private final float[] tscale = new float[MAX_TEXTS];
    private final int[] tcolor = new int[MAX_TEXTS];
    private int textCursor;

    private float flashAlpha;
    private int flashColor;
    private float shakeTime;
    private float shakeDuration;
    private float shakeMagnitude;
    private float density = 1f;

    private final Random random = new Random();
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flashPaint = new Paint();

    public EffectsManager() {
        Typeface bold = Typeface.create("sans-serif-black", Typeface.BOLD);
        textFill.setTypeface(bold);
        textFill.setTextAlign(Paint.Align.CENTER);
        textStroke.setTypeface(bold);
        textStroke.setTextAlign(Paint.Align.CENTER);
        textStroke.setStyle(Paint.Style.STROKE);
        textStroke.setStrokeJoin(Paint.Join.ROUND);
        textStroke.setColor(0xFFFFFFFF);
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public void clear() {
        for (int i = 0; i < MAX_PARTICLES; i++) life[i] = 0f;
        for (int i = 0; i < MAX_TEXTS; i++) tlife[i] = 0f;
        flashAlpha = 0f;
        shakeTime = 0f;
    }

    private int nextParticle() {
        for (int n = 0; n < MAX_PARTICLES; n++) {
            int i = (cursor + n) % MAX_PARTICLES;
            if (life[i] <= 0f) {
                cursor = (i + 1) % MAX_PARTICLES;
                return i;
            }
        }
        int i = cursor;
        cursor = (cursor + 1) % MAX_PARTICLES;
        return i;
    }

    /** Explosão de partículas circulares a partir de um ponto. */
    public void burst(float x, float y, int c, int count, float speedDp, float sizeDp) {
        for (int n = 0; n < count; n++) {
            int i = nextParticle();
            double angle = random.nextDouble() * Math.PI * 2.0;
            float speed = (0.4f + random.nextFloat() * 0.6f) * speedDp * density;
            px[i] = x;
            py[i] = y;
            pvx[i] = (float) Math.cos(angle) * speed;
            pvy[i] = (float) Math.sin(angle) * speed - 60f * density;
            maxLife[i] = 0.45f + random.nextFloat() * 0.35f;
            life[i] = maxLife[i];
            psize[i] = (0.6f + random.nextFloat() * 0.6f) * sizeDp * density;
            gravity[i] = 420f * density;
            rotation[i] = 0f;
            spin[i] = 0f;
            color[i] = c;
            square[i] = false;
        }
    }

    /** Confetes caindo do topo da tela. */
    public void confetti(float screenWidth, int count) {
        for (int n = 0; n < count; n++) {
            int i = nextParticle();
            px[i] = random.nextFloat() * screenWidth;
            py[i] = -10f * density;
            pvx[i] = (random.nextFloat() * 2f - 1f) * 60f * density;
            pvy[i] = (120f + random.nextFloat() * 160f) * density;
            maxLife[i] = 3.5f;
            life[i] = maxLife[i];
            psize[i] = (5f + random.nextFloat() * 4f) * density;
            gravity[i] = 40f * density;
            rotation[i] = random.nextFloat() * 360f;
            spin[i] = (random.nextFloat() * 2f - 1f) * 400f;
            color[i] = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.length)];
            square[i] = true;
        }
    }

    public void floatText(String text, float x, float y, int c, float scale) {
        int i = textCursor;
        textCursor = (textCursor + 1) % MAX_TEXTS;
        texts[i] = text;
        tx[i] = x;
        ty[i] = y;
        tlife[i] = 1f;
        tscale[i] = scale;
        tcolor[i] = c;
    }

    public void flash(int c, float alpha) {
        flashColor = c;
        flashAlpha = alpha;
    }

    public void shake(float magnitudeDp, float duration) {
        shakeMagnitude = magnitudeDp * density;
        shakeDuration = duration;
        shakeTime = duration;
    }

    public void update(float dt) {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (life[i] <= 0f) continue;
            life[i] -= dt;
            pvy[i] += gravity[i] * dt;
            px[i] += pvx[i] * dt;
            py[i] += pvy[i] * dt;
            rotation[i] += spin[i] * dt;
        }
        for (int i = 0; i < MAX_TEXTS; i++) {
            if (tlife[i] <= 0f) continue;
            tlife[i] -= dt * 1.1f;
            ty[i] -= 70f * density * dt;
        }
        if (flashAlpha > 0f) flashAlpha = Math.max(0f, flashAlpha - dt * 1.8f);
        if (shakeTime > 0f) shakeTime = Math.max(0f, shakeTime - dt);
    }

    public float getShakeX() {
        if (shakeTime <= 0f || shakeDuration <= 0f) return 0f;
        return (random.nextFloat() * 2f - 1f) * shakeMagnitude * (shakeTime / shakeDuration);
    }

    public float getShakeY() {
        if (shakeTime <= 0f || shakeDuration <= 0f) return 0f;
        return (random.nextFloat() * 2f - 1f) * shakeMagnitude * 0.5f * (shakeTime / shakeDuration);
    }

    public void drawParticles(Canvas canvas) {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (life[i] <= 0f) continue;
            float k = Math.min(1f, life[i] / maxLife[i] * 1.6f);
            particlePaint.setColor(color[i]);
            particlePaint.setAlpha((int) (255 * k));
            if (square[i]) {
                canvas.save();
                canvas.rotate(rotation[i], px[i], py[i]);
                float s = psize[i];
                canvas.drawRect(px[i] - s, py[i] - s * 0.5f, px[i] + s, py[i] + s * 0.5f, particlePaint);
                canvas.restore();
            } else {
                canvas.drawCircle(px[i], py[i], psize[i] * (0.4f + 0.6f * k), particlePaint);
            }
        }
    }

    public void drawTexts(Canvas canvas) {
        for (int i = 0; i < MAX_TEXTS; i++) {
            if (tlife[i] <= 0f || texts[i] == null) continue;
            float k = tlife[i];
            float pop = k > 0.8f ? 1f + (k - 0.8f) * 2f : 1f;
            float size = 26f * density * tscale[i] * pop;
            int alpha = (int) (255 * Math.min(1f, k * 2f));
            textStroke.setTextSize(size);
            textStroke.setStrokeWidth(size * 0.18f);
            textStroke.setAlpha(alpha);
            textFill.setTextSize(size);
            textFill.setColor(tcolor[i]);
            textFill.setAlpha(alpha);
            canvas.drawText(texts[i], tx[i], ty[i], textStroke);
            canvas.drawText(texts[i], tx[i], ty[i], textFill);
        }
    }

    public void drawFlash(Canvas canvas, int width, int height) {
        if (flashAlpha <= 0f) return;
        flashPaint.setColor(flashColor);
        flashPaint.setAlpha((int) (255 * Math.min(1f, flashAlpha)));
        canvas.drawRect(0, 0, width, height, flashPaint);
    }
}
