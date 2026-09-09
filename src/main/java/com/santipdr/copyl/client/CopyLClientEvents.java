package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.screen.MessageEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

/** Complete runtime surface of the CopyL-only client. */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class CopyLClientEvents {
    private static final int MAX_OUTGOING_MESSAGE_LENGTH = 256;

    private static final boolean[] messageKeyDown = new boolean[CopyLKeyMappings.SLOT_COUNT];
    private static final int[] observedMessageKeys = new int[CopyLKeyMappings.SLOT_COUNT];
    private static boolean openKeyDown;
    private static int observedOpenKey = Integer.MIN_VALUE;

    static {
        Arrays.fill(observedMessageKeys, Integer.MIN_VALUE);
    }

    private CopyLClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        CopyLConfig config = CopyLConfig.get();
        pollOpenKey(minecraft, config);
        pollQuickMessages(minecraft, config);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetTransientKeys();
    }

    private static void pollOpenKey(Minecraft minecraft, CopyLConfig config) {
        int key = config.openKey;
        boolean down = keyDown(minecraft, key);
        if (observedOpenKey != key) {
            observedOpenKey = key;
            openKeyDown = down;
            return;
        }

        if (down && !openKeyDown && minecraft.screen == null) {
            minecraft.setScreen(new MessageEditorScreen(null));
        }
        openKeyDown = down;
    }

    private static void pollQuickMessages(Minecraft minecraft, CopyLConfig config) {
        MessageConfig messages = MessageConfig.getInstance();
        boolean canSend = minecraft.player != null
                && minecraft.player.connection != null
                && minecraft.screen == null;

        for (int i = 0; i < messageKeyDown.length; i++) {
            int key = messages.getKeyCode(i);
            boolean reserved = key >= 0 && key == config.openKey;
            boolean down = key >= 0 && !reserved && keyDown(minecraft, key);

            if (observedMessageKeys[i] != key) {
                observedMessageKeys[i] = key;
                messageKeyDown[i] = down;
                continue;
            }

            if (canSend && down && !messageKeyDown[i]) sendSlot(minecraft, i);
            messageKeyDown[i] = down;
        }
    }

    private static void sendSlot(Minecraft minecraft, int slot) {
        if (minecraft.player == null || minecraft.player.connection == null) return;

        String message = MessageConfig.getInstance().getMessage(slot);
        if (message == null || message.isBlank()) return;

        // CopyL 3.1 sends exactly what the user configured. No placeholders,
        // target inspection or hidden transformation is performed here.
        message = truncateUtf16Safely(message, MAX_OUTGOING_MESSAGE_LENGTH);
        if (message.isBlank()) return;

        if (message.startsWith("/") && message.length() > 1) {
            minecraft.player.connection.sendCommand(message.substring(1));
        } else {
            minecraft.player.connection.sendChat(message);
        }
    }

    private static String truncateUtf16Safely(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) return value == null ? "" : value;
        int end = maxChars;
        if (end > 0
                && end < value.length()
                && Character.isHighSurrogate(value.charAt(end - 1))
                && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static void resetTransientKeys() {
        openKeyDown = false;
        observedOpenKey = Integer.MIN_VALUE;
        Arrays.fill(messageKeyDown, false);
        Arrays.fill(observedMessageKeys, Integer.MIN_VALUE);
    }

    private static boolean keyDown(Minecraft minecraft, int keyCode) {
        return keyCode >= 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), keyCode);
    }
}
