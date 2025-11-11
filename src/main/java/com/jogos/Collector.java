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
import java.util.Objects;

/**
 * Collector com personagem animado à direita (sincronizado com o movimento).
 * Mantém hitbox e velocidade originais.
 */
public class Collector {

    private final Group node; // imageView + hitbox + personagem
    private final ImageView imageView;
    private final Rectangle hitboxRect;
    private final AnimatedSprite personagem;

    // lógica original
    public double x, y;
    private final double desiredHeight;
    private final Rectangle2D visibleInImage;
    private double renderedW = 0;
    private double renderedH = 0;
    private double hitboxXLocal = 0;
    private double hitboxYLocal = 0;

    public Collector(double x, double y, double desiredHeight) {
        this.x = x;
        this.y = y;
        this.desiredHeight = desiredHeight;

        // imagem original do coletor
        Image img = ImageLoader.load("MackTrashBin.png");
        Rectangle2D visible = ImageLoader.getVisibleBounds("MackTrashBin.png");
        this.visibleInImage = visible != null ? visible : new Rectangle2D(0, 0, img.getWidth(), img.getHeight());

        imageView = new ImageView(img);
        imageView.setViewport(this.visibleInImage);
        imageView.setFitHeight(desiredHeight);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // hitbox original
        hitboxRect = new Rectangle(10, 10);
        hitboxRect.setFill(Color.color(1, 0, 0, 0.0));
        hitboxRect.setStroke(Color.RED);
        hitboxRect.setStrokeWidth(2);
        hitboxRect.setVisible(false);

        // personagem animado (altura igual à do coletor)
        personagem = new AnimatedSprite(
                "com/jogos/Empurrando",
                "com/jogos/Parado",
                "com/jogos/Puxando",
                desiredHeight * 0.95 // altura quase igual ao coletor
        );

        node = new Group(imageView, hitboxRect, personagem.getNode());
        Platform.runLater(this::updateHitboxFromImage);
        updateView();
    }

    private void updateHitboxFromImage() {
        Bounds b = imageView.getBoundsInLocal();
        renderedW = b.getWidth();
        renderedH = b.getHeight();

        double offsetX = 0;
        double offsetY = 0;
        imageView.setTranslateX(offsetX);
        imageView.setTranslateY(offsetY);

        double hbW = Math.max(12, renderedW * 0.55);
        double hbH = Math.max(12, renderedH * 0.32);
        hitboxXLocal = offsetX + (renderedW - hbW) / 2.0;
        hitboxYLocal = offsetY + renderedH - hbH - (renderedH * 0.04);

        hitboxRect.setTranslateX(hitboxXLocal);
        hitboxRect.setTranslateY(hitboxYLocal);
        hitboxRect.setWidth(hbW);
        hitboxRect.setHeight(hbH);
    }

    public void applyInput(double dir, double screenWidth) {
        double speed = Math.max(6.0, screenWidth * 0.012); // velocidade original
        x += dir * speed;

        double visibleW = renderedW > 0 ? renderedW :
                (desiredHeight * (imageView.getImage().getWidth() / imageView.getImage().getHeight()));
        if (x < 0) x = 0;
        if (x + visibleW > screenWidth) x = screenWidth - visibleW;

        // animação conforme direção
        if (dir < 0) personagem.play("Empurrando");
        else if (dir > 0) personagem.play("Puxando");
        else personagem.play("Parado");

        updateView();
    }

    public void updateView() {
        node.setTranslateX(x);
        node.setTranslateY(y);

        updateHitboxFromImage();

        // Escala boa (mantida)
        double escala = 3.0;
        personagem.getNode().setScaleX(escala);
        personagem.getNode().setScaleY(escala);

        // Ajuste fino de posição
        double personagemOffsetX = renderedW - 120; // encosta mais no coletor
        double personagemOffsetY = renderedH - personagem.getHeight() - 10;
        personagem.getNode().setTranslateX(personagemOffsetX);
        personagem.getNode().setTranslateY(personagemOffsetY);
    }


    public Node getNode() { return node; }
    public void setHitboxVisible(boolean visible) { hitboxRect.setVisible(visible); }

    public boolean intersects(GameItem item) {
        Bounds hb = hitboxRect.localToScene(hitboxRect.getBoundsInLocal());
        Rectangle2D ri = new Rectangle2D(hb.getMinX(), hb.getMinY(), hb.getWidth(), hb.getHeight());
        Rectangle2D rj = item.getGlobalBounds();
        return ri.intersects(rj);
    }

    public Rectangle2D getBounds() {
        Bounds hb = hitboxRect.localToScene(hitboxRect.getBoundsInLocal());
        return new Rectangle2D(hb.getMinX(), hb.getMinY(), hb.getWidth(), hb.getHeight());
    }
}
