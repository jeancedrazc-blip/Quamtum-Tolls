package mcjty.rftoolsbuilder.constructor.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Shared visual language for every Quantum Tools screen.
 *
 * The theme deliberately keeps Minecraft's pixel readability while using a
 * dense industrial HUD hierarchy: graphite surfaces, cyan structural accents,
 * amber interaction accents and semantic green/red state colors.
 */
public final class QuantumUiTheme {
    public static final int BG = 0xFF05090D;
    public static final int SURFACE = 0xFF09131B;
    public static final int SURFACE_2 = 0xFF0D1C26;
    public static final int SURFACE_3 = 0xFF102631;
    public static final int DEEP = 0xFF020609;

    public static final int BORDER = 0xFF1E4D5E;
    public static final int BORDER_DIM = 0xFF15343F;
    public static final int CYAN = 0xFF21DAF4;
    public static final int CYAN_DIM = 0xFF147A8C;
    public static final int AMBER = 0xFFF19A45;
    public static final int GREEN = 0xFF62E39A;
    public static final int RED = 0xFFFF6670;
    public static final int YELLOW = 0xFFF0D169;

    public static final int TEXT = 0xFFE8F5F7;
    public static final int TEXT_SOFT = 0xFFB6CDD3;
    public static final int MUTED = 0xFF7898A2;
    public static final int DISABLED = 0xFF42545A;

    private QuantumUiTheme() {}

    public static void window(GuiGraphicsExtractor gui, int x, int y, int w, int h) {
        gui.fill(x, y, x + w, y + h, BG);
        gui.fill(x + 2, y + 2, x + w - 2, y + h - 2, SURFACE_2);
        cornerCuts(gui, x, y, w, h, BG);
        frame(gui, x + 2, y + 2, x + w - 2, y + h - 2, BORDER_DIM);
    }

    public static void panel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        panel(gui, x1, y1, x2, y2, BORDER, SURFACE);
    }

    public static void panel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, int border, int fill) {
        gui.fill(x1, y1, x2, y2, border);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
        // Small clipped corners keep the HUD technical without becoming anti-vanilla.
        gui.fill(x1, y1, x1 + 3, y1 + 1, BG);
        gui.fill(x2 - 3, y1, x2, y1 + 1, BG);
        gui.fill(x1, y2 - 1, x1 + 3, y2, BG);
        gui.fill(x2 - 3, y2 - 1, x2, y2, BG);
    }

    public static void frame(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, int color) {
        gui.fill(x1, y1, x2, y1 + 1, color);
        gui.fill(x1, y2 - 1, x2, y2, color);
        gui.fill(x1, y1, x1 + 1, y2, color);
        gui.fill(x2 - 1, y1, x2, y2, color);
    }

    public static void sectionHeader(GuiGraphicsExtractor gui, Font font, Component title, int x, int y, int width) {
        gui.text(font, title, x, y, MUTED, false);
        int start = x + font.width(title) + 6;
        if (start < x + width) gui.fill(start, y + 5, x + width, y + 6, BORDER_DIM);
    }

    public static void title(GuiGraphicsExtractor gui, Font font, Component title, int centerX, int y) {
        gui.text(font, title, centerX - font.width(title) / 2, y, CYAN, false);
    }

    public static void segmentedBar(GuiGraphicsExtractor gui, int x, int y, int width, int height,
                                    int value, int max, int color, int segments) {
        gui.fill(x, y, x + width, y + height, DEEP);
        frame(gui, x, y, x + width, y + height, BORDER_DIM);
        if (max <= 0 || value <= 0) return;

        double ratio = Math.min(1.0, Math.max(0.0, value / (double) max));
        int innerW = Math.max(0, width - 2);
        int filled = (int) Math.round(innerW * ratio);
        gui.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);

        if (segments > 1) {
            for (int i = 1; i < segments; i++) {
                int sx = x + 1 + innerW * i / segments;
                gui.fill(sx, y + 1, sx + 1, y + height - 1, DEEP);
            }
        }
    }

    public static void verticalGauge(GuiGraphicsExtractor gui, int x, int y, int width, int height,
                                     int value, int max, int color, int segments) {
        gui.fill(x, y, x + width, y + height, DEEP);
        frame(gui, x, y, x + width, y + height, BORDER_DIM);
        if (max <= 0 || value <= 0) return;

        double ratio = Math.min(1.0, Math.max(0.0, value / (double) max));
        int innerH = Math.max(0, height - 2);
        int filled = (int) Math.round(innerH * ratio);
        gui.fill(x + 1, y + height - 1 - filled, x + width - 1, y + height - 1, color);

        if (segments > 1) {
            for (int i = 1; i < segments; i++) {
                int sy = y + 1 + innerH * i / segments;
                gui.fill(x + 1, sy, x + width - 1, sy + 1, DEEP);
            }
        }
    }

    public static void statusLamp(GuiGraphicsExtractor gui, int x, int y, int color, boolean bright) {
        int c = bright ? color : BORDER_DIM;
        gui.fill(x, y, x + 7, y + 7, DEEP);
        gui.fill(x + 1, y + 1, x + 6, y + 6, c);
        gui.fill(x + 2, y + 2, x + 5, y + 5, bright ? 0xFFFFFFFF : c);
    }

    public static void slotFrame(GuiGraphicsExtractor gui, int x, int y, boolean active, int accent) {
        int border = active ? accent : BORDER_DIM;
        gui.fill(x - 2, y - 2, x + 20, y + 20, border);
        gui.fill(x - 1, y - 1, x + 19, y + 19, DEEP);
        gui.fill(x, y, x + 18, y + 18, 0xFF0A141B);
    }

    public static void buttonSurface(GuiGraphicsExtractor gui, int x, int y, int width, int height,
                                     boolean active, boolean hovered, boolean selected, int accent) {
        int border = !active ? BORDER_DIM : selected ? CYAN : hovered ? accent : BORDER;
        int fill = !active ? 0xFF0A1014 : selected ? 0xFF0B2B35 : hovered ? 0xFF102936 : SURFACE;
        panel(gui, x, y, x + width, y + height, border, fill);
        if (selected) gui.fill(x + 3, y + height - 3, x + width - 3, y + height - 2, CYAN);
        else if (hovered && active) gui.fill(x + 3, y + height - 3, x + width - 3, y + height - 2, accent);
    }

    public static int buttonText(boolean active, boolean hovered, boolean selected) {
        if (!active) return DISABLED;
        if (selected) return CYAN;
        if (hovered) return TEXT;
        return TEXT_SOFT;
    }

    private static void cornerCuts(GuiGraphicsExtractor gui, int x, int y, int w, int h, int color) {
        gui.fill(x, y, x + 5, y + 2, color);
        gui.fill(x, y, x + 2, y + 5, color);
        gui.fill(x + w - 5, y, x + w, y + 2, color);
        gui.fill(x + w - 2, y, x + w, y + 5, color);
        gui.fill(x, y + h - 2, x + 5, y + h, color);
        gui.fill(x, y + h - 5, x + 2, y + h, color);
        gui.fill(x + w - 5, y + h - 2, x + w, y + h, color);
        gui.fill(x + w - 2, y + h - 5, x + w, y + h, color);
    }
}
