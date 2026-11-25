package com.example.passivetree.client;

import com.example.passivetree.SkillService;
import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.data.PlayerSkills;
import com.example.passivetree.network.AllocatePacket;
import com.example.passivetree.network.ChooseClassPacket;
import com.example.passivetree.network.DeallocatePacket;
import com.example.passivetree.network.Net;
import com.example.passivetree.network.RespecPacket;
import com.example.passivetree.tree.NodeStat;
import com.example.passivetree.tree.SkillNode;
import com.example.passivetree.tree.SkillTree;
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
    private boolean builtHasClass = false;
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
        PlayerSkills s = skills();
        builtHasClass = s != null && !s.getClassId().isEmpty();

        if (!builtHasClass) {
            int bw = 130, bh = 20, gap = 6;
            int n = tree.classes.size();
            int rows = (n + 1) / 2;
            int y0 = height / 2 - (rows * (bh + gap)) / 2 + 12;
            for (int i = 0; i < n; i++) {
                SkillTree.ClassDef cd = tree.classes.get(i);
                int col = i % 2;
                int row = i / 2;
                int x = width / 2 + (col == 0 ? -bw - gap / 2 : gap / 2);
                addRenderableWidget(Button.builder(
                                Component.translatable("class.passivetree." + cd.id()),
                                b -> Net.CHANNEL.sendToServer(new ChooseClassPacket(cd.id())))
                        .bounds(x, y0 + row * (bh + gap), bw, bh).build());
            }
        } else {
            addRenderableWidget(Button.builder(Component.translatable("screen.passivetree.respec"),
                            b -> Net.CHANNEL.sendToServer(new RespecPacket()))
                    .bounds(width - 90, height - 28, 80, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.passivetree.center"),
                            b -> centerOnStart())
                    .bounds(width - 180, height - 28, 80, 20).build());
        }

        if (!centered) {
            centerOnStart();
            centered = true;
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
            case START -> 20;
            case SMALL -> 11;
            case NOTABLE -> 17;
            case KEYSTONE -> 25;
        };
        return (float) Math.max(3.0, base * Math.min(1.25, 0.5 + zoom * 0.6) / 2.0);
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        PlayerSkills s = skills();
        boolean hasClass = s != null && !s.getClassId().isEmpty();
        if (hasClass != builtHasClass) {
            clearWidgets();
            init();
        }

        g.fill(0, 0, width, height, 0xFF0A0A10);

        if (!builtHasClass || s == null) {
            Component title = Component.translatable("screen.passivetree.choose_class");
            g.drawString(font, title, width / 2 - font.width(title) / 2, height / 2 - 108, 0xFFE8C170);
            super.render(g, mx, my, pt);
            return;
        }

        computeHover(mx, my);
        drawEdges(g, s);
        drawNodes(g, s);
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
        Component cls = Component.translatable("screen.passivetree.class",
                Component.translatable("class.passivetree." + s.getClassId()));
        g.drawString(font, cls, 8, 8, 0xFFFFFFFF);
        Component pts = Component.translatable("screen.passivetree.points", s.availablePoints(tree));
        g.drawString(font, pts, width / 2 - font.width(pts) / 2, 8, 0xFFE8C170);
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

    // ---- edges + background rings ----------------------------------------

    private void drawEdges(GuiGraphics g, PlayerSkills s) {
        g.flush();
        Matrix4f m = g.pose().last().pose();
        beginDraw();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // guide rings
        double ox = originX(), oy = originY();
        int segs = 96;
        for (float rr : RING_RADII) {
            double r = rr * zoom;
            for (int i = 0; i < segs; i++) {
                double a0 = 2 * Math.PI * i / segs;
                double a1 = 2 * Math.PI * (i + 1) / segs;
                line(buf, m, ox + r * Math.cos(a0), oy + r * Math.sin(a0),
                        ox + r * Math.cos(a1), oy + r * Math.sin(a1), 0.8, 0xFF15151E);
            }
        }

        double zs = Math.max(0.7, zoom);
        for (SkillTree.Edge e : tree.edges) {
            boolean a = s.isAllocated(e.a().id);
            boolean b = s.isAllocated(e.b().id);
            double x1 = sx(e.a()), y1 = sy(e.a()), x2 = sx(e.b()), y2 = sy(e.b());
            if (a && b) {
                line(buf, m, x1, y1, x2, y2, 3.6 * zs, 0x40E8C170); // soft glow
                line(buf, m, x1, y1, x2, y2, 1.7 * zs, 0xFFF2D080);
            } else if (a || b) {
                line(buf, m, x1, y1, x2, y2, 1.2 * zs, 0xFF55667C);
            } else {
                line(buf, m, x1, y1, x2, y2, 1.0 * zs, 0xFF2E2E3A);
            }
        }
        endDraw();
    }

    // ---- nodes -------------------------------------------------------------

    private void drawNodes(GuiGraphics g, PlayerSkills s) {
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

            int fill, border, marker;
            if (allocated) {
                fill = 0xFFE8C170;
                border = 0xFFFFF0B0;
                marker = 0xFF6B4A12;
            } else if (available) {
                fill = 0xFF3F6082;
                border = 0xFFBBD8FF;
                marker = 0xFFBBD8FF;
            } else {
                fill = 0xFF1E1E27;
                border = 0xFF4A4A58;
                marker = 0xFF4F4F5E;
            }
            int classCol = n.type == SkillNode.Type.START ? classColor(n.id) : 0;
            if (n.type == SkillNode.Type.START) {
                border = classCol;
                if (!allocated) marker = classCol;
            } else if (n.type == SkillNode.Type.KEYSTONE && !allocated) {
                border = 0xFFD9822B;
                marker = 0xFFD9822B;
            }
            double bw = switch (n.type) {
                case SMALL -> 1.2;
                case NOTABLE -> 1.8;
                case START, KEYSTONE -> 2.2;
            };

            if (allocated) {
                poly(buf, m, cx, cy, r * 1.8, sides, 0, 0x30E8C170);
            }
            if (n.id.equals(hoveredId)) {
                poly(buf, m, cx, cy, r + 2.6, sides, 0, 0xFFFFFFFF);
            }
            poly(buf, m, cx, cy, r, sides, 0, border);
            poly(buf, m, cx, cy, Math.max(1, r - bw), sides, 0, fill);

            switch (n.type) {
                case NOTABLE -> poly(buf, m, cx, cy, r * 0.46, 4, Math.PI / 4, marker);
                case KEYSTONE -> {
                    poly(buf, m, cx, cy, r * 0.65, 4, 0, marker);
                    poly(buf, m, cx, cy, r * 0.65, 4, Math.PI / 4, marker);
                }
                case START -> poly(buf, m, cx, cy, r * 0.5, 16, 0, marker);
                default -> { }
            }
        }
        endDraw();
    }

    private void drawLabels(GuiGraphics g) {
        if (zoom < 0.45) return;
        for (SkillNode n : tree.nodes.values()) {
            if (n.type != SkillNode.Type.START && !(n.type == SkillNode.Type.KEYSTONE && zoom >= 0.7)) continue;
            int cx = (int) Math.round(sx(n));
            int cy = (int) Math.round(sy(n));
            if (cx < -60 || cx > width + 60 || cy < -20 || cy > height + 20) continue;
            int ty = cy + (int) nodeRadius(n) + 4;
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
        clickedOnTree = builtHasClass && my > HEADER_H;
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
        if (!builtHasClass) return super.mouseScrolled(mx, my, delta);
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
