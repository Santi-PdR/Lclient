package com.santipdr.copyl.client;

import com.santipdr.copyl.client.screen.LClientSettingsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class CopyLClientSetup {
    private CopyLClientSetup() {
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new LClientSettingsScreen(parent)
                )
        );
    }
}
