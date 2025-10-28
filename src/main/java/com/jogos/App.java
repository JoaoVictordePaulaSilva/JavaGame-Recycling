package com.jogos;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Node;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class App extends Application {

    private static final int WIDTH = 720;
    private static final int HEIGHT = 900;
    private static final Path HIGH_SCORE_FILE = Path.of("highscore.txt");

    private static final double START_ITEM_SPEED = 120.0; // px/s
    private static final double START_SPAWN_INTERVAL = 1.0; // s
    private static final double SPAWN_ACCEL = 0.996;
    private static final double COLLECTOR_SPEED = 420.0;
    private static final int START_LIVES = 3;

    private Pane root;
    private Text scoreText, livesText, infoText, highScoreText, msgText;
    private Collector collector;
    private javafx.scene.image.Image reuseImage;
    private javafx.scene.image.Image collectorImage;

    private final List<GameItem> items = new ArrayList<>();
    private double itemSpeed = START_ITEM_SPEED;
    private double spawnInterval = START_SPAWN_INTERVAL;
    private double spawnTimer = 0.0;
    private int score = 0;
    private int lives = START_LIVES;
    private int highScore = 0;

    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean running = false;
    private boolean paused = false;

    private final Random rng = ThreadLocalRandom.current();
    // enum local removida — use com.jogos.ItemType (arquivo ItemType.java)

    @Override
    public void start(Stage stage) {
        // carrega highscore e imagens via classes dedicadas
        highScore = HighScoreManager.load(HIGH_SCORE_FILE);
        reuseImage = ImageLoader.load("DirtyPaper.png");
        // carrega a lata de lixo colocada em src/main/resources/com/jogos/LataDeLixo.jpg
        collectorImage = ImageLoader.load("LataDeLixo.jpg");
        if (collectorImage == null) {
            // fallback: tenta outros nomes/extensões se necessário
            collectorImage = ImageLoader.load("LataDeLixo.png");
        }

        root = new Pane();
        root.setPrefSize(WIDTH, HEIGHT);
        StackPane container = new StackPane(root);
        StackPane.setAlignment(root, Pos.TOP_LEFT);
        container.setStyle("-fx-background-color: linear-gradient(#b3e5fc, #ffffff);");
        Scene scene = new Scene(container, WIDTH, HEIGHT);

        // layout inicial: centraliza janela no monitor e aplica escala inicial
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double maxW = Math.max(200, vb.getWidth() - 40);
        double maxH = Math.max(200, vb.getHeight() - 80);
        double scale = Math.min(1.0, Math.min(maxW / WIDTH, maxH / HEIGHT));
        root.setScaleX(scale);
        root.setScaleY(scale);
        double windowW = WIDTH * scale;
        double windowH = HEIGHT * scale;
        stage.setScene(scene);
        stage.setWidth(windowW);
        stage.setHeight(windowH);
        stage.setX(vb.getMinX() + (vb.getWidth() - windowW) / 2);
        stage.setY(vb.getMinY() + (vb.getHeight() - windowH) / 2);
        stage.setResizable(false);

        // textos UI
        scoreText = createText(12, 28, "Score: 0", 18, Color.DARKBLUE);
        livesText = createText(12, 52, "Lives: " + lives, 16, Color.DARKRED);
        highScoreText = createText(WIDTH - 160, 28, "High: " + highScore, 16, Color.DARKGREEN);
        infoText = createText(12, HEIGHT - 12, "←/→ mover • P pausar • R reiniciar • ENTER iniciar", 14, Color.DIMGRAY);
        msgText = createText(WIDTH/2.0 - 220, HEIGHT/2.0 - 60, "", 28, Color.FIREBRICK);
        msgText.setTextOrigin(VPos.CENTER);
        root.getChildren().addAll(scoreText, livesText, highScoreText, infoText, msgText);

        // collector: dimensões maiores para a lata ficar visível
        double collectorW = 180;  // largura maior
        double collectorH = 320;  // altura bem maior para mostrar a lata completa
        double collectorX = WIDTH / 2.0 - (collectorW / 2.0);
        double collectorY = HEIGHT - collectorH - 10; // mais próximo da base

        if (collectorImage != null) {
            collector = new Collector(collectorX, collectorY, collectorW, collectorH, collectorImage);
            collector.setDebug(true); // mantém debug para ver hitbox
            System.out.println("Imagem carregada: " + (collectorImage != null ? "SIM" : "NÃO"));
        } else {
            System.err.println("Fallback: Usando retângulo azul para o coletor.");
            collector = new Collector(collectorX, collectorY, 120, 48, Color.DODGERBLUE);
        }
        root.getChildren().add(collector.view);
        collector.view.toFront();

        // DEBUG: mostra hitbox da lata para calibrar visualmente — remova em produção
        collector.setDebug(true);
        // se quiser forçar um tamanho final diferente, use:
        // collector.setSize(180, 180); 

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

        showTitle();

        AnimationTimer timer = new AnimationTimer() {
            private long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double delta = (now - last) / 1_000_000_000.0;
                last = now;
                if (running && !paused) update(delta);
            }
        };
        timer.start();

        // binding dinâmico de escala baseado no tamanho da Scene (mantém proporção)
        DoubleBinding scaleBinding = Bindings.createDoubleBinding(
            () -> {
                double sw = scene.getWidth();
                double sh = scene.getHeight();
                double s = Math.min(sw / WIDTH, sh / HEIGHT);
                return Math.max(0.5, Math.min(1.0, s));
            },
            scene.widthProperty(), scene.heightProperty()
        );
        root.scaleXProperty().bind(scaleBinding);
        root.scaleYProperty().bind(scaleBinding);

        // permite redimensionar — binding cuida da escala
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setResizable(true);

        stage.setTitle("ReciclaMack - Refatorado (com Imagem)");
        stage.show();
    }

    private void startGame() {
        running = true; paused = false;
        score = 0; lives = START_LIVES;
        itemSpeed = START_ITEM_SPEED; spawnInterval = START_SPAWN_INTERVAL; spawnTimer = 0;
        clearItems(); updateUI(); msgText.setText("");
    }

    private void togglePause() {
        paused = !paused;
        msgText.setText(paused ? "PAUSADO" : "");
    }

    private void restart() {
        running = false; paused = false; clearItems(); showTitle();
    }

    private void endGame() {
        running = false;
        msgText.setText("GAME OVER — Pressione ENTER para reiniciar");
        if (score > highScore) {
            highScore = score;
            highScoreText.setText("High: " + highScore);
            HighScoreManager.save(HIGH_SCORE_FILE, highScore);
        }
    }

    private void update(double delta) {
        double dx = 0;
        if (movingLeft) dx -= COLLECTOR_SPEED * delta;
        if (movingRight) dx += COLLECTOR_SPEED * delta;
        collector.moveBy(dx);
        collector.clamp(0, WIDTH);

        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0;
            spawnInterval = Math.max(0.25, spawnInterval * SPAWN_ACCEL);
            itemSpeed += 2.0;
            spawnItem();
        }

        Iterator<GameItem> it = items.iterator();
        while (it.hasNext()) {
            GameItem gi = it.next();
            gi.y += itemSpeed * delta;
            gi.updateView();
            if (gi.y > HEIGHT) {
                it.remove();
                root.getChildren().remove(gi.view);
                score = Math.max(0, score - 1);
                updateUI();
            } else if (collector.intersects(gi)) {
                applyItemEffect(gi.type);
                it.remove();
                root.getChildren().remove(gi.view);
                updateUI();
            }
        }
    }

    private void spawnItem() {
        ItemType type = ItemType.values()[rng.nextInt(ItemType.values().length)];
        double size = 36;
        double x = rng.nextDouble(10, Math.max(10, WIDTH - size - 10));
        double y = -size - rng.nextDouble(10, 80);

        GameItem gi;
        if (type == ItemType.REUSE && reuseImage != null) {
            gi = new GameItem(type, x, y, size, reuseImage);
        } else {
            Color color = switch (type) {
                case METAL -> Color.GOLD;
                case BATTERY -> Color.CRIMSON;
                case PLASTIC -> Color.LIMEGREEN;
                case REUSE -> Color.LIGHTGRAY;
            };
            gi = new GameItem(type, x, y, size, color);
        }

        items.add(gi);
        root.getChildren().add(gi.view);
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

    public static void main(String[] args) {
        launch(args);
    }
}
