package com.astryxion.chaospersists.world.dimension.chunkgenerator;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import java.util.stream.IntStream;

/**
 * Classic 1.12-style blended noise used for Twilight Forest rolling-hill terrain.
 */
public final class ChaosPlainsBlendedNoise {

    public static final double XZ_SCALE = 0.9999999814507745D;
    public static final double Y_SCALE = 0.9999999814507745D;
    public static final double XZ_FACTOR = 80.0D;
    public static final double Y_FACTOR = 160.0D;

    private final PerlinNoise mainNoise;
    private final PerlinNoise minLimitNoise;
    private final PerlinNoise maxLimitNoise;

    public ChaosPlainsBlendedNoise(RandomSource random) {
        this.mainNoise = PerlinNoise.create(random, IntStream.range(0, 8));
        this.minLimitNoise = PerlinNoise.create(random, IntStream.range(0, 16));
        this.maxLimitNoise = PerlinNoise.create(random, IntStream.range(0, 16));
    }

    public double sampleAndClampNoise(int x, int y, int z, double scaleXZ, double scaleY, double factorXZ, double factorY) {
        double blended = 0.0D;
        double minSample = 0.0D;
        double maxSample = 0.0D;
        double scale = 1.0D;

        for (int oct = 0; oct < 8; ++oct) {
            ImprovedNoise mainOct = this.mainNoise.getOctaveNoise(oct);
            if (mainOct != null) {
                blended +=
                        mainOct.noise(
                                        PerlinNoise.wrap((double) x * factorXZ * scale),
                                        PerlinNoise.wrap((double) y * factorY * scale),
                                        PerlinNoise.wrap((double) z * factorXZ * scale),
                                        factorY * scale,
                                        (double) y * factorY * scale)
                                / scale;
            }
            scale /= 2.0D;
        }

        double blend = (blended / 10.0D + 1.0D) / 2.0D;
        boolean useMax = blend >= 1.0D;
        boolean useMin = blend <= 0.0D;
        scale = 1.0D;

        for (int oct = 0; oct < 16; ++oct) {
            double sampleX = PerlinNoise.wrap((double) x * scaleXZ * scale);
            double sampleY = PerlinNoise.wrap((double) y * scaleY * scale);
            double sampleZ = PerlinNoise.wrap((double) z * scaleXZ * scale);
            double verticalScale = scaleY * scale;

            if (!useMax) {
                ImprovedNoise minOct = this.minLimitNoise.getOctaveNoise(oct);
                if (minOct != null) {
                    minSample += minOct.noise(sampleX, sampleY, sampleZ, verticalScale, (double) y * verticalScale) / scale;
                }
            }

            if (!useMin) {
                ImprovedNoise maxOct = this.maxLimitNoise.getOctaveNoise(oct);
                if (maxOct != null) {
                    maxSample += maxOct.noise(sampleX, sampleY, sampleZ, verticalScale, (double) y * verticalScale) / scale;
                }
            }

            scale /= 2.0D;
        }

        return Mth.clampedLerp(minSample / 512.0D, maxSample / 512.0D, blend);
    }
}
