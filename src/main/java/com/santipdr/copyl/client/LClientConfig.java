package com.santipdr.copyl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("lclient.json");
    private static LClientConfig instance;

    public int wheelKey = GLFW.GLFW_KEY_RIGHT_ALT;
    public int reconMarkKey = GLFW.GLFW_KEY_V;

    public boolean quickMessages = true;
    public boolean soundRadar = true;
    public boolean lootEsp = true;
    public boolean combatPanel = true;
    public boolean smartOffhand = true;
    public boolean entityAlerts = true;
    public boolean recon = true;
    public boolean journeyMap = true;

    public int soundRadarRange = 72;
    public int lootEspRange = 64;
    public int entityAlertRange = 72;
    public int entityAlertWarmupTicks = 80;
    public int foodThreshold = 14;
    public int foodRestoreThreshold = 18;

    public boolean journeyMapAttackerWaypoint = true;
    public boolean journeyMapReconWaypoint = true;

    public static synchronized LClientConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static LClientConfig load() {
        LClientConfig config;
        if (!Files.exists(PATH)) {
            config = new LClientConfig();
            config.save();
            return config;
        }
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            config = GSON.fromJson(reader, LClientConfig.class);
            if (config == null) config = new LClientConfig();
        } catch (Exception exception) {
            System.err.println("[Lclient] No se pudo leer " + PATH + ": " + exception.getMessage());
            config = new LClientConfig();
        }
        config.sanitize();
        return config;
    }

    private void sanitize() {
        soundRadarRange = clamp(soundRadarRange, 24, 160, 72);
        lootEspRange = clamp(lootEspRange, 16, 160, 64);
        entityAlertRange = clamp(entityAlertRange, 16, 128, 72);
        entityAlertWarmupTicks = clamp(entityAlertWarmupTicks, 20, 200, 80);
        foodThreshold = clamp(foodThreshold, 1, 19, 14);
        int minRestore = Math.min(20, foodThreshold + 1);
        foodRestoreThreshold = clamp(foodRestoreThreshold, minRestore, 20, Math.max(minRestore, 18));
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value >= min && value <= max) return value;
        return Math.max(min, Math.min(max, fallback));
    }

    public synchronized void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception exception) {
            System.err.println("[Lclient] No se pudo guardar " + PATH + ": " + exception.getMessage());
        }
    }
}
