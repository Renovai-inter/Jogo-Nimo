package com.nimo.game.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.nimo.game.R;
import com.nimo.game.audio.SoundManager;
import com.nimo.game.engine.Config;
import com.nimo.game.engine.GameEngine;
import com.nimo.game.engine.GameRenderer;
import com.nimo.game.engine.GameState;
import com.nimo.game.engine.TiltController;

/** Tela da partida: hospeda o GameView e as sobreposições de pausa / fim de jogo. */
public class GameActivity extends Activity implements GameEngine.Listener {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private GameEngine engine;
    private TiltController tilt;
    private SoundManager sound;
    private GameView gameView;

    private View btnPause;
    private View overlayPause;
    private View pauseCard;
    private View overlayEnd;
    private View endCard;
    private TextView endTitle;
    private TextView endScore;
    private TextView endCollected;
    private TextView endErrors;
    private Button btnEndPrimary;
    private ObjectAnimator titlePulse;
    private boolean leaving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_game);
        UiUtils.hideSystemBars(this);

        sound = SoundManager.get(this);
        tilt = new TiltController(this);
        engine = new GameEngine(this, tilt, sound);
        engine.setListener(this);

        gameView = findViewById(R.id.game_view);
        gameView.setup(engine, new GameRenderer(this));

        btnPause = findViewById(R.id.btn_pause);
        overlayPause = findViewById(R.id.overlay_pause);
        pauseCard = findViewById(R.id.pause_card);
        overlayEnd = findViewById(R.id.overlay_end);
        endCard = findViewById(R.id.end_card);
        endTitle = findViewById(R.id.end_title);
        endScore = findViewById(R.id.end_score);
        endCollected = findViewById(R.id.end_collected);
        endErrors = findViewById(R.id.end_errors);
        btnEndPrimary = findViewById(R.id.btn_end_primary);

        View btnContinue = findViewById(R.id.btn_continue);
        View btnRestart = findViewById(R.id.btn_restart);
        View btnPauseMenu = findViewById(R.id.btn_pause_menu);
        View btnEndMenu = findViewById(R.id.btn_end_menu);

        UiUtils.addPressEffect(btnPause);
        UiUtils.addPressEffect(btnContinue);
        UiUtils.addPressEffect(btnRestart);
        UiUtils.addPressEffect(btnPauseMenu);
        UiUtils.addPressEffect(btnEndPrimary);
        UiUtils.addPressEffect(btnEndMenu);

        btnPause.setOnClickListener(v -> {
            UiUtils.playClick(this);
            engine.pause();
        });
        btnContinue.setOnClickListener(v -> {
            UiUtils.playClick(this);
            engine.resume();
        });
        btnRestart.setOnClickListener(v -> {
            UiUtils.playClick(this);
            engine.restart();
        });
        btnPauseMenu.setOnClickListener(v -> {
            UiUtils.playClick(this);
            goToMenu();
        });
        btnEndPrimary.setOnClickListener(v -> {
            UiUtils.playClick(this);
            engine.restart();
        });
        btnEndMenu.setOnClickListener(v -> {
            UiUtils.playClick(this);
            goToMenu();
        });

        // Margens seguras: HUD e botão de pausa abaixo da barra de status / recorte da câmera.
        final int baseMargin = (int) UiUtils.dp(this, 10);
        UiUtils.listenInsets(findViewById(R.id.game_root), (l, t, r, b) -> {
            gameView.setTopInset(t);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) btnPause.getLayoutParams();
            lp.topMargin = t + baseMargin;
            lp.setMarginEnd(r + baseMargin);
            btnPause.setLayoutParams(lp);
        });
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onResume() {
        super.onResume();
        UiUtils.hideSystemBars(this);
        tilt.register();
        if (engine.getState() == GameState.PLAYING) {
            sound.resumeAll();
        }
    }

    @Override
    protected void onPause() {
        // Ao sair do app, a partida é pausada automaticamente.
        engine.pause();
        tilt.unregister();
        sound.pauseAll();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        engine.setListener(null);
        mainHandler.removeCallbacksAndMessages(null);
        if (titlePulse != null) titlePulse.cancel();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiUtils.hideSystemBars(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        GameState state = engine.getState();
        if (state == GameState.PLAYING) {
            engine.pause();
        } else if (state == GameState.PAUSED) {
            engine.resume();
        } else {
            goToMenu();
        }
    }

    // ------------------------------------------------------------------ estados

    @Override
    public void onGameStateChanged(GameState state) {
        // Chamado na thread do jogo: a interface só pode ser alterada na thread principal.
        mainHandler.post(() -> applyState(state));
    }

    private void applyState(GameState state) {
        if (isFinishing() || leaving) return;
        switch (state) {
            case PLAYING:
                hideOverlay(overlayPause);
                hideOverlay(overlayEnd);
                stopTitlePulse();
                btnPause.setVisibility(View.VISIBLE);
                sound.resumeAll();
                break;
            case PAUSED:
                hideOverlay(overlayEnd);
                btnPause.setVisibility(View.INVISIBLE);
                sound.pauseAll();
                showOverlay(overlayPause, pauseCard);
                break;
            case GAME_OVER:
                showEnd(false);
                break;
            case VICTORY:
                showEnd(true);
                break;
            case MENU:
            case TUTORIAL:
            default:
                break;
        }
    }

    private void showEnd(boolean victory) {
        hideOverlay(overlayPause);
        btnPause.setVisibility(View.INVISIBLE);

        int score = victory ? Config.MAX_SCORE : engine.getScore();
        endTitle.setText(victory ? R.string.victory_title : R.string.game_over_title);
        endTitle.setTextColor(getColor(victory ? R.color.green_dark : R.color.red));
        endScore.setText(getString(R.string.result_score, score));
        endCollected.setText(getString(R.string.result_collected, engine.getCollected()));
        if (victory) {
            endErrors.setVisibility(View.GONE);
            btnEndPrimary.setText(R.string.btn_play_again);
            btnEndPrimary.setBackgroundResource(R.drawable.bg_button_green);
        } else {
            endErrors.setVisibility(View.VISIBLE);
            endErrors.setText(getString(R.string.result_errors, engine.getErrors(), Config.MAX_ERRORS));
            btnEndPrimary.setText(R.string.btn_try_again);
            btnEndPrimary.setBackgroundResource(R.drawable.bg_button_orange);
        }

        // Pequeno atraso para o jogador ver o último acontecimento (e o confete na vitória).
        mainHandler.postDelayed(() -> {
            GameState s = engine.getState();
            if (leaving || (s != GameState.GAME_OVER && s != GameState.VICTORY)) return;
            showOverlay(overlayEnd, endCard);
            if (victory) startTitlePulse();
        }, victory ? 700 : 450);
    }

    private void showOverlay(View overlay, View card) {
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(180).start();
        card.setScaleX(0.6f);
        card.setScaleY(0.6f);
        card.animate().scaleX(1f).scaleY(1f).setDuration(360)
                .setInterpolator(new OvershootInterpolator(1.8f)).start();
    }

    private void hideOverlay(View overlay) {
        if (overlay.getVisibility() != View.VISIBLE) return;
        overlay.animate().cancel();
        overlay.setVisibility(View.GONE);
    }

    private void startTitlePulse() {
        stopTitlePulse();
        titlePulse = ObjectAnimator.ofFloat(endTitle, View.ROTATION, -4f, 4f);
        titlePulse.setDuration(420);
        titlePulse.setRepeatMode(ValueAnimator.REVERSE);
        titlePulse.setRepeatCount(ValueAnimator.INFINITE);
        titlePulse.start();
    }

    private void stopTitlePulse() {
        if (titlePulse != null) {
            titlePulse.cancel();
            titlePulse = null;
        }
        endTitle.setRotation(0f);
    }

    private void goToMenu() {
        if (leaving) return;
        leaving = true;
        engine.exitToMenu();
        sound.pauseAll();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
