package com.jogos;

import com.jogos.utils.ImageUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * GameItem:
 * - tenta carregar imagem via ImageLoader
 * - usa visible bounds da imagem (ImageUtils) para definir viewport + hitbox
 * - expõe métodos: updateView(), isOffScreen(...), getGlobalBounds(), setHitboxVisible(...)
 */
public class GameItem {
    public final ItemType type;
    public double x;
    public double y;
    private final double logicalSize; // target size on screen (max dimension)

    private final Group node; // imageView + hitbox
    private final ImageView imageView;
    private final Rectangle hitboxRect;
    private Rectangle2D visibleInImage; // pixels inside original image

    public GameItem(ItemType type, double startX, double startY, double size) {
        this.type = type;
        this.x = startX;
        this.y = startY;
        this.logicalSize = Math.max(24, size);

        // resource mapping
        String res = switch (type) {
            case METAL -> "BrokenBottle.png";
            case PLASTIC -> "BananaPeel.png";
            case REUSE -> "DirtyPaper.png";
            case BATTERY -> "Bomba.png";
            default -> null;
        };

        Image img = null;
        if (res != null) img = ImageLoader.load(res);

        if (img != null) {
            visibleInImage = ImageLoader.getVisibleBounds(res);
            if (visibleInImage == null) visibleInImage = new Rectangle2D(0, 0, img.getWidth(), img.getHeight());

            imageView = new ImageView(img);
            imageView.setViewport(visibleInImage);

            // scale to logicalSize using width of visible viewport
            double vw = visibleInImage.getWidth();
            double vh = visibleInImage.getHeight();
            if (vw <= 0) vw = img.getWidth();
            double scale = logicalSize / vw;
            imageView.setFitWidth(vw * scale);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            // create hitbox sized to rendered viewport scaled
            double renderedW = vw * scale;
            double renderedH = vh * scale;
            hitboxRect = new Rectangle(renderedW, renderedH);
            hitboxRect.setFill(Color.color(0, 1, 0, 0.0));
            hitboxRect.setStroke(Color.LIME);
            hitboxRect.setVisible(false);

            node = new Group(imageView, hitboxRect);

            // position image/rect within group (image at 0,0; hitbox the same but can be trimmed)
            imageView.setTranslateX(0);
            imageView.setTranslateY(0);
            hitboxRect.setTranslateX(0);
            hitboxRect.setTranslateY(0);

        } else {
            // fallback: plain rectangle
            imageView = null;
            Rectangle fallback = new Rectangle(logicalSize, logicalSize, switch (type) {
                case METAL -> Color.SILVER;
                case PLASTIC -> Color.DEEPSKYBLUE;
                case REUSE -> Color.GOLD;
                case BATTERY -> Color.CRIMSON;
                default -> Color.GRAY;
            });
            hitboxRect = new Rectangle(logicalSize, logicalSize);
            hitboxRect.setFill(Color.TRANSPARENT);
            hitboxRect.setStroke(Color.LIME);
            hitboxRect.setVisible(false);
            node = new Group(fallback, hitboxRect);
            fallback.setTranslateX(0);
            fallback.setTranslateY(0);
            hitboxRect.setTranslateX(0);
            hitboxRect.setTranslateY(0);
        }

        updateView();
    }

    public Node getNode() { return node; }

    public void updateView() {
        node.setTranslateX(x);
        node.setTranslateY(y);
    }

    public boolean isOffScreen(double screenH) {
        return y > screenH + 200;
    }

    public void setHitboxVisible(boolean visible) {
        hitboxRect.setVisible(visible);
    }

    /**
     * Returns the global bounds of the hitbox (scene coords) for collision checks.
     */
    public Rectangle2D getGlobalBounds() {
        javafx.geometry.Bounds b = hitboxRect.localToScene(hitboxRect.getBoundsInLocal());
        return new Rectangle2D(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
    }
}
