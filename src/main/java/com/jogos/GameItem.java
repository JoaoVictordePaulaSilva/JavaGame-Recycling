package com.jogos;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class GameItem {
    public final Node view;
    public final ItemType type;
    public double x, y, size;

    // construtor por cor (retângulo)
    public GameItem(ItemType type, double x, double y, double size, Color color) {
        this.type = type;
        this.x = x; this.y = y; this.size = size;
        this.view = new Rectangle(size, size, color);
        updateView();
    }

    // construtor por imagem
    public GameItem(ItemType type, double x, double y, double size, Image image) {
        this.type = type;
        this.x = x; this.y = y; this.size = size;
        ImageView iv = new ImageView(image);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        this.view = iv;
        updateView();
    }

    public void updateView() {
        view.setTranslateX(x);
        view.setTranslateY(y);
    }
}