package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CopyLKeyMappings {
    public static final int SLOT_COUNT = 10;
    public static final String CATEGORY = "key.categories.copyl";

    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.copyl.open_editor",
            GLFW.GLFW_KEY_MINUS,
            CATEGORY
    );

    public static final KeyMapping[] SLOTS = new KeyMapping[SLOT_COUNT];

    static {
        for (int i = 0; i < SLOT_COUNT; i++) {
            SLOTS[i] = new KeyMapping(
                    "key.copyl.slot." + (i + 1),
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY
            );
        }
    }

    private CopyLKeyMappings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
        for (KeyMapping slot : SLOTS) {
            event.register(slot);
        }
    }
}
