package mcjty.rftoolsbuilder.constructor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Compact, mouse-only placement panel. It intentionally leaves most of the
 * world visible so the hologram can be aligned while the controls are open.
 */
public final class SchematicPlacementScreen extends Screen {
    private static final int PANEL_WIDTH = 236;
    private static final int BG = 0xE60A1118;
    private static final int PANEL = 0xE613222C;
    private static final int BORDER = 0xFF24576A;
    private static final int CYAN = 0xFF20D9F3;
    private static final int TEXT = 0xFFE8F5F7;
    private static final int MUTED = 0xFF8CA6AE;
    private static final int GREEN = 0xFF69E6A0;
    private static final int ORANGE = 0xFFF1A052;
    private static final int RED = 0xFFFF6670;

    private boolean finished;

    public SchematicPlacementScreen() {
        super(Component.translatable("screen.rftoolsbuilder.schematic_placement"));
    }

    @Override
    protected void init() {
        super.init();

        int x = panelLeft() + 12;
        int y = 102;

        addAxisRow(x, y, 1, 0, 0);
        addAxisRow(x, y + 25, 0, 1, 0);
        addAxisRow(x, y + 50, 0, 0, 1);

        int rotationY = y + 86;
        addButton("0°", x + 20, rotationY, 43, () -> SchematicPlacementHandler.setRotation(0));
        addButton("90°", x + 67, rotationY, 43, () -> SchematicPlacementHandler.setRotation(1));
        addButton("180°", x + 114, rotationY, 43, () -> SchematicPlacementHandler.setRotation(2));
        addButton("270°", x + 161, rotationY, 43, () -> SchematicPlacementHandler.setRotation(3));

        int mirrorY = rotationY + 25;
        addButton(Component.translatable("screen.rftoolsbuilder.placement.mirror_none"), x + 20, mirrorY, 58,
                () -> SchematicPlacementHandler.setMirror(0));
        addButton(Component.translatable("screen.rftoolsbuilder.placement.mirror_lr"), x + 82, mirrorY, 58,
                () -> SchematicPlacementHandler.setMirror(1));
        addButton(Component.translatable("screen.rftoolsbuilder.placement.mirror_fb"), x + 144, mirrorY, 60,
                () -> SchematicPlacementHandler.setMirror(2));

        int placementY = mirrorY + 34;
        addButton(Component.translatable("screen.rftoolsbuilder.placement.target"), x, placementY, 66,
                SchematicPlacementHandler::placeAtLook);
        addButton(Component.translatable("screen.rftoolsbuilder.placement.front"), x + 70, placementY, 66,
                SchematicPlacementHandler::placeInFront);
        addButton(Component.translatable("screen.rftoolsbuilder.placement.center"), x + 140, placementY, 66,
                SchematicPlacementHandler::centerOnPlayer);

        addButton(Component.translatable("screen.rftoolsbuilder.placement.reset"), x, placementY + 25, 206,
                SchematicPlacementHandler::reset);

        int bottom = Math.max(placementY + 58, height - 36);
        addButton(Component.translatable("screen.rftoolsbuilder.placement.clear"), x, bottom, 62, this::clear);
        addButton(Component.translatable("screen.rftoolsbuilder.placement.cancel"), x + 66, bottom, 66, this::cancel);
        addButton(Component.translatable("screen.rftoolsbuilder.placement.confirm"), x + 136, bottom, 70, this::confirm);
    }

    private void addAxisRow(int x, int y, int dx, int dy, int dz) {
        addButton("-10", x + 20, y, 43, () -> SchematicPlacementHandler.nudge(-10 * dx, -10 * dy, -10 * dz));
        addButton("-1", x + 67, y, 43, () -> SchematicPlacementHandler.nudge(-dx, -dy, -dz));
        addButton("+1", x + 114, y, 43, () -> SchematicPlacementHandler.nudge(dx, dy, dz));
        addButton("+10", x + 161, y, 43, () -> SchematicPlacementHandler.nudge(10 * dx, 10 * dy, 10 * dz));
    }

    private void addButton(String label, int x, int y, int width, Runnable action) {
        addButton(Component.literal(label), x, y, width, action);
    }

    private void addButton(Component label, int x, int y, int width, Runnable action) {
        addRenderableWidget(Button.builder(label, button -> action.run()).bounds(x, y, width, 20).build());
    }

    private int panelLeft() {
        return Math.max(8, width - PANEL_WIDTH - 8);
    }

    private void confirm() {
        if (!SchematicPlacementHandler.confirm()) return;
        finished = true;
        closeScreen();
    }

    private void clear() {
        if (!SchematicPlacementHandler.clearDeployment()) return;
        finished = true;
        closeScreen();
    }

    private void cancel() {
        if (!finished) SchematicPlacementHandler.cancel();
        finished = true;
        closeScreen();
    }

    private void closeScreen() {
        Minecraft mc = minecraft;
        if (mc != null) mc.setScreen(null);
    }

    @Override
    public void onClose() {
        if (!finished) {
            SchematicPlacementHandler.cancel();
            finished = true;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int right = width - 8;
        int top = 8;
        int bottom = height - 8;

        gui.fill(left, top, right, bottom, BORDER);
        gui.fill(left + 1, top + 1, right - 1, bottom - 1, BG);
        gui.fill(left + 5, top + 30, right - 5, top + 92, PANEL);

        gui.text(font, Component.translatable("screen.rftoolsbuilder.schematic_placement"), left + 12, top + 10, CYAN, false);

        BlockPos anchor = SchematicPlacementHandler.anchor();
        gui.text(font, Component.literal("X " + anchor.getX() + "   Y " + anchor.getY() + "   Z " + anchor.getZ()),
                left + 12, top + 39, TEXT, false);

        gui.text(font, Component.translatable("screen.rftoolsbuilder.placement.size",
                        SchematicPlacementHandler.sizeX(),
                        SchematicPlacementHandler.sizeY(),
                        SchematicPlacementHandler.sizeZ()),
                left + 12, top + 54, MUTED, false);

        gui.text(font, Component.translatable("screen.rftoolsbuilder.placement.rotation",
                        SchematicPlacementHandler.rotationDegrees()),
                left + 12, top + 68, MUTED, false);
        gui.text(font, Component.translatable("screen.rftoolsbuilder.placement.mirror",
                        SchematicPlacementHandler.mirrorName()),
                left + 110, top + 68, MUTED, false);

        int previewColor = SchematicPlacementHandler.previewReady() ? GREEN : ORANGE;
        Component preview = SchematicPlacementHandler.previewReady()
                ? Component.translatable("screen.rftoolsbuilder.placement.preview_ready",
                        SchematicPlacementHandler.previewBlockCount())
                : Component.translatable("screen.rftoolsbuilder.placement.preview_loading");
        gui.text(font, preview, left + 12, top + 82, previewColor, false);

        gui.text(font, Component.literal("X"), left + 15, 108, CYAN, false);
        gui.text(font, Component.literal("Y"), left + 15, 133, CYAN, false);
        gui.text(font, Component.literal("Z"), left + 15, 158, CYAN, false);

        gui.text(font, Component.translatable("screen.rftoolsbuilder.placement.rotation_label"), left + 12, 184, MUTED, false);
        gui.text(font, Component.translatable("screen.rftoolsbuilder.placement.mirror_label"), left + 12, 209, MUTED, false);

        if (!SchematicPlacementHandler.hasValidCard()) {
            gui.text(font, Component.translatable("screen.rftoolsbuilder.placement.card_missing"), left + 12, bottom - 50, RED, false);
        }

        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }
}
