package com.nimo.game.engine;

import com.nimo.game.R;

/**
 * Todas as constantes ajustáveis do jogo em um só lugar.
 * Distâncias e velocidades são proporcionais ao tamanho da tela para funcionar
 * igual em qualquer aparelho.
 */
public final class Config {

    private Config() {
    }

    // ---------------- Partida ----------------
    public static final int MAX_SCORE = 100;
    public static final int MAX_ERRORS = 3;
    public static final float COUNTDOWN_SECONDS = 3f;
    public static final float GO_MESSAGE_SECONDS = 0.8f;
    public static final float LEVEL_BANNER_SECONDS = 1.8f;
    public static final float VICTORY_CONFETTI_SECONDS = 4f;

    // ---------------- Layout (proporções da tela) ----------------
    /** Altura (proporção da tela) da linha do chão onde o Nimo pisa. */
    public static final float GROUND_LINE_RATIO = 0.90f;
    /** Largura do Nimo em relação à largura da tela. */
    public static final float PLAYER_WIDTH_RATIO = 0.27f;
    /** Altura máxima do Nimo em relação à altura da tela (telas largas/tablets). */
    public static final float PLAYER_MAX_HEIGHT_RATIO = 0.21f;
    /** Tamanho dos produtos em relação à largura da tela. */
    public static final float ITEM_SIZE_RATIO = 0.14f;
    public static final float ITEM_MAX_HEIGHT_RATIO = 0.075f;

    // ---------------- Jogador ----------------
    /** Velocidade máxima do Nimo, em larguras de tela por segundo. */
    public static final float PLAYER_SPEED = 1.3f;
    /** Quão rápido o Nimo atinge a velocidade desejada (maior = mais responsivo). */
    public static final float PLAYER_ACCELERATION = 10f;

    // ---------------- Acelerômetro ----------------
    /** Zona morta em m/s²: inclinações menores que isso não movem o Nimo. */
    public static final float TILT_DEAD_ZONE = 0.8f;
    /** Inclinação (m/s²) a partir da qual o Nimo anda na velocidade máxima. */
    public static final float TILT_FULL_SPEED = 5.0f;
    /** Multiplicador de sensibilidade (1 = padrão). */
    public static final float TILT_SENSITIVITY = 1.0f;
    /** Filtro passa-baixa do sensor (0..1, menor = mais suave). */
    public static final float TILT_SMOOTHING = 0.2f;
    /** Curva de resposta (>1 deixa inclinações pequenas mais lentas e precisas). */
    public static final float TILT_CURVE = 1.35f;
    /** Se true, o controle por toque também funciona em aparelhos com acelerômetro. */
    public static final boolean ALWAYS_ALLOW_TOUCH = false;
    /** Distância (larguras de tela) entre dedo e Nimo para velocidade máxima no toque. */
    public static final float TOUCH_FULL_SPEED_DISTANCE = 0.12f;

    // ---------------- Produtos ----------------
    /** Multiplicador global da velocidade de queda. */
    public static final float ITEM_FALL_SPEED = 1f;
    public static final float FALL_SPEED_VARIATION = 0.06f;
    public static final float ITEM_HITBOX_SCALE = 0.78f;
    public static final float ITEM_WOBBLE_DEGREES = 10f;

    // ---------------- Spawn ----------------
    /** Multiplicador global do intervalo entre produtos. */
    public static final float SPAWN_INTERVAL = 1f;
    public static final float SPAWN_INTERVAL_JITTER = 0.15f;
    public static final float FIRST_SPAWN_DELAY = 0.4f;
    public static final float SPAWN_RETRY_DELAY = 0.12f;
    /** Distância horizontal mínima entre produtos (em tamanhos de produto). */
    public static final float MIN_HORIZONTAL_DISTANCE = 1.2f;
    /** Distância vertical mínima entre produtos (em tamanhos de produto). */
    public static final float MIN_VERTICAL_DISTANCE = 1.5f;
    public static final int SPAWN_MAX_ATTEMPTS = 14;
    public static final int SPAWN_ZONES = 5;
    public static final float ZONE_REPEAT_PENALTY = 1.5f;
    public static final float ZONE_USAGE_DECAY = 0.55f;
    public static final int TYPE_BAG_SIZE = 12;
    public static final int MAX_WRONG_IN_A_ROW = 2;
    public static final int FIRST_CORRECT_ITEMS = 2;

