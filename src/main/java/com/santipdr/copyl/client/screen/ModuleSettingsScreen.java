package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.LClientConfig;
import com.santipdr.copyl.client.LootEspRenderer;
import com.santipdr.copyl.client.MessageConfig;
import com.santipdr.copyl.client.integration.JourneyMapBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class ModuleSettingsScreen extends Screen {
    private enum CaptureTarget {
        NONE,
        LOOT_TOGGLE,
        RECON_ZOOM,
        RECON_WAYPOINT
    }

    private final Screen parent;
    private final LClientWheelScreen.Module module;
    private Button enabledButton;
    private Button secondaryButton;
    private Button tertiaryButton;
    private Button actionButton;
    private Button quaternaryButton;
    private CaptureTarget captureTarget = CaptureTarget.NONE;
    private String feedback = "";
    private long feedbackUntil;

    public ModuleSettingsScreen(Screen parent, LClientWheelScreen.Module module) {
        super(Component.literal(module.title));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        boolean compact = height < 250;
        boolean tight = height < 200;
        int buttonHeight = compact ? 18 : 20;
        int step = tight ? 21 : compact ? 23 : 27;
        int startY = tight ? 34 : compact ? 50 : height / 2 - 66;
        int buttonWidth = Math.min(240, Math.max(180, width - 24));
        int left = cx - buttonWidth / 2;

        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), b -> {
            toggleEnabled();
            refreshLabels();
        }).bounds(left, startY, buttonWidth, buttonHeight).build());

        secondaryButton = addRenderableWidget(Button.builder(secondaryLabel(), b -> secondaryAction())
                .bounds(left, startY + step, buttonWidth, buttonHeight).build());
        tertiaryButton = addRenderableWidget(Button.builder(tertiaryLabel(), b -> tertiaryAction())
                .bounds(left, startY + step * 2, buttonWidth, buttonHeight).build());
        actionButton = addRenderableWidget(Button.builder(actionLabel(), b -> action())
                .bounds(left, startY + step * 3, buttonWidth, buttonHeight).build());
        quaternaryButton = addRenderableWidget(Button.builder(quaternaryLabel(), b -> quaternaryAction())
                .bounds(left, startY + step * 4, buttonWidth, buttonHeight).build());

        int backY = startY + step * 5 + (compact ? 2 : 7);
        addRenderableWidget(Button.builder(Component.literal("Volver a la ruleta"), b -> onClose())
                .bounds(left, backY, buttonWidth, buttonHeight).build());

        updateVisibility();
    }

    private Component enabledLabel() {
        return Component.literal("Estado: " + (isEnabled() ? "ACTIVADO" : "DESACTIVADO"));
    }

    private Component secondaryLabel() {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case LOOT_ESP -> Component.literal("Alcance: " + c.lootEspRange + " m");
            case SMART_OFFHAND -> Component.literal("Mover comida con hambre ≤ " + c.foodThreshold);
            case RECON -> Component.literal(captureTarget == CaptureTarget.RECON_ZOOM
                    ? "PULSA LA TECLA DE ZOOM"
                    : "Tecla de zoom: " + keyName(c.reconZoomKey));
            case JOURNEYMAP -> Component.literal("Waypoint de Recon: " + yesNo(c.journeyMapReconWaypoint));
            default -> Component.literal("");
        };
    }

    private Component tertiaryLabel() {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case LOOT_ESP -> Component.literal("Stack mínimo: " + c.lootEspMinStack);
            case SMART_OFFHAND -> Component.literal("Restaurar con hambre ≥ " + c.foodRestoreThreshold);
            case RECON -> Component.literal(captureTarget == CaptureTarget.RECON_WAYPOINT
                    ? "PULSA LA TECLA DE WAYPOINT"
                    : "Tecla de waypoint: " + keyName(c.reconWaypointKey));
            default -> Component.literal("");
        };
    }

    private Component actionLabel() {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case LOOT_ESP -> Component.literal(captureTarget == CaptureTarget.LOOT_TOGGLE
                    ? "PULSA LA TECLA PARA LOOT ESP"
                    : "Tecla activar/desactivar: " + keyName(c.lootEspToggleKey));
            case SMART_OFFHAND -> Component.literal("Comida elegida: " + smartFoodName(c.smartOffhandFoodId));
            case RECON -> Component.literal("Alcance del raycast: " + c.reconRange + " m");
            case JOURNEYMAP -> Component.literal("Limpiar waypoint táctico de Lclient");
            default -> Component.literal("");
        };
    }

    private Component quaternaryLabel() {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case LOOT_ESP -> Component.literal("Marcador vertical tras paredes: " + yesNo(c.lootEspBeacon));
            case SMART_OFFHAND -> Component.literal("Si falta la elegida, usar AUTO: " + yesNo(c.smartOffhandFallbackToAuto));
            case RECON -> Component.literal("Zoom guardado: FOV " + c.reconZoomFov + " (rueda en vivo)");
            default -> Component.literal("");
        };
    }

    private void secondaryAction() {
        LClientConfig c = LClientConfig.get();
        switch (module) {
            case LOOT_ESP -> c.lootEspRange = cycle(c.lootEspRange, 32, 64, 96, 128, 160, 192);
            case SMART_OFFHAND -> c.foodThreshold = cycle(c.foodThreshold, 8, 10, 12, 14, 16);
            case RECON -> captureTarget = CaptureTarget.RECON_ZOOM;
            case JOURNEYMAP -> {
                c.journeyMapReconWaypoint = !c.journeyMapReconWaypoint;
                if (!c.journeyMapReconWaypoint) JourneyMapBridge.clearTacticalWaypoints();
            }
            default -> { }
        }
        c.save();
        refreshLabels();
    }

    private void tertiaryAction() {
        LClientConfig c = LClientConfig.get();
        switch (module) {
            case LOOT_ESP -> c.lootEspMinStack = cycle(c.lootEspMinStack, 1, 2, 4, 8, 16, 32);
            case SMART_OFFHAND -> c.foodRestoreThreshold = cycle(c.foodRestoreThreshold, 16, 18, 20);
            case RECON -> captureTarget = CaptureTarget.RECON_WAYPOINT;
            default -> { }
        }
        c.save();
        refreshLabels();
    }

    private void action() {
        LClientConfig c = LClientConfig.get();
        if (module == LClientWheelScreen.Module.LOOT_ESP) {
            captureTarget = CaptureTarget.LOOT_TOGGLE;
        } else if (module == LClientWheelScreen.Module.SMART_OFFHAND) {
            if (minecraft != null) minecraft.setScreen(new FoodSelectionScreen(this));
            return;
        } else if (module == LClientWheelScreen.Module.RECON) {
            c.reconRange = cycle(c.reconRange, 128, 192, 256, 384, 512);
        } else if (module == LClientWheelScreen.Module.JOURNEYMAP) {
            boolean cleared = JourneyMapBridge.clearTacticalWaypoints();
            showFeedback(cleared ? "Waypoint de Lclient eliminado." : JourneyMapBridge.getStatusText());
        }
        c.save();
        refreshLabels();
    }

    private void quaternaryAction() {
        LClientConfig c = LClientConfig.get();
        if (module == LClientWheelScreen.Module.LOOT_ESP) {
            c.lootEspBeacon = !c.lootEspBeacon;
        } else if (module == LClientWheelScreen.Module.SMART_OFFHAND) {
            c.smartOffhandFallbackToAuto = !c.smartOffhandFallbackToAuto;
        } else if (module == LClientWheelScreen.Module.RECON) {
            c.reconZoomFov = cycle(c.reconZoomFov, 10, 16, 24, 32, 40, 50);
        } else {
            return;
        }
        c.save();
        refreshLabels();
    }

    private void refreshLabels() {
        if (enabledButton != null) enabledButton.setMessage(enabledLabel());
        if (secondaryButton != null) secondaryButton.setMessage(secondaryLabel());
        if (tertiaryButton != null) tertiaryButton.setMessage(tertiaryLabel());
        if (actionButton != null) actionButton.setMessage(actionLabel());
        if (quaternaryButton != null) quaternaryButton.setMessage(quaternaryLabel());
        updateVisibility();
    }

    private void updateVisibility() {
        if (secondaryButton == null || tertiaryButton == null || actionButton == null || quaternaryButton == null) return;
        secondaryButton.visible = module != LClientWheelScreen.Module.COPYL;
        tertiaryButton.visible = module == LClientWheelScreen.Module.LOOT_ESP
                || module == LClientWheelScreen.Module.SMART_OFFHAND
                || module == LClientWheelScreen.Module.RECON;
        actionButton.visible = module == LClientWheelScreen.Module.LOOT_ESP
                || module == LClientWheelScreen.Module.SMART_OFFHAND
                || module == LClientWheelScreen.Module.RECON
                || module == LClientWheelScreen.Module.JOURNEYMAP;
        quaternaryButton.visible = module == LClientWheelScreen.Module.LOOT_ESP
                || module == LClientWheelScreen.Module.SMART_OFFHAND
                || module == LClientWheelScreen.Module.RECON;
    }

    private static int cycle(int current, int... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return values[0];
    }

    private static String yesNo(boolean value) {
        return value ? "SÍ" : "NO";
    }

    private String keyName(int key) {
        return key < 0 ? "Sin asignar" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }

    private static String smartFoodName(String idText) {
        if (idText == null || idText.isBlank()) return "AUTO";
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null) return idText;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) return idText;
        try {
            String name = new ItemStack(item).getHoverName().getString();
            return name == null || name.isBlank() ? idText : name;
        } catch (RuntimeException | LinkageError ignored) {
            return idText;
        }
    }

    private boolean isEnabled() {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case COPYL -> c.quickMessages;
            case LOOT_ESP -> c.lootEsp;
            case SMART_OFFHAND -> c.smartOffhand;
            case RECON -> c.recon;
            case JOURNEYMAP -> c.journeyMap;
        };
    }

    private void toggleEnabled() {
        LClientConfig c = LClientConfig.get();
        switch (module) {
            case COPYL -> c.quickMessages = !c.quickMessages;
            case LOOT_ESP -> {
                c.lootEsp = !c.lootEsp;
                if (!c.lootEsp) LootEspRenderer.clearCache();
            }
            case SMART_OFFHAND -> c.smartOffhand = !c.smartOffhand;
            case RECON -> {
                c.recon = !c.recon;
                if (!c.recon) JourneyMapBridge.clearTacticalWaypoints();
            }
            case JOURNEYMAP -> {
                c.journeyMap = !c.journeyMap;
                if (!c.journeyMap) JourneyMapBridge.clearTacticalWaypoints();
            }
        }
        c.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureTarget != CaptureTarget.NONE) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                captureTarget = CaptureTarget.NONE;
                refreshLabels();
                return true;
            }

            int newKey = (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) ? -1 : keyCode;
            LClientConfig config = LClientConfig.get();

            if (newKey >= 0 && conflictsWithReservedKey(newKey, config)) {
                showFeedback("Esa tecla ya está reservada por otro control de Lclient.");
                captureTarget = CaptureTarget.NONE;
                refreshLabels();
                return true;
            }

            if (newKey >= 0) clearCopyLConflict(newKey);

            if (captureTarget == CaptureTarget.LOOT_TOGGLE) config.lootEspToggleKey = newKey;
            else if (captureTarget == CaptureTarget.RECON_ZOOM) config.reconZoomKey = newKey;
            else if (captureTarget == CaptureTarget.RECON_WAYPOINT) config.reconWaypointKey = newKey;
            config.save();
            captureTarget = CaptureTarget.NONE;
            refreshLabels();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean conflictsWithReservedKey(int newKey, LClientConfig config) {
        if (newKey == config.wheelKey) return true;
        return switch (captureTarget) {
            case LOOT_TOGGLE -> newKey == config.reconZoomKey || newKey == config.reconWaypointKey;
            case RECON_ZOOM -> newKey == config.lootEspToggleKey || newKey == config.reconWaypointKey;
            case RECON_WAYPOINT -> newKey == config.lootEspToggleKey || newKey == config.reconZoomKey;
            default -> false;
        };
    }

    private static void clearCopyLConflict(int newKey) {
        MessageConfig messages = MessageConfig.getInstance();
        boolean changed = false;
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            if (messages.getKeyCode(i) == newKey) {
                messages.setKeyCode(i, -1);
                changed = true;
            }
        }
        if (changed) messages.save();
    }

    private void showFeedback(String text) {
        feedback = text == null ? "" : text;
        feedbackUntil = System.currentTimeMillis() + 3600L;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        boolean compact = height < 250;
        boolean tight = height < 200;
        int maxTextWidth = Math.max(160, width - 24);

        graphics.drawCenteredString(font, module.title, width / 2, tight ? 5 : 10, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                font.plainSubstrByWidth(module.subtitle, maxTextWidth),
                width / 2,
                tight ? 17 : 24,
                0xFFAAB7C4);

        if (!tight) {
            String info = switch (module) {
                case LOOT_ESP -> "X-ray propio; sólo items cargados por el cliente.";
                case SMART_OFFHAND -> "AUTO usa comida segura; selección exacta respeta tu elección.";
                case RECON -> "Mantén Zoom; rueda ajusta; waypoint usa el raycast largo.";
                case JOURNEYMAP -> JourneyMapBridge.getStatusText();
                case COPYL -> "Los mensajes rápidos se editan desde el sector CopyL de la ruleta.";
            };
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(info, maxTextWidth),
                    width / 2,
                    compact ? 37 : 60,
                    module == LClientWheelScreen.Module.JOURNEYMAP && !JourneyMapBridge.isReady()
                            ? 0xFFFFB28A
                            : 0xFF8193A4);
        }

        if (!feedback.isBlank() && System.currentTimeMillis() <= feedbackUntil) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(feedback, maxTextWidth),
                    width / 2,
                    height - 10,
                    0xFFFFB28A);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        captureTarget = CaptureTarget.NONE;
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
