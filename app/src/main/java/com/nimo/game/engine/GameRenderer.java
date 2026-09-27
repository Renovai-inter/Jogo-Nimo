package com.nimo.game.engine;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.nimo.game.R;

import java.util.List;

/** Desenha a partida: cenário, produtos, Nimo, efeitos e HUD. */
public class GameRenderer {

    private static final int COLOR_TEXT = 0xFF33264A;
    private static final int COLOR_RED = 0xFFE53935;

    private final Resources res;
    private final float density;
    private final float fontScale;
    private final SceneRenderer scene = new SceneRenderer(true);

    private final Bitmap nimoSource;
    private final Bitmap[] itemSources;
    private Bitmap nimoBitmap;
    private final Bitmap[] itemBitmaps;

    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint hurtPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint xPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bigFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bigStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final String goText;
    private final String hintTilt;
    private final String hintTouch;
    private final String[] levelNames;

    private int cachedScore = -1;
    private int cachedCollected = -1;
    private int cachedErrors = -1;
    private int cachedLevel = -1;
    private String scoreText = "";
    private String collectedText = "";
    private String errorsText = "";
    private String levelText = "";
    private String levelUpText = "";

    private volatile int topInset;

    public GameRenderer(Context context) {
        res = context.getResources();
        density = res.getDisplayMetrics().density;
        fontScale = Math.max(0.85f, Math.min(1.3f, res.getConfiguration().fontScale));

        nimoSource = BitmapFactory.decodeResource(res, R.drawable.nimo);
        ItemType[] types = ItemType.values();
        itemSources = new Bitmap[types.length];
        itemBitmaps = new Bitmap[types.length];
        for (int i = 0; i < types.length; i++) {
            itemSources[i] = BitmapFactory.decodeResource(res, types[i].drawableRes);
        }

        goText = res.getString(R.string.countdown_go);
        hintTilt = res.getString(R.string.hint_tilt);
        hintTouch = res.getString(R.string.hint_touch);
        levelNames = new String[Config.LEVELS_NORMAL.length];
        for (int i = 0; i < levelNames.length; i++) {
            levelNames[i] = res.getString(Config.LEVELS_NORMAL[i].nameRes);
        }

        hurtPaint.setColorFilter(new PorterDuffColorFilter(0x99FF2A2A, PorterDuff.Mode.SRC_ATOP));
        shadowPaint.setColor(0x33000000);
        fallbackPaint.setColor(0xFFE0407F);

        Typeface black = Typeface.create("sans-serif-black", Typeface.BOLD);
        Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        panelPaint.setColor(0xE6FFFFFF);
        panelStroke.setStyle(Paint.Style.STROKE);
        panelStroke.setStrokeWidth(3f * density);
        panelStroke.setColor(0xFFFF7EB6);
        scorePaint.setTypeface(black);
        scorePaint.setColor(COLOR_TEXT);
        smallPaint.setTypeface(medium);
        smallPaint.setColor(COLOR_TEXT);
        pillTextPaint.setTypeface(black);
        pillTextPaint.setColor(0xFFFFFFFF);
        pillTextPaint.setTextAlign(Paint.Align.CENTER);
        barBgPaint.setColor(0xFFE8E1EC);
        barFillPaint.setColor(0xFFFFB300);
        boxStroke.setStyle(Paint.Style.STROKE);
        boxStroke.setStrokeWidth(2.5f * density);
        xPaint.setStyle(Paint.Style.STROKE);
        xPaint.setStrokeCap(Paint.Cap.ROUND);
        xPaint.setColor(COLOR_RED);
        bigFill.setTypeface(black);
        bigFill.setTextAlign(Paint.Align.CENTER);
        bigStroke.setTypeface(black);
        bigStroke.setTextAlign(Paint.Align.CENTER);
        bigStroke.setStyle(Paint.Style.STROKE);
        bigStroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setTopInset(int px) {
        topInset = px;
    }

    /** Deve ser chamado (com a trava do engine) depois de engine.setSize(). */
    public void onSizeChanged(int w, int h, GameEngine engine) {
        scene.setSize(w, h, engine.getGroundY());
        Nimo nimo = engine.getNimo();
        if (nimoSource != null) {
            nimoBitmap = Bitmap.createScaledBitmap(nimoSource,
                    Math.max(1, Math.round(nimo.getWidth())), Math.max(1, Math.round(nimo.getHeight())), true);
        }
        float size = engine.getItemSize();
        for (int i = 0; i < itemSources.length; i++) {
            Bitmap src = itemSources[i];
            if (src == null) continue;
            float scale = size / Math.max(src.getWidth(), src.getHeight());
            itemBitmaps[i] = Bitmap.createScaledBitmap(src,
                    Math.max(1, Math.round(src.getWidth() * scale)),
                    Math.max(1, Math.round(src.getHeight() * scale)), true);
        }
    }

    public void update(float dt) {
        scene.update(dt);
    }

    public void draw(Canvas canvas, GameEngine engine) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        EffectsManager fx = engine.getEffects();

        canvas.drawColor(SceneRenderer.SKY_COLOR);
        canvas.save();
        canvas.translate(fx.getShakeX(), fx.getShakeY());
        scene.draw(canvas);
        drawItems(canvas, engine);
        drawNimo(canvas, engine.getNimo());
        fx.drawParticles(canvas);
        fx.drawTexts(canvas);
        canvas.restore();
        fx.drawFlash(canvas, w, h);

        drawHud(canvas, engine, w);
        drawCenterMessages(canvas, engine, w, h);
    }

