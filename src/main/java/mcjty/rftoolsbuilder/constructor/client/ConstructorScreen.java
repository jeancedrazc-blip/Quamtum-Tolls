package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorBlockEntity;
import mcjty.rftoolsbuilder.constructor.ConstructorMenu;
import mcjty.rftoolsbuilder.constructor.ConstructorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;

public final class ConstructorScreen extends AbstractContainerScreen<ConstructorMenu> {
    private static final int BG = 0xFF071018;
    private static final int PANEL = 0xFF0D1B26;
    private static final int PANEL_2 = 0xFF122735;
    private static final int BORDER = 0xFF1F5366;
    private static final int CYAN = 0xFF1CD6F2;
    private static final int ORANGE = 0xFFF18432;
    private static final int RED = 0xFFFF5A63;
    private static final int GREEN = 0xFF67E39A;
    private static final int TEXT = 0xFFE7F7FA;
    private static final int MUTED = 0xFF8EA9B2;
    private static final int DARK = 0xFF050A0E;

    public ConstructorScreen(ConstructorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 272, 184);
        this.titleLabelY = 7;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        addRenderableWidget(Button.builder(Component.literal("PAUSE / RESUME"), b -> sendButton(0))
                .bounds(x + 12, y + 150, 116, 20).build());
        addRenderableWidget(Button.builder(Component.literal("CLEAR"), b -> sendButton(1))
                .bounds(x + 144, y + 150, 116, 20).build());
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
        gui.centeredText(font, Component.literal("CONSTRUCTOR CONTROL"), x + imageWidth / 2, y + 8, CYAN);
        gui.fill(x + 8, y + 22, x + imageWidth - 8, y + 23, BORDER);

        panel(gui, x + 10, y + 30, x + 86, y + 140);
        panel(gui, x + 90, y + 30, x + 190, y + 140);
        panel(gui, x + 194, y + 30, x + 262, y + 140);

