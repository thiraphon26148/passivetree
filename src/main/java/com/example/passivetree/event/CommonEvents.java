package com.example.passivetree.event;

import com.example.passivetree.PassiveTreeMod;
import com.example.passivetree.SkillService;
import com.example.passivetree.command.ModCommands;
import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.data.PlayerSkillsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PassiveTreeMod.MOD_ID)
public class CommonEvents {

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            if (!player.getCapability(ModCapabilities.PLAYER_SKILLS).isPresent()) {
                PlayerSkillsProvider provider = new PlayerSkillsProvider();
                event.addCapability(new ResourceLocation(PassiveTreeMod.MOD_ID, "skills"), provider);
                event.addListener(provider::invalidate);
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(ModCapabilities.PLAYER_SKILLS).ifPresent(old ->
                event.getEntity().getCapability(ModCapabilities.PLAYER_SKILLS).ifPresent(neu -> neu.copyFrom(old)));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SkillService.applyAttributes(sp);
            SkillService.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SkillService.applyAttributes(sp);
            SkillService.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SkillService.sync(sp);
        }
    }

    /** Passive points = highest XP level the player has ever reached (+ bonus points from commands). */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;
        SkillService.get(sp).ifPresent(s -> {
            if (sp.experienceLevel > s.getHighestLevel()) {
                s.setHighestLevel(sp.experienceLevel);
                SkillService.sync(sp);
            }
        });
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}
