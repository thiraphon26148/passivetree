package com.example.passivetree.network;

import com.example.passivetree.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AllocatePacket {
    private final String nodeId;

    public AllocatePacket(String nodeId) {
        this.nodeId = nodeId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(nodeId, 64);
    }

    public static AllocatePacket decode(FriendlyByteBuf buf) {
        return new AllocatePacket(buf.readUtf(64));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p != null) SkillService.allocate(p, nodeId);
        });
        c.setPacketHandled(true);
    }
}
