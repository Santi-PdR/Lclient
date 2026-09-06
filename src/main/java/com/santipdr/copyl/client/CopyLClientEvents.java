package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.integration.JourneyMapBridge;
import com.santipdr.copyl.client.screen.LClientWheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class CopyLClientEvents {
    private static final boolean[] messageKeyDown = new boolean[CopyLKeyMappings.SLOT_COUNT];
    private static boolean wheelKeyDown;
    private static boolean reconKeyDown;
    private static float previousHealth = -1.0F;
    private static int tickCounter;

    private static int smartFoodSourceSlot = -1;
    private static boolean smartOffhandActive;
    private static ItemStack smartOriginalOffhand = ItemStack.EMPTY;
    private static ItemStack smartInsertedFood = ItemStack.EMPTY;

    private static final Map<Integer, Boolean> glowingLootOriginal = new HashMap<>();
    private static final Map<Long, Integer> warmChunks = new HashMap<>();
    private static ClientLevel trackedLevel;
    private static int worldEnteredTick;

    private CopyLClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();
        tickCounter++;

        pollWheelKey(minecraft, config);
        pollQuickMessages(minecraft, config);

        if (minecraft.player == null || minecraft.level == null) {
            resetWorldState();
            return;
        }

        if (minecraft.level != trackedLevel) {
            resetWorldState();
            trackedLevel = minecraft.level;
            worldEnteredTick = tickCounter;
        }

        if (config.entityAlerts && tickCounter % 10 == 0) {
            updateWarmChunks(minecraft, config);
        } else if (!config.entityAlerts) {
            warmChunks.clear();
        }

        pollRecon(minecraft, config);
        recordDamage(minecraft, config);
        if (minecraft.screen == null) handleSmartOffhand(minecraft, config);
        if ((tickCounter & 3) == 0) updateLootEsp(minecraft, config);
    }

    private static void pollWheelKey(Minecraft minecraft, LClientConfig config) {
        boolean down = keyDown(minecraft, config.wheelKey);
        if (down && !wheelKeyDown && minecraft.screen == null) {
            minecraft.setScreen(new LClientWheelScreen(null));
        }
        wheelKeyDown = down;
    }

    private static void pollQuickMessages(Minecraft minecraft, LClientConfig config) {
        if (!config.quickMessages || minecraft.player == null || minecraft.player.connection == null || minecraft.screen != null) {
            for (int i = 0; i < messageKeyDown.length; i++) messageKeyDown[i] = false;
            return;
        }

        MessageConfig messages = MessageConfig.getInstance();
        for (int i = 0; i < messageKeyDown.length; i++) {
            int key = messages.getKeyCode(i);
            boolean down = key >= 0 && keyDown(minecraft, key);
            if (down && !messageKeyDown[i]) sendSlot(minecraft, i);
            messageKeyDown[i] = down;
        }
    }

    private static void sendSlot(Minecraft minecraft, int slot) {
        String message = MessageConfig.getInstance().getMessage(slot);
        if (message.isBlank()) return;
        if (message.startsWith("/") && message.length() > 1) {
            minecraft.player.connection.sendCommand(message.substring(1));
        } else {
            minecraft.player.connection.sendChat(message);
        }
    }

    private static void pollRecon(Minecraft minecraft, LClientConfig config) {
        boolean down = config.recon && keyDown(minecraft, config.reconMarkKey);
        if (down && !reconKeyDown && minecraft.screen == null && minecraft.hitResult != null) {
            if (minecraft.hitResult instanceof BlockHitResult block) {
                LClientHud.notify("Recon: " + block.getBlockPos().toShortString());
                if (config.journeyMap && config.journeyMapReconWaypoint) {
                    JourneyMapBridge.markRecon(block.getBlockPos(), minecraft.level.dimension());
                }
            } else if (minecraft.hitResult instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                LClientHud.notify("Recon: " + entity.getName().getString() + " @ " + entity.blockPosition().toShortString());
                if (config.journeyMap && config.journeyMapReconWaypoint) {
                    JourneyMapBridge.markRecon(entity.blockPosition(), minecraft.level.dimension());
                }
            }
        }
        reconKeyDown = down;
    }

    private static void recordDamage(Minecraft minecraft, LClientConfig config) {
        float current = minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount();
        if (previousHealth >= 0.0F && current + 0.01F < previousHealth && config.combatPanel) {
            float damage = previousHealth - current;
            DamageSource source = minecraft.player.getLastDamageSource();
            Entity attacker = source == null ? null : source.getEntity();
            if (attacker == null && source != null) attacker = source.getDirectEntity();

            String who = attacker == null ? "Entidad/desconocido" : attacker.getName().getString();
            String type = attacker == null
                    ? "desconocida"
                    : BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString();
            String playerPos = minecraft.player.blockPosition().toShortString();
            String attackerPos = attacker == null ? "?" : attacker.blockPosition().toShortString();
            String dimension = minecraft.level.dimension().location().toString();

            LClientHud.recordCombat(String.format(
                    Locale.ROOT,
                    "%.1f dmg | %s [%s] | tú %s | atacante %s | %s",
                    damage,
                    who,
                    type,
                    playerPos,
                    attackerPos,
                    dimension
            ));

            if (attacker != null && config.journeyMap && config.journeyMapAttackerWaypoint) {
                JourneyMapBridge.markAttacker(attacker.blockPosition(), who, minecraft.level.dimension());
            }
        }
        previousHealth = current;
    }

    private static void handleSmartOffhand(Minecraft minecraft, LClientConfig config) {
        if (!config.smartOffhand || minecraft.gameMode == null) {
            clearSmartOffhandState();
            return;
        }

        Player player = minecraft.player;
        int food = player.getFoodData().getFoodLevel();

        if (!smartOffhandActive) {
            if (food > config.foodThreshold || player.getOffhandItem().isEdible()) return;

            int source = findBestFoodSlot(player);
            if (source < 0) return;

            ItemStack original = player.getOffhandItem().copy();
            swapInventoryWithOffhand(minecraft, source);
            ItemStack inserted = player.getOffhandItem().copy();

            if (!inserted.isEmpty() && inserted.isEdible()) {
                smartFoodSourceSlot = source;
                smartOriginalOffhand = original;
                smartInsertedFood = inserted;
                smartOffhandActive = true;
                LClientHud.notify("Comida movida temporalmente a la mano secundaria");
            } else {
                swapInventoryWithOffhand(minecraft, source);
                clearSmartOffhandState();
            }
            return;
        }

        ItemStack currentOffhand = player.getOffhandItem();
        boolean stillManagedFood = currentOffhand.isEmpty()
                || ItemStack.isSameItemSameTags(currentOffhand, smartInsertedFood);
        if (!stillManagedFood) {
            clearSmartOffhandState();
            LClientHud.notify("Smart Offhand cancelado: cambiaste la mano secundaria");
            return;
        }

        if (food < config.foodRestoreThreshold || player.isUsingItem()) return;

        if (!canSafelyRestore(player)) {
            clearSmartOffhandState();
            LClientHud.notify("Smart Offhand no restauró para evitar mover un objeto incorrecto");
            return;
        }

        swapInventoryWithOffhand(minecraft, smartFoodSourceSlot);
        clearSmartOffhandState();
        LClientHud.notify("Objeto anterior restaurado en la mano secundaria");
    }

    private static boolean canSafelyRestore(Player player) {
        if (smartFoodSourceSlot < 0 || smartFoodSourceSlot >= 36) return false;
        ItemStack sourceStack = player.getInventory().getItem(smartFoodSourceSlot);
        if (smartOriginalOffhand.isEmpty()) return sourceStack.isEmpty();
        return !sourceStack.isEmpty() && ItemStack.isSameItemSameTags(sourceStack, smartOriginalOffhand);
    }

    private static int findBestFoodSlot(Player player) {
        int bestSlot = -1;
        float bestScore = -1.0F;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.isEdible()) continue;

            FoodProperties food = stack.getItem().getFoodProperties();
            float score = stack.getCount();
            if (food != null) {
                score += food.getNutrition() * 20.0F + food.getSaturationMod() * 10.0F;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static void swapInventoryWithOffhand(Minecraft minecraft, int inventoryIndex) {
        int menuSlot = inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
        int containerId = minecraft.player.inventoryMenu.containerId;
        minecraft.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, minecraft.player);
        minecraft.gameMode.handleInventoryMouseClick(containerId, 45, 0, ClickType.PICKUP, minecraft.player);
        minecraft.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, minecraft.player);
    }

    private static void updateLootEsp(Minecraft minecraft, LClientConfig config) {
        Set<Integer> present = new HashSet<>();
        double maxDistanceSq = (double) config.lootEspRange * config.lootEspRange;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item)) continue;
            int id = item.getId();
            present.add(id);

            boolean shouldGlow = config.lootEsp && minecraft.player.distanceToSqr(item) <= maxDistanceSq;
            if (shouldGlow) {
                glowingLootOriginal.putIfAbsent(id, item.isGlowing());
                item.setGlowingTag(true);
            } else if (glowingLootOriginal.containsKey(id)) {
                item.setGlowingTag(glowingLootOriginal.remove(id));
            }
        }

        glowingLootOriginal.keySet().removeIf(id -> !present.contains(id));
    }

    private static void updateWarmChunks(Minecraft minecraft, LClientConfig config) {
        int radiusChunks = Math.max(2, (config.entityAlertRange + 15) / 16 + 1);
        ChunkPos center = minecraft.player.chunkPosition();
        Set<Long> relevantLoaded = new HashSet<>();

        for (int x = center.x - radiusChunks; x <= center.x + radiusChunks; x++) {
            for (int z = center.z - radiusChunks; z <= center.z + radiusChunks; z++) {
                if (!minecraft.level.hasChunk(x, z)) continue;
                long key = ChunkPos.asLong(x, z);
                relevantLoaded.add(key);
                warmChunks.putIfAbsent(key, tickCounter);
            }
        }

        warmChunks.keySet().removeIf(key -> !relevantLoaded.contains(key));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;

        LClientConfig config = LClientConfig.get();
        if (!config.entityAlerts) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.level != event.getLevel()) return;

        Entity entity = event.getEntity();
        if (entity == minecraft.player
                || entity instanceof ItemEntity
                || entity instanceof net.minecraft.world.entity.LightningBolt) {
            return;
        }

        double max = config.entityAlertRange;
        if (minecraft.player.distanceToSqr(entity) > max * max) return;

        // Suppress the flood produced by initial login, dimension changes and
        // entities that merely enter client render/tracking range with a chunk.
        if (tickCounter - worldEnteredTick < config.entityAlertWarmupTicks) return;
        long chunkKey = ChunkPos.asLong(entity.chunkPosition().x, entity.chunkPosition().z);
        Integer firstSeenTick = warmChunks.get(chunkKey);
        if (firstSeenTick == null || tickCounter - firstSeenTick < config.entityAlertWarmupTicks) return;

        int distance = (int) Math.round(minecraft.player.distanceTo(entity));
        LClientHud.notify("Ha aparecido una entidad cerca (" + distance + " m)");
    }

    private static void resetWorldState() {
        previousHealth = -1.0F;
        clearSmartOffhandState();
        glowingLootOriginal.clear();
        warmChunks.clear();
        trackedLevel = null;
    }

    private static void clearSmartOffhandState() {
        smartFoodSourceSlot = -1;
        smartOffhandActive = false;
        smartOriginalOffhand = ItemStack.EMPTY;
        smartInsertedFood = ItemStack.EMPTY;
    }

    private static boolean keyDown(Minecraft minecraft, int keyCode) {
        return keyCode >= 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), keyCode);
    }
}
