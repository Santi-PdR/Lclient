package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.integration.LClientJourneyMapPlugin;
import com.santipdr.copyl.client.screen.LClientWheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
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
    private static final Set<Integer> glowingLootIds = new HashSet<>();

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
            previousHealth = -1.0F;
            smartOffhandActive = false;
            smartFoodSourceSlot = -1;
            glowingLootIds.clear();
            return;
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
        if (message.startsWith("/") && message.length() > 1) minecraft.player.connection.sendCommand(message.substring(1));
        else minecraft.player.connection.sendChat(message);
    }

    private static void pollRecon(Minecraft minecraft, LClientConfig config) {
        boolean down = config.recon && keyDown(minecraft, config.reconMarkKey);
        if (down && !reconKeyDown && minecraft.screen == null && minecraft.hitResult != null) {
            if (minecraft.hitResult instanceof BlockHitResult block) {
                LClientHud.notify("Recon: " + block.getBlockPos().getX() + " " + block.getBlockPos().getY() + " " + block.getBlockPos().getZ());
                if (config.journeyMap) LClientJourneyMapPlugin.markRecon(block.getBlockPos(), minecraft.level.dimension());
            } else if (minecraft.hitResult instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                LClientHud.notify("Recon: " + entity.getName().getString() + " @ " + entity.blockPosition().toShortString());
                if (config.journeyMap) LClientJourneyMapPlugin.markRecon(entity.blockPosition(), minecraft.level.dimension());
            }
        }
        reconKeyDown = down;
    }

    private static void recordDamage(Minecraft minecraft, LClientConfig config) {
        float current = minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount();
        if (previousHealth >= 0.0F && current + 0.01F < previousHealth && config.combatPanel) {
            DamageSource source = minecraft.player.getLastDamageSource();
            Entity attacker = source == null ? null : source.getEntity();
            if (attacker == null && source != null) attacker = source.getDirectEntity();
            String who = attacker == null ? "Entidad/desconocido" : attacker.getName().getString();
            String hitPos = minecraft.player.blockPosition().toShortString();
            String attackerPos = attacker == null ? "?" : attacker.blockPosition().toShortString();
            LClientHud.recordCombat("Golpe de " + who + " | tú: " + hitPos + " | atacante: " + attackerPos);
            if (attacker != null && config.journeyMap && config.journeyMapAttackerWaypoint) {
                LClientJourneyMapPlugin.markAttacker(attacker.blockPosition(), who, minecraft.level.dimension());
            }
        }
        previousHealth = current;
    }

    private static void handleSmartOffhand(Minecraft minecraft, LClientConfig config) {
        if (!config.smartOffhand || minecraft.gameMode == null) return;
        Player player = minecraft.player;
        int food = player.getFoodData().getFoodLevel();

        if (!smartOffhandActive && food <= config.foodThreshold && !player.getOffhandItem().isEdible()) {
            int source = findFoodSlot(player);
            if (source >= 0) {
                swapInventoryWithOffhand(minecraft, source);
                smartFoodSourceSlot = source;
                smartOffhandActive = true;
                LClientHud.notify("Comida movida temporalmente a la mano secundaria");
            }
        } else if (smartOffhandActive && food >= config.foodRestoreThreshold) {
            if (smartFoodSourceSlot >= 0) swapInventoryWithOffhand(minecraft, smartFoodSourceSlot);
            smartFoodSourceSlot = -1;
            smartOffhandActive = false;
            LClientHud.notify("Objeto anterior restaurado en la mano secundaria");
        }
    }

    private static int findFoodSlot(Player player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.isEdible()) return i;
        }
        return -1;
    }

    private static void swapInventoryWithOffhand(Minecraft minecraft, int inventoryIndex) {
        int menuSlot = inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
        int containerId = minecraft.player.inventoryMenu.containerId;
        minecraft.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, minecraft.player);
        minecraft.gameMode.handleInventoryMouseClick(containerId, 45, 0, ClickType.PICKUP, minecraft.player);
        minecraft.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, minecraft.player);
    }

    private static void updateLootEsp(Minecraft minecraft, LClientConfig config) {
        Set<Integer> seen = new HashSet<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item)) continue;
            boolean shouldGlow = config.lootEsp && minecraft.player.distanceToSqr(item) <= (double) config.lootEspRange * config.lootEspRange;
            if (shouldGlow) {
                item.setGlowingTag(true);
                seen.add(item.getId());
            } else if (glowingLootIds.contains(item.getId())) {
                item.setGlowingTag(false);
            }
        }
        glowingLootIds.clear();
        glowingLootIds.addAll(seen);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        LClientConfig config = LClientConfig.get();
        if (!config.entityAlerts) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        Entity entity = event.getEntity();
        if (entity == minecraft.player || entity instanceof ItemEntity || entity instanceof net.minecraft.world.entity.LightningBolt) return;
        if (entity.tickCount > 2) return;
        double max = config.entityAlertRange;
        if (minecraft.player.distanceToSqr(entity) > max * max) return;
        int distance = (int) Math.round(minecraft.player.distanceTo(entity));
        LClientHud.notify("Ha aparecido una entidad cerca: " + entity.getName().getString() + " (" + distance + " m)");
    }

    private static boolean keyDown(Minecraft minecraft, int keyCode) {
        return keyCode >= 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), keyCode);
    }
}
