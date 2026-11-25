package com.example.passivetree.network;

import com.example.passivetree.client.ClientPacketHandler;
import com.example.passivetree.data.PlayerSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server -> client: the full state of the player's passive tree. */
public class SyncPacket {
    public final String classId;
    public final List<String> allocated;
    public final int highestLevel;
    public final int bonusPoints;

    public SyncPacket(String classId, List<String> allocated, int highestLevel, int bonusPoints) {
        this.classId = classId;
        this.allocated = allocated;
        this.highestLevel = highestLevel;
        this.bonusPoints = bonusPoints;
    }

    public static SyncPacket of(PlayerSkills s) {
        return new SyncPacket(s.getClassId(), new ArrayList<>(s.getAllocated()), s.getHighestLevel(), s.getBonusPoints());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(classId);
        buf.writeVarInt(allocated.size());
        for (String id : allocated) buf.writeUtf(id);
        buf.writeVarInt(highestLevel);
        buf.writeVarInt(bonusPoints);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        String classId = buf.readUtf();
        int n = buf.readVarInt();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(buf.readUtf());
        int level = buf.readVarInt();
        int bonus = buf.readVarInt();
        return new SyncPacket(classId, list, level, bonus);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSync(this)));
        c.setPacketHandled(true);
    }
}
