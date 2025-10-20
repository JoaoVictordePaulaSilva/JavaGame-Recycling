package com.jogos;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos; // <--- adicionado
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane; // <--- adicionado
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Screen;                 // <--- adicionado
import javafx.geometry.Rectangle2D;        // <--- adicionado
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Versão refatorada: orientação a objetos, melhor UX (menu, pausa, reinício),
 * movimento suave, configuração centralizada e persistência de highscore.
 */
public class App extends Application {

    // --- CONFIG ---
    private static final int WIDTH = 720;
    private static final int HEIGHT = 900;
    private static final Path HIGH_SCORE_FILE = Path.of("highscore.txt");

    // gameplay tuning
    private static final double START_ITEM_SPEED = 120.0; // px/s
    private static final double START_SPAWN_INTERVAL = 1.0; // s
    private static final double SPAWN_ACCEL = 0.996; // multiplicative decay
    private static final double COLLECTOR_SPEED = 420.0; // px/s
    private static final int START_LIVES = 3;

    // --- UI / nodes ---
    private Pane root;
    private Text scoreText, livesText, infoText, highScoreText, msgText;
    private Collector collector;

    // --- game state / manager ---
    private final List<GameItem> items = new ArrayList<>();
    private double itemSpeed = START_ITEM_SPEED;
    private double spawnInterval = START_SPAWN_INTERVAL;
    private double spawnTimer = 0.0;
    private int score = 0;
    private int lives = START_LIVES;
    private int highScore = 0;

    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean running = false; // started
    private boolean paused = false;

    private final Random rng = ThreadLocalRandom.current();

    private enum ItemType { METAL, BATTERY, PLASTIC, REUSE }

