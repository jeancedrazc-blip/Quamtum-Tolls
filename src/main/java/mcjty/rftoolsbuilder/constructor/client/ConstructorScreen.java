package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorMenu;
import mcjty.rftoolsbuilder.constructor.ConstructorReplaceMode;
import mcjty.rftoolsbuilder.constructor.ConstructorStatus;
import mcjty.rftoolsbuilder.constructor.SchematicCardItem;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumButton;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Constructor control terminal using the shared Quantum Tools UI system. */
public final class ConstructorScreen extends AbstractContainerScreen<ConstructorMenu> {
    private QuantumButton startButton;
    private QuantumButton pauseButton;
    private QuantumButton stopButton;

    public ConstructorScreen(ConstructorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 272, 288);
        this.titleLabelY = 7;
        this.inventoryLabelY = 198;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        startButton = addRenderableWidget(new QuantumButton(x + 12, y + 145, 72, 19,
                Component.literal("▶ START"), () -> sendButton(2),
                () -> menu.data().get(6) != 0, QuantumUiTheme.GREEN));
        pauseButton = addRenderableWidget(new QuantumButton(x + 88, y + 145, 84, 19,
                Component.literal("Ⅱ PAUSE"), () -> sendButton(0),
                () -> status(menu.data().get(2)) == ConstructorStatus.PAUSED, QuantumUiTheme.AMBER));
        stopButton = addRenderableWidget(new QuantumButton(x + 176, y + 145, 84, 19,
                Component.literal("■ STOP"), () -> sendButton(1),
                () -> false, QuantumUiTheme.RED));

        addModeButton(x + 12, y + 169, 57, "AIR ONLY", ConstructorReplaceMode.DONT_REPLACE);
        addModeButton(x + 72, y + 169, 57, "SOLID", ConstructorReplaceMode.REPLACE_SOLID);
        addModeButton(x + 132, y + 169, 57, "ANY", ConstructorReplaceMode.REPLACE_ANY);
        addModeButton(x + 192, y + 169, 68, "+ AIR", ConstructorReplaceMode.REPLACE_EMPTY);

