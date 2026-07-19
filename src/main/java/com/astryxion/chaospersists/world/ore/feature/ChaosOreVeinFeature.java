package com.astryxion.chaospersists.world.ore.feature;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.util.OreStats;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Vanilla-{@link Feature} replacement for the legacy {@code ChunkOreGenerator.generateBlockOre} ellipsoid
 * vein scan. Ported line-for-line for the placement math (same vein shape/size behavior players are used
 * to), but:
 * <ul>
 *   <li>uses {@link WorldGenLevel#getBlockState}/{@link WorldGenLevel#setBlock} instead of the legacy
 *       chunk-array helpers, so it works with any {@link WorldGenLevel}, not just a raw {@code LevelChunk};</li>
 *   <li>clamps to the level's actual {@code getMinBuildHeight()}/{@code getMaxBuildHeight()} instead of the
 *       old hardcoded 0-127 (1.7.10 world height) range, so veins can reach modern deepslate depth;</li>
 *   <li>matches replaceable blocks by tag ({@link OreVeinConfiguration#targets()}) instead of a single
 *       exact block, so a vein that reaches deepslate depth (or passes through andesite/diorite/granite/tuff)
 *       actually places ore there instead of silently only ever replacing literal {@code minecraft:stone};</li>
 *   <li>reads its rate/clump-size/depth from live config each time it runs (via
 *       {@link ChaosPersists#getOreStats(String)}), so this still honors config edits and the "LessOre"
 *       toggle exactly like the code it replaces.</li>
 * </ul>
 * mindepth/maxdepth in the config were written for the old 0-127 range, so they're treated as "height
 * above the bottom of the world" and offset by {@code getMinBuildHeight()} here, rather than as literal
 * Y-levels, to keep their old meaning on a taller/deeper world.
 */
public class ChaosOreVeinFeature extends Feature<OreVeinConfiguration> {
    public ChaosOreVeinFeature(Codec<OreVeinConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreVeinConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OreVeinConfiguration config = context.config();

        OreStats stats = ChaosPersists.getOreStats(config.statsKey());
        if (stats == null || stats.rate <= 0) {
            return false;
        }

        int worldMinY = level.getMinBuildHeight();
        int worldMaxY = level.getMaxBuildHeight() - 1;
        int mindepth = Mth.clamp(worldMinY + stats.mindepth, worldMinY, worldMaxY);
        int maxdepth = Mth.clamp(worldMinY + stats.maxdepth, worldMinY, worldMaxY);
        if (mindepth > maxdepth) {
            return false;
        }

        int baseX = context.origin().getX();
        int baseZ = context.origin().getZ();

        int patchy = stats.rate + random.nextInt(9);
        if (ChaosPersists.LessOre != 0) {
            patchy /= 3;
        }

        boolean placedAny = false;
        for (int i = 0; i < patchy; ++i) {
            int posX = baseX + random.nextInt(16);
            int posY = mindepth + random.nextInt(maxdepth - mindepth + 1);
            int posZ = baseZ + random.nextInt(16);
            if (generateVein(level, random, posX, posY, posZ, config, stats.clumpsize)) {
                placedAny = true;
            }
        }
        return placedAny;
    }

    /** Same ellipsoid-vein math as the legacy {@code generateBlockOre}, targeting a {@link WorldGenLevel}. */
    private boolean generateVein(
            WorldGenLevel level,
            RandomSource random,
            int originX,
            int originY,
            int originZ,
            OreVeinConfiguration config,
            int numberOfBlocks) {
        float f = random.nextFloat() * 3.1415927f;
        double d0 = (originX + 8) + Mth.sin(f) * numberOfBlocks / 8.0f;
        double d1 = (originX + 8) - Mth.sin(f) * numberOfBlocks / 8.0f;
        double d2 = (originZ + 8) + Mth.cos(f) * numberOfBlocks / 8.0f;
        double d3 = (originZ + 8) - Mth.cos(f) * numberOfBlocks / 8.0f;
        double d4 = originY + random.nextInt(3) - 2;
        double d5 = originY + random.nextInt(3) - 2;

        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int worldMinY = level.getMinBuildHeight();
        int worldMaxY = level.getMaxBuildHeight() - 1;

        for (int l = 0; l <= numberOfBlocks; ++l) {
            double d6 = d0 + (d1 - d0) * l / (double) numberOfBlocks;
            double d7 = d4 + (d5 - d4) * l / (double) numberOfBlocks;
            double d8 = d2 + (d3 - d2) * l / (double) numberOfBlocks;
            double d9 = random.nextDouble() * numberOfBlocks / 16.0;
            double d10 = (Mth.sin(l * 3.1415927f / numberOfBlocks) + 1.0f) * d9 + 1.0;
            double d11 = d10;

            int i1 = Mth.floor(d6 - d10 / 2.0);
            int j1 = Mth.floor(d7 - d11 / 2.0);
            int k1 = Mth.floor(d8 - d10 / 2.0);
            int l1 = Mth.floor(d6 + d10 / 2.0);
            int i2 = Mth.floor(d7 + d11 / 2.0);
            int j2 = Mth.floor(d8 + d10 / 2.0);

            for (int x = i1; x <= l1; ++x) {
                double dx = (x + 0.5 - d6) / (d10 / 2.0);
                if (dx * dx >= 1.0) {
                    continue;
                }
                for (int y = j1; y <= i2; ++y) {
                    if (y < worldMinY || y > worldMaxY) {
                        continue;
                    }
                    double dy = (y + 0.5 - d7) / (d11 / 2.0);
                    if (dx * dx + dy * dy >= 1.0) {
                        continue;
                    }
                    for (int z = k1; z <= j2; ++z) {
                        double dz = (z + 0.5 - d8) / (d10 / 2.0);
                        if (dx * dx + dy * dy + dz * dz >= 1.0) {
                            continue;
                        }
                        pos.set(x, y, z);
                        net.minecraft.world.level.block.state.BlockState candidate = level.getBlockState(pos);
                        if (config.matches(candidate)) {
                            level.setBlock(pos, config.stateFor(candidate), 2);
                            placedAny = true;
                        }
                    }
                }
            }
        }
        return placedAny;
    }
}
