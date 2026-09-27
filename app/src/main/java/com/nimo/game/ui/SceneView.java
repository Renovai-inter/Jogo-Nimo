package com.nimo.game.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.nimo.game.engine.SceneRenderer;

/** Cenário animado (nuvens em movimento) usado como fundo do menu e do tutorial. */
public class SceneView extends View {

    private final SceneRenderer scene = new SceneRenderer(false);
    private long lastFrame;

    public SceneView(Context context) {
        super(context);
    }

    public SceneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SceneView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scene.setSize(w, h, h * 0.9f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = lastFrame == 0L ? 0f : Math.min(0.05f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        scene.update(dt);
        scene.draw(canvas);
        if (isAttachedToWindow() && getWindowVisibility() == VISIBLE) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            lastFrame = 0L;
            invalidate();
        }
    }
}
