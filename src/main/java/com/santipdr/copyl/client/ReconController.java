package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.integration.JourneyMapBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tactical Recon controller with render-only zoom and long-range client raycast. */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class ReconController {
    private static boolean zoomActive;
    private static boolean waypointKeyDown;
    private static Object trackedLevel;
    private static double smoothedFov = -1.0D;
    private static String statusText = "";
    private static long statusUntil;

    private ReconController() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();

        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }

        if (trackedLevel != minecraft.level) {
            reset();
            trackedLevel = minecraft.level;
        }

        boolean canUseRecon = config.recon && minecraft.screen == null;
        zoomActive = canUseRecon && keyDown(minecraft, config.reconZoomKey);
        if (!zoomActive) smoothedFov = -1.0D;

        boolean waypointDown = canUseRecon && keyDown(minecraft, config.reconWaypointKey);
        if (zoomActive && waypointDown && !waypointKeyDown) {
            createWaypoint(minecraft, config);
        }
        waypointKeyDown = waypointDown;
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!zoomActive || event.getScrollDelta() == 0.0D) return;

        LClientConfig config = LClientConfig.get();
        int direction = event.getScrollDelta() > 0.0D ? -1 : 1;
        int next = Mth.clamp(config.reconZoomFov + direction * 2, 8, 50);
        if (next != config.reconZoomFov) {
            config.reconZoomFov = next;
            config.save();
            setStatus("Zoom " + zoomText(Minecraft.getInstance(), next));
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!zoomActive) {
            smoothedFov = -1.0D;
            return;
        }

        double target = LClientConfig.get().reconZoomFov;
        if (smoothedFov < 0.0D) smoothedFov = event.getFOV();
        smoothedFov += (target - smoothedFov) * 0.38D;
        event.setFOV(smoothedFov);
    }

    public static boolean isZoomActive() {
        return zoomActive;
    }

    public static int getZoomFov() {
        return LClientConfig.get().reconZoomFov;
    }

    public static String getZoomText() {
        return zoomText(Minecraft.getInstance(), LClientConfig.get().reconZoomFov);
    }

    private static String zoomText(Minecraft minecraft, int targetFov) {
        int baseFov = minecraft.options.fov().get();
        double multiplier = Math.max(1.0D, (double) baseFov / Math.max(1, targetFov));
        return String.format(java.util.Locale.ROOT, "x%.1f", multiplier);
    }

    public static String getStatusText() {
        return System.currentTimeMillis() <= statusUntil ? statusText : "";
    }

    public static HitResult getTargetHit() {
        return getTargetHit(Minecraft.getInstance());
    }

    public static HitResult getTargetHit(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return null;

        double range = LClientConfig.get().reconRange;
        Vec3 eye = minecraft.player.getEyePosition(1.0F);
        Vec3 look = minecraft.player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult blockHit = minecraft.level.clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));

        double blockDistanceSq = blockHit.getType() == HitResult.Type.MISS
                ? range * range
                : eye.distanceToSqr(blockHit.getLocation());

        AABB searchBox = minecraft.player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                minecraft.player,
                eye,
                end,
                searchBox,
                entity -> entity != minecraft.player && !entity.isSpectator() && entity.isPickable(),
                blockDistanceSq
        );

        return entityHit != null ? entityHit : blockHit;
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
        HitResult hit = getTargetHit(minecraft);
        BlockPos position = targetBlockPos(hit);
        if (position == null) return;

        if (!config.journeyMap || !config.journeyMapReconWaypoint || !JourneyMapBridge.isReady()) {
            setStatus("JourneyMap+ no disponible");
            return;
        }

        String label = targetLabel(minecraft, hit, position);
        JourneyMapBridge.markRecon(position, label, minecraft.level.dimension());
        setStatus("Waypoint · " + label + " · " + position.toShortString());
    }

    private static String targetLabel(Minecraft minecraft, HitResult hit, BlockPos position) {
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            String name = entity.getName().getString();
            return name.isBlank() ? BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath() : name;
        }
        if (minecraft.level != null && hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(position).getBlock()).getPath();
        }
        return "Dirección";
    }

    private static void setStatus(String text) {
        statusText = text;
        statusUntil = System.currentTimeMillis() + 1800L;
    }

    private static void reset() {
        zoomActive = false;
        waypointKeyDown = false;
        trackedLevel = null;
        smoothedFov = -1.0D;
        statusText = "";
        statusUntil = 0L;
    }

    private static boolean keyDown(Minecraft minecraft, int keyCode) {
        return keyCode >= 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), keyCode);
    }
}
