package com.santipdr.copyl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Portable clipboard backup for CopyL's global binding and 10 quick-message slots. */
public final class CopyLProfileCodec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FORMAT = "copyl-profile";
    private static final int SCHEMA = 1;
    private static final int MAX_PROFILE_CHARS = 16384;

    private CopyLProfileCodec() {
    }

    public enum ImportResult {
        SUCCESS,
        INVALID,
        SAVE_FAILED
    }

    public static String exportProfile() {
        MessageConfig messages = MessageConfig.getInstance();
        ProfileData data = new ProfileData();
        data.format = FORMAT;
        data.schema = SCHEMA;
        data.openBinding = CopyLConfig.get().openKey;
        data.names = messages.copyNames();
        data.messages = messages.copyMessages();
        data.bindings = messages.copyKeyCodes();
        return GSON.toJson(data);
    }

    public static ImportResult importProfile(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > MAX_PROFILE_CHARS) {
            return ImportResult.INVALID;
        }

        final ProfileData data;
        try {
            data = GSON.fromJson(raw, ProfileData.class);
        } catch (Exception exception) {
            return ImportResult.INVALID;
        }

        if (data == null
                || !FORMAT.equals(data.format)
                || data.schema != SCHEMA
                || data.names == null
                || data.messages == null
                || data.bindings == null
                || data.names.length != CopyLKeyMappings.SLOT_COUNT
                || data.messages.length != CopyLKeyMappings.SLOT_COUNT
                || data.bindings.length != CopyLKeyMappings.SLOT_COUNT) {
            return ImportResult.INVALID;
        }

        int importedOpen = CopyLBinding.sanitize(data.openBinding);
        if (data.openBinding != CopyLBinding.UNBOUND && importedOpen == CopyLBinding.UNBOUND) {
            return ImportResult.INVALID;
        }

        int[] importedBindings = data.bindings.clone();
        for (int i = 0; i < importedBindings.length; i++) {
            int sanitized = CopyLBinding.sanitize(importedBindings[i]);
            if (importedBindings[i] != CopyLBinding.UNBOUND && sanitized == CopyLBinding.UNBOUND) {
                return ImportResult.INVALID;
            }
            importedBindings[i] = sanitized;
            if (importedBindings[i] == importedOpen) importedBindings[i] = CopyLBinding.UNBOUND;
        }

        MessageConfig messageConfig = MessageConfig.getInstance();
        CopyLConfig generalConfig = CopyLConfig.get();

        String[] oldNames = messageConfig.copyNames();
        String[] oldMessages = messageConfig.copyMessages();
        int[] oldBindings = messageConfig.copyKeyCodes();
        int oldOpen = generalConfig.openKey;

        // replaceAll() also resolves duplicate slot bindings. Temporarily expose the imported
        // global binding so it can remove any conflicting slot binding before persistence.
        generalConfig.openKey = importedOpen;
        if (!messageConfig.replaceAll(data.names, data.messages, importedBindings)) {
            generalConfig.openKey = oldOpen;
            return ImportResult.SAVE_FAILED;
        }

        if (!generalConfig.save()) {
            generalConfig.openKey = oldOpen;
            messageConfig.replaceAll(oldNames, oldMessages, oldBindings);
            return ImportResult.SAVE_FAILED;
        }

        return ImportResult.SUCCESS;
    }

    private static final class ProfileData {
        private String format;
        private int schema;
        private int openBinding;
        private String[] names;
        private String[] messages;
        private int[] bindings;
    }
}
