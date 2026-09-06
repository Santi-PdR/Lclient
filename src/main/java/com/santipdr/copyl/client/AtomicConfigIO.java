package com.santipdr.copyl.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Small helper for crash-resistant UTF-8 config persistence. */
public final class AtomicConfigIO {
    private AtomicConfigIO() {
    }

    public static void write(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        try {
            Files.move(
                    temp,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static Path backupBroken(Path target) {
        if (!Files.exists(target)) return null;
        String baseName = target.getFileName().toString();
        Path backup = target.resolveSibling(baseName + ".broken-" + System.currentTimeMillis());
        try {
            return Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            return null;
        }
    }
}
