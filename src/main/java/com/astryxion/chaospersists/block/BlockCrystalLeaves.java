package com.astryxion.chaospersists.block;

import com.astryxion.chaospersists.core.ChaosPersists;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.IForgeShearable;

public class BlockCrystalLeaves extends LeavesBlock implements IForgeShearable {

    public BlockCrystalLeaves() {
        super(net.minecraft.world.level.block.Block.Properties.copy(Blocks.OAK_LEAVES).sound(SoundType.GRASS).randomTicks());
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        RandomSource random = level.getRandom();
        if (random.nextInt(100) == 1) {
            popResource(level, pos, new ItemStack(ChaosPersists.MyCrystalApple));
        }
        if (random.nextInt(50) == 1) {
            if (this == ChaosPersists.MyCrystalLeaves) {
                popResource(level, pos, new ItemStack(ChaosPersists.MyCrystalPlant));
            }
            if (this == ChaosPersists.MyCrystalLeaves2) {
                popResource(level, pos, new ItemStack(ChaosPersists.MyCrystalPlant2));
            }
            if (this == ChaosPersists.MyCrystalLeaves3) {
                popResource(level, pos, new ItemStack(ChaosPersists.MyCrystalPlant3));
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public List<ItemStack> onSheared(net.minecraft.world.entity.player.Player player, ItemStack item, Level level, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
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
