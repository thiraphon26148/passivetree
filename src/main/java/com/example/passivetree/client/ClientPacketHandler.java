package com.example.passivetree.client;

import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.network.StatsPacket;
import com.example.passivetree.network.SyncPacket;
import net.minecraft.client.Minecraft;

/** Client-only code, only ever called through DistExecutor. */
public final class ClientPacketHandler {
    public static void handleSync(SyncPacket pk) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getCapability(ModCapabilities.PLAYER_SKILLS)
                .ifPresent(s -> s.setAll(pk.classId, pk.allocated, pk.highestLevel, pk.bonusPoints));
    }

    public static void handleStats(StatsPacket pk) {
        ClientStats.values = pk.values;
        ClientStats.health = pk.health;
        ClientStats.level = pk.level;
        ClientStats.xpProgress = pk.xpProgress;
        ClientStats.received = true;
    }

    private ClientPacketHandler() {}
}