    // ---------------- Fair play ----------------
    /** Tempo de reação humano considerado (segundos). */
    public static final float REACTION_TIME = 0.25f;
    /** Quanto da cesta pode ser usado como folga ao calcular alcance. */
    public static final float CATCH_TOLERANCE = 0.7f;

    // ---------------- Loop ----------------
    public static final float MAX_DELTA_TIME = 0.05f;
    public static final long TARGET_FRAME_NANOS = 16_666_667L;

    // ---------------- Preferências ----------------
    public static final String PREFS_NAME = "nimo_prefs";
    public static final String PREF_TUTORIAL_SEEN = "tutorial_seen";
    public static final String PREF_DIFFICULTY = "difficulty_mode";

    // ---------------- Níveis ----------------
    // Cada modo de dificuldade (escolhido em Configurações) tem sua própria tabela com os
    // 3 níveis da partida. Dentro de cada nível os valores vão de "início" a "fim"
    // conforme a pontuação sobe. "fair play" = fração da velocidade máxima do Nimo que o
    // spawn considera alcançável (sempre < 1: nada fica impossível).

    /** Modo FÁCIL: produtos mais lentos, mais espaçados e com menos itens errados. */
    public static final LevelSettings[] LEVELS_EASY = {
            //                 nome                   pontos    queda (telas/s)  intervalo (s)  máx itens  % errados  fair play  cor
            new LevelSettings(R.string.level_easy, 0, 30, 0.15f, 0.18f, 2.10f, 1.85f, 2, 3, 0.20f, 0.50f, 0xFF43A047),
            new LevelSettings(R.string.level_medium, 30, 60, 0.19f, 0.22f, 1.75f, 1.50f, 3, 3, 0.22f, 0.50f, 0xFFFB8C00),
            new LevelSettings(R.string.level_hard, 60, 100, 0.23f, 0.27f, 1.45f, 1.25f, 3, 4, 0.25f, 0.52f, 0xFFE53935),
    };

    /** Modo MÉDIO: o balanceamento original do jogo. */
    public static final LevelSettings[] LEVELS_NORMAL = {
            new LevelSettings(R.string.level_easy, 0, 30, 0.19f, 0.23f, 1.75f, 1.45f, 3, 3, 0.25f, 0.70f, 0xFF43A047),
            new LevelSettings(R.string.level_medium, 30, 60, 0.26f, 0.32f, 1.25f, 1.00f, 4, 5, 0.33f, 0.70f, 0xFFFB8C00),
            new LevelSettings(R.string.level_hard, 60, 100, 0.35f, 0.44f, 0.92f, 0.70f, 6, 7, 0.40f, 0.70f, 0xFFE53935),
    };

    /** Modo DIFÍCIL: partida mais longa e nível final muito mais exigente. */
    public static final LevelSettings[] LEVELS_HARD = {
            new LevelSettings(R.string.level_easy, 0, 30, 0.19f, 0.23f, 2.50f, 2.10f, 3, 3, 0.25f, 0.70f, 0xFF43A047),
            new LevelSettings(R.string.level_medium, 30, 60, 0.26f, 0.33f, 1.95f, 1.50f, 4, 5, 0.38f, 0.72f, 0xFFFB8C00),
            new LevelSettings(R.string.level_hard, 60, 100, 0.54f, 0.72f, 0.60f, 0.44f, 8, 11, 0.65f, 0.90f, 0xFFE53935),
    };
}
