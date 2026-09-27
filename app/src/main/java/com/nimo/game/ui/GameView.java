package com.nimo.game.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.nimo.game.engine.Config;
import com.nimo.game.engine.GameEngine;
import com.nimo.game.engine.GameRenderer;
import com.nimo.game.engine.GameState;
import com.nimo.game.engine.SceneRenderer;

/**
 * SurfaceView com um game loop próprio em uma thread separada.
 * O movimento é baseado em delta time, então a velocidade é igual em qualquer aparelho.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private GameEngine engine;
    private GameRenderer renderer;
    private Thread thread;
    private volatile boolean running;
    private boolean useHardwareCanvas = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
    }

    public void setup(GameEngine engine, GameRenderer renderer) {
        this.engine = engine;
        this.renderer = renderer;
    }

    public void setTopInset(int px) {
        if (renderer != null) renderer.setTopInset(px);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (engine == null || renderer == null) return;
        synchronized (engine) {
            engine.setSize(width, height);
            renderer.onSizeChanged(width, height, engine);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    private void startLoop() {
        if (running || engine == null) return;
        running = true;
        thread = new Thread(this, "NimoGameLoop");
        thread.start();
    }

    private void stopLoop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            boolean retry = true;
            while (retry) {
                try {
                    t.join(500);
                    retry = false;
                } catch (InterruptedException ignored) {
                    // tenta novamente
                }
            }
        }
    }

    @Override
    public void run() {
        SurfaceHolder holder = getHolder();
        long lastTime = System.nanoTime();
        while (running) {
            long frameStart = System.nanoTime();
            float dt = (frameStart - lastTime) / 1_000_000_000f;
            lastTime = frameStart;
            if (dt > Config.MAX_DELTA_TIME) dt = Config.MAX_DELTA_TIME;
            if (dt < 0f) dt = 0f;

            synchronized (engine) {
                if (engine.isReady()) {
                    engine.update(dt);
                    renderer.update(engine.getState() == GameState.PAUSED ? 0f : dt);
                }
            }

            Canvas canvas = null;
            try {
                canvas = lockCanvas(holder);
                if (canvas != null) {
                    synchronized (engine) {
                        if (engine.isReady()) {
                            renderer.draw(canvas, engine);
                        } else {
                            canvas.drawColor(SceneRenderer.SKY_COLOR);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Superfície pode ter sido destruída durante o desenho.
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {
                        // ignora
                    }
                }
            }

            long elapsed = System.nanoTime() - frameStart;
            long sleepMs = (Config.TARGET_FRAME_NANOS - elapsed) / 1_000_000L;
            if (sleepMs > 1) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        }
    }

    private Canvas lockCanvas(SurfaceHolder holder) {
        if (useHardwareCanvas && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return holder.lockHardwareCanvas();
            } catch (Exception e) {
                useHardwareCanvas = false;
            }
        }
        return holder.lockCanvas();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (engine == null || !engine.getTilt().isTouchControlEnabled()) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                engine.getTilt().setTouch(true, event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                engine.getTilt().setTouch(false, 0f);
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                engine.getTilt().setTouch(false, 0f);
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
