package com.example.passivetree.client;

import com.example.passivetree.SkillService;
import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.data.PlayerSkills;
import com.example.passivetree.network.AllocatePacket;
import com.example.passivetree.network.ChooseClassPacket;
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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SkillTreeScreen extends Screen {
    private static final int HEADER_H = 24;
    private static final DecimalFormat NUM = new DecimalFormat("0.##");

    private final SkillTree tree = SkillTree.get();

    private double offX = 0;
    private double offY = 0;
    private double zoom = 0.9;
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
            int bw = 150, bh = 20, gap = 6;
            int n = tree.classes.size();
            int y0 = height / 2 - (n * (bh + gap)) / 2 + 12;
            for (int i = 0; i < n; i++) {
                SkillTree.ClassDef cd = tree.classes.get(i);
                addRenderableWidget(Button.builder(
                                Component.translatable("class.passivetree." + cd.id()),
                                b -> Net.CHANNEL.sendToServer(new ChooseClassPacket(cd.id())))
                        .bounds(width / 2 - bw / 2, y0 + i * (bh + gap), bw, bh).build());
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

    private double sx(SkillNode n) {
        return width / 2.0 + offX + n.x * zoom;
    }

    private double sy(SkillNode n) {
        return height / 2.0 + HEADER_H / 2.0 + offY + n.y * zoom;
    }

    private int nodeSize(SkillNode n) {
        double base = switch (n.type) {
            case START -> 15;
            case SMALL -> 11;
            case NOTABLE -> 17;
            case KEYSTONE -> 24;
        };
        return Math.max(5, (int) Math.round(base * Math.min(1.2, 0.5 + zoom * 0.6)));
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

        g.fill(0, 0, width, height, 0xFF0B0B12);

        if (!builtHasClass || s == null) {
            Component title = Component.translatable("screen.passivetree.choose_class");
            g.drawString(font, title, width / 2 - font.width(title) / 2, height / 2 - 100, 0xFFE8C170);
            super.render(g, mx, my, pt);
            return;
        }

        drawEdges(g, s);
        drawNodes(g, s, mx, my);
        drawHeader(g, s);
        super.render(g, mx, my, pt);

        if (hoveredId != null && my > HEADER_H) {
            SkillNode n = tree.nodes.get(hoveredId);
            if (n != null) {
                g.renderComponentTooltip(font, tooltipFor(n, s), mx, my);
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

    private void drawEdges(GuiGraphics g, PlayerSkills s) {
        g.flush();
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (SkillTree.Edge e : tree.edges) {
            boolean a = s.isAllocated(e.a().id);
            boolean b = s.isAllocated(e.b().id);
            int color;
            double half;
            if (a && b) {
                color = 0xFFE8C170;
                half = 1.6;
            } else if (a || b) {
                color = 0xFF5A6A80;
                half = 1.2;
            } else {
                color = 0xFF33333F;
                half = 1.0;
            }
            half *= Math.max(0.7, zoom);

            double x1 = sx(e.a()), y1 = sy(e.a()), x2 = sx(e.b()), y2 = sy(e.b());
            double dx = x2 - x1, dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 0.001) continue;
            float nx = (float) (-dy / len * half);
            float ny = (float) (dx / len * half);

            int ca = (color >> 24) & 255, cr = (color >> 16) & 255, cg = (color >> 8) & 255, cb = color & 255;
            buf.vertex(m, (float) x1 + nx, (float) y1 + ny, 0f).color(cr, cg, cb, ca).endVertex();
            buf.vertex(m, (float) x1 - nx, (float) y1 - ny, 0f).color(cr, cg, cb, ca).endVertex();
            buf.vertex(m, (float) x2 - nx, (float) y2 - ny, 0f).color(cr, cg, cb, ca).endVertex();
            buf.vertex(m, (float) x2 + nx, (float) y2 + ny, 0f).color(cr, cg, cb, ca).endVertex();
        }

        Tesselator.getInstance().end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawNodes(GuiGraphics g, PlayerSkills s, int mx, int my) {
        hoveredId = null;
        double best = Double.MAX_VALUE;

        for (SkillNode n : tree.nodes.values()) {
            int cx = (int) Math.round(sx(n));
            int cy = (int) Math.round(sy(n));
            if (cx < -40 || cx > width + 40 || cy < -40 || cy > height + 40) continue;

            int size = nodeSize(n);
            boolean allocated = s.isAllocated(n.id);
            boolean available = !allocated && SkillService.canAllocate(s, tree, n);

            int fill, border;
            if (allocated) {
                fill = 0xFFE8C170;
                border = 0xFFFFF0B0;
            } else if (available) {
                fill = 0xFF4A6A8A;
                border = 0xFFBBD8FF;
            } else {
                fill = 0xFF22222B;
                border = 0xFF4A4A58;
            }
            if (n.type == SkillNode.Type.START) {
                border = classColor(n.id);
            } else if (n.type == SkillNode.Type.KEYSTONE && !allocated) {
                border = 0xFFD9822B;
            }
            int bw = switch (n.type) {
                case SMALL -> 1;
                case NOTABLE -> 2;
                case START, KEYSTONE -> 3;
            };

            octagon(g, cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, Math.max(1, size / 4), border);
            int in = Math.min(bw, size / 2 - 1);
            octagon(g, cx - size / 2 + in, cy - size / 2 + in, cx + size / 2 - in, cy + size / 2 - in,
                    Math.max(0, size / 4 - 1), fill);

            double d = Math.max(Math.abs(mx - cx), Math.abs(my - cy));
            if (d <= Math.max(size / 2.0, 6) && d < best) {
                best = d;
                hoveredId = n.id;
            }
        }

        if (hoveredId != null) {
            SkillNode n = tree.nodes.get(hoveredId);
            int cx = (int) Math.round(sx(n));
            int cy = (int) Math.round(sy(n));
            int h = nodeSize(n) / 2 + 2;
            int c = 0xFFFFFFFF;
            g.fill(cx - h, cy - h, cx + h, cy - h + 1, c);
            g.fill(cx - h, cy + h - 1, cx + h, cy + h, c);
            g.fill(cx - h, cy - h, cx - h + 1, cy + h, c);
            g.fill(cx + h - 1, cy - h, cx + h, cy + h, c);
        }
    }

    private int classColor(String startNodeId) {
        for (SkillTree.ClassDef cd : tree.classes) {
            if (cd.start().equals(startNodeId)) return cd.color();
        }
        return 0xFFFFFFFF;
    }

    private void octagon(GuiGraphics g, int x1, int y1, int x2, int y2, int c, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        g.fill(x1 + c, y1, x2 - c, y2, color);
        g.fill(x1, y1 + c, x2, y2 - c, color);
        int h = c / 2;
        g.fill(x1 + h, y1 + h, x2 - h, y2 - h, color);
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
        if (clickedOnTree && button == 0 && dragDist < 4 && hoveredId != null) {
            Net.CHANNEL.sendToServer(new AllocatePacket(hoveredId));
        }
        clickedOnTree = false;
        dragDist = 0;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!builtHasClass) return super.mouseScrolled(mx, my, delta);
        double old = zoom;
        zoom = Math.max(0.3, Math.min(2.5, zoom * (delta > 0 ? 1.1 : 1.0 / 1.1)));
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
