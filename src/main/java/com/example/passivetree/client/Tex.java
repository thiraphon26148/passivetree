package com.example.passivetree.client;

import com.example.passivetree.PassiveTreeMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Texture helpers: icons, node frames, class emblems, logo, background. All files live in textures/gui. */
public final class Tex {
    private static ResourceLocation gui(String name) {
        return new ResourceLocation(PassiveTreeMod.MOD_ID, "textures/gui/" + name + ".png");
    }

    public static final ResourceLocation ICONS = gui("icons");      // 8 cells of 64x64
    public static final ResourceLocation FRAMES = gui("frames");    // 4 cells of 64x64: small, notable, keystone, start
    public static final ResourceLocation LOGO = gui("logo_title");  // 480x80
    public static final ResourceLocation BG = gui("bg");            // 256x256 tile

    public static ResourceLocation emblem(String classId) {
        return gui("emblem_" + classId);                            // 128x128
    }

    private static void tint(int argb) {
        RenderSystem.setShaderColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f,
                (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
    }

    private static void untint() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Draws icon cell {@code index} (0-7) tinted with an ARGB colour. */
    public static void icon(GuiGraphics g, int index, double cx, double cy, double size, int argb) {
        if (index < 0) return;
        RenderSystem.enableBlend();
        tint(argb);
        int s = Math.max(2, (int) Math.round(size));
        g.blit(ICONS, (int) Math.round(cx - s / 2.0), (int) Math.round(cy - s / 2.0), s, s,
                index * 64f, 0f, 64, 64, 512, 64);
        untint();
    }

    /** Draws a node frame (0 small, 1 notable, 2 keystone, 3 start) tinted with an ARGB colour. */
    public static void frame(GuiGraphics g, int index, double cx, double cy, double size, int argb) {
        RenderSystem.enableBlend();
        tint(argb);
        int s = Math.max(2, (int) Math.round(size));
        g.blit(FRAMES, (int) Math.round(cx - s / 2.0), (int) Math.round(cy - s / 2.0), s, s,
                index * 64f, 0f, 64, 64, 256, 64);
        untint();
    }

    /** Draws a class emblem with its top-left corner at x,y. */
    public static void emblem(GuiGraphics g, String classId, int x, int y, int size) {
        RenderSystem.enableBlend();
        untint();
        g.blit(emblem(classId), x, y, size, size, 0f, 0f, 128, 128, 128, 128);
    }

    /** Draws the title logo centred horizontally at cx, with its top at y, scaled to the given width. */
    public static void logo(GuiGraphics g, int cx, int y, int width) {
        RenderSystem.enableBlend();
        untint();
        int h = width * 80 / 480;
        g.blit(LOGO, cx - width / 2, y, width, h, 0f, 0f, 480, 80, 480, 80);
    }

    /** Tiled, slowly moving starfield background. */
    public static void background(GuiGraphics g, int w, int h, double scrollX, double scrollY) {
        RenderSystem.disableBlend();
        untint();
        float u = (float) (((-scrollX * 0.15) % 256 + 256) % 256);
        float v = (float) (((-scrollY * 0.15) % 256 + 256) % 256);
        g.blit(BG, 0, 0, w, h, u, v, w, h, 256, 256);
    }

    private Tex() {}
}
