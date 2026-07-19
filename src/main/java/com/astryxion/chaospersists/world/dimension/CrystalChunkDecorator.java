package com.astryxion.chaospersists.world.dimension;

import com.astryxion.chaospersists.block.BlockCrystalPlant;
import com.astryxion.chaospersists.block.CrystalMaze;
import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Per-chunk crystal plains decoration ported from 1.12 {@code ChunkProviderChaos5#generateChunk}.
 */
public final class CrystalChunkDecorator {
    private CrystalChunkDecorator() {}

    public static void decorate(Level level, RandomSource random, int chunkX, int chunkZ, LevelChunk chunk) {
        if (level.isClientSide()) {
            return;
        }
        // Undo bad flood passes from earlier builds so trees/flowers can place on grass again.
        clearFloodWater(level, chunkX, chunkZ);
        restoreCrystalSurface(level, chunkX, chunkZ);
        new CrystalMaze()
                .buildCrystalMaze(level, chunkX, 25, chunkZ, chunk);
        addPinkTourmaline(level, random, chunkX, chunkZ);
        addTigersEye(level, random, chunkX, chunkZ);
        addCrystalTrees(level, random, chunkX, chunkZ);
        generateCrystalOres(level, random, chunkX, chunkZ, chunk);
        addCrystalFlowers(level, random, chunkX, chunkZ);
        addRice(level, random, chunkX, chunkZ);
        addQuinoa(level, random, chunkX, chunkZ);
    }

    private static void addPinkTourmaline(Level level, RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(30) != 1 || ChaosPersists.CrystalCrystal == null) {
            return;
        }
        int randPosX = 3 + chunkX + random.nextInt(10);
        int randPosY = 30 + random.nextInt(5);
        int randPosZ = 3 + chunkZ + random.nextInt(10);
        int patchy = 1 + random.nextInt(10);
        for (int i = 0; i < patchy; ++i) {
            float dx = random.nextFloat() - random.nextFloat();
            float dz = random.nextFloat() - random.nextFloat();
            float dy = 0.5f + random.nextFloat() / 2.0f;
            int width = random.nextInt(2);
            int length = 1 + width * 3 + random.nextInt(15);
            float rx = randPosX;
            float ry = randPosY;
            float rz = randPosZ;
            for (int iy = 0; iy <= length; ++iy) {
                for (int ix = 0; ix <= width; ++ix) {
                    for (int iz = 0; iz <= width; ++iz) {
                        ChaosPersists.setBlockFast(
                                level,
                                (int) (rx + ix),
                                (int) ry,
                                (int) (rz + iz),
                                ChaosPersists.CrystalCrystal,
                                0,
                                2);
                    }
                }
                ry += dy;
                rx += dx;
                rz += dz;
            }
        }
    }

    private static void addTigersEye(Level level, RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(30) != 1 || ChaosPersists.TigersEye == null) {
            return;
        }
        int randPosX = 3 + chunkX + random.nextInt(10);
        int randPosY = 5 + random.nextInt(5);
        int randPosZ = 3 + chunkZ + random.nextInt(10);
        int patchy = 1 + random.nextInt(5);
        for (int i = 0; i < patchy; ++i) {
            float dx = random.nextFloat() - random.nextFloat();
            float dz = random.nextFloat() - random.nextFloat();
            float dy = 0.5f + random.nextFloat() / 2.0f;
            int length = random.nextInt(6);
            float rx = randPosX;
            float ry = randPosY;
            float rz = randPosZ;
            for (int iy = 0; iy <= length; ++iy) {
                ChaosPersists.setBlockFast(
                        level, (int) rx, (int) ry, (int) rz, ChaosPersists.TigersEye, 0, 2);
                ry += dy;
                rx += dx;
                rz += dz;
            }
        }
    }

    private static void addCrystalTrees(Level level, RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.MyCrystalPlant == null || ChaosPersists.CrystalGrass == null) {
            return;
        }
        if (random.nextInt(5) != 0) {
            return;
        }
        BlockCrystalPlant crystalPlant = (BlockCrystalPlant) ChaosPersists.MyCrystalPlant;
        int what = random.nextInt(5);
        int howmany = 1 + random.nextInt(7);
        if (what != 0) {
            howmany *= 2;
        }
        for (int i = 0; i < howmany; ++i) {
            int posX = 4 + chunkX + random.nextInt(8);
            int posZ = 4 + chunkZ + random.nextInt(8);
            int posY = findCrystalPlantingY(level, posX, posZ);
            if (posY < 0) {
                continue;
            }
            if (what == 0) {
                crystalPlant.TallCrystalTree(level, posX, posY, posZ);
            } else {
                crystalPlant.ScragglyCrystalTreeWithBranches(level, posX, posY, posZ);
            }
        }
    }

    /** Strip source water placed by earlier flood logic; crystal decoration needs dry grass. */
    private static void clearFloodWater(Level level, int chunkX, int chunkZ) {
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); ++y) {
                    BlockPos pos = new BlockPos(chunkX + dx, y, chunkZ + dz);
                    if (level.getFluidState(pos).isSource()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void restoreCrystalSurface(Level level, int chunkX, int chunkZ) {
        Block crystalGrass = ChaosPersists.CrystalGrass;
        Block crystalStone = ChaosPersists.CrystalStone;
        if (crystalGrass == null || crystalStone == null) {
            return;
        }
        BlockState grass = crystalGrass.defaultBlockState();
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                BlockPos top = new BlockPos(x, topY, z);
                BlockState topState = level.getBlockState(top);
                if (topState.is(crystalStone)) {
                    level.setBlock(top, grass, 2);
                    continue;
                }
                if (topState.isAir() && level.getBlockState(top.below()).is(crystalStone)) {
                    level.setBlock(top, grass, 2);
                }
            }
        }
    }

    private static int findCrystalPlantingY(Level level, int x, int z) {
        if (ChaosPersists.CrystalGrass == null) {
            return -1;
        }
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int y = topY + 1; y >= level.getMinBuildHeight() + 8; --y) {
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos ground = feet.below();
            if (level.getBlockState(feet).isAir()
                    && level.getBlockState(ground).is(ChaosPersists.CrystalGrass)
                    && level.getFluidState(ground).isEmpty()) {
                return y;
            }
        }
        return -1;
    }

    private static void generateCrystalOres(
            Level level, RandomSource random, int chunkX, int chunkZ, LevelChunk chunk) {
        Block crystalStone = ChaosPersists.CrystalStone;
        if (crystalStone == null) {
            return;
        }
        int patchy = 25 + random.nextInt(30);
        if (random.nextInt(20) == 0) {
            patchy += 30;
        }
        for (int i = 0; i < patchy; ++i) {
            int randPosX = 2 + chunkX + random.nextInt(12);
            int randPosY = random.nextInt(128);
            int randPosZ = 2 + chunkZ + random.nextInt(12);
            if (randPosY <= 45) {
                continue;
            }
            Block ore = switch (random.nextInt(11)) {
                case 0 -> ChaosPersists.MyUrchinSpawnBlock;
                case 1 -> ChaosPersists.MyFlounderSpawnBlock;
                case 2 -> ChaosPersists.MySkateSpawnBlock;
                case 3 -> ChaosPersists.MyRotatorSpawnBlock;
                case 4 -> ChaosPersists.MyPeacockSpawnBlock;
                case 5 -> ChaosPersists.MyFairySpawnBlock;
                case 6 -> ChaosPersists.MyDungeonBeastSpawnBlock;
                case 7 -> ChaosPersists.MyVortexSpawnBlock;
                case 8 -> ChaosPersists.MyRatSpawnBlock;
                case 9 -> ChaosPersists.MyWhaleSpawnBlock;
                default -> ChaosPersists.MyIrukandjiSpawnBlock;
            };
            if (ore != null) {
                ChaosPersists.Chunker.generateBlockOre(
                        level, random, randPosX, randPosY, randPosZ, chunk, ore, 4, crystalStone);
            }
        }
        patchy = 3 + random.nextInt(8);
        for (int i = 0; i < patchy; ++i) {
            int randPosX = 2 + chunkX + random.nextInt(12);
            int randPosY = random.nextInt(128);
            int randPosZ = 2 + chunkZ + random.nextInt(12);
            if (ChaosPersists.CrystalCoal != null) {
                ChaosPersists.Chunker.generateBlockOre(
                        level,
                        random,
                        randPosX,
                        randPosY,
                        randPosZ,
                        chunk,
                        ChaosPersists.CrystalCoal,
                        6,
                        crystalStone);
            }
        }
        patchy = 15 + random.nextInt(20);
        for (int i = 0; i < patchy; ++i) {
            int randPosX = 2 + chunkX + random.nextInt(12);
            int randPosY = random.nextInt(24);
            int randPosZ = 2 + chunkZ + random.nextInt(12);
            if (ChaosPersists.CrystalRat != null) {
                ChaosPersists.Chunker.generateBlockOre(
                        level,
                        random,
                        randPosX,
                        randPosY,
                        randPosZ,
                        chunk,
                        ChaosPersists.CrystalRat,
                        6,
                        crystalStone);
            }
        }
        patchy = 12 + random.nextInt(20);
        for (int i = 0; i < patchy; ++i) {
            int randPosX = 2 + chunkX + random.nextInt(12);
            int randPosY = random.nextInt(24);
            int randPosZ = 2 + chunkZ + random.nextInt(12);
            if (ChaosPersists.CrystalFairy != null) {
                ChaosPersists.Chunker.generateBlockOre(
                        level,
                        random,
                        randPosX,
                        randPosY,
                        randPosZ,
                        chunk,
                        ChaosPersists.CrystalFairy,
                        6,
                        crystalStone);
            }
        }
    }

    private static void addCrystalFlowers(Level level, RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(3) != 0) {
            return;
        }
        int howmany = 1 + random.nextInt(13);
        int what = random.nextInt(4);
        for (int i = 0; i < howmany; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            int posY = findCrystalPlantingY(level, posX, posZ);
            if (posY < 0) {
                continue;
            }
            Block flower =
                    switch (what) {
                        case 0 -> ChaosPersists.CrystalFlowerRedBlock;
                        case 1 -> ChaosPersists.CrystalFlowerGreenBlock;
                        case 2 -> ChaosPersists.CrystalFlowerBlueBlock;
                        default -> ChaosPersists.CrystalFlowerYellowBlock;
                    };
            if (flower != null) {
                ChaosPersists.setBlockFast(level, posX, posY, posZ, flower, 0, 2);
            }
        }
    }

    private static void addRice(Level level, RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(10) != 0 || ChaosPersists.MyRicePlant == null) {
            return;
        }
        for (int i = 0; i < 5; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            int posY = findCrystalPlantingY(level, posX, posZ);
            if (posY < 0) {
                continue;
            }
            ChaosPersists.setBlockFast(level, posX, posY, posZ, ChaosPersists.MyRicePlant, 0, 2);
        }
    }

    private static void addQuinoa(Level level, RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(20) != 0 || ChaosPersists.MyQuinoaPlant1 == null) {
            return;
        }
        for (int i = 0; i < 5; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            int posY = findCrystalPlantingY(level, posX, posZ);
            if (posY < 0) {
                continue;
            }
            ChaosPersists.setBlockFast(level, posX, posY, posZ, ChaosPersists.MyQuinoaPlant1, 0, 2);
        }
    }
}
