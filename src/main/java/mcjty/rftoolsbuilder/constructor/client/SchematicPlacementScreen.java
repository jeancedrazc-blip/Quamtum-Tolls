package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.client.ui.QuantumButton;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * World-visible schematic editor inspired by Create's tool workflow.
 *
 * Instead of exposing one dense coordinate form, editing is split into clear
 * manipulation modes. The hologram stays visible behind the non-pausing HUD;
 * exact step controls remain available under PRECISE for deterministic builds.
 */
public final class SchematicPlacementScreen extends Screen {
    private static final int DOCK_HEIGHT = 124;
    private static final int TOOL_W = 74;
    private static final int TOOL_GAP = 3;

    private final Map<SchematicPlacementTool, QuantumButton> toolButtons = new EnumMap<>(SchematicPlacementTool.class);
    private final List<QuantumButton> contextButtons = new ArrayList<>();
    private final Map<QuantumButton, SchematicPlacementTool> contextOwners = new HashMap<>();
    private boolean finished;

    public SchematicPlacementScreen() {
        super(Component.translatable("screen.rftoolsbuilder.schematic_placement"));
    }

    @Override
    protected void init() {
        super.init();
        contextButtons.clear();
        contextOwners.clear();
        toolButtons.clear();

        int dockWidth = Math.min(width - 16, 550);
        int left = (width - dockWidth) / 2;
        int top = height - DOCK_HEIGHT - 8;
        int toolTotal = SchematicPlacementTool.values().length * TOOL_W
                + (SchematicPlacementTool.values().length - 1) * TOOL_GAP;
        int toolX = left + Math.max(8, (dockWidth - toolTotal) / 2);

        int i = 0;
        for (SchematicPlacementTool placementTool : SchematicPlacementTool.values()) {
            int bx = toolX + i * (TOOL_W + TOOL_GAP);
            QuantumButton button = new QuantumButton(bx, top + 22, TOOL_W, 18, placementTool.label(),
                    () -> selectTool(placementTool), () -> SchematicPlacementHandler.tool() == placementTool,
                    QuantumUiTheme.AMBER);
            toolButtons.put(placementTool, addRenderableWidget(button));
            i++;
        }

        // DEPLOY
        context(SchematicPlacementTool.DEPLOY, left + 14, top + 53, 96, "AT TARGET", SchematicPlacementHandler::placeAtLook);
        context(SchematicPlacementTool.DEPLOY, left + 114, top + 53, 96, "IN FRONT", SchematicPlacementHandler::placeInFront);
        context(SchematicPlacementTool.DEPLOY, left + 214, top + 53, 96, "CENTER PLAYER", SchematicPlacementHandler::centerOnPlayer);

        // MOVE X/Z in schematic-local coordinates, matching Create's transform semantics.
        context(SchematicPlacementTool.MOVE_XZ, left + 14, top + 53, 70, "X -1", () -> SchematicPlacementHandler.nudgeLocal(-1, 0));
        context(SchematicPlacementTool.MOVE_XZ, left + 88, top + 53, 70, "X +1", () -> SchematicPlacementHandler.nudgeLocal(1, 0));
        context(SchematicPlacementTool.MOVE_XZ, left + 162, top + 53, 70, "Z -1", () -> SchematicPlacementHandler.nudgeLocal(0, -1));
        context(SchematicPlacementTool.MOVE_XZ, left + 236, top + 53, 70, "Z +1", () -> SchematicPlacementHandler.nudgeLocal(0, 1));
        context(SchematicPlacementTool.MOVE_XZ, left + 14, top + 76, 70, "X -10", () -> SchematicPlacementHandler.nudgeLocal(-10, 0));
        context(SchematicPlacementTool.MOVE_XZ, left + 88, top + 76, 70, "X +10", () -> SchematicPlacementHandler.nudgeLocal(10, 0));
        context(SchematicPlacementTool.MOVE_XZ, left + 162, top + 76, 70, "Z -10", () -> SchematicPlacementHandler.nudgeLocal(0, -10));
        context(SchematicPlacementTool.MOVE_XZ, left + 236, top + 76, 70, "Z +10", () -> SchematicPlacementHandler.nudgeLocal(0, 10));

        // MOVE Y
        context(SchematicPlacementTool.MOVE_Y, left + 14, top + 53, 70, "Y -10", () -> SchematicPlacementHandler.nudge(0, -10, 0));
        context(SchematicPlacementTool.MOVE_Y, left + 88, top + 53, 70, "Y -1", () -> SchematicPlacementHandler.nudge(0, -1, 0));
        context(SchematicPlacementTool.MOVE_Y, left + 162, top + 53, 70, "Y +1", () -> SchematicPlacementHandler.nudge(0, 1, 0));
        context(SchematicPlacementTool.MOVE_Y, left + 236, top + 53, 70, "Y +10", () -> SchematicPlacementHandler.nudge(0, 10, 0));

        // ROTATE
        context(SchematicPlacementTool.ROTATE, left + 14, top + 53, 92, "↶ 90°", () -> SchematicPlacementHandler.rotate90(false));
        context(SchematicPlacementTool.ROTATE, left + 110, top + 53, 92, "↷ 90°", () -> SchematicPlacementHandler.rotate90(true));
        context(SchematicPlacementTool.ROTATE, left + 206, top + 53, 100, "180°", () -> SchematicPlacementHandler.setRotation(SchematicPlacementHandler.rotationQuarter() + 2));

        // MIRROR
        context(SchematicPlacementTool.MIRROR, left + 14, top + 53, 92, "MIRROR X", SchematicPlacementHandler::flipX);
        context(SchematicPlacementTool.MIRROR, left + 110, top + 53, 92, "NONE", () -> SchematicPlacementHandler.setMirror(0));
        context(SchematicPlacementTool.MIRROR, left + 206, top + 53, 100, "MIRROR Z", SchematicPlacementHandler::flipZ);

        // PRECISE: deterministic ±1/±10 adjustment on all three world axes.
        context(SchematicPlacementTool.PRECISE, left + 14, top + 53, 48, "X-10", () -> SchematicPlacementHandler.nudge(-10, 0, 0));
        context(SchematicPlacementTool.PRECISE, left + 65, top + 53, 42, "X-1", () -> SchematicPlacementHandler.nudge(-1, 0, 0));
        context(SchematicPlacementTool.PRECISE, left + 110, top + 53, 42, "X+1", () -> SchematicPlacementHandler.nudge(1, 0, 0));
        context(SchematicPlacementTool.PRECISE, left + 155, top + 53, 48, "X+10", () -> SchematicPlacementHandler.nudge(10, 0, 0));
        context(SchematicPlacementTool.PRECISE, left + 14, top + 76, 48, "Z-10", () -> SchematicPlacementHandler.nudge(0, 0, -10));
        context(SchematicPlacementTool.PRECISE, left + 65, top + 76, 42, "Z-1", () -> SchematicPlacementHandler.nudge(0, 0, -1));
        context(SchematicPlacementTool.PRECISE, left + 110, top + 76, 42, "Z+1", () -> SchematicPlacementHandler.nudge(0, 0, 1));
        context(SchematicPlacementTool.PRECISE, left + 155, top + 76, 48, "Z+10", () -> SchematicPlacementHandler.nudge(0, 0, 10));
        context(SchematicPlacementTool.PRECISE, left + 209, top + 53, 48, "Y-10", () -> SchematicPlacementHandler.nudge(0, -10, 0));
        context(SchematicPlacementTool.PRECISE, left + 260, top + 53, 42, "Y-1", () -> SchematicPlacementHandler.nudge(0, -1, 0));
        context(SchematicPlacementTool.PRECISE, left + 209, top + 76, 42, "Y+1", () -> SchematicPlacementHandler.nudge(0, 1, 0));
        context(SchematicPlacementTool.PRECISE, left + 254, top + 76, 48, "Y+10", () -> SchematicPlacementHandler.nudge(0, 10, 0));

        // Global controls remain available regardless of tool.
        addRenderableWidget(new QuantumButton(left + dockWidth - 224, top + 100, 50, 18,
                Component.literal("RESET"), SchematicPlacementHandler::reset));
        addRenderableWidget(new QuantumButton(left + dockWidth - 170, top + 100, 50, 18,
                Component.literal("CLEAR"), this::clear, () -> false, QuantumUiTheme.RED));
        addRenderableWidget(new QuantumButton(left + dockWidth - 116, top + 100, 50, 18,
                Component.literal("CANCEL"), this::cancel, () -> false, QuantumUiTheme.RED));
        addRenderableWidget(new QuantumButton(left + dockWidth - 62, top + 100, 54, 18,
                Component.literal("APPLY"), this::confirm, () -> false, QuantumUiTheme.GREEN));

        updateContextVisibility();
    }

