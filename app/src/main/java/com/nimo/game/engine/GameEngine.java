package com.nimo.game.engine;

import android.content.Context;

import com.nimo.game.R;
import com.nimo.game.audio.SoundManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras e estado da partida. Todas as operações públicas são sincronizadas no próprio
 * objeto, que também é usado como trava pelo loop do jogo (GameView).
 */
public class GameEngine {

    public interface Listener {
        /** Chamado (na thread do jogo) quando o estado muda. */
        void onGameStateChanged(GameState state);
    }

    private static final int COLOR_POSITIVE = 0xFF2E9E44;
    private static final int COLOR_NEGATIVE = 0xFFE53935;
    private static final int COLOR_GOLD = 0xFFFFC107;

    private final Nimo nimo = new Nimo();
    private final ArrayList<FallingItem> items = new ArrayList<>();
    private final ArrayList<FallingItem> pool = new ArrayList<>();
    private final DifficultyManager difficulty = new DifficultyManager();
    private final SpawnManager spawnManager = new SpawnManager();
    private final CollisionManager collisionManager = new CollisionManager();
    private final EffectsManager effects = new EffectsManager();
    private final TiltController tilt;
    private final SoundManager sound;
    private final String textWrong;
    private final String textMissed;
    private Listener listener;

    private volatile GameState state = GameState.TUTORIAL;
    private int width;
    private int height;
    private float itemSize;
    private float groundY;
    private boolean started;

    private volatile int score;
    private volatile int collected;
    private volatile int errors;
    private float countdown;
    private float goTimer;
    private float levelBannerTime;
    private float errorPulse;
    private float stateTime;
    private int lastLevelIndex;

    public GameEngine(Context context, TiltController tilt, SoundManager sound) {
        this.tilt = tilt;
        this.sound = sound;
        this.textWrong = context.getString(R.string.float_wrong);
        this.textMissed = context.getString(R.string.float_missed);
        effects.setDensity(context.getResources().getDisplayMetrics().density);
        difficulty.setMode(DifficultyMode.load(context));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // ------------------------------------------------------------------ ciclo de vida

    public synchronized void setSize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        width = w;
        height = h;
        itemSize = Math.min(w * Config.ITEM_SIZE_RATIO, h * Config.ITEM_MAX_HEIGHT_RATIO);
        groundY = h * Config.GROUND_LINE_RATIO;
        float playerWidth = Math.min(w * Config.PLAYER_WIDTH_RATIO,
                h * Config.PLAYER_MAX_HEIGHT_RATIO / Nimo.ASPECT);
        nimo.layout(w, playerWidth, groundY, Config.PLAYER_SPEED * w);
        for (int i = 0; i < items.size(); i++) {
            FallingItem it = items.get(i);
            it.size = itemSize;
            it.x = Math.max(itemSize * 0.5f, Math.min(w - itemSize * 0.5f, it.x));
        }
        if (!started) {
            started = true;
            startNewGame();
        }
    }

    public boolean isReady() {
        return width > 0 && height > 0;
    }

    public synchronized void restart() {
        if (!isReady()) return;
        startNewGame();
    }

    private void startNewGame() {
        recycleAll();
        score = 0;
        collected = 0;
        errors = 0;
        countdown = Config.COUNTDOWN_SECONDS;
        goTimer = 0f;
        levelBannerTime = 0f;
        errorPulse = 0f;
        difficulty.update(0);
        lastLevelIndex = difficulty.getLevelIndex();
        spawnManager.reset();
        effects.clear();
        nimo.reset(width);
        setState(GameState.PLAYING, true);
    }

    public synchronized void pause() {
        if (state == GameState.PLAYING) {
            setState(GameState.PAUSED, false);
        }
    }

    public synchronized void resume() {
        if (state == GameState.PAUSED) {
            setState(GameState.PLAYING, false);
        }
    }

    public synchronized void exitToMenu() {
        recycleAll();
        setState(GameState.MENU, false);
    }

    private void setState(GameState newState, boolean force) {
        if (state == newState && !force) return;
        state = newState;
        stateTime = 0f;
        Listener l = listener;
        if (l != null) {
            l.onGameStateChanged(newState);
        }
    }

    // ------------------------------------------------------------------ atualização

    /** Avança a simulação. dt em segundos (delta time). */
    public synchronized void update(float dt) {
        if (!isReady()) return;
        stateTime += dt;
        switch (state) {
            case PLAYING:
                effects.update(dt);
                updatePlaying(dt);
                break;
            case VICTORY:
                effects.update(dt);
                nimo.celebrate(dt);
                if (stateTime < Config.VICTORY_CONFETTI_SECONDS) {
                    effects.confetti(width, 2);
                }
                break;
            case GAME_OVER:
                effects.update(dt);
                break;
            default:
                // PAUSED, MENU e TUTORIAL: tudo congelado.
                break;
        }
    }

