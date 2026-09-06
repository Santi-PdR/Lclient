package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.LClientConfig;
import com.santipdr.copyl.client.integration.JourneyMapBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final LClientWheelScreen.Module module;
    private Button enabledButton;
    private Button secondaryButton;
    private Button tertiaryButton;
    private Button actionButton;
    private boolean captureReconKey;

    public ModuleSettingsScreen(Screen parent, LClientWheelScreen.Module module) {
        super(Component.literal(module.title));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int startY = height / 2 - 48;

        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), b -> {
            toggleEnabled();
            refreshLabels();
        }).bounds(cx - 110, startY, 220, 20).build());

        secondaryButton = addRenderableWidget(Button.builder(secondaryLabel(), b -> secondaryAction())
                .bounds(cx - 110, startY + 28, 220, 20).build());

        tertiaryButton = addRenderableWidget(Button.builder(tertiaryLabel(), b -> tertiaryAction())
                .bounds(cx - 110, startY + 56, 220, 20).build());

        actionButton = addRenderableWidget(Button.builder(actionLabel(), b -> action())
                .bounds(cx - 110, startY + 84, 220, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Volver a la ruleta"), b -> onClose())
                .bounds(cx - 110, startY + 120, 220, 20).build());

        updateVisibility();
    }

    private Component enabledLabel() {
        return Component.literal("Estado: " + (isEnabled() ? "ACTIVADO" : "DESACTIVADO"));
    }

    private Component secondaryLabel() {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case SOUND_RADAR -> Component.literal("Alcance: " + c.soundRadarRange + " m");
            case LOOT_ESP -> Component.literal("Alcance: " + c.lootEspRange + " m");
            case SMART_OFFHAND -> Component.literal("Mover comida con hambre ≤ " + c.foodThreshold);
            case ENTITY_ALERTS -> Component.literal("Radio de aviso: " + c.entityAlertRange + " m");
            case RECON -> Component.literal(captureReconKey ? "PULSA UNA TECLA" : "Tecla para marcar: " + keyName(c.reconMarkKey));
            case JOURNEYMAP -> Component.literal("Waypoint del atacante: " + yesNo(c.journeyMapAttackerWaypoint));
            default -> Component.literal("Configuración");
        };
    }

    private Component tertiaryLabel() {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case SMART_OFFHAND -> Component.literal("Restaurar con hambre ≥ " + c.foodRestoreThreshold);
            case ENTITY_ALERTS -> Component.literal("Filtro de chunks: " + (c.entityAlertWarmupTicks / 20.0F) + " s");
            case JOURNEYMAP -> Component.literal("Waypoint de Recon: " + yesNo(c.journeyMapReconWaypoint));
            default -> Component.literal("");
        };
    }

    private Component actionLabel() {
        return module == LClientWheelScreen.Module.JOURNEYMAP
                ? Component.literal("Limpiar waypoints tácticos de Lclient")
                : Component.literal("");
    }

    private static String yesNo(boolean value) {
        return value ? "SÍ" : "NO";
    }

    private void secondaryAction() {
        LClientConfig c = LClientConfig.get();
        switch (module) {
            case SOUND_RADAR -> c.soundRadarRange = cycle(c.soundRadarRange, 48, 72, 96, 128);
            case LOOT_ESP -> c.lootEspRange = cycle(c.lootEspRange, 32, 64, 96, 128);
            case SMART_OFFHAND -> c.foodThreshold = cycle(c.foodThreshold, 8, 12, 14, 16);
            case ENTITY_ALERTS -> c.entityAlertRange = cycle(c.entityAlertRange, 32, 48, 72, 96);
            case RECON -> captureReconKey = true;
            case JOURNEYMAP -> c.journeyMapAttackerWaypoint = !c.journeyMapAttackerWaypoint;
            default -> { }
        }
        c.save();
        refreshLabels();
    }

    private void tertiaryAction() {
        LClientConfig c = LClientConfig.get();
        switch (module) {
            case SMART_OFFHAND -> c.foodRestoreThreshold = cycle(c.foodRestoreThreshold, 16, 18, 20);
            case ENTITY_ALERTS -> c.entityAlertWarmupTicks = cycle(c.entityAlertWarmupTicks, 40, 80, 120);
            case JOURNEYMAP -> c.journeyMapReconWaypoint = !c.journeyMapReconWaypoint;
            default -> { }
        }
        c.save();
        refreshLabels();
    }

    private void action() {
        if (module == LClientWheelScreen.Module.JOURNEYMAP) {
            JourneyMapBridge.clearTacticalWaypoints();
        }
    }

    private void refreshLabels() {
        if (enabledButton != null) enabledButton.setMessage(enabledLabel());
        if (secondaryButton != null) secondaryButton.setMessage(secondaryLabel());
        if (tertiaryButton != null) tertiaryButton.setMessage(tertiaryLabel());
        if (actionButton != null) actionButton.setMessage(actionLabel());
        updateVisibility();
    }

    private void updateVisibility() {
        if (secondaryButton == null || tertiaryButton == null || actionButton == null) return;
        secondaryButton.visible = module != LClientWheelScreen.Module.COMBAT;
        tertiaryButton.visible = module == LClientWheelScreen.Module.SMART_OFFHAND
                || module == LClientWheelScreen.Module.ENTITY_ALERTS
                || module == LClientWheelScreen.Module.JOURNEYMAP;
        actionButton.visible = module == LClientWheelScreen.Module.JOURNEYMAP;
    }

    private static int cycle(int current, int... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return values[0];
    }

    private String keyName(int key) {
        return key < 0 ? "Sin asignar" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }

    private boolean isEnabled() {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case SOUND_RADAR -> c.soundRadar;
            case LOOT_ESP -> c.lootEsp;
            case COMBAT -> c.combatPanel;
            case SMART_OFFHAND -> c.smartOffhand;
            case ENTITY_ALERTS -> c.entityAlerts;
            case RECON -> c.recon;
            case JOURNEYMAP -> c.journeyMap;
            case COPYL -> c.quickMessages;
        };
    }

    private void toggleEnabled() {
        LClientConfig c = LClientConfig.get();
        switch (module) {
            case SOUND_RADAR -> c.soundRadar = !c.soundRadar;
            case LOOT_ESP -> c.lootEsp = !c.lootEsp;
            case COMBAT -> c.combatPanel = !c.combatPanel;
            case SMART_OFFHAND -> c.smartOffhand = !c.smartOffhand;
            case ENTITY_ALERTS -> c.entityAlerts = !c.entityAlerts;
            case RECON -> c.recon = !c.recon;
            case JOURNEYMAP -> c.journeyMap = !c.journeyMap;
            case COPYL -> c.quickMessages = !c.quickMessages;
        }
        c.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureReconKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                captureReconKey = false;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                LClientConfig.get().reconMarkKey = -1;
                LClientConfig.get().save();
                captureReconKey = false;
            } else {
                LClientConfig.get().reconMarkKey = keyCode;
                LClientConfig.get().save();
                captureReconKey = false;
            }
            refreshLabels();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, module.title, width / 2, 28, 0xFFFFFFFF);
        graphics.drawCenteredString(font, module.subtitle, width / 2, 44, 0xFFAAB7C4);

        if (module == LClientWheelScreen.Module.COMBAT) {
            graphics.drawCenteredString(font,
                    "Registra daño, entidad real, coordenadas del atacante y tu posición.",
                    width / 2,
                    height / 2 + 4,
                    0xFF8FA0B0);
        } else if (module == LClientWheelScreen.Module.JOURNEYMAP) {
            String status;
            if (!JourneyMapBridge.isInstalled()) status = "JourneyMap no está instalado";
            else if (JourneyMapBridge.isReady()) status = "JourneyMap 5.10.x / API 1.9 conectada";
            else status = "JourneyMap detectado; esperando inicialización de API 1.9";
            graphics.drawCenteredString(font, status, width / 2, 62, 0xFF8FA0B0);
        } else if (module == LClientWheelScreen.Module.ENTITY_ALERTS) {
            graphics.drawCenteredString(font,
                    "Ignora mobs que sólo entran con chunks nuevos; avisa apariciones en zonas ya cargadas.",
                    width / 2,
                    62,
                    0xFF8FA0B0);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
