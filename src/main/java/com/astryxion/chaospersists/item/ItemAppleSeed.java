package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public class ItemAppleSeed extends Item {

    public ItemAppleSeed(int i) {
        this(new Properties().stacksTo(16));
    }

    public ItemAppleSeed(Properties properties) {
        super(properties);
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

        if (!level.isClientSide) {
            Block leaves = resolveLeavesBlock();
            if (leaves == null) {
                return InteractionResult.FAIL;
            }
            makeTree(level, pos.getX(), pos.getY(), pos.getZ(), leaves, null);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private Block resolveLeavesBlock() {
        if (this == ChaosPersists.MyAppleSeed) {
            return modBlock(ChaosPersists.MyAppleLeaves);
        }
        if (this == ChaosPersists.MyCherrySeed) {
            return modBlock(ChaosPersists.MyCherryLeaves);
        }
        return modBlock(ChaosPersists.MyPeachLeaves);
    }

    private static Block modBlock(Object field) {
        if (field instanceof Block block) {
            return block;
        }
        return (Block) (Object) field;
    }

    /** Legacy {@code World}/{@code Chunk} callers in {@link com.astryxion.chaospersists.core.ChaosWorld}. */
    public void makeTree(Object world, int x, int y, int z, Object blkid, Object chunk) {
        makeTree((Level) world, x, y, z, modBlock(blkid), chunk != null ? (LevelChunk) chunk : null);
    }

    public void makeTree(Level level, int x, int y, int z, Block blkid, LevelChunk chunk) {
        BlockPos base = new BlockPos(x, y, z);
        BlockState ground = level.getBlockState(base);
        if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT) && !ground.is(Blocks.FARMLAND)) {
            return;
        }

        LevelChunk refChunk = chunk != null ? chunk : level.getChunkAt(base);

        int h1 = 12;
        int h2 = 6;
        int h3 = 9;
        int h4 = 6;
        int h5 = 14;
        int w1 = 5;
        int w2 = 3;

        Block peachLeaves = modBlock(ChaosPersists.MyPeachLeaves);
        Block cherryLeaves = modBlock(ChaosPersists.MyCherryLeaves);
        Block appleLeaves = modBlock(ChaosPersists.MyAppleLeaves);

        if (blkid == peachLeaves) {
            h1 = 10;
            h2 = 5;
            h3 = 7;
            h4 = 5;
            h5 = 12;
            w1 = 4;
            w2 = 2;
        }

        if (blkid == cherryLeaves) {
            h1 = 8;
            h2 = 3;
            h3 = 5;
            h4 = 3;
            h5 = 10;
            w1 = 3;
            w2 = 1;
        }

        for (int j = 1; j < h1; j++) {
            level.setBlock(new BlockPos(x, y + j, z), Blocks.OAK_LOG.defaultBlockState(), 2);
        }

        for (int j = 1; j < w1; j++) {
            ChaosPersists.setBlockSuperFast(level, x + j, y + h2, z, Blocks.OAK_LOG, 0, 2, refChunk);
            ChaosPersists.setBlockSuperFast(level, x - j, y + h2, z, Blocks.OAK_LOG, 0, 2, refChunk);
            ChaosPersists.setBlockSuperFast(level, x, y + h2, z + j, Blocks.OAK_LOG, 0, 2, refChunk);
            ChaosPersists.setBlockSuperFast(level, x, y + h2, z - j, Blocks.OAK_LOG, 0, 2, refChunk);
        }

        for (int j = 1; j < w2; j++) {
            ChaosPersists.setBlockSuperFast(level, x + j, y + h3, z, Blocks.OAK_LOG, 0, 2, refChunk);
            ChaosPersists.setBlockSuperFast(level, x - j, y + h3, z, Blocks.OAK_LOG, 0, 2, refChunk);
            ChaosPersists.setBlockSuperFast(level, x, y + h3, z + j, Blocks.OAK_LOG, 0, 2, refChunk);
            ChaosPersists.setBlockSuperFast(level, x, y + h3, z - j, Blocks.OAK_LOG, 0, 2, refChunk);
        }

        for (int i = h4; i < h5; i++) {
            int width = 6;
            if (i > 8) {
                width = 5;
            }
            if (i > 10) {
                width = 4;
            }

            if (blkid != appleLeaves) {
                width--;
            }

            for (int j = -width; j <= width; j++) {
                for (int k = -width; k <= width; k++) {
                    BlockPos leafPos = new BlockPos(x + k, y + i, z + j);
                    if (!level.getBlockState(leafPos).isAir()) {
                        continue;
                    }
                    ChaosPersists.setBlockSuperFast(level, x + k, y + i, z + j, blkid, 0, 2, refChunk);
                }
            }
        }
    }
}
