package com.nimo.game.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Random;

/**
 * Desenha o cenário cartunesco: céu, sol, montanhas, colinas, árvores, campo,
 * flores, trilha e nuvens animadas. A parte estática é desenhada uma única vez
 * em um bitmap (por tamanho de tela) para economizar processamento.
 */
public class SceneRenderer {

    public static final int SKY_COLOR = 0xFF4FA8F0;
    private static final int CLOUD_COUNT = 6;

    private final boolean drawPath;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudShadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private Bitmap background;
    private int width;
    private int height;
    private float groundY;

    private final float[] cloudX = new float[CLOUD_COUNT];
    private final float[] cloudY = new float[CLOUD_COUNT];
    private final float[] cloudSize = new float[CLOUD_COUNT];
    private final float[] cloudSpeed = new float[CLOUD_COUNT];

    public SceneRenderer(boolean drawPath) {
        this.drawPath = drawPath;
        cloudPaint.setColor(0xF2FFFFFF);
        cloudShadePaint.setColor(0x1A2A6FA8);
        paint.setDither(true);
    }

    public void setSize(int w, int h, float groundLineY) {
        if (w <= 0 || h <= 0) return;
        if (w == width && h == height && background != null) return;
        width = w;
        height = h;
        groundY = groundLineY;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmp);
        drawStatic(c);
        background = bmp;

