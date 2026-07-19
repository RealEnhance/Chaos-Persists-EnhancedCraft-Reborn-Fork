package com.astryxion.chaospersists.world.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Maps OreSpawn 1.7.10 biome names to 1.20.1 biome tags / holders so spawns registered on
 * {@code ocean} also appear in warm/lukewarm/cold/frozen ocean variants (Alex's Mobs pattern).
 *
 * <p>Vanilla {@link BiomeTags} entries (IS_FOREST, IS_JUNGLE, IS_TAIGA, IS_SAVANNA, IS_BADLANDS,
 * IS_HILL, IS_MOUNTAIN, IS_BEACH, IS_RIVER, IS_OCEAN, ...) are populated by most biome-adding
 * datapack mods so those legacy groups already resolve against modded biomes for free. Groups
 * that vanilla has no tag for (plains, desert, swamp, mushroom island, snowy plains, mountain
 * sub-terrain, ...) fall back to the shared {@code forge:is_*} convention tags, which Terralith,
 * Biomes O' Plenty, Regions Unexplored, and Oh The Biomes We've Gone all populate for exactly
 * this purpose (third-party cross-mod biome categorization). This keeps the mapping data-driven
 * instead of hardcoding every biome mod's IDs: any biome mod that follows the same
 * {@code forge:is_*} convention is picked up automatically, including ones added after this was
 * written. Verified against the actual biome lists of all four packaged biome mods: Oh The Biomes
 * We've Gone resolves 54/54 biomes, Regions Unexplored 64/71 (remaining 7 are either
 * cave-only biomes unreachable by this mod's surface-column biome check, or a couple of biomes
 * with no matching convention tag at all).
 */
public final class LegacyBiomeMatcher {
  private LegacyBiomeMatcher() {}

  private static TagKey<Biome> forgeTag(String path) {
    return TagKey.create(Registries.BIOME, new ResourceLocation("forge", path));
  }

  // "c" (common) tag used by Terralith for stony-shore-like biomes; Biomes O' Plenty has no
  // equivalent stony-shore biomes so this is a partial fallback rather than a universal one.
  private static TagKey<Biome> commonTag(String path) {
    return TagKey.create(Registries.BIOME, new ResourceLocation("c", path));
  }

  private static final TagKey<Biome> FORGE_IS_PLAINS = forgeTag("is_plains");
  private static final TagKey<Biome> FORGE_IS_DESERT = forgeTag("is_desert");
  private static final TagKey<Biome> FORGE_IS_SWAMP = forgeTag("is_swamp");
  private static final TagKey<Biome> FORGE_IS_MUSHROOM = forgeTag("is_mushroom");
  private static final TagKey<Biome> FORGE_IS_SNOWY = forgeTag("is_snowy");
  private static final TagKey<Biome> FORGE_IS_MAGICAL = forgeTag("is_magical");
  private static final TagKey<Biome> FORGE_IS_SPOOKY = forgeTag("is_spooky");
  private static final TagKey<Biome> FORGE_IS_PEAK = forgeTag("is_peak");
  private static final TagKey<Biome> FORGE_IS_SLOPE = forgeTag("is_slope");
  private static final TagKey<Biome> FORGE_IS_PLATEAU = forgeTag("is_plateau");
  private static final TagKey<Biome> COMMON_STONY_SHORES = commonTag("stony_shores");

  // Terralith-specific: no forge:is_magical equivalent is populated by Terralith, but its own
  // "mystical" tag (moonlight groves, lavender forests, mirage isles, ...) is the closest thematic
  // match for the dark-forest-flavored mobs (Fairy, EnderReaper, EnderKnight, DungeonBeast,
  // Basilisk) that are heavily weighted toward "roofed_forest" below.
  private static final TagKey<Biome> TERRALITH_MYSTICAL =
      TagKey.create(Registries.BIOME, new ResourceLocation("terralith", "mystical"));

