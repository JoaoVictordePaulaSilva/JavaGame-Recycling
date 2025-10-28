package com.jogos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HighScoreManager {

    public static int load(Path p) {
        try {
            if (Files.exists(p)) {
                String s = Files.readString(p).trim();
                return Integer.parseInt(s);
            }
        } catch (Exception ignored) { }
        return 0;
    }

    public static void save(Path p, int value) {
        try {
            Files.writeString(p, Integer.toString(value));
        } catch (IOException ignored) { }
    }
}