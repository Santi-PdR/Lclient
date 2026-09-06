package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.LClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LClientWheelScreen extends Screen {
    public enum Module {
        COPYL("CopyL", "Mensajes y comandos rápidos"),
        LOOT_ESP("Loot ESP", "Cajas visibles a través de bloques para objetos tirados"),
        SMART_OFFHAND("Smart Offhand", "Comida temporal y restauración segura de la offhand"),
        RECON("Advanced Recon", "Zoom táctico variable + raycast largo + waypoint"),
        JOURNEYMAP("JourneyMap+", "Integración táctica opcional para Recon");

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
        graphics.fill(0, 0, width, height, 0xB0000000);

        int cx = width / 2;
        int cy = height / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        selected = distance < 54.0D || distance > 215.0D ? null : moduleFromAngle(Math.atan2(dy, dx));

        renderCenter(graphics, cx, cy);
        renderModules(graphics, cx, cy);

        if (selected != null) {
            graphics.drawCenteredString(font, selected.subtitle, cx, height - 36, 0xFFD1DCE6);
            graphics.drawCenteredString(font,
                    "Click izquierdo: abrir  ·  Click derecho: activar/desactivar",
                    cx,
                    height - 20,
                    0xFF8293A3);
        } else {
            graphics.drawCenteredString(font, "Esc: cerrar", cx, height - 20, 0xFF71808E);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCenter(GuiGraphics graphics, int cx, int cy) {
        int size = 48;
        graphics.fill(cx - size - 1, cy - size - 1, cx + size + 1, cy + size + 1, 0x804B6A83);
        graphics.fill(cx - size, cy - size, cx + size, cy + size, 0xF00D131A);
        graphics.fill(cx - size, cy - size, cx + size, cy - size + 2, 0xFF6FC2FF);
        graphics.drawCenteredString(font, "LCLIENT", cx, cy - 17, 0xFFF4F8FB);

        if (selected == null) {
            graphics.drawCenteredString(font, "5 módulos", cx, cy + 1, 0xFF9EB0C0);
            graphics.drawCenteredString(font, "sin relleno inútil", cx, cy + 14, 0xFF718393);
        } else {
            boolean enabled = isEnabled(selected);
            graphics.drawCenteredString(font, selected.title, cx, cy, 0xFF9ED8FF);
            graphics.drawCenteredString(font,
                    enabled ? "ACTIVADO" : "DESACTIVADO",
                    cx,
                    cy + 15,
                    enabled ? 0xFF8DE5A3 : 0xFF9AA5AF);
        }
    }

    private void renderModules(GuiGraphics graphics, int cx, int cy) {
        Module[] modules = Module.values();
        double step = Math.PI * 2.0D / modules.length;

        for (int i = 0; i < modules.length; i++) {
            double angle = -Math.PI / 2.0D + i * step;
            int x = cx + (int) Math.round(Math.cos(angle) * 158.0D);
            int y = cy + (int) Math.round(Math.sin(angle) * 102.0D);
            Module module = modules[i];
            boolean hovered = selected == module;
            boolean enabled = isEnabled(module);

            int cardW = Math.max(112, font.width(module.title) + 34);
            int cardH = 28;
            int border = hovered ? 0xFF6FC2FF : enabled ? 0x80506D83 : 0x5038424C;
            int fill = hovered ? 0xF0223B50 : enabled ? 0xE0121B24 : 0xC00D1218;

            graphics.fill(x - cardW / 2 - 1, y - cardH / 2 - 1,
                    x + cardW / 2 + 1, y + cardH / 2 + 1, border);
            graphics.fill(x - cardW / 2, y - cardH / 2,
                    x + cardW / 2, y + cardH / 2, fill);

            graphics.fill(x - cardW / 2 + 7, y - 2, x - cardW / 2 + 11, y + 2,
                    enabled ? 0xFF75E08B : 0xFF626D77);
            graphics.drawCenteredString(font,
                    module.title,
                    x + 5,
                    y - 4,
                    hovered ? 0xFFFFFFFF : enabled ? 0xFFDCE6EE : 0xFF89949E);
        }
    }

    private Module moduleFromAngle(double angle) {
        Module[] modules = Module.values();
        double step = Math.PI * 2.0D / modules.length;
        double normalized = (angle + Math.PI / 2.0D + Math.PI * 2.0D) % (Math.PI * 2.0D);
        int index = (int) Math.floor((normalized + step / 2.0D) / step) % modules.length;
        return modules[index];
    }

    private boolean isEnabled(Module module) {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case COPYL -> c.quickMessages;
            case LOOT_ESP -> c.lootEsp;
            case SMART_OFFHAND -> c.smartOffhand;
            case RECON -> c.recon;
            case JOURNEYMAP -> c.journeyMap;
        };
    }

    private void toggle(Module module) {
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selected == null || minecraft == null) return super.mouseClicked(mouseX, mouseY, button);

        if (button == 1) {
            toggle(selected);
            return true;
        }
        if (button == 0) {
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
