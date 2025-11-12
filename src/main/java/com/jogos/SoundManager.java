package com.jogos;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class SoundManager {

    private static double musicVolume = 1.0;
    private static double effectsVolume = 1.0;

    private static MediaPlayer backgroundMusic;
    private static AudioClip collectSound;
    private static AudioClip explosionSound;

    public static void init() {
        try {
            // === Música de fundo ===
            URL musicUrl = SoundManager.class.getResource("/com/jogos/Audios/668879__zhr__retroclassic-game-music.wav");
            if (musicUrl != null) {
                Media music = new Media(musicUrl.toExternalForm());
                backgroundMusic = new MediaPlayer(music);
                backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
                backgroundMusic.setVolume(musicVolume * 0.6);
            }

            // === Som de coleta (metade do volume base) ===
            URL collectUrl = SoundManager.class.getResource("/com/jogos/Audios/831946__sadiquecat__blowing-dji-mic3.wav");
            if (collectUrl != null) {
                collectSound = new AudioClip(collectUrl.toExternalForm());
                collectSound.setVolume(effectsVolume * 0.5);
            }

            // === Som de explosão ===
            URL explosionUrl = SoundManager.class.getResource("/com/jogos/Audios/67471__qubodup__m67_fragmentation_grenade_explosion_2_no_echo.wav");
            if (explosionUrl != null) {
                explosionSound = new AudioClip(explosionUrl.toExternalForm());
                explosionSound.setVolume(effectsVolume);
            }

        } catch (Exception e) {
            System.err.println("Erro ao carregar sons: " + e.getMessage());
        }
    }

    public static void playMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(musicVolume * 0.6);
            backgroundMusic.play();
        }
    }

    public static void stopMusic() {
        if (backgroundMusic != null) backgroundMusic.stop();
    }

    public static void playCollect() {
        if (collectSound != null) collectSound.play(effectsVolume * 0.5);
    }

    public static void playExplosion() {
        if (explosionSound != null) explosionSound.play(effectsVolume);
    }

    public static void setMusicVolume(double volume) {
        musicVolume = Math.max(0, Math.min(1, volume));
        if (backgroundMusic != null) backgroundMusic.setVolume(musicVolume * 0.6);
    }

    public static void setEffectsVolume(double volume) {
        effectsVolume = Math.max(0, Math.min(1, volume));
        if (collectSound != null) collectSound.setVolume(effectsVolume * 0.5);
        if (explosionSound != null) explosionSound.setVolume(effectsVolume);
    }

    public static double getMusicVolume() {
        return musicVolume;
    }

    public static double getEffectsVolume() {
        return effectsVolume;
    }
}
