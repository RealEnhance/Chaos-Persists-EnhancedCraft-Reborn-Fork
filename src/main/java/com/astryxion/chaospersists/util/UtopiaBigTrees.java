package com.astryxion.chaospersists.util;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Utopia giant trees using vanilla logs/leaves (1.12 {@link Trees#SkyTree} / {@link Trees#WindTree} layout).
 */
public final class UtopiaBigTrees {
    private UtopiaBigTrees() {}

    private static void skyTreeBranch(Level level, int x, int y, int z, int length, int dirx, int dirz) {
        for (int i = 1; i < length; ++i) {
            ChaosPersists.setBlockFast(level, x + i * dirx, y, z + i * dirz, Blocks.OAK_LOG, 0, 2);
            BlockPos above = new BlockPos(x + i * dirx, y + 1, z + i * dirz);
            if (level.getBlockState(above).isAir()) {
                ChaosPersists.setBlockFast(level, x + i * dirx, y + 1, z + i * dirz, Blocks.OAK_LEAVES, 0, 2);
            }
            BlockPos side1 = new BlockPos(x + i * dirx + dirz, y, z + i * dirz + dirx);
            if (level.getBlockState(side1).isAir()) {
                ChaosPersists.setBlockFast(level, x + i * dirx + dirz, y, z + i * dirz + dirx, Blocks.OAK_LEAVES, 0, 2);
            }
            BlockPos side2 = new BlockPos(x + i * dirx - dirz, y, z + i * dirz - dirx);
            if (level.getBlockState(side2).isAir()) {
                ChaosPersists.setBlockFast(level, x + i * dirx - dirz, y, z + i * dirz - dirx, Blocks.OAK_LEAVES, 0, 2);
            }
        }
        BlockPos tip = new BlockPos(x + length * dirx, y, z + length * dirz);
        if (level.getBlockState(tip).isAir()) {
            ChaosPersists.setBlockFast(level, x + length * dirx, y, z + length * dirz, Blocks.OAK_LEAVES, 0, 2);
        }
    }

    /** Tall spreading canopy tree (1.12 SkyTree, vanilla oak). */
    public static void skyTree(Level level, int x, int y, int z) {
        Block ground = level.getBlockState(new BlockPos(x, y, z)).getBlock();
        if (ground != Blocks.GRASS_BLOCK && ground != Blocks.DIRT) {
            return;
        }
        RandomSource random = level.getRandom();
        int height = random.nextInt(15) + 190;
        if (height - y < 20) {
            return;
        }
        int width = random.nextInt(10) + 25;
        for (int j = y; j <= height; ++j) {
            ChaosPersists.setBlockFast(level, x, j, z, Blocks.OAK_LOG, 0, 2);
        }
        ChaosPersists.setBlockFast(level, x, height + 1, z, Blocks.OAK_LEAVES, 0, 2);
        skyTreeBranch(level, x, height, z, width, 1, 0);
        skyTreeBranch(level, x, height, z, width, -1, 0);
        skyTreeBranch(level, x, height, z, width, 0, 1);
        skyTreeBranch(level, x, height, z, width, 0, -1);
        height -= 5;
        width /= 3;
        skyTreeBranch(level, x, height -= random.nextInt(4), z, width, 1, 0);
        skyTreeBranch(level, x, height, z, width, -1, 0);
        skyTreeBranch(level, x, height, z, width, 0, 1);
        skyTreeBranch(level, x, height, z, width, 0, -1);
    }

    private static void windTreeBranch(Level level, int x, int y, int z, int length, int dirx, int dirz) {
        for (int i = 1; i <= length; ++i) {
            ChaosPersists.setBlockFast(level, x + i * dirx, y, z + i * dirz, Blocks.OAK_LOG, 0, 2);
            BlockPos above = new BlockPos(x + i * dirx, y + 1, z + i * dirz);
            if (level.getBlockState(above).isAir()) {
                ChaosPersists.setBlockFast(level, x + i * dirx, y + 1, z + i * dirz, Blocks.OAK_LEAVES, 0, 2);
            }
            if (i < length / 3) {
                BlockPos highLeaf = new BlockPos(x + i * dirx, y + 2, z + i * dirz);
                if (level.getBlockState(highLeaf).isAir()) {
                    ChaosPersists.setBlockFast(level, x + i * dirx, y + 2, z + i * dirz, Blocks.OAK_LEAVES, 0, 2);
                }
            }
            if (i > length / 3) {
                BlockPos side1 = new BlockPos(x + i * dirx + dirz, y, z + i * dirz + dirx);
                if (level.getBlockState(side1).isAir()) {
                    ChaosPersists.setBlockFast(level, x + i * dirx + dirz, y, z + i * dirz + dirx, Blocks.OAK_LEAVES, 0, 2);
                }
                BlockPos side2 = new BlockPos(x + i * dirx - dirz, y, z + i * dirz - dirx);
                if (level.getBlockState(side2).isAir()) {
                    ChaosPersists.setBlockFast(level, x + i * dirx - dirz, y, z + i * dirz - dirx, Blocks.OAK_LEAVES, 0, 2);
                }
            }
        }
        BlockPos tip1 = new BlockPos(x + (length + 1) * dirx, y, z + (length + 1) * dirz);
        if (level.getBlockState(tip1).isAir()) {
            ChaosPersists.setBlockFast(level, x + (length + 1) * dirx, y, z + (length + 1) * dirz, Blocks.OAK_LEAVES, 0, 2);
        }
        BlockPos tip2 = new BlockPos(x + (length + 2) * dirx, y, z + (length + 2) * dirz);
        if (level.getBlockState(tip2).isAir()) {
            ChaosPersists.setBlockFast(level, x + (length + 2) * dirx, y, z + (length + 2) * dirz, Blocks.OAK_LEAVES, 0, 2);
        }
    }

    /** Directional wind-shaped tree (1.12 WindTree, vanilla oak). */
    public static void windTree(Level level, int x, int y, int z, int dir) {
        if (dir < 0 || dir > 3) {
            return;
        }
        int dirx = 1;
        int dirz = 0;
        if (dir == 1) {
            dirx = -1;
        } else if (dir == 2) {
            dirx = 0;
            dirz = 1;
        } else if (dir == 3) {
            dirx = 0;
            dirz = -1;
        }
        Block ground = level.getBlockState(new BlockPos(x, y, z)).getBlock();
        if (ground != Blocks.GRASS_BLOCK && ground != Blocks.DIRT) {
            return;
        }
        RandomSource random = level.getRandom();
        int height = random.nextInt(8) + 40;
        int width = random.nextInt(4) + 8;
        for (int j = 0; j < height; ++j) {
            ChaosPersists.setBlockFast(level, x, j + y, z, Blocks.OAK_LOG, 0, 2);
            if (j > height / 5) {
                ChaosPersists.setBlockFast(level, x + dirx, j + y, z + dirz, Blocks.OAK_LEAVES, 0, 2);
            }
            if (j > height / 4 && j % 4 == 0) {
                windTreeBranch(level, x, j + y, z, height - j, dirx, dirz);
            }
        }
        ChaosPersists.setBlockFast(level, x, y + height, z, Blocks.OAK_LEAVES, 0, 2);
    }
}