        drawEnergy(gui, x, y);
        drawJob(gui, x, y);
        drawTarget(gui, x, y);
    }

    private void drawEnergy(GuiGraphicsExtractor gui, int x, int y) {
        int energy = Math.max(0, menu.data().get(0));
        int capacity = Math.max(1, menu.data().get(1));
        ConstructorStatus status = status(menu.data().get(2));

        gui.text(font, Component.literal("ENERGY"), x + 16, y + 36, MUTED);

        int bx1 = x + 22;
        int by1 = y + 52;
        int bx2 = x + 42;
        int by2 = y + 126;
        gui.fill(bx1, by1, bx2, by2, DARK);
        gui.fill(bx1 + 1, by1 + 1, bx2 - 1, by2 - 1, 0xFF0A2630);

        int innerHeight = by2 - by1 - 2;
        int fill = (int) ((long) innerHeight * energy / capacity);
        gui.fill(bx1 + 1, by2 - 1 - fill, bx2 - 1, by2 - 1, CYAN);

        gui.text(font, Component.literal(formatFe(energy)), x + 47, y + 62, TEXT);
        gui.text(font, Component.literal("/ " + formatFe(capacity)), x + 47, y + 74, MUTED);
        gui.text(font, Component.literal("STATUS"), x + 16, y + 112, MUTED);
        gui.text(font, Component.literal(statusText(status)), x + 16, y + 124, statusColor(status));
    }

    private void drawJob(GuiGraphicsExtractor gui, int x, int y) {
        int index = Math.max(0, menu.data().get(3));
        int total = Math.max(0, menu.data().get(4));
        int shot = Math.max(0, menu.data().get(5));
        boolean running = menu.data().get(6) != 0;

        gui.text(font, Component.literal("SCHEMATIC / JOB"), x + 96, y + 36, MUTED);

        int shown = total <= 0 ? 0 : Math.min(total, index + 1);
        gui.text(font, Component.literal("Block " + shown + " / " + total), x + 96, y + 52, TEXT);

        int px1 = x + 96;
        int py1 = y + 68;
        int px2 = x + 184;
        int py2 = y + 78;
        gui.fill(px1, py1, px2, py2, DARK);
        gui.fill(px1 + 1, py1 + 1, px2 - 1, py2 - 1, 0xFF0A2630);
        int progressWidth = total <= 0 ? 0 : (int) ((long) (px2 - px1 - 2) * Math.min(total, index) / total);
        if (status(menu.data().get(2)) == ConstructorStatus.COMPLETE) progressWidth = px2 - px1 - 2;
        gui.fill(px1 + 1, py1 + 1, px1 + 1 + progressWidth, py2 - 1, CYAN);

        BlockState target = targetState();
        gui.text(font, Component.literal("CURRENT BLOCK"), x + 96, y + 88, MUTED);
        gui.text(font, Component.literal(target == null ? "-" : shortId(target)), x + 96, y + 100, TEXT);

        gui.text(font, Component.literal("SHOT"), x + 96, y + 116, MUTED);
        int shotPct = Math.min(100, shot * 100 / Math.max(1, ConstructorBlockEntity.FLIGHT_TICKS));
        gui.text(font, Component.literal((running ? "ACTIVE  " : "HALTED  ") + shotPct + "%"), x + 96, y + 128,
                running ? GREEN : ORANGE);
    }

    private void drawTarget(GuiGraphicsExtractor gui, int x, int y) {
        int cost = Math.max(0, menu.data().get(7));
        BlockPos target = targetPos();

        gui.text(font, Component.literal("TARGET"), x + 200, y + 36, MUTED);
        if (target == null) {
            gui.text(font, Component.literal("No target"), x + 200, y + 52, TEXT);
        } else {
            gui.text(font, Component.literal("X " + target.getX()), x + 200, y + 52, TEXT);
            gui.text(font, Component.literal("Y " + target.getY()), x + 200, y + 64, TEXT);
            gui.text(font, Component.literal("Z " + target.getZ()), x + 200, y + 76, TEXT);
        }

        gui.text(font, Component.literal("COST / SHOT"), x + 200, y + 96, MUTED);
        gui.text(font, Component.literal(formatFe(cost)), x + 200, y + 108, CYAN);

        ConstructorStatus s = status(menu.data().get(2));
        if (s == ConstructorStatus.WAITING_MATERIAL) {
            gui.text(font, Component.literal("MATERIAL"), x + 200, y + 122, ORANGE);
        } else if (s == ConstructorStatus.WAITING_ENERGY) {
            gui.text(font, Component.literal("LOW FE"), x + 200, y + 122, ORANGE);
        } else if (s == ConstructorStatus.BLOCKED) {
            gui.text(font, Component.literal("BLOCKED"), x + 200, y + 122, RED);
        }
    }

    private BlockState targetState() {
        return menu.constructor() == null ? null : menu.constructor().targetState();
    }

    private BlockPos targetPos() {
        return menu.constructor() == null ? null : menu.constructor().targetPos();
    }

    private static String shortId(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        int colon = id.indexOf(':');
        String compact = colon >= 0 ? id.substring(colon + 1) : id;
        return compact.length() <= 15 ? compact : compact.substring(0, 14) + "…";
    }

    private static ConstructorStatus status(int value) {
        ConstructorStatus[] values = ConstructorStatus.values();
        return values[Math.max(0, Math.min(values.length - 1, value))];
    }

    private static String statusText(ConstructorStatus status) {
        return switch (status) {
            case IDLE -> "Idle";
            case READY -> "Ready";
            case AIMING -> "Aiming";
            case CHARGING -> "Charging";
            case FIRING -> "Firing";
            case WAITING_ENERGY -> "Waiting FE";
            case WAITING_MATERIAL -> "Waiting material";
            case WAITING_CHUNK -> "Waiting chunk";
            case PAUSED -> "Paused";
            case BLOCKED -> "Blocked";
            case COMPLETE -> "Complete";
            case ERROR -> "Error";
        };
    }

    private static int statusColor(ConstructorStatus status) {
        return switch (status) {
            case AIMING, CHARGING, FIRING, READY -> CYAN;
            case COMPLETE -> GREEN;
            case WAITING_ENERGY, WAITING_MATERIAL, WAITING_CHUNK, PAUSED -> ORANGE;
            case BLOCKED, ERROR -> RED;
            default -> TEXT;
        };
    }

    private static String formatFe(int value) {
        if (value >= 1_000_000) return String.format("%.2f MFE", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1f kFE", value / 1_000.0);
        return value + " FE";
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
    }
}
