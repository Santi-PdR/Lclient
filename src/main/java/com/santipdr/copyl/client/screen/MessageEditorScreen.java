package com.santipdr.copyl.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.santipdr.copyl.client.CopyLConfig;
import com.santipdr.copyl.client.CopyLKeyMappings;
import com.santipdr.copyl.client.MessageConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

/** Transactional, responsive editor for CopyL quick-message slots. */
public final class MessageEditorScreen extends Screen {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private static final int MAX_NAME_LENGTH = 24;
    private static final int CARD_HEIGHT = 48;
    private static final int CARD_STEP = 52;

    private final Screen parent;
    private final EditBox[] nameFields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final EditBox[] messageFields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final Button[] keyButtons = new Button[CopyLKeyMappings.SLOT_COUNT];

    private final String[] draftNames = new String[CopyLKeyMappings.SLOT_COUNT];
    private final String[] draftMessages = new String[CopyLKeyMappings.SLOT_COUNT];
    private final int[] draftKeys = new int[CopyLKeyMappings.SLOT_COUNT];

    private final int[] cardX = new int[CopyLKeyMappings.SLOT_COUNT];
    private final int[] cardY = new int[CopyLKeyMappings.SLOT_COUNT];
    private final int[] cardW = new int[CopyLKeyMappings.SLOT_COUNT];

    private int bindingIndex = -1;
    private int page;
    private String warning = "";
    private long warningUntil;
    private int actionY;

