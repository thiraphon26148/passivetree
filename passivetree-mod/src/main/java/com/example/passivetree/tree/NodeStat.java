package com.example.passivetree.tree;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class NodeStat {
    public enum Op {
        ADD(AttributeModifier.Operation.ADDITION),
        MULT_BASE(AttributeModifier.Operation.MULTIPLY_BASE),
        MULT_TOTAL(AttributeModifier.Operation.MULTIPLY_TOTAL);

        public final AttributeModifier.Operation mc;

        Op(AttributeModifier.Operation mc) {
            this.mc = mc;
        }

        public static Op parse(String s) {
            return switch (s.toLowerCase()) {
                case "mult_base" -> MULT_BASE;
                case "mult_total" -> MULT_TOTAL;
                default -> ADD;
            };
        }
    }

    public record Key(ResourceLocation attribute, Op op) {}

    public final ResourceLocation attribute;
    public final Op op;
    public final double value;

    public NodeStat(ResourceLocation attribute, Op op, double value) {
        this.attribute = attribute;
        this.op = op;
        this.value = value;
    }

    public Key key() {
        return new Key(attribute, op);
    }
}