    // ------------------------------------------------------------------ mundo

    private void drawItems(Canvas canvas, GameEngine engine) {
        List<FallingItem> items = engine.getItems();
        for (int i = 0; i < items.size(); i++) {
            FallingItem it = items.get(i);
            Bitmap bmp = itemBitmaps[it.type.ordinal()];
            canvas.save();
            canvas.rotate(it.getRotation(), it.x, it.y);
            if (bmp != null) {
                canvas.drawBitmap(bmp, it.x - bmp.getWidth() * 0.5f, it.y - bmp.getHeight() * 0.5f, bitmapPaint);
            } else {
                canvas.drawCircle(it.x, it.y, it.size * 0.4f, fallbackPaint);
            }
            canvas.restore();
        }
    }

    private void drawNimo(Canvas canvas, Nimo nimo) {
        float bottom = nimo.getBottomY();
        float bodyX = nimo.getLeft() + nimo.getWidth() * 0.6f;
        float bob = nimo.getBob();
        float shadowScale = 1f - Math.min(0.4f, bob / Math.max(1f, nimo.getHeight()) * 2f);
        rect.set(bodyX - nimo.getWidth() * 0.34f * shadowScale, bottom - nimo.getHeight() * 0.03f,
                bodyX + nimo.getWidth() * 0.34f * shadowScale, bottom + nimo.getHeight() * 0.03f);
        canvas.drawOval(rect, shadowPaint);

        canvas.save();
        canvas.rotate(nimo.getTiltDegrees(), bodyX, bottom);
        float squash = nimo.getSquash();
        canvas.scale(1f + 0.07f * squash, 1f - 0.09f * squash, bodyX, bottom);
        float top = nimo.getTop() - bob;
        if (nimoBitmap != null) {
            canvas.drawBitmap(nimoBitmap, nimo.getLeft(), top, nimo.isHurtBlink() ? hurtPaint : bitmapPaint);
        } else {
            rect.set(nimo.getLeft(), top, nimo.getLeft() + nimo.getWidth(), top + nimo.getHeight());
            canvas.drawOval(rect, fallbackPaint);
        }
        canvas.restore();
    }

    // ------------------------------------------------------------------ HUD

    private void refreshTexts(GameEngine engine) {
        int score = engine.getScore();
        int collected = engine.getCollected();
        int errors = engine.getErrors();
        int level = engine.getDifficulty().getLevelIndex();
        if (score != cachedScore) {
            cachedScore = score;
            scoreText = res.getString(R.string.hud_score, score, Config.MAX_SCORE);
        }
        if (collected != cachedCollected) {
            cachedCollected = collected;
            collectedText = res.getString(R.string.hud_collected, collected);
        }
        if (errors != cachedErrors) {
            cachedErrors = errors;
            errorsText = res.getString(R.string.hud_errors, errors, Config.MAX_ERRORS);
        }
        if (level != cachedLevel) {
            cachedLevel = level;
            levelText = res.getString(R.string.hud_level, levelNames[level]);
            levelUpText = res.getString(R.string.level_up, levelNames[level]);
        }
    }

