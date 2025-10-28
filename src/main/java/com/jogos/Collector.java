package com.jogos;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

/**
 * Collector que mantém:
 * - imagem renderizada com preserveRatio
 * - hitbox (Rectangle) alinhada ao content renderizado da imagem
 * - métodos para debug / padding e cálculo de colisão previsível
 */
public class Collector {
    public final Node view; // Group contendo imageView + hitbox
    private final ImageView imageView; // null no fallback
    private final Rectangle visualRect; // fallback quando sem imagem
    private final Rectangle hitboxRect; // retângulo usado para colisão e debug (local ao group)
    public double x, y, w, h; // coordenadas lógicas do collector
    private double collisionInset = 0; // pixels a remover do hitbox por borda
    private boolean debug = false;
    private double hitboxExtraTop = 0; // ajustes finos se quiser expandir para cima
    private double hitboxExtraBottom = 0; // ... ou para baixo

    // fallback sem imagem
    public Collector(double x, double y, double w, double h, Color color) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        imageView = null;
        visualRect = new Rectangle(w, h, color);
        visualRect.setStroke(Color.BLACK);
        visualRect.setStrokeWidth(1.0);

        hitboxRect = new Rectangle(w, h);
        hitboxRect.setFill(Color.color(1, 1, 1, 0));
        hitboxRect.setStroke(Color.color(1, 0, 0, 0));
        hitboxRect.setStrokeType(StrokeType.INSIDE);

        Group g = new Group(visualRect, hitboxRect);
        this.view = g;
        updateView();
    }

    // com imagem: preserva proporção e depois calcula hitbox conforme conteúdo renderizado
    public Collector(double x, double y, double w, double h, Image image) {
        this.x = x; this.y = y; this.w = w; this.h = h;

        imageView = new ImageView(image);
        // usaremos fitHeight para manter proporção e tamanho previsível
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitHeight(h);   // força altura lógica; largura será ajustada pela proporção

        visualRect = null;

        hitboxRect = new Rectangle(Math.max(0, w), Math.max(0, h));
        hitboxRect.setFill(Color.color(1, 1, 1, 0));
        hitboxRect.setStroke(Color.color(1, 0, 0, 0));
        hitboxRect.setStrokeType(StrokeType.INSIDE);

        Group g = new Group(imageView, hitboxRect);
        this.view = g;

        // após a ImageView calcular seu tamanho, alinhar a imagem e atualizar hitbox
        imageView.boundsInLocalProperty().addListener((obs, oldB, newB) -> Platform.runLater(this::updateHitboxFromImage));
        Platform.runLater(this::updateHitboxFromImage);

        updateView();
    }

    private void updateHitboxFromImage() {
        if (imageView == null) {
            // fallback: use w/h
            hitboxRect.setTranslateX(collisionInset);
            hitboxRect.setTranslateY(collisionInset);
            hitboxRect.setWidth(Math.max(0, w - 2 * collisionInset));
            hitboxRect.setHeight(Math.max(0, h - 2 * collisionInset));
            return;
        }

        Bounds b = imageView.getBoundsInLocal(); // tamanho renderizado da imagem
        double renderedW = b.getWidth();
        double renderedH = b.getHeight();

        // center image horizontally inside logical width w
        double offsetX = (w - renderedW) / 2.0;
        if (Double.isNaN(offsetX)) offsetX = 0;

        // alinhar a imagem no topo do "slot" lógico (para a lata ficar "em cima" do chão)
        double offsetY = 0;

        imageView.setTranslateX(offsetX);
        imageView.setTranslateY(offsetY);

        // hitbox cobre a parte visível da imagem (pode ajustar inset/extra)
        double hbX = offsetX + collisionInset;
        double hbY = offsetY + collisionInset - hitboxExtraTop;
        double hbW = Math.max(0, renderedW - 2 * collisionInset);
        double hbH = Math.max(0, renderedH - 2 * collisionInset + hitboxExtraBottom);

        hitboxRect.setTranslateX(hbX);
        hitboxRect.setTranslateY(hbY);
        hitboxRect.setWidth(hbW);
        hitboxRect.setHeight(hbH);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        if (debug) {
            hitboxRect.setStroke(Color.color(1, 0, 0, 0.85));
            hitboxRect.setStrokeWidth(2.0);
        } else {
            hitboxRect.setStroke(Color.color(1, 0, 0, 0));
        }
    }

    public void setCollisionInset(double inset) {
        this.collisionInset = Math.max(0, inset);
        Platform.runLater(this::updateHitboxFromImage);
    }

    // expande/contrai a hitbox acima/abaixo da imagem (útil para cobrir o corpo da lata)
    public void setHitboxExtra(double extraTop, double extraBottom) {
        this.hitboxExtraTop = Math.max(0, extraTop);
        this.hitboxExtraBottom = Math.max(0, extraBottom);
        Platform.runLater(this::updateHitboxFromImage);
    }

    public void setSize(double newW, double newH) {
        this.w = newW;
        this.h = newH;
        if (imageView != null) {
            imageView.setFitHeight(newH);
        }
        Platform.runLater(this::updateHitboxFromImage);
        updateView();
    }

    public void moveBy(double dx) { x += dx; updateView(); }

    public void clamp(double minX, double maxWidth) {
        x = Math.max(minX, Math.min(maxWidth - w, x));
        updateView();
    }

    public void updateView() {
        view.setTranslateX(x);
        view.setTranslateY(y);
        Platform.runLater(this::updateHitboxFromImage);
    }

    // usa a hitbox calculada (coordenadas globais) para colisões com itens
    public boolean intersects(GameItem gi) {
        // hitbox global:
        double hbX = x + hitboxRect.getTranslateX();
        double hbY = y + hitboxRect.getTranslateY();
        double hbW = hitboxRect.getWidth();
        double hbH = hitboxRect.getHeight();

        Rectangle2D ri = new Rectangle2D(hbX, hbY, Math.max(0, hbW), Math.max(0, hbH));
        Rectangle2D rj = new Rectangle2D(gi.x, gi.y, gi.size, gi.size);
        return ri.intersects(rj);
    }
}