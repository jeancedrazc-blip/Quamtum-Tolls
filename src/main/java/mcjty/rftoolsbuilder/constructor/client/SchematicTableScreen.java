package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.SchematicCardItem;
import mcjty.rftoolsbuilder.constructor.SchematicFolderIndex;
import mcjty.rftoolsbuilder.constructor.SchematicTableBlockEntity;
import mcjty.rftoolsbuilder.constructor.SchematicTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class SchematicTableScreen extends AbstractContainerScreen<SchematicTableMenu> {
    private static final int BG = 0xFF071018;
    private static final int PANEL = 0xFF0D1B26;
    private static final int PANEL_2 = 0xFF122735;
    private static final int BORDER = 0xFF1F5366;
    private static final int CYAN = 0xFF1CD6F2;
    private static final int ORANGE = 0xFFF18432;
    private static final int GREEN = 0xFF67E39A;
    private static final int TEXT = 0xFFE7F7FA;
    private static final int MUTED = 0xFF8EA9B2;
    private static final int DARK = 0xFF050A0E;
    private static final int ROWS = 5;

    private final Button[] rowButtons = new Button[ROWS];
    private List<SchematicFolderIndex.Entry> schematics = List.of();
    private int selectedIndex = -1;
    private int firstVisible;
    private Button confirmButton;
    private Button upButton;
    private Button downButton;

    public SchematicTableScreen(SchematicTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 272, 220);
        this.inventoryLabelY = 128;
        this.titleLabelY = 7;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            rowButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> selectRow(row))
                    .bounds(x + 66, y + 38 + i * 18, 174, 16).build());
        }

        upButton = addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-1))
                .bounds(x + 244, y + 38, 18, 16).build());
        downButton = addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(1))
                .bounds(x + 244, y + 110, 18, 16).build());

        addRenderableWidget(Button.builder(Component.literal("REFRESH"), b -> {
            refreshSchematics();
            sendButton(SchematicTableBlockEntity.BUTTON_REFRESH);
        }).bounds(x + 66, y + 112, 82, 18).build());

        confirmButton = addRenderableWidget(Button.builder(Component.literal("WRITE TO CARD"), b -> writeSelected())
                .bounds(x + 154, y + 112, 86, 18).build());

        refreshSchematics();
    }

    private void refreshSchematics() {
        schematics = SchematicFolderIndex.list();
        if (schematics.isEmpty()) {
            selectedIndex = -1;
            firstVisible = 0;
        } else {
            if (selectedIndex < 0 || selectedIndex >= schematics.size()) selectedIndex = 0;
            firstVisible = Math.max(0, Math.min(firstVisible, Math.max(0, schematics.size() - ROWS)));
            ensureSelectedVisible();
        }
        updateRows();
    }

    private void selectRow(int row) {
        int index = firstVisible + row;
        if (index < 0 || index >= schematics.size()) return;
        selectedIndex = index;
        updateRows();
    }

    private void scroll(int direction) {
        if (schematics.size() <= ROWS) return;
        firstVisible = Math.max(0, Math.min(firstVisible + direction, schematics.size() - ROWS));
        updateRows();
    }

    private void ensureSelectedVisible() {
        if (selectedIndex < firstVisible) firstVisible = selectedIndex;
        if (selectedIndex >= firstVisible + ROWS) firstVisible = selectedIndex - ROWS + 1;
    }

    private void updateRows() {
        for (int row = 0; row < ROWS; row++) {
            Button button = rowButtons[row];
            if (button == null) continue;
            int index = firstVisible + row;
            boolean exists = index >= 0 && index < schematics.size();
            button.visible = exists;
            button.active = exists;
            if (exists) {
                String name = schematics.get(index).fileName();
                button.setMessage(Component.literal((index == selectedIndex ? "▶ " : "  ") + trim(name, 25)));
            }
        }
        if (confirmButton != null) confirmButton.active = selectedIndex >= 0 && menu.data().get(0) != SchematicTableBlockEntity.STATUS_NO_CARD;
        if (upButton != null) upButton.active = firstVisible > 0;
        if (downButton != null) downButton.active = firstVisible + ROWS < schematics.size();
    }

    private void writeSelected() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) return;
        sendButton(SchematicTableBlockEntity.BUTTON_SELECT_BASE + selectedIndex);
    }

    private void sendButton(int id) {
        Minecraft mc = this.minecraft;
        if (mc != null && mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private static void panel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        gui.fill(x1, y1, x2, y2, BORDER);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, PANEL);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        gui.fill(x, y, x + imageWidth, y + imageHeight, BG);
        gui.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL_2);
        gui.centeredText(font, Component.literal("SCHEMATIC TABLE"), x + imageWidth / 2, y + 8, CYAN);
        gui.fill(x + 8, y + 22, x + imageWidth - 8, y + 23, BORDER);

        panel(gui, x + 10, y + 30, x + 58, y + 132);
        panel(gui, x + 62, y + 30, x + 264, y + 132);

        gui.text(font, Component.literal("CARD"), x + 18, y + 36, MUTED);
        gui.fill(x + 19, y + 43, x + 47, y + 71, DARK);
        gui.fill(x + 20, y + 44, x + 46, y + 70, CYAN);
        gui.fill(x + 22, y + 46, x + 44, y + 68, 0xFF08141C);

        String cardName = menu.table() == null || menu.table().card().isEmpty()
                ? "Insert card"
                : SchematicCardItem.sourceName(menu.table().card());
        gui.text(font, Component.literal(trim(cardName, 8)), x + 16, y + 80, TEXT);

        gui.text(font, Component.literal("SCHEMATICS /schematics"), x + 68, y + 34, MUTED);
        if (schematics.isEmpty()) {
            gui.text(font, Component.literal("No .nbt schematics found"), x + 72, y + 58, ORANGE);
        }

        int status = menu.data().get(0);
        gui.text(font, Component.literal(statusText(status)), x + 68, y + 132, statusColor(status));

        gui.fill(x + 8, y + 138, x + imageWidth - 8, y + 139, BORDER);
        gui.text(font, Component.literal("PLAYER INVENTORY"), x + 48, y + 132 + 10, MUTED);
    }

    private static String statusText(int status) {
        return switch (status) {
            case SchematicTableBlockEntity.STATUS_READY -> "Choose a schematic and write it to the card";
            case SchematicTableBlockEntity.STATUS_WRITTEN -> "Schematic written to card";
            case SchematicTableBlockEntity.STATUS_NO_SCHEMATICS -> "No schematics found in /schematics";
            case SchematicTableBlockEntity.STATUS_INVALID_SELECTION -> "Invalid schematic selection";
            default -> "Insert a Schematic Card";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case SchematicTableBlockEntity.STATUS_WRITTEN -> GREEN;
            case SchematicTableBlockEntity.STATUS_NO_SCHEMATICS, SchematicTableBlockEntity.STATUS_INVALID_SELECTION -> ORANGE;
            default -> TEXT;
        };
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateRows();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
    }
}