    @Override
    public void start(Stage stage) {
        loadHighScore();

        root = new Pane();
        // garante que o root tenha o tamanho lógico do jogo e fique ancorado ao topo-esquerdo
        root.setPrefSize(WIDTH, HEIGHT);
        StackPane container = new StackPane(root);
        StackPane.setAlignment(root, Pos.TOP_LEFT); // preserva o sistema de coordenadas original
        container.setStyle("-fx-background-color: linear-gradient(#b3e5fc, #ffffff);");
        Scene scene = new Scene(container, WIDTH, HEIGHT);

        // Ajuste automático para caber na tela: calcula escala e centraliza
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double maxW = Math.max(200, vb.getWidth() - 40);   // margem mínima
        double maxH = Math.max(200, vb.getHeight() - 80);
        double scale = Math.min(1.0, Math.min(maxW / WIDTH, maxH / HEIGHT));
        // aplica escala no root (mantém layout relativo)
        root.setScaleX(scale);
        root.setScaleY(scale);
        // define tamanho da janela e centraliza
        double windowW = WIDTH * scale;
        double windowH = HEIGHT * scale;
        stage.setScene(scene);
        stage.setWidth(windowW);
        stage.setHeight(windowH);
        stage.setX(vb.getMinX() + (vb.getWidth() - windowW) / 2);
        stage.setY(vb.getMinY() + (vb.getHeight() - windowH) / 2);
        stage.setResizable(false);

        // UI texts
        scoreText = createText(12, 28, "Score: 0", 18, Color.DARKBLUE);
        livesText = createText(12, 52, "Lives: " + lives, 16, Color.DARKRED);
        highScoreText = createText(WIDTH - 160, 28, "High: " + highScore, 16, Color.DARKGREEN);
        infoText = createText(12, HEIGHT - 12, "←/→ mover • P pausar • R reiniciar • ENTER iniciar", 14, Color.DIMGRAY);
        msgText = createText(WIDTH/2.0 - 220, HEIGHT/2.0 - 60, "", 28, Color.FIREBRICK);
        msgText.setTextOrigin(VPos.CENTER);

        root.getChildren().addAll(scoreText, livesText, highScoreText, infoText, msgText);

        // collector
        // aumentar altura para melhor visibilidade; reposiciona um pouco acima do fim
        collector = new Collector(WIDTH/2.0 - 60, HEIGHT - 100, 120, 36, Color.DODGERBLUE);
        // garante que a cesta tenha contorno e fique sempre visível à frente dos itens
        collector.view.setStroke(Color.BLACK);
        collector.view.setStrokeWidth(1.0);
        root.getChildren().add(collector.view);
        collector.view.toFront();

        // input
        scene.setOnKeyPressed(e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.LEFT || c == KeyCode.A) movingLeft = true;
            if (c == KeyCode.RIGHT || c == KeyCode.D) movingRight = true;
            if (c == KeyCode.P && running) togglePause();
            if (c == KeyCode.R) restart();
            if (c == KeyCode.ENTER && !running) startGame();
        });
        scene.setOnKeyReleased(e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.LEFT || c == KeyCode.A) movingLeft = false;
            if (c == KeyCode.RIGHT || c == KeyCode.D) movingRight = false;
        });

        // show title / instructions
        showTitle();

        // game loop
        AnimationTimer timer = new AnimationTimer() {
            private long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double delta = (now - last) / 1_000_000_000.0;
                last = now;
                if (running && !paused) {
                    update(delta);
                }
            }
        };
        timer.start();

        // Java
        // após criar Scene scene = new Scene(root, WIDTH, HEIGHT);
        DoubleBinding scaleBinding = Bindings.createDoubleBinding(
            () -> {
                double sw = scene.getWidth();
                double sh = scene.getHeight();
                double s = Math.min(sw / WIDTH, sh / HEIGHT);
                return Math.max(0.5, Math.min(1.0, s)); // mantém escala entre 0.5 e 1.0
            },
            scene.widthProperty(), scene.heightProperty()
        );
        root.scaleXProperty().bind(scaleBinding);
        root.scaleYProperty().bind(scaleBinding);
        
        // aplica a Scene e permite redimensionar (o binding cuida da escala)
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setResizable(true);

        stage.setTitle("ReciclaMack - Refatorado");
        stage.show();
    }

    // --- high level actions ---
    private void startGame() {
        running = true;
        paused = false;
        score = 0;
        lives = START_LIVES;
        itemSpeed = START_ITEM_SPEED;
        spawnInterval = START_SPAWN_INTERVAL;
        spawnTimer = 0;
        clearItems();
        updateUI();
        msgText.setText("");
    }

    private void togglePause() {
        paused = !paused;
        msgText.setText(paused ? "PAUSADO" : "");
    }

    private void restart() {
        running = false;
        paused = false;
        clearItems();
        showTitle();
    }

    private void endGame() {
        running = false;
        msgText.setText("GAME OVER — Pressione ENTER para reiniciar");
        if (score > highScore) {
            highScore = score;
            highScoreText.setText("High: " + highScore);
            saveHighScore();
        }
    }

    // --- core update ---
    private void update(double delta) {
        // collector movement
        double dx = 0;
        if (movingLeft) dx -= COLLECTOR_SPEED * delta;
        if (movingRight) dx += COLLECTOR_SPEED * delta;
        collector.moveBy(dx);
        collector.clamp(0, WIDTH);

        // spawn
        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0;
            spawnInterval = Math.max(0.25, spawnInterval * SPAWN_ACCEL);
            itemSpeed += 2.0;
            spawnItem();
        }

        // move items
        Iterator<GameItem> it = items.iterator();
        while (it.hasNext()) {
            GameItem gi = it.next();
            gi.y += itemSpeed * delta;
            gi.updateView();
            if (gi.y > HEIGHT) {
                // missed
                it.remove();
                root.getChildren().remove(gi.view);
                score = Math.max(0, score - 1);
                updateUI();
            } else if (collector.intersects(gi)) {
                // caught
                applyItemEffect(gi.type);
                it.remove();
                root.getChildren().remove(gi.view);
                updateUI();
            }
        }
    }

    // --- item handling ---
    private void spawnItem() {
        ItemType type = ItemType.values()[rng.nextInt(ItemType.values().length)];
        Color color = switch (type) {
            case METAL -> Color.GOLD;
            case BATTERY -> Color.CRIMSON;
            case PLASTIC -> Color.LIMEGREEN;
            case REUSE -> Color.LIGHTGRAY;
        };
        double size = 36;
        double x = rng.nextDouble(10, Math.max(10, WIDTH - size - 10));
        double y = -size - rng.nextDouble(10, 80);
        GameItem gi = new GameItem(type, x, y, size, color);
        items.add(gi);
        root.getChildren().add(gi.view);
        // garante que a cesta continue em primeiro plano após spawnar novos itens
        if (collector != null) collector.view.toFront();
    }

    private void applyItemEffect(ItemType type) {
        switch (type) {
            case METAL -> score += 2;
            case PLASTIC -> score += 1;
            case REUSE -> score += 3;
            case BATTERY -> {
                lives -= 1;
                if (lives <= 0) endGame();
            }
        }
    }

    private void clearItems() {
        for (GameItem gi : items) root.getChildren().remove(gi.view);
        items.clear();
    }

    // --- UI helpers ---
    private void updateUI() {
        scoreText.setText("Score: " + score);
        livesText.setText("Lives: " + lives);
        highScoreText.setText("High: " + highScore);
    }

    private void showTitle() {
        msgText.setText("RECICLA MACK\nPressione ENTER para começar");
        msgText.setFont(Font.font(28));
    }

    private Text createText(double x, double y, String txt, int size, Color color) {
        Text t = new Text(x, y, txt);
        t.setFont(Font.font(size));
        t.setFill(color);
        return t;
    }

    // --- persistence ---
    private void loadHighScore() {
        try {
            if (Files.exists(HIGH_SCORE_FILE)) {
                String s = Files.readString(HIGH_SCORE_FILE).trim();
                highScore = Integer.parseInt(s);
            }
        } catch (Exception ignored) { highScore = 0; }
    }

    private void saveHighScore() {
        try {
            Files.writeString(HIGH_SCORE_FILE, Integer.toString(highScore));
        } catch (IOException ignored) { }
    }

    // --- small OOP model classes ---
    private static final class Collector {
        final Rectangle view;
        double x, y, w, h;
        Collector(double x, double y, double w, double h, Color color) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            view = new Rectangle(w, h, color);
            view.setTranslateX(x);
            view.setTranslateY(y);
        }
        void moveBy(double dx) {
            x += dx;
            view.setTranslateX(x);
        }
        void clamp(double minX, double maxWidth) {
            x = Math.max(minX, Math.min(maxWidth - w, x));
            view.setTranslateX(x);
        }
        boolean intersects(GameItem gi) {
            return gi.view.getBoundsInParent().intersects(view.getBoundsInParent());
        }
    }

    private static final class GameItem {
        final Rectangle view;
        final ItemType type;
        double x, y, size;
        GameItem(ItemType type, double x, double y, double size, Color color) {
            this.type = type;
            this.x = x; this.y = y; this.size = size;
            view = new Rectangle(size, size, color);
            updateView();
        }
        void updateView() {
            view.setTranslateX(x);
            view.setTranslateY(y);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
