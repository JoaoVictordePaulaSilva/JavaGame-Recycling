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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;

// --- Imports Adicionados ---
import javafx.scene.Node; // Importante: Usado tanto no GameItem quanto no Collector
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.InputStream;
// --- Fim dos Imports Adicionados ---

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Versão refatorada: orientação a objetos, melhor UX (menu, pausa, reinício),
 * movimento suave, configuração centralizada e persistência de highscore.
 * --- MODIFICADO para incluir imagem PNG para o item REUSE ---
 * --- MODIFICADO (2) para incluir imagem PNG para o COLLECTOR ---
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
    private Image reuseImage; // <-- Para o item "DirtyPaper.png"
    private Image collectorImage; // <-- NOVO: Para a lixeira "MackTrashBin.png"

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
        loadImages(); // <-- Carrega AMBAS as imagens

        root = new Pane();
        // garante que o root tenha o tamanho lógico do jogo e fique ancorado ao topo-esquerdo
        root.setPrefSize(WIDTH, HEIGHT);
        StackPane container = new StackPane(root);
        StackPane.setAlignment(root, Pos.TOP_LEFT); // preserva o sistema de coordenadas original
        container.setStyle("-fx-background-color: linear-gradient(#b3e5fc, #ffffff);");
        Scene scene = new Scene(container, WIDTH, HEIGHT);

        // Ajuste automático para caber na tela: calcula escala e centraliza
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double maxW = Math.max(200, vb.getWidth() - 40);      // margem mínima
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

        // --- collector (MODIFICADO) ---
        // Define as dimensões desejadas para a lixeira
        double collectorW = 120;
        double collectorH = 120; // Ajuste a altura conforme necessário para a imagem
        double collectorX = WIDTH/2.0 - (collectorW / 2.0);
        double collectorY = HEIGHT - 130; // Posição Y (distância do fundo)

        if (collectorImage != null) {
            // Usa a imagem se ela foi carregada
            collector = new Collector(collectorX, collectorY, collectorW, collectorH, collectorImage);
        } else {
            // Fallback: Usa o retângulo azul se a imagem falhar
            System.err.println("Fallback: Usando retângulo azul para o coletor.");
            collector = new Collector(collectorX, HEIGHT - 100, collectorW, 36, Color.DODGERBLUE);
        }

        // Garante que a lixeira (Node) fique sempre visível à frente dos itens
        root.getChildren().add(collector.view);
        collector.view.toFront();
        // --- Fim da modificação do collector ---

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

        stage.setTitle("ReciclaMack - Refatorado (com Imagem)");
        stage.show();
    }

    // --- Método loadImages() MODIFICADO ---
    /**
     * Carrega as imagens do jogo a partir dos recursos (classpath).
     */
    private void loadImages() {
        // 1. Carrega 'DirtyPaper.png' (item REUSE)
        try (InputStream is = App.class.getResourceAsStream("DirtyPaper.png")) {
            if (is == null) {
                System.err.println("Erro: Não foi possível encontrar 'DirtyPaper.png' no pacote 'com.jogos'. Usando cor de fallback.");
                reuseImage = null;
            } else {
                reuseImage = new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar 'DirtyPaper.png'.");
            e.printStackTrace();
            reuseImage = null;
        }
        
        // 2. NOVO: Carrega 'MackTrashBin.png' (Coletor)
        try (InputStream is = App.class.getResourceAsStream("MackTrashBin.png")) {
            if (is == null) {
                System.err.println("Erro: Não foi possível encontrar 'MackTrashBin.png' no pacote 'com.jogos'. Usando fallback.");
                collectorImage = null;
            } else {
                collectorImage = new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar 'MackTrashBin.png'.");
            e.printStackTrace();
            collectorImage = null;
        }
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
            spawnItem(); // <-- Método foi modificado
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
            } else if (collector.intersects(gi)) { // <-- Intersecção funciona com Node
                // caught
                applyItemEffect(gi.type);
                it.remove();
                root.getChildren().remove(gi.view);
                updateUI();
            }
        }
    }

    // --- item handling ---
    /**
     * Método modificado para usar a imagem 'reuseImage' se disponível
     * para o tipo REUSE, ou uma cor como fallback.
     */
    private void spawnItem() {
        ItemType type = ItemType.values()[rng.nextInt(ItemType.values().length)];
        double size = 36;
        double x = rng.nextDouble(10, Math.max(10, WIDTH - size - 10));
        double y = -size - rng.nextDouble(10, 80);

        GameItem gi;

        // Verifica se é REUSE e se a imagem foi carregada com sucesso
        if (type == ItemType.REUSE && reuseImage != null) {
            // Usa o construtor de Imagem
            gi = new GameItem(type, x, y, size, reuseImage);
        } else {
            // Lógica original: Usa o construtor de Cor
            Color color = switch (type) {
                case METAL -> Color.GOLD;
                case BATTERY -> Color.CRIMSON;
                case PLASTIC -> Color.LIMEGREEN;
                case REUSE -> Color.LIGHTGRAY; // Cor de fallback se a imagem falhar
            };
            gi = new GameItem(type, x, y, size, color);
        }

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
        livesText.setText("Lives: " + lives); // Corrigido: Removido 'D'
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

    // --- Classe Collector MODIFICADA ---
    private static final class Collector {
        final Node view; // <-- Alterado de Rectangle para Node
        double x, y, w, h;

        /**
         * Construtor para Coletor baseado em Cor (Cria um Rectangle)
         */
        Collector(double x, double y, double w, double h, Color color) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.view = new Rectangle(w, h, color); // Usa Rectangle
            // Adiciona borda preta para o retângulo de fallback
            ((Rectangle)this.view).setStroke(Color.BLACK);
            ((Rectangle)this.view).setStrokeWidth(1.0);
            updateView();
        }
        
        /**
         * NOVO Construtor para Coletor baseado em Imagem (Cria um ImageView)
         */
        Collector(double x, double y, double w, double h, Image image) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            ImageView iv = new ImageView(image);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(true); // Mantém a proporção
            this.view = iv; // Usa ImageView
            updateView();
        }

        void moveBy(double dx) {
            x += dx;
            updateView();
        }

        void clamp(double minX, double maxWidth) {
            x = Math.max(minX, Math.min(maxWidth - w, x));
            updateView();
        }
        
        void updateView() {
            view.setTranslateX(x);
            view.setTranslateY(y);
        }

        boolean intersects(GameItem gi) {
            // Funciona com Node (Rectangle ou ImageView)
            return gi.view.getBoundsInParent().intersects(view.getBoundsInParent());
        }
    }
    // --- Fim da Classe Collector Modificada ---


    // --- Classe GameItem (Original modificada) ---
    private static final class GameItem {
        final Node view; // <-- Alterado de Rectangle para Node
        final ItemType type;
        double x, y, size;

        /**
         * Construtor para Itens baseados em Cor (Cria um Rectangle)
         */
        GameItem(ItemType type, double x, double y, double size, Color color) {
            this.type = type;
            this.x = x; this.y = y; this.size = size;
            this.view = new Rectangle(size, size, color); // Usa Rectangle
            updateView();
        }

        /**
         * Novo Construtor para Itens baseados em Imagem (Cria um ImageView)
         */
        GameItem(ItemType type, double x, double y, double size, Image image) {
            this.type = type;
            this.x = x; this.y = y; this.size = size;
            ImageView iv = new ImageView(image);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true); // Mantém a proporção da imagem
            this.view = iv; // Usa ImageView
            updateView();
        }

        void updateView() {
            view.setTranslateX(x);
            view.setTranslateY(y);
        }
    }
    // --- Fim da Classe GameItem ---


    public static void main(String[] args) {
        launch(args);
    }
}