    public MessageEditorScreen(Screen parent) {
        super(Component.literal("CopyL"));
        this.parent = parent;

        MessageConfig config = MessageConfig.getInstance();
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            draftNames[i] = config.getName(i);
            draftMessages[i] = config.getMessage(i);
            draftKeys[i] = config.getKeyCode(i);
        }
    }

    @Override
    protected void init() {
        Arrays.fill(nameFields, null);
        Arrays.fill(messageFields, null);
        Arrays.fill(keyButtons, null);
        Arrays.fill(cardX, -1);
        Arrays.fill(cardY, -1);
        Arrays.fill(cardW, -1);

        boolean wide = useWideLayout();
        int pageSize = pageSize();
        int maxPage = Math.max(0, (CopyLKeyMappings.SLOT_COUNT - 1) / pageSize);
        page = Math.max(0, Math.min(page, maxPage));

        int contentWidth = Math.min(wide ? 820 : 520, Math.max(220, width - 24));
        int left = (width - contentWidth) / 2;
        int top = wide ? 64 : 56;
        int gap = 12;
        int columnWidth = wide ? (contentWidth - gap) / 2 : contentWidth;

        int firstSlot = wide ? 0 : page * pageSize;
        int lastSlot = wide
                ? CopyLKeyMappings.SLOT_COUNT
                : Math.min(CopyLKeyMappings.SLOT_COUNT, firstSlot + pageSize);

        for (int i = firstSlot; i < lastSlot; i++) {
            int local = wide ? i : i - firstSlot;
            int column = wide ? local / 5 : 0;
            int row = wide ? local % 5 : local;
            int x = left + column * (columnWidth + gap);
            int y = top + row * CARD_STEP;
            createSlotCard(i, x, y, columnWidth);
        }

        int visibleRows = wide ? 5 : pageSize;
        actionY = Math.min(height - 27, top + visibleRows * CARD_STEP + 4);
        int actionWidth = Math.min(132, Math.max(88, (contentWidth - 10) / 2));

        addRenderableWidget(Button.builder(Component.literal("Guardar cambios"), b -> saveAndClose())
                .bounds(width / 2 - actionWidth - 5, actionY, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> cancelAndClose())
                .bounds(width / 2 + 5, actionY, actionWidth, 20).build());

        if (!wide && maxPage > 0) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("‹"), b -> changePage(-1))
                    .bounds(width / 2 - 70, 31, 28, 18).build());
            previous.active = page > 0;

            Button next = addRenderableWidget(Button.builder(Component.literal("›"), b -> changePage(1))
                    .bounds(width / 2 + 42, 31, 28, 18).build());
            next.active = page < maxPage;
        }
    }

    private void createSlotCard(int slot, int x, int y, int width) {
        cardX[slot] = x;
        cardY[slot] = y;
        cardW[slot] = width;

        int innerX = x + 34;
        int innerWidth = Math.max(90, width - 42);
        int keyWidth = Math.min(92, Math.max(66, innerWidth / 3));
        int nameWidth = Math.max(54, innerWidth - keyWidth - 6);

        EditBox name = new EditBox(font, innerX, y + 4, nameWidth, 18,
                Component.literal("Nombre del slot " + (slot + 1)));
        name.setMaxLength(MAX_NAME_LENGTH);
        name.setValue(draftNames[slot]);
        name.setHint(Component.literal("Nombre"));
        nameFields[slot] = addRenderableWidget(name);

        keyButtons[slot] = addRenderableWidget(Button.builder(keyLabel(slot), b -> {
            captureFields();
            bindingIndex = slot;
            warning = "";
            updateKeyLabels();
        }).bounds(innerX + nameWidth + 6, y + 4, keyWidth, 18).build());

        EditBox message = new EditBox(font, innerX, y + 26, innerWidth, 18,
                Component.literal("Mensaje del slot " + (slot + 1)));
        message.setMaxLength(MAX_MESSAGE_LENGTH);
        message.setValue(draftMessages[slot]);
        message.setHint(Component.literal("Mensaje o /comando..."));
        messageFields[slot] = addRenderableWidget(message);
    }

    private void changePage(int direction) {
        captureFields();
        bindingIndex = -1;
        page += direction;
        rebuildWidgets();
    }

    private boolean useWideLayout() {
        return width >= 700 && height >= 350;
    }

    private int pageSize() {
        if (useWideLayout()) return CopyLKeyMappings.SLOT_COUNT;
        int available = Math.max(104, height - 118);
        return Math.max(2, Math.min(5, available / CARD_STEP));
    }

    private Component keyLabel(int slot) {
        if (bindingIndex == slot) return Component.literal("PULSA TECLA");
        int key = draftKeys[slot];
        return Component.literal(key < 0
                ? "Sin tecla"
                : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString());
    }

    private void updateKeyLabels() {
        for (int i = 0; i < keyButtons.length; i++) {
            if (keyButtons[i] != null) keyButtons[i].setMessage(keyLabel(i));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingIndex = -1;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                draftKeys[bindingIndex] = -1;
                bindingIndex = -1;
            } else if (keyCode == CopyLConfig.get().openKey) {
                showWarning("Esa tecla está reservada para abrir CopyL.");
                bindingIndex = -1;
            } else {
                assignDraftKey(bindingIndex, keyCode);
                bindingIndex = -1;
            }
            updateKeyLabels();
            return true;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void assignDraftKey(int slot, int keyCode) {
        for (int i = 0; i < draftKeys.length; i++) {
            if (i != slot && draftKeys[i] == keyCode) draftKeys[i] = -1;
        }
        draftKeys[slot] = keyCode;
    }

    private void showWarning(String text) {
        warning = text;
        warningUntil = System.currentTimeMillis() + 3500L;
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        captureFields();
        bindingIndex = -1;
        super.resize(minecraft, width, height);
    }

    private void captureFields() {
        for (int i = 0; i < draftNames.length; i++) {
            if (nameFields[i] != null) draftNames[i] = nameFields[i].getValue();
            if (messageFields[i] != null) draftMessages[i] = messageFields[i].getValue();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawBackdrop(graphics);
        drawCards(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawHeader(graphics);
    }

    private void drawBackdrop(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xA905080D);
        graphics.fill(0, 0, width, 52, 0xE60B1118);
        graphics.fill(0, 51, width, 53, 0xFF55B9E8);
        graphics.fill(0, actionY - 5, width, height, 0x9E090D12);
    }

    private void drawCards(GuiGraphics graphics) {
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            if (cardX[i] < 0) continue;
            int x = cardX[i];
            int y = cardY[i];
            int w = cardW[i];
            boolean configured = currentMessage(i) != null && !currentMessage(i).isBlank();
            boolean assigned = draftKeys[i] >= 0;

            graphics.fill(x + 2, y + 2, x + w + 2, y + CARD_HEIGHT + 2, 0x66000000);
            graphics.fill(x, y, x + w, y + CARD_HEIGHT, 0xD9141B23);
            graphics.fill(x, y, x + 3, y + CARD_HEIGHT,
                    configured ? 0xFF55B9E8 : 0xFF344451);
            graphics.fill(x + 3, y, x + w, y + 1, 0x553B5263);

            int badgeColor = assigned ? 0xFF8EDBFF : 0xFF6E7E8B;
            graphics.drawCenteredString(font,
                    String.format("%02d", i + 1),
                    x + 18,
                    y + 19,
                    badgeColor);
        }
    }

    private void drawHeader(GuiGraphics graphics) {
        int configured = 0;
        int assigned = 0;
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            String value = currentMessage(i);
            if (value != null && !value.isBlank()) configured++;
            if (draftKeys[i] >= 0) assigned++;
        }

        graphics.drawString(font, "COPYL", 14, 11, 0xFFFFFFFF, false);
        graphics.drawString(font, "MENSAJES RÁPIDOS", 14, 25, 0xFF7C9AAF, false);
        String status = configured + "/10 configurados  •  " + assigned + " teclas";
        graphics.drawString(font,
                status,
                Math.max(14, width - font.width(status) - 14),
                18,
                0xFF9FCFE6,
                false);

        if (!useWideLayout()) {
            int pageSize = pageSize();
            int maxPage = Math.max(0, (CopyLKeyMappings.SLOT_COUNT - 1) / pageSize);
            graphics.drawCenteredString(font,
                    "Página " + (page + 1) + "/" + (maxPage + 1),
                    width / 2,
                    36,
                    0xFFA8B8C6);
        }

        if (!warning.isBlank() && System.currentTimeMillis() <= warningUntil) {
            graphics.drawCenteredString(font,
                    font.plainSubstrByWidth(warning, Math.max(100, width - 30)),
                    width / 2,
                    Math.max(54, actionY - 13),
                    0xFFFFB777);
        }
    }

    private String currentMessage(int slot) {
        if (messageFields[slot] != null) return messageFields[slot].getValue();
        return draftMessages[slot];
    }

    private void saveAndClose() {
        captureFields();
        MessageConfig config = MessageConfig.getInstance();
        for (int i = 0; i < draftNames.length; i++) {
            config.setName(i, draftNames[i]);
            config.setMessage(i, draftMessages[i]);
        }
        for (int i = 0; i < draftKeys.length; i++) config.setKeyCode(i, draftKeys[i]);
        config.save();
        closeToParent();
    }

    private void cancelAndClose() {
        bindingIndex = -1;
        closeToParent();
    }

    @Override
    public void onClose() {
        cancelAndClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void closeToParent() {
        Arrays.fill(nameFields, null);
        Arrays.fill(messageFields, null);
        Arrays.fill(keyButtons, null);
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
