package com.santipdr.copyl.client;

import com.santipdr.copyl.client.screen.CopyLSettingsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class CopyLClientSetup {
    private CopyLClientSetup() {
    }

    /** Forge 47.x still exposes the config-screen extension through ModLoadingContext#get(). */
    @SuppressWarnings("removal")
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new CopyLSettingsScreen(parent)
                )
        );
    }
}
