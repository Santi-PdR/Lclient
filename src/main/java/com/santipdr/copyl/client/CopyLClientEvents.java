package com.santipdr.copyl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.CopyL;
import com.santipdr.copyl.client.screen.LClientWheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CopyL.MOD_ID, value = Dist.CLIENT)
public final class CopyLClientEvents {
    private static final int MAX_OUTGOING_MESSAGE_LENGTH = 256;

    private static final boolean[] messageKeyDown = new boolean[CopyLKeyMappings.SLOT_COUNT];
    private static boolean wheelKeyDown;
    private static boolean lootEspToggleKeyDown;

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
        pollLootEspToggle(minecraft, config);
        pollQuickMessages(minecraft, config);

        if (minecraft.player == null || minecraft.level == null) {
            clearSmartOffhandState();
            return;
        }

        if (minecraft.screen == null) handleSmartOffhand(minecraft, config);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        // Forge fires this while the local player/game mode are normally still
        // available. Restore only if the tracked swap can still be proven safe.
        if (smartOffhandActive) restoreManagedOffhandWhenDisabling(minecraft);
        resetTransientKeys();
    }

    private static void pollWheelKey(Minecraft minecraft, LClientConfig config) {
        boolean down = keyDown(minecraft, config.wheelKey);
        if (down && !wheelKeyDown && minecraft.screen == null) {
            minecraft.setScreen(new LClientWheelScreen(null));
        }
        wheelKeyDown = down;
    }

    private static void pollLootEspToggle(Minecraft minecraft, LClientConfig config) {
        boolean down = keyDown(minecraft, config.lootEspToggleKey);
        if (down && !lootEspToggleKeyDown
                && minecraft.screen == null
                && minecraft.player != null
                && minecraft.level != null) {
            config.lootEsp = !config.lootEsp;
            if (!config.lootEsp) LootEspRenderer.clearCache();
            config.save();
        }
        // Keep the physical state synchronized even while a screen is open so
        // closing a menu with X held cannot look like a new press.
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

            if (canSend && down && !messageKeyDown[i]) sendSlot(minecraft, i);

            // Important: while a GUI is open, mirror the real physical state
            // instead of resetting to false. Otherwise a key held while the GUI
            // closes becomes a phantom fresh press on the next tick.
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

        String message = MessageConfig.getInstance().getMessage(slot);
        if (message.isBlank()) return;

        message = expandQuickMessage(message, minecraft.player);
        if (message.length() > MAX_OUTGOING_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_OUTGOING_MESSAGE_LENGTH);
        }
        if (message.isBlank()) return;

        if (message.startsWith("/") && message.length() > 1) {
            minecraft.player.connection.sendCommand(message.substring(1));
        } else {
            minecraft.player.connection.sendChat(message);
        }
    }

    private static String expandQuickMessage(String message, Player player) {
        String dimension = player.level().dimension().location().toString();
        return message
                .replace("{x}", Integer.toString(player.blockPosition().getX()))
                .replace("{y}", Integer.toString(player.blockPosition().getY()))
                .replace("{z}", Integer.toString(player.blockPosition().getZ()))
                .replace("{pos}", player.blockPosition().toShortString())
                .replace("{dim}", dimension)
                .replace("{hp}", Integer.toString(Math.round(player.getHealth())))
                .replace("{food}", Integer.toString(player.getFoodData().getFoodLevel()))
                .replace("{name}", player.getGameProfile().getName());
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
        // Never send inventory click packets during death/spectator transitions.
        // The server may be rebuilding the player inventory at exactly that time.
        if (!player.isAlive() || player.isSpectator()) {
            clearSmartOffhandState();
            return;
        }
        if (!player.inventoryMenu.getCarried().isEmpty()) return;

        int foodLevel = player.getFoodData().getFoodLevel();
        if (!smartOffhandActive) {
            if (foodLevel > config.foodThreshold || player.getOffhandItem().isEdible() || player.isUsingItem()) return;

            int source = findFoodSlot(player, config);
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
                // Server/mod rejected or changed the click sequence. Attempt to
                // put the slot back only when the first swap visibly succeeded.
                if (!ItemStack.isSameItemSameTags(player.getOffhandItem(), original)) {
                    swapInventoryWithOffhand(minecraft, source);
                }
                clearSmartOffhandState();
            }
            return;
        }

        ItemStack currentOffhand = player.getOffhandItem();
        boolean stillManagedFood = currentOffhand.isEmpty()
                || ItemStack.isSameItemSameTags(currentOffhand, smartInsertedFood);
        if (!stillManagedFood) {
            // User or another mod took control of offhand; stop managing it and
            // never overwrite that newer choice.
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
                && minecraft.player.isAlive()
                && !minecraft.player.isSpectator()
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
            if (stack.isEmpty() || !stack.isEdible()) continue;
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
        float bestScore = -1.0F;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.isEdible()) continue;

            FoodProperties food = stack.getItem().getFoodProperties();
            float score = Math.min(stack.getCount(), 16) * 0.45F;
            if (food != null) {
                score += food.getNutrition() * 3.0F;
                score += food.getSaturationModifier() * food.getNutrition() * 2.0F;
                if (food.isFastFood()) score += 0.5F;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static void swapInventoryWithOffhand(Minecraft minecraft, int inventoryIndex) {
        if (minecraft.player == null || minecraft.gameMode == null) return;
        if (inventoryIndex < 0 || inventoryIndex >= 36) return;

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

    private static void resetTransientKeys() {
        wheelKeyDown = false;
        lootEspToggleKeyDown = false;
        for (int i = 0; i < messageKeyDown.length; i++) messageKeyDown[i] = false;
    }

    private static boolean keyDown(Minecraft minecraft, int keyCode) {
        return keyCode >= 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), keyCode);
    }
}
