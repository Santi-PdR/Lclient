package com.santipdr.copyl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Persistent configuration for Lclient's client-side modules. */
public final class LClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("lclient.json");
    private static LClientConfig instance;

    public int wheelKey = GLFW.GLFW_KEY_RIGHT_ALT;

    public boolean quickMessages = true;

    public boolean lootEsp = true;
    public int lootEspToggleKey = GLFW.GLFW_KEY_X;
    public int lootEspRange = 96;
    public int lootEspMinStack = 1;
    /** Adds a taller no-depth marker so loot remains obvious behind thick terrain. */
    public boolean lootEspBeacon = true;

    public boolean smartOffhand = true;
    public int foodThreshold = 14;
    public int foodRestoreThreshold = 18;
    /** Empty means AUTO. Otherwise this is the exact registry id to look for, e.g. minecraft:golden_carrot. */
    public String smartOffhandFoodId = "";
    /** When the selected food is missing, allow AUTO to choose another edible item. */
    public boolean smartOffhandFallbackToAuto = false;

    public boolean recon = true;
    public int reconZoomKey = GLFW.GLFW_KEY_C;
    public int reconWaypointKey = GLFW.GLFW_KEY_V;
    /** Lower FOV means stronger zoom. This value is also changed live by the mouse wheel. */
    public int reconZoomFov = 24;
    /** Maximum long-range raycast used by Recon instead of vanilla reach. */
    public int reconRange = 256;

    public boolean journeyMap = true;
    public boolean journeyMapReconWaypoint = true;

    public static synchronized LClientConfig get() {
        if (instance == null) instance = load();
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
            if (config == null) throw new IllegalStateException("config vacía");

            String before = GSON.toJson(config);
            config.sanitize();
            String after = GSON.toJson(config);
            if (!before.equals(after)) {
                // Persist migrations/repairs once instead of re-fixing the same
                // invalid values on every Minecraft start.
                AtomicConfigIO.write(PATH, after);
            }
            return config;
        } catch (Exception exception) {
            Path backup = AtomicConfigIO.backupBroken(PATH);
            System.err.println("[Lclient] No se pudo leer " + PATH + ": " + exception.getMessage()
                    + (backup == null ? "" : " · copia: " + backup));
            LClientConfig config = new LClientConfig();
            config.save();
            return config;
        }
    }

    private void sanitize() {
        wheelKey = sanitizeKey(wheelKey, GLFW.GLFW_KEY_RIGHT_ALT);
        lootEspToggleKey = sanitizeKey(lootEspToggleKey, GLFW.GLFW_KEY_X);
        reconZoomKey = sanitizeKey(reconZoomKey, GLFW.GLFW_KEY_C);
        reconWaypointKey = sanitizeKey(reconWaypointKey, GLFW.GLFW_KEY_V);

        if (lootEspToggleKey >= 0 && lootEspToggleKey == wheelKey) lootEspToggleKey = -1;
        if (reconZoomKey >= 0 && (reconZoomKey == wheelKey || reconZoomKey == lootEspToggleKey)) reconZoomKey = -1;
        if (reconWaypointKey >= 0 && (reconWaypointKey == wheelKey
                || reconWaypointKey == lootEspToggleKey
                || reconWaypointKey == reconZoomKey)) {
            reconWaypointKey = -1;
        }

        lootEspRange = clamp(lootEspRange, 16, 192, 96);
        lootEspMinStack = clamp(lootEspMinStack, 1, 64, 1);

        if (smartOffhandFoodId == null) smartOffhandFoodId = "";
        smartOffhandFoodId = smartOffhandFoodId.trim().toLowerCase(Locale.ROOT);
        if (smartOffhandFoodId.length() > 128) smartOffhandFoodId = smartOffhandFoodId.substring(0, 128);
        if (!smartOffhandFoodId.isEmpty() && ResourceLocation.tryParse(smartOffhandFoodId) == null) {
            smartOffhandFoodId = "";
        }

        foodThreshold = clamp(foodThreshold, 1, 19, 14);
        int minRestore = Math.min(20, foodThreshold + 1);
        foodRestoreThreshold = clamp(foodRestoreThreshold, minRestore, 20, Math.max(minRestore, 18));
        reconZoomFov = clamp(reconZoomFov, 8, 50, 24);
        reconRange = clamp(reconRange, 64, 512, 256);
    }

    private static int sanitizeKey(int key, int fallback) {
        if (key == -1) return -1;
        if (key < GLFW.GLFW_KEY_SPACE || key > GLFW.GLFW_KEY_LAST) return fallback;
        return key;
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value >= min && value <= max) return value;
        return Math.max(min, Math.min(max, fallback));
    }

    public synchronized void save() {
        sanitize();
        try {
            AtomicConfigIO.write(PATH, GSON.toJson(this));
        } catch (Exception exception) {
            System.err.println("[Lclient] No se pudo guardar " + PATH + ": " + exception.getMessage());
        }
    }
}
