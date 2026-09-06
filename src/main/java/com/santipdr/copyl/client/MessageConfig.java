package com.santipdr.copyl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class MessageConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("copyl-messages.json");
    private static final MessageConfig INSTANCE = new MessageConfig();
    private static final int MAX_MESSAGE_LENGTH = 256;

    private final String[] messages = new String[CopyLKeyMappings.SLOT_COUNT];
    private boolean loaded;

    private MessageConfig() {
        Arrays.fill(messages, "");
    }

    public static MessageConfig getInstance() {
        INSTANCE.ensureLoaded();
        return INSTANCE;
    }

    public synchronized String getMessage(int index) {
        ensureLoaded();
        checkIndex(index);
        return messages[index];
    }

    public synchronized void setMessage(int index, String message) {
        ensureLoaded();
        checkIndex(index);
        messages[index] = normalize(message);
    }

    public synchronized void save() {
        ensureLoaded();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(new ConfigData(messages.clone()), writer);
            }
        } catch (IOException exception) {
            System.err.println("[CopyL] Could not save " + CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;

        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null && data.messages != null) {
                for (int i = 0; i < messages.length && i < data.messages.length; i++) {
                    messages[i] = normalize(data.messages[i]);
                }
            }
        } catch (Exception exception) {
            System.err.println("[CopyL] Could not read " + CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= CopyLKeyMappings.SLOT_COUNT) {
            throw new IndexOutOfBoundsException("CopyL slot " + index + " is outside 0-9");
        }
    }

    private static final class ConfigData {
        private final String[] messages;

        private ConfigData(String[] messages) {
            this.messages = messages;
        }
    }
}
