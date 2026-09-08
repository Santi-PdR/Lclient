package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.screen.LClientWheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class CopyLClientEvents {
    private static final int MAX_OUTGOING_MESSAGE_LENGTH = 256;

    private static final boolean[] messageKeyDown = new boolean[CopyLKeyMappings.SLOT_COUNT];
    private static final int[] observedMessageKeys = new int[CopyLKeyMappings.SLOT_COUNT];
    private static boolean wheelKeyDown;
    private static boolean lootEspToggleKeyDown;
    private static int observedWheelKey = Integer.MIN_VALUE;
    private static int observedLootEspToggleKey = Integer.MIN_VALUE;

    private static int smartFoodSourceSlot = -1;
    private static boolean smartOffhandActive;
    private static ItemStack smartOriginalOffhand = ItemStack.EMPTY;
    private static ItemStack smartInsertedFood = ItemStack.EMPTY;
    private static String protectedOffhandKey = "";

    static {
        Arrays.fill(observedMessageKeys, Integer.MIN_VALUE);
    }

    private CopyLClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();

        pollWheelKey(minecraft, config);
        pollLootEspToggle(minecraft, config);
        pollQuickMessages(minecraft, config);

        if (minecraft.player == null || minecraft.level == null) {
            clearSmartOffhandState();
            return;
        }

        if (!config.smartOffhand) {
            restoreManagedOffhandWhenDisabling(minecraft);
            return;
        }

        if (minecraft.screen == null) handleSmartOffhand(minecraft, config);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (smartOffhandActive) restoreManagedOffhandWhenDisabling(minecraft);
        clearSmartOffhandState();
        LootEspRenderer.clearCache();
        resetTransientKeys();
    }

    private static void pollWheelKey(Minecraft minecraft, LClientConfig config) {
        int key = config.wheelKey;
        boolean down = keyDown(minecraft, key);
        if (observedWheelKey != key) {
            observedWheelKey = key;
            wheelKeyDown = down;
            return;
        }
        if (down && !wheelKeyDown && minecraft.screen == null) {
            minecraft.setScreen(new LClientWheelScreen(null));
        }
        wheelKeyDown = down;
    }

    private static void pollLootEspToggle(Minecraft minecraft, LClientConfig config) {
        int key = config.lootEspToggleKey;
        boolean down = keyDown(minecraft, key);
        if (observedLootEspToggleKey != key) {
            observedLootEspToggleKey = key;
            lootEspToggleKeyDown = down;
            return;
        }
        if (down && !lootEspToggleKeyDown
                && minecraft.screen == null
                && minecraft.player != null
                && minecraft.level != null) {
            config.lootEsp = !config.lootEsp;
            if (!config.lootEsp) LootEspRenderer.clearCache();
            config.save();
        }
        lootEspToggleKeyDown = down;
    }

    private static void pollQuickMessages(Minecraft minecraft, LClientConfig config) {
        MessageConfig messages = MessageConfig.getInstance();
        boolean canSend = config.quickMessages
                && minecraft.player != null
                && minecraft.player.connection != null
                && minecraft.screen == null;

        for (int i = 0; i < messageKeyDown.length; i++) {
            int key = messages.getKeyCode(i);
            boolean reserved = isReservedLclientKey(key, config);
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

    private static boolean isReservedLclientKey(int key, LClientConfig config) {
        if (key < 0) return false;
        return key == config.wheelKey
                || key == config.lootEspToggleKey
                || key == config.reconZoomKey
                || key == config.reconWaypointKey;
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

        String dimension = player.level().dimension().location().toString();
        TargetContext target = resolveTargetContext(minecraft);
        return message
                .replace("{x}", Integer.toString(player.blockPosition().getX()))
                .replace("{y}", Integer.toString(player.blockPosition().getY()))
                .replace("{z}", Integer.toString(player.blockPosition().getZ()))
                .replace("{pos}", player.blockPosition().toShortString())
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
                .replace("{targethp}", Integer.toString(target.health))
                .replace("{targetmaxhp}", Integer.toString(target.maxHealth))
                .replace("{targetspeed}", oneDecimal(target.speed))
                .replace("{targetdy}", Integer.toString(target.deltaY))
                .replace("{targetbearing}", target.bearing)
                .replace("{targetmotion}", target.motion)
                .replace("{targetitem}", target.item);
    }

    private static TargetContext resolveTargetContext(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return TargetContext.NONE;

        HitResult hit = ReconController.isZoomActive()
                ? ReconController.getTargetHit(minecraft)
                : minecraft.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return TargetContext.NONE;

        try {
            if (hit instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                BlockPos pos = entity.blockPosition();
                int distance = Math.max(0, (int) Math.round(minecraft.player.distanceTo(entity)));
                String type = safeEntityType(entity);
                int health = -1;
                int maxHealth = -1;
                String item = "none";
                if (entity instanceof LivingEntity living) {
                    try {
                        health = Math.round(living.getHealth());
                        maxHealth = Math.round(living.getMaxHealth());
                    } catch (RuntimeException | LinkageError ignored) {
                        health = -1;
                        maxHealth = -1;
                    }
                    ItemStack main = safeMainHand(living);
                    ItemStack off = safeOffhand(living);
                    if (!main.isEmpty()) item = safeStackLabel(main);
                    else if (!off.isEmpty()) item = safeStackLabel(off);
                }

                ReconTelemetry.Snapshot telemetry = ReconTelemetry.snapshot(minecraft, entity);
                double speed = telemetry == null ? 0.0D : telemetry.speedMetersPerSecond();
                int deltaY = telemetry == null ? pos.getY() - minecraft.player.blockPosition().getY() : telemetry.deltaY();
                String bearing = telemetry == null ? "none" : telemetry.cardinal();
                String motion = telemetry == null ? "none" : telemetry.motion();
                return new TargetContext(
                        ReconController.safeEntityLabel(entity),
                        type,
                        pos,
                        distance,
                        health,
                        maxHealth,
                        speed,
                        deltaY,
                        bearing,
                        motion,
                        item
                );
            }
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();
                var id = BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(pos).getBlock());
                String label = id == null ? "block" : id.toString();
                int distance = Math.max(0, (int) Math.round(
                        minecraft.player.getEyePosition(1.0F).distanceTo(hit.getLocation())
                ));
                int deltaY = pos.getY() - minecraft.player.blockPosition().getY();
                return new TargetContext(label, label, pos, distance, -1, -1, 0.0D, deltaY, "none", "none", "none");
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return TargetContext.NONE;
    }

    private record TargetContext(
            String label,
            String type,
            BlockPos position,
            int distance,
            int health,
            int maxHealth,
            double speed,
            int deltaY,
            String bearing,
            String motion,
            String item
    ) {
        private static final TargetContext NONE = new TargetContext(
                "none", "none", null, -1, -1, -1, 0.0D, 0, "none", "none", "none"
        );
    }

    private static String safeEntityType(Entity entity) {
        try {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return id == null ? "entity" : id.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "entity";
        }
    }

    private static ItemStack safeMainHand(LivingEntity living) {
        try {
            return living.getMainHandItem();
        } catch (RuntimeException | LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack safeOffhand(LivingEntity living) {
        try {
            return living.getOffhandItem();
        } catch (RuntimeException | LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", Double.isFinite(value) ? value : 0.0D);
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

    private static void handleSmartOffhand(Minecraft minecraft, LClientConfig config) {
        if (!config.smartOffhand || minecraft.gameMode == null) {
            restoreManagedOffhandWhenDisabling(minecraft);
            return;
        }

        Player player = minecraft.player;
        if (player == null) {
            clearSmartOffhandState();
            return;
        }
        if (!player.isAlive() || player.isSpectator()) {
            clearSmartOffhandState();
            return;
        }
        if (!player.inventoryMenu.getCarried().isEmpty()) return;

        int foodLevel = player.getFoodData().getFoodLevel();
        if (!smartOffhandActive) {
            if (foodLevel > config.foodThreshold || isUsableFoodStack(player.getOffhandItem(), player)) {
                protectedOffhandKey = "";
                return;
            }
            if (player.isUsingItem()) return;

            ItemStack original = player.getOffhandItem().copy();
            if (config.smartOffhandProtectCombatItems && isProtectedCombatOffhand(original)) {
                String key = protectedStackKey(original);
                if (!key.equals(protectedOffhandKey)) {
                    LClientNotifications.info(
                            "offhand-protected",
                            "Smart Offhand",
                            "Protegido: " + safeStackLabel(original)
                    );
                }
                protectedOffhandKey = key;
                return;
            }
            protectedOffhandKey = "";

            int source = findFoodSlot(player, config);
            if (source < 0) return;

            swapInventoryWithOffhand(minecraft, source);
            ItemStack inserted = player.getOffhandItem().copy();
            ItemStack sourceAfterSwap = player.getInventory().getItem(source);

            if (isUsableFoodStack(inserted, player)
                    && stackExactlyMatches(sourceAfterSwap, original)) {
                smartFoodSourceSlot = source;
                smartOriginalOffhand = original;
                smartInsertedFood = inserted;
                smartOffhandActive = true;
                LClientNotifications.info(
                        "offhand-equip",
                        "Smart Offhand",
                        "Equipada: " + safeStackLabel(inserted)
                );
            } else {
                if (stackExactlyMatches(sourceAfterSwap, original)
                        && !ItemStack.isSameItemSameTags(player.getOffhandItem(), original)) {
                    swapInventoryWithOffhand(minecraft, source);
                }
                clearSmartOffhandState();
            }
            return;
        }

        ItemStack currentOffhand = player.getOffhandItem();
        boolean stillManagedFood = currentOffhand.isEmpty()
                || (ItemStack.isSameItemSameTags(currentOffhand, smartInsertedFood)
                && currentOffhand.getCount() <= smartInsertedFood.getCount());
        if (!stillManagedFood) {
            clearSmartOffhandState();
            return;
        }

        if (foodLevel < config.foodRestoreThreshold || player.isUsingItem()) return;
        if (!canSafelyRestore(player)) {
            LClientNotifications.warning(
                    "offhand-restore-blocked",
                    "Smart Offhand",
                    "No restauré la offhand: el slot original cambió"
            );
            clearSmartOffhandState();
            return;
        }

        ItemStack restored = smartOriginalOffhand.copy();
        swapInventoryWithOffhand(minecraft, smartFoodSourceSlot);
        LClientNotifications.success(
                "offhand-restore",
                "Smart Offhand",
                restored.isEmpty() ? "Offhand original restaurada" : "Restaurado: " + safeStackLabel(restored)
        );
        clearSmartOffhandState();
    }

    private static void restoreManagedOffhandWhenDisabling(Minecraft minecraft) {
        if (!smartOffhandActive) return;

        Player player = minecraft.player;
        if (player == null || !player.isAlive() || player.isSpectator()) {
            clearSmartOffhandState();
            return;
        }
        if (minecraft.gameMode == null || !player.inventoryMenu.getCarried().isEmpty()) return;

        if (!canSafelyRestore(player)) {
            LClientNotifications.warning(
                    "offhand-disable-blocked",
                    "Smart Offhand",
                    "No restauré al desactivar: el slot original cambió"
            );
            clearSmartOffhandState();
            return;
        }

        ItemStack restored = smartOriginalOffhand.copy();
        swapInventoryWithOffhand(minecraft, smartFoodSourceSlot);
        LClientNotifications.success(
                "offhand-disable-restore",
                "Smart Offhand",
                restored.isEmpty() ? "Offhand original restaurada" : "Restaurado: " + safeStackLabel(restored)
        );
        clearSmartOffhandState();
    }

    private static boolean canSafelyRestore(Player player) {
        if (smartFoodSourceSlot < 0 || smartFoodSourceSlot >= 36) return false;
        ItemStack sourceStack = player.getInventory().getItem(smartFoodSourceSlot);
        return stackExactlyMatches(sourceStack, smartOriginalOffhand);
    }

    private static boolean stackExactlyMatches(ItemStack actual, ItemStack expected) {
        if (actual == null || expected == null) return false;
        if (expected.isEmpty()) return actual.isEmpty();
        return !actual.isEmpty()
                && actual.getCount() == expected.getCount()
                && ItemStack.isSameItemSameTags(actual, expected);
    }

    private static int findFoodSlot(Player player, LClientConfig config) {
        String preferredId = config.smartOffhandFoodId == null ? "" : config.smartOffhandFoodId.trim();
        if (!preferredId.isEmpty()) {
            int selected = findPreferredFoodSlot(player, preferredId);
            if (selected >= 0) return selected;
            if (!config.smartOffhandFallbackToAuto) return -1;
        }
        return findBestAutoFoodSlot(player);
    }

    private static int findPreferredFoodSlot(Player player, String preferredId) {
        ResourceLocation wanted = ResourceLocation.tryParse(preferredId);
        if (wanted == null) return -1;

        int bestSlot = -1;
        int bestCount = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!isUsableFoodStack(stack, player)) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!wanted.equals(id)) continue;
            if (stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static int findBestAutoFoodSlot(Player player) {
        int bestSlot = -1;
        float bestScore = -Float.MAX_VALUE;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!isUsableFoodStack(stack, player)) continue;

            FoodProperties food = safeFoodProperties(stack, player);
            if (food == null) continue;

            float score = Math.min(stack.getCount(), 16) * 0.45F;
            score += food.getNutrition() * 3.0F;
            score += food.getSaturationModifier() * food.getNutrition() * 2.0F;
            if (food.isFastFood()) score += 0.5F;
            score -= harmfulFoodPenalty(food);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static boolean isUsableFoodStack(ItemStack stack, Player player) {
        try {
            return stack != null
                    && !stack.isEmpty()
                    && stack.isEdible()
                    && stack.getFoodProperties(player) != null;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static FoodProperties safeFoodProperties(ItemStack stack, Player player) {
        try {
            return stack.getFoodProperties(player);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static float harmfulFoodPenalty(FoodProperties food) {
        float penalty = 0.0F;
        for (var entry : food.getEffects()) {
            var effect = entry.getFirst();
            if (effect == null || effect.getEffect().isBeneficial()) continue;

            Float chanceValue = entry.getSecond();
            float chance = chanceValue == null ? 0.0F : Math.max(0.0F, Math.min(1.0F, chanceValue));
            float durationWeight = Math.min(effect.getDuration(), 600) / 100.0F;
            float amplifierWeight = Math.min(effect.getAmplifier(), 4) + 1.0F;
            penalty += chance * (12.0F + durationWeight + amplifierWeight * 4.0F);
        }
        return penalty;
    }

    private static boolean isProtectedCombatOffhand(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            return stack.is(Items.TOTEM_OF_UNDYING) || stack.getItem() instanceof ShieldItem;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static String protectedStackKey(ItemStack stack) {
        try {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id == null ? safeStackLabel(stack) : id.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return safeStackLabel(stack);
        }
    }

    private static String safeStackLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "vacío";
        try {
            String name = stack.getHoverName().getString();
            if (name != null && !name.isBlank()) return name;
        } catch (RuntimeException | LinkageError ignored) {
        }
        try {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id == null ? "item" : id.getPath();
        } catch (RuntimeException | LinkageError ignored) {
            return "item";
        }
    }

    private static void swapInventoryWithOffhand(Minecraft minecraft, int inventoryIndex) {
        if (minecraft.player == null || minecraft.gameMode == null) return;
        if (inventoryIndex < 0 || inventoryIndex >= 36) return;
        if (!minecraft.player.inventoryMenu.getCarried().isEmpty()) return;

        int menuSlot = inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
        int containerId = minecraft.player.inventoryMenu.containerId;
        minecraft.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, minecraft.player);
        minecraft.gameMode.handleInventoryMouseClick(containerId, 45, 0, ClickType.PICKUP, minecraft.player);
        minecraft.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, minecraft.player);
    }

    private static void clearSmartOffhandState() {
        smartFoodSourceSlot = -1;
        smartOffhandActive = false;
        smartOriginalOffhand = ItemStack.EMPTY;
        smartInsertedFood = ItemStack.EMPTY;
        protectedOffhandKey = "";
    }

    private static void resetTransientKeys() {
        wheelKeyDown = false;
        lootEspToggleKeyDown = false;
        observedWheelKey = Integer.MIN_VALUE;
        observedLootEspToggleKey = Integer.MIN_VALUE;
        Arrays.fill(messageKeyDown, false);
        Arrays.fill(observedMessageKeys, Integer.MIN_VALUE);
    }

    private static boolean keyDown(Minecraft minecraft, int keyCode) {
        return keyCode >= 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), keyCode);
    }
}
