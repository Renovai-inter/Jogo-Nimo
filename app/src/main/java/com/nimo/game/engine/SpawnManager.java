package com.nimo.game.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Gerador inteligente de produtos ("aleatoriedade controlada").
 *
 * Regras de fair play aplicadas antes de criar cada produto:
 *  1. Distância mínima horizontal/vertical entre produtos (nunca sobrepostos).
 *  2. Um produto mais rápido nunca alcança outro na mesma coluna antes do chão.
 *  3. Todo produto correto é alcançável a partir do anterior, considerando a
 *     velocidade do Nimo reduzida (fairSpeedFactor de cada nível) e o tempo de reação humano.
 *  4. Um produto errado nunca chega junto de um correto no mesmo lugar: sempre
 *     existe uma forma de pegar o correto sem encostar no errado.
 *  5. Um produto errado não pode bloquear o único caminho possível entre dois corretos.
 *  6. Tipos são sorteados de uma "sacola" balanceada (proporção de errados controlada),
 *     com limite de errados seguidos, e as posições preferem regiões pouco usadas.
 */
public class SpawnManager {

    private static final int MAX_TIMELINE = 64;
    private static final float PENDING_GRACE = 0.25f;

    private final Random random = new Random();
    private final ArrayList<ItemType> bag = new ArrayList<>();
    private final List<ItemType> correctTypes = ItemType.ofCategory(ItemCategory.CORRECT);
    private final List<ItemType> wrongTypes = ItemType.ofCategory(ItemCategory.WRONG);
    private final float[] zoneUsage = new float[Config.SPAWN_ZONES];
    private final float[] zoneWeights = new float[Config.SPAWN_ZONES];

    // Linha do tempo dos produtos corretos pendentes (tempo de chegada, posição x).
    private final float[] timelineT = new float[MAX_TIMELINE];
    private final float[] timelineX = new float[MAX_TIMELINE];
    private int timelineCount;

    private int bagLevel = -1;
    private int wrongStreak;
    private int forcedCorrect;
    private float timer;

    public void reset() {
        bag.clear();
        bagLevel = -1;
        wrongStreak = 0;
        forcedCorrect = Config.FIRST_CORRECT_ITEMS;
        timer = Config.FIRST_SPAWN_DELAY;
        Arrays.fill(zoneUsage, 0f);
    }

    public void update(float dt, GameEngine engine) {
        timer -= dt;
        if (timer > 0f) return;

        DifficultyManager difficulty = engine.getDifficulty();
        if (engine.getItems().size() >= difficulty.getMaxItems()) {
            timer = Config.SPAWN_RETRY_DELAY;
            return;
        }

        ItemType type = peekType(difficulty);
        float variation = 1f + (random.nextFloat() * 2f - 1f) * Config.FALL_SPEED_VARIATION;
        float speed = difficulty.getFallSpeed(engine.getHeight()) * variation;

        if (trySpawn(engine, type, speed)) {
            consumeType();
            float jitter = 1f + (random.nextFloat() * 2f - 1f) * Config.SPAWN_INTERVAL_JITTER;
            timer = difficulty.getSpawnInterval() * jitter;
        } else {
            if (!type.isCorrect() && bag.size() > 1) {
                // Um errado que não coube agora vai para o fim da sacola.
                bag.add(bag.remove(0));
            }
            timer = Config.SPAWN_RETRY_DELAY;
        }
    }

    // ------------------------------------------------------------------ tipos

    private ItemType peekType(DifficultyManager difficulty) {
        int level = difficulty.getLevelIndex();
        if (bag.isEmpty() || bagLevel != level) {
            refillBag(difficulty);
        }
        boolean needCorrect = forcedCorrect > 0 || wrongStreak >= Config.MAX_WRONG_IN_A_ROW;
        if (needCorrect && !bag.get(0).isCorrect()) {
            int index = -1;
            for (int i = 1; i < bag.size(); i++) {
                if (bag.get(i).isCorrect()) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                Collections.swap(bag, 0, index);
            } else {
                bag.add(0, weighted(correctTypes));
            }
        }
        return bag.get(0);
    }

    private void consumeType() {
        ItemType type = bag.remove(0);
        if (type.isCorrect()) {
            wrongStreak = 0;
            if (forcedCorrect > 0) forcedCorrect--;
        } else {
            wrongStreak++;
        }
    }

    private void refillBag(DifficultyManager difficulty) {
        bag.clear();
        int size = Config.TYPE_BAG_SIZE;
        int wrongCount = Math.round(size * difficulty.getWrongRatio());
        for (int i = 0; i < wrongCount; i++) {
            bag.add(weighted(wrongTypes));
        }
        for (int i = wrongCount; i < size; i++) {
            bag.add(weighted(correctTypes));
        }
        Collections.shuffle(bag, random);
        bagLevel = difficulty.getLevelIndex();
    }