    private void drawHud(Canvas canvas, GameEngine engine, int w) {
        refreshTexts(engine);
        float d = density;
        float pad = 10f * d;
        float left = pad;
        float top = topInset + pad;
        float right = w - 76f * d; // espaço do botão de pausa
        if (right - left < 180f * d) right = w - pad;
        float innerL = left + 12f * d;
        float innerR = right - 12f * d;
        float innerW = innerR - innerL;

        // Linha A: pontos + nível
        float scoreSize = 17f * d * fontScale;
        float pillSize = 12.5f * d * fontScale;
        scorePaint.setTextSize(scoreSize);
        pillTextPaint.setTextSize(pillSize);
        float scoreW = scorePaint.measureText(scoreText);
        float pillW = pillTextPaint.measureText(levelText) + 18f * d;
        float need = scoreW + 10f * d + pillW;
        if (need > innerW) {
            float k = innerW / need;
            scoreSize *= k;
            pillSize *= k;
            scorePaint.setTextSize(scoreSize);
            pillTextPaint.setTextSize(pillSize);
            pillW = pillTextPaint.measureText(levelText) + 18f * d * k;
        }
        float pillH = pillSize * 1.9f;
        float rowATop = top + 10f * d;
        float rowAH = Math.max(scoreSize * 1.3f, pillH);
        float rowACenter = rowATop + rowAH * 0.5f;

        // Linha B: barra de progresso
        float barTop = rowATop + rowAH + 6f * d;
        float barH = 9f * d;

        // Linha C: coletados + X X X
        float box = 24f * d * Math.min(fontScale, 1.15f);
        float rowCTop = barTop + barH + 8f * d;
        float rowCCenter = rowCTop + box * 0.5f;
        float bottom = rowCTop + box + 10f * d;

        rect.set(left, top, right, bottom);
        canvas.drawRoundRect(rect, 18f * d, 18f * d, panelPaint);
        canvas.drawRoundRect(rect, 18f * d, 18f * d, panelStroke);

        canvas.drawText(scoreText, innerL, rowACenter + scoreSize * 0.36f, scorePaint);
        LevelSettings level = engine.getDifficulty().getLevel();
        pillPaint.setColor(level.color);
        rect.set(innerR - pillW, rowACenter - pillH * 0.5f, innerR, rowACenter + pillH * 0.5f);
        canvas.drawRoundRect(rect, pillH * 0.5f, pillH * 0.5f, pillPaint);
        canvas.drawText(levelText, rect.centerX(), rowACenter + pillSize * 0.36f, pillTextPaint);

        rect.set(innerL, barTop, innerR, barTop + barH);
        canvas.drawRoundRect(rect, barH * 0.5f, barH * 0.5f, barBgPaint);
        float progress = Math.min(1f, engine.getScore() / (float) Config.MAX_SCORE);
        if (progress > 0f) {
            rect.set(innerL, barTop, innerL + Math.max(barH, innerW * progress), barTop + barH);
            canvas.drawRoundRect(rect, barH * 0.5f, barH * 0.5f, barFillPaint);
        }

        float gap = 6f * d;
        float boxesW = box * Config.MAX_ERRORS + gap * (Config.MAX_ERRORS - 1);
        float boxesLeft = innerR - boxesW;
        float smallSize = 14f * d * fontScale;
        smallPaint.setTextSize(smallSize);
        float collectedW = smallPaint.measureText(collectedText);
        float availableForText = boxesLeft - innerL - 8f * d;
        if (collectedW > availableForText && collectedW > 0f) {
            smallSize *= availableForText / collectedW;
            smallPaint.setTextSize(smallSize);
            collectedW = smallPaint.measureText(collectedText);
        }
        float textBase = rowCCenter + smallSize * 0.36f;
        canvas.drawText(collectedText, innerL, textBase, smallPaint);
        float errorsW = smallPaint.measureText(errorsText);
        if (innerL + collectedW + 12f * d + errorsW + 8f * d <= boxesLeft) {
            smallPaint.setColor(COLOR_RED);
            canvas.drawText(errorsText, boxesLeft - 8f * d - errorsW, textBase, smallPaint);
            smallPaint.setColor(COLOR_TEXT);
        }

        int errors = engine.getErrors();
        float pulse = engine.getErrorPulse();
        for (int i = 0; i < Config.MAX_ERRORS; i++) {
            float bx = boxesLeft + i * (box + gap);
            float cx = bx + box * 0.5f;
            float s = box * 0.5f;
            boolean used = i < errors;
            if (used && i == errors - 1 && pulse > 0f) {
                s *= 1f + 0.45f * (float) Math.sin(Math.min(1f, pulse / 0.6f) * Math.PI);
            }
            rect.set(cx - s, rowCCenter - s, cx + s, rowCCenter + s);
            boxPaint.setColor(used ? 0xFFFFEBEE : 0xFFFFFFFF);
            boxStroke.setColor(used ? COLOR_RED : 0xFFC9BFCF);
            canvas.drawRoundRect(rect, 6f * d, 6f * d, boxPaint);
            canvas.drawRoundRect(rect, 6f * d, 6f * d, boxStroke);
            if (used) {
                float m = s * 0.5f;
                xPaint.setStrokeWidth(Math.max(2f, s * 0.32f));
                canvas.drawLine(cx - m, rowCCenter - m, cx + m, rowCCenter + m, xPaint);
                canvas.drawLine(cx + m, rowCCenter - m, cx - m, rowCCenter + m, xPaint);
            }
        }
    }