        addRenderableWidget(new QuantumButton(x + 12, y + 188, 104, 15,
                Component.literal("SKIP MISSING"), () -> sendButton(4),
                () -> menu.data().get(10) != 0, QuantumUiTheme.AMBER));
        addRenderableWidget(new QuantumButton(x + 120, y + 188, 140, 15,
                Component.literal("REPLACE BLOCK ENTITIES"), () -> sendButton(5),
                () -> menu.data().get(11) != 0, QuantumUiTheme.AMBER));
    }

    private void addModeButton(int x, int y, int width, String label, ConstructorReplaceMode mode) {
        addRenderableWidget(new QuantumButton(x, y, width, 17, Component.literal(label),
                () -> setReplaceMode(mode), () -> replaceMode(menu.data().get(9)) == mode,
                QuantumUiTheme.CYAN));
    }

    private void setReplaceMode(ConstructorReplaceMode requested) {
        int current = replaceMode(menu.data().get(9)).ordinal();
        int target = requested.ordinal();
        int count = ConstructorReplaceMode.values().length;
        int steps = Math.floorMod(target - current, count);
        for (int i = 0; i < steps; i++) sendButton(3);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ConstructorStatus state = status(menu.data().get(2));
        boolean hasCard = menu.data().get(8) != 0;
        boolean shotInFlight = state == ConstructorStatus.FIRING;
        boolean hasJob = menu.data().get(4) > 0 || state != ConstructorStatus.IDLE;

        if (startButton != null) {
            startButton.active = hasCard && !shotInFlight
                    && state != ConstructorStatus.AIMING && state != ConstructorStatus.CHARGING;
        }
        if (pauseButton != null) {
            pauseButton.active = hasJob && state != ConstructorStatus.COMPLETE && state != ConstructorStatus.ERROR;
            pauseButton.setMessage(Component.literal(state == ConstructorStatus.PAUSED ? "▶ RESUME" : "Ⅱ PAUSE"));
        }
        if (stopButton != null) stopButton.active = hasJob && !shotInFlight;
    }

    private void sendButton(int id) {
        Minecraft mc = this.minecraft;
        if (mc != null && mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;

        QuantumUiTheme.window(gui, x, y, imageWidth, imageHeight);
        QuantumUiTheme.title(gui, font, Component.literal("CONSTRUCTOR // BUILD TERMINAL"), x + imageWidth / 2, y + 8);
        gui.fill(x + 8, y + 23, x + imageWidth - 8, y + 24, QuantumUiTheme.BORDER_DIM);

        QuantumUiTheme.panel(gui, x + 10, y + 31, x + 86, y + 138);
        QuantumUiTheme.panel(gui, x + 90, y + 31, x + 190, y + 138);
        QuantumUiTheme.panel(gui, x + 194, y + 31, x + 262, y + 138);

        drawEnergy(gui, x, y);
        drawJob(gui, x, y);
        drawTarget(gui, x, y);

        QuantumUiTheme.sectionHeader(gui, font, Component.literal("BUILD CONTROL"), x + 12, y + 137, 248);
        QuantumUiTheme.sectionHeader(gui, font, Component.literal("PLACEMENT POLICY"), x + 12, y + 164, 248);

        QuantumUiTheme.slotFrame(gui, x + 112, y + 116, menu.data().get(8) != 0, QuantumUiTheme.CYAN);

        // Machine controls end at y=203. Keep a hard visual separation before
        // the first vanilla inventory row at y=208 so widgets never overlap slots.
        gui.fill(x + 8, y + 204, x + imageWidth - 8, y + 205, QuantumUiTheme.BORDER_DIM);
        gui.text(font, Component.literal("PLAYER INVENTORY"), x + 48, y + 196, QuantumUiTheme.MUTED, false);
    }

    private void drawEnergy(GuiGraphicsExtractor gui, int x, int y) {
        int energy = Math.max(0, menu.data().get(0));
        int capacity = Math.max(1, menu.data().get(1));
        ConstructorStatus state = status(menu.data().get(2));

        gui.text(font, Component.literal("POWER CORE"), x + 16, y + 38, QuantumUiTheme.MUTED, false);
        QuantumUiTheme.verticalGauge(gui, x + 19, y + 53, 17, 60, energy, capacity, QuantumUiTheme.CYAN, 8);
        gui.text(font, Component.literal(formatFe(energy)), x + 42, y + 57, QuantumUiTheme.TEXT, false);
        gui.text(font, Component.literal(formatPercent(energy, capacity)), x + 42, y + 70, QuantumUiTheme.CYAN, false);
        gui.text(font, Component.literal("CAP"), x + 42, y + 87, QuantumUiTheme.MUTED, false);
        gui.text(font, Component.literal(formatFe(capacity)), x + 42, y + 99, QuantumUiTheme.TEXT_SOFT, false);

        int stateColor = statusColor(state);
        QuantumUiTheme.statusLamp(gui, x + 17, y + 119, stateColor, state != ConstructorStatus.IDLE);
        gui.text(font, Component.literal(statusText(state)), x + 30, y + 118, stateColor, false);
    }

    private void drawJob(GuiGraphicsExtractor gui, int x, int y) {
        int index = Math.max(0, menu.data().get(3));
        int total = Math.max(0, menu.data().get(4));
        int shot = Math.max(0, menu.data().get(5));
        int flightTicks = Math.max(1, menu.data().get(12));
        ConstructorStatus state = status(menu.data().get(2));

        gui.text(font, Component.literal("SCHEMATIC JOB"), x + 96, y + 38, QuantumUiTheme.MUTED, false);
        int shown = total <= 0 ? 0 : Math.min(total, index + (state == ConstructorStatus.COMPLETE ? 0 : 1));
        gui.text(font, Component.literal(shown + " / " + total), x + 96, y + 52, QuantumUiTheme.TEXT, false);

        int progress = state == ConstructorStatus.COMPLETE ? total : Math.min(total, index);
        QuantumUiTheme.segmentedBar(gui, x + 96, y + 66, 88, 10, progress, Math.max(1, total), QuantumUiTheme.CYAN, 10);

        BlockState target = targetState();
        gui.text(font, Component.literal("CURRENT"), x + 96, y + 82, QuantumUiTheme.MUTED, false);
        gui.text(font, Component.literal(target == null ? (menu.data().get(13) != 0 ? "entity" : "-") : shortId(target)),
                x + 96, y + 94, QuantumUiTheme.TEXT_SOFT, false);

        String card = cardName();
        gui.text(font, Component.literal("CARD"), x + 96, y + 108, QuantumUiTheme.MUTED, false);
        gui.text(font, Component.literal(trim(card, 15)), x + 136, y + 108, QuantumUiTheme.TEXT, false);

        if (state == ConstructorStatus.FIRING) {
            int shotPct = Math.min(100, shot * 100 / flightTicks);
            gui.text(font, Component.literal("PROJECTILE " + shotPct + "%"), x + 96, y + 122, QuantumUiTheme.GREEN, false);
        } else {
            int rules = replacementCount();
            gui.text(font, Component.literal(rules == 0 ? "no substitution rules" : rules + " substitution rule" + (rules == 1 ? "" : "s")),
                    x + 96, y + 122, rules == 0 ? QuantumUiTheme.MUTED : QuantumUiTheme.AMBER, false);
        }
    }

    private void drawTarget(GuiGraphicsExtractor gui, int x, int y) {
        int cost = Math.max(0, menu.data().get(7));
        BlockPos target = targetPos();
        ConstructorStatus state = status(menu.data().get(2));

        gui.text(font, Component.literal("TARGET"), x + 200, y + 38, QuantumUiTheme.MUTED, false);
        if (target == null) {
            gui.text(font, Component.literal("No target"), x + 200, y + 53, QuantumUiTheme.TEXT_SOFT, false);
        } else {
            gui.text(font, Component.literal("X " + target.getX()), x + 200, y + 52, QuantumUiTheme.TEXT, false);
            gui.text(font, Component.literal("Y " + target.getY()), x + 200, y + 64, QuantumUiTheme.TEXT, false);
            gui.text(font, Component.literal("Z " + target.getZ()), x + 200, y + 76, QuantumUiTheme.TEXT, false);
        }

        gui.text(font, Component.literal("ENERGY / SHOT"), x + 200, y + 92, QuantumUiTheme.MUTED, false);
        gui.text(font, Component.literal(formatFe(cost)), x + 200, y + 104, QuantumUiTheme.CYAN, false);

        if (state == ConstructorStatus.WAITING_MATERIAL) {
            gui.text(font, Component.literal("MISSING MATERIAL"), x + 200, y + 121, QuantumUiTheme.AMBER, false);
        } else if (state == ConstructorStatus.WAITING_ENERGY) {
            gui.text(font, Component.literal("ENERGY REQUIRED"), x + 200, y + 121, QuantumUiTheme.AMBER, false);
        } else if (state == ConstructorStatus.WAITING_CHUNK) {
            gui.text(font, Component.literal("CHUNK UNLOADED"), x + 200, y + 121, QuantumUiTheme.YELLOW, false);
        } else if (state == ConstructorStatus.BLOCKED) {
            gui.text(font, Component.literal("PLACEMENT BLOCKED"), x + 200, y + 121, QuantumUiTheme.RED, false);
        } else if (state == ConstructorStatus.ERROR) {
            gui.text(font, Component.literal("SCHEMATIC ERROR"), x + 200, y + 121, QuantumUiTheme.RED, false);
        }
    }

    private int replacementCount() {
        if (menu.constructor() == null) return 0;
        ItemStack card = menu.constructor().schematicCard();
        return card.getItem() instanceof SchematicCardItem ? SchematicCardItem.replacementCount(card) : 0;
    }

    private String cardName() {
        if (menu.constructor() == null) return "No card";
        ItemStack card = menu.constructor().schematicCard();
        if (!(card.getItem() instanceof SchematicCardItem)) return "No card";
        return SchematicCardItem.sourceName(card);
    }

    private BlockState targetState() { return menu.constructor() == null ? null : menu.constructor().targetState(); }
    private BlockPos targetPos() { return menu.constructor() == null ? null : menu.constructor().targetPos(); }

    private static String shortId(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        int colon = id.indexOf(':');
        String compact = colon >= 0 ? id.substring(colon + 1) : id;
        return compact.length() <= 13 ? compact : compact.substring(0, 12) + "…";
    }

    private static ConstructorStatus status(int value) {
        ConstructorStatus[] values = ConstructorStatus.values();
        return values[Math.max(0, Math.min(values.length - 1, value))];
    }

    private static ConstructorReplaceMode replaceMode(int value) {
        ConstructorReplaceMode[] values = ConstructorReplaceMode.values();
        return values[Math.max(0, Math.min(values.length - 1, value))];
    }

    private static String statusText(ConstructorStatus status) {
        return switch (status) {
            case IDLE -> "IDLE";
            case READY -> "READY";
            case AIMING -> "AIMING";
            case CHARGING -> "CHARGING";
            case FIRING -> "FIRING";
            case WAITING_ENERGY -> "WAIT FE";
            case WAITING_MATERIAL -> "WAIT ITEM";
            case WAITING_CHUNK -> "WAIT CHUNK";
            case PAUSED -> "PAUSED";
            case BLOCKED -> "BLOCKED";
            case COMPLETE -> "COMPLETE";
            case ERROR -> "ERROR";
        };
    }

    private static int statusColor(ConstructorStatus status) {
        return switch (status) {
            case AIMING, CHARGING, FIRING, READY -> QuantumUiTheme.CYAN;
            case COMPLETE -> QuantumUiTheme.GREEN;
            case WAITING_ENERGY, WAITING_MATERIAL, WAITING_CHUNK, PAUSED -> QuantumUiTheme.AMBER;
            case BLOCKED, ERROR -> QuantumUiTheme.RED;
            default -> QuantumUiTheme.TEXT_SOFT;
        };
    }

    private static String formatFe(int value) {
        if (value >= 1_000_000) return String.format("%.2f MFE", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1f kFE", value / 1_000.0);
        return value + " FE";
    }

    private static String formatPercent(int value, int max) {
        if (max <= 0) return "0%";
        return Math.min(100, Math.max(0, (int) ((long) value * 100 / max))) + "%";
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
    }
}
