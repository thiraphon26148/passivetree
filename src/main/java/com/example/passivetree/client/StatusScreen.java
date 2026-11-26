package com.example.passivetree.client;

import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.data.PlayerSkills;
import com.example.passivetree.network.Net;
import com.example.passivetree.network.RequestStatsPacket;
import com.example.passivetree.tree.NodeStat;
import com.example.passivetree.tree.SkillNode;
import com.example.passivetree.tree.SkillTree;
import com.example.passivetree.tree.StatDefs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A PoE 2 style character sheet: class, level, passive points and all the stats the passive tree touches. */
public class StatusScreen extends Screen {
    private static final int VW = 400, VH = 250;
    private static final DecimalFormat NUM = new DecimalFormat("0.##");

    // list on the right
    private static final int LIST_X = 294, LIST_Y = 48, LIST_W = 94, LIST_ROWS = 14, LIST_ROW_H = 11;
    // buttons (virtual coordinates)
    private static final int[] BTN_TREE = {124, 222, 130, 18};
    private static final int[] BTN_CLOSE = {262, 222, 70, 18};

    private final SkillTree tree = SkillTree.get();
    private int tickCount = 0;
    private int scroll = 0;

    public StatusScreen() {
        super(Component.translatable("status.passivetree.title"));
    }

