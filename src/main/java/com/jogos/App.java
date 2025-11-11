package com.jogos;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.*;

public class App extends Application {

    private static final Path HIGH_SCORE_FILE = Path.of("highscore.txt");

    private StackPane rootStack;
    private Pane gamePane;
    private VBox mainMenuPane;
    private VBox optionsPane;
    private Rectangle ground;

    private HBox hud;
    private Label scoreLabel;
    private Label livesLabel;
    private Label highScoreLabel;

    private Collector collector;
    private final List<GameItem> items = new ArrayList<>();
    private final Random rng = new Random();

    private double screenW;
    private double screenH;

    private Stage primaryStage;

    private boolean showingOptions = false;
    private boolean inMenu = true;
    private boolean showHitboxes = false;

    private int score = 0;
    private int lives = 3;
    private int highScore = 0;
    private double spawnTimer = 0.0;
    private double spawnInterval = 1.0;
    private double itemFallSpeedFactor = 0.0025;

    private boolean leftPressed = false;
    private boolean rightPressed = false;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        Rectangle2D bounds = Screen.getPrimary().getBounds();
        screenW = bounds.getWidth();
        screenH = bounds.getHeight();

        highScore = HighScoreManager.load(HIGH_SCORE_FILE);

        rootStack = new StackPane();
        Scene scene = new Scene(rootStack, screenW, screenH);
        stage.setScene(scene);
        stage.setTitle("ReciclaMack");

        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreen(true);

        gamePane = new Pane();
        gamePane.setPrefSize(screenW, screenH);
        gamePane.setStyle("-fx-background-color: linear-gradient(#b3e5fc, #ffffff);");

        // === CRIAÇÃO DO GROUND ===
        createGround();

        buildHud();
        createCollector();
        ensureCollectorAndHudOnPane();

        createMainMenu();
        createOptionsMenu();

        rootStack.getChildren().addAll(gamePane, mainMenuPane);

        scene.setOnKeyPressed(e -> {
            KeyCode c = e.getCode();
            if (inMenu) {
                if (c == KeyCode.ENTER) startGame();
                if (c == KeyCode.ESCAPE && showingOptions) hideOptions();
            } else {
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

        AnimationTimer loop = new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double deltaSeconds = (now - last) / 1_000_000_000.0;
                last = now;

                if (!inMenu) {
                    double moveDir = 0;
                    if (leftPressed) moveDir -= 1;
                    if (rightPressed) moveDir += 1;
                    collector.applyInput(moveDir, screenW);

                    spawnTimer += deltaSeconds;
                    if (spawnTimer >= spawnInterval) {
                        spawnTimer = 0;
                        spawnInterval = Math.max(0.20, spawnInterval * 0.985);
                        itemFallSpeedFactor *= 1.008;
                        spawnItem();
                    }

                    updateItems(deltaSeconds);
                }
            }
        };
        loop.start();

        stage.show();
    }

    private void createGround() {
        double groundHeight = screenH * 0.07;
        ground = new Rectangle(0, screenH - groundHeight, screenW, groundHeight);
        ground.setFill(Color.rgb(110, 70, 40)); // marrom claro
        ground.setStroke(Color.rgb(90, 60, 30));
        ground.setStrokeWidth(2);
        gamePane.getChildren().add(ground);
    }

    private void buildHud() {
        scoreLabel = new Label("Score: 0");
        livesLabel = new Label("Lives: " + lives);
        highScoreLabel = new Label("High: " + highScore);
        scoreLabel.setFont(Font.font(20));
        livesLabel.setFont(Font.font(20));
        highScoreLabel.setFont(Font.font(20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hud = new HBox(12, scoreLabel, livesLabel, spacer, highScoreLabel);
        hud.setPadding(new Insets(10));
        hud.setMinWidth(screenW);
    }

    private void createCollector() {
        double collectorHeight = screenH * 0.20;
        double groundTopY = ground.getY();
        double collectorY = groundTopY - collectorHeight + 10; // “em cima” do chão
        collector = new Collector((screenW - (screenW * 0.14)) / 2.0, collectorY, collectorHeight);
    }

    private void ensureCollectorAndHudOnPane() {
        gamePane.getChildren().removeAll(collector.getNode(), hud);
        gamePane.getChildren().addAll(collector.getNode(), hud);
    }

    // === Menus (iguais aos anteriores, sem placeholders extras) ===
    private void createMainMenu() {
        mainMenuPane = new VBox(12);
        mainMenuPane.setAlignment(Pos.CENTER);
        mainMenuPane.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        Label title = new Label("RECICLA MACK");
        title.setFont(Font.font(48));
        title.setTextFill(Color.WHITE);

        Button playBtn = makeMenuButton("Jogar", e -> startGame());
        Button optionsBtn = makeMenuButton("Opções", e -> showOptions());
        Button exitBtn = makeMenuButton("Sair", e -> {
            HighScoreManager.save(HIGH_SCORE_FILE, highScore);
            System.exit(0);
        });

        mainMenuPane.getChildren().addAll(title, playBtn, optionsBtn, exitBtn);
    }

    private void createOptionsMenu() {
        optionsPane = new VBox(10);
        optionsPane.setAlignment(Pos.CENTER);
        optionsPane.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        optionsPane.setVisible(false);

        Label title = new Label("Opções");
        title.setFont(Font.font(32));
        title.setTextFill(Color.WHITE);

        Button fullscreenBtn = makeMenuButton("Alternar Tela Cheia / Janela", e -> {
            boolean fs = !primaryStage.isFullScreen();
            primaryStage.setFullScreen(fs);
            updateScreenSizeFromStage();
        });

        Label resLabel = new Label("Resolução (modo janela)");
        resLabel.setPrefWidth(280);
        resLabel.setPrefHeight(40);
        resLabel.setAlignment(Pos.CENTER);
        resLabel.setStyle(
                "-fx-background-color: white;" +
                "-fx-text-fill: black;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 10;"
        );

        ComboBox<String> resolutionBox = new ComboBox<>();
        resolutionBox.getItems().addAll("1024x576", "1280x720", "1600x900", "1920x1080");
        resolutionBox.setValue((int) screenW + "x" + (int) screenH);
        resolutionBox.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: white;" +
                "-fx-text-fill: black;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: transparent;" +
                "-fx-alignment: center;"
        );
        resolutionBox.setOnAction(e -> {
            String val = resolutionBox.getValue();
            if (val != null && !val.isEmpty()) {
                String[] parts = val.split("x");
                try {
                    int w = Integer.parseInt(parts[0]);
                    int h = Integer.parseInt(parts[1]);
                    primaryStage.setFullScreen(false);
                    primaryStage.setWidth(w);
                    primaryStage.setHeight(h);
                    updateScreenSizeFromStage();
                } catch (Exception ignored) {}
            }
        });

        Button backToMenuBtn = makeMenuButton("Voltar ao Menu Principal", e -> {
            hideOptions();
            endGame();
        });
        Button backBtn = makeMenuButton("Voltar ao Jogo", e -> hideOptions());

        VBox inner = new VBox(10, title, fullscreenBtn, resLabel, resolutionBox, backToMenuBtn, backBtn);
        inner.setAlignment(Pos.CENTER);
        optionsPane.getChildren().add(inner);
    }

    private Button makeMenuButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(text);
        btn.setPrefWidth(280);
        btn.setPrefHeight(40);
        btn.setStyle(
                "-fx-background-color: white;" +
                "-fx-text-fill: black;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 10;"
        );
        btn.setOnAction(action);
        return btn;
    }

