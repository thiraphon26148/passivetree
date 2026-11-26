package com.example.passivetree.client;

import com.example.passivetree.PassiveTreeMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PassiveTreeMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {
    public static final KeyMapping OPEN_KEY =
            new KeyMapping("key.passivetree.open", InputConstants.KEY_K, "key.categories.passivetree");
    public static final KeyMapping STATUS_KEY =
            new KeyMapping("key.passivetree.status", InputConstants.KEY_C, "key.categories.passivetree");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
        event.register(STATUS_KEY);
    }
}
