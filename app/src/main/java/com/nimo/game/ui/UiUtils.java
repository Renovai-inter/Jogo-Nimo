package com.nimo.game.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Insets;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.nimo.game.audio.SoundManager;

/** Utilidades de interface compartilhadas pelas telas. */
public final class UiUtils {

    private UiUtils() {
    }

    public interface InsetsCallback {
        void onInsets(int left, int top, int right, int bottom);
    }

    public static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    /** Efeito de "apertar" (encolhe levemente) em botões. */
    @SuppressLint("ClickableViewAccessibility")
    public static void addPressEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    public static void playClick(Context context) {
        SoundManager.get(context).play(SoundManager.CLICK);
    }

    /** Tela cheia imersiva (esconde barras do sistema). */
    @SuppressWarnings("deprecation")
    public static void hideSystemBars(Activity activity) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    /** Informa as margens seguras (barras do sistema e recorte da câmera). */
    @SuppressWarnings("deprecation")
    public static void listenInsets(View root, InsetsCallback callback) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l;
            int t;
            int r;
            int b;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                l = i.left;
                t = i.top;
                r = i.right;
                b = i.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            callback.onInsets(l, t, r, b);
            return insets;
        });
        root.requestApplyInsets();
    }

    /** Aplica as margens seguras como padding extra em uma view. */
    public static void applySafePadding(View root, View target) {
        final int baseL = target.getPaddingLeft();
        final int baseT = target.getPaddingTop();
        final int baseR = target.getPaddingRight();
        final int baseB = target.getPaddingBottom();
        listenInsets(root, (l, t, r, b) -> target.setPadding(baseL + l, baseT + t, baseR + r, baseB + b));
    }
}
