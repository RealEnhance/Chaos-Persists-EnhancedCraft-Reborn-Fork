package com.astryxion.chaospersists.world.biome;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Village Mania plains biome (1.12 {@code BiomeVillagePlains} / {@code BiomeGenUtopianPlains#setVillageCreatures}).
 * Live registration uses Forge {@code forge:add_spawns} biome modifiers under {@code forge/biome_modifier/};
 * biome JSON spawner lists are empty (Craziness Awakened-style). This mirrors 1.12 weights for reference only.
 */
public final class BiomeVillagePlains {
    private BiomeVillagePlains() {}

    /** 1.12 cumulative utopia + village spawn tables (config gates preserved). */
    public static MobSpawnSettings buildVillageSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        BiomeUtopia.addUtopiaSpawns(spawns);
        addVillageOnlySpawns(spawns);
        return spawns.build();
    }

    private static void addVillageOnlySpawns(MobSpawnSettings.Builder spawns) {
        if (ChaosPersists.Robot1Enable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_ROBOT1.get(), 25, 4, 8));
        }
        if (ChaosPersists.Robot2Enable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_ROBOT2.get(), 16, 2, 8));
        }
        if (ChaosPersists.Robot3Enable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_ROBOT3.get(), 12, 2, 4));
        }
        if (ChaosPersists.Robot4Enable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_ROBOT4.get(), 8, 1, 2));
        }
        if (ChaosPersists.Robot5Enable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_ROBOT5.get(), 20, 4, 8));
        }
        if (ChaosPersists.JefferyEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_GIANT_ROBOT.get(), 8, 1, 2));
        }
        if (ChaosPersists.SpiderDriverEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_SPIDER_DRIVER.get(), 20, 3, 5));
        }
        if (ChaosPersists.GodzillaEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_MOBZILLA.get(), 2, 1, 1));
        }
        if (ChaosPersists.FireflyEnable != 0) {
            spawns.addSpawn(
                    MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_FIREFLY.get(), 10, 3, 6));
        }
        if (ChaosPersists.GirlfriendEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_GIRLFRIEND.get(), 1, 1, 1));
        }
        if (ChaosPersists.BoyfriendEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_BOYFRIEND.get(), 1, 1, 1));
        }
        if (ChaosPersists.CowEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_RED_COW.get(), 8, 4, 8));
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_GOLD_COW.get(), 3, 2, 6));
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_ENCHANTED_COW.get(), 2, 2, 4));
        }
        if (ChaosPersists.ButterflyEnable != 0) {
            spawns.addSpawn(
                    MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_BUTTERFLY.get(), 25, 3, 6));
        }
        if (ChaosPersists.MothEnable != 0) {
            spawns.addSpawn(
                    MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_MOTH.get(), 20, 1, 5));
        }
        if (ChaosPersists.ChipmunkEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_CHIPMUNK.get(), 5, 1, 2));
        }
        if (ChaosPersists.CockateilEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_BIRD.get(), 15, 2, 4));
        }
        if (ChaosPersists.TshirtEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_TSHIRT.get(), 2, 1, 1));
        }
        if (ChaosPersists.CoinEnable != 0) {
            spawns.addSpawn(
                    MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_COIN.get(), 2, 1, 1));
        }
        if (ChaosPersists.CriminalEnable != 0) {
            spawns.addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_CRIMINAL.get(), 15, 1, 2));
        }
    }
}
