package com.example.passivetree.client;

import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.data.PlayerSkills;
import com.example.passivetree.network.ChooseClassPacket;
import com.example.passivetree.network.Net;
import com.example.passivetree.tree.SkillTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class ClassSelectScreen extends Screen {
    private static final int VW = 400, VH = 250;
    private static final int CW = 88, CH = 84, GAP = 8;

    private final SkillTree tree = SkillTree.get();
    private int hovered = -1;

    public ClassSelectScreen() {
        super(Component.translatable("screen.passivetree.choose_class"));
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

    private int cardX(int i) {
        int total = 4 * CW + 3 * GAP;
        return (VW - total) / 2 + (i % 4) * (CW + GAP);
    }

    private int cardY(int i) {
        return 74 + (i / 4) * (CH + GAP);
    }

    private int cardAt(double mx, double my) {
        double vx = (mx - ox()) / scale();
        double vy = (my - oy()) / scale();
        for (int i = 0; i < tree.classes.size(); i++) {
            if (Ui.inside(vx, vy, cardX(i), cardY(i), CW, CH)) return i;
        }
        return -1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        PlayerSkills s = skills();
        if (s != null && !s.getClassId().isEmpty()) {
            Minecraft.getInstance().setScreen(new SkillTreeScreen());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Tex.background(g, width, height, 0, 0);
        hovered = cardAt(mx, my);

        float s = scale();
        g.pose().pushPose();
        g.pose().translate(ox(), oy(), 0);
        g.pose().scale(s, s, 1f);

        Tex.logo(g, VW / 2, 4, 250);
        Component title = Component.translatable("screen.passivetree.choose_class");
        g.drawCenteredString(font, title, VW / 2, 50, Ui.GOLD);
        g.drawCenteredString(font, Component.translatable("screen.passivetree.choose_hint"), VW / 2, 62, Ui.TEXT_DIM);

        for (int i = 0; i < tree.classes.size(); i++) {
            SkillTree.ClassDef cd = tree.classes.get(i);
            int x = cardX(i), y = cardY(i);
            boolean hov = i == hovered;
            int col = cd.color();
            int border = hov ? col : ((col & 0x00FFFFFF) | 0x80000000);
            g.fill(x - 1, y - 1, x + CW + 1, y + CH + 1, border);
            g.fillGradient(x, y, x + CW, y + CH, hov ? 0xFF262634 : 0xF0181822, 0xF00D0D14);
            Tex.emblem(g, cd.id(), x + (CW - 40) / 2, y + 6, 40);
            Ui.drawCenteredFit(g, font, Component.translatable("class.passivetree." + cd.id()),
                    x + CW / 2, y + 50, CW - 6, col);
            List<FormattedCharSequence> lines = font.split(
                    Component.translatable("class.passivetree." + cd.id() + ".desc"), CW - 8);
            for (int k = 0; k < Math.min(2, lines.size()); k++) {
                g.drawString(font, lines.get(k), x + 4, y + 62 + k * 9, 0xFFA8A8B8, false);
            }
        }
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = cardAt(mx, my);
        if (button == 0 && i >= 0) {
            Net.CHANNEL.sendToServer(new ChooseClassPacket(tree.classes.get(i).id()));
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mx, my, button);
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
