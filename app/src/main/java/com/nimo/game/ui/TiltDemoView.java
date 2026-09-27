package com.nimo.game.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.nimo.game.R;

/** Animação do tutorial: um celular inclinando e o Nimo andando para o mesmo lado. */
public class TiltDemoView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path arrow = new Path();
    private Bitmap nimo;
    private float time;
    private long lastFrame;

    public TiltDemoView(Context context) {
        super(context);
        init();
    }

    public TiltDemoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TiltDemoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        nimo = BitmapFactory.decodeResource(getResources(), R.drawable.nimo, options);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = lastFrame == 0L ? 0f : Math.min(0.05f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        time += dt;

        float w = getWidth();
        float h = getHeight();
        float d = getResources().getDisplayMetrics().density;
        float s = (float) Math.sin(time * 1.7f);

        // Celular inclinando
        float cx = w * 0.20f;
        float cy = h * 0.5f;
        float ph = h * 0.74f;
        float pw = ph * 0.52f;
        canvas.save();
        canvas.rotate(s * 24f, cx, cy);
        paint.setColor(0xFF37474F);
        rect.set(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2);
        canvas.drawRoundRect(rect, 10f * d, 10f * d, paint);
        paint.setColor(0xFF90CAF9);
        rect.set(cx - pw / 2 + 5f * d, cy - ph / 2 + 12f * d, cx + pw / 2 - 5f * d, cy + ph / 2 - 14f * d);
        canvas.drawRoundRect(rect, 4f * d, 4f * d, paint);
        paint.setColor(0xFF7CCB6B);
        rect.set(rect.left, rect.bottom - (rect.height() * 0.3f), rect.right, rect.bottom);
        canvas.drawRect(rect, paint);
        paint.setColor(0xFFB0BEC5);
        canvas.drawCircle(cx, cy + ph / 2 - 7f * d, 3f * d, paint);
        canvas.restore();

        // Pista e Nimo
        float trackL = w * 0.40f;
        float trackR = w * 0.97f;
        float groundY = h * 0.90f;
        paint.setColor(0xFFDDBA80);
        rect.set(trackL, groundY - 3f * d, trackR, groundY + 5f * d);
        canvas.drawRoundRect(rect, 4f * d, 4f * d, paint);

        float nimoH = h * 0.72f;
        float nimoW = nimo != null ? nimoH * nimo.getWidth() / (float) nimo.getHeight() : nimoH * 0.76f;
        float range = (trackR - trackL - nimoW) * 0.5f;
        float centerX = (trackL + trackR) * 0.5f + s * range;
        rect.set(centerX - nimoW / 2, groundY - nimoH, centerX + nimoW / 2, groundY);
        if (nimo != null) {
            canvas.save();
            canvas.rotate(s * 6f, centerX, groundY);
            canvas.drawBitmap(nimo, null, rect, bitmapPaint);
            canvas.restore();
        }

        // Setas indicando a direção
        float arrowY = h * 0.14f;
        drawArrow(canvas, trackL + 14f * d, arrowY, -1, s < -0.2f, d);
        drawArrow(canvas, trackR - 14f * d, arrowY, 1, s > 0.2f, d);

        postInvalidateOnAnimation();
    }

    private void drawArrow(Canvas canvas, float x, float y, int dir, boolean active, float d) {
        paint.setColor(active ? 0xFFE0407F : 0x55E0407F);
        float size = 11f * d;
        arrow.reset();
        arrow.moveTo(x + dir * size, y);
        arrow.lineTo(x - dir * size * 0.6f, y - size);
        arrow.lineTo(x - dir * size * 0.6f, y + size);
        arrow.close();
        canvas.drawPath(arrow, paint);
    }
}
