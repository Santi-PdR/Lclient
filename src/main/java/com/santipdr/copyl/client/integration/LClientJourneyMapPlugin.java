package com.santipdr.copyl.client.integration;

import com.santipdr.copyl.CopyL;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.DisplayType;
import journeymap.client.api.display.Waypoint;
import journeymap.client.api.event.ClientEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** JourneyMap 5.10.x / API 1.9 plugin used only for Recon waypoints. */
@journeymap.client.api.ClientPlugin
public final class LClientJourneyMapPlugin implements IClientPlugin {
    private static IClientAPI api;
    private static Waypoint lastRecon;

    @Override
    public void initialize(IClientAPI clientAPI) {
        api = clientAPI;
    }

    @Override
    public String getModId() {
        return CopyL.MOD_ID;
    }

    @Override
    public void onEvent(ClientEvent event) {
    }

    public static boolean isReady() {
        if (api == null) return false;
        try {
            return api.playerAccepts(CopyL.MOD_ID, DisplayType.Waypoint);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void markRecon(BlockPos pos, String label, ResourceKey<Level> dimension) {
        if (!isReady()) return;
        try {
            if (lastRecon != null) api.remove(lastRecon);
            String safeLabel = label == null || label.isBlank() ? "Recon" : label.trim();
            if (safeLabel.length() > 42) safeLabel = safeLabel.substring(0, 42);
            lastRecon = new Waypoint(
                    CopyL.MOD_ID,
                    "lclient_recon",
                    "Recon · " + safeLabel,
                    dimension,
                    pos
            ).setColor(0x5AAFFF).setPersistent(false).setEditable(false);
            api.show(lastRecon);
        } catch (Throwable ignored) {
            lastRecon = null;
        }
    }

    public static void clearTacticalWaypoints() {
        if (api == null) return;
        try {
            if (lastRecon != null) api.remove(lastRecon);
        } catch (Throwable ignored) {
        } finally {
            lastRecon = null;
        }
    }
}
