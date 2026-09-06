package com.santipdr.copyl.client;

import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.screen.MessageEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class CopyLClientEvents {
    private CopyLClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean canOpenEditor = minecraft.player != null && minecraft.level != null && minecraft.screen == null;

        while (CopyLKeyMappings.OPEN_EDITOR.consumeClick()) {
            if (canOpenEditor) {
                minecraft.setScreen(new MessageEditorScreen(null));
            }
        }

        boolean canSend = minecraft.player != null
                && minecraft.level != null
                && minecraft.player.connection != null
                && minecraft.screen == null;

        for (int i = 0; i < CopyLKeyMappings.SLOTS.length; i++) {
            while (CopyLKeyMappings.SLOTS[i].consumeClick()) {
                if (canSend) {
                    sendSlot(minecraft, i);
                }
            }
        }
    }

    private static void sendSlot(Minecraft minecraft, int slot) {
        String message = MessageConfig.getInstance().getMessage(slot);
        if (message.isBlank()) {
            return;
        }

        if (message.startsWith("/") && message.length() > 1) {
            minecraft.player.connection.sendCommand(message.substring(1));
        } else {
            minecraft.player.connection.sendChat(message);
        }
    }
}
