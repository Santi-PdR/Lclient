package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.LClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Picker for Smart Offhand's preferred edible item. */
public final class FoodSelectionScreen extends Screen {
    private static final int FULL_PAGE_SIZE = 8;
    private static final int COMPACT_PAGE_SIZE = 4;

    private final Screen parent;
    private final List<FoodChoice> foods = new ArrayList<>();
    private EditBox idField;
    private int page;
    private String feedback;

    public FoodSelectionScreen(Screen parent) {
        this(parent, 0, "");
    }

    private FoodSelectionScreen(Screen parent, int page, String feedback) {
        super(Component.literal("Smart Offhand — Comida"));
        this.parent = parent;
        this.page = Math.max(0, page);
        this.feedback = feedback == null ? "" : feedback;
    }

    @Override
    protected void init() {
        rebuildFoods();
        int pageSize = pageSize();
        int maxPage = foods.isEmpty() ? 0 : (foods.size() - 1) / pageSize;
        page = Math.min(page, maxPage);

        int cx = width / 2;
        int startY = Math.max(48, Math.min(58, height / 4));
        int buttonWidth = Math.min(300, Math.max(220, width - 28));
        int left = cx - buttonWidth / 2;

        addRenderableWidget(Button.builder(Component.literal("AUTO · elegir automáticamente"), b -> {
            LClientConfig config = LClientConfig.get();
            config.smartOffhandFoodId = "";
            config.save();
            feedback = "Selección: AUTO";
            rebuildScreen();
        }).bounds(left, startY, buttonWidth, 20).build());

        int from = page * pageSize;
        int to = Math.min(foods.size(), from + pageSize);
        int gap = 8;
        int columnWidth = (buttonWidth - gap) / 2;
        int rows = (pageSize + 1) / 2;
        for (int i = from; i < to; i++) {
            FoodChoice choice = foods.get(i);
            int local = i - from;
            int column = local % 2;
            int row = local / 2;
            int x = left + column * (columnWidth + gap);
            int y = startY + 29 + row * 26;
            boolean selected = choice.id.equals(LClientConfig.get().smartOffhandFoodId);
            String prefix = selected ? "✓ " : "";
            String rawLabel = prefix + choice.name + " · x" + choice.count;
            String label = font.plainSubstrByWidth(rawLabel, columnWidth - 10);
            addRenderableWidget(Button.builder(Component.literal(label), b -> select(choice.id))
                    .bounds(x, y, columnWidth, 20).build());
        }

        int pagerY = startY + 29 + rows * 26 + 2;
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (page > 0) {
                page--;
                feedback = "";
                rebuildScreen();
            }
        }).bounds(left, pagerY, 46, 20).build()).active = page > 0;

        addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if (page < maxPage) {
                page++;
                feedback = "";
                rebuildScreen();
            }
        }).bounds(left + buttonWidth - 46, pagerY, 46, 20).build()).active = page < maxPage;

        int idButtonWidth = 80;
        idField = new EditBox(font, left, pagerY + 31, buttonWidth - idButtonWidth - 6, 20, Component.literal("ID de comida"));
        idField.setMaxLength(128);
        idField.setHint(Component.literal("minecraft:golden_carrot"));
        idField.setValue(LClientConfig.get().smartOffhandFoodId);
        addRenderableWidget(idField);

        addRenderableWidget(Button.builder(Component.literal("Aplicar ID"), b -> applyManualId())
                .bounds(left + buttonWidth - idButtonWidth, pagerY + 31, idButtonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Usar mano principal"), b -> selectMainHand())
                .bounds(left, pagerY + 58, columnWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> onClose())
                .bounds(left + columnWidth + gap, pagerY + 58, columnWidth, 20).build());
    }

    private int pageSize() {
        return height < 300 ? COMPACT_PAGE_SIZE : FULL_PAGE_SIZE;
    }

    private void rebuildFoods() {
        foods.clear();
        if (minecraft == null || minecraft.player == null) return;

        Map<ResourceLocation, FoodChoice> unique = new LinkedHashMap<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (!isUsableFoodStack(stack, minecraft.player)) continue;
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
        String selectedId = LClientConfig.get().smartOffhandFoodId;
        foods.sort(Comparator
                .comparing((FoodChoice choice) -> !choice.id.equals(selectedId))
                .thenComparing((FoodChoice choice) -> -choice.count)
                .thenComparing(choice -> choice.name, String.CASE_INSENSITIVE_ORDER));
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
        if (!isUsableFoodStack(stack, minecraft.player)) {
            feedback = "El item de tu mano principal no es una comida utilizable.";
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
        if (item == null) {
            feedback = "Ese ID no corresponde a un item cargado.";
            return;
        }

        LivingEntity eater = minecraft == null ? null : minecraft.player;
        boolean usable = false;
        if (minecraft != null && minecraft.player != null) {
            for (int slot = 0; slot < 36; slot++) {
                ItemStack stack = minecraft.player.getInventory().getItem(slot);
                if (stack.getItem() == item && isUsableFoodStack(stack, minecraft.player)) {
                    usable = true;
                    break;
                }
            }
        }
        if (!usable) {
            ItemStack defaultStack = new ItemStack(item);
            usable = defaultStack.isEdible() && defaultStack.getFoodProperties(eater) != null;
        }

        if (!usable) {
            feedback = "Ese ID no tiene propiedades de comida válidas para Smart Offhand.";
            return;
        }
        select(id.toString());
    }

    private static boolean isUsableFoodStack(ItemStack stack, LivingEntity eater) {
        return !stack.isEmpty() && stack.isEdible() && stack.getFoodProperties(eater) != null;
    }

    private String displayName(String idText) {
        if (idText == null || idText.isBlank()) return "AUTO";
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null) return idText;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null ? idText : new ItemStack(item).getHoverName().getString();
    }

    private void rebuildScreen() {
        if (minecraft != null) minecraft.setScreen(new FoodSelectionScreen(parent, page, feedback));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        String selected = LClientConfig.get().smartOffhandFoodId;
        String current = "Actual: " + displayName(selected) + " · detectada, AUTO o ID modded";
        graphics.drawCenteredString(font,
                font.plainSubstrByWidth(current, Math.max(160, width - 24)),
                width / 2,
                32,
                0xFFA9BAC9);
        if (foods.isEmpty() && minecraft != null && minecraft.player != null) {
            graphics.drawCenteredString(font,
                    "No hay comidas utilizables detectadas en los 36 slots.",
                    width / 2,
                    47,
                    0xFF8C9DAC);
        }
        if (!feedback.isBlank()) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(feedback, Math.max(160, width - 24)),
                    width / 2,
                    height - 14,
                    0xFF8FD8A0);
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
