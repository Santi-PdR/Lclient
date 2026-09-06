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
        selected = moduleAt(mouseX, mouseY, cx, cy);

        renderCenter(graphics, cx, cy);
        renderModules(graphics, cx, cy);

        int footerWidth = Math.max(140, width - 28);
        if (selected != null) {
            String subtitle = font.plainSubstrByWidth(selected.subtitle, footerWidth);
            graphics.drawCenteredString(font, subtitle, cx, height - 42, 0xFFD1DCE6);
            String line = font.plainSubstrByWidth(statusLine(selected), footerWidth);
            graphics.drawCenteredString(font,
                    line,
                    cx,
                    height - 29,
                    selected == Module.JOURNEYMAP && isEnabled(selected) && !JourneyMapBridge.isReady()
                            ? 0xFFFFB28A
                            : 0xFF91A8BA);
            String controls = width < 360
                    ? "Izq: configurar · Der: activar"
                    : "Click izquierdo: configurar  ·  Click derecho: activar/desactivar";
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(controls, footerWidth),
                    cx,
                    height - 16,
                    0xFF8293A3);
        } else {
            graphics.drawCenteredString(font, "Esc: cerrar", cx, height - 16, 0xFF71808E);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCenter(GuiGraphics graphics, int cx, int cy) {
        int size = Math.min(50, Math.max(34, Math.min(width, height) / 6));
        graphics.fill(cx - size - 1, cy - size - 1, cx + size + 1, cy + size + 1, 0x804B6A83);
        graphics.fill(cx - size, cy - size, cx + size, cy + size, 0xF00D131A);
        graphics.fill(cx - size, cy - size, cx + size, cy - size + 2, 0xFF6FC2FF);
        graphics.drawCenteredString(font, "LCLIENT", cx, cy - Math.min(20, size - 12), 0xFFF4F8FB);

        if (selected == null) {
            graphics.drawCenteredString(font, "5 módulos", cx, cy, 0xFF9EB0C0);
            if (size >= 42) graphics.drawCenteredString(font, "client-side", cx, cy + 14, 0xFF718393);
        } else {
            boolean enabled = isEnabled(selected);
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(selected.title, size * 2 - 8),
                    cx,
                    cy - 2,
                    0xFF9ED8FF);
            graphics.drawCenteredString(font,
                    enabled ? "ACTIVADO" : "DESACTIVADO",
                    cx,
                    cy + 13,
                    enabled ? 0xFF8DE5A3 : 0xFF9AA5AF);
            if (size >= 44) {
                graphics.drawCenteredString(font,
                        font.plainSubstrByWidth(compactStatus(selected), size * 2 - 8),
                        cx,
                        cy + 27,
                        0xFF7F94A6);
            }
        }
    }

    private void renderModules(GuiGraphics graphics, int cx, int cy) {
        Module[] modules = Module.values();
        double step = Math.PI * 2.0D / modules.length;
        double radiusX = radiusX();
        double radiusY = radiusY();

        for (int i = 0; i < modules.length; i++) {
            double angle = -Math.PI / 2.0D + i * step;
            int x = cx + (int) Math.round(Math.cos(angle) * radiusX);
            int y = cy + (int) Math.round(Math.sin(angle) * radiusY);
            Module module = modules[i];
            boolean hovered = selected == module;
            boolean enabled = isEnabled(module);

            int maxCardW = Math.max(88, width / 2 - 12);
            int cardW = Math.min(maxCardW, Math.max(104, font.width(module.title) + 30));
            int cardH = 30;
            int border = hovered ? 0xFF6FC2FF : enabled ? 0x80506D83 : 0x5038424C;
            int fill = hovered ? 0xF0223B50 : enabled ? 0xE0121B24 : 0xC00D1218;

            graphics.fill(x - cardW / 2 - 1, y - cardH / 2 - 1,
                    x + cardW / 2 + 1, y + cardH / 2 + 1, border);
            graphics.fill(x - cardW / 2, y - cardH / 2,
                    x + cardW / 2, y + cardH / 2, fill);

            graphics.fill(x - cardW / 2 + 7, y - 2, x - cardW / 2 + 11, y + 2,
                    enabled ? 0xFF75E08B : 0xFF626D77);
            String title = font.plainSubstrByWidth(module.title, cardW - 24);
            graphics.drawCenteredString(font,
                    title,
                    x + 5,
                    y - 4,
                    hovered ? 0xFFFFFFFF : enabled ? 0xFFDCE6EE : 0xFF89949E);
        }
    }

    /**
     * Select by normalized elliptical radius so the hit zones follow the same
     * responsive layout used to draw the module cards.
     */
    private Module moduleAt(double mouseX, double mouseY, int cx, int cy) {
        double rx = radiusX();
        double ry = radiusY();
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double nx = dx / Math.max(1.0D, rx);
        double ny = dy / Math.max(1.0D, ry);
        double radius = Math.sqrt(nx * nx + ny * ny);
        if (radius < 0.50D || radius > 1.48D) return null;
        return moduleFromAngle(Math.atan2(ny, nx));
    }

    private double radiusX() {
        return Math.max(76.0D, Math.min(160.0D, (width - 140.0D) / 2.0D));
    }

    private double radiusY() {
        return Math.max(50.0D, Math.min(104.0D, (height - 130.0D) / 2.0D));
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
            case JOURNEYMAP -> !c.journeyMap ? "apagado" : JourneyMapBridge.isReady() ? "API lista" : "revisar";
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
            case JOURNEYMAP -> !c.journeyMap ? "JourneyMap+ desactivado" : JourneyMapBridge.getStatusText();
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
            case LOOT_ESP -> {
                c.lootEsp = !c.lootEsp;
                if (!c.lootEsp) com.santipdr.copyl.client.LootEspRenderer.clearCache();
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
