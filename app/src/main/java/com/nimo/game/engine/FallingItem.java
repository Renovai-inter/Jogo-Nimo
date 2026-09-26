package com.nimo.game.engine;

import android.graphics.RectF;

/** Um produto caindo. Os objetos são reutilizados (pool) para evitar alocações. */
public class FallingItem {
    public ItemType type;
    /** Centro do produto em pixels. */
    public float x;
    public float y;
    /** Velocidade de queda em pixels por segundo. */
    public float speed;
    public float size;
    public float wobblePhase;
    public boolean active;

    public void init(ItemType type, float x, float y, float speed, float size, float phase) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.size = size;
        this.wobblePhase = phase;
        this.active = true;
    }

    public void update(float dt) {
        y += speed * dt;
        wobblePhase += dt * 3f;
    }

    public float getBottom() {
        return y + size * 0.5f;
    }

    public float getRotation() {
        return (float) Math.sin(wobblePhase) * Config.ITEM_WOBBLE_DEGREES;
    }

    /** Área de colisão (um pouco menor que a imagem, para ser justa). */
    public void getHitRect(RectF out) {
        float half = size * 0.5f * Config.ITEM_HITBOX_SCALE;
        out.set(x - half, y - half, x + half, y + half);
    }

    /** Segundos até o centro do produto alcançar a linha informada. */
    public float timeToReach(float lineY) {
        return (lineY - y) / speed;
    }
}
