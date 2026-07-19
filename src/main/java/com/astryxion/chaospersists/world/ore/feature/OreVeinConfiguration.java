package com.astryxion.chaospersists.world.ore.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Data for {@link ChaosOreVeinFeature}: which block to place, which block(s) it's allowed to replace,
 * and which live {@code OreStats} config entry (rate/clumpsize/mindepth/maxdepth) governs how much of it
 * gets placed. The stats themselves are looked up by key at generation time (see
 * {@code ChaosPersists#getOreStats(String)}) rather than baked into this configuration, so server-config
 * changes (e.g. a pack's "LessOre" toggle) still take effect without needing to regenerate this JSON.
 *
 * <p>{@code targets} is a list of block tags (e.g. {@code minecraft:stone_ore_replaceables} and
 * {@code minecraft:deepslate_ore_replaceables}) rather than a single exact block. Using a single exact
 * block (the original implementation) meant veins that geometrically reached deepslate depth, andesite,
 * diorite, granite, tuff, etc. would silently fail to place anywhere except literal {@code minecraft:stone},
 * since {@code BlockState#is(Block)} only matches that one block. Matching against the standard
 * replaceable-stone tags instead means the ore generates through every vanilla stone-tier and
 * deepslate-tier block, exactly like vanilla ores do.
 *
 * <p>{@code deepslateState}, when present, is placed instead of {@code state} whenever the block being
 * replaced is deepslate-tier (matches {@code minecraft:deepslate_ore_replaceables}), mirroring how vanilla
 * ore features place a {@code deepslate_*} variant once a vein reaches deepslate depth. It's optional so
 * existing configs without it keep placing {@code state} everywhere, same as before this field existed.
 */
public record OreVeinConfiguration(
        BlockState state, List<TagKey<Block>> targets, String statsKey, Optional<BlockState> deepslateState)
        implements FeatureConfiguration {
    public static final Codec<OreVeinConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BlockState.CODEC.fieldOf("state").forGetter(OreVeinConfiguration::state),
                    TagKey.codec(Registries.BLOCK)
                            .listOf()
                            .fieldOf("targets")
                            .forGetter(OreVeinConfiguration::targets),
                    Codec.STRING.fieldOf("stats_key").forGetter(OreVeinConfiguration::statsKey),
                    BlockState.CODEC
                            .optionalFieldOf("deepslate_state")
                            .forGetter(OreVeinConfiguration::deepslateState))
            .apply(instance, OreVeinConfiguration::new));

    /** True if {@code candidate} is replaceable by this vein, i.e. matches any tag in {@link #targets}. */
    public boolean matches(BlockState candidate) {
        for (TagKey<Block> tag : targets) {
            if (candidate.is(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The state to actually place for a given replaceable candidate: {@link #deepslateState} if it's
     * configured and {@code candidate} is deepslate-tier, otherwise the normal {@link #state}.
     */
    public BlockState stateFor(BlockState candidate) {
        if (deepslateState.isPresent() && candidate.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return deepslateState.get();
        }
        return state;
    }
}
