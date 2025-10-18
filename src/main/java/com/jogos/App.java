// ...existing code...
package com.jogos;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class App extends Application {

    private Pane root;
    private Rectangle coletor;
    private List<Rectangle> itens = new ArrayList<>();
    private int score = 0;
    private Text scoreText;
    private Text livesText;
    private Text gameOverText;

    private double velocidadeItens = 100; // px/s
    private final int LARGURA = 600;
    private final int ALTURA = 800;
    private Random random = new Random();

    // movimento suave do coletor
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private double coletorSpeed = 400; // px por segundo

    // dificuldade / spawn
    private double spawnInterval = 1.0; // segundos
    private double spawnTimer = 0.0;
    private double dificuldadeAumento = 0.995; // fator de redução do intervalo

    // vidas / estado
    private int lives = 3;
    private boolean gameOver = false;

    private enum ItemType { METAL, BATTERY, PLASTIC, REUSE }

    @Override
    public void start(Stage primaryStage) {
        root = new Pane();
        Scene scene = new Scene(root, LARGURA, ALTURA);

        // Criando o coletor
        coletor = new Rectangle(100, 20, Color.BLUE);
        coletor.setTranslateX(LARGURA / 2 - 50);
        coletor.setTranslateY(ALTURA - 50);
        root.getChildren().add(coletor);

        // Texto da pontuação e vidas
        scoreText = new Text(10, 20, "Score: 0");
        scoreText.setFill(Color.BLACK);
        scoreText.setStyle("-fx-font-size: 20;");
        root.getChildren().add(scoreText);

        livesText = new Text(10, 45, "Lives: " + lives);
        livesText.setFill(Color.BLACK);
        livesText.setStyle("-fx-font-size: 18;");
        root.getChildren().add(livesText);

        gameOverText = new Text(LARGURA/2 - 100, ALTURA/2, "");
        gameOverText.setFill(Color.RED);
        gameOverText.setStyle("-fx-font-size: 36;");
        root.getChildren().add(gameOverText);

        // Movimentação suave: pressed / released
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                movingLeft = true;
            } else if (event.getCode() == KeyCode.RIGHT) {
                movingRight = true;
            } else if (event.getCode() == KeyCode.R && gameOver) {
                restartGame();
            }
        });
        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                movingLeft = false;
            } else if (event.getCode() == KeyCode.RIGHT) {
                movingRight = false;
            }
        });

        // Loop principal do jogo
        AnimationTimer timer = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                double delta = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                if (!gameOver) {
                    // movimento coletor
                    updateColetor(delta);
                    // spawn controlado
                    spawnTimer += delta;
                    if (spawnTimer >= spawnInterval) {
                        spawnItem();
                        spawnTimer = 0;
                        // aumenta a dificuldade
                        spawnInterval = Math.max(0.2, spawnInterval * dificuldadeAumento);
                        velocidadeItens += 2; // aumenta levemente velocidade dos itens
                    }
                    moveItens(delta);
                    checkColisao();
                }
            }
        };
        timer.start();

        primaryStage.setTitle("Jogo de Reciclagem de Lixo Eletrônico");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Limita posição e atualiza via delta
    private void updateColetor(double delta) {
        double dx = 0;
        if (movingLeft) dx -= coletorSpeed * delta;
        if (movingRight) dx += coletorSpeed * delta;
        double nx = coletor.getTranslateX() + dx;
        nx = Math.max(0, Math.min(LARGURA - coletor.getWidth(), nx));
        coletor.setTranslateX(nx);
    }

    // Cria um item aleatório que cai do topo com tipo
    private void spawnItem() {
        int r = random.nextInt(4);
        ItemType type = ItemType.values()[r];
        Color cor = switch (type) {
            case METAL -> Color.GOLD;      // Metais preciosos
            case BATTERY -> Color.RED;     // Baterias perigosas
            case PLASTIC -> Color.GREEN;   // Plásticos e cabos
            case REUSE -> Color.GRAY;      // Itens de reuso
        };

        Rectangle item = new Rectangle(40, 40, cor);
        item.setTranslateX(random.nextInt(LARGURA - 40));
        item.setTranslateY(-40);
        item.setUserData(type); // guarda o tipo
        itens.add(item);
        root.getChildren().add(item);
    }

    // Move os itens para baixo baseado em velocidade (px/s)
    private void moveItens(double delta) {
        Iterator<Rectangle> it = itens.iterator();
        while (it.hasNext()) {
            Rectangle item = it.next();
            item.setTranslateY(item.getTranslateY() + velocidadeItens * delta);

            // Remove item se passar da tela
            if (item.getTranslateY() > ALTURA) {
                it.remove();
                root.getChildren().remove(item);
                // penalidade por perder item: -1 ponto (ou ajustar por tipo)
                score -= 1;
                updateScore();
            }
        }
    }

    // Verifica colisão com o coletor e efeitos por tipo
    private void checkColisao() {
        Iterator<Rectangle> it = itens.iterator();
        while (it.hasNext()) {
            Rectangle item = it.next();
            if (coletor.getBoundsInParent().intersects(item.getBoundsInParent())) {
                ItemType type = (ItemType) item.getUserData();
                switch (type) {
                    case METAL -> score += 2;
                    case PLASTIC -> score += 1;
                    case REUSE -> score += 3;
                    case BATTERY -> {
                        // item perigoso: perde vida
                        lives -= 1;
                        livesText.setText("Lives: " + lives);
                        if (lives <= 0) endGame();
                    }
                }
                updateScore();
                root.getChildren().remove(item);
                it.remove();
            }
        }
    }

    private void updateScore() {
        scoreText.setText("Score: " + score);
    }

    private void endGame() {
        gameOver = true;
        gameOverText.setText("GAME OVER\nPressione R para reiniciar");
    }

    private void restartGame() {
        // limpa itens
        for (Rectangle r : itens) root.getChildren().remove(r);
        itens.clear();
        score = 0;
        lives = 3;
        velocidadeItens = 100;
        spawnInterval = 1.0;
        spawnTimer = 0.0;
        gameOver = false;
        gameOverText.setText("");
        updateScore();
        livesText.setText("Lives: " + lives);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
// ...existing code...
