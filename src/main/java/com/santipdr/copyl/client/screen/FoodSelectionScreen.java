package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.LClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Picker for Smart Offhand's preferred edible item. */
public final class FoodSelectionScreen extends Screen {
    private static final int PAGE_SIZE = 8;

    private final Screen parent;
    private final List<FoodChoice> foods = new ArrayList<>();
    private EditBox idField;
    private int page;
    private String feedback = "";

    public FoodSelectionScreen(Screen parent) {
        super(Component.literal("Smart Offhand — Comida"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildFoods();
        int cx = width / 2;
        int startY = 58;

        addRenderableWidget(Button.builder(Component.literal("AUTO · elegir automáticamente"), b -> {
            LClientConfig config = LClientConfig.get();
            config.smartOffhandFoodId = "";
            config.save();
            feedback = "Selección: AUTO";
            rebuildScreen();
        }).bounds(cx - 150, startY, 300, 20).build());

        int from = page * PAGE_SIZE;
        int to = Math.min(foods.size(), from + PAGE_SIZE);
        for (int i = from; i < to; i++) {
            FoodChoice choice = foods.get(i);
            int local = i - from;
            int column = local % 2;
            int row = local / 2;
            int x = cx - 150 + column * 154;
            int y = startY + 29 + row * 26;
            boolean selected = choice.id.equals(LClientConfig.get().smartOffhandFoodId);
            String prefix = selected ? "✓ " : "";
            String label = prefix + choice.name + " · x" + choice.count;
            addRenderableWidget(Button.builder(Component.literal(label), b -> select(choice.id))
                    .bounds(x, y, 146, 20).build());
        }

        int pagerY = startY + 29 + 4 * 26 + 2;
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (page > 0) {
                page--;
                rebuildScreen();
            }
        }).bounds(cx - 150, pagerY, 46, 20).build()).active = page > 0;

        int maxPage = foods.isEmpty() ? 0 : (foods.size() - 1) / PAGE_SIZE;
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if (page < maxPage) {
                page++;
                rebuildScreen();
            }
        }).bounds(cx + 104, pagerY, 46, 20).build()).active = page < maxPage;

        idField = new EditBox(font, cx - 150, pagerY + 31, 214, 20, Component.literal("ID de comida"));
        idField.setMaxLength(128);
        idField.setHint(Component.literal("minecraft:golden_carrot"));
        idField.setValue(LClientConfig.get().smartOffhandFoodId);
        addRenderableWidget(idField);

        addRenderableWidget(Button.builder(Component.literal("Aplicar ID"), b -> applyManualId())
                .bounds(cx + 70, pagerY + 31, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Usar mano principal"), b -> selectMainHand())
                .bounds(cx - 150, pagerY + 58, 146, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> onClose())
                .bounds(cx + 4, pagerY + 58, 146, 20).build());
    }

    private void rebuildFoods() {
        foods.clear();
        if (minecraft == null || minecraft.player == null) return;

        Map<ResourceLocation, FoodChoice> unique = new LinkedHashMap<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.isEdible()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) continue;
            FoodChoice existing = unique.get(id);
            if (existing == null) {
                unique.put(id, new FoodChoice(id.toString(), stack.getHoverName().getString(), stack.getCount()));
            } else {
                existing.count += stack.getCount();
            }
        }
        foods.addAll(unique.values());
    }

    private void select(String id) {
        LClientConfig config = LClientConfig.get();
        config.smartOffhandFoodId = id;
        config.save();
        feedback = "Comida elegida: " + displayName(id);
        rebuildScreen();
    }

    private void selectMainHand() {
        if (minecraft == null || minecraft.player == null) return;
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty() || !stack.isEdible()) {
            feedback = "El item de tu mano principal no es comida.";
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) select(id.toString());
    }

    private void applyManualId() {
        String value = idField == null ? "" : idField.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        if (value.isEmpty()) {
            select("");
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            feedback = "ID inválido.";
            return;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || !new ItemStack(item).isEdible()) {
            feedback = "Ese ID no corresponde a una comida cargada.";
            return;
        }
        select(id.toString());
    }

    private String displayName(String idText) {
        if (idText == null || idText.isBlank()) return "AUTO";
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null) return idText;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null ? idText : new ItemStack(item).getHoverName().getString();
    }

    private void rebuildScreen() {
        if (minecraft != null) minecraft.setScreen(new FoodSelectionScreen(parent));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        String selected = LClientConfig.get().smartOffhandFoodId;
        graphics.drawCenteredString(font,
                "Actual: " + displayName(selected) + " · elige una detectada, AUTO o un ID modded",
                width / 2,
                37,
                0xFFA9BAC9);
        if (foods.isEmpty()) {
            graphics.drawCenteredString(font,
                    "No hay comidas detectadas en los 36 slots del inventario.",
                    width / 2,
                    87,
                    0xFF8C9DAC);
        }
        if (!feedback.isBlank()) {
            graphics.drawCenteredString(font, feedback, width / 2, height - 20, 0xFF8FD8A0);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private static final class FoodChoice {
        private final String id;
        private final String name;
        private int count;

        private FoodChoice(String id, String name, int count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }
    }
}
