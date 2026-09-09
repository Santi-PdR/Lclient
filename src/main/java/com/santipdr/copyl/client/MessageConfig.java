package com.santipdr.copyl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
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
        resetDefaults();
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

    /** Assigning one binding to a slot clears it from every other slot. */
    public synchronized void setKeyCode(int index, int keyCode) {
        ensureLoaded();
        checkIndex(index);
        keyCode = CopyLBinding.sanitize(keyCode);
        if (keyCode >= 0) {
            for (int i = 0; i < keyCodes.length; i++) {
                if (i != index && keyCodes[i] == keyCode) keyCodes[i] = CopyLBinding.UNBOUND;
            }
        }
        keyCodes[index] = keyCode;
    }

    public synchronized String[] copyNames() {
        ensureLoaded();
        return names.clone();
    }

    public synchronized String[] copyMessages() {
        ensureLoaded();
        return messages.clone();
    }

    public synchronized int[] copyKeyCodes() {
        ensureLoaded();
        return keyCodes.clone();
    }

    /**
     * Applies the whole editor state transactionally: disk is written first and the live
     * configuration only changes after that write succeeds.
     */
    public synchronized boolean replaceAll(String[] newNames, String[] newMessages, int[] newKeys) {
        ensureLoaded();
        if (newNames == null || newMessages == null || newKeys == null
                || newNames.length != names.length
                || newMessages.length != messages.length
                || newKeys.length != keyCodes.length) {
            return false;
        }

        String[] normalizedNames = new String[names.length];
        String[] normalizedMessages = new String[messages.length];
        int[] normalizedKeys = new int[keyCodes.length];

        for (int i = 0; i < names.length; i++) {
            normalizedNames[i] = normalizeName(newNames[i], i);
            normalizedMessages[i] = normalizeMessage(newMessages[i]);

            int binding = CopyLBinding.sanitize(newKeys[i]);
            if (binding >= 0) {
                for (int j = 0; j < i; j++) {
                    if (normalizedKeys[j] == binding) {
                        normalizedKeys[j] = CopyLBinding.UNBOUND;
                        break;
                    }
                }
            }
            normalizedKeys[i] = binding;
        }

        int openBinding = CopyLConfig.get().openKey;
        if (openBinding >= 0) {
            for (int i = 0; i < normalizedKeys.length; i++) {
                if (normalizedKeys[i] == openBinding) normalizedKeys[i] = CopyLBinding.UNBOUND;
            }
        }

        ConfigData next = new ConfigData(normalizedNames, normalizedMessages, normalizedKeys);
        if (!writeData(next)) return false;

        System.arraycopy(normalizedNames, 0, names, 0, names.length);
        System.arraycopy(normalizedMessages, 0, messages, 0, messages.length);
        System.arraycopy(normalizedKeys, 0, keyCodes, 0, keyCodes.length);
        return true;
    }

    /** Clears a binding from any slot without mutating live state when persistence fails. */
    public synchronized boolean clearBindingAndSave(int binding) {
        ensureLoaded();
        if (binding < 0) return true;

        int[] nextKeys = keyCodes.clone();
        boolean changed = false;
        for (int i = 0; i < nextKeys.length; i++) {
            if (nextKeys[i] == binding) {
                nextKeys[i] = CopyLBinding.UNBOUND;
                changed = true;
            }
        }
        return !changed || replaceAll(names.clone(), messages.clone(), nextKeys);
    }

    public synchronized boolean save() {
        ensureLoaded();
        return writeCurrentState();
    }

    private boolean writeCurrentState() {
        return writeData(new ConfigData(names.clone(), messages.clone(), keyCodes.clone()));
    }

    private boolean writeData(ConfigData data) {
        try {
            AtomicConfigIO.write(CONFIG_PATH, GSON.toJson(data));
            return true;
        } catch (Exception exception) {
            System.err.println("[CopyL] No se pudo guardar " + CONFIG_PATH + ": " + exception.getMessage());
            return false;
        }
    }

    private synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(CONFIG_PATH)) {
            writeCurrentState();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null) throw new IllegalStateException("config vacía");

            boolean repaired = false;

            if (data.names == null || data.names.length != names.length) repaired = true;
            for (int i = 0; i < names.length; i++) {
                String raw = data.names != null && i < data.names.length ? data.names[i] : defaultName(i);
                String normalized = normalizeName(raw, i);
                names[i] = normalized;
                if (!safeEquals(raw, normalized)) repaired = true;
            }

            if (data.messages == null || data.messages.length != messages.length) repaired = true;
            for (int i = 0; i < messages.length; i++) {
                String raw = data.messages != null && i < data.messages.length ? data.messages[i] : "";
                String normalized = normalizeMessage(raw);
                messages[i] = normalized;
                if (!safeEquals(raw, normalized)) repaired = true;
            }

            if (data.keyCodes == null || data.keyCodes.length != keyCodes.length) repaired = true;
            for (int i = 0; i < keyCodes.length; i++) {
                int raw = data.keyCodes != null && i < data.keyCodes.length ? data.keyCodes[i] : CopyLBinding.UNBOUND;
                int binding = CopyLBinding.sanitize(raw);
                if (binding != raw) repaired = true;

                if (binding >= 0) {
                    for (int j = 0; j < i; j++) {
                        if (keyCodes[j] == binding) {
                            binding = CopyLBinding.UNBOUND;
                            repaired = true;
                            break;
                        }
                    }
                }
                keyCodes[i] = binding;
            }

            if (clearReservedConflict(CopyLConfig.get().openKey)) repaired = true;
            if (repaired) writeCurrentState();
        } catch (Exception exception) {
            Path backup = AtomicConfigIO.backupBroken(CONFIG_PATH);
            System.err.println("[CopyL] No se pudo leer " + CONFIG_PATH + ": " + exception.getMessage()
                    + (backup == null ? "" : " · copia: " + backup));
            resetDefaults();
            writeCurrentState();
        }
    }

    private boolean clearReservedConflict(int openKey) {
        if (openKey < 0) return false;
        boolean changed = false;
        for (int i = 0; i < keyCodes.length; i++) {
            if (keyCodes[i] == openKey) {
                keyCodes[i] = CopyLBinding.UNBOUND;
                changed = true;
            }
        }
        return changed;
    }

    private void resetDefaults() {
        for (int i = 0; i < names.length; i++) names[i] = defaultName(i);
        Arrays.fill(messages, "");
        Arrays.fill(keyCodes, CopyLBinding.UNBOUND);
    }

    private static String normalizeMessage(String value) {
        return truncateUtf16Safely(cleanSingleLine(value), MAX_MESSAGE_LENGTH);
    }

    private static String normalizeName(String value, int index) {
        String cleaned = cleanSingleLine(value).trim();
        if (cleaned.isEmpty()) return defaultName(index);
        return truncateUtf16Safely(cleaned, MAX_NAME_LENGTH);
    }

    private static String cleanSingleLine(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || Character.isISOControl(c)) {
                out.append(' ');
                continue;
            }
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
                    out.append(c).append(value.charAt(++i));
                } else {
                    out.append('?');
                }
                continue;
            }
            if (Character.isLowSurrogate(c)) {
                out.append('?');
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String truncateUtf16Safely(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        int end = maxChars;
        if (end > 0
                && end < value.length()
                && Character.isHighSurrogate(value.charAt(end - 1))
                && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
