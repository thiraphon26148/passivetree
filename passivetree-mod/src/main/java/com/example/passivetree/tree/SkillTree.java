package com.example.passivetree.tree;

import com.example.passivetree.PassiveTreeMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The whole passive tree, loaded once from /passivetree/tree.json inside the mod jar. */
public final class SkillTree {
    public record ClassDef(String id, String start, int color) {}
    public record Edge(SkillNode a, SkillNode b) {}

    private static SkillTree instance;

    public final Map<String, SkillNode> nodes = new LinkedHashMap<>();
    public final Map<String, Set<String>> adjacency = new HashMap<>();
    public final List<Edge> edges = new ArrayList<>();
    public final List<ClassDef> classes = new ArrayList<>();

    public static synchronized SkillTree get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public Set<String> neighbors(String id) {
        return adjacency.getOrDefault(id, Collections.emptySet());
    }

    public ClassDef classById(String id) {
        for (ClassDef c : classes) {
            if (c.id().equals(id)) return c;
        }
        return null;
    }

    private static SkillTree load() {
        SkillTree tree = new SkillTree();
        try (InputStream in = SkillTree.class.getResourceAsStream("/passivetree/tree.json")) {
            if (in == null) throw new IllegalStateException("tree.json not found");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

            for (JsonElement e : root.getAsJsonArray("classes")) {
                JsonObject o = e.getAsJsonObject();
                String color = GsonHelper.getAsString(o, "color", "#FFFFFF");
                int c = 0xFF000000 | Integer.parseInt(color.substring(1), 16);
                tree.classes.add(new ClassDef(GsonHelper.getAsString(o, "id"), GsonHelper.getAsString(o, "start"), c));
            }

            Map<String, List<String>> pending = new LinkedHashMap<>();
            for (JsonElement e : root.getAsJsonArray("nodes")) {
                JsonObject o = e.getAsJsonObject();
                String id = GsonHelper.getAsString(o, "id");
                SkillNode.Type type = SkillNode.Type.valueOf(GsonHelper.getAsString(o, "type").toUpperCase());

                List<NodeStat> stats = new ArrayList<>();
                if (o.has("stats")) {
                    for (JsonElement se : o.getAsJsonArray("stats")) {
                        JsonObject so = se.getAsJsonObject();
                        stats.add(new NodeStat(
                                new ResourceLocation(GsonHelper.getAsString(so, "attribute")),
                                NodeStat.Op.parse(GsonHelper.getAsString(so, "op", "add")),
                                GsonHelper.getAsDouble(so, "value")));
                    }
                }
                tree.nodes.put(id, new SkillNode(id, GsonHelper.getAsString(o, "name"), type,
                        GsonHelper.getAsFloat(o, "x"), GsonHelper.getAsFloat(o, "y"), stats));

                List<String> links = new ArrayList<>();
                if (o.has("links")) {
                    for (JsonElement le : o.getAsJsonArray("links")) links.add(le.getAsString());
                }
                pending.put(id, links);
            }

            Set<String> seen = new HashSet<>();
            for (Map.Entry<String, List<String>> en : pending.entrySet()) {
                for (String target : en.getValue()) {
                    SkillNode a = tree.nodes.get(en.getKey());
                    SkillNode b = tree.nodes.get(target);
                    if (b == null) {
                        PassiveTreeMod.LOGGER.warn("Unknown link target {} from {}", target, en.getKey());
                        continue;
                    }
                    String key = en.getKey().compareTo(target) < 0 ? en.getKey() + "|" + target : target + "|" + en.getKey();
                    if (!seen.add(key)) continue;
                    tree.adjacency.computeIfAbsent(a.id, k -> new HashSet<>()).add(b.id);
                    tree.adjacency.computeIfAbsent(b.id, k -> new HashSet<>()).add(a.id);
                    tree.edges.add(new Edge(a, b));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load passive tree", ex);
        }
        return tree;
    }
}
