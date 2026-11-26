package com.example.passivetree.tree;

import net.minecraft.resources.ResourceLocation;

/** The attributes shown on the status screen. The order matches the cells of textures/gui/icons.png. */
public final class StatDefs {
    public static final String[] ATTRS = {
            "minecraft:generic.attack_damage",
            "minecraft:generic.max_health",
            "minecraft:generic.armor",
            "minecraft:generic.armor_toughness",
            "minecraft:generic.knockback_resistance",
            "minecraft:generic.movement_speed",
            "minecraft:generic.attack_speed",
            "minecraft:generic.luck"
    };

    public static final int ATTACK_DAMAGE = 0;
    public static final int MAX_HEALTH = 1;
    public static final int ARMOR = 2;
    public static final int TOUGHNESS = 3;
    public static final int KNOCKBACK = 4;
    public static final int MOVE_SPEED = 5;
    public static final int ATTACK_SPEED = 6;
    public static final int LUCK = 7;

    /** Icon cell for an attribute, or -1 if there is none. */
    public static int iconIndex(ResourceLocation attribute) {
        String s = attribute.toString();
        for (int i = 0; i < ATTRS.length; i++) {
            if (ATTRS[i].equals(s)) return i;
        }
        return -1;
    }

    private StatDefs() {}
}
