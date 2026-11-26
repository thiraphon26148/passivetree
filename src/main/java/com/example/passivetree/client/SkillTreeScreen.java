package com.example.passivetree.client;

import com.example.passivetree.SkillService;
import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.data.PlayerSkills;
import com.example.passivetree.network.AllocatePacket;
import com.example.passivetree.network.DeallocatePacket;
import com.example.passivetree.network.Net;
import com.example.passivetree.network.RespecPacket;
import com.example.passivetree.tree.NodeStat;
import com.example.passivetree.tree.SkillNode;
import com.example.passivetree.tree.SkillTree;
import com.example.passivetree.tree.StatDefs;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SkillTreeScreen extends Screen {
    private static final int HEADER_H = 24;
    private static final DecimalFormat NUM = new DecimalFormat("0.##");
    private static final float[] RING_RADII = {150f, 300f, 450f, 600f};

    private final SkillTree tree = SkillTree.get();

    private double offX = 0;
    private double offY = 0;
    private double zoom = 0.75;
    private boolean centered = false;
    private boolean clickedOnTree = false;
    private double dragDist = 0;
    private String hoveredId = null;

    public SkillTreeScreen() {
        super(Component.translatable("screen.passivetree.title"));
    }

    private PlayerSkills skills() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return null;
        return p.getCapability(ModCapabilities.PLAYER_SKILLS).orElse(null);
    }

    // ------------------------------------------------------------ setup

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.passivetree.status_button"),
                        b -> Minecraft.getInstance().setScreen(new StatusScreen()))
                .bounds(width - 96, 2, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.passivetree.respec"),
                        b -> Net.CHANNEL.sendToServer(new RespecPacket()))
                .bounds(width - 90, height - 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.passivetree.center"),
                        b -> centerOnStart())
                .bounds(width - 180, height - 28, 80, 20).build());

        if (!centered) {
            centerOnStart();
            centered = true;
        }
    }

    @Override
    public void tick() {
        PlayerSkills s = skills();
        if (s == null || s.getClassId().isEmpty()) {
            Minecraft.getInstance().setScreen(new ClassSelectScreen());
        }
    }

    private void centerOnStart() {
        PlayerSkills s = skills();
        SkillTree.ClassDef cd = s == null ? null : tree.classById(s.getClassId());
        if (cd != null) {
            SkillNode n = tree.nodes.get(cd.start());
            if (n != null) {
                offX = -n.x * zoom;
                offY = -n.y * zoom;
                return;
            }
        }
        offX = 0;
        offY = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ coordinates

    private double originX() {
        return width / 2.0 + offX;
    }

    private double originY() {
        return height / 2.0 + HEADER_H / 2.0 + offY;
    }

    private double sx(SkillNode n) {
        return originX() + n.x * zoom;
    }

    private double sy(SkillNode n) {
        return originY() + n.y * zoom;
    }

    private float nodeRadius(SkillNode n) {
        double base = switch (n.type) {
            case START -> 22;
            case SMALL -> 12;
            case NOTABLE -> 19;
            case KEYSTONE -> 27;
        };
        return (float) Math.max(3.0, base * Math.min(1.25, 0.5 + zoom * 0.6) / 2.0);
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        PlayerSkills s = skills();
        Tex.background(g, width, height, offX, offY);
        if (s == null || s.getClassId().isEmpty()) {
            super.render(g, mx, my, pt);
            return;
        }

        computeHover(mx, my);
        drawHalos(g);
        drawEdges(g, s);
        drawNodeBases(g, s);
        drawNodeTextures(g, s);
        drawLabels(g);
        drawHeader(g, s);
        super.render(g, mx, my, pt);

        if (hoveredId != null && my > HEADER_H) {
            SkillNode n = tree.nodes.get(hoveredId);
            if (n != null) {
                g.renderComponentTooltip(font, tooltipFor(n, s), mx, my);
            }
        }
    }

    private void computeHover(int mx, int my) {
        hoveredId = null;
        double best = Double.MAX_VALUE;
        for (SkillNode n : tree.nodes.values()) {
            double cx = sx(n), cy = sy(n);
            if (cx < -40 || cx > width + 40 || cy < -40 || cy > height + 40) continue;
            double d = Math.hypot(mx - cx, my - cy);
            double hit = Math.max(nodeRadius(n) + 1.5, 6);
            if (d <= hit && d < best) {
                best = d;
                hoveredId = n.id;
            }
        }
    }

    private void drawHeader(GuiGraphics g, PlayerSkills s) {
        g.fill(0, 0, width, HEADER_H, 0xF0101018);
        g.fill(0, HEADER_H - 1, width, HEADER_H, Ui.GOLD_DARK);
        SkillTree.ClassDef cd = tree.classById(s.getClassId());
        int color = cd == null ? 0xFFFFFFFF : cd.color();
        Tex.emblem(g, s.getClassId(), 5, 3, 18);
        g.drawString(font, Component.translatable("class.passivetree." + s.getClassId()), 28, 8, color);

        Component pts = Component.translatable("screen.passivetree.points", s.availablePoints(tree));
        int pw = font.width(pts);
        g.fill(width / 2 - pw / 2 - 6, 3, width / 2 + pw / 2 + 6, HEADER_H - 4, 0xFF1C1810);
        g.fill(width / 2 - pw / 2 - 6, 3, width / 2 + pw / 2 + 6, 4, Ui.GOLD_DARK);
        g.fill(width / 2 - pw / 2 - 6, HEADER_H - 5, width / 2 + pw / 2 + 6, HEADER_H - 4, Ui.GOLD_DARK);
        g.drawString(font, pts, width / 2 - pw / 2, 8, Ui.GOLD);
        g.drawString(font, Component.translatable("screen.passivetree.hint"), 8, height - 14, 0xFF777788);
    }

    // ---- low level shapes -------------------------------------------------

    private static void vtx(BufferBuilder buf, Matrix4f m, double x, double y, int color) {
        buf.vertex(m, (float) x, (float) y, 0f)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255)
                .endVertex();
    }

    /** A thick line as a quad. */
    private static void line(BufferBuilder buf, Matrix4f m, double x1, double y1, double x2, double y2, double half, int color) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001) return;
        double nx = -dy / len * half;
        double ny = dx / len * half;
        vtx(buf, m, x1 + nx, y1 + ny, color);
        vtx(buf, m, x1 - nx, y1 - ny, color);
        vtx(buf, m, x2 - nx, y2 - ny, color);
        vtx(buf, m, x2 + nx, y2 + ny, color);
    }

    /** A filled regular polygon (many sides = circle) as a triangle list. */
    private static void poly(BufferBuilder buf, Matrix4f m, double cx, double cy, double r, int sides, double rot, int color) {
        for (int i = 0; i < sides; i++) {
            double a0 = rot + 2 * Math.PI * i / sides;
            double a1 = rot + 2 * Math.PI * (i + 1) / sides;
            vtx(buf, m, cx, cy, color);
            vtx(buf, m, cx + r * Math.cos(a0), cy + r * Math.sin(a0), color);
            vtx(buf, m, cx + r * Math.cos(a1), cy + r * Math.sin(a1), color);
        }
    }

    private static void beginDraw() {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    private static void endDraw() {
        Tesselator.getInstance().end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static int withAlpha(int rgb, int alpha) {
        return (rgb & 0x00FFFFFF) | (alpha << 24);
    }

    // ---- soft class-coloured halos behind each start node ------------------

    private void drawHalos(GuiGraphics g) {
        g.flush();
        Matrix4f m = g.pose().last().pose();
        beginDraw();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (SkillTree.ClassDef cd : tree.classes) {
            SkillNode n = tree.nodes.get(cd.start());
            if (n == null) continue;
            double cx = sx(n), cy = sy(n);
            poly(buf, m, cx, cy, 190 * zoom, 56, 0, withAlpha(cd.color(), 0x10));
            poly(buf, m, cx, cy, 130 * zoom, 48, 0, withAlpha(cd.color(), 0x12));
            poly(buf, m, cx, cy, 70 * zoom, 40, 0, withAlpha(cd.color(), 0x14));
        }
        endDraw();
    }

    // ---- edges + guide rings -----------------------------------------------

    private void drawEdges(GuiGraphics g, PlayerSkills s) {
        Matrix4f m = g.pose().last().pose();
        beginDraw();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        double ox = originX(), oy = originY();
        int segs = 96;
        for (float rr : RING_RADII) {
            double r = rr * zoom;
            for (int i = 0; i < segs; i++) {
                double a0 = 2 * Math.PI * i / segs;
                double a1 = 2 * Math.PI * (i + 1) / segs;
                line(buf, m, ox + r * Math.cos(a0), oy + r * Math.sin(a0),
                        ox + r * Math.cos(a1), oy + r * Math.sin(a1), 0.8, 0x80232333);
            }
        }

        double zs = Math.max(0.7, zoom);
        for (SkillTree.Edge e : tree.edges) {
            boolean a = s.isAllocated(e.a().id);
            boolean b = s.isAllocated(e.b().id);
            double x1 = sx(e.a()), y1 = sy(e.a()), x2 = sx(e.b()), y2 = sy(e.b());
            if (a && b) {
                line(buf, m, x1, y1, x2, y2, 3.8 * zs, 0x40E8C170);
                line(buf, m, x1, y1, x2, y2, 1.7 * zs, 0xFFF2D080);
            } else if (a || b) {
                line(buf, m, x1, y1, x2, y2, 1.3 * zs, 0xFF5E6F88);
            } else {
                line(buf, m, x1, y1, x2, y2, 1.0 * zs, 0xFF30303E);
            }
        }
        endDraw();
    }

    // ---- node glow + dark/gold fill ------------------------------------------

    private void drawNodeBases(GuiGraphics g, PlayerSkills s) {
        Matrix4f m = g.pose().last().pose();
        beginDraw();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (SkillNode n : tree.nodes.values()) {
            double cx = sx(n), cy = sy(n);
            if (cx < -40 || cx > width + 40 || cy < -40 || cy > height + 40) continue;
            float r = nodeRadius(n);
            int sides = r > 9 ? 28 : (r > 5 ? 20 : 14);
            boolean allocated = s.isAllocated(n.id);
            boolean available = !allocated && SkillService.canAllocate(s, tree, n);

            int fill = allocated ? 0xFFE8C170 : (available ? 0xFF35567A : 0xFF14141C);

            if (allocated) {
                poly(buf, m, cx, cy, r * 2.0, sides, 0, 0x30E8C170);
                poly(buf, m, cx, cy, r * 1.5, sides, 0, 0x30E8C170);
            } else if (available) {
                poly(buf, m, cx, cy, r * 1.6, sides, 0, 0x222E6BB8);
            }
            if (n.id.equals(hoveredId)) {
                poly(buf, m, cx, cy, r * 1.15 + 2.5, sides, 0, 0xFFFFFFFF);
            }
            poly(buf, m, cx, cy, r * 0.92, sides, 0, fill);
        }
        endDraw();
    }

    // ---- frames, icons and emblems (textures) ------------------------------------

    private void drawNodeTextures(GuiGraphics g, PlayerSkills s) {
        for (SkillNode n : tree.nodes.values()) {
            double cx = sx(n), cy = sy(n);
            if (cx < -40 || cx > width + 40 || cy < -40 || cy > height + 40) continue;
            float r = nodeRadius(n);
            if (r < 3.5f) continue;

            boolean allocated = s.isAllocated(n.id);
            boolean available = !allocated && SkillService.canAllocate(s, tree, n);

            int frameCol, iconCol;
            if (allocated) {
                frameCol = 0xFFFFF0B0;
                iconCol = 0xFF3A2808;
            } else if (available) {
                frameCol = 0xFFBBD8FF;
                iconCol = 0xFFDCEBFF;
            } else {
                frameCol = n.type == SkillNode.Type.KEYSTONE ? 0xFFD9822B
                        : (n.type == SkillNode.Type.NOTABLE ? 0xFF8C8CA2 : 0xFF565668);
                iconCol = 0xFF70708A;
            }

            double d = r * 2.0;
            switch (n.type) {
                case START -> {
                    String cid = n.id.substring(0, n.id.indexOf('_'));
                    Tex.emblem(g, cid, (int) Math.round(cx - d * 0.55), (int) Math.round(cy - d * 0.55), (int) Math.round(d * 1.1));
                    Tex.frame(g, 3, cx, cy, d * 1.3, allocated ? 0xFFFFF0B0 : 0xFF8C8CA2);
                }
                case SMALL -> {
                    Tex.frame(g, 0, cx, cy, d * 1.12, frameCol);
                    Tex.icon(g, iconFor(n), cx, cy, d * 0.62, iconCol);
                }
                case NOTABLE -> {
                    Tex.frame(g, 1, cx, cy, d * 1.18, frameCol);
                    Tex.icon(g, iconFor(n), cx, cy, d * 0.6, iconCol);
                }
                case KEYSTONE -> {
                    Tex.frame(g, 2, cx, cy, d * 1.18, frameCol);
                    Tex.icon(g, iconFor(n), cx, cy, d * 0.58, allocated ? iconCol : (available ? iconCol : 0xFFB9702A));
                }
            }
        }
    }

    private int iconFor(SkillNode n) {
        for (NodeStat st : n.stats) {
            int idx = StatDefs.iconIndex(st.attribute);
            if (idx >= 0) return idx;
        }
        return -1;
    }

    private void drawLabels(GuiGraphics g) {
        if (zoom < 0.45) return;
        for (SkillNode n : tree.nodes.values()) {
            if (n.type != SkillNode.Type.START && !(n.type == SkillNode.Type.KEYSTONE && zoom >= 0.7)) continue;
            int cx = (int) Math.round(sx(n));
            int cy = (int) Math.round(sy(n));
            if (cx < -60 || cx > width + 60 || cy < -20 || cy > height + 20) continue;
            int ty = cy + (int) (nodeRadius(n) * 1.3) + 3;
            if (n.type == SkillNode.Type.START) {
                String cid = n.id.substring(0, n.id.indexOf('_'));
                g.drawCenteredString(font, Component.translatable("class.passivetree." + cid), cx, ty, classColor(n.id));
            } else {
                g.drawCenteredString(font, Component.literal(n.name), cx, ty, 0xFFD9822B);
            }
        }
    }

    private int classColor(String startNodeId) {
        for (SkillTree.ClassDef cd : tree.classes) {
            if (cd.start().equals(startNodeId)) return cd.color();
        }
        return 0xFFFFFFFF;
    }

    // ------------------------------------------------------------ tooltip

    private List<Component> tooltipFor(SkillNode n, PlayerSkills s) {
        List<Component> lines = new ArrayList<>();
        ChatFormatting titleColor = switch (n.type) {
            case KEYSTONE -> ChatFormatting.GOLD;
            case NOTABLE -> ChatFormatting.YELLOW;
            default -> ChatFormatting.WHITE;
        };
        lines.add(Component.literal(n.name).withStyle(titleColor, ChatFormatting.BOLD));
        lines.add(Component.translatable("type.passivetree." + n.type.name().toLowerCase()).withStyle(ChatFormatting.DARK_GRAY));
        for (NodeStat st : n.stats) {
            lines.add(statLine(st));
        }
        lines.add(Component.empty());
        if (s.isAllocated(n.id)) {
            lines.add(Component.translatable("tooltip.passivetree.allocated").withStyle(ChatFormatting.GREEN));
            if (SkillService.canDeallocate(s, tree, n)) {
                lines.add(Component.translatable("tooltip.passivetree.can_refund").withStyle(ChatFormatting.GRAY));
            } else if (n.type != SkillNode.Type.START) {
                lines.add(Component.translatable("tooltip.passivetree.cannot_refund").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (SkillService.canAllocate(s, tree, n)) {
            lines.add(Component.translatable("tooltip.passivetree.can_allocate", n.cost()).withStyle(ChatFormatting.AQUA));
        } else {
            boolean connected = false;
            for (String nb : tree.neighbors(n.id)) {
                if (s.isAllocated(nb)) {
                    connected = true;
                    break;
                }
            }
            if (connected) {
                lines.add(Component.translatable("tooltip.passivetree.need_points", n.cost()).withStyle(ChatFormatting.RED));
            } else {
                lines.add(Component.translatable("tooltip.passivetree.not_connected").withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }

    private static Component statLine(NodeStat st) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(st.attribute);
        Component name = attr != null
                ? Component.translatable(attr.getDescriptionId())
                : Component.literal(st.attribute.toString());
        boolean flat = st.op == NodeStat.Op.ADD;
        double v = flat ? st.value : st.value * 100.0;
        String num = (v >= 0 ? "+" : "") + NUM.format(v) + (flat ? "" : "%");
        return Component.literal(num + " ").append(name)
                .withStyle(v >= 0 ? ChatFormatting.BLUE : ChatFormatting.RED);
    }

    // ------------------------------------------------------------ input

    private void playSound(boolean allocate) {
        Minecraft mc = Minecraft.getInstance();
        if (allocate) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4F));
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.7F));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            clickedOnTree = false;
            return true;
        }
        clickedOnTree = my > HEADER_H;
        dragDist = 0;
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (clickedOnTree) {
            offX += dx;
            offY += dy;
            dragDist += Math.abs(dx) + Math.abs(dy);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (clickedOnTree && dragDist < 4 && hoveredId != null) {
            PlayerSkills s = skills();
            SkillNode n = tree.nodes.get(hoveredId);
            if (s != null && n != null) {
                if (button == 0 && SkillService.canAllocate(s, tree, n)) {
                    Net.CHANNEL.sendToServer(new AllocatePacket(hoveredId));
                    playSound(true);
                } else if (button == 1 && SkillService.canDeallocate(s, tree, n)) {
                    Net.CHANNEL.sendToServer(new DeallocatePacket(hoveredId));
                    playSound(false);
                }
            }
        }
        clickedOnTree = false;
        dragDist = 0;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double old = zoom;
        zoom = Math.max(0.25, Math.min(2.5, zoom * (delta > 0 ? 1.1 : 1.0 / 1.1)));
        double cx = width / 2.0;
        double cy = height / 2.0 + HEADER_H / 2.0;
        double worldX = (mx - cx - offX) / old;
        double worldY = (my - cy - offY) / old;
        offX = mx - cx - worldX * zoom;
        offY = my - cy - worldY * zoom;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientModEvents.OPEN_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (ClientModEvents.STATUS_KEY.matches(keyCode, scanCode)) {
            Minecraft.getInstance().setScreen(new StatusScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
