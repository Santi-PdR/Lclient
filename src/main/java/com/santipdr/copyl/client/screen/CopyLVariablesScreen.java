package com.santipdr.copyl.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Built-in reference for CopyL variables so users do not need external docs. */
public final class CopyLVariablesScreen extends Screen {
    private record Entry(String token, String meaning) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry("{pos}", "Tus coordenadas X Y Z"),
            new Entry("{x} {y} {z}", "Tus coordenadas por separado"),
            new Entry("{dim}", "Dimensión actual"),
            new Entry("{hp}", "Tu vida redondeada"),
            new Entry("{food}", "Tu nivel de hambre"),
            new Entry("{name}", "Tu nombre"),
            new Entry("{yaw} {pitch}", "Dirección horizontal / vertical de tu cámara"),
            new Entry("{target}", "Nombre del objetivo actual"),
            new Entry("{targettype}", "ID/tipo del objetivo"),
            new Entry("{targetdist}", "Distancia al objetivo"),
            new Entry("{targetpos}", "Coordenadas del objetivo"),
            new Entry("{targetx} {targety} {targetz}", "Coordenadas del objetivo por separado"),
            new Entry("{targethp} {targetmaxhp}", "Vida actual / máxima conocida"),
            new Entry("{targetspeed}", "Velocidad del objetivo en m/s"),
            new Entry("{targetdy}", "Diferencia vertical respecto a vos"),
            new Entry("{targetbearing}", "Rumbo cardinal del objetivo"),
            new Entry("{targetmotion}", "Acercándose, alejándose, izquierda, derecha o estable"),
            new Entry("{targetitem}", "Item visible en mano/offhand del objetivo")
    );

    private final Screen parent;
    private int page;

    public CopyLVariablesScreen(Screen parent) {
        super(Component.literal("CopyL — Variables"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int pageSize = pageSize();
        int maxPage = Math.max(0, (ENTRIES.size() - 1) / pageSize);
        page = Math.max(0, Math.min(page, maxPage));

        int cx = width / 2;
        int y = height - 27;
        int buttonW = Math.min(300, Math.max(140, width - 24));
        int left = cx - buttonW / 2;
        int navW = 36;
        int gap = 6;
        int middleW = Math.max(1, buttonW - navW * 2 - gap * 2);

        Button prev = addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (page > 0) {
                page--;
                rebuildWidgets();
            }
        }).bounds(left, y, navW, 20).build());
        prev.active = page > 0;

        addRenderableWidget(Button.builder(Component.literal("Volver a CopyL"), b -> onClose())
                .bounds(left + navW + gap, y, middleW, 20).build());

        Button next = addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if (page < maxPage) {
                page++;
                rebuildWidgets();
            }
        }).bounds(left + buttonW - navW, y, navW, 20).build());
        next.active = page < maxPage;
    }

    private int pageSize() {
        return Math.max(4, Math.min(9, (height - 76) / 24));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int pageSize = pageSize();
        int maxPage = Math.max(0, (ENTRIES.size() - 1) / pageSize);
        page = Math.max(0, Math.min(page, maxPage));

        graphics.drawCenteredString(font, title, width / 2, 9, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                font.plainSubstrByWidth("Durante Recon, las variables target usan el raycast largo; fuera de Recon usan la mira vanilla.", Math.max(100, width - 20)),
                width / 2,
                23,
                0xFF91A6B7);

        int contentW = Math.min(560, Math.max(140, width - 20));
        int left = (width - contentW) / 2;
        int from = page * pageSize;
        int to = Math.min(ENTRIES.size(), from + pageSize);
        int y = 43;

        for (int i = from; i < to; i++) {
            Entry entry = ENTRIES.get(i);
            graphics.fill(left, y, left + contentW, y + 19, 0xA80C1218);
            graphics.fill(left, y, left + 2, y + 19, 0xFF6FC2FF);

            int tokenW = Math.min(150, Math.max(76, contentW / 3));
            graphics.drawString(font,
                    font.plainSubstrByWidth(entry.token, tokenW - 8),
                    left + 8,
                    y + 5,
                    0xFFBDE6FF,
                    false);
            graphics.drawString(font,
                    font.plainSubstrByWidth(entry.meaning, Math.max(20, contentW - tokenW - 14)),
                    left + tokenW,
                    y + 5,
                    0xFFAAB8C5,
                    false);
            y += 24;
        }

        graphics.drawCenteredString(font,
                "Página " + (page + 1) + "/" + (maxPage + 1),
                width / 2,
                height - 40,
                0xFF718596);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
