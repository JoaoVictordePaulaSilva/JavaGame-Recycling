package com.jogos;

import javafx.scene.image.Image;

import java.io.InputStream;

public final class ImageLoader {

    // tenta vários caminhos relativos/com leading slash e retorna null se não achar
    public static Image load(String resourceName) {
        String[] candidates = new String[] {
            resourceName,
            "/" + resourceName,
            "com/jogos/" + resourceName,
            "/com/jogos/" + resourceName
        };
        for (String r : candidates) {
            try (InputStream is = ImageLoader.class.getResourceAsStream(r)) {
                if (is != null) {
                    Image img = new Image(is);
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
}