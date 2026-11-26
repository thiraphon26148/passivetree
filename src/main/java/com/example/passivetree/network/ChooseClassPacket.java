package com.example.passivetree.network;

import com.example.passivetree.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChooseClassPacket {
    private final String classId;

    public ChooseClassPacket(String classId) {
        this.classId = classId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(classId, 64);
    }

    public static ChooseClassPacket decode(FriendlyByteBuf buf) {
        return new ChooseClassPacket(buf.readUtf(64));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p != null) SkillService.chooseClass(p, classId);
        });
        c.setPacketHandled(true);
    }
}
