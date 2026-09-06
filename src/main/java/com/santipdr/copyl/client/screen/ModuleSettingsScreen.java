package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.LClientConfig;
import com.santipdr.copyl.client.LClientHud;
import com.santipdr.copyl.client.integration.LClientJourneyMapPlugin;
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
    private boolean captureReconKey;

    public ModuleSettingsScreen(Screen parent, LClientWheelScreen.Module module) {
        super(Component.literal(module.title));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), b -> {
            toggleEnabled();
            b.setMessage(enabledLabel());
        }).bounds(cx - 110, height / 2 - 26, 220, 20).build());

        secondaryButton = addRenderableWidget(Button.builder(secondaryLabel(), b -> secondaryAction())
                .bounds(cx - 110, height / 2 + 2, 220, 20).build());
        secondaryButton.visible = module != LClientWheelScreen.Module.COMBAT;

        addRenderableWidget(Button.builder(Component.literal("Volver a la ruleta"), b -> onClose())
                .bounds(cx - 110, height / 2 + 38, 220, 20).build());
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
            case JOURNEYMAP -> Component.literal("Waypoint del último atacante: " + (c.journeyMapAttackerWaypoint ? "SÍ" : "NO"));
            default -> Component.literal("Configuración");
        };
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
        secondaryButton.setMessage(secondaryLabel());
    }

    private static int cycle(int current, int... values) {
        for (int i = 0; i < values.length; i++) if (values[i] == current) return values[(i + 1) % values.length];
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
            } else {
                LClientConfig.get().reconMarkKey = keyCode;
                LClientConfig.get().save();
                captureReconKey = false;
            }
            secondaryButton.setMessage(secondaryLabel());
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
            graphics.drawCenteredString(font, "Muestra objetivo, últimas entidades que te golpearon y coordenadas.", width / 2, height / 2 + 4, 0xFF8FA0B0);
        } else if (module == LClientWheelScreen.Module.JOURNEYMAP) {
            graphics.drawCenteredString(font, LClientJourneyMapPlugin.isReady() ? "JourneyMap API conectada" : "JourneyMap API aún no inicializada", width / 2, height / 2 + 29, 0xFF8FA0B0);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
