package com.astryxion.chaospersists.world.biome;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Utopia plains biome (1.12 {@code BiomeGenUtopianPlains} spawn table + no vanilla trees; trees from {@link com.astryxion.chaospersists.util.UtopiaBigTrees}).
 */
public final class BiomeUtopia {
    private BiomeUtopia() {}

    public static Biome create() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.7F)
                .downfall(0.5F)
                .specialEffects(
                        new BiomeSpecialEffects.Builder()
                                .waterColor(353825)
                                .waterFogColor(0x050533)
                                .fogColor(0xC0D8FF)
                                .skyColor(0x78A7FF)
                                .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

    /** 1.12 {@code BiomeGenUtopianPlains} constructor spawn entries (applied after entity types register). */
    public static MobSpawnSettings buildUtopiaSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        addUtopiaSpawns(spawns);
        return spawns.build();
    }

    public static void addUtopiaSpawns(MobSpawnSettings.Builder spawns) {
        if (ChaosPersists.GazelleEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_GAZELLE.get(), 10, 2, 4));
        }
        if (ChaosPersists.FireflyEnable != 0) {
            spawns.addSpawn(MobCategory.AMBIENT, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_FIREFLY.get(), 15, 3, 6));
        }
        if (ChaosPersists.GirlfriendEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_GIRLFRIEND.get(), 1, 1, 1));
        }
        if (ChaosPersists.BoyfriendEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_BOYFRIEND.get(), 1, 1, 1));
        }
        if (ChaosPersists.CowEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_RED_COW.get(), 10, 4, 8));
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_GOLD_COW.get(), 4, 2, 6));
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_ENCHANTED_COW.get(), 2, 2, 4));
        }
        if (ChaosPersists.ButterflyEnable != 0) {
            spawns.addSpawn(MobCategory.AMBIENT, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_BUTTERFLY.get(), 20, 3, 6));
        }
        if (ChaosPersists.MothEnable != 0) {
            spawns.addSpawn(MobCategory.AMBIENT, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_MOTH.get(), 10, 1, 5));
        }
        if (ChaosPersists.ChipmunkEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_CHIPMUNK.get(), 3, 1, 2));
        }
        if (ChaosPersists.CockateilEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_BIRD.get(), 10, 2, 4));
        }
        if (ChaosPersists.GoldFishEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_GOLD_FISH.get(), 1, 1, 1));
        }
        if (ChaosPersists.WhaleEnable != 0) {
            spawns.addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_WHALE.get(), 1, 1, 1));
        }
        if (ChaosPersists.FlounderEnable != 0) {
            spawns.addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_FLOUNDER.get(), 2, 2, 4));
        }
        if (ChaosPersists.CoinEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_COIN.get(), 2, 1, 1));
        }
        if (ChaosPersists.CricketEnable != 0) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_CRICKET.get(), 5, 4, 6));
        }
        if (ChaosPersists.FrogEnable != 0) {
            spawns.addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(ChaosPersists.ENTITY_TYPE_FROG.get(), 5, 4, 6));
        }
    }
}
