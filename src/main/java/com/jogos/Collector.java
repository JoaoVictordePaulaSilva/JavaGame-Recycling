package com.jogos;

import com.jogos.utils.ImageUtils;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Collector:
 * - Usa ImageLoader + ImageUtils para calcular a hitbox baseada na área visível da png.
 * - Escala automaticamente segundo o desiredHeight passado no construtor.
 * - Exponhe métodos para movimentação, interseção com GameItem e toggle de hitbox.
 */
public class Collector {

    private final Group node;         // imageView + hitbox rect
    private final ImageView imageView;
    private final Rectangle hitboxRect;

    // logical position (node.translateX/Y)
    public double x, y;
    private final double desiredHeight; // altura lógica (em px na tela)

    // visible bounds from image (in image pixels)
    private final Rectangle2D visibleInImage;

    // rendered sizes (computed)
    private double renderedW = 0;
    private double renderedH = 0;
    private double hitboxXLocal = 0;
    private double hitboxYLocal = 0;

    // input state
    private boolean left = false;
    private boolean right = false;

    public Collector(double x, double y, double desiredHeight) {
        this.x = x;
        this.y = y;
        this.desiredHeight = desiredHeight;

        // load image
        Image img = ImageLoader.load("MackTrashBin.png");
        Rectangle2D visible = ImageLoader.getVisibleBounds("MackTrashBin.png");
        this.visibleInImage = visible != null ? visible : new Rectangle2D(0, 0, img.getWidth(), img.getHeight());

        imageView = new ImageView(img);
        // use viewport to focus on visible area
        imageView.setViewport(this.visibleInImage);

        // fit by height (keeps aspect ratio of viewport)
        imageView.setFitHeight(desiredHeight);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // empty hitbox rect; size will be computed once imageView layout is resolved
        hitboxRect = new Rectangle(10, 10);
        hitboxRect.setFill(Color.color(1, 0, 0, 0.0));
        hitboxRect.setStroke(Color.RED);
        hitboxRect.setStrokeWidth(2);
        hitboxRect.setVisible(false);

        node = new Group(imageView, hitboxRect);

        // schedule hitbox compute after layout
        Platform.runLater(this::updateHitboxFromImage);

        updateView();
    }

    private void updateHitboxFromImage() {
        // imageView.getBoundsInLocal gives rendered viewport size
        Bounds b = imageView.getBoundsInLocal();
        renderedW = b.getWidth();
        renderedH = b.getHeight();

        // For collector, place the image so its visual bottom sits at node's bottom.
        // We'll center it horizontally within its renderedW width (node is anchored at x,y)
        double offsetX = 0;
        double offsetY = 0; // render at top of node; we'll position node so it appears at y.

        imageView.setTranslateX(offsetX);
        imageView.setTranslateY(offsetY);

        // Create hitbox that covers the lower center portion of the visible sprite.
        // Heuristics tuned for a typical trash bin: narrow width and placed near bottom.
        double hbW = Math.max(12, renderedW * 0.55);
        double hbH = Math.max(12, renderedH * 0.32);

        hitboxXLocal = offsetX + (renderedW - hbW) / 2.0;
        hitboxYLocal = offsetY + renderedH - hbH - (renderedH * 0.04);

        hitboxRect.setTranslateX(hitboxXLocal);
        hitboxRect.setTranslateY(hitboxYLocal);
        hitboxRect.setWidth(hbW);
        hitboxRect.setHeight(hbH);
    }

    // called by game loop to move collector according to input (-1 left, 0 still, 1 right)
    public void applyInput(double dir, double screenWidth) {
        double speed = Math.max(6.0, screenWidth * 0.012); // adapt speed to screen width
        x += dir * speed;
        // clamp so that the visible sprite doesn't go off-screen
        double visibleW = renderedW > 0 ? renderedW : (desiredHeight * (imageView.getImage().getWidth() / imageView.getImage().getHeight()));
        if (x < 0) x = 0;
        if (x + visibleW > screenWidth) x = screenWidth - visibleW;
        updateView();
    }

    public void updateView() {
        node.setTranslateX(x);
        node.setTranslateY(y);
        // ensure hitbox is updated (in case render info changed)
        updateHitboxFromImage();
    }

    public Node getNode() {
        return node;
    }

    public void setLeft(boolean v) { this.left = v; }
    public void setRight(boolean v) { this.right = v; }

    public void setHitboxVisible(boolean visible) {
        hitboxRect.setVisible(visible);
    }

    // crate intersection check with GameItem using global coordinates
    public boolean intersects(GameItem item) {
        Bounds hb = hitboxRect.localToScene(hitboxRect.getBoundsInLocal());
        Rectangle2D ri = new Rectangle2D(hb.getMinX(), hb.getMinY(), hb.getWidth(), hb.getHeight());
        Rectangle2D rj = item.getGlobalBounds();
        return ri.intersects(rj);
    }

    // convenience getter bounding rect in scene coords
    public Rectangle2D getBounds() {
        Bounds hb = hitboxRect.localToScene(hitboxRect.getBoundsInLocal());
        return new Rectangle2D(hb.getMinX(), hb.getMinY(), hb.getWidth(), hb.getHeight());
    }
}
