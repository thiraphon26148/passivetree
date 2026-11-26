package com.example.passivetree.client;

import com.example.passivetree.PassiveTreeMod;
import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.data.PlayerSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PassiveTreeMod.MOD_ID, value = Dist.CLIENT)
public class ClientForgeEvents {

    /** Opens the passive tree, or the class selection if the player has no class yet. */
    public static void openTree() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        PlayerSkills s = p.getCapability(ModCapabilities.PLAYER_SKILLS).orElse(null);
        boolean hasClass = s != null && !s.getClassId().isEmpty();
        mc.setScreen(hasClass ? new SkillTreeScreen() : new ClassSelectScreen());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.OPEN_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) openTree();
        }
        while (ClientModEvents.STATUS_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new StatusScreen());
        }
    }
}
