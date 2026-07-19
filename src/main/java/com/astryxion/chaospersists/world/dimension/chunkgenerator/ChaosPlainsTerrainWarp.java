package com.astryxion.chaospersists.world.dimension.chunkgenerator;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;

/**
 * Fixed-biome rolling-hill warp modeled on Twilight Forest {@code TFTerrainWarp} and 1.12 Utopia
 * ({@code baseHeight 0.125}, {@code heightVariation 0.05}).
 */
public class ChaosPlainsTerrainWarp {

    private final int cellWidth;
    private final int cellHeight;
    private final int cellCountY;
    private final NoiseSettings noiseSettings;
    private final ChaosPlainsNoiseSlider topSlide;
    private final ChaosPlainsNoiseSlider bottomSlide;
    private final double depth;
    private final double scale;
    private final double dimensionDensityFactor;
    private final double dimensionDensityOffset;
    /** Lifts TF-style low terrain (around Y 5) up to overworld sea-level plains (around Y 60). */
    private final int terrainHeightOffset;
    /** Multiplier on blended horizontal noise (crystal uses higher values for 1.12 heightVariation 0.5). */
    private final double noiseStrength;

    private ChaosPlainsBlendedNoise blendedNoise;

    public ChaosPlainsTerrainWarp(
            int width,
            int height,
            int yCount,
            ChaosPlainsNoiseSlider topSlide,
            ChaosPlainsNoiseSlider bottomSlide,
            NoiseSettings settings,
            double depth,
            double scale,
            double dimensionDensityFactor,
            double dimensionDensityOffset,
            int terrainHeightOffset,
            double noiseStrength) {
        this.cellWidth = width;
        this.cellHeight = height;
        this.cellCountY = yCount;
        this.noiseSettings = settings;
        this.topSlide = topSlide;
        this.bottomSlide = bottomSlide;
        this.depth = depth;
        this.scale = scale;
        this.dimensionDensityFactor = dimensionDensityFactor;
        this.dimensionDensityOffset = dimensionDensityOffset;
        this.terrainHeightOffset = terrainHeightOffset;
        this.noiseStrength = noiseStrength;
    }

    public void fillNoiseColumn(RandomState random, double[] column, int x, int z, int min, int max) {
        ChaosPlainsBlendedNoise blend = this.getBlendedNoise(random);
        double modifiedDepth = this.depth * 0.5D - 0.125D;
        double modifiedScale = this.scale * 0.9D + 0.1D;
        double offset = modifiedDepth * 0.265625D;
        double factor = 96.0D / modifiedScale;

        double scaleXZ = 684.412D * ChaosPlainsBlendedNoise.XZ_SCALE;
        double scaleY = 684.412D * ChaosPlainsBlendedNoise.Y_SCALE;
        double factorXZ = scaleXZ / ChaosPlainsBlendedNoise.XZ_FACTOR;
        double factorY = scaleY / ChaosPlainsBlendedNoise.Y_FACTOR;
        double density = -0.46875D;

        for (int index = 0; index <= max; ++index) {
            int y = index + min;
            double noise = blend.sampleAndClampNoise(x, y, z, scaleXZ, scaleY, factorXZ, factorY);
            double totalDensity =
                    this.computeInitialDensity(y, offset, factor, density) + noise * this.noiseStrength;
            totalDensity = this.applySlide(totalDensity, y);
            column[index] = totalDensity;
        }
    }

    private ChaosPlainsBlendedNoise getBlendedNoise(RandomState random) {
        if (this.blendedNoise == null) {
            WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
            this.blendedNoise = new ChaosPlainsBlendedNoise(worldgenRandom);
        }
        return this.blendedNoise;
    }

    private double computeInitialDensity(int y, double offset, double factor, double density) {
        double adjustedY = (double) y - (double) this.terrainHeightOffset / (double) this.cellHeight;
        double base = 1.0D - adjustedY * 2.0D / 32.0D + density;
        double factored = base * this.dimensionDensityFactor + this.dimensionDensityOffset;
        double total = (factored + offset) * factor;
        return total * (total > 0.0D ? 4.0D : 1.0D);
    }

    private double applySlide(double density, int height) {
        int minCell = Math.floorDiv(this.noiseSettings.minY(), this.cellHeight);
        int relativeHeight = height - minCell;
        density = this.topSlide.applySlide(density, this.cellCountY - relativeHeight);
        density = this.bottomSlide.applySlide(density, relativeHeight);
        return density;
    }
}
