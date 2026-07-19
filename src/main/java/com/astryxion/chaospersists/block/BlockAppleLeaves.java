package com.astryxion.chaospersists.block;

import com.astryxion.chaospersists.core.ChaosPersists;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.IForgeShearable;

public class BlockAppleLeaves extends LeavesBlock implements IForgeShearable {

    public BlockAppleLeaves() {
        super(net.minecraft.world.level.block.Block.Properties.copy(Blocks.OAK_LEAVES).sound(SoundType.GRASS).randomTicks());
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        RandomSource random = level.getRandom();
        if (random.nextInt(25) == 1) {
            popResource(level, pos, new ItemStack(Items.APPLE));
        }
        if (random.nextInt(500) == 2) {
            popResource(level, pos, new ItemStack(Items.GOLDEN_APPLE));
        }
        if (random.nextInt(1000) == 3) {
            popResource(level, pos, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        }
        if (random.nextInt(10000) == 4) {
            popResource(level, pos, new ItemStack(ChaosPersists.MagicApple));
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public boolean isShearable(ItemStack item, Level level, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(net.minecraft.world.entity.player.Player player, ItemStack item, Level level, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 1;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        if (ChaosPersists.FastGraphicsLeaves == 0 && adjacentState.getBlock() == this) {
            return true;
        }
        return super.skipRendering(state, adjacentState, side);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return ChaosPersists.FastGraphicsLeaves != 0;
    }
}
