package com.astryxion.chaospersists.entity;

import com.astryxion.chaospersists.compat.forge.fml.common.registry.EntityRegistry;
import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * OreSpawn 1.7.10 relied on implicit spawn rules; 1.20.1 requires explicit spawn placement
 * registration. Called once at the end of {@code preInit} after {@link EntityRegistry#addSpawn}
 * records legacy categories.
 *
 * <p>Most OreSpawn {@code waterCreature} mobs are not vanilla {@code WaterAnimal} — they spawn on
 * beaches and river banks (Y {@code >=} 50, daytime) without requiring a water block at the spawn
 * position. Only truly submerged mobs (whale, sea monster) use {@link SpawnPlacements.Type#IN_WATER}.
 */
public final class ChaosSpawnPlacements {
  private ChaosSpawnPlacements() {}

  public static void registerAllAfterLegacySpawns() {
    for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
      if (!ChaosPersists.MODID.equals(id.getNamespace())) {
        continue;
      }
      EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
      if (type == null || type.getCategory() == MobCategory.MISC) {
        continue;
      }
      registerEntity(type);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void registerEntity(EntityType<?> type) {
    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
    if (id == null) {
      return;
    }
    switch (id.getPath()) {
      case "large_worm" ->
          SpawnPlacements.register(
              (EntityType<WormLarge>) type,
              SpawnPlacements.Type.ON_GROUND,
              Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
              ChaosSpawnPlacements::checkLargeWormSpawnRules);
      case "frog" ->
          SpawnPlacements.register(
              (EntityType<Frog>) type,
              SpawnPlacements.Type.NO_RESTRICTIONS,
              Heightmap.Types.MOTION_BLOCKING,
              Frog::checkFrogSpawnRules);
      case "attack_squid" ->
          SpawnPlacements.register(
              (EntityType<AttackSquid>) type,
              SpawnPlacements.Type.NO_RESTRICTIONS,
              Heightmap.Types.MOTION_BLOCKING,
              AttackSquid::checkAttackSquidSpawnRules);
      case "sea_viper" ->
          SpawnPlacements.register(
              (EntityType<SeaViper>) type,
              SpawnPlacements.Type.NO_RESTRICTIONS,
              Heightmap.Types.MOTION_BLOCKING,
              SeaViper::checkSeaViperSpawnRules);
      case "whale", "sea_monster" ->
          SpawnPlacements.register(
              (EntityType<? extends Mob>) type,
              SpawnPlacements.Type.IN_WATER,
              Heightmap.Types.MOTION_BLOCKING,
              ChaosSpawnPlacements::checkSubmergedWaterSpawnRules);
      default -> registerByLegacyCategory(type);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void registerByLegacyCategory(EntityType<?> type) {
    MobCategory category = EntityRegistry.getLegacyPlacementCategory(type);
    switch (category) {
      case MONSTER ->
          SpawnPlacements.register(
              (EntityType<? extends Monster>) type,
              SpawnPlacements.Type.ON_GROUND,
              Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
              ChaosSpawnPlacements::checkLandMonsterSpawnRules);
      case CREATURE ->
          SpawnPlacements.register(
              (EntityType<? extends Animal>) type,
              SpawnPlacements.Type.ON_GROUND,
              Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
              ChaosSpawnPlacements::checkLandAnimalSpawnRules);
      case AMBIENT ->
          SpawnPlacements.register(
              (EntityType<? extends Mob>) type,
              SpawnPlacements.Type.NO_RESTRICTIONS,
              Heightmap.Types.MOTION_BLOCKING,
              ChaosSpawnPlacements::checkLandAmbientSpawnRules);
      case WATER_CREATURE, WATER_AMBIENT ->
          SpawnPlacements.register(
              (EntityType<? extends Mob>) type,
              SpawnPlacements.Type.NO_RESTRICTIONS,
              Heightmap.Types.MOTION_BLOCKING,
              ChaosSpawnPlacements::checkLegacyWaterCreatureSpawnRules);
      default ->
          SpawnPlacements.register(
              (EntityType<? extends Mob>) type,
              SpawnPlacements.Type.ON_GROUND,
              Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
              ChaosSpawnPlacements::checkLandMobSpawnRules);
    }
  }

  private static boolean isSubmergedInWater(ServerLevelAccessor level, BlockPos pos) {
    if (level.getFluidState(pos).is(FluidTags.WATER)) {
      return true;
    }
    return level.getFluidState(pos.below()).is(FluidTags.WATER);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static boolean checkLargeWormSpawnRules(
      EntityType<? extends Mob> type,
      ServerLevelAccessor level,
      MobSpawnType spawnType,
      BlockPos pos,
      RandomSource random) {
    if (isSubmergedInWater(level, pos)) {
      return false;
    }
    return WormLarge.checkWormLargeSpawnRules((EntityType<WormLarge>) type, level, spawnType, pos, random);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static boolean checkLandAnimalSpawnRules(
      EntityType<? extends Animal> type,
      ServerLevelAccessor level,
      MobSpawnType spawnType,
      BlockPos pos,
      RandomSource random) {
    if (isSubmergedInWater(level, pos)) {
      return false;
    }
    return Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
  }

  private static boolean checkLandMobSpawnRules(
      EntityType<? extends Mob> type,
      ServerLevelAccessor level,
      MobSpawnType spawnType,
      BlockPos pos,
      RandomSource random) {
    if (isSubmergedInWater(level, pos)) {
      return false;
    }
    return Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
  }

  private static boolean checkLandAmbientSpawnRules(
      EntityType<? extends Mob> type,
      ServerLevelAccessor level,
      MobSpawnType spawnType,
      BlockPos pos,
      RandomSource random) {
    if (isSubmergedInWater(level, pos)) {
      return false;
    }
    return Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
  }

  private static boolean checkLandMonsterSpawnRules(
      EntityType<? extends Monster> type,
      ServerLevelAccessor level,
      MobSpawnType spawnType,
      BlockPos pos,
      RandomSource random) {
    if (isSubmergedInWater(level, pos)) {
      return false;
    }
    return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
  }

  /** OreSpawn 1.7.10 water creatures: Y >= 50; day/night handled in entity {@code checkSpawnRules}. */
  private static boolean checkLegacyWaterCreatureSpawnRules(
      EntityType<? extends Mob> type,
      ServerLevelAccessor level,
      MobSpawnType spawnType,
      BlockPos pos,
      RandomSource random) {
    return pos.getY() >= 50;
  }

  /** Whales / sea monsters spawn submerged in ocean water columns. */
  private static boolean checkSubmergedWaterSpawnRules(
      EntityType<? extends Mob> type,
      ServerLevelAccessor level,
      MobSpawnType spawnType,
      BlockPos pos,
      RandomSource random) {
    return pos.getY() >= 50 && level.getFluidState(pos).is(FluidTags.WATER);
  }
}
