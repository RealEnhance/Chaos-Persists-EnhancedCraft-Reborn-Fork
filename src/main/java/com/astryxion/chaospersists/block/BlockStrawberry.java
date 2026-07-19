package com.astryxion.chaospersists.block;

import com.astryxion.chaospersists.core.ChaosPersists;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.ItemLike;

public class BlockStrawberry extends CropBlock {

    public BlockStrawberry() {
        this(0);
    }

    public BlockStrawberry(int par1) {
        super(net.minecraft.world.level.block.Block.Properties.copy(Blocks.WHEAT).noOcclusion());
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ChaosPersists.MyStrawberry;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        RandomSource r = builder.getLevel().getRandom();
        return Collections.singletonList(new ItemStack(ChaosPersists.MyStrawberry, 1 + r.nextInt(5)));
    }
}
