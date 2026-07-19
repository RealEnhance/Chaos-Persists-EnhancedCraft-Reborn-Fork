package com.astryxion.chaospersists.world.biome;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Islands / Danger dimension biome (1.12 {@code BiomeDangerPlains} / {@code BiomeGenUtopianPlains#setIslandCreatures}).
 * Live registration uses Forge {@code forge:add_spawns} biome modifiers under {@code forge/biome_modifier/};
 * biome JSON spawner lists are empty. Weights below mirror 1.12; JSON files use the same scaling as utopia/mining.
 */
public final class BiomeDangerPlains {
    private BiomeDangerPlains() {}

    public static MobSpawnSettings buildDangerSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        addDangerSpawns(spawns);
        return spawns.build();
    }

    public static void addDangerSpawns(MobSpawnSettings.Builder spawns) {
        if (ChaosPersists.ButterflyEnable != 0) {
            spawns.addSpawn(
                    MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_BUTTERFLY.get(), 5, 2, 6));
        }
        if (ChaosPersists.CockateilEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_BIRD.get(), 4, 1, 2));
        }
        if (ChaosPersists.MothEnable != 0) {
            spawns.addSpawn(
                    MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_MOTH.get(), 5, 2, 4));
        }
        if (ChaosPersists.FireflyEnable != 0) {
            spawns.addSpawn(
                    MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_FIREFLY.get(), 10, 4, 8));
        }
        if (ChaosPersists.DragonEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_DRAGON.get(), 1, 1, 2));
        }
        if (ChaosPersists.StinkyEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_STINKY.get(), 2, 1, 2));
        }
        if (ChaosPersists.CliffRacerEnable != 0) {
            spawns.addSpawn(
                    MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_CLIFF_RACER.get(), 20, 3, 6));
        }
        if (ChaosPersists.CloudSharkEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_CLOUD_SHARK.get(), 1, 1, 1));
        }
        if (ChaosPersists.GoldFishEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_GOLD_FISH.get(), 5, 2, 4));
        }
        if (ChaosPersists.CreepingHorrorEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_CREEPING_HORROR.get(), 25, 2, 4));
        }
        if (ChaosPersists.TerribleTerrorEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_TERRIBLE_TERROR.get(), 25, 3, 6));
        }
        if (ChaosPersists.LurkingTerrorEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_LURKING_TERROR.get(), 1, 1, 1));
        }
        if (ChaosPersists.PitchBlackEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_NIGHTMARE.get(), 15, 3, 6));
        }
        if (ChaosPersists.LeafMonsterEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_LEAF_MONSTER.get(), 35, 2, 4));
        }
        if (ChaosPersists.EnderReaperEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_ENDER_REAPER.get(), 25, 2, 4));
        }
        if (ChaosPersists.HerculesBeetleEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_HERCULES_BEETLE.get(), 5, 1, 2));
        }
    }
}
