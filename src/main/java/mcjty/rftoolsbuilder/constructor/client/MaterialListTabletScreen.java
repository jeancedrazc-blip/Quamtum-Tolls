package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.MaterialListTabletItem;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumButton;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

/** Tall glass-fronted material list UI based on the approved sketch. */
public final class MaterialListTabletScreen extends Screen {
    private static final int PANEL_W = 246;
    private static final int PANEL_H = 286;
    private final ItemStack tablet;
    private final List<MaterialRow> rows = new ArrayList<>();
    private String schematicName = "-";
    private int total;

    public MaterialListTabletScreen(ItemStack tablet) {
        super(Component.literal("MATERIAL LIST TABLET"));
        this.tablet = tablet;
        readTablet();
    }

    private void readTablet() {
        CustomData data = tablet.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        var tag = data.copyTag();
        schematicName = tag.getString("QTSchematicName").orElse("-");
        total = tag.getIntOr("QTMaterialTotal", 0);
        String encoded = tag.getString("QTMaterials").orElse("");
        for (String token : encoded.split(";")) {
            int split = token.lastIndexOf('=');
            if (split <= 0) continue;
            try {
                Identifier id = Identifier.parse(token.substring(0, split));
                int count = Integer.parseInt(token.substring(split + 1));
                var block = BuiltInRegistries.BLOCK.getValue(id);
                rows.add(new MaterialRow(block.getName().getString(), count));
            } catch (RuntimeException ignored) {
            }
        }
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

        gui.text(font, Component.literal(MaterialListTabletItem.isWritten(tablet) ? schematicName : "BLANK TABLET"),
                left + 20, top + 42, QuantumUiTheme.CYAN, false);
        gui.text(font, Component.literal(MaterialListTabletItem.isWritten(tablet)
                        ? total + " blocks · " + rows.size() + " material types"
                        : "Insert this tablet in the Constructor input"),
                left + 20, top + 57, QuantumUiTheme.TEXT_SOFT, false);

        QuantumUiTheme.panel(gui, left + 18, top + 68, left + 68, top + 82, QuantumUiTheme.CYAN, 0xFF0A2029);
        gui.text(font, Component.literal("ALL"), left + 34, top + 71, QuantumUiTheme.CYAN, false);
        QuantumUiTheme.panel(gui, left + 72, top + 68, left + 132, top + 82, QuantumUiTheme.BORDER_DIM, 0xFF0A151C);
        gui.text(font, Component.literal("MISSING"), left + 79, top + 71, QuantumUiTheme.TEXT_SOFT, false);
        QuantumUiTheme.panel(gui, left + 136, top + 68, left + 207, top + 82, QuantumUiTheme.BORDER_DIM, 0xFF0A151C);
        gui.text(font, Component.literal("AVAILABLE"), left + 142, top + 71, QuantumUiTheme.TEXT_SOFT, false);

        int rowY = top + 88;
        for (int row = 0; row < 8; row++) {
            int y = rowY + row * 22;
            QuantumUiTheme.panel(gui, left + 18, y, left + PANEL_W - 28, y + 19,
                    QuantumUiTheme.BORDER_DIM, row % 2 == 0 ? 0xFF0A1820 : 0xFF0C1C25);
            QuantumUiTheme.slotFrame(gui, left + 21, y + 1, row < rows.size(), QuantumUiTheme.CYAN);
            if (row < rows.size()) {
                MaterialRow material = rows.get(row);
                gui.text(font, Component.literal(trim(material.name(), 20)), left + 47, y + 4,
                        QuantumUiTheme.TEXT, false);
                gui.text(font, Component.literal("0 / " + material.required()), left + 47, y + 11,
                        QuantumUiTheme.RED, false);
            }
        }

        // Thin textured scrollbar from the sketch.
        int sx = left + PANEL_W - 13;
        gui.fill(sx, top + 34, sx + 4, top + PANEL_H - 16, QuantumUiTheme.DEEP);
        gui.fill(sx + 1, top + 36, sx + 3, top + 92, QuantumUiTheme.CYAN_DIM);
        gui.fill(sx + 1, top + 38, sx + 2, top + 90, QuantumUiTheme.CYAN);
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private record MaterialRow(String name, int required) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
