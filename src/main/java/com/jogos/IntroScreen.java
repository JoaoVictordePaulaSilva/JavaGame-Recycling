package com.jogos;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class IntroScreen {

    private final Stage stage;
    private final Runnable onFinish; // ação após a introdução (iniciar o jogo)

    public IntroScreen(Stage stage, Runnable onFinish) {
        this.stage = stage;
        this.onFinish = onFinish;
    }

    public void show() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");

        String introText = "Super Carlos saiu correndo de casa ao ver uma chuva de objetos caindo do céu.\n" +
                "Animado, ele decidiu coletar tudo para transformar em reciclagem…\n" +
                "mas atenção: nem tudo é seguro! Entre os itens, algumas bombas podem atrapalhar sua missão!";

        Text textNode = new Text(introText);
        textNode.setFill(Color.YELLOW);
        textNode.setFont(Font.font("Arial", 28));
        textNode.setWrappingWidth(stage.getWidth() * 0.8); // quebra de linha automática
        textNode.setTranslateY(stage.getHeight()); // começa fora da tela (embaixo)

        root.getChildren().add(textNode);

        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
        stage.setScene(scene);

        // animação rolando para cima (estilo Star Wars)
        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.017), e -> {
            textNode.setTranslateY(textNode.getTranslateY() - 1.0); // velocidade do scroll
            if (textNode.getTranslateY() + textNode.getBoundsInParent().getHeight() < 0) {
                timeline.stop();
                onFinish.run(); // chama a ação de iniciar o jogo
            }
        });
        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
