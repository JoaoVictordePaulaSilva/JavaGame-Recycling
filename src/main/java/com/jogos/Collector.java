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
 * Collector com personagem animado à direita (sincronizado com o movimento).
 * Mantém hitbox e velocidade originais.
 */
public class Collector {

    private final Group node; // imageView + hitbox + personagem
    private final ImageView imageView;
    private final Rectangle hitboxRect;
    private final AnimatedSprite personagem;

    public double x, y;
    private final double desiredHeight;
    private final Rectangle2D visibleInImage;
    private double renderedW = 0;
    private double renderedH = 0;
    private double hitboxXLocal = 0;
    private double hitboxYLocal = 0;

    // controla a animação atual
    private String currentAnimation = "Parado";

    public Collector(double x, double y, double desiredHeight) {
        this.x = x;
        // 🔹 Abaixa ainda mais o coletor no eixo Y
        this.y = y + 60; 
        this.desiredHeight = desiredHeight;

        Image img = ImageLoader.load("MackTrashBin.png");
        Rectangle2D visible = ImageLoader.getVisibleBounds("MackTrashBin.png");
        this.visibleInImage = visible != null ? visible : new Rectangle2D(0, 0, img.getWidth(), img.getHeight());

        imageView = new ImageView(img);
        imageView.setViewport(this.visibleInImage);

        double coletorScale = 0.75;
        imageView.setFitHeight(desiredHeight * coletorScale);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        hitboxRect = new Rectangle(10, 10);
        hitboxRect.setFill(Color.color(1, 0, 0, 0.0));
        hitboxRect.setStroke(Color.RED);
        hitboxRect.setStrokeWidth(2);
        hitboxRect.setVisible(false);

        personagem = new AnimatedSprite(
                "com/jogos/Empurrando",
                "com/jogos/Parado",
                "com/jogos/Puxando",
                desiredHeight * 0.95
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
        double hbH = Math.max(12, renderedH * 0.48);
        hitboxXLocal = offsetX + (renderedW - hbW) / 2.0;
        hitboxYLocal = offsetY + renderedH - hbH - (renderedH * 0.04);

        hitboxRect.setTranslateX(hitboxXLocal);
        hitboxRect.setTranslateY(hitboxYLocal);
        hitboxRect.setWidth(hbW);
        hitboxRect.setHeight(hbH);
    }

    public void applyInput(double dir, double screenWidth) {
        double speed = Math.max(6.0, screenWidth * 0.012);
        x += dir * speed;

        double visibleW = renderedW > 0 ? renderedW :
                (desiredHeight * (imageView.getImage().getWidth() / imageView.getImage().getHeight()));
        if (x < 0) x = 0;
        if (x + visibleW > screenWidth) x = screenWidth - visibleW;

        if (dir < 0) setAnimation("Empurrando");
        else if (dir > 0) setAnimation("Puxando");
        else setAnimation("Parado");

        updateView();
    }

    private void setAnimation(String anim) {
        currentAnimation = anim;
        personagem.play(anim);
    }

    public void updateView() {
        node.setTranslateX(x);
        node.setTranslateY(y);

        updateHitboxFromImage();

        // 🔹 Escala menor
        double escala = 4.0;
        personagem.getNode().setScaleX(escala);
        personagem.getNode().setScaleY(escala);

        // 🔹 Ajuste horizontal independente
        double personagemOffsetX = renderedW - 110;
        switch (currentAnimation) {
            case "Parado": personagemOffsetX += 15; break;
            case "Empurrando": personagemOffsetX -= 10; break; // mov. esquerda 5px
            case "Puxando": personagemOffsetX -= 10; break;    // mov. esquerda 5px
        }

        // 🔹 Ajuste vertical: sobe um pouco o personagem
        double personagemOffsetY = renderedH - personagem.getHeight() - 70; // antes -35 → agora -45

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
