package com.astryxion.chaospersists.world.ore.feature;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.util.OreStats;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Vanilla-{@link Feature} replacement for the legacy {@code ChaosWorld.generateRuby}/{@code generateOresInChunk}
 * "ore directly under a lava pocket" search: for each attempt, pick a random column, then scan straight down
 * from a random starting height looking for a lava block with a replaceable floor beneath it, and place ore
 * there. Unlike the plain ellipsoid vein handled by {@link ChaosOreVeinFeature}, this doesn't carve a blob of
 * ore through stone — it only ever places a single ore block, directly under the first lava pocket found.
 *
 * <p>Reuses {@link OreVeinConfiguration} for its data (the block to place, which tags count as a valid floor,
 * and which live {@code OreStats} entry governs rate/depth) since the shape is identical to what that record
 * already provides; {@code clumpsize} isn't used here since this feature places single blocks, not veins.
 *
 * <p>As with {@link ChaosOreVeinFeature}, this clamps to the level's actual min/max build height instead of
 * the legacy hardcoded 0-127 range, so it still works on worlds with modern deepslate depth, and it matches
 * the floor via block tags instead of a single exact {@code minecraft:stone} check so it keeps working once a
 * search column reaches deepslate, andesite, diorite, granite, or tuff instead of silently placing nothing.
 */
public class ChaosLavaPocketOreFeature extends Feature<OreVeinConfiguration> {
    public ChaosLavaPocketOreFeature(Codec<OreVeinConfiguration> codec) {
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

        int patchy = stats.rate + random.nextInt(7);
        if (ChaosPersists.LessOre != 0) {
            patchy /= 3;
        }

        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        for (int i = 0; i < patchy; ++i) {
            int posX = baseX + random.nextInt(16);
            int posZ = baseZ + random.nextInt(16);
            int startY = mindepth + random.nextInt(maxdepth - mindepth + 1);

            for (int y = startY; y > worldMinY; --y) {
                pos.set(posX, y, posZ);
                below.set(posX, y - 1, posZ);
                net.minecraft.world.level.block.state.BlockState floor = level.getBlockState(below);
                if (!level.getBlockState(pos).is(Blocks.LAVA) || !config.matches(floor)) {
                    continue;
                }
                level.setBlock(below, config.stateFor(floor), 2);
                placedAny = true;
                break;
            }
        }
        return placedAny;
    }
}
