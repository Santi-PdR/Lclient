package com.santipdr.copyl.client.integration;

import com.santipdr.copyl.CopyL;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@JourneyMapPlugin(apiVersion = "2.0.0")
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

    public static boolean isReady() {
        return api != null;
    }

    public static void markAttacker(BlockPos pos, String name, ResourceKey<Level> dimension) {
        if (api == null) return;
        try {
            if (lastAttacker != null) api.removeWaypoint(CopyL.MOD_ID, lastAttacker);
            lastAttacker = WaypointFactory.createWaypoint(CopyL.MOD_ID, pos, "Último atacante: " + name, dimension, false);
            lastAttacker.setColor(0xE34B4B);
            api.addWaypoint(CopyL.MOD_ID, lastAttacker);
        } catch (Throwable ignored) {
        }
    }

    public static void markRecon(BlockPos pos, ResourceKey<Level> dimension) {
        if (api == null) return;
        try {
            if (lastRecon != null) api.removeWaypoint(CopyL.MOD_ID, lastRecon);
            lastRecon = WaypointFactory.createWaypoint(CopyL.MOD_ID, pos, "Recon Lclient", dimension, false);
            lastRecon.setColor(0x5AAFFF);
            api.addWaypoint(CopyL.MOD_ID, lastRecon);
        } catch (Throwable ignored) {
        }
    }
}
