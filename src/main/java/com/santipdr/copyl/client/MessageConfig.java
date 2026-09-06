package com.santipdr.copyl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

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
    private static final int MAX_NAME_LENGTH = 24;

    private final String[] names = new String[CopyLKeyMappings.SLOT_COUNT];
    private final String[] messages = new String[CopyLKeyMappings.SLOT_COUNT];
    private final int[] keyCodes = new int[CopyLKeyMappings.SLOT_COUNT];
    private boolean loaded;

    private MessageConfig() {
        for (int i = 0; i < names.length; i++) names[i] = defaultName(i);
        Arrays.fill(messages, "");
        Arrays.fill(keyCodes, -1);
    }

    public static MessageConfig getInstance() {
        INSTANCE.ensureLoaded();
        return INSTANCE;
    }

    public synchronized String getName(int index) {
        ensureLoaded();
        checkIndex(index);
        return names[index];
    }

    public synchronized void setName(int index, String name) {
        ensureLoaded();
        checkIndex(index);
        names[index] = normalizeName(name, index);
    }

    public synchronized String getMessage(int index) {
        ensureLoaded();
        checkIndex(index);
        return messages[index];
    }

    public synchronized void setMessage(int index, String message) {
        ensureLoaded();
        checkIndex(index);
        messages[index] = normalizeMessage(message);
    }

    public synchronized int getKeyCode(int index) {
        ensureLoaded();
        checkIndex(index);
        return keyCodes[index];
    }

    /**
     * CopyL keybinds are exclusive: assigning a key to one slot automatically
     * clears that same key from any other CopyL slot.
     */
    public synchronized void setKeyCode(int index, int keyCode) {
        ensureLoaded();
        checkIndex(index);
        if (keyCode >= 0) {
            for (int i = 0; i < keyCodes.length; i++) {
                if (i != index && keyCodes[i] == keyCode) keyCodes[i] = -1;
            }
        }
        keyCodes[index] = keyCode;
    }

    public synchronized void save() {
        ensureLoaded();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(new ConfigData(names.clone(), messages.clone(), keyCodes.clone()), writer);
            }
        } catch (Exception exception) {
            System.err.println("[Lclient/CopyL] No se pudo guardar " + CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null && data.names != null) {
                for (int i = 0; i < names.length && i < data.names.length; i++) names[i] = normalizeName(data.names[i], i);
            }
            if (data != null && data.messages != null) {
                for (int i = 0; i < messages.length && i < data.messages.length; i++) messages[i] = normalizeMessage(data.messages[i]);
            }
            if (data != null && data.keyCodes != null) {
                for (int i = 0; i < keyCodes.length && i < data.keyCodes.length; i++) {
                    int key = data.keyCodes[i];
                    if (key < 0) {
                        keyCodes[i] = -1;
                    } else {
                        boolean duplicate = false;
                        for (int j = 0; j < i; j++) {
                            if (keyCodes[j] == key) {
                                duplicate = true;
                                break;
                            }
                        }
                        keyCodes[i] = duplicate ? -1 : key;
                    }
                }
            }
        } catch (Exception exception) {
            System.err.println("[Lclient/CopyL] No se pudo leer " + CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private static String normalizeMessage(String value) {
        if (value == null) return "";
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }

    private static String normalizeName(String value, int index) {
        if (value == null || value.trim().isEmpty()) return defaultName(index);
        String trimmed = value.trim();
        return trimmed.length() <= MAX_NAME_LENGTH ? trimmed : trimmed.substring(0, MAX_NAME_LENGTH);
    }

    private static String defaultName(int index) {
        return "Mensaje " + (index + 1);
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= CopyLKeyMappings.SLOT_COUNT) throw new IndexOutOfBoundsException("CopyL slot " + index);
    }

    private static final class ConfigData {
        private final String[] names;
        private final String[] messages;
        private final int[] keyCodes;

        private ConfigData(String[] names, String[] messages, int[] keyCodes) {
            this.names = names;
            this.messages = messages;
            this.keyCodes = keyCodes;
        }
    }
}
