package com.nimo.game.engine;

import android.graphics.RectF;

/** Testes de colisão entre o Nimo e os produtos, e dos produtos com o chão. */
public class CollisionManager {

    private final RectF catchRect = new RectF();
    private final RectF itemRect = new RectF();

    /** Verdadeiro quando o produto toca a área de captura (cabeça + cesta) do Nimo. */
    public boolean isCaught(Nimo nimo, FallingItem item) {
        nimo.getCatchRect(catchRect);
        item.getHitRect(itemRect);
        return RectF.intersects(catchRect, itemRect);
    }

    public boolean hasHitGround(FallingItem item, float groundY) {
        return item.getBottom() >= groundY;
    }
}
