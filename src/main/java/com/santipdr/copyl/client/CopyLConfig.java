package com.santipdr.copyl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Minimal persistent configuration for the CopyL-only client. */
public final class CopyLConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("lclient.json");
    private static CopyLConfig instance;

    /** Raw GLFW key used to open the CopyL editor. Not registered in vanilla Controls. */
    public int openKey = GLFW.GLFW_KEY_RIGHT_ALT;

    public static synchronized CopyLConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static CopyLConfig load() {
        if (!Files.exists(PATH)) {
            CopyLConfig config = new CopyLConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            CopyLConfig config = new CopyLConfig();

            // Preserve the old Lclient wheel key as CopyL's editor key during migration.
            if (root.has("openKey") && root.get("openKey").isJsonPrimitive()) {
                config.openKey = root.get("openKey").getAsInt();
            } else if (root.has("wheelKey") && root.get("wheelKey").isJsonPrimitive()) {
                config.openKey = root.get("wheelKey").getAsInt();
            }

            config.sanitize();
            // Always rewrite once so every removed Lclient-module field disappears from disk too.
            AtomicConfigIO.write(PATH, GSON.toJson(config));
            return config;
        } catch (Exception exception) {
            Path backup = AtomicConfigIO.backupBroken(PATH);
            System.err.println("[CopyL] No se pudo leer " + PATH + ": " + exception.getMessage()
                    + (backup == null ? "" : " · copia: " + backup));
            CopyLConfig config = new CopyLConfig();
            config.save();
            return config;
        }
    }

    private void sanitize() {
        if (openKey == -1) return;
        if (openKey < GLFW.GLFW_KEY_SPACE || openKey > GLFW.GLFW_KEY_LAST) {
            openKey = GLFW.GLFW_KEY_RIGHT_ALT;
        }
    }

    public synchronized void save() {
        sanitize();
        try {
            AtomicConfigIO.write(PATH, GSON.toJson(this));
        } catch (Exception exception) {
            System.err.println("[CopyL] No se pudo guardar " + PATH + ": " + exception.getMessage());
        }
    }
}
