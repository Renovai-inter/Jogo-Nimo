package com.nimo.game.engine;

import android.graphics.RectF;

/** O jogador: Nimo, o guaxinim rosa com sua cesta. */
public class Nimo {

    /** Altura / largura da imagem do Nimo. */
    public static final float ASPECT = 420f / 320f;
    /** Posição horizontal (fração da largura) do centro da cesta, usado como "x" do Nimo. */
    public static final float ANCHOR_X = 0.64f;
    // Área de captura (cabeça + cesta) em frações da imagem.
    public static final float CATCH_LEFT = 0.34f;
    public static final float CATCH_RIGHT = 0.94f;
    public static final float CATCH_TOP = 0.30f;
    public static final float CATCH_BOTTOM = 0.84f;

    private static final float CATCH_ANIM_TIME = 0.25f;
    private static final float HURT_ANIM_TIME = 0.6f;

    private float x;
    private float vx;
    private float width;
    private float height;
    private float bottomY;
    private float maxSpeed;
    private float minX;
    private float maxX;
    private float walkPhase;
    private float catchTimer;
    private float hurtTimer;
    private float celebrateTime;
    private boolean celebrating;

    public void layout(int screenWidth, float playerWidth, float groundY, float maxSpeedPx) {
        width = playerWidth;
        height = playerWidth * ASPECT;
        bottomY = groundY + height * 0.03f;
        maxSpeed = maxSpeedPx;
        minX = ANCHOR_X * width;
        maxX = screenWidth - (1f - ANCHOR_X) * width;
        if (x == 0f) {
            x = screenWidth * 0.5f;
        }
        x = clamp(x, minX, maxX);
    }

    public void reset(int screenWidth) {
        x = clamp(screenWidth * 0.5f, minX, maxX);
        vx = 0f;
        walkPhase = 0f;
        catchTimer = 0f;
        hurtTimer = 0f;
        celebrateTime = 0f;
        celebrating = false;
    }

    /**
     * @param moveFactor -1 (esquerda, velocidade máxima) .. 0 (parado) .. 1 (direita).
     */
    public void update(float dt, float moveFactor) {
        float target = moveFactor * maxSpeed;
        float k = Math.min(1f, Config.PLAYER_ACCELERATION * dt);
        vx += (target - vx) * k;
        if (Math.abs(moveFactor) < 0.001f && Math.abs(vx) < maxSpeed * 0.02f) {
            vx = 0f;
        }
        x += vx * dt;
        if (x < minX) {
            x = minX;
            if (vx < 0f) vx = 0f;
        } else if (x > maxX) {
            x = maxX;
            if (vx > 0f) vx = 0f;
        }
        walkPhase += Math.abs(vx) / Math.max(1f, width) * dt * 9f;
        tickTimers(dt);
    }

    public void celebrate(float dt) {
        celebrating = true;
        celebrateTime += dt;
        vx = 0f;
        tickTimers(dt);
    }

    private void tickTimers(float dt) {
        if (catchTimer > 0f) catchTimer = Math.max(0f, catchTimer - dt);
        if (hurtTimer > 0f) hurtTimer = Math.max(0f, hurtTimer - dt);
    }

    public void onCatch() {
        catchTimer = CATCH_ANIM_TIME;
    }

    public void onHurt() {
        hurtTimer = HURT_ANIM_TIME;
    }

    public void getCatchRect(RectF out) {
        float left = getLeft();
        float top = getTop();
        out.set(left + CATCH_LEFT * width, top + CATCH_TOP * height,
                left + CATCH_RIGHT * width, top + CATCH_BOTTOM * height);
    }

    public float getCatchHalfWidth() {
        return (CATCH_RIGHT - CATCH_LEFT) * 0.5f * width;
    }

    public float getCatchTopY() {
        return getTop() + CATCH_TOP * height;
    }

    public float getX() {
        return x;
    }

    public float getLeft() {
        return x - ANCHOR_X * width;
    }

    public float getTop() {
        return bottomY - height;
    }

    public float getBottomY() {
        return bottomY;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getMinX() {
        return minX;
    }

    public float getMaxX() {
        return maxX;
    }

    /** Pequena inclinação do corpo conforme a velocidade. */
    public float getTiltDegrees() {
        if (maxSpeed <= 0f) return 0f;
        return vx / maxSpeed * 7f;
    }

    /** Deslocamento vertical (passos ao andar, pulos ao comemorar). */
    public float getBob() {
        if (celebrating) {
            return Math.abs((float) Math.sin(celebrateTime * 6f)) * height * 0.12f;
        }
        if (Math.abs(vx) < 1f) return 0f;
        return Math.abs((float) Math.sin(walkPhase)) * height * 0.025f;
    }

    /** 0..1 — achatamento rápido ao coletar um produto. */
    public float getSquash() {
        if (catchTimer <= 0f) return 0f;
        float k = catchTimer / CATCH_ANIM_TIME;
        return (float) Math.sin(k * Math.PI);
    }

    public boolean isHurtBlink() {
        return hurtTimer > 0f && ((int) (hurtTimer * 14f)) % 2 == 0;
    }

    private static float clamp(float v, float min, float max) {
        if (max < min) return (min + max) * 0.5f;
        return v < min ? min : (v > max ? max : v);
    }
}
