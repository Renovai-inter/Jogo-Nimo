package com.nimo.game.engine;

/**
 * Parâmetros de um nível de dificuldade. Os valores "start" valem no início do nível e os
 * valores "end" no final; entre eles a dificuldade cresce gradualmente com a pontuação.
 */
public final class LevelSettings {
    public final int nameRes;
    public final int minScore;
    public final int maxScore;
    /** Velocidade de queda em alturas de tela por segundo. */
    public final float fallSpeedStart;
    public final float fallSpeedEnd;
    /** Intervalo entre surgimentos, em segundos. */
    public final float spawnIntervalStart;
    public final float spawnIntervalEnd;
    public final int maxItemsStart;
    public final int maxItemsEnd;
    /** Proporção de produtos errados (0 a 1). */
    public final float wrongRatio;
    /**
     * Fração da velocidade máxima do Nimo que o spawn considera "alcançável" (fair play).
     * Sempre menor que 1: nenhum produto exige mais do que o Nimo consegue fazer.
     */
    public final float fairSpeedFactor;
    public final int color;

    public LevelSettings(int nameRes, int minScore, int maxScore,
                         float fallSpeedStart, float fallSpeedEnd,
                         float spawnIntervalStart, float spawnIntervalEnd,
                         int maxItemsStart, int maxItemsEnd,
                         float wrongRatio, float fairSpeedFactor, int color) {
        this.nameRes = nameRes;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.fallSpeedStart = fallSpeedStart;
        this.fallSpeedEnd = fallSpeedEnd;
        this.spawnIntervalStart = spawnIntervalStart;
        this.spawnIntervalEnd = spawnIntervalEnd;
        this.maxItemsStart = maxItemsStart;
        this.maxItemsEnd = maxItemsEnd;
        this.wrongRatio = wrongRatio;
        this.fairSpeedFactor = fairSpeedFactor;
        this.color = color;
    }
}
