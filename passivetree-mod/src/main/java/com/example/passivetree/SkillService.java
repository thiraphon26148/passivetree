package com.example.passivetree;

import com.example.passivetree.data.ModCapabilities;
import com.example.passivetree.data.PlayerSkills;
import com.example.passivetree.network.Net;
import com.example.passivetree.network.SyncPacket;
import com.example.passivetree.tree.NodeStat;
import com.example.passivetree.tree.SkillNode;
import com.example.passivetree.tree.SkillTree;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** All game rules for the passive tree. Runs on the server (the can* checks are also used by the client UI). */
public final class SkillService {

    public static Optional<PlayerSkills> get(Player p) {
        return p.getCapability(ModCapabilities.PLAYER_SKILLS).resolve();
    }

    // ------------------------------------------------------------ rules

    public static boolean canAllocate(PlayerSkills s, SkillTree tree, SkillNode node) {
        if (s.getClassId().isEmpty()) return false;
        if (s.isAllocated(node.id)) return false;
        if (s.availablePoints(tree) < node.cost()) return false;
        for (String n : tree.neighbors(node.id)) {
            if (s.isAllocated(n)) return true;
        }
        return false;
    }

    /** A node can be refunded if it is allocated, is not the class start, and everything else stays connected to the start. */
    public static boolean canDeallocate(PlayerSkills s, SkillTree tree, SkillNode node) {
        SkillTree.ClassDef cd = tree.classById(s.getClassId());
        if (cd == null) return false;
        if (!s.isAllocated(node.id) || node.id.equals(cd.start())) return false;
        Set<String> remaining = new HashSet<>(s.getAllocated());
        remaining.remove(node.id);
        return reachable(tree, cd.start(), remaining).size() == remaining.size();
    }

    /** All nodes in {@code allowed} that can be reached from {@code start} by walking only through {@code allowed}. */
    public static Set<String> reachable(SkillTree tree, String start, Set<String> allowed) {
        Set<String> seen = new HashSet<>();
        if (!allowed.contains(start)) return seen;
        Deque<String> stack = new ArrayDeque<>();
        stack.push(start);
        seen.add(start);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            for (String nb : tree.neighbors(cur)) {
                if (allowed.contains(nb) && seen.add(nb)) {
                    stack.push(nb);
                }
            }
        }
        return seen;
    }

    /** Repairs saved data after the tree was changed: unknown nodes and nodes cut off from the start are dropped. */
    public static void sanitize(PlayerSkills s, SkillTree tree) {
        SkillTree.ClassDef cd = tree.classById(s.getClassId());
        if (cd == null) {
            s.setClassId("");
            s.getAllocated().clear();
            return;
        }
        s.getAllocated().removeIf(id -> !tree.nodes.containsKey(id));
        s.getAllocated().add(cd.start());
        s.getAllocated().retainAll(reachable(tree, cd.start(), new HashSet<>(s.getAllocated())));
    }

    // ------------------------------------------------------------ actions

    public static void chooseClass(ServerPlayer p, String classId) {
        SkillTree tree = SkillTree.get();
        SkillTree.ClassDef cd = tree.classById(classId);
        if (cd == null) return;
        get(p).ifPresent(s -> {
            if (!s.getClassId().isEmpty()) return; // already chosen
            s.setClassId(cd.id());
            s.getAllocated().clear();
            s.getAllocated().add(cd.start());
            applyAttributes(p);
            sync(p);
        });
    }

    public static void allocate(ServerPlayer p, String nodeId) {
        SkillTree tree = SkillTree.get();
        SkillNode node = tree.nodes.get(nodeId);
        if (node == null) return;
        get(p).ifPresent(s -> {
            if (!canAllocate(s, tree, node)) return;
            s.getAllocated().add(nodeId);
            applyAttributes(p);
            sync(p);
        });
    }

    public static void deallocate(ServerPlayer p, String nodeId) {
        SkillTree tree = SkillTree.get();
        SkillNode node = tree.nodes.get(nodeId);
        if (node == null) return;
        get(p).ifPresent(s -> {
            if (!canDeallocate(s, tree, node)) return;
            s.getAllocated().remove(nodeId);
            applyAttributes(p);
            sync(p);
        });
    }

    /** Free full respec (keeps the class). */
    public static void respec(ServerPlayer p) {
        SkillTree tree = SkillTree.get();
        get(p).ifPresent(s -> {
            SkillTree.ClassDef cd = tree.classById(s.getClassId());
            if (cd == null) return;
            s.getAllocated().clear();
            s.getAllocated().add(cd.start());
            applyAttributes(p);
            sync(p);
        });
    }

    public static void resetClass(ServerPlayer p) {
        get(p).ifPresent(s -> {
            s.setClassId("");
            s.getAllocated().clear();
            applyAttributes(p);
            sync(p);
        });
    }

    public static void addPoints(ServerPlayer p, int amount) {
        get(p).ifPresent(s -> {
            s.addBonusPoints(amount);
            sync(p);
        });
    }

    public static void sync(ServerPlayer p) {
        get(p).ifPresent(s -> Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), SyncPacket.of(s)));
    }

    // ------------------------------------------------------------ attributes

    private static UUID modifierId(NodeStat.Key key) {
        String seed = PassiveTreeMod.MOD_ID + "/" + key.attribute() + "/" + key.op().name();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    /** Recalculates every attribute modifier from the allocated nodes. Safe to call any time. */
    public static void applyAttributes(ServerPlayer p) {
        SkillTree tree = SkillTree.get();
        Optional<PlayerSkills> opt = get(p);
        if (opt.isEmpty()) return;
        PlayerSkills s = opt.get();

        sanitize(s, tree);

        // 1) remove every modifier this mod could have added
        for (SkillNode node : tree.nodes.values()) {
            for (NodeStat stat : node.stats) {
                AttributeInstance inst = instance(p, stat);
                if (inst != null) inst.removeModifier(modifierId(stat.key()));
            }
        }

        // 2) sum the allocated nodes per (attribute, operation)
        Map<NodeStat.Key, Double> totals = new HashMap<>();
        for (String id : s.getAllocated()) {
            SkillNode node = tree.nodes.get(id);
            if (node == null) continue;
            for (NodeStat stat : node.stats) {
                totals.merge(stat.key(), stat.value, Double::sum);
            }
        }

        // 3) add one modifier per key
        for (Map.Entry<NodeStat.Key, Double> e : totals.entrySet()) {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(e.getKey().attribute());
            if (attr == null) continue;
            AttributeInstance inst = p.getAttribute(attr);
            if (inst == null) continue;
            inst.addPermanentModifier(new AttributeModifier(
                    modifierId(e.getKey()), "Passive tree", e.getValue(), e.getKey().op().mc));
        }

        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }

    private static AttributeInstance instance(ServerPlayer p, NodeStat stat) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(stat.attribute);
        return attr == null ? null : p.getAttribute(attr);
    }

    private SkillService() {}
}
