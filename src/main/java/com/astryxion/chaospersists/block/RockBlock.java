package com.astryxion.chaospersists.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class RockBlock extends Block {

    public RockBlock() {
        super(net.minecraft.world.level.block.Block.Properties.of()
                .strength(2.0f, 1.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }
}