    private void context(SchematicPlacementTool owner, int x, int y, int width, String label, Runnable action) {
        QuantumButton button = new QuantumButton(x, y, width, 18, Component.literal(label), action);
        contextButtons.add(addRenderableWidget(button));
        contextOwners.put(button, owner);
    }

    private void selectTool(SchematicPlacementTool selected) {
        SchematicPlacementHandler.setTool(selected);
        updateContextVisibility();
    }

    private void updateContextVisibility() {
        for (QuantumButton button : contextButtons) {
            button.visible = contextOwners.get(button) == SchematicPlacementHandler.tool();
        }
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
        int dockWidth = Math.min(width - 16, 550);
        int dockLeft = (width - dockWidth) / 2;
        int dockTop = height - DOCK_HEIGHT - 8;

        QuantumUiTheme.window(gui, dockLeft, dockTop, dockWidth, DOCK_HEIGHT);
        QuantumUiTheme.title(gui, font, Component.literal("SCHEMATIC TRANSFORM"), dockLeft + dockWidth / 2, dockTop + 7);

        // Compact telemetry module stays away from the center of the world preview.
        int infoX = 8;
        int infoY = 8;
        int infoW = 224;
        int infoH = 96;
        QuantumUiTheme.panel(gui, infoX, infoY, infoX + infoW, infoY + infoH,
                QuantumUiTheme.BORDER, 0xE609131B);
        gui.text(font, Component.literal("SCHEMATIC / LIVE PREVIEW"), infoX + 9, infoY + 8, QuantumUiTheme.CYAN, false);

        BlockPos anchor = SchematicPlacementHandler.anchor();
        gui.text(font, Component.literal("ANCHOR"), infoX + 9, infoY + 25, QuantumUiTheme.MUTED, false);
        gui.text(font, Component.literal("X " + anchor.getX() + "   Y " + anchor.getY() + "   Z " + anchor.getZ()),
                infoX + 55, infoY + 25, QuantumUiTheme.TEXT, false);

        gui.text(font, Component.literal("SIZE"), infoX + 9, infoY + 39, QuantumUiTheme.MUTED, false);
        gui.text(font, Component.literal(SchematicPlacementHandler.sizeX() + " × "
                        + SchematicPlacementHandler.sizeY() + " × " + SchematicPlacementHandler.sizeZ()),
                infoX + 55, infoY + 39, QuantumUiTheme.TEXT_SOFT, false);

        gui.text(font, Component.literal("ROT"), infoX + 9, infoY + 53, QuantumUiTheme.MUTED, false);
        gui.text(font, Component.literal(SchematicPlacementHandler.rotationDegrees() + "°"),
                infoX + 55, infoY + 53, QuantumUiTheme.TEXT_SOFT, false);
        gui.text(font, Component.literal("MIRROR"), infoX + 94, infoY + 53, QuantumUiTheme.MUTED, false);
        gui.text(font, Component.literal(SchematicPlacementHandler.mirrorName()),
                infoX + 145, infoY + 53, QuantumUiTheme.TEXT_SOFT, false);

        int readyColor = SchematicPlacementHandler.previewReady() ? QuantumUiTheme.GREEN : QuantumUiTheme.AMBER;
        QuantumUiTheme.statusLamp(gui, infoX + 9, infoY + 70, readyColor, SchematicPlacementHandler.previewReady());
        String preview = SchematicPlacementHandler.previewReady()
                ? SchematicPlacementHandler.previewBlockCount() + " blocks / " + SchematicPlacementHandler.previewEntityCount() + " entities"
                : "Loading preview…";
        gui.text(font, Component.literal(preview), infoX + 22, infoY + 69, readyColor, false);

        SchematicPlacementTool activeTool = SchematicPlacementHandler.tool();
        gui.text(font, activeTool.description(), dockLeft + 14, dockTop + 103, QuantumUiTheme.MUTED, false);

        if (!SchematicPlacementHandler.hasValidCard()) {
            gui.text(font, Component.literal("SCHEMATIC CARD LOST — changes cannot be applied"),
                    dockLeft + 14, dockTop + 88, QuantumUiTheme.RED, false);
        }

        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }
}
