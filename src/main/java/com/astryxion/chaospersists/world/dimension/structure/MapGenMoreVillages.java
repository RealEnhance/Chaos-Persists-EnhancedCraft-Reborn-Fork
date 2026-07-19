package com.astryxion.chaospersists.world.dimension.structure;

/**
 * 1.12 {@code MapGenMoreVillages} used {@link net.minecraft.world.gen.structure.MapGenVillage} with spacing 9
 * (vs vanilla 32) and salt 10387312. Village Mania dimension generation is datapack-driven in 1.20.1:
 * {@code data/chaospersists/worldgen/structure_set/village_mania_villages.json}.
 */
public final class MapGenMoreVillages {
    /** 1.12 {@code field_82665_g} — chunk grid spacing between village attempts. */
    public static final int SPACING = 9;
    /** 1.12 {@code field_82666_h} — minimum separation from grid (via {@code nextInt(spacing - separation)}). */
    public static final int SEPARATION = 2;
    /** 1.12 village structure seed salt ({@code 10387312}). */
    public static final int SALT = 10387312;

    private MapGenMoreVillages() {}
}