    private ItemType weighted(List<ItemType> types) {
        int total = 0;
        for (ItemType t : types) total += t.spawnWeight;
        int r = random.nextInt(Math.max(1, total));
        for (ItemType t : types) {
            r -= t.spawnWeight;
            if (r < 0) return t;
        }
        return types.get(0);
    }

    // ------------------------------------------------------------------ posição

    private boolean trySpawn(GameEngine engine, ItemType type, float speed) {
        float width = engine.getWidth();
        float size = engine.getItemSize();
        float half = size * 0.5f;
        Nimo nimo = engine.getNimo();

        float minX = half + width * 0.01f;
        float maxX = width - half - width * 0.01f;
        if (type.isCorrect()) {
            // Produtos corretos sempre em uma região que a cesta consegue alcançar.
            minX = Math.max(minX, nimo.getMinX() - nimo.getCatchHalfWidth() * 0.5f);
            maxX = Math.min(maxX, nimo.getMaxX() + nimo.getCatchHalfWidth() * 0.5f);
        }
        if (maxX < minX) {
            float mid = (minX + maxX) * 0.5f;
            minX = mid;
            maxX = mid;
        }
        float spawnY = -half;
        buildTimeline(engine);

        float zoneWidth = width / Config.SPAWN_ZONES;
        for (int attempt = 0; attempt < Config.SPAWN_MAX_ATTEMPTS; attempt++) {
            int zone = pickZone();
            float x = clamp(zone * zoneWidth + random.nextFloat() * zoneWidth, minX, maxX);
            if (isFair(engine, type, x, spawnY, speed)) {
                spawn(engine, type, x, spawnY, speed, zoneOf(x, zoneWidth));
                return true;
            }
        }

        // Tentativas aleatórias falharam: procurar posições seguras determinísticas.
        float step = size * Config.MIN_HORIZONTAL_DISTANCE;
        if (type.isCorrect()) {
            float anchor = timelineCount > 0 ? timelineX[timelineCount - 1] : nimo.getX();
            for (int k = 0; k <= 6; k++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    if (k == 0 && sign > 0) continue;
                    float x = clamp(anchor + sign * k * step, minX, maxX);
                    if (isFair(engine, type, x, spawnY, speed)) {
                        spawn(engine, type, x, spawnY, speed, zoneOf(x, zoneWidth));
                        return true;
                    }
                }
            }
        } else {
            int slots = 9;
            int offset = random.nextInt(slots);
            for (int n = 0; n < slots; n++) {
                int s = (n + offset) % slots;
                float x = minX + (maxX - minX) * (s / (float) (slots - 1));
                if (isFair(engine, type, x, spawnY, speed)) {
                    spawn(engine, type, x, spawnY, speed, zoneOf(x, zoneWidth));
                    return true;
                }
            }
        }
        return false;
    }

    private void spawn(GameEngine engine, ItemType type, float x, float y, float speed, int zone) {
        FallingItem item = engine.obtainItem();
        item.init(type, x, y, speed, engine.getItemSize(), random.nextFloat() * 6.2832f);
        engine.addItem(item);
        for (int i = 0; i < zoneUsage.length; i++) {
            zoneUsage[i] *= Config.ZONE_USAGE_DECAY;
        }
        zoneUsage[zone] += 1f;
    }

    private int pickZone() {
        float total = 0f;
        for (int i = 0; i < zoneWeights.length; i++) {
            zoneWeights[i] = 1f / (1f + zoneUsage[i] * Config.ZONE_REPEAT_PENALTY);
            total += zoneWeights[i];
        }
        float r = random.nextFloat() * total;
        for (int i = 0; i < zoneWeights.length; i++) {
            r -= zoneWeights[i];
            if (r <= 0f) return i;
        }
        return zoneWeights.length - 1;
    }

    private int zoneOf(float x, float zoneWidth) {
        int zone = (int) (x / Math.max(1f, zoneWidth));
        return Math.max(0, Math.min(Config.SPAWN_ZONES - 1, zone));
    }

    /** Monta a linha do tempo: posição atual do Nimo + produtos corretos ainda pegáveis. */
    private void buildTimeline(GameEngine engine) {
        Nimo nimo = engine.getNimo();
        float catchY = nimo.getCatchTopY();
        timelineCount = 0;
        timelineT[0] = 0f;
        timelineX[0] = nimo.getX();
        timelineCount = 1;
        List<FallingItem> items = engine.getItems();
        for (int i = 0; i < items.size() && timelineCount < MAX_TIMELINE; i++) {
            FallingItem it = items.get(i);
            if (!it.type.isCorrect()) continue;
            float t = it.timeToReach(catchY);
            if (t < -PENDING_GRACE) continue;
            // Inserção ordenada por tempo de chegada.
            int j = timelineCount - 1;
            while (j >= 0 && timelineT[j] > t) {
                timelineT[j + 1] = timelineT[j];
                timelineX[j + 1] = timelineX[j];
                j--;
            }
            timelineT[j + 1] = t;
            timelineX[j + 1] = it.x;
            timelineCount++;
        }
    }

    /** Verifica todas as regras de fair play para um produto candidato. */
    boolean isFair(GameEngine engine, ItemType type, float x, float y, float speed) {
        float size = engine.getItemSize();
        float minH = size * Config.MIN_HORIZONTAL_DISTANCE;
        float minV = size * Config.MIN_VERTICAL_DISTANCE;
        float groundY = engine.getGroundY();
        List<FallingItem> items = engine.getItems();

        // 1 e 2: espaçamento e ultrapassagem.
        for (int i = 0; i < items.size(); i++) {
            FallingItem e = items.get(i);
            if (Math.abs(e.x - x) >= minH) continue;
            float dy = e.y - y;
            if (Math.abs(dy) < minV) return false;
            if (dy > 0f && speed > e.speed) {
                float timeToClose = (dy - minV) / (speed - e.speed);
                float timeToGround = (groundY - e.y) / e.speed;
                if (timeToClose < timeToGround) return false;
            }
        }

        Nimo nimo = engine.getNimo();
        float vEff = nimo.getMaxSpeed() * engine.getDifficulty().getFairSpeedFactor();
        float catchHalf = nimo.getCatchHalfWidth();
        float avoid = catchHalf + size * 0.5f * Config.ITEM_HITBOX_SCALE + size * 0.1f;
        float catchY = nimo.getCatchTopY();
        float tc = (catchY - y) / speed;

        int prev = -1;
        int next = -1;
        for (int i = 0; i < timelineCount; i++) {
            if (timelineT[i] <= tc) {
                prev = i;
            } else {
                next = i;
                break;
            }
        }

        if (type.isCorrect()) {
            // 3: alcançável a partir do anterior e até o próximo.
            if (prev >= 0 && !reachable(timelineT[prev], timelineX[prev], tc, x, vEff, catchHalf)) return false;
            if (next >= 0 && !reachable(tc, x, timelineT[next], timelineX[next], vEff, catchHalf)) return false;
            // 4 e 5: errados existentes não podem ficar no caminho.
            for (int i = 0; i < items.size(); i++) {
                FallingItem w = items.get(i);
                if (w.type.isCorrect()) continue;
                float tw = w.timeToReach(catchY);
                if (tw < -PENDING_GRACE) continue;
                if (!wrongAvoidable(tw, w.x, tc, x, vEff, avoid)) return false;
                if (prev >= 0 && tw > timelineT[prev] && tw < tc
                        && pathBlocked(timelineT[prev], timelineX[prev], tc, x, tw, w.x, vEff, avoid)) {
                    return false;
                }
                if (next >= 0 && tw > tc && tw < timelineT[next]
                        && pathBlocked(tc, x, timelineT[next], timelineX[next], tw, w.x, vEff, avoid)) {
                    return false;
                }
            }
        } else {
            // 4: o errado não pode chegar junto de nenhum correto no mesmo lugar.
            for (int i = 0; i < timelineCount; i++) {
                if (!wrongAvoidable(tc, x, timelineT[i], timelineX[i], vEff, avoid)) return false;
            }
            // 5: o errado não pode bloquear o caminho entre dois corretos.
            if (prev >= 0 && next >= 0
                    && pathBlocked(timelineT[prev], timelineX[prev], timelineT[next], timelineX[next],
                    tc, x, vEff, avoid)) {
                return false;
            }
        }
        return true;
    }

    private static boolean reachable(float t1, float x1, float t2, float x2, float vEff, float catchHalf) {
        float required = Math.max(0f, Math.abs(x2 - x1) - catchHalf * Config.CATCH_TOLERANCE);
        float available = vEff * Math.max(0f, (t2 - t1) - Config.REACTION_TIME);
        return required <= available;
    }

    private static boolean wrongAvoidable(float tw, float wx, float tc, float cx, float vEff, float avoid) {
        float move = vEff * Math.max(0f, Math.abs(tw - tc) - Config.REACTION_TIME * 0.5f);
        return Math.abs(wx - cx) + move >= avoid;
    }

    private static boolean pathBlocked(float t1, float x1, float t2, float x2,
                                       float tw, float wx, float vEff, float avoid) {
        float dt = t2 - t1;
        if (dt <= 0.0001f) return false;
        float slack = Math.max(0f, dt - Math.abs(x2 - x1) / vEff);
        float f = (tw - t1) / dt;
        float expectedX = x1 + (x2 - x1) * f;
        float freedom = vEff * slack * 0.5f;
        return Math.abs(wx - expectedX) + freedom < avoid;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
}
