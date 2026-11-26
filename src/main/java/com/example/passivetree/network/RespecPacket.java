package com.example.passivetree.network;

import com.example.passivetree.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RespecPacket {
    public RespecPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static RespecPacket decode(FriendlyByteBuf buf) {
        return new RespecPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p != null) SkillService.respec(p);
        });
        c.setPacketHandled(true);
    }
}
