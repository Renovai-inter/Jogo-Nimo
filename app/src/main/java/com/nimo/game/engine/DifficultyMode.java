package com.nimo.game.engine;

import android.content.Context;
import android.content.SharedPreferences;

import com.nimo.game.R;

/** Modo de dificuldade escolhido na tela de Configurações. */
public enum DifficultyMode {
    EASY(R.string.mode_easy, R.string.mode_easy_desc, Config.LEVELS_EASY),
    NORMAL(R.string.mode_normal, R.string.mode_normal_desc, Config.LEVELS_NORMAL),
    HARD(R.string.mode_hard, R.string.mode_hard_desc, Config.LEVELS_HARD);

    /** Modo usado quando o jogador ainda não escolheu nenhum. */
    public static final DifficultyMode DEFAULT = HARD;

    public final int nameRes;
    public final int descriptionRes;
    public final LevelSettings[] levels;

    DifficultyMode(int nameRes, int descriptionRes, LevelSettings[] levels) {
        this.nameRes = nameRes;
        this.descriptionRes = descriptionRes;
        this.levels = levels;
    }

    public static DifficultyMode load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs == null) return DEFAULT;
        String saved = prefs.getString(Config.PREF_DIFFICULTY, DEFAULT.name());
        for (DifficultyMode mode : values()) {
            if (mode.name().equals(saved)) return mode;
        }
        return DEFAULT;
    }

    public static void save(Context context, DifficultyMode mode) {
        context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(Config.PREF_DIFFICULTY, mode.name()).apply();
    }
}