        Random r = new Random(7);
        for (int i = 0; i < CLOUD_COUNT; i++) {
            cloudX[i] = r.nextFloat() * w;
            cloudY[i] = h * (0.05f + 0.28f * r.nextFloat());
            cloudSize[i] = w * (0.10f + 0.08f * r.nextFloat());
            cloudSpeed[i] = w * (0.010f + 0.018f * r.nextFloat());
        }
    }

    public void update(float dt) {
        for (int i = 0; i < CLOUD_COUNT; i++) {
            cloudX[i] += cloudSpeed[i] * dt;
            if (cloudX[i] - cloudSize[i] * 0.6f > width) {
                cloudX[i] = -cloudSize[i] * 1.6f;
            }
        }
    }

    public void draw(Canvas canvas) {
        if (background == null) {
            canvas.drawColor(SKY_COLOR);
            return;
        }
        canvas.drawBitmap(background, 0f, 0f, null);
        for (int i = 0; i < CLOUD_COUNT; i++) {
            drawCloud(canvas, cloudX[i], cloudY[i], cloudSize[i]);
        }
    }

    private void drawCloud(Canvas c, float x, float y, float s) {
        rect.set(x - s * 0.45f, y - s * 0.05f + s * 0.08f, x + s * 1.45f, y + s * 0.45f + s * 0.08f);
        c.drawRoundRect(rect, s * 0.25f, s * 0.25f, cloudShadePaint);
        c.drawCircle(x, y, s * 0.42f, cloudPaint);
        c.drawCircle(x + s * 0.48f, y - s * 0.22f, s * 0.52f, cloudPaint);
        c.drawCircle(x + s * 1.0f, y, s * 0.40f, cloudPaint);
        rect.set(x - s * 0.45f, y - s * 0.05f, x + s * 1.45f, y + s * 0.42f);
        c.drawRoundRect(rect, s * 0.22f, s * 0.22f, cloudPaint);
    }

    private void drawStatic(Canvas c) {
        float w = width;
        float h = height;

        // Céu
        paint.setShader(new LinearGradient(0, 0, 0, h * 0.72f, 0xFF3D9BE9, 0xFFC4E9FF, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Sol
        paint.setColor(0x40FFF3B0);
        c.drawCircle(w * 0.84f, h * 0.10f, w * 0.14f, paint);
        paint.setColor(0x66FFF3B0);
        c.drawCircle(w * 0.84f, h * 0.10f, w * 0.10f, paint);
        paint.setColor(0xFFFFE15A);
        c.drawCircle(w * 0.84f, h * 0.10f, w * 0.075f, paint);

        // Montanhas ao fundo
        path.reset();
        path.moveTo(0, h * 0.62f);
        path.lineTo(0, h * 0.50f);
        path.lineTo(w * 0.18f, h * 0.40f);
        path.lineTo(w * 0.34f, h * 0.51f);
        path.lineTo(w * 0.52f, h * 0.37f);
        path.lineTo(w * 0.72f, h * 0.51f);
        path.lineTo(w * 0.88f, h * 0.42f);
        path.lineTo(w, h * 0.49f);
        path.lineTo(w, h * 0.62f);
        path.close();
        paint.setColor(0xFF8DB9DD);
        c.drawPath(path, paint);
        paint.setColor(0xFFF4FAFF);
        drawSnowCap(c, w * 0.18f, h * 0.40f, w * 0.055f, h * 0.030f);
        drawSnowCap(c, w * 0.52f, h * 0.37f, w * 0.060f, h * 0.034f);
        drawSnowCap(c, w * 0.88f, h * 0.42f, w * 0.050f, h * 0.026f);

        // Colina de trás
        path.reset();
        path.moveTo(0, h * 0.60f);
        path.quadTo(w * 0.25f, h * 0.52f, w * 0.55f, h * 0.60f);
        path.quadTo(w * 0.80f, h * 0.66f, w, h * 0.57f);
        path.lineTo(w, h * 0.80f);
        path.lineTo(0, h * 0.80f);
        path.close();
        paint.setColor(0xFF7CCB6B);
        c.drawPath(path, paint);

        drawTree(c, w * 0.10f, h * 0.585f, h * 0.075f);
        drawTree(c, w * 0.24f, h * 0.565f, h * 0.085f);
        drawTree(c, w * 0.83f, h * 0.61f, h * 0.07f);
        drawTree(c, w * 0.94f, h * 0.595f, h * 0.08f);

        // Colina da frente
        path.reset();
        path.moveTo(0, h * 0.69f);
        path.quadTo(w * 0.35f, h * 0.61f, w * 0.70f, h * 0.68f);
        path.quadTo(w * 0.88f, h * 0.71f, w, h * 0.665f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(0xFF62BA52);
        c.drawPath(path, paint);

        // Campo
        paint.setShader(new LinearGradient(0, h * 0.72f, 0, h, 0xFF62BA52, 0xFF3F8F2E, Shader.TileMode.CLAMP));
        c.drawRect(0, h * 0.72f, w, h, paint);
        paint.setShader(null);
        paint.setColor(0x12FFFFFF);
        for (int i = 0; i < 5; i++) {
            float top = h * (0.74f + i * 0.05f);
            c.drawRect(0, top, w, top + h * 0.022f, paint);
        }

        // Arbustos
        drawBush(c, w * 0.05f, h * 0.715f, w * 0.07f);
        drawBush(c, w * 0.40f, h * 0.705f, w * 0.06f);
        drawBush(c, w * 0.66f, h * 0.715f, w * 0.075f);
        drawBush(c, w * 0.97f, h * 0.705f, w * 0.06f);

        // Trilha onde o Nimo anda
        if (drawPath) {
            float top = groundY - h * 0.014f;
            float bottom = groundY + h * 0.036f;
            rect.set(-w * 0.05f, top, w * 1.05f, bottom);
            paint.setColor(0xFFC49A5A);
            c.drawRoundRect(rect, h * 0.03f, h * 0.03f, paint);
            rect.set(-w * 0.05f, top + h * 0.004f, w * 1.05f, bottom - h * 0.006f);
            paint.setColor(0xFFDDBA80);
            c.drawRoundRect(rect, h * 0.03f, h * 0.03f, paint);
            Random pebbles = new Random(11);
            paint.setColor(0xFFB8905A);
            for (int i = 0; i < 26; i++) {
                float px = pebbles.nextFloat() * w;
                float py = top + h * 0.01f + pebbles.nextFloat() * (bottom - top - h * 0.02f);
                c.drawCircle(px, py, h * (0.002f + pebbles.nextFloat() * 0.0025f), paint);
            }
        }

        // Flores e tufos de grama
        Random r = new Random(42);
        int[] petals = {0xFFFFFFFF, 0xFFFFE066, 0xFFFF8FC7, 0xFFB39DDB};
        for (int i = 0; i < 46; i++) {
            float fx = r.nextFloat() * w;
            float fy = h * 0.74f + r.nextFloat() * h * 0.26f;
            if (drawPath && fy > groundY - h * 0.03f && fy < groundY + h * 0.05f) continue;
            float s = h * (0.004f + r.nextFloat() * 0.003f);
            paint.setColor(petals[r.nextInt(petals.length)]);
            c.drawCircle(fx - s, fy, s, paint);
            c.drawCircle(fx + s, fy, s, paint);
            c.drawCircle(fx, fy - s, s, paint);
            c.drawCircle(fx, fy + s, s, paint);
            paint.setColor(0xFFFFB300);
            c.drawCircle(fx, fy, s * 0.8f, paint);
        }
        paint.setColor(0xFF3E8E30);
        for (int i = 0; i < 70; i++) {
            float gx = r.nextFloat() * w;
            float gy = h * 0.73f + r.nextFloat() * h * 0.27f;
            drawGrassTuft(c, gx, gy, h * (0.010f + r.nextFloat() * 0.008f));
        }
        if (drawPath) {
            paint.setColor(0xFF4FA23E);
            float step = w / 18f;
            for (int i = 0; i <= 18; i++) {
                drawGrassTuft(c, i * step + r.nextFloat() * step * 0.5f, groundY - h * 0.012f, h * 0.012f);
            }
        }
    }

    private void drawSnowCap(Canvas c, float px, float py, float halfW, float capH) {
        path.reset();
        path.moveTo(px, py);
        path.lineTo(px + halfW, py + capH);
        path.lineTo(px + halfW * 0.35f, py + capH * 0.75f);
        path.lineTo(px, py + capH * 1.05f);
        path.lineTo(px - halfW * 0.4f, py + capH * 0.7f);
        path.lineTo(px - halfW, py + capH);
        path.close();
        c.drawPath(path, paint);
    }

    private void drawTree(Canvas c, float x, float baseY, float size) {
        paint.setColor(0xFF7B5234);
        c.drawRect(x - size * 0.07f, baseY - size * 0.45f, x + size * 0.07f, baseY + size * 0.05f, paint);
        paint.setColor(0xFF2F8A3A);
        c.drawCircle(x, baseY - size * 0.62f, size * 0.32f, paint);
        c.drawCircle(x - size * 0.22f, baseY - size * 0.48f, size * 0.24f, paint);
        c.drawCircle(x + size * 0.22f, baseY - size * 0.48f, size * 0.24f, paint);
        paint.setColor(0xFF45A64E);
        c.drawCircle(x - size * 0.08f, baseY - size * 0.70f, size * 0.14f, paint);
    }

    private void drawBush(Canvas c, float x, float y, float s) {
        paint.setColor(0xFF4A9A3C);
        c.drawCircle(x - s * 0.5f, y, s * 0.45f, paint);
        c.drawCircle(x + s * 0.5f, y, s * 0.45f, paint);
        c.drawCircle(x, y - s * 0.25f, s * 0.55f, paint);
        paint.setColor(0xFF5CB84B);
        c.drawCircle(x - s * 0.1f, y - s * 0.4f, s * 0.22f, paint);
    }

    private void drawGrassTuft(Canvas c, float x, float y, float s) {
        path.reset();
        path.moveTo(x - s * 0.5f, y);
        path.lineTo(x - s * 0.3f, y - s);
        path.lineTo(x - s * 0.1f, y);
        path.lineTo(x + s * 0.05f, y - s * 1.3f);
        path.lineTo(x + s * 0.2f, y);
        path.lineTo(x + s * 0.4f, y - s * 0.9f);
        path.lineTo(x + s * 0.55f, y);
        path.close();
        c.drawPath(path, paint);
    }
}
