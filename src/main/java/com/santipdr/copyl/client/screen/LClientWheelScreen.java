package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.LClientConfig;
import com.santipdr.copyl.client.MessageConfig;
import com.santipdr.copyl.client.integration.JourneyMapBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class LClientWheelScreen extends Screen {
    public enum Module {
        COPYL("CopyL", "Mensajes y comandos rápidos con slots nombrados"),
        LOOT_ESP("Loot ESP", "X-ray de objetos cargados + toggle instantáneo"),
        SMART_OFFHAND("Smart Offhand", "Comida elegible y restauración segura de offhand"),
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
            graphics.drawCenteredString(font, selected.subtitle, cx, height - 42, 0xFFD1DCE6);
            graphics.drawCenteredString(font, statusLine(selected), cx, height - 29, 0xFF91A8BA);
            graphics.drawCenteredString(font,
                    "Click izquierdo: configurar  ·  Click derecho: activar/desactivar",
                    cx,
                    height - 16,
                    0xFF8293A3);
        } else {
            graphics.drawCenteredString(font, "Esc: cerrar", cx, height - 20, 0xFF71808E);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCenter(GuiGraphics graphics, int cx, int cy) {
        int size = 50;
        graphics.fill(cx - size - 1, cy - size - 1, cx + size + 1, cy + size + 1, 0x804B6A83);
        graphics.fill(cx - size, cy - size, cx + size, cy + size, 0xF00D131A);
        graphics.fill(cx - size, cy - size, cx + size, cy - size + 2, 0xFF6FC2FF);
        graphics.drawCenteredString(font, "LCLIENT", cx, cy - 20, 0xFFF4F8FB);

        if (selected == null) {
            graphics.drawCenteredString(font, "5 módulos", cx, cy, 0xFF9EB0C0);
            graphics.drawCenteredString(font, "client-side", cx, cy + 14, 0xFF718393);
        } else {
            boolean enabled = isEnabled(selected);
            graphics.drawCenteredString(font, selected.title, cx, cy - 2, 0xFF9ED8FF);
            graphics.drawCenteredString(font,
                    enabled ? "ACTIVADO" : "DESACTIVADO",
                    cx,
                    cy + 13,
                    enabled ? 0xFF8DE5A3 : 0xFF9AA5AF);
            String compact = compactStatus(selected);
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(compact, 92),
                    cx,
                    cy + 27,
                    0xFF7F94A6);
        }
    }

    private void renderModules(GuiGraphics graphics, int cx, int cy) {
        Module[] modules = Module.values();
        double step = Math.PI * 2.0D / modules.length;

        for (int i = 0; i < modules.length; i++) {
            double angle = -Math.PI / 2.0D + i * step;
            int x = cx + (int) Math.round(Math.cos(angle) * 160.0D);
            int y = cy + (int) Math.round(Math.sin(angle) * 104.0D);
            Module module = modules[i];
            boolean hovered = selected == module;
            boolean enabled = isEnabled(module);

            int cardW = Math.max(116, font.width(module.title) + 36);
            int cardH = 30;
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

    private String compactStatus(Module module) {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case COPYL -> configuredMessages() + "/10 listos";
            case LOOT_ESP -> keyName(c.lootEspToggleKey) + " · " + c.lootEspRange + "m";
            case SMART_OFFHAND -> foodName(c.smartOffhandFoodId);
            case RECON -> keyName(c.reconZoomKey) + " · FOV " + c.reconZoomFov;
            case JOURNEYMAP -> JourneyMapBridge.isReady() ? "API lista" : "opcional";
        };
    }

    private String statusLine(Module module) {
        LClientConfig c = LClientConfig.get();
        return switch (module) {
            case COPYL -> configuredMessages() + " mensajes configurados · " + boundMessages() + " con tecla";
            case LOOT_ESP -> "Toggle " + keyName(c.lootEspToggleKey) + " · " + c.lootEspRange + "m · stack ≥ " + c.lootEspMinStack;
            case SMART_OFFHAND -> "Comida: " + foodName(c.smartOffhandFoodId)
                    + (c.smartOffhandFallbackToAuto ? " · fallback AUTO" : " · selección estricta");
            case RECON -> "Zoom " + keyName(c.reconZoomKey) + " · waypoint " + keyName(c.reconWaypointKey)
                    + " · raycast " + c.reconRange + "m";
            case JOURNEYMAP -> JourneyMapBridge.isReady()
                    ? "JourneyMap conectado y listo"
                    : JourneyMapBridge.isInstalled() ? "JourneyMap iniciando" : "JourneyMap no instalado";
        };
    }

    private int configuredMessages() {
        MessageConfig config = MessageConfig.getInstance();
        int count = 0;
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) if (!config.getMessage(i).isBlank()) count++;
        return count;
    }

    private int boundMessages() {
        MessageConfig config = MessageConfig.getInstance();
        int count = 0;
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) if (config.getKeyCode(i) >= 0) count++;
        return count;
    }

    private static String keyName(int key) {
        return key < 0 ? "—" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }

    private static String foodName(String idText) {
        if (idText == null || idText.isBlank()) return "AUTO";
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null) return idText;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null ? idText : new ItemStack(item).getHoverName().getString();
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
