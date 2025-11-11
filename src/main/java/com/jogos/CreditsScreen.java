package com.jogos;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class CreditsScreen {

    private final StackPane rootStack;
    private final Runnable onFinish;

    public CreditsScreen(StackPane rootStack, Runnable onFinish) {
        this.rootStack = rootStack;
        this.onFinish = onFinish;
    }

    public void show() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: black;");

        double width = rootStack.getWidth();
        double height = rootStack.getHeight();

        String creditsText = "Obrigado por jogar!\n\n" +
                "Desenvolvedores: João Victor de Paula Silva e Anderson De Lima Santos Júnior\n" +
                "Artista: Miguel Trezza Ferreira\n\n" +
                "Sons: Freesound.org (CC0 e CC BY 4.0)\n" +
                " - qubodup, Sadiquecat, ZHRØ\n\n" +
                "Créditos de áudio e imagens conforme licenças originais.";

        Text textNode = new Text(creditsText);
        textNode.setFill(Color.YELLOW);
        textNode.setFont(Font.font("Arial", 28));
        textNode.setWrappingWidth(width * 0.8);
        textNode.setTranslateY(height); // começa fora da tela (embaixo)

        overlay.getChildren().add(textNode);
        rootStack.getChildren().add(overlay);

        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.017), e -> {
            textNode.setTranslateY(textNode.getTranslateY() - 1.5);

            if (textNode.getTranslateY() + textNode.getBoundsInParent().getHeight() < 0) {
                timeline.stop();
                PauseTransition pause = new PauseTransition(Duration.seconds(3));
                pause.setOnFinished(ev -> {
                    rootStack.getChildren().remove(overlay);
                    onFinish.run();
                });
                pause.play();
            }
        });

        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