    private void updatePlaying(float dt) {
        if (errorPulse > 0f) errorPulse = Math.max(0f, errorPulse - dt);
        if (levelBannerTime > 0f) levelBannerTime = Math.max(0f, levelBannerTime - dt);
        if (goTimer > 0f) goTimer = Math.max(0f, goTimer - dt);

        nimo.update(dt, tilt.getMoveFactor(nimo.getX(), width));

        if (countdown > 0f) {
            countdown -= dt;
            if (countdown <= 0f) {
                countdown = 0f;
                goTimer = Config.GO_MESSAGE_SECONDS;
            }
            return;
        }

        spawnManager.update(dt, this);

        for (int i = items.size() - 1; i >= 0; i--) {
            if (i >= items.size()) continue;
            FallingItem item = items.get(i);
            item.update(dt);
            if (collisionManager.isCaught(nimo, item)) {
                recycle(i);
                onItemCaught(item);
            } else if (collisionManager.hasHitGround(item, groundY)) {
                recycle(i);
                onItemLanded(item);
            }
            if (state != GameState.PLAYING) return;
        }
    }

    private void onItemCaught(FallingItem item) {
        if (item.type.isCorrect()) {
            score = Math.min(score + item.type.points, Config.MAX_SCORE);
            collected++;
            nimo.onCatch();
            effects.burst(item.x, item.y, COLOR_GOLD, 14, 220f, 6f);
            effects.burst(item.x, item.y, 0xFFFFFFFF, 6, 160f, 4f);
            effects.floatText("+" + item.type.points, item.x, item.y - itemSize * 0.4f, COLOR_POSITIVE, 1.1f);
            sound.play(SoundManager.COLLECT);
            difficulty.update(score);
            if (difficulty.getLevelIndex() != lastLevelIndex) {
                lastLevelIndex = difficulty.getLevelIndex();
                levelBannerTime = Config.LEVEL_BANNER_SECONDS;
            }
            if (score >= Config.MAX_SCORE) {
                onVictory();
            }
        } else {
            effects.burst(item.x, item.y, COLOR_NEGATIVE, 16, 240f, 6f);
            effects.floatText(textWrong, item.x, item.y - itemSize * 0.4f, COLOR_NEGATIVE, 1f);
            nimo.onHurt();
            addError();
        }
    }

    private void onItemLanded(FallingItem item) {
        float y = groundY - itemSize * 0.3f;
        if (item.type.isCorrect()) {
            // Produto certo perdido = erro.
            effects.burst(item.x, y, 0xFF8D6E63, 12, 160f, 5f);
            effects.floatText(textMissed, item.x, y - itemSize * 0.5f, COLOR_NEGATIVE, 1f);
            addError();
        } else {
            // Produto errado no chão = comportamento correto, sem penalidade.
            effects.burst(item.x, y, 0xFFB0A080, 8, 110f, 4f);
            sound.play(SoundManager.DROP);
        }
    }

    private void addError() {
        if (errors >= Config.MAX_ERRORS) return;
        errors++;
        errorPulse = 0.6f;
        effects.flash(0xFFFF1744, 0.35f);
        effects.shake(8f, 0.3f);
        sound.play(SoundManager.ERROR);
        if (errors >= Config.MAX_ERRORS) {
            onGameOver();
        }
    }

    private void onVictory() {
        score = Config.MAX_SCORE;
        for (int i = 0; i < items.size(); i++) {
            FallingItem it = items.get(i);
            effects.burst(it.x, it.y, 0xFFFFFFFF, 8, 150f, 5f);
        }
        recycleAll();
        effects.confetti(width, 60);
        sound.play(SoundManager.VICTORY);
        setState(GameState.VICTORY, false);
    }

    private void onGameOver() {
        for (int i = 0; i < items.size(); i++) {
            FallingItem it = items.get(i);
            effects.burst(it.x, it.y, 0xFFBDBDBD, 6, 120f, 4f);
        }
        recycleAll();
        setState(GameState.GAME_OVER, false);
    }

    // ------------------------------------------------------------------ pool de itens

    FallingItem obtainItem() {
        if (pool.isEmpty()) {
            return new FallingItem();
        }
        return pool.remove(pool.size() - 1);
    }

    void addItem(FallingItem item) {
        items.add(item);
    }

    private void recycle(int index) {
        FallingItem item = items.remove(index);
        item.active = false;
        pool.add(item);
    }

    private void recycleAll() {
        for (int i = items.size() - 1; i >= 0; i--) {
            recycle(i);
        }
    }

    // ------------------------------------------------------------------ getters

    public GameState getState() {
        return state;
    }

    public Nimo getNimo() {
        return nimo;
    }

    public List<FallingItem> getItems() {
        return items;
    }

    public DifficultyManager getDifficulty() {
        return difficulty;
    }

    public EffectsManager getEffects() {
        return effects;
    }

    public TiltController getTilt() {
        return tilt;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getItemSize() {
        return itemSize;
    }

    public float getGroundY() {
        return groundY;
    }

    public int getScore() {
        return score;
    }

    public int getCollected() {
        return collected;
    }

    public int getErrors() {
        return errors;
    }

    public float getCountdown() {
        return countdown;
    }

    public float getGoTimer() {
        return goTimer;
    }

    public float getLevelBannerTime() {
        return levelBannerTime;
    }

    public float getErrorPulse() {
        return errorPulse;
    }
}
