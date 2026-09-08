package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.integration.JourneyMapBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tactical Recon controller with render-only zoom and long-range client raycast. */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class ReconController {
    private static final long ZOOM_SAVE_DEBOUNCE_MS = 450L;
    private static final long TARGET_CACHE_NS = 33_000_000L;
    private static final long TARGET_MEMORY_MS = 650L;
    private static final double TARGET_EYE_EPSILON_SQ = 0.0025D;
    private static final double TARGET_LOOK_DOT_MIN = 0.99995D;

    private static boolean zoomActive;
    private static boolean waypointKeyDown;
    private static ClientLevel trackedLevel;
    private static double smoothedFov = -1.0D;
    private static double scrollAccumulator;
    private static String statusText = "";
    private static long statusUntil;
    private static boolean zoomConfigDirty;
    private static long zoomSaveAt;

    private static ClientLevel targetCacheLevel;
    private static HitResult targetCacheHit;
    private static Vec3 targetCacheEye;
    private static Vec3 targetCacheLook;
    private static int targetCacheRange = -1;
    private static long targetCacheUntilNs;

    private static ClientLevel trackedEntityLevel;
    private static Entity trackedEntity;
    private static long trackedEntityUntilMs;

    private ReconController() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();
        flushZoomConfigIfDue(config, false);

        if (minecraft.player == null || minecraft.level == null) {
            flushZoomConfigIfDue(config, true);
            resetRuntime();
            return;
        }

        if (trackedLevel != minecraft.level) {
            flushZoomConfigIfDue(config, true);
            resetRuntime();
            trackedLevel = minecraft.level;
        }

        boolean wasZoomActive = zoomActive;
        boolean canUseRecon = config.recon && minecraft.screen == null;
        zoomActive = canUseRecon && keyDown(minecraft, config.reconZoomKey);
        if (!zoomActive) {
            smoothedFov = -1.0D;
            scrollAccumulator = 0.0D;
            invalidateTargetCache();
            clearTrackedEntity();
        }
        if (wasZoomActive && !zoomActive) {
            flushZoomConfigIfDue(config, true);
        }

        boolean physicalWaypointDown = keyDown(minecraft, config.reconWaypointKey);
        if (canUseRecon && zoomActive && physicalWaypointDown && !waypointKeyDown) {
            createWaypoint(minecraft, config);
        }
        waypointKeyDown = physicalWaypointDown;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        flushZoomConfigIfDue(LClientConfig.get(), true);
        resetRuntime();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        double delta = event.getScrollDelta();
        if (!zoomActive || delta == 0.0D) return;

        event.setCanceled(true);
        scrollAccumulator += delta;
        int steps = (int) scrollAccumulator;
        if (steps == 0) return;
        scrollAccumulator -= steps;

        LClientConfig config = LClientConfig.get();
        int next = Mth.clamp(config.reconZoomFov - steps * 2, 8, 50);
        if (next != config.reconZoomFov) {
            config.reconZoomFov = next;
            zoomConfigDirty = true;
            zoomSaveAt = System.currentTimeMillis() + ZOOM_SAVE_DEBOUNCE_MS;
            setStatus("Zoom " + zoomText(Minecraft.getInstance(), next));
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!zoomActive) {
            smoothedFov = -1.0D;
            return;
        }

        double unzoomedFov = event.getFOV();
        double target = Math.min(LClientConfig.get().reconZoomFov, unzoomedFov);
        if (smoothedFov < 0.0D) smoothedFov = unzoomedFov;
        smoothedFov += (target - smoothedFov) * 0.38D;
        event.setFOV(Math.min(smoothedFov, unzoomedFov));
    }

    public static boolean isZoomActive() {
        return zoomActive;
    }

    public static int getZoomFov() {
        Minecraft minecraft = Minecraft.getInstance();
        int configured = LClientConfig.get().reconZoomFov;
        return Math.min(configured, minecraft.options.fov().get());
    }

    public static String getZoomText() {
        return zoomText(Minecraft.getInstance(), LClientConfig.get().reconZoomFov);
    }

    private static String zoomText(Minecraft minecraft, int targetFov) {
        int baseFov = minecraft.options.fov().get();
        int effectiveTarget = Math.min(baseFov, Math.max(1, targetFov));
        double multiplier = Math.max(1.0D, (double) baseFov / Math.max(1, effectiveTarget));
        return String.format(java.util.Locale.ROOT, "x%.1f", multiplier);
    }

    public static String getStatusText() {
        return System.currentTimeMillis() <= statusUntil ? statusText : "";
    }

    public static HitResult getTargetHit() {
        return getTargetHit(Minecraft.getInstance());
    }

    public static HitResult getTargetHit(Minecraft minecraft) {
        return getTargetHit(minecraft, false);
    }

    private static HitResult getTargetHit(Minecraft minecraft, boolean forceFresh) {
        if (minecraft.player == null || minecraft.level == null) {
            invalidateTargetCache();
            clearTrackedEntity();
            return null;
        }

        int range = LClientConfig.get().reconRange;
        Vec3 eye = minecraft.player.getEyePosition(1.0F);
        Vec3 look = minecraft.player.getViewVector(1.0F);
        long nowNs = System.nanoTime();

        if (!forceFresh && canReuseTarget(minecraft.level, eye, look, range, nowNs)) {
            rememberEntityFromHit(minecraft.level, targetCacheHit);
            return targetCacheHit;
        }

        Vec3 end = eye.add(look.scale(range));
        BlockHitResult blockHit;
        try {
            blockHit = minecraft.level.clip(new ClipContext(
                    eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player
            ));
        } catch (RuntimeException | LinkageError ignored) {
            blockHit = BlockHitResult.miss(
                    end,
                    Direction.getNearest((float) look.x, (float) look.y, (float) look.z),
                    BlockPos.containing(end)
            );
        }

        double blockDistanceSq = blockHit.getType() == HitResult.Type.MISS
                ? (double) range * range
                : eye.distanceToSqr(blockHit.getLocation());

        AABB searchBox = minecraft.player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult entityHit = null;
        try {
            entityHit = ProjectileUtil.getEntityHitResult(
                    minecraft.player,
                    eye,
                    end,
                    searchBox,
                    entity -> safePickableEntity(entity, minecraft.player),
                    blockDistanceSq
            );
        } catch (RuntimeException | LinkageError ignored) {
            // A broken third-party entity must not take the entire Recon HUD down.
        }

        HitResult result = entityHit != null ? entityHit : blockHit;
        targetCacheLevel = minecraft.level;
        targetCacheHit = result;
        targetCacheEye = eye;
        targetCacheLook = look;
        targetCacheRange = range;
        targetCacheUntilNs = nowNs + TARGET_CACHE_NS;
        rememberEntityFromHit(minecraft.level, result);
        return result;
    }

    private static boolean safePickableEntity(Entity entity, Entity player) {
        if (entity == null || entity == player) return false;
        try {
            return !entity.isRemoved() && !entity.isSpectator() && entity.isPickable();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean canReuseTarget(ClientLevel level, Vec3 eye, Vec3 look, int range, long nowNs) {
        if (targetCacheLevel != level
                || targetCacheHit == null
                || targetCacheEye == null
                || targetCacheLook == null
                || targetCacheRange != range
                || nowNs >= targetCacheUntilNs) {
            return false;
        }
        if (targetCacheHit instanceof EntityHitResult entityHit && entityHit.getEntity().isRemoved()) return false;
        if (targetCacheEye.distanceToSqr(eye) > TARGET_EYE_EPSILON_SQ) return false;
        return targetCacheLook.dot(look) >= TARGET_LOOK_DOT_MIN;
    }

    private static void rememberEntityFromHit(ClientLevel level, HitResult hit) {
        if (!(hit instanceof EntityHitResult entityHit)) return;
        Entity entity = entityHit.getEntity();
        if (entity == null || entity.isRemoved()) return;
        trackedEntityLevel = level;
        trackedEntity = entity;
        trackedEntityUntilMs = System.currentTimeMillis() + TARGET_MEMORY_MS;
    }

    public static Entity getTrackedEntity(Minecraft minecraft) {
        if (!zoomActive || minecraft == null || minecraft.level == null || trackedEntity == null) return null;
        if (trackedEntityLevel != minecraft.level
                || trackedEntity.isRemoved()
                || System.currentTimeMillis() > trackedEntityUntilMs) {
            clearTrackedEntity();
            return null;
        }
        return trackedEntity;
    }

    public static boolean isTrackedFromMemory(HitResult currentHit, Entity entity) {
        if (entity == null) return false;
        if (currentHit instanceof EntityHitResult entityHit) return entityHit.getEntity() != entity;
        return true;
    }

    public static BlockPos targetBlockPos(HitResult hit) {
        if (hit == null) return null;
        if (hit instanceof EntityHitResult entityHit) return entityHit.getEntity().blockPosition();
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) return blockHit.getBlockPos();
        return BlockPos.containing(hit.getLocation());
    }

    public static double targetDistance(Minecraft minecraft, HitResult hit) {
        if (minecraft.player == null || hit == null) return 0.0D;
        if (hit instanceof EntityHitResult entityHit) return minecraft.player.distanceTo(entityHit.getEntity());
        return minecraft.player.getEyePosition(1.0F).distanceTo(hit.getLocation());
    }

    private static void createWaypoint(Minecraft minecraft, LClientConfig config) {
        HitResult hit = getTargetHit(minecraft, true);
        BlockPos position = targetBlockPos(hit);
        if (position == null) {
            setStatus("Recon no encontró un punto para marcar");
            return;
        }

        if (!config.journeyMap || !config.journeyMapReconWaypoint) {
            setStatus("JourneyMap+ está desactivado");
            return;
        }
        if (!JourneyMapBridge.isInstalled()) {
            setStatus("JourneyMap no está instalado");
            return;
        }
        if (!JourneyMapBridge.isReady()) {
            setStatus(shortStatus(JourneyMapBridge.getStatusText()));
            return;
        }

        String label = targetLabel(minecraft, hit, position);
        if (JourneyMapBridge.markRecon(position, label, minecraft.level.dimension())) {
            setStatus("Waypoint · " + label + " · " + position.toShortString());
        } else {
            setStatus(shortStatus(JourneyMapBridge.getStatusText()));
        }
    }

    private static String targetLabel(Minecraft minecraft, HitResult hit, BlockPos position) {
        if (hit instanceof EntityHitResult entityHit) {
            return safeEntityLabel(entityHit.getEntity());
        }
        if (minecraft.level != null && hit != null && hit.getType() == HitResult.Type.BLOCK) {
            try {
                return BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(position).getBlock()).getPath();
            } catch (RuntimeException | LinkageError ignored) {
                return "Bloque";
            }
        }
        return "Dirección";
    }

    public static String safeEntityLabel(Entity entity) {
        if (entity == null) return "Entidad";
        String fallback = "Entidad";
        try {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (id != null) fallback = id.getPath();
        } catch (RuntimeException | LinkageError ignored) {
        }
        try {
            String name = entity.getName().getString();
            return name == null || name.isBlank() ? fallback : name;
        } catch (RuntimeException | LinkageError ignored) {
            return fallback;
        }
    }

    private static String shortStatus(String text) {
        if (text == null || text.isBlank()) return "JourneyMap+ no disponible";
        return text.length() <= 88 ? text : text.substring(0, 88);
    }

    private static void setStatus(String text) {
        statusText = text == null ? "" : text;
        statusUntil = System.currentTimeMillis() + 2200L;
    }

    private static void flushZoomConfigIfDue(LClientConfig config, boolean force) {
        if (!zoomConfigDirty) return;
        if (!force && System.currentTimeMillis() < zoomSaveAt) return;
        config.save();
        zoomConfigDirty = false;
        zoomSaveAt = 0L;
    }

    private static void invalidateTargetCache() {
        targetCacheLevel = null;
        targetCacheHit = null;
        targetCacheEye = null;
        targetCacheLook = null;
        targetCacheRange = -1;
        targetCacheUntilNs = 0L;
    }

    private static void clearTrackedEntity() {
        trackedEntityLevel = null;
        trackedEntity = null;
        trackedEntityUntilMs = 0L;
    }

    private static void resetRuntime() {
        zoomActive = false;
        waypointKeyDown = false;
        trackedLevel = null;
        smoothedFov = -1.0D;
        scrollAccumulator = 0.0D;
        statusText = "";
        statusUntil = 0L;
        invalidateTargetCache();
        clearTrackedEntity();
    }

    private static boolean keyDown(Minecraft minecraft, int keyCode) {
        return keyCode >= 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), keyCode);
    }
}
