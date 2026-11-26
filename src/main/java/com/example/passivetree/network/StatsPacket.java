package com.example.passivetree.network;

import com.example.passivetree.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: live attribute values (in the order of StatDefs.ATTRS) plus health and level. */
public class StatsPacket {
    public final double[] values;
    public final float health;
    public final int level;
    public final float xpProgress;

    public StatsPacket(double[] values, float health, int level, float xpProgress) {
        this.values = values;
        this.health = health;
        this.level = level;
        this.xpProgress = xpProgress;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(values.length);
        for (double v : values) buf.writeDouble(v);
        buf.writeFloat(health);
        buf.writeVarInt(level);
        buf.writeFloat(xpProgress);
    }

    public static StatsPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        double[] v = new double[n];
        for (int i = 0; i < n; i++) v[i] = buf.readDouble();
        return new StatsPacket(v, buf.readFloat(), buf.readVarInt(), buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStats(this)));
        c.setPacketHandled(true);
    }
}
