package com.nimo.game.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.nimo.game.R;
import com.nimo.game.engine.Config;
import com.nimo.game.engine.DifficultyMode;

/** Tela inicial (estado MENU). */
public class MainActivity extends Activity {

    private static final int[] MODE_COLORS = {R.color.green_dark, R.color.orange_dark, R.color.red};

    private ObjectAnimator nimoBob;
    private View overlaySettings;
    private View settingsCard;
    private final View[] optionViews = new View[DifficultyMode.values().length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiUtils.hideSystemBars(this);
        UiUtils.applySafePadding(findViewById(R.id.menu_root), findViewById(R.id.menu_content));

        View title = findViewById(R.id.menu_title);
        View subtitle = findViewById(R.id.menu_subtitle);
        View nimo = findViewById(R.id.menu_nimo);
        View play = findViewById(R.id.btn_play);
        View howTo = findViewById(R.id.btn_how_to_play);
        View settings = findViewById(R.id.btn_settings);

        // Animação de entrada do título.
        title.setScaleX(0.4f);
        title.setScaleY(0.4f);
        title.setAlpha(0f);
        title.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
                .setInterpolator(new OvershootInterpolator(2.2f)).start();
        subtitle.setAlpha(0f);
        subtitle.animate().alpha(1f).setStartDelay(300).setDuration(500).start();
        play.setTranslationY(UiUtils.dp(this, 40));
        play.setAlpha(0f);
        play.animate().translationY(0f).alpha(1f).setStartDelay(420).setDuration(450)
                .setInterpolator(new OvershootInterpolator()).start();
        howTo.setTranslationY(UiUtils.dp(this, 40));
        howTo.setAlpha(0f);
        howTo.animate().translationY(0f).alpha(1f).setStartDelay(520).setDuration(450)
                .setInterpolator(new OvershootInterpolator()).start();
        settings.setTranslationY(UiUtils.dp(this, 40));
        settings.setAlpha(0f);
        settings.animate().translationY(0f).alpha(1f).setStartDelay(620).setDuration(450)
                .setInterpolator(new OvershootInterpolator()).start();

        // Nimo "respirando"/pulando levemente.
        nimoBob = ObjectAnimator.ofFloat(nimo, View.TRANSLATION_Y, 0f, -UiUtils.dp(this, 12));
        nimoBob.setDuration(900);
        nimoBob.setRepeatMode(ValueAnimator.REVERSE);
        nimoBob.setRepeatCount(ValueAnimator.INFINITE);
        nimoBob.setInterpolator(new AccelerateDecelerateInterpolator());

        UiUtils.addPressEffect(play);
        UiUtils.addPressEffect(howTo);
        UiUtils.addPressEffect(settings);

        play.setOnClickListener(v -> {
            UiUtils.playClick(this);
            SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
            if (prefs.getBoolean(Config.PREF_TUTORIAL_SEEN, false)) {
                startActivity(new Intent(this, GameActivity.class));
            } else {
                startActivity(new Intent(this, TutorialActivity.class));
            }
        });
        howTo.setOnClickListener(v -> {
            UiUtils.playClick(this);
            startActivity(new Intent(this, TutorialActivity.class));
        });
        settings.setOnClickListener(v -> {
            UiUtils.playClick(this);
            openSettings();
        });

        setupSettings();
    }

    // ------------------------------------------------------------------ configurações

    private void setupSettings() {
        overlaySettings = findViewById(R.id.overlay_settings);
        settingsCard = findViewById(R.id.settings_card);
        LinearLayout options = findViewById(R.id.settings_options);
        LayoutInflater inflater = LayoutInflater.from(this);
        DifficultyMode[] modes = DifficultyMode.values();
        for (int i = 0; i < modes.length; i++) {
            final DifficultyMode mode = modes[i];
            View option = inflater.inflate(R.layout.item_settings_option, options, false);
            TextView title = option.findViewById(R.id.option_title);
            TextView description = option.findViewById(R.id.option_description);
            title.setText(mode.nameRes);
            title.setTextColor(getColor(MODE_COLORS[i]));
            description.setText(mode.descriptionRes);
            option.setOnClickListener(v -> {
                UiUtils.playClick(this);
                // Salvo na hora: vale a partir da próxima partida.
                DifficultyMode.save(this, mode);
                selectMode(mode);
            });
            UiUtils.addPressEffect(option);
            options.addView(option);
            optionViews[i] = option;
        }

        View ok = findViewById(R.id.btn_settings_ok);
        UiUtils.addPressEffect(ok);
        ok.setOnClickListener(v -> {
            UiUtils.playClick(this);
            closeSettings();
        });
        // Tocar fora do cartão também fecha.
        overlaySettings.setOnClickListener(v -> closeSettings());
        settingsCard.setClickable(true);
    }

    private void openSettings() {
        selectMode(DifficultyMode.load(this));
        overlaySettings.setVisibility(View.VISIBLE);
        overlaySettings.setAlpha(0f);
        overlaySettings.animate().alpha(1f).setDuration(180).start();
        settingsCard.setScaleX(0.6f);
        settingsCard.setScaleY(0.6f);
        settingsCard.animate().scaleX(1f).scaleY(1f).setDuration(360)
                .setInterpolator(new OvershootInterpolator(1.8f)).start();
    }

    private void closeSettings() {
        overlaySettings.animate().cancel();
        overlaySettings.setVisibility(View.GONE);
    }

    /** Destaca o modo escolhido na lista. */
    private void selectMode(DifficultyMode mode) {
        DifficultyMode[] modes = DifficultyMode.values();
        for (int i = 0; i < modes.length; i++) {
            View option = optionViews[i];
            boolean selected = modes[i] == mode;
            option.setSelected(selected);
            ImageView check = option.findViewById(R.id.option_check);
            check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            String name = getString(modes[i].nameRes);
            option.setContentDescription(selected ? getString(R.string.cd_mode_selected, name)
                    : name + ". " + getString(modes[i].descriptionRes));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (overlaySettings != null && overlaySettings.getVisibility() == View.VISIBLE) {
            closeSettings();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiUtils.hideSystemBars(this);
        if (nimoBob != null && !nimoBob.isStarted()) nimoBob.start();
        else if (nimoBob != null) nimoBob.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nimoBob != null) nimoBob.pause();
    }

    @Override
    protected void onDestroy() {
        if (nimoBob != null) nimoBob.cancel();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiUtils.hideSystemBars(this);
    }
}
