package com.nimo.game.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.nimo.game.R;

/** Título "NIMO" grande, com contorno e degradê no estilo cartoon. */
public class TitleView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text = "NIMO";
    private float baseSize;
    private int gradientHeight = -1;

    public TitleView(Context context) {
        super(context);
        init();
    }

    public TitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        text = getContext().getString(R.string.menu_title);
        setContentDescription(text);
        float density = getResources().getDisplayMetrics().density;
        baseSize = 96f * density;
        Typeface typeface = Typeface.create("sans-serif-black", Typeface.BOLD);
        for (Paint p : new Paint[]{fill, stroke, outline, shadow}) {
            p.setTypeface(typeface);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(baseSize);
            p.setLetterSpacing(0.06f);
        }
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setColor(0xFFFFFFFF);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeJoin(Paint.Join.ROUND);
        outline.setColor(0xFF6E1238);
        shadow.setColor(0x55000000);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (baseSize * 1.25f);
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float size = baseSize;
        fill.setTextSize(size);
        float measured = fill.measureText(text);
        if (measured > w * 0.92f && measured > 0f) {
            size *= (w * 0.92f) / measured;
        }
        for (Paint p : new Paint[]{fill, stroke, outline, shadow}) {
            p.setTextSize(size);
        }
        stroke.setStrokeWidth(size * 0.16f);
        outline.setStrokeWidth(size * 0.05f);
        if (gradientHeight != h) {
            gradientHeight = h;
            fill.setShader(new LinearGradient(0, h * 0.1f, 0, h * 0.85f,
                    0xFFFF8CC6, 0xFFC2185B, Shader.TileMode.CLAMP));
        }
        float x = w * 0.5f;
        float y = h * 0.5f + size * 0.36f;
        canvas.drawText(text, x + 4f * density, y + 6f * density, shadow);
        canvas.drawText(text, x, y, stroke);
        canvas.drawText(text, x, y, outline);
        canvas.drawText(text, x, y, fill);
    }
}