    // ------------------------------------------------------------------ mensagens

    private void drawCenterMessages(Canvas canvas, GameEngine engine, int w, int h) {
        if (engine.getState() != GameState.PLAYING && engine.getState() != GameState.PAUSED) return;
        float d = density;
        float cx = w * 0.5f;
        float countdown = engine.getCountdown();
        if (countdown > 0f) {
            int number = (int) Math.ceil(countdown);
            float frac = countdown - (float) Math.floor(countdown);
            float size = 96f * d * (1f + 0.35f * frac * frac);
            drawOutlined(canvas, String.valueOf(number), cx, h * 0.42f, size, 0xFFFF5FA2, 0xFFFFFFFF);
            String hint = engine.getTilt().isTouchControlEnabled() && !engine.getTilt().hasAccelerometer()
                    ? hintTouch : hintTilt;
            drawOutlined(canvas, hint, cx, h * 0.50f, 26f * d, COLOR_TEXT, 0xFFFFFFFF);
        } else if (engine.getGoTimer() > 0f) {
            float k = engine.getGoTimer() / Config.GO_MESSAGE_SECONDS;
            drawOutlined(canvas, goText, cx, h * 0.42f, 84f * d * (0.9f + 0.3f * k), 0xFF43B649, 0xFFFFFFFF);
        } else if (engine.getLevelBannerTime() > 0f) {
            float t = engine.getLevelBannerTime() / Config.LEVEL_BANNER_SECONDS;
            float pop = t > 0.85f ? 1f + (t - 0.85f) * 2.5f : 1f;
            LevelSettings level = engine.getDifficulty().getLevel();
            drawOutlined(canvas, levelUpText, cx, h * 0.36f, 44f * d * pop, level.color, 0xFFFFFFFF);
        }
    }

    private void drawOutlined(Canvas canvas, String text, float x, float y, float size, int fill, int stroke) {
        bigFill.setTextSize(size);
        bigStroke.setTextSize(size);
        float maxWidth = canvas.getWidth() * 0.92f;
        float measured = bigFill.measureText(text);
        if (measured > maxWidth) {
            float k = maxWidth / measured;
            bigFill.setTextSize(size * k);
            bigStroke.setTextSize(size * k);
            size *= k;
        }
        bigStroke.setStrokeWidth(size * 0.14f);
        bigStroke.setColor(stroke);
        bigFill.setColor(fill);
        canvas.drawText(text, x, y, bigStroke);
        canvas.drawText(text, x, y, bigFill);
    }
}
