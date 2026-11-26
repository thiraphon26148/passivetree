package com.example.passivetree.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Small drawing helpers for the fancy panels. */
public final class Ui {
    public static final int GOLD = 0xFFE8C170;
    public static final int GOLD_DARK = 0xFF6B5A2E;
    public static final int TEXT_DIM = 0xFF8A8A9A;

    /** A framed dark panel with a gold border and a thin inner line. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, GOLD_DARK);
        g.fillGradient(x, y, x + w, y + h, 0xF2181822, 0xF20D0D14);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0xFF2A2A3A);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFF2A2A3A);
        g.fill(x + 2, y + 2, x + 3, y + h - 2, 0xFF2A2A3A);
        g.fill(x + w - 3, y + 2, x + w - 2, y + h - 2, 0xFF2A2A3A);
        // little gold corner studs
        int c = 0xFFE8C170;
        g.fill(x - 2, y - 2, x + 3, y + 1, c);
        g.fill(x + w - 3, y - 2, x + w + 2, y + 1, c);
        g.fill(x - 2, y + h - 1, x + 3, y + h + 2, c);
        g.fill(x + w - 3, y + h - 1, x + w + 2, y + h + 2, c);
    }

    /** A section divider with a centred gold diamond. */
    public static void divider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w / 2 - 4, y + 1, GOLD_DARK);
        g.fill(x + w / 2 + 4, y, x + w, y + 1, GOLD_DARK);
        g.fill(x + w / 2 - 1, y - 2, x + w / 2 + 1, y + 3, GOLD);
        g.fill(x + w / 2 - 2, y - 1, x + w / 2 + 2, y + 2, GOLD);
    }

    /** Draws text centred on cx, shrinking it if it is wider than maxW. */
    public static void drawCenteredFit(GuiGraphics g, Font font, Component c, int cx, int y, int maxW, int color) {
        int w = font.width(c);
        float k = w > maxW ? (float) maxW / w : 1f;
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(k, k, 1f);
        g.drawString(font, c, -w / 2, 0, color);
        g.pose().popPose();
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private Ui() {}
}
