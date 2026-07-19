package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ItemExperienceTreeSeed extends Item {

    public ItemExperienceTreeSeed(int i) {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState ground = level.getBlockState(pos);
        if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT) && !ground.is(Blocks.FARMLAND)) {
            return InteractionResult.FAIL;
        }

        BlockPos above = pos.above();
        if (!level.isEmptyBlock(above)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            for (int j1 = 0; j1 < 10; ++j1) {
                level.addParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        (double) ((float) pos.getX() + level.random.nextFloat()),
                        (double) pos.getY() + 1.0 + (double) level.random.nextFloat(),
                        (double) ((float) pos.getZ() + level.random.nextFloat()),
                        0.0,
                        0.0,
                        0.0);
            }
            return InteractionResult.SUCCESS;
        }

        Block plant = ChaosPersists.MyExperiencePlant;
        if (plant == null) {
            return InteractionResult.FAIL;
        }

        level.setBlock(above, plant.defaultBlockState(), 2);
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
