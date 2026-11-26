package com.example.passivetree.tree;

import java.util.List;

public final class SkillNode {
    public enum Type { START, SMALL, NOTABLE, KEYSTONE }

    public final String id;
    public final String name;
    public final Type type;
    public final float x;
    public final float y;
    public final List<NodeStat> stats;

    public SkillNode(String id, String name, Type type, float x, float y, List<NodeStat> stats) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.stats = stats;
    }

    public int cost() {
        return type == Type.KEYSTONE ? 2 : 1;
    }
}
