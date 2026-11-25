package com.example.passivetree.network;

import com.example.passivetree.PassiveTreeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PassiveTreeMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        CHANNEL.registerMessage(id++, ChooseClassPacket.class, ChooseClassPacket::encode, ChooseClassPacket::decode, ChooseClassPacket::handle);
        CHANNEL.registerMessage(id++, AllocatePacket.class, AllocatePacket::encode, AllocatePacket::decode, AllocatePacket::handle);
        CHANNEL.registerMessage(id++, DeallocatePacket.class, DeallocatePacket::encode, DeallocatePacket::decode, DeallocatePacket::handle);
        CHANNEL.registerMessage(id++, RespecPacket.class, RespecPacket::encode, RespecPacket::decode, RespecPacket::handle);
    }

    private Net() {}
}
