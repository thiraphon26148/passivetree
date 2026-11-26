package com.example.passivetree.command;

import com.example.passivetree.SkillService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("passivetree")
                .then(Commands.literal("points")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            SkillService.addPoints(p, amount);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.passivetree.points_added", amount), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("respec")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            SkillService.respec(p);
                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.passivetree.respec_done"), false);
                            return 1;
                        }))
                .then(Commands.literal("resetclass")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            SkillService.resetClass(p);
                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.passivetree.class_reset"), false);
                            return 1;
                        })));
    }

    private ModCommands() {}
}