    private PlayerSkills skills() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? null : p.getCapability(ModCapabilities.PLAYER_SKILLS).orElse(null);
    }

    private float scale() {
        return Math.min(1f, Math.min((width - 8f) / VW, (height - 8f) / VH));
    }

    private float ox() {
        return (width - VW * scale()) / 2f;
    }

    private float oy() {
        return (height - VH * scale()) / 2f;
    }

    @Override
    protected void init() {
        Net.CHANNEL.sendToServer(new RequestStatsPacket());
    }

    @Override
    public void tick() {
        if (++tickCount % 20 == 0) {
            Net.CHANNEL.sendToServer(new RequestStatsPacket());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ helpers

    private static String signed(double v) {
        return (v >= 0 ? "+" : "") + NUM.format(v);
    }

    private String bonus(int idx, Map<NodeStat.Key, Double> totals) {
        ResourceLocation id = new ResourceLocation(StatDefs.ATTRS[idx]);
        double add = totals.getOrDefault(new NodeStat.Key(id, NodeStat.Op.ADD), 0.0);
        double mb = totals.getOrDefault(new NodeStat.Key(id, NodeStat.Op.MULT_BASE), 0.0);
        double mt = totals.getOrDefault(new NodeStat.Key(id, NodeStat.Op.MULT_TOTAL), 0.0);
        StringBuilder sb = new StringBuilder();
        if (Math.abs(add) > 1e-9) {
            if (idx == StatDefs.KNOCKBACK) sb.append(signed(add * 100)).append('%');
            else sb.append(signed(add));
        }
        if (Math.abs(mb) > 1e-9) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(signed(mb * 100)).append('%');
        }
        if (Math.abs(mt) > 1e-9) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(signed(mt * 100)).append('%');
        }
        return sb.length() == 0 ? "" : "(" + sb + ")";
    }

    private void row(GuiGraphics g, int x, int y, int w, int iconIdx, Component label, String bonus, String value) {
        if (iconIdx >= 0) {
            Tex.icon(g, iconIdx, x + 5, y + 4, 11, Ui.GOLD);
        }
        g.drawString(font, label, x + 14, y, 0xFFD0D0DC, false);
        int vw = font.width(value);
        g.drawString(font, value, x + w - vw, y, 0xFFFFFFFF, false);
        if (!bonus.isEmpty()) {
            g.drawString(font, bonus, x + w - vw - 4 - font.width(bonus), y, 0xFF6FA8FF, false);
        }
    }

    private void sectionHeader(GuiGraphics g, int x, int y, int w, String key) {
        g.drawString(font, Component.translatable(key), x, y, Ui.GOLD, false);
        g.fill(x, y + 10, x + w, y + 11, Ui.GOLD_DARK);
    }

    private void button(GuiGraphics g, int[] r, Component label, boolean hover) {
        g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, hover ? Ui.GOLD : Ui.GOLD_DARK);
        g.fillGradient(r[0], r[1], r[0] + r[2], r[1] + r[3], hover ? 0xFF33333F : 0xFF22222C, hover ? 0xFF22222C : 0xFF14141C);
        Ui.drawCenteredFit(g, font, label, r[0] + r[2] / 2, r[1] + 5, r[2] - 6, hover ? 0xFFFFFFFF : 0xFFD0D0DC);
    }

    private List<SkillNode> highlightedNodes(PlayerSkills s) {
        List<SkillNode> keys = new ArrayList<>();
        List<SkillNode> nots = new ArrayList<>();
        if (s != null) {
            for (String id : s.getAllocated()) {
                SkillNode n = tree.nodes.get(id);
                if (n == null) continue;
                if (n.type == SkillNode.Type.KEYSTONE) keys.add(n);
                else if (n.type == SkillNode.Type.NOTABLE) nots.add(n);
            }
        }
        keys.addAll(nots);
        return keys;
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Tex.background(g, width, height, 0, 0);
        float sc = scale();
        double vx = (mx - ox()) / sc;
        double vy = (my - oy()) / sc;

        g.pose().pushPose();
        g.pose().translate(ox(), oy(), 0);
        g.pose().scale(sc, sc, 1f);

        Ui.panel(g, 0, 0, VW, VH);
        g.drawString(font, Component.translatable("status.passivetree.title"), 14, 10, Ui.GOLD, false);
        Ui.divider(g, 10, 24, VW - 20);

        PlayerSkills s = skills();
        LocalPlayer player = Minecraft.getInstance().player;
        boolean hasClass = s != null && !s.getClassId().isEmpty();
        SkillTree.ClassDef cd = hasClass ? tree.classById(s.getClassId()) : null;
        Map<NodeStat.Key, Double> totals = s == null ? Map.of() : tree.totals(s.getAllocated());

        drawLeftColumn(g, s, player, cd);
        drawStats(g, totals);
        drawList(g, s, vx, vy);

        button(g, BTN_TREE, Component.translatable("screen.passivetree.tree_button"), Ui.inside(vx, vy, BTN_TREE[0], BTN_TREE[1], BTN_TREE[2], BTN_TREE[3]));
        button(g, BTN_CLOSE, Component.translatable("screen.passivetree.close"), Ui.inside(vx, vy, BTN_CLOSE[0], BTN_CLOSE[1], BTN_CLOSE[2], BTN_CLOSE[3]));

        g.pose().popPose();
    }

    private void drawLeftColumn(GuiGraphics g, PlayerSkills s, LocalPlayer player, SkillTree.ClassDef cd) {
        int cx = 64;
        if (cd != null) {
            Tex.emblem(g, cd.id(), cx - 28, 32, 56);
            Ui.drawCenteredFit(g, font, Component.translatable("class.passivetree." + cd.id()), cx, 94, 100, cd.color());
        } else {
            Ui.drawCenteredFit(g, font, Component.translatable("status.passivetree.no_class"), cx, 94, 100, Ui.TEXT_DIM);
        }

        int level = ClientStats.received ? ClientStats.level : (player == null ? 0 : player.experienceLevel);
        float prog = ClientStats.received ? ClientStats.xpProgress : (player == null ? 0f : player.experienceProgress);
        g.drawCenteredString(font, Component.translatable("status.passivetree.level", level), cx, 106, 0xFFFFFFFF);
        g.fill(14, 118, 114, 123, 0xFF05050A);
        g.fill(15, 119, 113, 122, 0xFF1C1C28);
        g.fill(15, 119, 15 + (int) (98 * Mth.clamp(prog, 0f, 1f)), 122, 0xFF7CCB3C);

        Ui.divider(g, 14, 132, 100);

        if (s == null) return;
        int notables = 0, keystones = 0;
        for (String id : s.getAllocated()) {
            SkillNode n = tree.nodes.get(id);
            if (n == null) continue;
            if (n.type == SkillNode.Type.NOTABLE) notables++;
            else if (n.type == SkillNode.Type.KEYSTONE) keystones++;
        }
        int totalNotables = 0, totalKeystones = 0;
        for (SkillNode n : tree.nodes.values()) {
            if (n.type == SkillNode.Type.NOTABLE) totalNotables++;
            else if (n.type == SkillNode.Type.KEYSTONE) totalKeystones++;
        }
        int total = s.totalPoints();
        int spent = s.spentPoints(tree);
        g.drawString(font, Component.translatable("status.passivetree.points_available", total - spent), 14, 140, Ui.GOLD, false);
        g.drawString(font, Component.translatable("status.passivetree.points_spent", spent, total), 14, 152, 0xFFB0B0C0, false);
        g.drawString(font, Component.translatable("status.passivetree.nodes", s.getAllocated().size(), tree.nodes.size()), 14, 170, 0xFFB0B0C0, false);
        g.drawString(font, Component.translatable("status.passivetree.notables", notables, totalNotables), 14, 182, 0xFFE8D26A, false);
        g.drawString(font, Component.translatable("status.passivetree.keystones", keystones, totalKeystones), 14, 194, 0xFFD9822B, false);
    }

    private Component attrName(int idx) {
        return Component.translatable("attribute.name.generic." + StatDefs.ATTRS[idx].substring("minecraft:generic.".length()));
    }

    private void drawStats(GuiGraphics g, Map<NodeStat.Key, Double> totals) {
        int x = 124, w = 160;
        double dmg = ClientStats.value(StatDefs.ATTACK_DAMAGE, 1.0);
        double aspd = ClientStats.value(StatDefs.ATTACK_SPEED, 4.0);
        double maxHp = ClientStats.value(StatDefs.MAX_HEALTH, 20.0);
        double armor = ClientStats.value(StatDefs.ARMOR, 0.0);
        double tough = ClientStats.value(StatDefs.TOUGHNESS, 0.0);
        double kb = ClientStats.value(StatDefs.KNOCKBACK, 0.0);
        double move = ClientStats.value(StatDefs.MOVE_SPEED, 0.1);
        double luck = ClientStats.value(StatDefs.LUCK, 0.0);

        // Offence
        int y = 34;
        sectionHeader(g, x, y, w, "status.passivetree.section.offence");
        y = 48;
        row(g, x, y, w, StatDefs.ATTACK_DAMAGE, attrName(StatDefs.ATTACK_DAMAGE), bonus(StatDefs.ATTACK_DAMAGE, totals), NUM.format(dmg));
        row(g, x, y + 12, w, StatDefs.ATTACK_SPEED, attrName(StatDefs.ATTACK_SPEED), bonus(StatDefs.ATTACK_SPEED, totals), NUM.format(aspd) + "/s");
        row(g, x, y + 24, w, -1, Component.translatable("status.passivetree.dps"), "", NUM.format(dmg * aspd));

        // Defence
        y = 92;
        sectionHeader(g, x, y, w, "status.passivetree.section.defence");
        y = 106;
        String hp = ClientStats.received ? NUM.format(ClientStats.health) + " / " + NUM.format(maxHp) : NUM.format(maxHp);
        row(g, x, y, w, StatDefs.MAX_HEALTH, attrName(StatDefs.MAX_HEALTH), bonus(StatDefs.MAX_HEALTH, totals), hp);
        row(g, x, y + 12, w, StatDefs.ARMOR, attrName(StatDefs.ARMOR), bonus(StatDefs.ARMOR, totals), NUM.format(armor));
        row(g, x, y + 24, w, StatDefs.TOUGHNESS, attrName(StatDefs.TOUGHNESS), bonus(StatDefs.TOUGHNESS, totals), NUM.format(tough));
        row(g, x, y + 36, w, StatDefs.KNOCKBACK, attrName(StatDefs.KNOCKBACK), bonus(StatDefs.KNOCKBACK, totals), NUM.format(kb * 100) + "%");
        // vanilla armor formula against a 10 damage hit
        double reduction = Mth.clamp(Math.max(armor / 5.0, armor - 4.0 * 10.0 / (tough + 8.0)), 0.0, 20.0) / 25.0;
        row(g, x, y + 48, w, -1, Component.translatable("status.passivetree.dmg_reduction"), "", NUM.format(reduction * 100) + "%");

        // Utility
        y = 174;
        sectionHeader(g, x, y, w, "status.passivetree.section.utility");
        y = 188;
        row(g, x, y, w, StatDefs.MOVE_SPEED, attrName(StatDefs.MOVE_SPEED), bonus(StatDefs.MOVE_SPEED, totals), NUM.format(move / 0.1 * 100) + "%");
        row(g, x, y + 12, w, StatDefs.LUCK, attrName(StatDefs.LUCK), bonus(StatDefs.LUCK, totals), NUM.format(luck));
    }

    private void drawList(GuiGraphics g, PlayerSkills s, double vx, double vy) {
        sectionHeader(g, LIST_X, 34, LIST_W, "status.passivetree.list_title");
        List<SkillNode> list = highlightedNodes(s);
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable("status.passivetree.list_empty"), LIST_X, LIST_Y, Ui.TEXT_DIM, false);
            return;
        }
        int maxScroll = Math.max(0, list.size() - LIST_ROWS);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        for (int i = 0; i < LIST_ROWS && scroll + i < list.size(); i++) {
            SkillNode n = list.get(scroll + i);
            int y = LIST_Y + i * LIST_ROW_H;
            boolean key = n.type == SkillNode.Type.KEYSTONE;
            int col = key ? 0xFFD9822B : 0xFFE8D26A;
            g.fill(LIST_X, y + 3, LIST_X + 4, y + 7, col);
            String name = font.plainSubstrByWidth(n.name, LIST_W - 10);
            g.drawString(font, name, LIST_X + 8, y, key ? 0xFFFFC080 : 0xFFD0D0DC, false);
        }
        if (maxScroll > 0) {
            // scroll bar
            int barH = LIST_ROWS * LIST_ROW_H;
            int thumbH = Math.max(8, barH * LIST_ROWS / list.size());
            int thumbY = LIST_Y + (barH - thumbH) * scroll / maxScroll;
            g.fill(LIST_X + LIST_W + 2, LIST_Y, LIST_X + LIST_W + 3, LIST_Y + barH, 0xFF2A2A3A);
            g.fill(LIST_X + LIST_W + 1, thumbY, LIST_X + LIST_W + 4, thumbY + thumbH, Ui.GOLD_DARK);
        }
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double vx = (mx - ox()) / scale();
        double vy = (my - oy()) / scale();
        if (button == 0) {
            if (Ui.inside(vx, vy, BTN_TREE[0], BTN_TREE[1], BTN_TREE[2], BTN_TREE[3])) {
                ClientForgeEvents.openTree();
                return true;
            }
            if (Ui.inside(vx, vy, BTN_CLOSE[0], BTN_CLOSE[1], BTN_CLOSE[2], BTN_CLOSE[3])) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double vx = (mx - ox()) / scale();
        double vy = (my - oy()) / scale();
        if (Ui.inside(vx, vy, LIST_X, LIST_Y, LIST_W + 6, LIST_ROWS * LIST_ROW_H)) {
            scroll += delta > 0 ? -1 : 1;
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientModEvents.STATUS_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (ClientModEvents.OPEN_KEY.matches(keyCode, scanCode)) {
            ClientForgeEvents.openTree();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
