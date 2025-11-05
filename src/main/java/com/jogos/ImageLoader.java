package com.jogos;

import com.jogos.utils.ImageUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class ImageLoader {

    private static final Map<String, Image> imageCache = new HashMap<>();
    private static final Map<String, Rectangle2D> boundsCache = new HashMap<>();

    /**
     * Carrega uma imagem do classpath e faz cache automático.
     * @param resourceName caminho relativo ao pacote com/jogos
     * @return Image carregada ou null se não encontrada
     */
    public static Image load(String resourceName) {
        if (imageCache.containsKey(resourceName)) {
            return imageCache.get(resourceName);
        }

        String[] candidates = {
            resourceName,
            "/" + resourceName,
            "com/jogos/" + resourceName,
            "/com/jogos/" + resourceName
        };

        for (String r : candidates) {
            try (InputStream is = ImageLoader.class.getResourceAsStream(r)) {
                if (is != null) {
                    Image img = new Image(is);
                    imageCache.put(resourceName, img);

                    // calcula e armazena o bounding box visível
                    Rectangle2D visible = ImageUtils.calculateVisibleBounds(img);
                    boundsCache.put(resourceName, visible);

                    System.out.println("ImageLoader: carregou recurso -> " + r);
                    return img;
                }
            } catch (Exception e) {
                System.err.println("ImageLoader: erro carregando " + r + " -> " + e.getMessage());
            }
        }

        System.err.println("ImageLoader: recurso não encontrado: " + resourceName);
        return null;
    }

    /**
     * Retorna o bounding box visível calculado para uma imagem já carregada.
     * @param resourceName nome do recurso usado em load()
     */
    public static Rectangle2D getVisibleBounds(String resourceName) {
        return boundsCache.get(resourceName);
    }

    /**
     * Limpa os caches (se quiser recarregar imagens modificadas durante o jogo).
     */
    public static void clearCache() {
        imageCache.clear();
        boundsCache.clear();
    }
}
