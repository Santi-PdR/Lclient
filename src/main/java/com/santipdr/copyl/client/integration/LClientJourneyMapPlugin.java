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

/**
 * JourneyMap 5.10.x plugin.
 *
 * IMPORTANT: nothing outside this package should directly reference this class.
 * JourneyMap 5.10.3 bundles API 1.20-1.9-SNAPSHOT, and loading this class when
 * JourneyMap is absent would otherwise cause a classloader failure. Use
 * JourneyMapBridge from the rest of Lclient.
 */
@journeymap.client.api.ClientPlugin
public final class LClientJourneyMapPlugin implements IClientPlugin {
    private static IClientAPI api;
    private static Waypoint lastAttacker;
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
        // No subscriptions are required for Lclient's tactical waypoints.
    }

    public static boolean isReady() {
        if (api == null) return false;
        try {
            return api.playerAccepts(CopyL.MOD_ID, DisplayType.Waypoint);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void markAttacker(BlockPos pos, String name, ResourceKey<Level> dimension) {
        if (!isReady()) return;
        try {
            if (lastAttacker != null) api.remove(lastAttacker);
            lastAttacker = new Waypoint(
                    CopyL.MOD_ID,
                    "lclient_last_attacker",
                    "Último atacante: " + name,
                    dimension,
                    pos
            ).setColor(0xE34B4B).setPersistent(false).setEditable(false);
            api.show(lastAttacker);
        } catch (Throwable ignored) {
            lastAttacker = null;
        }
    }

    public static void markRecon(BlockPos pos, ResourceKey<Level> dimension) {
        if (!isReady()) return;
        try {
            if (lastRecon != null) api.remove(lastRecon);
            lastRecon = new Waypoint(
                    CopyL.MOD_ID,
                    "lclient_recon",
                    "Recon Lclient",
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
            if (lastAttacker != null) api.remove(lastAttacker);
            if (lastRecon != null) api.remove(lastRecon);
        } catch (Throwable ignored) {
        } finally {
            lastAttacker = null;
            lastRecon = null;
        }
    }
}
