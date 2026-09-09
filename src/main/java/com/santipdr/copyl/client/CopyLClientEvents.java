package com.santipdr.copyl.client;

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

    private static final boolean[] messageBindingDown = new boolean[CopyLKeyMappings.SLOT_COUNT];
    private static final int[] observedMessageBindings = new int[CopyLKeyMappings.SLOT_COUNT];
    private static boolean openBindingDown;
    private static int observedOpenBinding = Integer.MIN_VALUE;

    static {
        Arrays.fill(observedMessageBindings, Integer.MIN_VALUE);
    }

    private CopyLClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        CopyLConfig config = CopyLConfig.get();
        pollOpenBinding(minecraft, config);
        pollQuickMessages(minecraft, config);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetTransientBindings();
    }

    private static void pollOpenBinding(Minecraft minecraft, CopyLConfig config) {
        int binding = config.openKey;
        boolean down = CopyLBinding.isDown(minecraft, binding);
        if (observedOpenBinding != binding) {
            observedOpenBinding = binding;
            openBindingDown = down;
            return;
        }

        if (down && !openBindingDown && minecraft.screen == null) {
            minecraft.setScreen(new MessageEditorScreen(null));
        }
        openBindingDown = down;
    }

    private static void pollQuickMessages(Minecraft minecraft, CopyLConfig config) {
        MessageConfig messages = MessageConfig.getInstance();
        boolean canSend = minecraft.player != null
                && minecraft.player.connection != null
                && minecraft.screen == null;

        for (int i = 0; i < messageBindingDown.length; i++) {
            int binding = messages.getKeyCode(i);
            boolean reserved = binding >= 0 && binding == config.openKey;
            boolean down = binding >= 0 && !reserved && CopyLBinding.isDown(minecraft, binding);

            if (observedMessageBindings[i] != binding) {
                observedMessageBindings[i] = binding;
                messageBindingDown[i] = down;
                continue;
            }

            // Physical state is tracked even while another screen is open. That way closing
            // inventory/chat/config while holding a CopyL binding never creates a fake new press.
            if (canSend && down && !messageBindingDown[i]) sendSlot(minecraft, i);
            messageBindingDown[i] = down;
        }
    }

    private static void sendSlot(Minecraft minecraft, int slot) {
        if (minecraft.player == null || minecraft.player.connection == null) return;

        String message = MessageConfig.getInstance().getMessage(slot);
        if (message == null || message.isBlank()) return;

        // CopyL sends exactly what the user configured. No placeholders or hidden transforms.
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

    private static void resetTransientBindings() {
        openBindingDown = false;
        observedOpenBinding = Integer.MIN_VALUE;
        Arrays.fill(messageBindingDown, false);
        Arrays.fill(observedMessageBindings, Integer.MIN_VALUE);
    }
}
