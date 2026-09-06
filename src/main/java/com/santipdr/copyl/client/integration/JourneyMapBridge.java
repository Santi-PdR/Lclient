package com.santipdr.copyl.client.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reflection-only boundary around optional JourneyMap integration.
 *
 * No JourneyMap API type escapes this class, so the rest of Lclient stays
 * loadable when JourneyMap is absent or its plugin has not initialized yet.
 */
public final class JourneyMapBridge {
    private static final String PLUGIN_CLASS = "com.santipdr.copyl.client.integration.LClientJourneyMapPlugin";
    private static final long RETRY_DELAY_MS = 2000L;

    private static Method readyMethod;
    private static Method markMethod;
    private static Method clearMethod;
    private static Method statusMethod;
    private static boolean bound;
    private static long nextBindAttempt;
    private static String bridgeError = "";

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
        Object result = invoke(readyMethod, "isReady");
        return result instanceof Boolean value && value;
    }

    public static boolean markRecon(BlockPos pos, String label, ResourceKey<Level> dimension) {
        Object result = invoke(markMethod, "markRecon", pos, label, dimension);
        return result instanceof Boolean value && value;
    }

    public static boolean clearTacticalWaypoints() {
        Object result = invoke(clearMethod, "clearTacticalWaypoints");
        return result instanceof Boolean value && value;
    }

    public static String getStatusText() {
        if (!isInstalled()) return "JourneyMap no instalado";
        if (!ensureBound()) return bridgeError.isBlank() ? "JourneyMap+ no disponible" : bridgeError;
        try {
            Object result = statusMethod.invoke(null);
            return result instanceof String text && !text.isBlank() ? text : "JourneyMap+ sin estado";
        } catch (Throwable throwable) {
            rememberBridgeError("No se pudo leer el estado de JourneyMap+", throwable);
            return bridgeError;
        }
    }

    private static Object invoke(Method method, String methodName, Object... args) {
        if (!isInstalled() || !ensureBound()) return null;
        try {
            Method target = switch (methodName) {
                case "isReady" -> readyMethod;
                case "markRecon" -> markMethod;
                case "clearTacticalWaypoints" -> clearMethod;
                default -> method;
            };
            if (target == null) return null;
            Object result = target.invoke(null, args);
            bridgeError = "";
            return result;
        } catch (Throwable throwable) {
            rememberBridgeError("JourneyMap+ falló en " + methodName, throwable);
            return null;
        }
    }

    private static synchronized boolean ensureBound() {
        if (bound) return true;
        if (!isInstalled()) return false;

        long now = System.currentTimeMillis();
        if (now < nextBindAttempt) return false;
        nextBindAttempt = now + RETRY_DELAY_MS;

        try {
            Class<?> plugin = Class.forName(PLUGIN_CLASS, true, JourneyMapBridge.class.getClassLoader());
            readyMethod = plugin.getMethod("isReady");
            markMethod = plugin.getMethod("markRecon", BlockPos.class, String.class, ResourceKey.class);
            clearMethod = plugin.getMethod("clearTacticalWaypoints");
            statusMethod = plugin.getMethod("getStatusText");
            bound = true;
            bridgeError = "";
            return true;
        } catch (Throwable throwable) {
            rememberBridgeError("No se pudo enlazar JourneyMap+", throwable);
            return false;
        }
    }

    private static void rememberBridgeError(String context, Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null && cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String detail = cause == null || cause.getMessage() == null ? "" : cause.getMessage().trim();
        bridgeError = detail.isBlank() ? context : context + ": " + detail;
        if (bridgeError.length() > 120) bridgeError = bridgeError.substring(0, 120);
    }
}
