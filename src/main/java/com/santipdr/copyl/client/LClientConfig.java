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
    public int foodThreshold = 14;
    public int foodRestoreThreshold = 18;
    public boolean journeyMapAttackerWaypoint = true;

    public static synchronized LClientConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static LClientConfig load() {
        if (!Files.exists(PATH)) {
            LClientConfig config = new LClientConfig();
            config.save();
            return config;
        }
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            LClientConfig config = GSON.fromJson(reader, LClientConfig.class);
            return config == null ? new LClientConfig() : config;
        } catch (Exception exception) {
            System.err.println("[Lclient] No se pudo leer " + PATH + ": " + exception.getMessage());
            return new LClientConfig();
        }
    }

    public synchronized void save() {
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
