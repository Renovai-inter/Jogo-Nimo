package com.nimo.game.engine;

/**
 * Define o nível atual (Fácil, Médio, Difícil) a partir da pontuação e interpola
 * os parâmetros dentro de cada nível para que a dificuldade suba gradualmente.
 */
public class DifficultyManager {

    private LevelSettings[] levels = DifficultyMode.DEFAULT.levels;
    private DifficultyMode mode = DifficultyMode.DEFAULT;
    private int levelIndex;
    private float progress;

    public void setMode(DifficultyMode mode) {
        this.mode = mode;
        this.levels = mode.levels;
    }

    public DifficultyMode getMode() {
        return mode;
    }

    public void update(int score) {
        int index = 0;
        for (int i = 0; i < levels.length; i++) {
            if (score >= levels[i].minScore) {
                index = i;
            }
        }
        levelIndex = index;
        LevelSettings level = levels[index];
        float range = Math.max(1, level.maxScore - level.minScore);
        progress = clamp01((score - level.minScore) / range);
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public LevelSettings getLevel() {
        return levels[levelIndex];
    }

    /** Velocidade de queda em pixels por segundo. */
    public float getFallSpeed(float screenHeight) {
        LevelSettings l = getLevel();
        return lerp(l.fallSpeedStart, l.fallSpeedEnd, progress) * screenHeight * Config.ITEM_FALL_SPEED;
    }

    public float getSpawnInterval() {
        LevelSettings l = getLevel();
        return lerp(l.spawnIntervalStart, l.spawnIntervalEnd, progress) * Config.SPAWN_INTERVAL;
    }

    public int getMaxItems() {
        LevelSettings l = getLevel();
        return Math.round(lerp(l.maxItemsStart, l.maxItemsEnd, progress));
    }

    public float getFairSpeedFactor() {
        return getLevel().fairSpeedFactor;
    }

    public float getWrongRatio() {
        return getLevel().wrongRatio;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
