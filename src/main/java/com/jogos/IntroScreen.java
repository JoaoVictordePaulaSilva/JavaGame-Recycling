package com.jogos;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class IntroScreen {

    private final StackPane rootStack; // StackPane principal do jogo
    private final Runnable onFinish;

    public IntroScreen(StackPane rootStack, Runnable onFinish) {
        this.rootStack = rootStack;
        this.onFinish = onFinish;
    }

    public void show() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: black;");

        double width = rootStack.getWidth();
        double height = rootStack.getHeight();

        String introText = "Super Carlos saiu correndo de casa ao ver uma chuva de objetos caindo do céu.\n" +
                "Animado, ele decidiu coletar tudo para transformar em reciclagem…\n" +
                "mas atenção: nem tudo é seguro! Entre os itens, algumas bombas podem atrapalhar sua missão!";

        Text textNode = new Text(introText);
        textNode.setFill(Color.YELLOW);
        textNode.setFont(Font.font("Arial", 28));
        textNode.setWrappingWidth(width * 0.8);
        textNode.setTranslateY(height * 0.8); // começa abaixo da tela

        overlay.getChildren().add(textNode);
        rootStack.getChildren().add(overlay);

        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.017), e -> {
            textNode.setTranslateY(textNode.getTranslateY() - 1.5); // move para cima

            // se o topo do texto chegou quase no topo da tela, faz pausa
            if (textNode.getTranslateY() <= 20) {
                timeline.stop(); // pausa o scroll
                PauseTransition pause = new PauseTransition(Duration.seconds(3)); // 3 segundos
                pause.setOnFinished(ev -> {
                    rootStack.getChildren().remove(overlay); // remove intro
                    onFinish.run(); // inicia jogo
                });
                pause.play();
            }
        });

        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
