package com.jogos;

public class SoundManager {
    private static double globalVolume = 1.0; // 100%

    public static void setGlobalVolume(double volume) {
        globalVolume = Math.max(0, Math.min(1, volume));
        // Aqui futuramente aplicaremos esse valor aos MediaPlayer e efeitos sonoros
    }

    public static double getGlobalVolume() {
        return globalVolume;
    }
}
