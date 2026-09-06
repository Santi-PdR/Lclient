package com.santipdr.copyl.client.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reflection-only boundary around JourneyMap.
 *
 * Keeping JourneyMap API types out of the rest of Lclient means the client can
 * still start normally when JourneyMap is missing or when its plugin has not
 * finished initializing yet.
 */
public final class JourneyMapBridge {
    private static final String PLUGIN_CLASS = "com.santipdr.copyl.client.integration.LClientJourneyMapPlugin";

    private JourneyMapBridge() {
    }

    public static boolean isInstalled() {
        try {
            return ModList.get().isLoaded("journeymap");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isReady() {
        Object result = invoke("isReady", new Class<?>[0]);
        return result instanceof Boolean value && value;
    }

    public static void markAttacker(BlockPos pos, String name, ResourceKey<Level> dimension) {
        invoke(
                "markAttacker",
                new Class<?>[]{BlockPos.class, String.class, ResourceKey.class},
                pos,
                name,
                dimension
        );
    }

    public static void markRecon(BlockPos pos, ResourceKey<Level> dimension) {
        invoke(
                "markRecon",
                new Class<?>[]{BlockPos.class, ResourceKey.class},
                pos,
                dimension
        );
    }

    public static void clearTacticalWaypoints() {
        invoke("clearTacticalWaypoints", new Class<?>[0]);
    }

    private static Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (!isInstalled()) return null;
        try {
            Class<?> plugin = Class.forName(PLUGIN_CLASS, true, JourneyMapBridge.class.getClassLoader());
            Method method = plugin.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
