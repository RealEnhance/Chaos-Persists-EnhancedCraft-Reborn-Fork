package com.astryxion.chaospersists.item;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 1.12 {@code ItemSeeds}: plants crop on configured soil (no food). */
public class ItemStrawberrySeed extends BlockItem {
    private final Block soilBlock;

    public ItemStrawberrySeed(Block cropBlock, Block soilBlock) {
        super(cropBlock, new Item.Properties());
        this.soilBlock = soilBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }
        BlockState ground = context.getLevel().getBlockState(context.getClickedPos());
        if (ground.getBlock() != this.soilBlock) {
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }
}
