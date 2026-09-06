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
    private enum CaptureTarget {
        NONE,
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

    public ModuleSettingsScreen(Screen parent, LClientWheelScreen.Module module) {
        super(Component.literal(module.title));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int startY = height / 2 - 66;

        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), b -> {
            toggleEnabled();
            refreshLabels();
        }).bounds(cx - 120, startY, 240, 20).build());

        secondaryButton = addRenderableWidget(Button.builder(secondaryLabel(), b -> secondaryAction())
                .bounds(cx - 120, startY + 27, 240, 20).build());
        tertiaryButton = addRenderableWidget(Button.builder(tertiaryLabel(), b -> tertiaryAction())
                .bounds(cx - 120, startY + 54, 240, 20).build());
        actionButton = addRenderableWidget(Button.builder(actionLabel(), b -> action())
                .bounds(cx - 120, startY + 81, 240, 20).build());
        quaternaryButton = addRenderableWidget(Button.builder(quaternaryLabel(), b -> quaternaryAction())
                .bounds(cx - 120, startY + 108, 240, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Volver a la ruleta"), b -> onClose())
                .bounds(cx - 120, startY + 142, 240, 20).build());

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
            case RECON -> Component.literal("Alcance del raycast: " + c.reconRange + " m");
            case JOURNEYMAP -> Component.literal("Limpiar waypoint táctico de Lclient");
            default -> Component.literal("");
        };
    }

    private Component quaternaryLabel() {
        LClientConfig c = LClientConfig.get();
        return module == LClientWheelScreen.Module.RECON
                ? Component.literal("Zoom guardado: FOV " + c.reconZoomFov + " (la rueda lo cambia en vivo)")
                : Component.literal("");
    }

    private void secondaryAction() {
        LClientConfig c = LClientConfig.get();
        switch (module) {
            case LOOT_ESP -> c.lootEspRange = cycle(c.lootEspRange, 32, 64, 96, 128, 160, 192);
            case SMART_OFFHAND -> c.foodThreshold = cycle(c.foodThreshold, 8, 12, 14, 16);
            case RECON -> captureTarget = CaptureTarget.RECON_ZOOM;
            case JOURNEYMAP -> c.journeyMapReconWaypoint = !c.journeyMapReconWaypoint;
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
        if (module == LClientWheelScreen.Module.RECON) {
            c.reconRange = cycle(c.reconRange, 128, 192, 256, 384, 512);
            c.save();
        } else if (module == LClientWheelScreen.Module.JOURNEYMAP) {
            JourneyMapBridge.clearTacticalWaypoints();
        }
        refreshLabels();
    }

    private void quaternaryAction() {
        if (module != LClientWheelScreen.Module.RECON) return;
        LClientConfig c = LClientConfig.get();
        c.reconZoomFov = cycle(c.reconZoomFov, 10, 16, 24, 32, 40, 50);
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
        actionButton.visible = module == LClientWheelScreen.Module.RECON
                || module == LClientWheelScreen.Module.JOURNEYMAP;
        quaternaryButton.visible = module == LClientWheelScreen.Module.RECON;
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
            case LOOT_ESP -> c.lootEsp = !c.lootEsp;
            case SMART_OFFHAND -> c.smartOffhand = !c.smartOffhand;
            case RECON -> c.recon = !c.recon;
            case JOURNEYMAP -> c.journeyMap = !c.journeyMap;
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
            if (captureTarget == CaptureTarget.RECON_ZOOM) config.reconZoomKey = newKey;
            else config.reconWaypointKey = newKey;
            config.save();
            captureTarget = CaptureTarget.NONE;
            refreshLabels();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, module.title, width / 2, 26, 0xFFFFFFFF);
        graphics.drawCenteredString(font, module.subtitle, width / 2, 42, 0xFFAAB7C4);

        String info = switch (module) {
            case LOOT_ESP -> "ESP propio de Lclient: cajas sin depth-test; no depende del glow vanilla.";
            case SMART_OFFHAND -> "Sólo restaura si el slot de respaldo sigue siendo seguro; no pisa cambios manuales.";
            case RECON -> "Mantén Zoom. Rueda arriba = más zoom; abajo = menos. Waypoint usa el raycast largo real.";
            case JOURNEYMAP -> JourneyMapBridge.isReady()
                    ? "JourneyMap 5.10.x conectado. Recon puede crear su waypoint temporal."
                    : JourneyMapBridge.isInstalled()
                    ? "JourneyMap detectado; esperando a que su API termine de iniciar."
                    : "JourneyMap no está instalado; Lclient funciona igualmente.";
            case COPYL -> "Los mensajes rápidos se editan desde el sector CopyL de la ruleta.";
        };
        graphics.drawCenteredString(font, info, width / 2, 60, 0xFF8193A4);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        captureTarget = CaptureTarget.NONE;
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
