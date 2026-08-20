package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.SchematicCardItem;
import mcjty.rftoolsbuilder.constructor.SchematicFolderIndex;
import mcjty.rftoolsbuilder.constructor.SchematicTableBlockEntity;
import mcjty.rftoolsbuilder.constructor.SchematicTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SchematicTableScreen extends AbstractContainerScreen<SchematicTableMenu> {
    private static final int BG = 0xFF050B11;
    private static final int PANEL = 0xFF0B1720;
    private static final int PANEL_2 = 0xFF0F212D;
    private static final int BORDER = 0xFF24576A;
    private static final int CYAN = 0xFF20D9F3;
    private static final int ORANGE = 0xFFF18432;
    private static final int GREEN = 0xFF69E6A0;
    private static final int TEXT = 0xFFE8F5F7;
    private static final int MUTED = 0xFF8CA6AE;
    private static final int DARK = 0xFF03070A;
    private static final int ROWS = 5;

    private final Button[] rowButtons = new Button[ROWS];
    private List<SchematicFolderIndex.Entry> schematics = List.of();
    private int selectedIndex = -1;
    private int firstVisible;
    private Button confirmButton;
    private Button upButton;
    private Button downButton;
    private Button folderButton;
    private Button refreshButton;

    public SchematicTableScreen(SchematicTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 296, 252);
        this.inventoryLabelY = 166;
        this.titleLabelY = 8;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            rowButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> selectRow(row))
                    .bounds(x + 65, y + 40 + i * 17, 150, 15).build());
        }

        upButton = addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-1))
                .bounds(x + 219, y + 40, 17, 15).build());
        downButton = addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(1))
                .bounds(x + 219, y + 108, 17, 15).build());
        folderButton = addRenderableWidget(Button.builder(Component.literal("FOLDER"), b -> openFolder())
                .bounds(x + 65, y + 128, 49, 17).build());
        refreshButton = addRenderableWidget(Button.builder(Component.literal("↻"), b -> refreshSchematics())
                .bounds(x + 118, y + 128, 28, 17).build());
        confirmButton = addRenderableWidget(Button.builder(Component.literal("WRITE CARD"), b -> writeSelected())
                .bounds(x + 150, y + 128, 86, 17).build());

        refreshSchematics();
    }

    private void openFolder() {
        try {
            Path folder = Path.of("schematics").toAbsolutePath().normalize();
            Files.createDirectories(folder);
            Util.getPlatform().openFile(folder.toFile());
        } catch (Exception ignored) {
        }
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
        if (menu.status() == SchematicTableBlockEntity.STATUS_UPLOADING) return;
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
        boolean busy = menu.status() == SchematicTableBlockEntity.STATUS_UPLOADING;
        for (int row = 0; row < ROWS; row++) {
            Button button = rowButtons[row];
            if (button == null) continue;
            int index = firstVisible + row;
            boolean exists = index >= 0 && index < schematics.size();
            button.visible = exists;
            button.active = exists && !busy;
            if (exists) {
                SchematicFolderIndex.Entry entry = schematics.get(index);
                String marker = switch (entry.format()) {
                    case VANILLA_NBT -> "NBT";
                    case SPONGE_SCHEM -> "SCHEM";
                    case LITEMATICA -> "LITEM";
                    case LEGACY_SCHEMATIC -> "OLD";
                };
                String name = "[" + marker + "] " + entry.fileName();
                button.setMessage(Component.literal((index == selectedIndex ? "▶ " : "  ") + trim(name, 21)));
            }
        }

        boolean inputPresent = menu.table() != null && menu.table().inputCard().getItem() instanceof SchematicCardItem;
        if (confirmButton != null) confirmButton.active = selectedIndex >= 0 && inputPresent && !menu.hasOutput() && !busy;
        if (upButton != null) upButton.active = !busy && firstVisible > 0;
        if (downButton != null) downButton.active = !busy && firstVisible + ROWS < schematics.size();
        if (folderButton != null) folderButton.active = !busy;
        if (refreshButton != null) refreshButton.active = !busy;
    }

    private void writeSelected() {
        if (selectedIndex < 0 || selectedIndex >= schematics.size()) return;
        if (menu.status() == SchematicTableBlockEntity.STATUS_UPLOADING || menu.hasOutput()) return;
        ClientSchematicUploader.upload(menu.blockPos(), schematics.get(selectedIndex));
    }

    private static void panel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        gui.fill(x1, y1, x2, y2, BORDER);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, PANEL);
    }

    private void centered(GuiGraphicsExtractor gui, Component component, int centerX, int y, int color) {
        gui.text(font, component, centerX - font.width(component) / 2, y, color, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        gui.fill(x, y, x + imageWidth, y + imageHeight, BG);
        gui.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL_2);
        centered(gui, Component.literal("SCHEMATIC TABLE"), x + imageWidth / 2, y + 8, CYAN);
        gui.fill(x + 8, y + 23, x + imageWidth - 8, y + 24, BORDER);

        panel(gui, x + 9, y + 31, x + 58, y + 148);
        panel(gui, x + 61, y + 31, x + 240, y + 148);
        panel(gui, x + 244, y + 31, x + 287, y + 148);

        centered(gui, Component.literal("INPUT"), x + 33, y + 37, MUTED);
        gui.text(font, Component.literal("SCHEMATIC FILES"), x + 65, y + 35, MUTED);
        centered(gui, Component.literal("OUTPUT"), x + 265, y + 37, MUTED);

        // Frame the actual Minecraft slots so they read as deliberate card bays.
        gui.fill(x + 22, y + 61, x + 44, y + 83, BORDER);
        gui.fill(x + 23, y + 62, x + 43, y + 82, DARK);
        gui.fill(x + 252, y + 61, x + 274, y + 83, menu.hasOutput() ? GREEN : BORDER);
        gui.fill(x + 253, y + 62, x + 273, y + 82, DARK);

        gui.text(font, Component.literal("blank / rewriteable"), x + 13, y + 91, MUTED);
        gui.text(font, Component.literal("written card"), x + 249, y + 91, MUTED);

        if (schematics.isEmpty()) {
            gui.text(font, Component.literal("No supported schematic files"), x + 67, y + 62, ORANGE);
            gui.text(font, Component.literal("NBT · SCHEM · LITEMATIC · SCHEMATIC"), x + 67, y + 76, MUTED);
        }

        int status = menu.status();
        if (status == SchematicTableBlockEntity.STATUS_UPLOADING) {
            int barX1 = x + 65;
            int barX2 = x + 236;
            int barY1 = y + 128;
            int barY2 = y + 145;
            gui.fill(barX1, barY1, barX2, barY2, DARK);
            gui.fill(barX1, barY1, barX2, barY1 + 1, BORDER);
            int width = (barX2 - barX1 - 2) * Math.max(0, Math.min(10_000, menu.progress())) / 10_000;
            gui.fill(barX1 + 1, barY1 + 2, barX1 + 1 + width, barY2 - 1, CYAN);
            centered(gui, Component.literal((menu.progress() / 100) + "%"), (barX1 + barX2) / 2, barY1 + 4, TEXT);
        }

        gui.text(font, Component.literal(statusText(status)), x + 12, y + 153, statusColor(status));
        gui.fill(x + 8, y + 164, x + imageWidth - 8, y + 165, BORDER);
        gui.text(font, Component.literal("PLAYER INVENTORY"), x + 67, y + 166, MUTED);
    }

    private static String statusText(int status) {
        return switch (status) {
            case SchematicTableBlockEntity.STATUS_READY -> "Ready — select a file and WRITE CARD";
            case SchematicTableBlockEntity.STATUS_UPLOADING -> "Uploading, hashing and validating on server…";
            case SchematicTableBlockEntity.STATUS_FINISHED -> "Finished — take the card from OUTPUT";
            case SchematicTableBlockEntity.STATUS_ERROR -> "Rejected — INPUT card restored safely";
            default -> "Insert a Schematic Card into INPUT";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case SchematicTableBlockEntity.STATUS_FINISHED -> GREEN;
            case SchematicTableBlockEntity.STATUS_ERROR -> ORANGE;
            case SchematicTableBlockEntity.STATUS_UPLOADING -> CYAN;
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
