package com.nimo.game.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.nimo.game.R;

/**
 * Efeitos sonoros simples. Se algum som não puder ser carregado o jogo continua
 * funcionando normalmente, apenas sem aquele som.
 */
public final class SoundManager {

    public static final int CLICK = 0;
    public static final int COLLECT = 1;
    public static final int ERROR = 2;
    public static final int DROP = 3;
    public static final int VICTORY = 4;

    private static final int[] SOUND_RES = {
            R.raw.snd_click,
            R.raw.snd_collect,
            R.raw.snd_error,
            R.raw.snd_drop,
            R.raw.snd_victory
    };
    private static final float VOLUME = 0.8f;

    private static SoundManager instance;

    private SoundPool soundPool;
    private final int[] soundIds = new int[SOUND_RES.length];
    private final boolean[] loaded = new boolean[SOUND_RES.length];
    private volatile boolean enabled = true;

    public static synchronized SoundManager get(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    private SoundManager(Context context) {
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(attributes)
                    .build();
            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status != 0) return;
                synchronized (loaded) {
                    for (int i = 0; i < soundIds.length; i++) {
                        if (soundIds[i] == sampleId) {
                            loaded[i] = true;
                        }
                    }
                }
            });
            for (int i = 0; i < SOUND_RES.length; i++) {
                try {
                    soundIds[i] = soundPool.load(context, SOUND_RES[i], 1);
                } catch (Exception e) {
                    soundIds[i] = 0;
                }
            }
        } catch (Exception e) {
            soundPool = null;
        }
    }

    public void play(int sound) {
        SoundPool pool = soundPool;
        if (pool == null || !enabled || sound < 0 || sound >= soundIds.length) return;
        boolean ready;
        synchronized (loaded) {
            ready = loaded[sound];
        }
        if (ready) {
            try {
                pool.play(soundIds[sound], VOLUME, VOLUME, 1, 0, 1f);
            } catch (Exception ignored) {
                // Som é opcional.
            }
        }
    }

    public void pauseAll() {
        if (soundPool != null) soundPool.autoPause();
    }

    public void resumeAll() {
        if (soundPool != null) soundPool.autoResume();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
