package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.MaterialListTabletItem;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumButton;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Tall glass-fronted material list UI based on the approved sketch. */
public final class MaterialListTabletScreen extends Screen {
    private static final int PANEL_W = 246;
    private static final int PANEL_H = 286;
    private final ItemStack tablet;

    public MaterialListTabletScreen(ItemStack tablet) {
        super(Component.literal("MATERIAL LIST TABLET"));
        this.tablet = tablet;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        addRenderableWidget(new QuantumButton(left + PANEL_W - 23, top + 7, 16, 16,
                Component.literal("×"), this::closeTablet, () -> false, QuantumUiTheme.RED));
    }

    private void closeTablet() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        QuantumUiTheme.window(gui, left, top, PANEL_W, PANEL_H);
        QuantumUiTheme.title(gui, font, Component.literal("MATERIAL LIST // TABLET"), left + PANEL_W / 2, top + 10);
        QuantumUiTheme.panel(gui, left + 11, top + 31, left + PANEL_W - 18, top + PANEL_H - 13,
                QuantumUiTheme.BORDER, 0xFF071118);

        gui.text(font, Component.literal(MaterialListTabletItem.isWritten(tablet) ? "SCHEMATIC MATERIALS" : "BLANK TABLET"),
                left + 20, top + 42, QuantumUiTheme.CYAN, false);
        gui.text(font, Component.literal(MaterialListTabletItem.isWritten(tablet)
                        ? "Material data is synchronized by the Constructor"
                        : "Insert this tablet in the Constructor input"),
                left + 20, top + 57, QuantumUiTheme.TEXT_SOFT, false);

        // Textured list rows and reserved icon cells.
        int rowY = top + 82;
        for (int row = 0; row < 8; row++) {
            int y = rowY + row * 22;
            QuantumUiTheme.panel(gui, left + 18, y, left + PANEL_W - 28, y + 19,
                    QuantumUiTheme.BORDER_DIM, row % 2 == 0 ? 0xFF0A1820 : 0xFF0C1C25);
            QuantumUiTheme.slotFrame(gui, left + 21, y + 1, false, QuantumUiTheme.CYAN);
            gui.fill(left + 47, y + 6, left + PANEL_W - 39, y + 8, 0xFF18333E);
            gui.fill(left + 47, y + 11, left + PANEL_W - 62, y + 13, 0xFF102832);
        }

        // Thin textured scrollbar from the sketch.
        int sx = left + PANEL_W - 13;
        gui.fill(sx, top + 34, sx + 4, top + PANEL_H - 16, QuantumUiTheme.DEEP);
        gui.fill(sx + 1, top + 36, sx + 3, top + 92, QuantumUiTheme.CYAN_DIM);
        gui.fill(sx + 1, top + 38, sx + 2, top + 90, QuantumUiTheme.CYAN);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
