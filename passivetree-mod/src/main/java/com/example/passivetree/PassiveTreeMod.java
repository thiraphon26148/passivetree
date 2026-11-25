package com.example.passivetree;

import com.example.passivetree.network.Net;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PassiveTreeMod.MOD_ID)
public class PassiveTreeMod {
    public static final String MOD_ID = "passivetree";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PassiveTreeMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Net.register();
            LOGGER.info("Passive Tree loaded {} nodes", com.example.passivetree.tree.SkillTree.get().nodes.size());
        });
    }
}
