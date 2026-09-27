package com.nimo.game.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nimo.game.R;
import com.nimo.game.engine.Config;
import com.nimo.game.engine.ItemType;
import com.nimo.game.engine.TiltController;

/** Tutorial exibido antes da primeira partida (estado TUTORIAL). */
public class TutorialActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        UiUtils.hideSystemBars(this);
        UiUtils.applySafePadding(findViewById(R.id.tutorial_root), findViewById(R.id.tutorial_content));

        LinearLayout rowCorrect = findViewById(R.id.row_correct);
        LinearLayout rowWrong = findViewById(R.id.row_wrong);
        LayoutInflater inflater = LayoutInflater.from(this);
        int delay = 0;
        for (ItemType type : ItemType.values()) {
            LinearLayout row = type.isCorrect() ? rowCorrect : rowWrong;
            View cell = inflater.inflate(R.layout.item_tutorial_product, row, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cell.setLayoutParams(lp);

            View slot = cell.findViewById(R.id.product_slot);
            ImageView image = cell.findViewById(R.id.product_image);
            ImageView badge = cell.findViewById(R.id.product_badge);
            TextView name = cell.findViewById(R.id.product_name);
            TextView points = cell.findViewById(R.id.product_points);

            image.setImageResource(type.drawableRes);
            name.setText(type.nameRes);
            image.setContentDescription(getString(type.nameRes));
            if (type.isCorrect()) {
                slot.setBackgroundResource(R.drawable.bg_slot_correct);
                badge.setImageResource(R.drawable.ic_badge_check);
                points.setText(getString(R.string.item_points, type.points));
                points.setTextColor(getColor(R.color.green_dark));
            } else {
                slot.setBackgroundResource(R.drawable.bg_slot_wrong);
                badge.setImageResource(R.drawable.ic_badge_x);
                points.setText(R.string.item_avoid);
                points.setTextColor(getColor(R.color.red));
            }
            row.addView(cell);

            // Pequena animação de entrada dos itens.
            cell.setScaleX(0.3f);
            cell.setScaleY(0.3f);
            cell.setAlpha(0f);
            cell.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(150 + delay)
                    .setDuration(380).start();
            delay += 90;
        }

        if (!TiltController.deviceHasAccelerometer(this)) {
            findViewById(R.id.tutorial_no_sensor).setVisibility(View.VISIBLE);
        }

        View start = findViewById(R.id.btn_start);
        UiUtils.addPressEffect(start);
        start.setOnClickListener(v -> {
            UiUtils.playClick(this);
            getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE).edit()
                    .putBoolean(Config.PREF_TUTORIAL_SEEN, true).apply();
            startActivity(new Intent(this, GameActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiUtils.hideSystemBars(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiUtils.hideSystemBars(this);
    }
}