    // ==== GAME LOOP SUPPORT ====
    private void startGame() {
        inMenu = false;
        rootStack.getChildren().removeAll(mainMenuPane, optionsPane);
        showingOptions = false;
        resetGame();
    }

    private void resetGame() {
        score = 0;
        lives = 3;
        spawnInterval = 1.0;
        spawnTimer = 0;
        itemFallSpeedFactor = 0.0025;
        items.forEach(it -> gamePane.getChildren().remove(it.getNode()));
        items.clear();
        updateHud();
        ensureCollectorAndHudOnPane();
    }

    private void showOptions() {
        if (!rootStack.getChildren().contains(optionsPane))
            rootStack.getChildren().add(optionsPane);
        optionsPane.setVisible(true);
        showingOptions = true;
    }

    private void hideOptions() {
        optionsPane.setVisible(false);
        rootStack.getChildren().remove(optionsPane);
        showingOptions = false;
    }

    private void showOptionsFromGame() {
        if (!showingOptions) {
            showOptions();
        } else hideOptions();
    }

    private void spawnItem() {
        double size = Math.max(48, screenW * 0.07);
        double x = 12 + rng.nextDouble() * (screenW - size - 24);
        double y = -size - rng.nextDouble(10, 80);
        ItemType t = ItemType.values()[rng.nextInt(ItemType.values().length)];
        GameItem gi = new GameItem(t, x, y, size);
        items.add(gi);
        gamePane.getChildren().add(gi.getNode());
        ensureCollectorAndHudOnPane();
    }

    private void updateItems(double deltaSeconds) {
        double fall = screenH * itemFallSpeedFactor * deltaSeconds * 60.0;
        Iterator<GameItem> it = items.iterator();
        while (it.hasNext()) {
            GameItem gi = it.next();
            gi.y += fall;
            gi.updateView();

            if (gi.isOffScreen(screenH)) {
                gamePane.getChildren().remove(gi.getNode());
                it.remove();
                continue;
            }

            if (collector.intersects(gi)) {
                gamePane.getChildren().remove(gi.getNode());
                it.remove();
                score += switch (gi.type) {
                    case METAL -> 2;
                    case PLASTIC -> 1;
                    case REUSE -> 3;
                    case BATTERY -> { lives--; yield 0; }
                };
                if (score > highScore) highScore = score;
                if (lives <= 0) endGame();
                updateHud();
            }
        }
        collector.setHitboxVisible(showHitboxes);
        items.forEach(i -> i.setHitboxVisible(showHitboxes));
    }

    private void updateHud() {
        scoreLabel.setText("Score: " + score);
        livesLabel.setText("Lives: " + lives);
        highScoreLabel.setText("High: " + highScore);
    }

    private void endGame() {
        inMenu = true;
        resetGame();
        if (!rootStack.getChildren().contains(mainMenuPane))
            rootStack.getChildren().add(mainMenuPane);
        HighScoreManager.save(HIGH_SCORE_FILE, highScore);
    }

    private void toggleHitboxes() {
        showHitboxes = !showHitboxes;
    }

    private void updateScreenSizeFromStage() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        if (primaryStage.isFullScreen()) {
            screenW = bounds.getWidth();
            screenH = bounds.getHeight();
        } else {
            screenW = primaryStage.getWidth();
            screenH = primaryStage.getHeight();
        }
        gamePane.setPrefSize(screenW, screenH);
        hud.setMinWidth(screenW);

        gamePane.getChildren().remove(ground);
        createGround();

        double collectorHeight = screenH * 0.20;
        double groundTopY = ground.getY();
        double collectorY = groundTopY - collectorHeight + 10;
        gamePane.getChildren().remove(collector.getNode());
        collector = new Collector(collector.getNode().getTranslateX(), collectorY, collectorHeight);
        ensureCollectorAndHudOnPane();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
