package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.LClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LClientWheelScreen extends Screen {
    public enum Module {
        COPYL("CopyL", "Mensajes rápidos"),
        SOUND_RADAR("Sound Radar", "Dirección, altura y distancia de sonidos"),
        LOOT_ESP("Loot ESP", "Resalta objetos tirados sin romper glow ajeno"),
        COMBAT("Combat + Notificaciones", "Objetivo, daño, atacante y coordenadas"),
        SMART_OFFHAND("Smart Offhand", "Comida temporal con restauración segura"),
        ENTITY_ALERTS("Entity Alerts", "Detecta apariciones en zonas ya cargadas"),
        RECON("Advanced Recon", "Coordenadas y marcado táctico"),
        JOURNEYMAP("JourneyMap+", "Waypoints tácticos compatibles con 5.10.x");

        public final String title;
        public final String subtitle;

        Module(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
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
        graphics.fill(0, 0, width, height, 0xA3000000);

        int cx = width / 2;
        int cy = height / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        selected = dist < 38 || dist > 190 ? null : moduleFromAngle(Math.atan2(dy, dx));

        int centerSize = 38;
        graphics.fill(cx - centerSize, cy - centerSize, cx + centerSize, cy + centerSize, 0xE010151D);
        graphics.fill(cx - centerSize, cy - centerSize, cx + centerSize, cy - centerSize + 2, 0xFF67B7FF);
        graphics.drawCenteredString(font, "LCLIENT", cx, cy - 11, 0xFFF2F6FA);

        if (selected == null) {
            graphics.drawCenteredString(font, "Mueve el cursor", cx, cy + 5, 0xFF8FA0B0);
            graphics.drawCenteredString(font, "para elegir", cx, cy + 16, 0xFF708090);
        } else {
            boolean enabled = isEnabled(selected);
            graphics.drawCenteredString(font, selected.title, cx, cy + 3, 0xFF8CCBFF);
            graphics.drawCenteredString(font, enabled ? "ACTIVADO" : "DESACTIVADO", cx, cy + 16,
                    enabled ? 0xFF93E6A3 : 0xFF9AA4AE);
        }

        Module[] modules = Module.values();
        for (int i = 0; i < modules.length; i++) {
            double angle = Math.toRadians(i * 45.0D);
            int x = cx + (int) (Math.cos(angle) * 132);
            int y = cy + (int) (Math.sin(angle) * 92);
            Module module = modules[i];
            boolean active = selected == module;
            boolean enabled = isEnabled(module);
            int cardW = Math.max(100, font.width(module.title) + 28);

            int card = active ? 0xEE264159 : enabled ? 0xD0141C25 : 0xB810141A;
            int border = active ? 0xFF67B7FF : enabled ? 0x804D657B : 0x60323B45;
            graphics.fill(x - cardW / 2 - 1, y - 14, x + cardW / 2 + 1, y + 14, border);
            graphics.fill(x - cardW / 2, y - 13, x + cardW / 2, y + 13, card);
            graphics.fill(x - cardW / 2 + 5, y - 2, x - cardW / 2 + 8, y + 1,
                    enabled ? 0xFF70D982 : 0xFF68717A);
            graphics.drawCenteredString(font, module.title, x + 3, y - 4,
                    active ? 0xFFFFFFFF : enabled ? 0xFFD5DEE7 : 0xFF86909A);
        }

        if (selected != null) {
            graphics.drawCenteredString(font, selected.subtitle, cx, height - 31, 0xFFC2CDD7);
            graphics.drawCenteredString(font, "Click izquierdo: configurar", cx, height - 17, 0xFF7F91A3);
        } else {
            graphics.drawCenteredString(font, "Esc: cerrar", cx, height - 17, 0xFF71808E);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Module moduleFromAngle(double angle) {
        double normalized = (angle + Math.PI * 2.0D) % (Math.PI * 2.0D);
        int index = (int) Math.floor((normalized + Math.PI / 8.0D) / (Math.PI / 4.0D)) & 7;
        return Module.values()[index];
    }

    private boolean isEnabled(Module module) {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case COPYL -> c.quickMessages;
            case SOUND_RADAR -> c.soundRadar;
            case LOOT_ESP -> c.lootEsp;
            case COMBAT -> c.combatPanel;
            case SMART_OFFHAND -> c.smartOffhand;
            case ENTITY_ALERTS -> c.entityAlerts;
            case RECON -> c.recon;
            case JOURNEYMAP -> c.journeyMap;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selected != null && minecraft != null) {
            if (selected == Module.COPYL) {
                minecraft.setScreen(new MessageEditorScreen(this));
            } else {
                minecraft.setScreen(new ModuleSettingsScreen(this, selected));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
