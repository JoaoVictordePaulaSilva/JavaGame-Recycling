package com.jogos;

import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.io.File;
import java.util.Arrays;

public class AnimatedBackground {
    private final ImageView imageView;
    private final Image[] frames;
    private int frame = 0;
    private long lastFrame = 0;
    private final long frameDelay = 300_000_000; // 300ms por frame (~3fps)
    private final AnimationTimer timer;

    public AnimatedBackground(String dirPath, double width, double height) {
        // Carrega as duas imagens da pasta (ordenadas)
        File dir = new File("src/main/resources/" + dirPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0)
            throw new RuntimeException("Nenhuma imagem encontrada em " + dirPath);
        Arrays.sort(files);

        frames = new Image[files.length];
        for (int i = 0; i < files.length; i++) {
            frames[i] = new Image(files[i].toURI().toString(), width, height, false, true);
        }

        imageView = new ImageView(frames[0]);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        // Timer da animação
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastFrame > frameDelay) {
                    frame = (frame + 1) % frames.length;
                    imageView.setImage(frames[frame]);
                    lastFrame = now;
                }
            }
        };
        timer.start();
    }

    public ImageView getView() {
        return imageView;
    }
}
