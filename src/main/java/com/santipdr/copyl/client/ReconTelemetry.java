package com.santipdr.copyl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Lightweight telemetry derived only from entity data already present client-side. */
public final class ReconTelemetry {
    private static final String[] CARDINALS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};

    private ReconTelemetry() {
    }

    public record Snapshot(
            int horizontalDistance,
            int deltaY,
            double speedMetersPerSecond,
            double closingMetersPerSecond,
            float relativeBearingDegrees,
            String cardinal,
            String motion
    ) {
        public String compactLine() {
            String closing = Math.abs(closingMetersPerSecond) < 0.1D
                    ? "cierre 0.0"
                    : String.format(Locale.ROOT, "cierre %+,.1f", closingMetersPerSecond).replace(',', '.');
            String vertical = deltaY > 0 ? "+" + deltaY : Integer.toString(deltaY);
            return String.format(Locale.ROOT, "Vel %.1f m/s · %s · ΔY %s · %s", speedMetersPerSecond, closing, vertical, cardinal);
        }
    }

    public static Snapshot snapshot(Minecraft minecraft, Entity entity) {
        if (minecraft == null || minecraft.player == null || entity == null) return null;
        try {
            Vec3 playerPos = minecraft.player.position();
            Vec3 targetPos = entity.position();
            double dx = targetPos.x - playerPos.x;
            double dz = targetPos.z - playerPos.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            int horizontalDistance = Math.max(0, (int) Math.round(horizontal));
            int deltaY = (int) Math.round(targetPos.y - playerPos.y);

            Vec3 targetVelocity = entity.getDeltaMovement();
            Vec3 relativeVelocity = targetVelocity.subtract(minecraft.player.getDeltaMovement());
            double speed = safeFinite(targetVelocity.length() * 20.0D);

            Vec3 line = targetPos.subtract(playerPos);
            double closing = 0.0D;
            if (line.lengthSqr() > 1.0E-6D) {
                closing = safeFinite(-relativeVelocity.dot(line.normalize()) * 20.0D);
            }

            float absoluteYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float relativeBearing = Mth.wrapDegrees(absoluteYaw - minecraft.player.getYRot());
            String cardinal = cardinal(absoluteYaw);
            String motion = motion(relativeVelocity, line, closing);

            return new Snapshot(horizontalDistance, deltaY, speed, closing, relativeBearing, cardinal, motion);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static String cardinal(float yaw) {
        float normalized = yaw % 360.0F;
        if (normalized < 0.0F) normalized += 360.0F;
        int index = Math.floorMod(Math.round(normalized / 45.0F), CARDINALS.length);
        return CARDINALS[index];
    }

    private static String motion(Vec3 relativeVelocity, Vec3 line, double closing) {
        if (line.lengthSqr() > 1.0E-6D) {
            Vec3 horizontal = new Vec3(line.x, 0.0D, line.z);
            if (horizontal.lengthSqr() > 1.0E-6D) {
                Vec3 forward = horizontal.normalize();
                Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
                double lateral = safeFinite(relativeVelocity.dot(right) * 20.0D);
                if (lateral > 0.45D) return "derecha";
                if (lateral < -0.45D) return "izquierda";
            }
        }
        if (closing > 0.45D) return "acercándose";
        if (closing < -0.45D) return "alejándose";
        return "estable";
    }

    private static double safeFinite(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }
}
