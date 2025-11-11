package com.jogos;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnimatedSprite {
    private final Group node;
    private final ImageView imageView;
    private final double height;
    private final List<Image> empurrando = new ArrayList<>();
    private final List<Image> puxando = new ArrayList<>();
    private final List<Image> parado = new ArrayList<>();
    private List<Image> atual;

    private int frame = 0;
    private long lastFrame = 0;
    private final long frameDelay = 180_000_000; // 180ms por frame (mais lento e suave)

    private final AnimationTimer timer;

    public AnimatedSprite(String dirEmpurrando, String dirParado, String dirPuxando, double height) {
        this.height = height;
        loadFrames(empurrando, dirEmpurrando);
        loadFrames(puxando, dirPuxando);
        loadFrames(parado, dirParado);
        atual = parado;

        imageView = new ImageView(atual.get(0));
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(true);
        node = new Group(imageView);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastFrame > frameDelay) {
                    frame = (frame + 1) % atual.size();
                    imageView.setImage(atual.get(frame));
                    lastFrame = now;
                }
            }
        };
        timer.start();
    }

    private void loadFrames(List<Image> list, String dirPath) {
        File dir = new File("src/main/resources/" + dirPath);
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null) return;

        // ordena e carrega
        java.util.Arrays.sort(files);
        for (File f : files) list.add(new Image(f.toURI().toString()));
    }

    public void play(String anim) {
        switch (anim) {
            case "Empurrando" -> atual = empurrando;
            case "Puxando" -> atual = puxando;
            default -> atual = parado;
        }
    }

    public Group getNode() { return node; }
    public double getHeight() { return height; }
}
