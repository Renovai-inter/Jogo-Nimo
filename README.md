# NimoGame — Nimo: Pegue os Produtos Certos

Jogo Android 100% nativo (Java + Canvas/SurfaceView), sem bibliotecas externas.

## Como abrir e executar
1. Extraia o ZIP.
2. No Android Studio: **File > Open** e selecione a pasta `NimoGame`.
3. Aguarde o **Gradle Sync** (o Android Studio baixa o Gradle 8.11.1 indicado em
   `gradle/wrapper/gradle-wrapper.properties`).
4. Conecte um celular (depuração USB) ou inicie um emulador e clique em **Run**.

> O arquivo binário `gradle/wrapper/gradle-wrapper.jar` não está incluído. O Android Studio
> não precisa dele para sincronizar. Para usar `./gradlew` pelo terminal, gere-o uma vez com
> `gradle wrapper --gradle-version 8.11.1` (ou pelo próprio Android Studio).

Requisitos: Android Studio Ladybug (2024.2) ou mais novo, JDK 17+, Android 7.0 (API 24) ou superior.

## Controles
- **Inclinar o celular** para a esquerda/direita move o Nimo.
- Sem acelerômetro (ex.: alguns emuladores): **toque e arraste** na tela.
  No emulador também dá para usar *Extended controls > Virtual sensors* para inclinar.

## Configurações (dificuldade)
No menu inicial, **CONFIGURAÇÕES** permite escolher o modo: **FÁCIL**, **MÉDIO** (balanceamento
original) ou **DIFÍCIL** (padrão). A escolha fica salva no aparelho e vale a partir da próxima partida.
Cada modo tem sua tabela em `Config.java` (`LEVELS_EASY`, `LEVELS_NORMAL`, `LEVELS_HARD`); o modo
padrão está em `DifficultyMode.DEFAULT`.

## Estrutura
- `engine/Config.java` — todas as constantes (pontuação, erros, velocidades, sensibilidade,
  zona morta, distâncias mínimas de spawn e as tabelas de níveis de cada modo).
- `engine/ItemType.java` — lista de produtos. Para criar um novo, adicione a imagem em
  `res/drawable-nodpi`, o nome em `strings.xml` e uma linha no enum (categoria, pontos, peso).
- `engine/GameEngine.java` — regras e estados (MENU, TUTORIAL, PLAYING, PAUSED, GAME_OVER, VICTORY).
- `engine/SpawnManager.java` — spawn inteligente com regras de fair play.
- `engine/DifficultyManager.java`, `CollisionManager.java`, `TiltController.java`,
  `EffectsManager.java`, `Nimo.java`, `FallingItem.java`, `GameRenderer.java`, `SceneRenderer.java`.
- `audio/SoundManager.java` — sons opcionais (o jogo funciona mesmo se falharem).
- `ui/` — `MainActivity` (menu), `TutorialActivity`, `GameActivity`, `GameView` (game loop com delta time).
