package com.jogos.utils;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/**
 * Utilitários para trabalhar com imagens (sprites).
 */
public class ImageUtils {

    /**
     * Calcula o menor retângulo que cobre todos os pixels não transparentes da imagem.
     *
     * @param img imagem PNG com transparência
     * @return Rectangle2D com os limites visíveis (em coordenadas da imagem)
     */
    public static Rectangle2D calculateVisibleBounds(Image img) {
        PixelReader reader = img.getPixelReader();
        if (reader == null) {
            return new Rectangle2D(0, 0, img.getWidth(), img.getHeight());
        }

        int w = (int) img.getWidth();
        int h = (int) img.getHeight();

        int minX = w, minY = h, maxX = 0, maxY = 0;
        boolean found = false;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = reader.getArgb(x, y);
                int alpha = (argb >> 24) & 0xff;
                if (alpha > 10) { // tolerância de transparência
                    found = true;
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (!found) {
            // imagem totalmente transparente, retorna área inteira
            return new Rectangle2D(0, 0, w, h);
        }

        return new Rectangle2D(minX, minY, (maxX - minX), (maxY - minY));
    }
}
