package com.example.passivetree.network;

import com.example.passivetree.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: "send me my current attribute values". */
public class RequestStatsPacket {
    public RequestStatsPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static RequestStatsPacket decode(FriendlyByteBuf buf) {
        return new RequestStatsPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p != null) SkillService.sendStats(p);
        });
        c.setPacketHandled(true);
    }
}
