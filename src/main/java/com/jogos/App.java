package com.jogos;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class App extends Application {

    // Persistência highscore
    private static final Path HIGH_SCORE_FILE = Path.of("highscore.txt");

    // UI root panes
    private StackPane rootStack;     // tem gamePane + overlays
    private Pane gamePane;           // onde o jogo roda
    private VBox mainMenuPane;       // menu inicial
    private VBox optionsPane;        // menu de opções (overlay)

    // HUD
    private HBox hud;
    private Label scoreLabel;
    private Label livesLabel;
    private Label highScoreLabel;

    // Game objects
    private Collector collector;
    private final List<GameItem> items = new ArrayList<>();
    private final Random rng = new Random();

    // Screen size (adaptável)
    private double screenW;
    private double screenH;

    // State
    private boolean showingOptions = false;
    private boolean inMenu = true;
    private boolean showHitboxes = false;

    // Gameplay variables
    private int score = 0;
    private int lives = 3;
    private int highScore = 0;
    private double spawnTimer = 0.0;
    private double spawnInterval = 1.0;
    private double itemFallSpeedFactor = 0.0025; // times screen height per frame tick

    // Input flags
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    @Override
    public void start(Stage stage) {
        // screen bounds (works across resolutions)
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        screenW = bounds.getWidth();
        screenH = bounds.getHeight();

        // load highs
        highScore = HighScoreManager.load(HIGH_SCORE_FILE);

        // root stack
        rootStack = new StackPane();
        Scene scene = new Scene(rootStack, screenW, screenH);
        stage.setScene(scene);
        stage.setTitle("ReciclaMack - Fullscreen");
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");

        // game pane (background)
        gamePane = new Pane();
        gamePane.setPrefSize(screenW, screenH);
        gamePane.setStyle("-fx-background-color: linear-gradient(#b3e5fc, #ffffff);");

        // HUD (top)
        buildHud();

        // Collector: tamanho mais visível (proporção à tela)
        double collectorDesiredHeight = screenH * 0.20; // 20% da altura da tela
        double collectorX = (screenW - (screenW * 0.14)) / 2.0;
        double collectorY = screenH - collectorDesiredHeight - (screenH * 0.03);
        collector = new Collector(collectorX, collectorY, collectorDesiredHeight);
        gamePane.getChildren().addAll(collector.getNode()); // hitbox e image dentro do node

        // keep hud on top
        gamePane.getChildren().add(hud);

        // create menus
        createMainMenu();
        createOptionsMenu();

        rootStack.getChildren().addAll(gamePane, mainMenuPane); // menu por cima inicialmente

        // key input: global
        scene.setOnKeyPressed(e -> {
            KeyCode c = e.getCode();
            if (inMenu) {
                // allow keyboard navigation (ENTER for play)
                if (c == KeyCode.ENTER) startGame();
                if (c == KeyCode.ESCAPE && showingOptions) hideOptions();
            } else {
                // in game
                if (c == KeyCode.LEFT || c == KeyCode.A) leftPressed = true;
                if (c == KeyCode.RIGHT || c == KeyCode.D) rightPressed = true;
                if (c == KeyCode.H) toggleHitboxes();
                if (c == KeyCode.ESCAPE) showOptionsFromGame();
            }
        });

        scene.setOnKeyReleased(e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.LEFT || c == KeyCode.A) leftPressed = false;
            if (c == KeyCode.RIGHT || c == KeyCode.D) rightPressed = false;
        });

        // game loop
        AnimationTimer loop = new AnimationTimer() {
            private long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double deltaSeconds = (now - last) / 1_000_000_000.0;
                last = now;

                if (!inMenu) {
                    // update collector movement
                    double moveDir = 0;
                    if (leftPressed) moveDir -= 1;
                    if (rightPressed) moveDir += 1;
                    collector.applyInput(moveDir, screenW);

                    // spawn items by timer
                    spawnTimer += deltaSeconds;
                    if (spawnTimer >= spawnInterval) {
                        spawnTimer = 0;
                        // slightly speed up spawn interval over time
                        spawnInterval = Math.max(0.25, spawnInterval * 0.997);
                        spawnItem();
                    }

                    // update items
                    updateItems(deltaSeconds);
                }
            }
        };
        loop.start();

        stage.show();
    }

    // ---------- UI builders ----------
    private void buildHud() {
        scoreLabel = new Label("Score: 0");
        scoreLabel.setFont(Font.font(20));
        livesLabel = new Label("Lives: " + lives);
        livesLabel.setFont(Font.font(20));
        highScoreLabel = new Label("High: " + highScore);
        highScoreLabel.setFont(Font.font(20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hud = new HBox(12, scoreLabel, livesLabel, spacer, highScoreLabel);
        hud.setPadding(new Insets(10));
        hud.setMinWidth(screenW);
        hud.setTranslateY(8);
        hud.setTranslateX(12);
    }

    private void createMainMenu() {
        mainMenuPane = new VBox(12);
        mainMenuPane.setAlignment(Pos.CENTER);
        mainMenuPane.setPrefSize(screenW, screenH);
        mainMenuPane.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        Label title = new Label("RECICLA MACK");
        title.setFont(Font.font(48));
        title.setTextFill(Color.WHITE);

        Button playBtn = new Button("Jogar");
        playBtn.setPrefWidth(280);
        playBtn.setOnAction(e -> startGame());

        Button optionsBtn = new Button("Opções");
        optionsBtn.setPrefWidth(280);
        optionsBtn.setOnAction(e -> showOptions());

        Button exitBtn = new Button("Sair");
        exitBtn.setPrefWidth(280);
        exitBtn.setOnAction(e -> {
            // salva highscore e fecha
            HighScoreManager.save(HIGH_SCORE_FILE, highScore);
            System.exit(0);
        });

        VBox box = new VBox(10, title, playBtn, optionsBtn, exitBtn);
        box.setAlignment(Pos.CENTER);
        mainMenuPane.getChildren().add(box);
    }

    private void createOptionsMenu() {
        optionsPane = new VBox(10);
        optionsPane.setAlignment(Pos.CENTER);
        optionsPane.setPrefSize(screenW, screenH);
        optionsPane.setStyle("-fx-background-color: rgba(10,10,10,0.65);");
        optionsPane.setVisible(false);

        Label title = new Label("Opções");
        title.setFont(Font.font(32));
        title.setTextFill(Color.WHITE);

        // Placeholder controls
        Slider masterVol = new Slider(0, 100, 80);
        masterVol.setPrefWidth(380);
        Label masterLabel = new Label("Volume (placeholder)");

        ChoiceBox<String> difficulty = new ChoiceBox<>();
        difficulty.getItems().addAll("Fácil", "Normal", "Difícil");
        difficulty.setValue("Normal");

        Label controlsLabel = new Label("Controles (placeholder)");
        Button restoreDefaults = new Button("Restaurar padrões (placeholder)");
        Button backBtn = new Button("Voltar");
        backBtn.setOnAction(e -> hideOptions());

        VBox inner = new VBox(8, title, masterLabel, masterVol, new Label("Dificuldade"), difficulty, controlsLabel, restoreDefaults, backBtn);
        inner.setAlignment(Pos.CENTER);
        optionsPane.getChildren().add(inner);
    }

    // ---------- menu actions ----------
    private void startGame() {
        // remove menu overlay and reset game state
        inMenu = false;
        rootStack.getChildren().remove(mainMenuPane);
        if (rootStack.getChildren().contains(optionsPane)) rootStack.getChildren().remove(optionsPane);
        showingOptions = false;
        resetGame();
    }

    private void resetGame() {
        // reset game state variables
        score = 0;
        lives = 3;
        spawnInterval = 1.0;
        spawnTimer = 0;
        items.forEach(it -> gamePane.getChildren().remove(it.getNode()));
        items.clear();
        updateHud();
    }

    private void showOptions() {
        if (!rootStack.getChildren().contains(optionsPane)) rootStack.getChildren().add(optionsPane);
        optionsPane.setVisible(true);
        showingOptions = true;
    }

    private void hideOptions() {
        optionsPane.setVisible(false);
        rootStack.getChildren().remove(optionsPane);
        showingOptions = false;
    }

    private void showOptionsFromGame() {
        // overlay options during game
        if (!showingOptions) {
            showOptions();
            inMenu = false; // still in game but options overlay
        } else {
            hideOptions();
        }
    }

    // ---------- game logic ----------
    private void spawnItem() {
        // tamanho proporcional à tela
        double size = Math.max(48, screenW * 0.07);
        double x = 12 + rng.nextDouble() * (screenW - size - 24);
        double y = -size - rng.nextDouble(10, 80);

        ItemType t = ItemType.values()[rng.nextInt(ItemType.values().length)];
        GameItem gi = new GameItem(t, x, y, size);
        items.add(gi);

        // adiciona o item na tela
        gamePane.getChildren().add(gi.getNode());

        // mantém o coletor e o HUD sempre por cima
        if (!gamePane.getChildren().contains(collector.getNode())) {
            gamePane.getChildren().add(collector.getNode());
        }
        if (!gamePane.getChildren().contains(hud)) {
            gamePane.getChildren().add(hud);
        } else {
            gamePane.getChildren().remove(hud);
            gamePane.getChildren().add(hud);
        }
    }

    private void updateItems(double deltaSeconds) {
        // fall speed is proportional to screen height and itemFallSpeedFactor
        double fall = screenH * itemFallSpeedFactor * deltaSeconds * 60.0; // tuned multiplier
        Iterator<GameItem> it = items.iterator();
        while (it.hasNext()) {
            GameItem gi = it.next();
            gi.y += fall;
            gi.updateView();

            // if off screen remove
            if (gi.isOffScreen(screenH)) {
                gamePane.getChildren().remove(gi.getNode());
                it.remove();
                continue;
            }

            // collision using precise bounds (collector uses its hitbox)
            if (collector.intersects(gi)) {
                // item collected
                gamePane.getChildren().remove(gi.getNode());
                it.remove();
                score += switch (gi.type) {
                    case METAL -> 2;
                    case PLASTIC -> 1;
                    case REUSE -> 3;
                    case BATTERY -> { lives -= 1; yield 0; }
                };
                if (score > highScore) highScore = score;
                if (lives <= 0) endGame();
                updateHud();
            }
        }

        // sync hitbox visibility
        collector.setHitboxVisible(showHitboxes);
        items.forEach(i -> i.setHitboxVisible(showHitboxes));
    }

    private void updateHud() {
        scoreLabel.setText("Score: " + score);
        livesLabel.setText("Lives: " + lives);
        highScoreLabel.setText("High: " + highScore);
    }

    private void endGame() {
        // show menu again
        inMenu = true;
        resetGame();
        if (!rootStack.getChildren().contains(mainMenuPane)) rootStack.getChildren().add(mainMenuPane);
        // save highscore
        HighScoreManager.save(HIGH_SCORE_FILE, highScore);
        updateHud();
    }

    // ---------- utilities ----------
    private void toggleHitboxes() {
        showHitboxes = !showHitboxes;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
