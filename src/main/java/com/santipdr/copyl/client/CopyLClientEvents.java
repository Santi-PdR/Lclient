package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.screen.MessageEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Locale;

/** The complete runtime surface of the CopyL-only client. */
@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class CopyLClientEvents {
    private static final int MAX_OUTGOING_MESSAGE_LENGTH = 256;
    private static final String[] CARDINALS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};

    private static final boolean[] messageKeyDown = new boolean[CopyLKeyMappings.SLOT_COUNT];
    private static final int[] observedMessageKeys = new int[CopyLKeyMappings.SLOT_COUNT];
    private static boolean openKeyDown;
    private static int observedOpenKey = Integer.MIN_VALUE;

    static {
        Arrays.fill(observedMessageKeys, Integer.MIN_VALUE);
    }

    private CopyLClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        CopyLConfig config = CopyLConfig.get();
        pollOpenKey(minecraft, config);
        pollQuickMessages(minecraft, config);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetTransientKeys();
    }

    private static void pollOpenKey(Minecraft minecraft, CopyLConfig config) {
        int key = config.openKey;
        boolean down = keyDown(minecraft, key);
        if (observedOpenKey != key) {
            observedOpenKey = key;
            openKeyDown = down;
            return;
        }

        if (down && !openKeyDown && minecraft.screen == null) {
            minecraft.setScreen(new MessageEditorScreen(null));
        }
        openKeyDown = down;
    }

    private static void pollQuickMessages(Minecraft minecraft, CopyLConfig config) {
        MessageConfig messages = MessageConfig.getInstance();
        boolean canSend = minecraft.player != null
                && minecraft.player.connection != null
                && minecraft.screen == null;

        for (int i = 0; i < messageKeyDown.length; i++) {
            int key = messages.getKeyCode(i);
            boolean reserved = key >= 0 && key == config.openKey;
            boolean down = key >= 0 && !reserved && keyDown(minecraft, key);

            if (observedMessageKeys[i] != key) {
                observedMessageKeys[i] = key;
                messageKeyDown[i] = down;
                continue;
            }

            if (canSend && down && !messageKeyDown[i]) sendSlot(minecraft, i);
            messageKeyDown[i] = down;
        }
    }

    private static void sendSlot(Minecraft minecraft, int slot) {
        if (minecraft.player == null || minecraft.player.connection == null) return;

        MessageConfig config = MessageConfig.getInstance();
        String message = config.getMessage(slot);
        if (message.isBlank()) return;

        message = expandQuickMessage(message, minecraft);
        message = truncateUtf16Safely(message, MAX_OUTGOING_MESSAGE_LENGTH);
        if (message.isBlank()) return;

        if (message.startsWith("/") && message.length() > 1) {
            minecraft.player.connection.sendCommand(message.substring(1));
        } else {
            minecraft.player.connection.sendChat(message);
        }
    }

    private static String expandQuickMessage(String message, Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) return message;

        TargetContext target = resolveTargetContext(minecraft);
        BlockPos playerPos = player.blockPosition();
        String dimension = player.level().dimension().location().toString();

        return message
                .replace("{x}", Integer.toString(playerPos.getX()))
                .replace("{y}", Integer.toString(playerPos.getY()))
                .replace("{z}", Integer.toString(playerPos.getZ()))
                .replace("{pos}", playerPos.toShortString())
                .replace("{dim}", dimension)
                .replace("{hp}", Integer.toString(Math.round(player.getHealth())))
                .replace("{food}", Integer.toString(player.getFoodData().getFoodLevel()))
                .replace("{name}", player.getGameProfile().getName())
                .replace("{yaw}", Integer.toString(Math.round(player.getYRot())))
                .replace("{pitch}", Integer.toString(Math.round(player.getXRot())))
                .replace("{target}", target.label)
                .replace("{targettype}", target.type)
                .replace("{targetdist}", Integer.toString(target.distance))
                .replace("{targetpos}", target.position == null ? "none" : target.position.toShortString())
                .replace("{targetx}", target.position == null ? "-" : Integer.toString(target.position.getX()))
                .replace("{targety}", target.position == null ? "-" : Integer.toString(target.position.getY()))
                .replace("{targetz}", target.position == null ? "-" : Integer.toString(target.position.getZ()))
                .replace("{targethp}", target.hp)
                .replace("{targetmaxhp}", target.maxHp)
                .replace("{targetspeed}", target.speed)
                .replace("{targetdy}", target.deltaY)
                .replace("{targetbearing}", target.bearing)
                .replace("{targetmotion}", target.motion)
                .replace("{targetitem}", target.item);
    }

    private static TargetContext resolveTargetContext(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) return TargetContext.NONE;

        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return TargetContext.NONE;

        try {
            if (hit instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                BlockPos position = entity.blockPosition();
                int distance = Math.max(0, (int) Math.round(player.distanceTo(entity)));
                String hp = "-";
                String maxHp = "-";
                String item = "-";

                if (entity instanceof LivingEntity living) {
                    hp = Integer.toString(Math.round(living.getHealth()));
                    maxHp = Integer.toString(Math.round(living.getMaxHealth()));
                    item = visibleItem(living);
                }

                double speed = entity.getDeltaMovement().length() * 20.0D;
                int deltaY = (int) Math.round(entity.getY() - player.getY());
                double dx = entity.getX() - player.getX();
                double dz = entity.getZ() - player.getZ();

                return new TargetContext(
                        safeEntityLabel(entity),
                        safeEntityType(entity),
                        position,
                        distance,
                        hp,
                        maxHp,
                        format1(speed),
                        Integer.toString(deltaY),
                        cardinal(dx, dz),
                        relativeMotion(player, entity),
                        item
                );
            }

            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos position = blockHit.getBlockPos();
                var id = BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(position).getBlock());
                String label = id == null ? "block" : id.toString();
                int distance = Math.max(0, (int) Math.round(
                        player.getEyePosition(1.0F).distanceTo(hit.getLocation())
                ));
                return new TargetContext(label, label, position, distance,
                        "-", "-", "-", "-", "-", "-", "-");
            }
        } catch (RuntimeException | LinkageError ignored) {
        }

        return TargetContext.NONE;
    }

    private static String safeEntityLabel(Entity entity) {
        try {
            String name = entity.getName().getString();
            if (name != null && !name.isBlank()) return name;
        } catch (RuntimeException | LinkageError ignored) {
        }
        return safeEntityType(entity);
    }

    private static String safeEntityType(Entity entity) {
        try {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return id == null ? "entity" : id.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "entity";
        }
    }

    private static String visibleItem(LivingEntity living) {
        try {
            ItemStack main = living.getMainHandItem();
            if (main != null && !main.isEmpty()) return safeItemName(main);
            ItemStack off = living.getOffhandItem();
            if (off != null && !off.isEmpty()) return safeItemName(off);
        } catch (RuntimeException | LinkageError ignored) {
        }
        return "-";
    }

    private static String safeItemName(ItemStack stack) {
        try {
            String name = stack.getHoverName().getString();
            if (name != null && !name.isBlank()) return name;
        } catch (RuntimeException | LinkageError ignored) {
        }
        try {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id == null ? "item" : id.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "item";
        }
    }

    private static String cardinal(double dx, double dz) {
        if (Math.abs(dx) < 1.0E-6D && Math.abs(dz) < 1.0E-6D) return "-";
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float normalized = yaw % 360.0F;
        if (normalized < 0.0F) normalized += 360.0F;
        int index = Math.floorMod(Math.round(normalized / 45.0F), CARDINALS.length);
        return CARDINALS[index];
    }

    private static String relativeMotion(Player player, Entity target) {
        try {
            Vec3 line = target.position().subtract(player.position());
            if (line.lengthSqr() <= 1.0E-6D) return "estable";

            Vec3 relativeVelocity = target.getDeltaMovement().subtract(player.getDeltaMovement());
            Vec3 horizontal = new Vec3(line.x, 0.0D, line.z);
            if (horizontal.lengthSqr() > 1.0E-6D) {
                Vec3 forward = horizontal.normalize();
                Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
                double lateral = relativeVelocity.dot(right) * 20.0D;
                if (lateral > 0.45D) return "derecha";
                if (lateral < -0.45D) return "izquierda";
            }

            double closing = -relativeVelocity.dot(line.normalize()) * 20.0D;
            if (closing > 0.45D) return "acercándose";
            if (closing < -0.45D) return "alejándose";
        } catch (RuntimeException | LinkageError ignored) {
        }
        return "estable";
    }

    private static String format1(double value) {
        if (!Double.isFinite(value)) return "-";
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private record TargetContext(
            String label,
            String type,
            BlockPos position,
            int distance,
            String hp,
            String maxHp,
            String speed,
            String deltaY,
            String bearing,
            String motion,
            String item
    ) {
        private static final TargetContext NONE = new TargetContext(
                "none", "none", null, -1,
                "-", "-", "-", "-", "-", "-", "-"
        );
    }

    private static String truncateUtf16Safely(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) return value == null ? "" : value;
        int end = maxChars;
        if (end > 0
                && end < value.length()
                && Character.isHighSurrogate(value.charAt(end - 1))
                && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static void resetTransientKeys() {
        openKeyDown = false;
        observedOpenKey = Integer.MIN_VALUE;
        Arrays.fill(messageKeyDown, false);
        Arrays.fill(observedMessageKeys, Integer.MIN_VALUE);
    }

    private static boolean keyDown(Minecraft minecraft, int keyCode) {
        return keyCode >= 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), keyCode);
    }
}
