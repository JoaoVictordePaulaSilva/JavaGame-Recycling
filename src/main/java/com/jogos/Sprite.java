package com.jogos;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Sprite {
    private final Group node;
    private final ImageView imageView;
    private final Rectangle debugRect;
    private final Rectangle2D visibleBounds;
    private final double logicalSize;

    public Sprite(Image image, Rectangle2D visibleBounds, double logicalSize) {
        this.visibleBounds = visibleBounds;
        this.logicalSize = logicalSize;

        imageView = new ImageView(image);
        imageView.setViewport(visibleBounds);
        imageView.setFitWidth(logicalSize);
        imageView.setFitHeight(logicalSize);
        imageView.setPreserveRatio(true);

        debugRect = new Rectangle(logicalSize, logicalSize);
        debugRect.setFill(Color.TRANSPARENT);
        debugRect.setStroke(Color.RED);
        debugRect.setVisible(false);

        node = new Group(imageView, debugRect);
    }

    public Node getNode() {
        return node;
    }

    public Rectangle2D getVisibleBounds() {
        return visibleBounds;
    }

    public void setDebug(boolean enabled) {
        debugRect.setVisible(enabled);
    }
}