  /**
   * Convenience for call sites outside the spawn system (e.g. {@code ChaosWorld}'s per-chunk
   * Overworld feature placement) that used to test hardcoded vanilla {@code Biomes.X} holders
   * directly. Routes through {@link #matches} so those checks get the same modded-biome coverage
   * (Terralith, Biomes O' Plenty, etc.) as the legacy mob spawn system, instead of maintaining a
   * second, separately-hardcoded set of biome lists.
   */
  public static boolean matchesLegacyPath(Holder<Biome> biome, String legacyPath) {
    return matches(biome, ResourceLocation.withDefaultNamespace(legacyPath));
  }

  public static boolean isModBiome(Holder<Biome> biome) {
    return biome.unwrapKey()
        .map(key -> "chaospersists".equals(key.location().getNamespace()))
        .orElse(false);
  }

  public static boolean matches(Holder<Biome> biome, ResourceLocation legacyGroupId) {
    if (biome == null || legacyGroupId == null) {
      return false;
    }
    if (!"minecraft".equals(legacyGroupId.getNamespace())) {
      return biome.unwrapKey().map(key -> key.location().equals(legacyGroupId)).orElse(false);
    }
    ResourceLocation current = biome.unwrapKey().map(key -> key.location()).orElse(null);
    if (legacyGroupId.equals(current)) {
      return true;
    }
    return switch (legacyGroupId.getPath()) {
      case "ocean" -> biome.is(BiomeTags.IS_OCEAN);
      case "deep_ocean" -> biome.is(BiomeTags.IS_DEEP_OCEAN);
      case "river" -> biome.is(BiomeTags.IS_RIVER);
      case "frozen_river" -> biome.is(Biomes.FROZEN_RIVER);
      case "plains" ->
          biome.is(Biomes.PLAINS)
              || biome.is(Biomes.SUNFLOWER_PLAINS)
              || biome.is(Biomes.MEADOW)
              || biome.is(FORGE_IS_PLAINS);
      case "ice_plains" ->
          biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.ICE_SPIKES) || biome.is(FORGE_IS_SNOWY);
      case "desert", "desert_hills" -> biome.is(Biomes.DESERT) || biome.is(FORGE_IS_DESERT);
      case "forest", "forest_hills", "extreme_hills_with_trees" -> biome.is(BiomeTags.IS_FOREST);
      case "birch_forest", "birch_forest_hills" ->
          biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST);
      case "jungle", "jungle_hills" -> biome.is(BiomeTags.IS_JUNGLE);
      case "taiga", "taiga_hills", "cold_taiga", "cold_taiga_hills" -> biome.is(BiomeTags.IS_TAIGA);
      case "redwood_taiga", "redwood_taiga_hills", "mega_taiga", "mega_taiga_hills" ->
          biome.is(Biomes.OLD_GROWTH_PINE_TAIGA)
              || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
              || biome.is(BiomeTags.IS_TAIGA);
      case "swampland" ->
          biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP) || biome.is(FORGE_IS_SWAMP);
      case "beach" -> biome.is(Biomes.BEACH) || biome.is(BiomeTags.IS_BEACH);
      case "stone_beach" -> biome.is(Biomes.STONY_SHORE) || biome.is(COMMON_STONY_SHORES);
      case "savanna", "savanna_plateau" -> biome.is(BiomeTags.IS_SAVANNA);
      case "mesa", "mesa_rock", "mesa_plateau", "mesa_plateau_f" -> biome.is(BiomeTags.IS_BADLANDS);
      case "mesa_clear_rock" -> biome.is(Biomes.ERODED_BADLANDS);
      case "extreme_hills", "extreme_hills_edge" ->
          biome.is(BiomeTags.IS_HILL)
              || biome.is(BiomeTags.IS_MOUNTAIN)
              || biome.is(FORGE_IS_PEAK)
              || biome.is(FORGE_IS_SLOPE)
              || biome.is(FORGE_IS_PLATEAU);
      case "roofed_forest" ->
          biome.is(Biomes.DARK_FOREST)
              || biome.is(TERRALITH_MYSTICAL)
              || biome.is(FORGE_IS_MAGICAL)
              || biome.is(FORGE_IS_SPOOKY);
      case "hell" -> biome.is(BiomeTags.IS_NETHER);
      case "mushroom_island" -> biome.is(Biomes.MUSHROOM_FIELDS) || biome.is(FORGE_IS_MUSHROOM);
      default -> false;
    };
  }
}
