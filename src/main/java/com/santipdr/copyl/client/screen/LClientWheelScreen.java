package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.LClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class LClientWheelScreen extends Screen {
    public enum Module {
        COPYL("CopyL", "Mensajes rápidos"),
        SOUND_RADAR("Sound Radar", "Dirección y distancia de sonidos"),
        LOOT_ESP("Loot ESP", "Resalta objetos tirados"),
        COMBAT("Combat + Notificaciones", "Objetivo, golpes y avisos"),
        SMART_OFFHAND("Smart Offhand", "Comida temporal en la offhand"),
        ENTITY_ALERTS("Entity Alerts", "Avisa nuevas entidades cercanas"),
        RECON("Advanced Recon", "Coordenadas y marcado táctico"),
        JOURNEYMAP("JourneyMap+", "Waypoints tácticos integrados");

        public final String title;
        public final String subtitle;
        Module(String title, String subtitle) { this.title = title; this.subtitle = subtitle; }
    }

    private final Screen parent;
    private Module selected;

    public LClientWheelScreen(Screen parent) {
        super(Component.literal("Lclient"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x99000000);
        int cx = width / 2;
        int cy = height / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        selected = dist < 34 ? null : moduleFromAngle(Math.atan2(dy, dx));

        graphics.fill(cx - 35, cy - 35, cx + 35, cy + 35, 0xE010151D);
        graphics.drawCenteredString(font, "LCLIENT", cx, cy - 6, 0xFFF2F6FA);
        graphics.drawCenteredString(font, selected == null ? "Ruleta" : selected.title, cx, cy + 8, 0xFF8CCBFF);

        Module[] modules = Module.values();
        for (int i = 0; i < modules.length; i++) {
            double angle = Math.toRadians(i * 45.0D);
            int x = cx + (int) (Math.cos(angle) * 126);
            int y = cy + (int) (Math.sin(angle) * 86);
            Module module = modules[i];
            boolean active = selected == module;
            int cardW = Math.max(92, font.width(module.title) + 16);
            int color = active ? 0xE0264159 : 0xC0121820;
            graphics.fill(x - cardW / 2, y - 13, x + cardW / 2, y + 13, color);
            graphics.drawCenteredString(font, module.title, x, y - 4, active ? 0xFFFFFFFF : 0xFFB8C4D0);
        }

        if (selected != null) {
            graphics.drawCenteredString(font, selected.subtitle, cx, height - 28, 0xFFB8C4D0);
            graphics.drawCenteredString(font, "Click: configurar", cx, height - 16, 0xFF7F91A3);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Module moduleFromAngle(double angle) {
        double normalized = (angle + Math.PI * 2.0D) % (Math.PI * 2.0D);
        int index = (int) Math.floor((normalized + Math.PI / 8.0D) / (Math.PI / 4.0D)) & 7;
        return Module.values()[index];
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selected != null && minecraft != null) {
            if (selected == Module.COPYL) minecraft.setScreen(new MessageEditorScreen(this));
            else minecraft.setScreen(new ModuleSettingsScreen(this, selected));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
