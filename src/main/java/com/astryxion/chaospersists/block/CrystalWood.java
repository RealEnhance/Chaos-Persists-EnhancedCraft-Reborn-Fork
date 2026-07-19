package com.astryxion.chaospersists.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class CrystalWood extends Block {

    public CrystalWood(float hardness, float resistance) {
        super(
                net.minecraft.world.level.block.Block.Properties.of()
                        .sound(SoundType.WOOD)
                        .strength(hardness, resistance)
                        .noOcclusion());
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, net.minecraft.core.Direction side) {
        return false;
    }
}
