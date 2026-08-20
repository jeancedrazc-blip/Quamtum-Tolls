package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.SchematicCardItem;
import mcjty.rftoolsbuilder.constructor.SchematicFolderIndex;
import mcjty.rftoolsbuilder.constructor.SchematicTableBlockEntity;
import mcjty.rftoolsbuilder.constructor.SchematicTableMenu;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
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
        super(menu, inventory, title, 272, 224);
        this.inventoryLabelY = 136;
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
                    .bounds(x + 64, y + 39 + i * 17, 160, 15).build());
        }

        upButton = addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-1))
                .bounds(x + 228, y + 39, 16, 15).build());
        downButton = addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(1))
                .bounds(x + 228, y + 107, 16, 15).build());
        folderButton = addRenderableWidget(Button.builder(Component.literal("DIR"), b -> openFolder())
                .bounds(x + 64, y + 125, 38, 17).build());
        refreshButton = addRenderableWidget(Button.builder(Component.literal("↻"), b -> refreshSchematics())
                .bounds(x + 106, y + 125, 28, 17).build());
        confirmButton = addRenderableWidget(Button.builder(Component.literal("WRITE"), b -> writeSelected())
                .bounds(x + 138, y + 125, 86, 17).build());

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
                button.setMessage(Component.literal((index == selectedIndex ? "▶ " : "  ") + trim(name, 22)));
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

    @Override
    public void extractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        gui.fill(x, y, x + imageWidth, y + imageHeight, BG);
        gui.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL_2);
        gui.centeredText(font, Component.literal("SCHEMATIC TABLE"), x + imageWidth / 2, y + 8, CYAN);
        gui.fill(x + 8, y + 22, x + imageWidth - 8, y + 23, BORDER);

        panel(gui, x + 9, y + 30, x + 57, y + 143);
        panel(gui, x + 61, y + 30, x + 247, y + 143);
        panel(gui, x + 251, y + 30, x + 263, y + 143);

        gui.text(font, Component.literal("INPUT"), x + 17, y + 36, MUTED);
        gui.text(font, Component.literal("FILES"), x + 65, y + 34, MUTED);
        gui.text(font, Component.literal("OUT"), x + 249, y + 36, MUTED);

        if (schematics.isEmpty()) {
            gui.text(font, Component.literal("No supported schematic files"), x + 69, y + 61, ORANGE);
            gui.text(font, Component.literal(".nbt  .schem  .litematic  .schematic"), x + 69, y + 74, MUTED);
        }

        int status = menu.status();
        String statusText = statusText(status);
        gui.text(font, Component.literal(statusText), x + 14, y + 151, statusColor(status));

        if (status == SchematicTableBlockEntity.STATUS_UPLOADING) {
            int barX1 = x + 64;
            int barX2 = x + 224;
            int barY1 = y + 126;
            int barY2 = y + 140;
            gui.fill(barX1, barY1, barX2, barY2, DARK);
            int width = (barX2 - barX1 - 2) * Math.max(0, Math.min(10_000, menu.progress())) / 10_000;
            gui.fill(barX1 + 1, barY1 + 1, barX1 + 1 + width, barY2 - 1, CYAN);
            gui.centeredText(font, Component.literal((menu.progress() / 100) + "%"), (barX1 + barX2) / 2, barY1 + 3, TEXT);
        }

        gui.fill(x + 8, y + 164, x + imageWidth - 8, y + 165, BORDER);
        gui.text(font, Component.literal("PLAYER INVENTORY"), x + 55, y + 168, MUTED);
    }

    private static String statusText(int status) {
        return switch (status) {
            case SchematicTableBlockEntity.STATUS_READY -> "Select a local schematic and write it to the input card";
            case SchematicTableBlockEntity.STATUS_UPLOADING -> "Uploading and validating schematic…";
            case SchematicTableBlockEntity.STATUS_FINISHED -> "Finished — take the written card from OUTPUT";
            case SchematicTableBlockEntity.STATUS_ERROR -> "Upload rejected — input card restored";
            default -> "Insert a Schematic Card in INPUT";
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
