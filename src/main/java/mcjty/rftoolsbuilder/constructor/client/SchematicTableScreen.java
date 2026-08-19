package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.SchematicTableBlockEntity;
import mcjty.rftoolsbuilder.constructor.SchematicTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SchematicTableScreen extends AbstractContainerScreen<SchematicTableMenu> {
    private static final int BG = 0xFF071018;
    private static final int PANEL = 0xFF0D1B26;
    private static final int PANEL_2 = 0xFF122735;
    private static final int BORDER = 0xFF1F5366;
    private static final int CYAN = 0xFF1CD6F2;
    private static final int ORANGE = 0xFFF18432;
    private static final int TEXT = 0xFFE7F7FA;
    private static final int MUTED = 0xFF8EA9B2;

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

        addRenderableWidget(Button.builder(Component.literal("<"), b -> sendButton(0)).bounds(x + 154, y + 36, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> sendButton(1)).bounds(x + 218, y + 36, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal("M"), b -> sendButton(2)).bounds(x + 242, y + 36, 22, 18).build());

        addRenderableWidget(Button.builder(Component.literal("-"), b -> sendButton(10)).bounds(x + 154, y + 62, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> sendButton(11)).bounds(x + 222, y + 62, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> sendButton(12)).bounds(x + 154, y + 84, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> sendButton(13)).bounds(x + 222, y + 84, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> sendButton(14)).bounds(x + 154, y + 106, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> sendButton(15)).bounds(x + 222, y + 106, 18, 18).build());

        addRenderableWidget(Button.builder(Component.literal("PREVIEW"), b -> sendButton(20)).bounds(x + 12, y + 92, 66, 20).build());
        addRenderableWidget(Button.builder(Component.literal("SAVE MAP"), b -> sendButton(30)).bounds(x + 82, y + 92, 66, 20).build());
        addRenderableWidget(Button.builder(Component.literal("SEND"), b -> sendButton(21)).bounds(x + 154, y + 128, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("CLEAR"), b -> sendButton(31)).bounds(x + 82, y + 114, 66, 16).build());
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

        panel(gui, x + 10, y + 30, x + 146, y + 136);
        panel(gui, x + 150, y + 30, x + 266, y + 124);

        gui.text(font, Component.literal("SCHEMATIC CARD"), x + 16, y + 34, MUTED);
        gui.fill(x + 20, y + 40, x + 46, y + 66, 0xFF050A0E);
        gui.fill(x + 21, y + 41, x + 45, y + 65, CYAN);
        gui.fill(x + 23, y + 43, x + 43, y + 63, 0xFF08141C);

        gui.text(font, Component.literal("REPLACE"), x + 58, y + 34, MUTED);
        gui.fill(x + 84, y + 40, x + 108, y + 64, 0xFF050A0E);
        gui.fill(x + 108, y + 50, x + 116, y + 54, ORANGE);
        gui.fill(x + 116, y + 40, x + 140, y + 64, 0xFF050A0E);

        int rotation = menu.data().get(0) * 90;
        int mirror = menu.data().get(1);
        int ox = menu.data().get(2);
        int oy = menu.data().get(3);
        int oz = menu.data().get(4);
        int status = menu.data().get(5);
        int replacements = menu.data().get(6);

        gui.text(font, Component.literal("Rotation"), x + 156, y + 34, MUTED);
        gui.centeredText(font, Component.literal(rotation + "°"), x + 198, y + 41, TEXT);
        gui.text(font, Component.literal("Mirror: " + mirrorText(mirror)), x + 156, y + 56, TEXT);
        gui.text(font, Component.literal("X"), x + 178, y + 66, MUTED);
        gui.centeredText(font, Component.literal(Integer.toString(ox)), x + 197, y + 67, TEXT);
        gui.text(font, Component.literal("Y"), x + 178, y + 88, MUTED);
        gui.centeredText(font, Component.literal(Integer.toString(oy)), x + 197, y + 89, TEXT);
        gui.text(font, Component.literal("Z"), x + 178, y + 110, MUTED);
        gui.centeredText(font, Component.literal(Integer.toString(oz)), x + 197, y + 111, TEXT);

        gui.text(font, Component.literal("Mappings: " + replacements + "/8"), x + 16, y + 72, TEXT);
        gui.text(font, Component.literal(statusText(status)), x + 16, y + 82, status == SchematicTableBlockEntity.STATUS_SENT ? CYAN : ORANGE);

        gui.fill(x + 8, y + 136, x + imageWidth - 8, y + 137, BORDER);
        gui.text(font, Component.literal("PLAYER INVENTORY"), x + 48, y + 132, MUTED);
    }

    private static String mirrorText(int value) {
        return switch (value) {
            case 1 -> "X";
            case 2 -> "Z";
            default -> "OFF";
        };
    }

    private static String statusText(int status) {
        return switch (status) {
            case SchematicTableBlockEntity.STATUS_READY -> "Card ready";
            case SchematicTableBlockEntity.STATUS_SENT -> "Sent to Constructor";
            case SchematicTableBlockEntity.STATUS_NO_CONSTRUCTOR -> "No Constructor within 16 blocks";
            case SchematicTableBlockEntity.STATUS_REPLACEMENT_SAVED -> "Replacement saved";
            case SchematicTableBlockEntity.STATUS_PREVIEW_READY -> "Test schematic prepared";
            case SchematicTableBlockEntity.STATUS_BAD_REPLACEMENT -> "Use two block samples";
            default -> "Insert a Schematic Card";
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
    }
}
