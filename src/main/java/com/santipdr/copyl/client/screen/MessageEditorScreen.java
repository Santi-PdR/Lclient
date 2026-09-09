package com.santipdr.copyl.client.screen;

import com.santipdr.copyl.client.CopyLBinding;
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
    private static final long DISCARD_CONFIRM_MS = 3500L;

    private final Screen parent;
    private final EditBox[] nameFields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final EditBox[] messageFields = new EditBox[CopyLKeyMappings.SLOT_COUNT];
    private final Button[] bindingButtons = new Button[CopyLKeyMappings.SLOT_COUNT];

    private final String[] draftNames;
    private final String[] draftMessages;
    private final int[] draftBindings;
    private final String[] originalNames;
    private final String[] originalMessages;
    private final int[] originalBindings;

    private final int[] cardX = new int[CopyLKeyMappings.SLOT_COUNT];
    private final int[] cardY = new int[CopyLKeyMappings.SLOT_COUNT];
    private final int[] cardW = new int[CopyLKeyMappings.SLOT_COUNT];

    private int bindingIndex = -1;
    private int page;
    private Component warning = Component.empty();
    private int warningColor = 0xFFFFB777;
    private long warningUntil;
    private long discardConfirmUntil;
    private int actionY;

    public MessageEditorScreen(Screen parent) {
        super(Component.translatable("screen.copyl.title"));
        this.parent = parent;

        MessageConfig config = MessageConfig.getInstance();
        draftNames = config.copyNames();
        draftMessages = config.copyMessages();
        draftBindings = config.copyKeyCodes();
        originalNames = draftNames.clone();
        originalMessages = draftMessages.clone();
        originalBindings = draftBindings.clone();
    }

    @Override
    protected void init() {
        Arrays.fill(nameFields, null);
        Arrays.fill(messageFields, null);
        Arrays.fill(bindingButtons, null);
        Arrays.fill(cardX, -1);
        Arrays.fill(cardY, -1);
        Arrays.fill(cardW, -1);

        boolean wide = useWideLayout();
        int pageSize = pageSize();
        int maxPage = Math.max(0, (CopyLKeyMappings.SLOT_COUNT - 1) / pageSize);
        page = Math.max(0, Math.min(page, maxPage));

        int contentWidth = Math.min(wide ? 820 : 520, Math.max(150, width - 16));
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
        int actionWidth = Math.min(132, Math.max(68, (contentWidth - 10) / 2));

        addRenderableWidget(Button.builder(Component.translatable("screen.copyl.save"), b -> saveAndClose())
                .bounds(width / 2 - actionWidth - 5, actionY, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.copyl.cancel"), b -> attemptCancel())
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
        int clearWidth = 18;
        int bindingWidth = Math.min(92, Math.max(52, innerWidth / 3));
        int nameWidth = Math.max(40, innerWidth - bindingWidth - clearWidth - 8);

        int occupied = nameWidth + bindingWidth + clearWidth + 8;
        if (occupied > innerWidth) {
            bindingWidth = Math.max(42, bindingWidth - (occupied - innerWidth));
        }

        EditBox name = new EditBox(font, innerX, y + 4, nameWidth, 18,
                Component.translatable("screen.copyl.slot_name", slot + 1));
        name.setMaxLength(MAX_NAME_LENGTH);
        name.setValue(draftNames[slot]);
        name.setHint(Component.translatable("screen.copyl.name_hint"));
        nameFields[slot] = addRenderableWidget(name);

        int bindingX = innerX + nameWidth + 4;
        bindingButtons[slot] = addRenderableWidget(Button.builder(bindingLabel(slot), b -> {
            captureFields();
            bindingIndex = slot;
            warning = Component.empty();
            updateBindingLabels();
        }).bounds(bindingX, y + 4, bindingWidth, 18).build());

        addRenderableWidget(Button.builder(Component.literal("×"), b -> clearSlot(slot))
                .bounds(bindingX + bindingWidth + 4, y + 4, clearWidth, 18).build());

        EditBox message = new EditBox(font, innerX, y + 26, innerWidth, 18,
                Component.translatable("screen.copyl.slot_message", slot + 1));
        message.setMaxLength(MAX_MESSAGE_LENGTH);
        message.setValue(draftMessages[slot]);
        message.setHint(Component.translatable("screen.copyl.message_hint"));
        messageFields[slot] = addRenderableWidget(message);
    }

    private void clearSlot(int slot) {
        captureFields();
        draftMessages[slot] = "";
        draftBindings[slot] = CopyLBinding.UNBOUND;
        if (messageFields[slot] != null) messageFields[slot].setValue("");
        updateBindingLabels();
        showWarning(Component.translatable("screen.copyl.slot_cleared", slot + 1), 0xFF8EDBFF);
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

    private Component bindingLabel(int slot) {
        if (bindingIndex == slot) return Component.translatable("screen.copyl.press_binding");
        return CopyLBinding.displayName(draftBindings[slot]);
    }

    private void updateBindingLabels() {
        for (int i = 0; i < bindingButtons.length; i++) {
            if (bindingButtons[i] != null) bindingButtons[i].setMessage(bindingLabel(i));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingIndex = -1;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                draftBindings[bindingIndex] = CopyLBinding.UNBOUND;
                bindingIndex = -1;
            } else {
                int candidate = CopyLBinding.sanitize(keyCode);
                if (candidate == CopyLConfig.get().openKey) {
                    showWarning(Component.translatable("screen.copyl.binding_reserved"), 0xFFFFB777);
                } else if (candidate >= 0) {
                    assignDraftBinding(bindingIndex, candidate);
                }
                bindingIndex = -1;
            }
            updateBindingLabels();
            return true;
        }

        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (control && (keyCode == GLFW.GLFW_KEY_S
                || keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            saveAndClose();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            attemptCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bindingIndex >= 0) {
            int candidate = CopyLBinding.encodeMouse(button);
            if (candidate == CopyLConfig.get().openKey) {
                showWarning(Component.translatable("screen.copyl.binding_reserved"), 0xFFFFB777);
            } else if (candidate >= 0) {
                assignDraftBinding(bindingIndex, candidate);
            }
            bindingIndex = -1;
            updateBindingLabels();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void assignDraftBinding(int slot, int binding) {
        int previousSlot = -1;
        for (int i = 0; i < draftBindings.length; i++) {
            if (i != slot && draftBindings[i] == binding) {
                draftBindings[i] = CopyLBinding.UNBOUND;
                previousSlot = i;
            }
        }
        draftBindings[slot] = binding;
        if (previousSlot >= 0) {
            showWarning(Component.translatable("screen.copyl.binding_moved", previousSlot + 1, slot + 1), 0xFF8EDBFF);
        }
    }

    private void showWarning(Component text, int color) {
        warning = text;
        warningColor = color;
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

    private boolean hasUnsavedChanges() {
        captureFields();
        return !Arrays.equals(draftNames, originalNames)
                || !Arrays.equals(draftMessages, originalMessages)
                || !Arrays.equals(draftBindings, originalBindings);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawBackdrop(graphics);
        drawCards(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawHeader(graphics);
    }

    private void drawBackdrop(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xA905080D);
        graphics.fill(0, 0, width, 52, 0xE60B1118);
        graphics.fill(0, 51, width, 53, 0xFF55B9E8);
        graphics.fill(0, Math.max(0, actionY - 5), width, height, 0x9E090D12);
    }

    private void drawCards(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            if (cardX[i] < 0) continue;
            int x = cardX[i];
            int y = cardY[i];
            int w = cardW[i];
            String message = currentMessage(i);
            boolean configured = message != null && !message.isBlank();
            boolean assigned = draftBindings[i] >= 0;
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CARD_HEIGHT;

            int accent = configured && assigned
                    ? 0xFF55B9E8
                    : (configured || assigned ? 0xFFE6AE63 : 0xFF344451);
            int body = hovered ? 0xEA19232D : 0xD9141B23;

            graphics.fill(x + 2, y + 2, x + w + 2, y + CARD_HEIGHT + 2, 0x66000000);
            graphics.fill(x, y, x + w, y + CARD_HEIGHT, body);
            graphics.fill(x, y, x + 3, y + CARD_HEIGHT, accent);
            graphics.fill(x + 3, y, x + w, y + 1, hovered ? 0xAA6494AE : 0x553B5263);

            graphics.drawCenteredString(font,
                    String.format("%02d", i + 1),
                    x + 18,
                    y + 8,
                    accent);

            String tag;
            if (!configured) tag = assigned ? "KEY" : "—";
            else tag = message.startsWith("/") ? "CMD" : "CHAT";
            graphics.drawCenteredString(font, tag, x + 18, y + 29, 0xFF91A4B2);
        }
    }

    private void drawHeader(GuiGraphics graphics) {
        int configured = 0;
        int assigned = 0;
        for (int i = 0; i < CopyLKeyMappings.SLOT_COUNT; i++) {
            String value = currentMessage(i);
            if (value != null && !value.isBlank()) configured++;
            if (draftBindings[i] >= 0) assigned++;
        }

        graphics.drawString(font, "COPYL", 14, 11, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.copyl.header_subtitle"), 14, 25, 0xFF7C9AAF, false);

        Component status = Component.translatable("screen.copyl.editor_summary", configured, assigned);
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
                    Component.translatable("screen.copyl.page", page + 1, maxPage + 1),
                    width / 2,
                    36,
                    0xFFA8B8C6);
        }

        if (warning != null && System.currentTimeMillis() <= warningUntil) {
            String clipped = font.plainSubstrByWidth(warning.getString(), Math.max(100, width - 30));
            graphics.drawCenteredString(font,
                    clipped,
                    width / 2,
                    Math.max(54, actionY - 13),
                    warningColor);
        }
    }

    private String currentMessage(int slot) {
        if (messageFields[slot] != null) return messageFields[slot].getValue();
        return draftMessages[slot];
    }

    private void saveAndClose() {
        captureFields();
        if (!MessageConfig.getInstance().replaceAll(draftNames, draftMessages, draftBindings)) {
            showWarning(Component.translatable("screen.copyl.save_failed"), 0xFFFF7777);
            return;
        }
        closeToParent();
    }

    private void attemptCancel() {
        bindingIndex = -1;
        updateBindingLabels();
        if (!hasUnsavedChanges()) {
            closeToParent();
            return;
        }

        long now = System.currentTimeMillis();
        if (now <= discardConfirmUntil) {
            closeToParent();
            return;
        }

        discardConfirmUntil = now + DISCARD_CONFIRM_MS;
        showWarning(Component.translatable("screen.copyl.unsaved_changes"), 0xFFFFB777);
    }

    @Override
    public void onClose() {
        attemptCancel();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void closeToParent() {
        Arrays.fill(nameFields, null);
        Arrays.fill(messageFields, null);
        Arrays.fill(bindingButtons, null);
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
