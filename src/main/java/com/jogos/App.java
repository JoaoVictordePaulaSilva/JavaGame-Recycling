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
    private double velocidadeItens = 2; // Velocidade inicial dos itens
    private Random random = new Random();

    private final int LARGURA = 600;
    private final int ALTURA = 800;

    @Override
    public void start(Stage primaryStage) {
        root = new Pane();
        Scene scene = new Scene(root, LARGURA, ALTURA);

        // Criando o coletor
        coletor = new Rectangle(100, 20, Color.BLUE);
        coletor.setTranslateX(LARGURA / 2 - 50);
        coletor.setTranslateY(ALTURA - 50);
        root.getChildren().add(coletor);

        // Texto da pontuação
        scoreText = new Text(10, 20, "Score: 0");
        scoreText.setFill(Color.BLACK);
        scoreText.setStyle("-fx-font-size: 20;");
        root.getChildren().add(scoreText);

        // Movimentação do coletor
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                coletor.setTranslateX(coletor.getTranslateX() - 20);
            } else if (event.getCode() == KeyCode.RIGHT) {
                coletor.setTranslateX(coletor.getTranslateX() + 20);
            }
        });

        // Loop principal do jogo
        AnimationTimer timer = new AnimationTimer() {
            private long lastSpawn = 0;

            @Override
            public void handle(long now) {
                // Criar novos itens a cada 1s
                if (now - lastSpawn > 1_000_000_000L) {
                    spawnItem();
                    lastSpawn = now;
                }

                moveItens();
                checkColisao();
            }
        };
        timer.start();

        primaryStage.setTitle("Jogo de Reciclagem de Lixo Eletrônico");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Cria um item aleatório que cai do topo
    private void spawnItem() {
        Color cor = switch (random.nextInt(4)) {
            case 0 -> Color.GOLD;      // Metais preciosos
            case 1 -> Color.RED;       // Baterias perigosas
            case 2 -> Color.GREEN;     // Plásticos e cabos
            default -> Color.GRAY;     // Itens de reuso
        };

        Rectangle item = new Rectangle(40, 40, cor);
        item.setTranslateX(random.nextInt(LARGURA - 40));
        item.setTranslateY(0);
        itens.add(item);
        root.getChildren().add(item);
    }

    // Move os itens para baixo
    private void moveItens() {
        Iterator<Rectangle> it = itens.iterator();
        while (it.hasNext()) {
            Rectangle item = it.next();
            item.setTranslateY(item.getTranslateY() + velocidadeItens);

            // Remove item se passar da tela
            if (item.getTranslateY() > ALTURA) {
                it.remove();
                root.getChildren().remove(item);
                score -= 1; // Perde ponto se não coletar
                updateScore();
            }
        }
    }

    // Verifica colisão com o coletor
    private void checkColisao() {
        Iterator<Rectangle> it = itens.iterator();
        while (it.hasNext()) {
            Rectangle item = it.next();
            if (coletor.getBoundsInParent().intersects(item.getBoundsInParent())) {
                score += 1; // Acerto
                updateScore();
                root.getChildren().remove(item);
                it.remove();
            }
        }
    }

    private void updateScore() {
        scoreText.setText("Score: " + score);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
