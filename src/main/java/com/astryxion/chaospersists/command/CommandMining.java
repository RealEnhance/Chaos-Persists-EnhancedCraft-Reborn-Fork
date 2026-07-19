package com.astryxion.chaospersists.command;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.world.dimension.teleporter.MiningTeleporter;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class CommandMining {
    private CommandMining() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("mining")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> execute(ctx.getSource())));
    }

    private static int execute(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(
                    Component.literal("This command can only be used by a player.").withStyle(ChatFormatting.RED));
            return 0;
        }
        ResourceKey<Level> miningKey = ChaosPersists.getMiningDimensionKey();
        if (player.level().dimension().equals(miningKey)) {
            player.sendSystemMessage(
                    Component.literal("You are already in the Mining dimension.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        ServerLevel world = player.server.getLevel(miningKey);
        if (world == null) {
            player.sendSystemMessage(
                    Component.literal("Mining dimension is not available.").withStyle(ChatFormatting.RED));
            return 0;
        }
        MiningTeleporter teleporter = new MiningTeleporter(player.getX(), player.getZ());
        player.changeDimension(world, teleporter);
        player.sendSystemMessage(Component.literal("Teleported to Mining.").withStyle(ChatFormatting.GREEN));
        return 1;
    }
}
