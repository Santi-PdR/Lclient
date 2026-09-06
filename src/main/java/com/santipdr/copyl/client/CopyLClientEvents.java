package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.screen.LClientWheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class CopyLClientEvents {
    private static final boolean[] messageKeyDown = new boolean[CopyLKeyMappings.SLOT_COUNT];
    private static boolean wheelKeyDown;

    private static int smartFoodSourceSlot = -1;
    private static boolean smartOffhandActive;
    private static ItemStack smartOriginalOffhand = ItemStack.EMPTY;
    private static ItemStack smartInsertedFood = ItemStack.EMPTY;

    private CopyLClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LClientConfig config = LClientConfig.get();

        pollWheelKey(minecraft, config);
        pollQuickMessages(minecraft, config);

        if (minecraft.player == null || minecraft.level == null) {
            clearSmartOffhandState();
            return;
        }

        if (minecraft.screen == null) handleSmartOffhand(minecraft, config);
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

    private static void handleSmartOffhand(Minecraft minecraft, LClientConfig config) {
        if (!config.smartOffhand || minecraft.gameMode == null) {
            restoreManagedOffhandWhenDisabling(minecraft);
            return;
        }

        Player player = minecraft.player;
        if (!player.inventoryMenu.getCarried().isEmpty()) return;

        int foodLevel = player.getFoodData().getFoodLevel();
        if (!smartOffhandActive) {
            if (foodLevel > config.foodThreshold || player.getOffhandItem().isEdible()) return;

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
            return;
        }

        if (foodLevel < config.foodRestoreThreshold || player.isUsingItem()) return;
        if (!canSafelyRestore(player)) {
            clearSmartOffhandState();
            return;
        }

        swapInventoryWithOffhand(minecraft, smartFoodSourceSlot);
        clearSmartOffhandState();
    }

    private static void restoreManagedOffhandWhenDisabling(Minecraft minecraft) {
        if (!smartOffhandActive) {
            clearSmartOffhandState();
            return;
        }

        if (minecraft.player != null
                && minecraft.gameMode != null
                && minecraft.player.inventoryMenu.getCarried().isEmpty()
                && canSafelyRestore(minecraft.player)) {
            swapInventoryWithOffhand(minecraft, smartFoodSourceSlot);
        }
        clearSmartOffhandState();
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
            if (food != null) score += food.getNutrition() * 20.0F;

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
