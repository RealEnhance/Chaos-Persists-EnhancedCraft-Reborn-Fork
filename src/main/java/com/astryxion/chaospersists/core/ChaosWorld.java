/*
 * Decompiled with CFR 0_125.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.fml.common.IWorldGenerator
 *  com.astryxion.chaospersists.BasiliskMaze
 *  com.astryxion.chaospersists.GenericDungeon
 *  com.astryxion.chaospersists.ItemAppleSeed
 *  com.astryxion.chaospersists.ItemMagicApple
 *  com.astryxion.chaospersists.OreGenericEgg
 *  com.astryxion.chaospersists.ChaosPersists
 *  com.astryxion.chaospersists.ChaosWorld
 *  com.astryxion.chaospersists.OreStats
 *  com.astryxion.chaospersists.RubyBirdDungeon
 *  com.astryxion.chaospersists.Trees
 *  com.astryxion.chaospersists.compat.minecraft.block.Block
 *  com.astryxion.chaospersists.compat.minecraft.block.BlockChest
 *  com.astryxion.chaospersists.compat.minecraft.block.BlockGrass
 *  com.astryxion.chaospersists.compat.minecraft.block.BlockLeaves
 *  com.astryxion.chaospersists.compat.minecraft.block.BlockLiquid
 *  com.astryxion.chaospersists.compat.minecraft.block.BlockSand
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLiving
 *  com.astryxion.chaospersists.compat.minecraft.init.Blocks
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.tileentity.MobSpawnerBaseLogic
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityChest
 *  net.minecraft.tileentity.TileEntityMobSpawner
 *  net.minecraft.util.WeightedRandomChestContent
 *  com.astryxion.chaospersists.compat.minecraft.world.World
 *  com.astryxion.chaospersists.compat.minecraft.world.WorldProvider
 *  com.astryxion.chaospersists.compat.minecraft.world.biome.Biome
 *  com.astryxion.chaospersists.compat.minecraft.world.chunk.Chunk
 *  com.astryxion.chaospersists.compat.minecraft.world.chunk.IChunkProvider
 *  com.astryxion.chaospersists.compat.minecraft.world.gen.feature.WorldGenMinable
 */
package com.astryxion.chaospersists.core;

import com.astryxion.chaospersists.util.MyUtils;
import com.astryxion.chaospersists.world.biome.BiomeMiningDimension;
import com.astryxion.chaospersists.world.biome.LegacyBiomeMatcher;
import com.astryxion.chaospersists.world.dimension.CrystalChunkDecorator;

import com.astryxion.chaospersists.world.dimension.structure.BasiliskMaze;
import com.astryxion.chaospersists.world.dimension.structure.GenericDungeon;
import com.astryxion.chaospersists.item.ItemAppleSeed;
import com.astryxion.chaospersists.item.ItemMagicApple;
import com.astryxion.chaospersists.world.ore.OreGenericEgg;
import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.util.OreStats;
import com.astryxion.chaospersists.world.dimension.structure.RubyBirdDungeon;
import com.astryxion.chaospersists.util.Trees;
import com.astryxion.chaospersists.util.SpawnerFixHelper;
import com.astryxion.chaospersists.util.WeightedRandomChestContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChaosWorld {
    private static final Logger LOGGER = LogManager.getLogger();
    public static int recently_placed = 50;
    private static final boolean DEBUG_NATURAL_DUPLICATOR_SPAWNS = true;

    private record PendingChunkGen(ResourceKey<Level> dimension, int chunkX, int chunkZ) {}

    /** Chunks that loaded before the server main loop; processed on {@link ServerStartedEvent}. */
    private static final Set<PendingChunkGen> PENDING_CHUNK_GEN = ConcurrentHashMap.newKeySet();

    /** Chunks already queued or pending — prevents duplicate plant/ore passes per chunk. */
    private static final Set<PendingChunkGen> SCHEDULED_CHUNK_GEN = ConcurrentHashMap.newKeySet();

    /** Chunks that finished {@link #runChunkWorldGen} (1.12 runs populate decoration once per chunk). */
    private static final Set<PendingChunkGen> PROCESSED_WORLD_GEN = ConcurrentHashMap.newKeySet();

    /** Deferred world-gen: legacy Java structures cannot run inside {@link net.minecraft.world.level.WorldGenLevel} without chunk-load deadlocks. */
    private static final Queue<PendingChunkGen> CHUNK_GEN_QUEUE = new ConcurrentLinkedQueue<>();

    /** Mod-dimension chunks near a player — decorated first so dimensions are playable immediately. */
    private static final Queue<PendingChunkGen> MOD_NEAR_CHUNK_GEN_QUEUE =
            new ConcurrentLinkedQueue<>();

    /** Dimensions that rely on deferred {@link #runChunkWorldGen} instead of vanilla chunk decoration. */
    private static final Set<ResourceKey<Level>> DEFERRED_POPULATE_DIMENSIONS =
            Set.of(
                    ChaosPersists.getUtopiaDimensionKey(),
                    ChaosPersists.getMiningDimensionKey(),
                    ChaosPersists.getVillageDimensionKey(),
                    ChaosPersists.getDangerDimensionKey(),
                    ChaosPersists.getCrystalDimensionKey(),
                    ChaosPersists.getChaosDimensionKey());

    /** Chunks captured from {@link ChunkEvent.Load} to avoid re-entrant {@link Level#getChunk} deadlocks. */
    private static final Map<PendingChunkGen, LevelChunk> CHUNK_GEN_CHUNKS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final int MAX_CHUNK_GEN_PER_TICK = 8;

    /** Utopia/danger/crystal populate is heavy (giant trees, islands, crystal structures). */
    private static final int MAX_HEAVY_CHUNK_GEN_PER_TICK = 4;

    /** Mining/village decoration includes ore passes and dungeons. */
    private static final int MAX_MEDIUM_CHUNK_GEN_PER_TICK = 6;

    /** Chebyshev chunk radius around each player for priority decoration in mod dimensions. */
    private static final int NEAR_PLAYER_CHUNK_RADIUS = 10;

    private static final long CHUNK_GEN_TICK_BUDGET_NS = 150_000_000L;

    /** Reserved for future worldgen-safe block writes; deferred path is active today. */
    static final ThreadLocal<Boolean> DURING_POPULATE_FEATURE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * True while {@link #prepareStructureFootprint} is synchronously flushing neighbor chunks.
     * Structure placement must be skipped during that flush or each prep pass spawns cascades of dungeons.
     */
    private static final ThreadLocal<Boolean> FLUSHING_FOR_STRUCTURE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean isDuringPopulateFeature() {
        return Boolean.TRUE.equals(DURING_POPULATE_FEATURE.get());
    }

    public static boolean isFlushingForStructure() {
        return Boolean.TRUE.equals(FLUSHING_FOR_STRUCTURE.get());
    }

    private boolean shouldPlaceStructures() {
        return !isFlushingForStructure();
    }

    public void generate(
            Random random,
            int chunkX,
            int chunkZ,
            Level level,
            LevelChunk levelChunk,
            Object chunkGenerator,
            Object chunkProvider) {
        if (level.isClientSide()) {
            return;
        }
        LevelChunk chunk =
                levelChunk != null ? levelChunk : level.getChunk(chunkX, chunkZ);
        if (recently_placed > 0) {
            --recently_placed;
        }
        if (level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())) {
            this.generateSurface(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            if (this.shouldPlaceStructures()) {
                if (!this.addHugeTree(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16,
                        chunk)) {
                    if (!this.addAppleTrees(
                                    level,
                                    net.minecraft.util.RandomSource.create(random.nextLong()),
                                    chunkX * 16,
                                    chunkZ * 16,
                                    chunk)
                            && !this.addOtherTrees(
                                    level,
                                    net.minecraft.util.RandomSource.create(random.nextLong()),
                                    chunkX * 16,
                                    chunkZ * 16)
                            && recently_placed == 0) {
                        this.addKingAltar(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    this.addVeggies(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()), chunkX * 16, chunkZ * 16);
                }
                boolean rbd = false;
                rbd = this.addRubyDungeon(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16);
                if (!rbd) {
                    this.addGenericDungeon(
                            level,
                            net.minecraft.util.RandomSource.create(random.nextLong()),
                            chunkX * 16,
                            chunkZ * 16);
                }
            }
            this.tryPlaceWaterLake(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16,
                    chunk,
                    16,
                    false);
            return;
        }
        if (level.dimension().equals(ChaosPersists.getMiningDimensionKey())) {
            int i;
            int baseX = chunkX * 16;
            int baseZ = chunkZ * 16;
            net.minecraft.util.RandomSource chunkOreRandom =
                    net.minecraft.util.RandomSource.create(random.nextLong());
            ChaosPersists.Chunker.generateOresInChunk(level, chunkOreRandom, baseX, baseZ, chunk);
            if (ChaosPersists.LessOre == 0) {
                ChaosPersists.Chunker.generateOresInChunk(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        baseX,
                        baseZ,
                        chunk);
                ChaosPersists.Chunker.generateOresInChunk(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        baseX,
                        baseZ,
                        chunk);
            }
            this.generateRuby(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            if (ChaosPersists.LessOre == 0) {
                int randPosY;
                int randPosX;
                int randPosZ;
                net.minecraft.util.RandomSource oreRandom =
                        net.minecraft.util.RandomSource.create(random.nextLong());
                this.generateRuby(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16);
                this.generateRuby(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16);
                for (i = 0; i < 45; ++i) {
                    randPosX = chunkX * 16 + random.nextInt(16);
                    randPosY = random.nextInt(128);
                    randPosZ = chunkZ * 16 + random.nextInt(16);
                    if (randPosY >= 50) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level, oreRandom, randPosX, randPosY, randPosZ, chunk, Blocks.LAPIS_ORE, 7);
                }
                for (i = 0; i < 25; ++i) {
                    randPosX = chunkX * 16 + random.nextInt(16);
                    randPosY = random.nextInt(128);
                    randPosZ = chunkZ * 16 + random.nextInt(16);
                    if (randPosY >= 50) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level, oreRandom, randPosX, randPosY, randPosZ, chunk, Blocks.LAPIS_ORE, 4);
                }
            }
            if (this.shouldPlaceStructures()) {
                if (recently_placed == 0 && random.nextInt(95) == 1) {
                    i = random.nextInt(7);
                    if (i == 0) {
                        this.addBasiliskMaze(level, chunkX * 16, chunkZ * 16);
                    }
                    if (i == 1) {
                        this.addKyuubiDungeon(
                                level,
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 2) {
                        this.addBeeHive(level, chunkX * 16, chunkZ * 16);
                    }
                    if (i == 3) {
                        this.addShadowDungeon(level, chunkX * 16, chunkZ * 16);
                    }
                    if (i == 4) {
                        this.addAlienWTF(level, chunkX * 16, chunkZ * 16);
                    }
                    if (i == 5) {
                        this.addEnderKnight(level, chunkX * 16, chunkZ * 16);
                    }
                    if (i == 6) {
                        this.addLeonNest(level, chunkX * 16, chunkZ * 16);
                    }
                } else {
                    this.addGenericDungeon(
                            level,
                            net.minecraft.util.RandomSource.create(random.nextLong()),
                            chunkX * 16,
                            chunkZ * 16);
                }
            }
            this.addLavaAndWater(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            this.addAnts(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16,
                    2);
            this.addAnts(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16,
                    2);
            this.addMosquitos(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            this.addMosquitos(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            this.addVeggies(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()), chunkX * 16, chunkZ * 16);
            this.addRocks(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            return;
        }
        if (level.dimension().equals(ChaosPersists.getDimensionKey(3))) {
            if (ChaosPersists.MosquitoEnable != 0) {
                this.addMosquitos(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX,
                        chunkZ);
            }
            this.addAnts(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16,
                    4);
            if (this.shouldPlaceStructures()) {
                this.addAppleTrees(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16,
                        chunk);
                this.addGenericDungeon(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16);
                if (recently_placed == 0) {
                    this.addDamselInDistress(
                            level,
                            net.minecraft.util.RandomSource.create(random.nextLong()),
                            chunkX * 16,
                            chunkZ * 16);
                }
                if (recently_placed == 0) {
                    this.addSpiderHangout(
                            level,
                            net.minecraft.util.RandomSource.create(random.nextLong()),
                            chunkX * 16,
                            chunkZ * 16);
                }
                if (recently_placed == 0) {
                    this.addRedAntHangout(
                            level,
                            net.minecraft.util.RandomSource.create(random.nextLong()),
                            chunkX * 16,
                            chunkZ * 16);
                }
            }
            this.tryPlaceWaterLake(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16,
                    chunk,
                    4,
                    false);
            return;
        }
        if (level.dimension().equals(ChaosPersists.getDangerDimensionKey())) {
            int i;
            int surfaceY = this.findIslandsGrassSurfaceY(level, chunkX * 16 + 8, chunkZ * 16 + 8);
            if (surfaceY < 0) {
                surfaceY = 7;
            }
            if (this.shouldPlaceStructures()
                    && recently_placed == 0
                    && random.nextInt(100) == 0
                    && this.D4BigSpaceCheck(
                            level, chunkX * 16, surfaceY, chunkZ * 16)) {
                i = random.nextInt(19);
                if (i < 3) {
                    this.addD4Castle(
                            level,
                            net.minecraft.util.RandomSource.create(random.nextLong()),
                            chunkX * 16,
                            chunkZ * 16);
                } else if (i < 7) {
                    this.addD4GenericDungeon(
                            level,
                            net.minecraft.util.RandomSource.create(random.nextLong()),
                            chunkX * 16,
                            chunkZ * 16);
                } else {
                    if (i == 7) {
                        this.addD4EnderCastle(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 8) {
                        this.addD4IncaPyramid(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 9) {
                        this.addD4RobotLab(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 10) {
                        this.addD4Mini(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 11) {
                        this.addD4RubyDungeon(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 12) {
                        this.addD4CephadromeAltar(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 13) {
                        this.addD4Greenhouse(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 14) {
                        this.addD4NightmareRookery(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 15) {
                        this.addD4StinkyHouse(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 16) {
                        this.addD4WhiteHouse(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 17) {
                        this.addPumpkin(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    if (i == 18) {
                        this.addD4Rainbow(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                }
            }
            if ((i = random.nextInt(300)) == 0) {
                this.addD4CloudShark(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16);
            }
            this.addUnstableAnts(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            this.addIslands(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            this.addD4Rocks(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            this.addScragglyTrees(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            return;
        }
        if (level.dimension().equals(ChaosPersists.getCrystalDimensionKey())) {
            RandomSource crystalRandom = net.minecraft.util.RandomSource.create(random.nextLong());
            CrystalChunkDecorator.decorate(level, crystalRandom, chunkX * 16, chunkZ * 16, chunk);
            if (!this.addFairyTree(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16)) {
                this.addCrystalTermites(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16);
                if (recently_placed == 0) {
                    if (!(this.addRotatorStation(level, net.minecraft.util.RandomSource.create(random.nextLong()), chunkX * 16, chunkZ * 16) || this.addUrchinSpawner(level, net.minecraft.util.RandomSource.create(random.nextLong()), chunkX * 16, chunkZ * 16) || this.addCrystalHauntedHouse(level, net.minecraft.util.RandomSource.create(random.nextLong()), chunkX * 16, chunkZ * 16) || this.addRoundRotator(level, net.minecraft.util.RandomSource.create(random.nextLong()), chunkX * 16, chunkZ * 16))) {
                        this.addCrystalBattleTower(
                                level,
                                net.minecraft.util.RandomSource.create(random.nextLong()),
                                chunkX * 16,
                                chunkZ * 16);
                    }
                    this.addIrukandji(
                            level,
                            net.minecraft.util.RandomSource.create(random.nextLong()),
                            chunkX * 16,
                            chunkZ * 16);
                }
            }
            this.addCrystalChestsAndSpawners(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            if (level.getRandom().nextInt(4) == 1) {
                this.addRocks(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        chunkX * 16,
                        chunkZ * 16);
            }
            return;
        }
        if (level.dimension().equals(ChaosPersists.getChaosDimensionKey())) {
            int baseX = chunkX * 16;
            int baseZ = chunkZ * 16;
            ChaosPersists.Chunker.generateOresInChunk(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    baseX,
                    baseZ,
                    chunk);
            if (random.nextInt(4) == 0) {
                this.addScragglyTrees(
                        level,
                        net.minecraft.util.RandomSource.create(random.nextLong()),
                        baseX,
                        baseZ);
            }
            this.addButterfliesAndMoths(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    baseX,
                    baseZ);
            this.addVeggies(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    baseX,
                    baseZ);
            this.addAnts(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    baseX,
                    baseZ,
                    2);
            return;
        }
        if (level.dimension() == net.minecraft.world.level.Level.NETHER) {
            this.generateNether(level, random, chunkX * 16, chunkZ * 16, chunk);
        } else if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            this.generateSurface(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
            this.generateOres(level, random, chunkX * 16, chunkZ * 16, chunk);
        } else if (level.dimension() == net.minecraft.world.level.Level.END) {
            this.generateEnd(
                    level,
                    net.minecraft.util.RandomSource.create(random.nextLong()),
                    chunkX * 16,
                    chunkZ * 16);
        }
    }

    private void generateEnd(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        this.addEndAnts(level, random, chunkX, chunkZ);
        int i = random.nextInt(4);
        if (i == 0) {
            this.addEndKnights(level, random, chunkX, chunkZ);
        }
        if (i == 1) {
            this.addEndReapers(level, random, chunkX, chunkZ);
        }
        if (i == 2) {
            this.addHospital(level, random, chunkX, chunkZ);
        }
        if (i == 3) {
            this.addEnderCastle(level, random, chunkX, chunkZ);
        }
    }

    private void generateNether(
            net.minecraft.world.level.Level level,
            Random random,
            int chunkX,
            int chunkZ,
            LevelChunk providedChunk) {
        net.minecraft.world.level.chunk.LevelChunk levelChunk =
                providedChunk != null
                        ? providedChunk
                        : (net.minecraft.world.level.chunk.LevelChunk)
                                level.getChunk(chunkX / 16, chunkZ / 16);
        net.minecraft.util.RandomSource oreRandom =
                net.minecraft.util.RandomSource.create(random.nextLong());
        int i;
        int randPosY;
        int randPosZ;
        int randPosX;
        if (ChaosPersists.MosquitoEnable != 0) {
            this.addNetherMosquitos(
                    level, net.minecraft.util.RandomSource.create(random.nextLong()), chunkX, chunkZ);
        }
        this.addNetherAnts(level, net.minecraft.util.RandomSource.create(random.nextLong()), chunkX, chunkZ);
        int patchy = 15 + random.nextInt(10);
        if (ChaosPersists.LessOre != 0) {
            patchy /= 3;
        }
        for (i = 0; i < patchy; ++i) {
            randPosX = 3 + chunkX + random.nextInt(13);
            randPosY = random.nextInt(108) + 10;
            randPosZ = 3 + chunkZ + random.nextInt(13);
            ChaosPersists.Chunker.generateBlockOre(
                    level,
                    oreRandom,
                    randPosX,
                    randPosY,
                    randPosZ,
                    levelChunk,
                    ChaosPersists.MyLavafoamBlock,
                    6,
                    Blocks.NETHERRACK);
        }
        patchy = 5 + random.nextInt(5);
        if (ChaosPersists.LessOre != 0) {
            patchy /= 3;
        }
        for (i = 0; i < patchy; ++i) {
            randPosX = 3 + chunkX + random.nextInt(13);
            randPosY = random.nextInt(108) + 10;
            randPosZ = 3 + chunkZ + random.nextInt(13);
            ChaosPersists.Chunker.generateBlockOre(
                    level,
                    oreRandom,
                    randPosX,
                    randPosY,
                    randPosZ,
                    levelChunk,
                    ChaosPersists.MyOreRubyBlock,
                    2,
                    Blocks.NETHERRACK);
        }
    }

    /**
     * Apply {@code forge/biome_modifier/*_mining.json} spawns only in the mining dimension (literal windswept hills).
     */
    @SubscribeEvent
    public void onDimensionPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        com.astryxion.chaospersists.world.biome.DimensionSpawnPopulation.onDimensionPotentialSpawns(
                event, serverLevel);
    }

    /**
     * 1.20.1 replacement for {@code GameRegistry.registerWorldGenerator(chaospersistsGen, 10)}.
     * Never calls {@link Level#getChunk} from inside {@link ChunkEvent.Load} (re-entrant load deadlock).
     * {@link net.minecraft.server.MinecraftServer#isRunning()} is true during {@code prepareLevels}
     * (spawn-prep at 0%), so use {@link net.minecraft.server.MinecraftServer#isReady()} and flush on
     * {@link ServerStartedEvent} instead.
     */
    @SubscribeEvent
    public void onChunkLoadForWorldGen(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean newChunk = event.isNewChunk();
        ResourceKey<Level> dimension = serverLevel.dimension();
        LevelChunk chunk = (LevelChunk) event.getChunk();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        if (isDeferredPopulateDimension(dimension)) {
            if (com.astryxion.chaospersists.world.ChaosChunkDecorationData.get(serverLevel)
                    .isDecorated(dimension, chunkX, chunkZ)) {
                return;
            }
        } else if (!newChunk) {
            return;
        }

        PendingChunkGen pending = new PendingChunkGen(dimension, chunkX, chunkZ);
        if (PROCESSED_WORLD_GEN.contains(pending) || !SCHEDULED_CHUNK_GEN.add(pending)) {
            return;
        }
        CHUNK_GEN_CHUNKS.put(pending, chunk);
        if (!serverLevel.getServer().isReady()) {
            PENDING_CHUNK_GEN.add(pending);
            return;
        }
        this.queuePendingChunk(serverLevel, pending);
    }

    private static boolean isDeferredPopulateDimension(ResourceKey<Level> dimension) {
        return DEFERRED_POPULATE_DIMENSIONS.contains(dimension);
    }

    private static int getMaxChunkGenPerTick(ResourceKey<Level> dimension) {
        if (dimension.equals(ChaosPersists.getUtopiaDimensionKey())
                || dimension.equals(ChaosPersists.getDangerDimensionKey())
                || dimension.equals(ChaosPersists.getCrystalDimensionKey())) {
            return MAX_HEAVY_CHUNK_GEN_PER_TICK;
        }
        if (dimension.equals(ChaosPersists.getMiningDimensionKey())
                || dimension.equals(ChaosPersists.getVillageDimensionKey())) {
            return MAX_MEDIUM_CHUNK_GEN_PER_TICK;
        }
        if (isDeferredPopulateDimension(dimension)) {
            return MAX_CHUNK_GEN_PER_TICK;
        }
        return MAX_CHUNK_GEN_PER_TICK;
    }

    private void queuePendingChunk(ServerLevel level, PendingChunkGen pending) {
        if (isDeferredPopulateDimension(pending.dimension)
                && this.isChunkNearPlayer(level, pending.chunkX, pending.chunkZ)) {
            MOD_NEAR_CHUNK_GEN_QUEUE.add(pending);
        } else {
            CHUNK_GEN_QUEUE.add(pending);
        }
    }

    private boolean isChunkNearPlayer(ServerLevel level, int chunkX, int chunkZ) {
        if (level.players().isEmpty()) {
            return true;
        }
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            net.minecraft.world.level.ChunkPos playerChunk = player.chunkPosition();
            if (Math.abs(playerChunk.x - chunkX) <= NEAR_PLAYER_CHUNK_RADIUS
                    && Math.abs(playerChunk.z - chunkZ) <= NEAR_PLAYER_CHUNK_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Chunks that missed decoration are re-queued while a player is in a mod dimension.
     */
    @SubscribeEvent
    public void onPlayerTickCatchUpDeferredPopulate(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player.level() instanceof ServerLevel level)) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        if (!isDeferredPopulateDimension(dimension)) {
            return;
        }
        if (event.player.tickCount % 20 != 0) {
            return;
        }
        int centerX = event.player.chunkPosition().x;
        int centerZ = event.player.chunkPosition().z;
        for (int dx = -NEAR_PLAYER_CHUNK_RADIUS; dx <= NEAR_PLAYER_CHUNK_RADIUS; ++dx) {
            for (int dz = -NEAR_PLAYER_CHUNK_RADIUS; dz <= NEAR_PLAYER_CHUNK_RADIUS; ++dz) {
                int chunkX = centerX + dx;
                int chunkZ = centerZ + dz;
                if (com.astryxion.chaospersists.world.ChaosChunkDecorationData.get(level)
                        .isDecorated(dimension, chunkX, chunkZ)) {
                    continue;
                }
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                PendingChunkGen pending = new PendingChunkGen(dimension, chunkX, chunkZ);
                if (PROCESSED_WORLD_GEN.contains(pending) || !SCHEDULED_CHUNK_GEN.add(pending)) {
                    continue;
                }
                MOD_NEAR_CHUNK_GEN_QUEUE.add(pending);
            }
        }
    }

    private static int findCrystalPlantingY(Level level, int x, int z) {
        if (ChaosPersists.CrystalGrass == null) {
            return -1;
        }
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int y = topY + 1; y >= level.getMinBuildHeight() + 8; --y) {
            net.minecraft.core.BlockPos feet = new net.minecraft.core.BlockPos(x, y, z);
            net.minecraft.core.BlockPos ground = feet.below();
            if (level.getBlockState(feet).isAir()
                    && level.getBlockState(ground).is(ChaosPersists.CrystalGrass)
                    && level.getFluidState(ground).isEmpty()) {
                return y;
            }
        }
        return -1;
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        for (PendingChunkGen pending : PENDING_CHUNK_GEN) {
            if (!PROCESSED_WORLD_GEN.contains(pending)) {
                ServerLevel level = event.getServer().getLevel(pending.dimension);
                if (level == null) {
                    CHUNK_GEN_QUEUE.add(pending);
                    continue;
                }
                this.queuePendingChunk(level, pending);
            }
        }
        PENDING_CHUNK_GEN.clear();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        CHUNK_GEN_QUEUE.clear();
        MOD_NEAR_CHUNK_GEN_QUEUE.clear();
        PENDING_CHUNK_GEN.clear();
        SCHEDULED_CHUNK_GEN.clear();
        PROCESSED_WORLD_GEN.clear();
        CHUNK_GEN_CHUNKS.clear();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (CHUNK_GEN_QUEUE.isEmpty() && MOD_NEAR_CHUNK_GEN_QUEUE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null || !server.isReady()) {
            return;
        }
        long deadline = System.nanoTime() + CHUNK_GEN_TICK_BUDGET_NS;
        Map<ResourceKey<Level>, Integer> dimensionProcessed = new HashMap<>();
        Queue<PendingChunkGen> modDeferred = new ConcurrentLinkedQueue<>();

        while (System.nanoTime() < deadline) {
            PendingChunkGen pending = MOD_NEAR_CHUNK_GEN_QUEUE.poll();
            boolean fromNear = pending != null;
            if (pending == null) {
                pending = CHUNK_GEN_QUEUE.poll();
            }
            if (pending == null) {
                break;
            }

            ServerLevel level = server.getLevel(pending.dimension);
            if (level == null) {
                SCHEDULED_CHUNK_GEN.remove(pending);
                CHUNK_GEN_CHUNKS.remove(pending);
                continue;
            }

            ResourceKey<Level> dimension = pending.dimension;
            if (isDeferredPopulateDimension(dimension)
                    && !fromNear
                    && !this.isChunkNearPlayer(level, pending.chunkX, pending.chunkZ)) {
                modDeferred.add(pending);
                continue;
            }

            int maxForDimension = getMaxChunkGenPerTick(dimension);
            int processed = dimensionProcessed.getOrDefault(dimension, 0);
            if (processed >= maxForDimension) {
                if (fromNear) {
                    MOD_NEAR_CHUNK_GEN_QUEUE.add(pending);
                } else {
                    CHUNK_GEN_QUEUE.add(pending);
                }
                break;
            }

            LevelChunk chunk = CHUNK_GEN_CHUNKS.remove(pending);
            if (chunk == null && !level.hasChunk(pending.chunkX, pending.chunkZ)) {
                SCHEDULED_CHUNK_GEN.remove(pending);
                continue;
            }
            SCHEDULED_CHUNK_GEN.remove(pending);
            if (PROCESSED_WORLD_GEN.add(pending)) {
                this.runChunkWorldGen(
                        level, chunk != null ? chunk : level.getChunk(pending.chunkX, pending.chunkZ));
                dimensionProcessed.put(dimension, processed + 1);
            }
        }

        while (!modDeferred.isEmpty()) {
            CHUNK_GEN_QUEUE.add(modDeferred.poll());
        }
    }

    private void runChunkWorldGen(ServerLevel serverLevel, LevelChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        Random random = new Random();
        random.setSeed(serverLevel.getSeed());
        random.setSeed(random.nextLong() ^ ((long) chunkX << 16) ^ (long) chunkZ);
        this.generate(random, chunkX, chunkZ, serverLevel, chunk, null, null);
        com.astryxion.chaospersists.world.biome.DimensionSpawnPopulation.afterChunkDecoration(
                serverLevel, chunk);
        ResourceKey<Level> dimension = serverLevel.dimension();
        if (isDeferredPopulateDimension(dimension)) {
            com.astryxion.chaospersists.world.ChaosChunkDecorationData.get(serverLevel)
                    .markDecorated(dimension, chunkX, chunkZ);
        }
    }

    public void generateSurface(
            Level level, RandomSource random, int chunkX, int chunkZ) {
        boolean ahh = false;
        this.addStrawberries(level, random, chunkX, chunkZ);
        this.addCorn(level, random, chunkX, chunkZ);
        this.addTomatoes(level, random, chunkX, chunkZ);
        this.addVeggies(level, random, chunkX, chunkZ);
        this.addButterfliesAndMoths(level, random, chunkX, chunkZ);
        if (ChaosPersists.MosquitoEnable != 0) {
            this.addMosquitos(level, random, chunkX, chunkZ);
        }
        if (ChaosPersists.DisableOverworldDungeons == 0
                && level.dimension() == net.minecraft.world.level.Level.OVERWORLD
                && recently_placed == 0) {
            int i = random.nextInt(6);
            if (i == 0) {
                this.addPlayPool(level, random, chunkX, chunkZ);
            }
            if (i == 1) {
                this.addWaterDragonLair(level, random, chunkX, chunkZ);
            }
            if (i == 2) {
                this.addGoldFishBowl(level, random, chunkX, chunkZ);
            }
            if (i == 3) {
                this.addGirlfriendIsland(level, random, chunkX, chunkZ);
            }
            if (i == 4) {
                this.addMonsterIsland(level, random, chunkX, chunkZ);
            }
            if (i == 5) {
                this.addFrogPond(level, random, chunkX, chunkZ);
            }
            if (!(ahh = this.addANest(level, random, chunkX, chunkZ))) {
                ahh = this.addHauntedHouse(level, random, chunkX, chunkZ);
            }
            if (!ahh) {
                ahh = this.addLeafMonster(level, random, chunkX, chunkZ);
            }
            if (!ahh) {
                ahh = this.addSpitBug(level, random, chunkX, chunkZ);
            }
            if (!ahh) {
                ahh = this.addIgloo(level, random, chunkX, chunkZ);
            }
            if (!ahh) {
                ahh = this.addBouncyCastle(level, random, chunkX, chunkZ);
            }
            if (!ahh) {
                ahh = this.addRubberDuckyPond(level, random, chunkX, chunkZ);
            }
        }
        this.addAnts(level, random, chunkX, chunkZ, 4);
        Holder<Biome> biome = level.getBiome(new BlockPos(chunkX, 0, chunkZ));
        if (LegacyBiomeMatcher.matchesLegacyPath(biome, "river")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "extreme_hills")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "desert")) {
            this.addRocks(level, random, chunkX, chunkZ);
        }
    }

    /**
     * 1.12 {@code WorldGenLakes} ponds for custom plains dimensions (vanilla removed lake features in 1.20.1).
     */
    private void tryPlaceWaterLake(
            Level level,
            RandomSource random,
            int chunkX,
            int chunkZ,
            LevelChunk chunk,
            int chunkRarity,
            boolean crystalTerrain) {
        if (random.nextInt(chunkRarity) != 0) {
            return;
        }
        int localX = random.nextInt(16);
        int localZ = random.nextInt(16);
        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ);
        BlockPos origin = new BlockPos(chunkX + localX, surfaceY, chunkZ + localZ);
        if (!this.isFlatLakeSite(level, chunk, origin, crystalTerrain)) {
            return;
        }
        this.placeWaterLakeAt(level, origin, crystalTerrain);
    }

    private boolean isFlatLakeSite(Level level, LevelChunk chunk, BlockPos origin, boolean crystalTerrain) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int baseX = origin.getX();
        int baseZ = origin.getZ();
        for (int dx = -6; dx <= 6; ++dx) {
            for (int dz = -6; dz <= 6; ++dz) {
                int x = baseX + dx;
                int z = baseZ + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                BlockPos surface = new BlockPos(x, y, z);
                if (!this.canLakeReplace(level.getBlockState(surface), crystalTerrain)) {
                    return false;
                }
            }
        }
        return maxY - minY <= 2;
    }

    private boolean canLakeReplace(BlockState state, boolean crystalTerrain) {
        if (state.isAir() || state.is(Blocks.WATER)) {
            return true;
        }
        if (crystalTerrain) {
            Block crystalGrass = ChaosPersists.CrystalGrass;
            Block crystalStone = ChaosPersists.CrystalStone;
            return (crystalGrass != null && state.is(crystalGrass))
                    || (crystalStone != null && state.is(crystalStone))
                    || state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.DIRT)
                    || state.is(Blocks.STONE);
        }
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.STONE)
                || state.is(Blocks.SAND)
                || state.is(Blocks.GRAVEL);
    }

    /** Port of 1.12 {@code WorldGenLakes#generate}. */
    private boolean placeWaterLakeAt(Level level, BlockPos origin, boolean crystalTerrain) {
        BlockPos pos = origin.offset(8, 0, 8);
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int l = 0; l < 16; ++l) {
            for (int i1 = 0; i1 < 16; ++i1) {
                for (int j1 = 0; j1 < 8; ++j1) {
                    if (!this.canLakeReplace(level.getBlockState(pos.offset(l, j1, i1)), crystalTerrain)) {
                        return false;
                    }
                }
            }
        }
        for (int l = 0; l < 16; ++l) {
            for (int i1 = 0; i1 < 16; ++i1) {
                for (int j1 = 0; j1 < 8; ++j1) {
                    if (l == 0
                            || l == 15
                            || i1 == 0
                            || i1 == 15
                            || j1 == 0
                            || j1 == 7) {
                        double d0 = (l - 7.5D) / 7.5D;
                        double d1 = (i1 - 7.5D) / 7.5D;
                        double d2 = (j1 - 3.5D) / 3.5D;
                        if (d0 * d0 + d1 * d1 + d2 * d2 < 1.0D) {
                            level.setBlock(pos.offset(l, j1, i1), water, 2);
                        }
                    } else {
                        level.setBlock(pos.offset(l, j1, i1), water, 2);
                    }
                }
            }
        }
        return true;
    }

    public void generateRuby(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.Ruby_stats.rate <= 0) {
            return;
        }
        int patchy = ChaosPersists.Ruby_stats.rate + random.nextInt(7);
        block0:
        for (int i = 0; i < patchy; ++i) {
            int randPosX = 3 + chunkX + random.nextInt(10);
            int randPosY = random.nextInt(128);
            int randPosZ = 3 + chunkZ + random.nextInt(10);
            if (randPosY > ChaosPersists.Ruby_stats.maxdepth || randPosY < ChaosPersists.Ruby_stats.mindepth) {
                continue;
            }
            for (int m = randPosY; m > 5; --m) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(randPosX, m, randPosZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(randPosX, m - 1, randPosZ);
                Block bid = level.getBlockState(pos).getBlock();
                if (!level.getBlockState(pos).is(Blocks.LAVA)
                        || !level.getBlockState(below).is(Blocks.STONE)) {
                    continue;
                }
                ChaosPersists.setBlockFast(
                        level, randPosX, m - 1, randPosZ, (Block) ChaosPersists.MyOreRubyBlock, 0, 2);
                continue block0;
            }
        }
    }

    public void generateOres(
            net.minecraft.world.level.Level level,
            Random random,
            int chunkX,
            int chunkZ,
            net.minecraft.world.level.chunk.LevelChunk providedChunk) {
        net.minecraft.world.level.chunk.LevelChunk levelChunk =
                providedChunk != null
                        ? providedChunk
                        : (net.minecraft.world.level.chunk.LevelChunk)
                                level.getChunk(chunkX / 16, chunkZ / 16);
        net.minecraft.util.RandomSource oreRandom =
                net.minecraft.util.RandomSource.create(random.nextLong());
        int i;
        int randPosY;
        int randPosZ;
        int patchy;
        int randPosX;
        if (ChaosPersists.SpawnOres_stats.rate > 0) {
            patchy = ChaosPersists.SpawnOres_stats.rate + random.nextInt(20);
            if (random.nextInt(20) == 0) {
                patchy += 30;
            }
            if (ChaosPersists.LessOre != 0) {
                patchy /= 3;
            }
            for (i = 0; i < patchy; ++i) {
                Block b;
                int j;
                randPosX = 3 + chunkX + random.nextInt(10);
                randPosY = random.nextInt(128);
                randPosZ = 3 + chunkZ + random.nextInt(10);
                if (randPosY > ChaosPersists.SpawnOres_stats.maxdepth || randPosY < ChaosPersists.SpawnOres_stats.mindepth) continue;
                if (random.nextInt(104) < 7) {
                    j = random.nextInt(7);
                    b = Blocks.AIR;
                    switch (j) {
                        case 0: {
                            b = ChaosPersists.MyBrutalflySpawnBlock;
                            break;
                        }
                        case 1: {
                            b = ChaosPersists.MyNastysaurusSpawnBlock;
                            break;
                        }
                        case 2: {
                            b = ChaosPersists.MyPointysaurusSpawnBlock;
                            break;
                        }
                        case 3: {
                            b = ChaosPersists.MyCricketSpawnBlock;
                            break;
                        }
                        case 4: {
                            b = ChaosPersists.MyFrogSpawnBlock;
                            break;
                        }
                        case 5: {
                            b = ChaosPersists.MyAloSpawnBlock;
                            break;
                        }
                        case 6: {
                            b = ChaosPersists.MyCrabSpawnBlock;
                            break;
                        }
                    }
                    ChaosPersists.Chunker.generateBlockOre(
                            level,
                            oreRandom,
                            randPosX,
                            randPosY,
                            randPosZ,
                            levelChunk,
                            b,
                            ChaosPersists.SpawnOres_stats.clumpsize);
                    continue;
                }
                j = random.nextInt(98);
                b = Blocks.AIR;
                switch (j) {
                    case 0: {
                        b = ChaosPersists.MySpiderSpawnBlock;
                        break;
                    }
                    case 1: {
                        b = ChaosPersists.MyBatSpawnBlock;
                        break;
                    }
                    case 2: {
                        b = ChaosPersists.MyCowSpawnBlock;
                        break;
                    }
                    case 3: {
                        b = ChaosPersists.MyPigSpawnBlock;
                        break;
                    }
                    case 4: {
                        b = ChaosPersists.MySquidSpawnBlock;
                        break;
                    }
                    case 5: {
                        b = ChaosPersists.MyChickenSpawnBlock;
                        break;
                    }
                    case 6: {
                        b = ChaosPersists.MyCreeperSpawnBlock;
                        break;
                    }
                    case 7: {
                        b = ChaosPersists.MySkeletonSpawnBlock;
                        break;
                    }
                    case 8: {
                        b = ChaosPersists.MyZombieSpawnBlock;
                        break;
                    }
                    case 9: {
                        b = ChaosPersists.MySlimeSpawnBlock;
                        break;
                    }
                    case 10: {
                        b = ChaosPersists.MyGhastSpawnBlock;
                        break;
                    }
                    case 11: {
                        b = ChaosPersists.MyZombiePigmanSpawnBlock;
                        break;
                    }
                    case 12: {
                        b = ChaosPersists.MyEndermanSpawnBlock;
                        break;
                    }
                    case 13: {
                        b = ChaosPersists.MyCaveSpiderSpawnBlock;
                        break;
                    }
                    case 14: {
                        b = ChaosPersists.MySilverfishSpawnBlock;
                        break;
                    }
                    case 15: {
                        b = ChaosPersists.MyMagmaCubeSpawnBlock;
                        break;
                    }
                    case 16: {
                        b = ChaosPersists.MyWitchSpawnBlock;
                        break;
                    }
                    case 17: {
                        b = ChaosPersists.MySheepSpawnBlock;
                        break;
                    }
                    case 18: {
                        b = ChaosPersists.MyWolfSpawnBlock;
                        break;
                    }
                    case 19: {
                        b = ChaosPersists.MyMooshroomSpawnBlock;
                        break;
                    }
                    case 20: {
                        b = ChaosPersists.MyOcelotSpawnBlock;
                        break;
                    }
                    case 21: {
                        b = ChaosPersists.MyBlazeSpawnBlock;
                        break;
                    }
                    case 22: {
                        b = ChaosPersists.MyWitherSkeletonSpawnBlock;
                        break;
                    }
                    case 23: {
                        b = ChaosPersists.MyEnderDragonSpawnBlock;
                        break;
                    }
                    case 24: {
                        b = ChaosPersists.MySnowGolemSpawnBlock;
                        break;
                    }
                    case 25: {
                        b = ChaosPersists.MyIronGolemSpawnBlock;
                        break;
                    }
                    case 26: {
                        b = ChaosPersists.MyWitherBossSpawnBlock;
                        break;
                    }
                    case 27: {
                        b = ChaosPersists.MyGirlfriendSpawnBlock;
                        break;
                    }
                    case 28: {
                        b = ChaosPersists.MyRedCowSpawnBlock;
                        break;
                    }
                    case 29: {
                        b = ChaosPersists.MyGoldCowSpawnBlock;
                        break;
                    }
                    case 30: {
                        b = ChaosPersists.MyEnchantedCowSpawnBlock;
                        break;
                    }
                    case 31: {
                        b = ChaosPersists.MyMOTHRASpawnBlock;
                        break;
                    }
                    case 32: {
                        b = ChaosPersists.MyAloSpawnBlock;
                        break;
                    }
                    case 33: {
                        b = ChaosPersists.MyCryoSpawnBlock;
                        break;
                    }
                    case 34: {
                        b = ChaosPersists.MyCamaSpawnBlock;
                        break;
                    }
                    case 35: {
                        b = ChaosPersists.MyVeloSpawnBlock;
                        break;
                    }
                    case 36: {
                        b = ChaosPersists.MyHydroSpawnBlock;
                        break;
                    }
                    case 37: {
                        b = ChaosPersists.MyBasilSpawnBlock;
                        break;
                    }
                    case 38: {
                        b = ChaosPersists.MyDragonflySpawnBlock;
                        break;
                    }
                    case 39: {
                        b = ChaosPersists.MyEmperorScorpionSpawnBlock;
                        break;
                    }
                    case 40: {
                        b = ChaosPersists.MyScorpionSpawnBlock;
                        break;
                    }
                    case 41: {
                        b = ChaosPersists.MyCaveFisherSpawnBlock;
                        break;
                    }
                    case 42: {
                        b = ChaosPersists.MySpyroSpawnBlock;
                        break;
                    }
                    case 43: {
                        b = ChaosPersists.MyBaryonyxSpawnBlock;
                        break;
                    }
                    case 44: {
                        b = ChaosPersists.MyGammaMetroidSpawnBlock;
                        break;
                    }
                    case 45: {
                        b = ChaosPersists.MyCockateilSpawnBlock;
                        break;
                    }
                    case 46: {
                        b = ChaosPersists.MyKyuubiSpawnBlock;
                        break;
                    }
                    case 47: {
                        b = ChaosPersists.MyAlienSpawnBlock;
                        break;
                    }
                    case 48: {
                        b = ChaosPersists.MyAttackSquidSpawnBlock;
                        break;
                    }
                    case 49: {
                        b = ChaosPersists.MyWaterDragonSpawnBlock;
                        break;
                    }
                    case 50: {
                        b = ChaosPersists.MyKrakenSpawnBlock;
                        break;
                    }
                    case 51: {
                        b = ChaosPersists.MyLizardSpawnBlock;
                        break;
                    }
                    case 52: {
                        b = ChaosPersists.MyCephadromeSpawnBlock;
                        break;
                    }
                    case 53: {
                        b = ChaosPersists.MyDragonSpawnBlock;
                        break;
                    }
                    case 54: {
                        b = ChaosPersists.MyBeeSpawnBlock;
                        break;
                    }
                    case 55: {
                        b = ChaosPersists.MyHorseSpawnBlock;
                        break;
                    }
                    case 56: {
                        b = ChaosPersists.MyTrooperBugSpawnBlock;
                        break;
                    }
                    case 57: {
                        b = ChaosPersists.MySpitBugSpawnBlock;
                        break;
                    }
                    case 58: {
                        b = ChaosPersists.MyStinkBugSpawnBlock;
                        break;
                    }
                    case 59: {
                        b = ChaosPersists.MyOstrichSpawnBlock;
                        break;
                    }
                    case 60: {
                        b = ChaosPersists.MyGazelleSpawnBlock;
                        break;
                    }
                    case 61: {
                        b = ChaosPersists.MyChipmunkSpawnBlock;
                        break;
                    }
                    case 62: {
                        b = ChaosPersists.MyCreepingHorrorSpawnBlock;
                        break;
                    }
                    case 63: {
                        b = ChaosPersists.MyTerribleTerrorSpawnBlock;
                        break;
                    }
                    case 64: {
                        b = ChaosPersists.MyCliffRacerSpawnBlock;
                        break;
                    }
                    case 65: {
                        b = ChaosPersists.MyTriffidSpawnBlock;
                        break;
                    }
                    case 66: {
                        b = ChaosPersists.MyPitchBlackSpawnBlock;
                        break;
                    }
                    case 67: {
                        b = ChaosPersists.MyLurkingTerrorSpawnBlock;
                        break;
                    }
                    case 68: {
                        b = ChaosPersists.MyGodzillaPartSpawnBlock;
                        break;
                    }
                    case 69: {
                        b = ChaosPersists.MyGodzillaSpawnBlock;
                        break;
                    }
                    case 70: {
                        b = ChaosPersists.MySmallWormSpawnBlock;
                        break;
                    }
                    case 71: {
                        b = ChaosPersists.MyMediumWormSpawnBlock;
                        break;
                    }
                    case 72: {
                        b = ChaosPersists.MyLargeWormSpawnBlock;
                        break;
                    }
                    case 73: {
                        b = ChaosPersists.MyCassowarySpawnBlock;
                        break;
                    }
                    case 74: {
                        b = ChaosPersists.MyCloudSharkSpawnBlock;
                        break;
                    }
                    case 75: {
                        b = ChaosPersists.MyGoldFishSpawnBlock;
                        break;
                    }
                    case 76: {
                        b = ChaosPersists.MyLeafMonsterSpawnBlock;
                        break;
                    }
                    case 77: {
                        b = ChaosPersists.MyTshirtSpawnBlock;
                        break;
                    }
                    case 78: {
                        b = ChaosPersists.MyEnderKnightSpawnBlock;
                        break;
                    }
                    case 79: {
                        b = ChaosPersists.MyEnderReaperSpawnBlock;
                        break;
                    }
                    case 80: {
                        b = ChaosPersists.MyBeaverSpawnBlock;
                        break;
                    }
                    case 81: {
                        b = ChaosPersists.MyTRexSpawnBlock;
                        break;
                    }
                    case 82: {
                        b = ChaosPersists.MyHerculesSpawnBlock;
                        break;
                    }
                    case 83: {
                        b = ChaosPersists.MyMantisSpawnBlock;
                        break;
                    }
                    case 84: {
                        b = ChaosPersists.MyStinkySpawnBlock;
                        break;
                    }
                    case 85: {
                        b = ChaosPersists.MyBoyfriendSpawnBlock;
                        break;
                    }
                    case 86: {
                        b = ChaosPersists.MyTheKingPartSpawnBlock;
                        break;
                    }
                    case 87: {
                        b = ChaosPersists.MyEasterBunnySpawnBlock;
                        break;
                    }
                    case 88: {
                        b = ChaosPersists.MyCaterKillerSpawnBlock;
                        break;
                    }
                    case 89: {
                        b = ChaosPersists.MyMolenoidSpawnBlock;
                        break;
                    }
                    case 90: {
                        b = ChaosPersists.MySeaMonsterSpawnBlock;
                        break;
                    }
                    case 91: {
                        b = ChaosPersists.MySeaViperSpawnBlock;
                        break;
                    }
                    case 92: {
                        b = ChaosPersists.MyLeonSpawnBlock;
                        break;
                    }
                    case 93: {
                        b = ChaosPersists.MyHammerheadSpawnBlock;
                        break;
                    }
                    case 94: {
                        b = ChaosPersists.MyRubberDuckySpawnBlock;
                        break;
                    }
                    case 95: {
                        b = ChaosPersists.MyVillagerSpawnBlock;
                        break;
                    }
                    case 96: {
                        b = ChaosPersists.MyCriminalSpawnBlock;
                        break;
                    }
                    case 97: {
                        b = ChaosPersists.MyTheQueenPartSpawnBlock;
                        break;
                    }
                }
                ChaosPersists.Chunker.generateBlockOre(
                        level,
                        oreRandom,
                        randPosX,
                        randPosY,
                        randPosZ,
                        levelChunk,
                        b,
                        ChaosPersists.SpawnOres_stats.clumpsize);
            }
        }
        // Uranium/Titanium/Amethyst/Salt/Ruby ore for the Overworld are no longer generated here — they now
        // go through the vanilla Feature/PlacedFeature/BiomeModifier pipeline (ChaosOreVeinFeature +
        // data/chaospersists/worldgen/{configured_feature,placed_feature}/<ore>_ore_vein.json +
        // forge/biome_modifier/<ore>_ore_overworld.json), which also fixes this branch's old
        // random.nextInt(128) height cap (0-127, the 1.7.10 world height) so veins can reach modern
        // deepslate depth, and matches every stone-tier/deepslate-tier block via the
        // stone_ore_replaceables/deepslate_ore_replaceables tags instead of only literal minecraft:stone.
        // Each still reads its stats live via ChaosPersists.getOreStats(...), so config edits (including
        // the "LessOre" toggle) behave exactly as before. Ruby uses a second Feature,
        // ChaosLavaPocketOreFeature, since its placement is a special "ore directly under a lava pocket"
        // search rather than a plain stone-replacing vein.
        patchy = 4 + random.nextInt(4);
        if (ChaosPersists.LessOre != 0) {
            patchy /= 2;
        }
        for (i = 0; i < patchy; ++i) {
            randPosX = 3 + chunkX + random.nextInt(10);
            randPosY = random.nextInt(128);
            randPosZ = 3 + chunkZ + random.nextInt(10);
            if (randPosY > 50 || randPosY < 5) continue;
            ChaosPersists.Chunker.generateBlockOre(
                    level, oreRandom, randPosX, randPosY, randPosZ, levelChunk, ChaosPersists.RedAntTroll, 4);
        }
        patchy = 4 + random.nextInt(4);
        if (ChaosPersists.LessOre != 0) {
            patchy /= 2;
        }
        for (i = 0; i < patchy; ++i) {
            randPosX = 3 + chunkX + random.nextInt(10);
            randPosY = random.nextInt(128);
            randPosZ = 3 + chunkZ + random.nextInt(10);
            if (randPosY > 50 || randPosY < 5) continue;
            ChaosPersists.Chunker.generateBlockOre(
                    level, oreRandom, randPosX, randPosY, randPosZ, levelChunk, ChaosPersists.TermiteTroll, 4);
        }
        if (ChaosPersists.LessOre == 0) {
            if (ChaosPersists.Diamond_stats.rate > 0) {
                for (i = 0; i < ChaosPersists.Diamond_stats.rate; ++i) {
                    randPosX = 3 + chunkX + random.nextInt(10);
                    randPosY = random.nextInt(128);
                    randPosZ = 3 + chunkZ + random.nextInt(10);
                    if (randPosY > ChaosPersists.Diamond_stats.maxdepth || randPosY < ChaosPersists.Diamond_stats.mindepth) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level,
                            oreRandom,
                            randPosX,
                            randPosY,
                            randPosZ,
                            levelChunk,
                            Blocks.DIAMOND_ORE,
                            ChaosPersists.Diamond_stats.clumpsize);
                }
            }
            if (ChaosPersists.BlkDiamond_stats.rate > 0) {
                for (i = 0; i < ChaosPersists.BlkDiamond_stats.rate; ++i) {
                    randPosX = 3 + chunkX + random.nextInt(10);
                    randPosY = random.nextInt(128);
                    randPosZ = 3 + chunkZ + random.nextInt(10);
                    if (randPosY > ChaosPersists.BlkDiamond_stats.maxdepth || randPosY < ChaosPersists.BlkDiamond_stats.mindepth) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level,
                            oreRandom,
                            randPosX,
                            randPosY,
                            randPosZ,
                            levelChunk,
                            Blocks.DIAMOND_BLOCK,
                            ChaosPersists.BlkDiamond_stats.clumpsize);
                }
            }
            if (ChaosPersists.Emerald_stats.rate > 0) {
                for (i = 0; i < ChaosPersists.Emerald_stats.rate; ++i) {
                    randPosX = 3 + chunkX + random.nextInt(10);
                    randPosY = random.nextInt(128);
                    randPosZ = 3 + chunkZ + random.nextInt(10);
                    if (randPosY > ChaosPersists.Emerald_stats.maxdepth || randPosY < ChaosPersists.Emerald_stats.mindepth) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level,
                            oreRandom,
                            randPosX,
                            randPosY,
                            randPosZ,
                            levelChunk,
                            Blocks.EMERALD_ORE,
                            ChaosPersists.Emerald_stats.clumpsize);
                }
            }
            if (ChaosPersists.BlkEmerald_stats.rate > 0) {
                for (i = 0; i < ChaosPersists.BlkEmerald_stats.rate; ++i) {
                    randPosX = 3 + chunkX + random.nextInt(10);
                    randPosY = random.nextInt(128);
                    randPosZ = 3 + chunkZ + random.nextInt(10);
                    if (randPosY > ChaosPersists.BlkEmerald_stats.maxdepth || randPosY < ChaosPersists.BlkEmerald_stats.mindepth) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level,
                            oreRandom,
                            randPosX,
                            randPosY,
                            randPosZ,
                            levelChunk,
                            Blocks.EMERALD_BLOCK,
                            ChaosPersists.BlkEmerald_stats.clumpsize);
                }
            }
            if (ChaosPersists.Gold_stats.rate > 0) {
                for (i = 0; i < ChaosPersists.Gold_stats.rate; ++i) {
                    randPosX = 3 + chunkX + random.nextInt(10);
                    randPosY = random.nextInt(128);
                    randPosZ = 3 + chunkZ + random.nextInt(10);
                    if (randPosY > ChaosPersists.Gold_stats.maxdepth || randPosY < ChaosPersists.Gold_stats.mindepth) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level,
                            oreRandom,
                            randPosX,
                            randPosY,
                            randPosZ,
                            levelChunk,
                            Blocks.GOLD_ORE,
                            ChaosPersists.Gold_stats.clumpsize);
                }
            }
            if (ChaosPersists.BlkGold_stats.rate > 0) {
                for (i = 0; i < ChaosPersists.BlkGold_stats.rate; ++i) {
                    randPosX = 3 + chunkX + random.nextInt(10);
                    randPosY = random.nextInt(128);
                    randPosZ = 3 + chunkZ + random.nextInt(10);
                    if (randPosY > ChaosPersists.BlkGold_stats.maxdepth || randPosY < ChaosPersists.BlkGold_stats.mindepth) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level,
                            oreRandom,
                            randPosX,
                            randPosY,
                            randPosZ,
                            levelChunk,
                            Blocks.GOLD_BLOCK,
                            ChaosPersists.BlkGold_stats.clumpsize);
                }
            }
            if (ChaosPersists.BlkRuby_stats.rate > 0) {
                for (i = 0; i < ChaosPersists.BlkRuby_stats.rate; ++i) {
                    randPosX = 3 + chunkX + random.nextInt(10);
                    randPosY = random.nextInt(128);
                    randPosZ = 3 + chunkZ + random.nextInt(10);
                    if (randPosY > ChaosPersists.BlkRuby_stats.maxdepth || randPosY < ChaosPersists.BlkRuby_stats.mindepth) continue;
                    ChaosPersists.Chunker.generateBlockOre(
                            level,
                            oreRandom,
                            randPosX,
                            randPosY,
                            randPosZ,
                            levelChunk,
                            ChaosPersists.MyBlockRubyBlock,
                            ChaosPersists.BlkRuby_stats.clumpsize);
                }
            }
        }
    }

    /** OreSpawn 1.7.10 {@code addCorn}/{@code addTomatoes}: Utopia, Village Mania, or Plains. */
    private static boolean isCornTomatoChunkEligible(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
        if (level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())
                || level.dimension().equals(ChaosPersists.getVillageDimensionKey())) {
            return true;
        }
        return LegacyBiomeMatcher.matchesLegacyPath(
                level.getBiome(new BlockPos(chunkX, 0, chunkZ)), "plains");
    }

    private static boolean isStrawberryForestBiome(Holder<Biome> biome) {
        return LegacyBiomeMatcher.matchesLegacyPath(biome, "forest")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "birch_forest");
    }

    private static boolean canSpawnStrawberryAt(net.minecraft.world.level.Level level, int posX, int posY, int posZ) {
        if (level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())) {
            return true;
        }
        return isStrawberryForestBiome(level.getBiome(new BlockPos(posX, posY, posZ)));
    }

    private static boolean isButterflyOverworldBiome(Holder<Biome> biome) {
        return LegacyBiomeMatcher.matchesLegacyPath(biome, "forest")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "river")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "jungle")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "swampland")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "birch_forest");
    }

    private static boolean isButterflyBiomeChunkGate(net.minecraft.world.level.Level level, Holder<Biome> chunkBiome) {
        return level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())
                || level.dimension().equals(ChaosPersists.getDimensionKey(6))
                || isButterflyOverworldBiome(chunkBiome);
    }

    private static boolean canSpawnVeggiesAt(net.minecraft.world.level.Level level, int posX, int posY, int posZ) {
        if (level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())
                || level.dimension().equals(ChaosPersists.getMiningDimensionKey())
                || level.dimension().equals(ChaosPersists.getDimensionKey(6))) {
            return true;
        }
        Holder<Biome> biome = level.getBiome(new BlockPos(posX, posY, posZ));
        return LegacyBiomeMatcher.matchesLegacyPath(biome, "river")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "swampland");
    }

    public void addStrawberries(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(20) != 0) {
            return;
        }
        block0:
        for (int i = 0; i < 5; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 40; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                        .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                if (!canSpawnStrawberryAt(level, posX, posY, posZ)) {
                    continue;
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY, posZ, (Block) ChaosPersists.MyStrawberryPlant, 0, 2);
                continue block0;
            }
        }
    }

    public boolean addHauntedHouse(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(285) != 0) {
            return false;
        }
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder =
                level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ));
        if (biomeHolder.is(net.minecraft.world.level.biome.Biomes.PLAINS)
                || biomeHolder.is(net.minecraft.world.level.biome.Biomes.TAIGA)
                || biomeHolder.is(net.minecraft.world.level.biome.Biomes.SWAMP)) {
            for (int i = 0; i < 5; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(pos).isAir()) {
                        break;
                    }
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                            .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeHauntedHouse(level, posX, posY, posZ);
                    recently_placed = 50;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addANest(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(230) != 0) {
            return false;
        }
        Holder<Biome> biome = level.getBiome(new BlockPos(chunkX, 0, chunkZ));
        if (LegacyBiomeMatcher.matchesLegacyPath(biome, "forest")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "jungle")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "birch_forest")) {
            for (int i = 0; i < 5; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 128; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(pos).isAir()) {
                        break;
                    }
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                            .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    if (random.nextInt(2) == 0) {
                        ChaosPersists.MyDungeon.makeSmallBeeHive(level, posX, posY, posZ);
                    } else {
                        ChaosPersists.MyDungeon.makeMantisHive(level, posX, posY, posZ);
                    }
                    recently_placed = 50;
                    return true;
                }
            }
        }
        return false;
    }

    /** OreSpawn 1.7.10 {@code addCorn}: 1/35 per chunk, up to 6 clusters (LessLag reduces count). */
    public void addCorn(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        int nc = 6;
        if (random.nextInt(35) != 1) {
            return;
        }
        if (ChaosPersists.LessLag == 1) {
            nc = 5;
        }
        if (ChaosPersists.LessLag == 2) {
            nc = 3;
        }
        if (!isCornTomatoChunkEligible(level, chunkX, chunkZ)) {
            return;
        }
        block0:
        for (int j = 0; j < nc; ++j) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            boolean is_all_air = true;
            for (int posY = 100; posY > 40; --posY) {
                int i;
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                        .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                for (i = 1; i < 10; ++i) {
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY + i, posZ)).isAir()) {
                        is_all_air = false;
                    }
                }
                if (!is_all_air) {
                    continue block0;
                }
                int corn_height = random.nextInt(5);
                if (++corn_height == 1) {
                    ChaosPersists.setBlockFast(
                            level, posX, posY, posZ, (Block) ChaosPersists.MyCornPlant1, 0, 2);
                }
                if (corn_height == 2) {
                    ChaosPersists.setBlockFast(
                            level, posX, posY, posZ, (Block) ChaosPersists.MyCornPlant2, 0, 2);
                    ChaosPersists.setBlockFast(
                            level, posX, posY + 1, posZ, (Block) ChaosPersists.MyCornPlant1, 0, 2);
                }
                if (corn_height <= 2) {
                    continue block0;
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY, posZ, (Block) ChaosPersists.MyCornPlant2, 0, 2);
                for (i = 1; i < corn_height; ++i) {
                    ChaosPersists.setBlockFast(
                            level, posX, posY + i, posZ, (Block) ChaosPersists.MyCornPlant4, 0, 2);
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY + corn_height, posZ, (Block) ChaosPersists.MyCornPlant1, 0, 2);
                continue block0;
            }
        }
    }

    /** OreSpawn 1.7.10 {@code addTomatoes}: 1/70 per chunk, 5 placement attempts. */
    public void addTomatoes(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        boolean is_all_air = true;
        if (random.nextInt(70) != 1) {
            return;
        }
        if (!isCornTomatoChunkEligible(level, chunkX, chunkZ)) {
            return;
        }
        block0:
        for (int j = 0; j < 5; ++j) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            is_all_air = true;
            for (int posY = 100; posY > 40; --posY) {
                int i;
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                        .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                for (i = 1; i < 10; ++i) {
                    if (level.getBlockState(new net.minecraft.core.BlockPos(posX, posY + i, posZ)).isAir()) {
                        continue;
                    }
                    is_all_air = false;
                }
                if (!is_all_air) {
                    continue block0;
                }
                int corn_height = random.nextInt(3);
                if (++corn_height == 1) {
                    ChaosPersists.setBlockFast(
                            level, posX, posY, posZ, (Block) ChaosPersists.MyTomatoPlant1, 0, 2);
                }
                if (corn_height == 2) {
                    ChaosPersists.setBlockFast(
                            level, posX, posY, posZ, (Block) ChaosPersists.MyTomatoPlant2, 0, 2);
                    ChaosPersists.setBlockFast(
                            level, posX, posY + 1, posZ, (Block) ChaosPersists.MyTomatoPlant1, 0, 2);
                }
                if (corn_height <= 2) {
                    continue block0;
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY, posZ, (Block) ChaosPersists.MyTomatoPlant3, 0, 2);
                for (i = 1; i < corn_height; ++i) {
                    ChaosPersists.setBlockFast(
                            level, posX, posY + i, posZ, (Block) ChaosPersists.MyTomatoPlant4, 0, 2);
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY + corn_height, posZ, (Block) ChaosPersists.MyTomatoPlant1, 0, 2);
                continue block0;
            }
        }
    }

    public void addButterfliesAndMoths(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(10 + ChaosPersists.LessLag * 2) != 0) {
            return;
        }
        Holder<Biome> biome = level.getBiome(new BlockPos(chunkX, 0, chunkZ));
        if (isButterflyBiomeChunkGate(level, biome)) {
            block0:
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(pos).isAir()) {
                        break;
                    }
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                            .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    if (!level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())
                            && !level.dimension().equals(ChaosPersists.getDimensionKey(6))
                            && !isButterflyOverworldBiome(level.getBiome(new BlockPos(posX, posY, posZ)))) {
                        continue;
                    }
                    int which = random.nextInt(3);
                    if (which == 0) {
                        ChaosPersists.setBlockFast(
                                level, posX, posY, posZ, (Block) ChaosPersists.MyButterflyPlant, 0, 2);
                        continue block0;
                    }
                    if (which == 1) {
                        ChaosPersists.setBlockFast(
                                level, posX, posY, posZ, (Block) ChaosPersists.MyMothPlant, 0, 2);
                        continue block0;
                    }
                    ChaosPersists.setBlockFast(
                            level, posX, posY, posZ, (Block) ChaosPersists.MyFireflyPlant, 0, 2);
                    continue block0;
                }
            }
        }
    }

    public void addPlayPool(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(350) != 0) {
            return;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.OCEAN)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir() || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makePlayPool(level, posX, posY, posZ);
                    recently_placed = 50;
                    return;
                }
            }
        }
    }

    public void addFrogPond(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(350) != 0) {
            return;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.PLAINS)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeFrogPond(level, posX, posY - 1, posZ);
                    recently_placed = 50;
                    return;
                }
            }
        }
    }

    public void addGoldFishBowl(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(350) != 0) {
            return;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.OCEAN)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeGoldFishBowl(level, posX, posY - 1, posZ);
                    recently_placed = 50;
                    return;
                }
            }
        }
    }

    public boolean addLeafMonster(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(275) != 0) {
            return false;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.PLAINS)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeLeafMonsterDungeon(level, posX, posY, posZ);
                    recently_placed = 50;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addRubberDuckyPond(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(275) != 0) {
            return false;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.PLAINS)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeRubberDuckyPond(level, posX, posY, posZ);
                    recently_placed = 50;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addSpitBug(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(190) != 0) {
            return false;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.SWAMP)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeSpitBugLair(level, posX, posY, posZ);
                    recently_placed = 50;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addIgloo(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(220) != 0) {
            return false;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.SNOW)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeIgloo(level, posX, posY - 2, posZ);
                    recently_placed = 50;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addBouncyCastle(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(230) != 0) {
            return false;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.DESERT)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.SAND)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeBouncyCastle(level, posX, posY - 1, posZ);
                    recently_placed = 50;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addDamselInDistress(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(250) != 0) {
            return false;
        }
        for (int i = 0; i < 4; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 40; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                        || !this.quickSpaceCheck(level, posX, posY - 1, posZ)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeDamselInDistress(level, posX, posY - 1, posZ);
                recently_placed = 50;
                return true;
            }
        }
        return false;
    }

    public boolean addSpiderHangout(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(350) != 0) {
            return false;
        }
        if (ChaosPersists.SpiderDriverEnable == 0) {
            return false;
        }
        for (int i = 0; i < 4; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 40; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                        || !this.quickSpaceCheck(level, posX, posY - 1, posZ)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeSpiderHangout(level, posX, posY - 1, posZ);
                recently_placed = 50;
                return true;
            }
        }
        return false;
    }

    public boolean addRedAntHangout(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(250) != 0) {
            return false;
        }
        for (int i = 0; i < 4; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 40; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                        || !this.quickSpaceCheck(level, posX, posY - 1, posZ)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeRedAntHangout(level, posX, posY - 1, posZ);
                recently_placed = 50;
                return true;
            }
        }
        return false;
    }

    public void addWaterDragonLair(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(350) != 0) {
            return;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.OCEAN)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir() || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeWaterDragonLair(level, posX, posY - 1, posZ);
                    recently_placed = 50;
                    return;
                }
            }
        }
    }

    public void addGirlfriendIsland(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(300) != 0) {
            return;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.OCEAN)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeGirlfriendIsland(level, posX, posY - 1, posZ);
                    recently_placed = 50;
                    return;
                }
            }
        }
    }

    public void addMonsterIsland(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(300) != 0) {
            return;
        }
        if (level.getBiome(new net.minecraft.core.BlockPos(chunkX, 0, chunkZ)).is(net.minecraft.world.level.biome.Biomes.OCEAN)) {
            for (int i = 0; i < 4; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                    if (!level.getBlockState(pos).isAir()
                            || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        continue;
                    }
                    ChaosPersists.MyDungeon.makeMonsterIsland(level, posX, posY - 1, posZ);
                    recently_placed = 50;
                    return;
                }
            }
        }
    }

    public void addMosquitos(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(25 + ChaosPersists.LessLag * 2) != 0) {
            return;
        }
        if ((level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())
                        || level.dimension().equals(ChaosPersists.getDimensionKey(3)))
                && random.nextInt(3) != 0) {
            return;
        }
        Holder<Biome> biome = level.getBiome(new BlockPos(chunkX, 0, chunkZ));
        if (level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())
                || level.dimension().equals(ChaosPersists.getMiningDimensionKey())
                || level.dimension().equals(ChaosPersists.getDimensionKey(3))
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "jungle")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "swampland")) {
            block0:
            for (int i = 0; i < 2; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(pos).isAir()) {
                        break;
                    }
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                            .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    ChaosPersists.setBlockFast(
                            level, posX, posY, posZ, (Block) ChaosPersists.MyMosquitoPlant, 0, 2);
                    continue block0;
                }
            }
        }
    }

    public void addNetherMosquitos(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(25) != 0) {
            return;
        }
        block0:
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 90; posY > 20; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.NETHERRACK)) {
                    continue;
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY, posZ, (Block) ChaosPersists.MyMosquitoPlant, 0, 2);
                continue block0;
            }
        }
    }

    public void addNetherAnts(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.RedAntEnable == 0) {
            return;
        }
        if (random.nextInt(25) != 0) {
            return;
        }
        block0:
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 90; posY > 20; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.NETHERRACK)) {
                    continue;
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY - 1, posZ, (Block) ChaosPersists.MyRedAntBlock, 0, 2);
                continue block0;
            }
        }
    }

    public void addAnts(
            net.minecraft.world.level.Level level,
            net.minecraft.util.RandomSource random,
            int chunkX,
            int chunkZ,
            int redfreq) {
        if (ChaosPersists.RedAntEnable == 0
                && ChaosPersists.BlackAntEnable == 0
                && ChaosPersists.RainbowAntEnable == 0
                && ChaosPersists.UnstableAntEnable == 0) {
            return;
        }
        if (redfreq < 2) {
            redfreq = 2;
        }
        if (random.nextInt(30 + ChaosPersists.LessLag * 4) != 0) {
            return;
        }
        block0:
        for (int i = 0; i < 4; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 40; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                        .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                if (random.nextInt(redfreq) == 0) {
                    int which = random.nextInt(4);
                    if (which == 0 && ChaosPersists.RedAntEnable != 0) {
                        ChaosPersists.setBlockFast(
                                level, posX, posY - 1, posZ, (Block) ChaosPersists.MyRedAntBlock, 0, 2);
                    }
                    if (which == 1 && ChaosPersists.RainbowAntEnable != 0) {
                        ChaosPersists.setBlockFast(
                                level, posX, posY - 1, posZ, (Block) ChaosPersists.MyRainbowAntBlock, 0, 2);
                    }
                    if (which == 2 && ChaosPersists.UnstableAntEnable != 0) {
                        ChaosPersists.setBlockFast(
                                level, posX, posY - 1, posZ, (Block) ChaosPersists.MyUnstableAntBlock, 0, 2);
                    }
                    if (which != 3 || ChaosPersists.TermiteEnable == 0) {
                        continue block0;
                    }
                    ChaosPersists.setBlockFast(
                            level, posX, posY - 1, posZ, (Block) ChaosPersists.TermiteBlock, 0, 2);
                    continue block0;
                }
                if (ChaosPersists.BlackAntEnable == 0) {
                    continue block0;
                }
                ChaosPersists.setBlockFast(level, posX, posY - 1, posZ, (Block) ChaosPersists.MyAntBlock, 0, 2);
                continue block0;
            }
        }
    }

    public void addEndAnts(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
    }

    public void addEndKnights(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(25) != 0) {
            return;
        }
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 90; posY > 10; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.END_STONE)
                        || !this.quickSpaceCheck(level, posX, posY, posZ)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeEnderKnightDungeon(level, posX, posY, posZ);
                return;
            }
        }
    }

    public void addEndReapers(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(25) != 0) {
            return;
        }
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 90; posY > 10; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.END_STONE)
                        || !this.quickSpaceCheck(level, posX, posY, posZ)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeEnderReaperGraveyard(level, posX, posY, posZ);
                return;
            }
        }
    }

    public void addHospital(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(25) != 0) {
            return;
        }
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 90; posY > 10; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.END_STONE)
                        || !this.quickSpaceCheck(level, posX, posY, posZ)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeEnderDragonHospital(level, posX, posY, posZ);
                return;
            }
        }
    }

    public void addEnderCastle(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(50) != 0) {
            return;
        }
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 90; posY > 10; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.END_STONE)
                        || !this.quickBigSpaceCheck(level, posX, posY, posZ)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeEnderCastle(level, posX, posY, posZ);
                return;
            }
        }
    }

    public void addUnstableAnts(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.UnstableAntEnable == 0) {
            return;
        }
        if (random.nextInt(30) != 0) {
            return;
        }
        block0:
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 20; posY > 2; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                        .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY - 1, posZ, (Block) ChaosPersists.MyUnstableAntBlock, 0, 2);
                continue block0;
            }
        }
    }

    public void addCrystalTermites(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.TermiteEnable == 0) {
            return;
        }
        if (random.nextInt(40) != 0) {
            return;
        }
        block0:
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || level.getBlockState(below).getBlock() != ChaosPersists.CrystalGrass) {
                    continue;
                }
                ChaosPersists.setBlockFast(
                        level, posX, posY - 1, posZ, (Block) ChaosPersists.CrystalTermiteBlock, 0, 2);
                continue block0;
            }
        }
    }

    public boolean addRotatorStation(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.RotatorEnable == 0) {
            return false;
        }
        if (random.nextInt(150) != 0) {
            return false;
        }
        net.minecraft.world.level.block.Block crystalGrass =
                (net.minecraft.world.level.block.Block) (Object) ChaosPersists.CrystalGrass;
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir() || !level.getBlockState(below).is(crystalGrass)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeRotatorStation(level, posX, posY, posZ);
                recently_placed = 50;
                return true;
            }
        }
        return false;
    }

    public boolean addRoundRotator(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.RotatorEnable == 0) {
            return false;
        }
        if (random.nextInt(150) != 0) {
            return false;
        }
        net.minecraft.world.level.block.Block crystalGrass =
                (net.minecraft.world.level.block.Block) (Object) ChaosPersists.CrystalGrass;
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir() || !level.getBlockState(below).is(crystalGrass)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeRoundRotator(level, posX, posY, posZ);
                recently_placed = 50;
                return true;
            }
        }
        return false;
    }

    public boolean addUrchinSpawner(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.UrchinEnable == 0) {
            return false;
        }
        if (random.nextInt(180) != 0) {
            return false;
        }
        net.minecraft.world.level.block.Block crystalGrass =
                (net.minecraft.world.level.block.Block) (Object) ChaosPersists.CrystalGrass;
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir() || !level.getBlockState(below).is(crystalGrass)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeUrchinSpawner(level, posX, posY, posZ);
                recently_placed = 50;
                return true;
            }
        }
        return false;
    }

    public boolean addCrystalHauntedHouse(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(230) != 0) {
            return false;
        }
        net.minecraft.world.level.block.Block crystalGrass =
                (net.minecraft.world.level.block.Block) (Object) ChaosPersists.CrystalGrass;
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(crystalGrass)) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeCrystalHauntedHouse(level, posX, posY, posZ);
                recently_placed = 50;
                return true;
            }
        }
        return false;
    }

    public boolean addCrystalBattleTower(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(280) != 0) {
            return false;
        }
        net.minecraft.world.level.block.Block crystalGrass =
                (net.minecraft.world.level.block.Block) (Object) ChaosPersists.CrystalGrass;
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || level.getBlockState(below).getBlock() != crystalGrass) {
                    continue;
                }
                ChaosPersists.MyDungeon.makeCrystalBattleTower(level, posX, posY, posZ);
                recently_placed = 50;
                return true;
            }
        }
        return false;
    }

    public void addIrukandji(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.IrukandjiEnable == 0) {
            return;
        }
        if (random.nextInt(80) != 0) {
            return;
        }
        for (int i = 0; i < 3; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.WATER)) {
                    continue;
                }
                this.placeChaosWorldSpawner(level, posX, posY, posZ, "irukandji");
                return;
            }
        }
    }

    public void addCrystalChestsAndSpawners(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        for (int i = 0; i < 3; ++i) {
            int posX = 1 + chunkX + random.nextInt(14);
            int posY = 25;
            int posZ = 1 + chunkZ + random.nextInt(14);
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            if (level.getBlockState(new net.minecraft.core.BlockPos(posX + 1, posY, posZ)).isAir()) {
                this.addCrystalChest(level, random, posX, posY, posZ, 5);
                break;
            }
            if (level.getBlockState(new net.minecraft.core.BlockPos(posX - 1, posY, posZ)).isAir()) {
                this.addCrystalChest(level, random, posX, posY, posZ, 4);
                break;
            }
            if (level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ + 1)).isAir()) {
                this.addCrystalChest(level, random, posX, posY, posZ, 2);
                break;
            }
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ - 1)).isAir()) {
                break;
            }
            this.addCrystalChest(level, random, posX, posY, posZ, 3);
            break;
        }
    }

    public void addCrystalChest(
            net.minecraft.world.level.Level level,
            net.minecraft.util.RandomSource random,
            int x,
            int y,
            int z,
            int dir) {
        int i = random.nextInt(3);
        if (i == 0) {
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
            level.setBlock(
                    pos,
                    net.minecraft.world.level.block.Blocks.CHEST
                            .defaultBlockState()
                            .setValue(
                                    net.minecraft.world.level.block.ChestBlock.FACING,
                                    this.crystalChestFacingFromLegacyMeta(dir)),
                    3);
            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                WeightedRandomChestContent.generateChestContents(
                        random,
                        Trees.CrystalChestContentsList,
                        chest,
                        1 + random.nextInt(3));
            }
        } else {
            int t = random.nextInt(2);
            if (t == 0) {
                this.placeChaosWorldSpawner(level, x, y, z, "dungeon_beast");
            }
            if (t == 1) {
                this.placeChaosWorldSpawner(level, x, y, z, "rat");
            }
        }
    }

    private static net.minecraft.core.Direction crystalChestFacingFromLegacyMeta(int meta) {
        return switch (meta) {
            case 2 -> net.minecraft.core.Direction.NORTH;
            case 3 -> net.minecraft.core.Direction.SOUTH;
            case 4 -> net.minecraft.core.Direction.WEST;
            case 5 -> net.minecraft.core.Direction.EAST;
            default -> net.minecraft.core.Direction.NORTH;
        };
    }

    private void placeChaosWorldSpawner(
            net.minecraft.world.level.Level level, int x, int y, int z, String mobPath) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState(), 2);
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
            net.minecraft.resources.ResourceLocation id =
                    SpawnerFixHelper.normalizeSpawnerEntityId(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    "chaospersists", mobPath));
            net.minecraft.world.entity.EntityType<?> type =
                    net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type != null) {
                spawner.setEntityId(type, level.getRandom());
            }
        }
    }

    public void addIslands(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        int posX = 2 + chunkX + random.nextInt(12);
        int posZ = 2 + chunkZ + random.nextInt(12);
        if (random.nextInt(10 + ChaosPersists.LessLag * 2) != 1) {
            return;
        }
        int plantY = this.findIslandsPlantingY(level, posX, posZ);
        if (plantY > 0) {
            ChaosPersists.setBlockFast(level, posX, plantY, posZ, (Block) ChaosPersists.MyIslandBlock, 0, 2);
        }
    }

    private int findIslandsGrassSurfaceY(net.minecraft.world.level.Level level, int posX, int posZ) {
        for (int y = Math.min(128, level.getMaxBuildHeight() - 1); y > level.getMinBuildHeight(); --y) {
            if (level.getBlockState(new net.minecraft.core.BlockPos(posX, y, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                return y;
            }
        }
        return -1;
    }

    private int findIslandsPlantingY(net.minecraft.world.level.Level level, int posX, int posZ) {
        int minY = level.getMinBuildHeight() + 1;
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 2); posY > minY; --posY) {
            net.minecraft.core.BlockPos airPos = new net.minecraft.core.BlockPos(posX, posY, posZ);
            if (!level.getBlockState(airPos).isAir()) {
                break;
            }
            if (level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                return posY;
            }
        }
        return -1;
    }

    public void addScragglyTrees(
            net.minecraft.world.level.Level level,
            net.minecraft.util.RandomSource random,
            int chunkX,
            int chunkZ) {
        int howmany = 1 + random.nextInt(10);
        if (ChaosPersists.LessLag == 1) {
            howmany /= 2;
        }
        if (ChaosPersists.LessLag == 2) {
            howmany /= 4;
        }
        if (howmany == 0) {
            return;
        }
        Trees trees = ChaosPersists.chaospersistsTrees;
        block0:
        for (int i = 0; i < howmany; ++i) {
            int posX = 2 + chunkX + random.nextInt(12);
            int posZ = 2 + chunkZ + random.nextInt(12);
            int plantY = this.findIslandsPlantingY(level, posX, posZ);
            if (plantY > 0) {
                trees.ScragglyTreeWithBranches(level, random, posX, plantY, posZ);
                continue block0;
            }
        }
    }

    public boolean addAppleTrees(
            net.minecraft.world.level.Level level,
            net.minecraft.util.RandomSource random,
            int chunkX,
            int chunkZ,
            LevelChunk chunk) {
        int freq = Math.abs(chunkX / 16) + Math.abs(chunkZ / 16);
        int howmany = 2;
        int which = 0;
        boolean added = false;
        howmany += random.nextInt(2 + (15 - (freq %= 15)) / 2);
        which = random.nextInt(10);
        if (random.nextInt(15 + freq) != 0) {
            return false;
        }
        if (ChaosPersists.LessLag == 1) {
            howmany /= 2;
        }
        if (ChaosPersists.LessLag == 2 && (howmany /= 4) < 1) {
            return false;
        }
        block0:
        for (int i = 0; i < howmany; ++i) {
            int posX = 2 + chunkX + random.nextInt(12);
            int posZ = 2 + chunkZ + random.nextInt(12);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                        .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                ItemAppleSeed a = (ItemAppleSeed) ChaosPersists.MyAppleSeed;
                if (which < 8) {
                    a.makeTree(level, posX, posY - 1, posZ, ChaosPersists.MyAppleLeaves, chunk);
                }
                if (which == 8) {
                    a.makeTree(level, posX, posY - 1, posZ, ChaosPersists.MyCherryLeaves, chunk);
                }
                if (which == 9) {
                    a.makeTree(level, posX, posY - 1, posZ, ChaosPersists.MyPeachLeaves, chunk);
                }
                added = true;
                continue block0;
            }
        }
        return added;
    }

    public boolean addHugeTree(
            net.minecraft.world.level.Level level,
            net.minecraft.util.RandomSource random,
            int chunkX,
            int chunkZ,
            LevelChunk chunk) {
        int made_one = 0;

        if (random.nextInt(50) != 0) {
            return false;
        }
        if ((ChaosPersists.LessLag == 1) && (random.nextInt(2) != 0)) {
            return false;
        }

        if ((ChaosPersists.LessLag == 2) && (random.nextInt(4) != 0)) {
            return false;
        }

        for (int i = 0; (i < 3) && (made_one == 0); i++) {
            int posX = 4 + chunkX + random.nextInt(8);
            int posZ = 4 + chunkZ + random.nextInt(8);
            for (int posY = 127; (posY > 50) && (made_one == 0); posY--) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                                .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                ItemMagicApple a = (ItemMagicApple) (Object) ChaosPersists.MagicApple;
                if (a == null) {
                    continue;
                }

                int tree_type = random.nextInt(4);
                int tree_radius = 6 - random.nextInt(2);
                boolean no_critters = false;
                Block leaf_type = Blocks.OAK_LEAVES;

                if (random.nextInt(100) > 25) {
                    no_critters = true;
                }
                int rand_treetype = random.nextInt(100);
                if (rand_treetype > 75) {
                    if ((tree_type != 3) && (random.nextInt(20) == 0)) {
                        leaf_type = ChaosPersists.MyAppleLeaves;
                    }
                    a.MakeBigSquareTree(
                            level,
                            posX,
                            posY - 1,
                            posZ,
                            Blocks.OAK_LOG,
                            leaf_type,
                            Blocks.MOSSY_COBBLESTONE,
                            tree_type,
                            tree_radius,
                            no_critters,
                            null);
                } else if (rand_treetype == 0) {
                    tree_radius = 6;
                    no_critters = true;
                    if (random.nextInt(2) == 0) {
                        a.MakeBigSquareTree(
                                level,
                                posX,
                                posY - 1,
                                posZ,
                                Blocks.GOLD_BLOCK,
                                Blocks.EMERALD_BLOCK,
                                Blocks.DIAMOND_BLOCK,
                                -1,
                                tree_radius,
                                no_critters,
                                null);
                    } else {
                        a.MakeBigSquareTree(
                                level,
                                posX,
                                posY - 1,
                                posZ,
                                Blocks.OBSIDIAN,
                                ChaosPersists.MyBlockRubyBlock,
                                ChaosPersists.MyBlockAmethystBlock,
                                -1,
                                tree_radius,
                                no_critters,
                                null);
                    }
                } else if (rand_treetype > 15) {
                    tree_radius = 6 - random.nextInt(3);
                    a.MakeBigCircularTree(
                            level,
                            posX,
                            posY - 1,
                            posZ,
                            Blocks.OAK_LOG,
                            leaf_type,
                            Blocks.MOSSY_COBBLESTONE,
                            tree_type,
                            tree_radius,
                            no_critters,
                            null);
                } else {
                    tree_radius = 6 - random.nextInt(3);
                    a.MakeBigRoundTree(
                            level,
                            posX,
                            posY - 1,
                            posZ,
                            Blocks.OAK_LOG,
                            leaf_type,
                            Blocks.MOSSY_COBBLESTONE,
                            tree_type,
                            tree_radius,
                            null);
                }

                made_one = 1;
                break;
            }
        }

        return made_one != 0;
    }

    public void addVeggies(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(15) != 0) {
            return;
        }
        Holder<Biome> biome = level.getBiome(new BlockPos(chunkX, 0, chunkZ));
        if (level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())
                || level.dimension().equals(ChaosPersists.getMiningDimensionKey())
                || level.dimension().equals(ChaosPersists.getDimensionKey(6))
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "river")
                || LegacyBiomeMatcher.matchesLegacyPath(biome, "swampland")) {
            block0:
            for (int i = 0; i < 8; ++i) {
                int posX = chunkX + random.nextInt(16);
                int posZ = chunkZ + random.nextInt(16);
                for (int posY = 100; posY > 40; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(pos).isAir()) {
                        break;
                    }
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                            .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    if (!canSpawnVeggiesAt(level, posX, posY, posZ)) {
                        continue;
                    }
                    int what = random.nextInt(6);
                    if (what == 0) {
                        ChaosPersists.setBlockFast(level, posX, posY, posZ, Blocks.CARROTS, 0, 2);
                        continue block0;
                    }
                    if (what == 1) {
                        ChaosPersists.setBlockFast(level, posX, posY, posZ, Blocks.POTATOES, 0, 2);
                        continue block0;
                    }
                    if (what == 2) {
                        ChaosPersists.setBlockFast(
                                level, posX, posY, posZ, (Block) ChaosPersists.MyRadishPlant, 0, 2);
                        continue block0;
                    }
                    if (what == 3) {
                        ChaosPersists.setBlockFast(
                                level, posX, posY, posZ, (Block) ChaosPersists.MyLettucePlant1, 0, 2);
                        continue block0;
                    }
                    if (what == 4) {
                        if (random.nextInt(10) != 0) {
                            continue block0;
                        }
                        ChaosPersists.setBlockFast(level, posX, posY, posZ, Blocks.MELON_STEM, 0, 2);
                        continue block0;
                    }
                    if (random.nextInt(50) != 1 || ChaosPersists.enableduplicatortree == 0) {
                        continue block0;
                    }
                    ChaosPersists.setBlockFast(level, posX, posY, posZ, (Block) ChaosPersists.MyDT, 0, 2);
                    if (DEBUG_NATURAL_DUPLICATOR_SPAWNS) {
                        LOGGER.info(
                                "ChaosPersists DEBUG: natural duplicator log placed at dim {} ({}, {}, {})",
                                level.dimension().location(),
                                posX,
                                posY,
                                posZ);
                    }
                    continue block0;
                }
            }
        }
    }

    public void addRocks(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(5) != 0) {
            return;
        }
        if (ChaosPersists.RockEnable == 0) {
            return;
        }
        Block crystalGrass = ChaosPersists.CrystalGrass;
        int howmany = 3 + random.nextInt(10);
        block0:
        for (int i = 0; i < howmany; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 110; posY > 40; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(posX, posY - 1, posZ);
                Block bid = level.getBlockState(below).getBlock();
                if (bid != Blocks.GRASS && bid != Blocks.SAND && bid != crystalGrass) {
                    continue;
                }
                this.spawnCreature(level, "Rock", (double) posX, (double) posY, (double) posZ);
                continue block0;
            }
        }
    }

    public void addD4Rocks(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(7) != 0) {
            return;
        }
        if (ChaosPersists.RockEnable == 0) {
            return;
        }
        int howmany = 3 + random.nextInt(10);
        block0:
        for (int i = 0; i < howmany; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 20; posY > 5; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                        .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                this.spawnCreature(level, "Rock", (double) posX, (double) posY, (double) posZ);
                continue block0;
            }
        }
    }

    public boolean addFairyTree(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        int posX = chunkX + 8;
        int posZ = chunkZ + 8;
        Block crystalGrass = ChaosPersists.CrystalGrass;
        if (random.nextInt(5) != 0) {
            return false;
        }
        for (int posY = 128; posY > 40; --posY) {
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
            if (!level.getBlockState(pos).isAir()
                    || level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ)).getBlock()
                            != crystalGrass) {
                continue;
            }
            for (int i = -8; i <= 8; ++i) {
                for (int j = -8; j <= 8; ++j) {
                    if (level.getBlockState(new net.minecraft.core.BlockPos(posX + i, posY, posZ + j)).isAir()) {
                        continue;
                    }
                    return false;
                }
            }
            for (int i = -2; i <= 2; ++i) {
                for (int j = -2; j <= 2; ++j) {
                    if (level.getBlockState(new net.minecraft.core.BlockPos(posX + i, posY - 1, posZ + j)).getBlock()
                            == crystalGrass) {
                        continue;
                    }
                    return false;
                }
            }
            if (random.nextInt(5) != 1) {
                ChaosPersists.chaospersistsTrees.FairyTree(level, posX, posY - 1, posZ);
            } else {
                ChaosPersists.chaospersistsTrees.FairyCastleTree(level, posX, posY, posZ);
            }
            recently_placed = 50;
            break;
        }
        return true;
    }

    public boolean addRubyDungeon(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(15) != 0) {
            return false;
        }
        for (int i = 0; i < 8; ++i) {
            int posX = chunkX + random.nextInt(8);
            int posZ = chunkZ + random.nextInt(8);
            for (int posY = 50; posY > 5; --posY) {
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                        .is(net.minecraft.world.level.block.Blocks.LAVA)) {
                    continue;
                }
                ChaosPersists.RubyDungeon.makeDungeon(level, posX, posY, posZ);
                return true;
            }
        }
        return false;
    }

    public boolean addGenericDungeon(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(16) != 0) {
            return false;
        }
        if (ChaosPersists.LessLag == 1 && random.nextInt(2) != 0) {
            return false;
        }
        if (ChaosPersists.LessLag == 2 && random.nextInt(4) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(4);
        int posZ = chunkZ + random.nextInt(4);
        int dungeonWidth = 12;
        int dungeonHeight = 6;
        int minSurface = Integer.MAX_VALUE;
        int maxSurface = Integer.MIN_VALUE;
        for (int dx = 0; dx < dungeonWidth; ++dx) {
            for (int dz = 0; dz < dungeonWidth; ++dz) {
                int surfaceY = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, posX + dx, posZ + dz);
                minSurface = Math.min(minSurface, surfaceY);
                maxSurface = Math.max(maxSurface, surfaceY);
            }
        }
        if (maxSurface - minSurface > 3) {
            return false;
        }
        int posY = minSurface - dungeonHeight - 8 - random.nextInt(12);
        if (posY < level.getMinBuildHeight() + 4) {
            return false;
        }
        if (posY + dungeonHeight >= minSurface) {
            return false;
        }
        ChaosPersists.MyDungeon.makeDungeon(level, posX, posY, posZ);
        return true;
    }

    public boolean addBeeHive(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
        int lowestY = 128;
        int lowestX = chunkX;
        int lowestZ = chunkZ;
        boolean found = false;
        for (int i = 0; i < 16; i += 3) {
            block1 : for (int j = 0; j < 16; j += 3) {
                int posX = chunkX + i;
                int posZ = chunkZ + j;
                for (int posY = 128; posY > 30; --posY) {
                    net.minecraft.core.BlockPos above = new net.minecraft.core.BlockPos(posX, posY + 1, posZ);
                    net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(above).isAir()
                            || !level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    if (posY >= lowestY) continue block1;
                    lowestY = posY;
                    lowestX = posX;
                    lowestZ = posZ;
                    found = true;
                    continue block1;
                }
            }
        }
        if (found && lowestY > 40) {
            int posY = lowestY + 3;
            ChaosPersists.MyDungeon.makeBeeHive(level, lowestX, posY, lowestZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addAlienWTF(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
        int lowestY = 128;
        int lowestX = chunkX;
        int lowestZ = chunkZ;
        boolean found = false;
        for (int i = 0; i < 16; i += 3) {
            block1 : for (int j = 0; j < 16; j += 3) {
                int posX = chunkX + i;
                int posZ = chunkZ + j;
                for (int posY = 128; posY > 30; --posY) {
                    net.minecraft.core.BlockPos above = new net.minecraft.core.BlockPos(posX, posY + 1, posZ);
                    net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(above).isAir()
                            || !level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    if (posY >= lowestY) continue block1;
                    lowestY = posY;
                    lowestX = posX;
                    lowestZ = posZ;
                    found = true;
                    continue block1;
                }
            }
        }
        if (found && lowestY > 40) {
            ChaosPersists.MyDungeon.makeAlienWTFDungeon(level, lowestX, lowestY, lowestZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addEnderKnight(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
        int lowestY = 128;
        int lowestX = chunkX;
        int lowestZ = chunkZ;
        boolean found = false;
        for (int i = 0; i < 16; i += 3) {
            block1 : for (int j = 0; j < 16; j += 3) {
                int posX = chunkX + i;
                int posZ = chunkZ + j;
                for (int posY = 128; posY > 30; --posY) {
                    net.minecraft.core.BlockPos above = new net.minecraft.core.BlockPos(posX, posY + 1, posZ);
                    net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(above).isAir()
                            || !level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    if (posY >= lowestY) continue block1;
                    lowestY = posY;
                    lowestX = posX;
                    lowestZ = posZ;
                    found = true;
                    continue block1;
                }
            }
        }
        if (found && lowestY > 40) {
            ChaosPersists.MyDungeon.makeEnderKnightDungeon(level, lowestX, lowestY, lowestZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addLeonNest(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
        int highestY = 30;
        int highestX = chunkX;
        int highestZ = chunkZ;
        boolean found = false;
        for (int i = 0; i < 16; i += 3) {
            block1 : for (int j = 0; j < 16; j += 3) {
                int posX = chunkX + i;
                int posZ = chunkZ + j;
                for (int posY = 128; posY > 80; --posY) {
                    net.minecraft.core.BlockPos above = new net.minecraft.core.BlockPos(posX, posY + 1, posZ);
                    net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(above).isAir()
                            || !level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    if (posY <= highestY) continue block1;
                    highestY = posY + 1;
                    highestX = posX;
                    highestZ = posZ;
                    found = true;
                    continue block1;
                }
            }
        }
        if (found && highestY > 80) {
            ChaosPersists.MyDungeon.makeLeonNest(level, highestX, highestY, highestZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addShadowDungeon(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
        int lowestY = 128;
        int lowestX = chunkX;
        int lowestZ = chunkZ;
        boolean found = false;
        for (int i = 0; i < 16; i += 3) {
            block1 : for (int j = 0; j < 16; j += 3) {
                int posX = chunkX + i;
                int posZ = chunkZ + j;
                for (int posY = 128; posY > 30; --posY) {
                    net.minecraft.core.BlockPos above = new net.minecraft.core.BlockPos(posX, posY + 1, posZ);
                    net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(above).isAir()
                            || !level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    if (posY >= lowestY) continue block1;
                    lowestY = posY;
                    lowestX = posX;
                    lowestZ = posZ;
                    found = true;
                    continue block1;
                }
            }
        }
        if (found && lowestY > 40) {
            ChaosPersists.MyDungeon.makeShadowDungeon(level, lowestX, lowestY, lowestZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4RubyDungeon(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            ChaosPersists.RubyDungeon.makeDungeon(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4CephadromeAltar(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            ChaosPersists.MyDungeon.makeCephadromeAltar(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4Castle(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            for (int x = -20; x < 33; ++x) {
                for (int z = -4; z < 33; ++z) {
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX + x, posY + 18, posZ + z))
                            .isAir()) {
                        return false;
                    }
                }
            }
            if (random.nextInt(2) == 1) {
                ChaosPersists.MyDungeon.makeEnormousCastle(level, posX, posY, posZ);
            } else {
                ChaosPersists.MyDungeon.makeEnormousCastleQ(level, posX, posY, posZ);
            }
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4Greenhouse(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            for (int x = -2; x < 25; ++x) {
                for (int z = -4; z < 25; ++z) {
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX + x, posY + 18, posZ + z))
                            .isAir()) {
                        return false;
                    }
                }
            }
            ChaosPersists.MyDungeon.makeGreenhouseDungeon(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4NightmareRookery(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            for (int x = -5; x < 25; ++x) {
                for (int z = -4; z < 5; ++z) {
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX + x, posY + 18, posZ + z))
                            .isAir()) {
                        return false;
                    }
                }
            }
            ChaosPersists.MyDungeon.makeNightmareRookery(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4StinkyHouse(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            for (int x = -8; x < 20; ++x) {
                for (int z = -8; z < 20; ++z) {
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX + x, posY + 18, posZ + z))
                            .isAir()) {
                        return false;
                    }
                }
            }
            ChaosPersists.MyDungeon.makeStinkyHouse(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4WhiteHouse(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            for (int x = -20; x < 30; ++x) {
                for (int z = -20; z < 300; ++z) {
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX + x, posY + 18, posZ + z))
                            .isAir()) {
                        return false;
                    }
                }
            }
            ChaosPersists.MyDungeon.makeWhiteHouse(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4EnderCastle(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            for (int x = -5; x < 25; ++x) {
                for (int z = -5; z < 25; ++z) {
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX + x, posY + 18, posZ + z))
                            .isAir()) {
                        return false;
                    }
                }
            }
            ChaosPersists.MyDungeon.makeEnderCastle(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4IncaPyramid(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            for (int x = -10; x < 50; ++x) {
                for (int z = -10; z < 40; ++z) {
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX + x, posY + 18, posZ + z))
                            .isAir()) {
                        return false;
                    }
                }
            }
            ChaosPersists.MyDungeon.makeIncaPyramid(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4RobotLab(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        net.minecraft.world.level.block.Block appleLeaves =
                (net.minecraft.world.level.block.Block) (Object) ChaosPersists.MyAppleLeaves;
        net.minecraft.world.level.block.Block scaryLeaves =
                (net.minecraft.world.level.block.Block) (Object) ChaosPersists.MyScaryLeaves;
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            for (int x = -5; x < 60; ++x) {
                for (int z = -5; z < 70; ++z) {
                    net.minecraft.world.level.block.Block bid =
                            level.getBlockState(new net.minecraft.core.BlockPos(posX + x, posY + 4, posZ + z)).getBlock();
                    if (bid == net.minecraft.world.level.block.Blocks.AIR
                            || bid == net.minecraft.world.level.block.Blocks.OAK_LOG
                            || bid == appleLeaves
                            || bid == scaryLeaves) {
                        continue;
                    }
                    return false;
                }
            }
            ChaosPersists.MyDungeon.makeRobotLab(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4Mini(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            ChaosPersists.MyDungeon.makeMiniDungeon(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addPumpkin(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(2) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = Math.min(128, level.getMaxBuildHeight() - 1); posY > level.getMinBuildHeight() + 3; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            ChaosPersists.MyDungeon.makePumpkin(level, posX, posY + 1, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public boolean addD4CloudShark(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        int posX = 4 + chunkX + random.nextInt(8);
        int posZ = 4 + chunkZ + random.nextInt(8);
        ChaosPersists.MyDungeon.makeCloudSharkDungeon(level, posX, 150 + random.nextInt(10), posZ);
        return true;
    }

    public boolean addD4Rainbow(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        int posX = 4 + chunkX + random.nextInt(8);
        int posZ = 4 + chunkZ + random.nextInt(8);
        int posY = 70 + random.nextInt(20);
        ChaosPersists.MyDungeon.makeRainbow(level, posX, posY, posZ);
        recently_placed = 50;
        return true;
    }

    public boolean addD4GenericDungeon(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (ChaosPersists.LessLag != 0 && random.nextInt(4) != 0) {
            return false;
        }
        int posX = chunkX + random.nextInt(8);
        int posZ = chunkZ + random.nextInt(8);
        for (int posY = 20; posY > 4; --posY) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY, posZ))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                continue;
            }
            ChaosPersists.MyDungeon.makeDungeon(level, posX, posY, posZ);
            recently_placed = 50;
            return true;
        }
        return false;
    }

    public void addLavaAndWater(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(5) != 0) {
            return;
        }
        block0:
        for (int i = 0; i < 6; ++i) {
            int posX = chunkX + random.nextInt(16);
            int posZ = chunkZ + random.nextInt(16);
            for (int posY = 128; posY > 75; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()) {
                    break;
                }
                if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                        .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                net.minecraft.core.BlockPos deep =
                        new net.minecraft.core.BlockPos(posX, posY - 2, posZ);
                if (!level.getBlockState(deep).is(net.minecraft.world.level.block.Blocks.DIRT)
                        && !level.getBlockState(deep).is(net.minecraft.world.level.block.Blocks.STONE)) {
                    continue block0;
                }
                int air = 0;
                int non_air = 0;
                net.minecraft.core.BlockPos side =
                        new net.minecraft.core.BlockPos(posX + 1, posY - 1, posZ);
                if (level.getBlockState(side).isAir()) {
                    ++air;
                }
                if (level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.DIRT)
                        || level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.STONE)
                        || level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    ++non_air;
                }
                side = new net.minecraft.core.BlockPos(posX - 1, posY - 1, posZ);
                if (level.getBlockState(side).isAir()) {
                    ++air;
                }
                if (level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.DIRT)
                        || level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.STONE)
                        || level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    ++non_air;
                }
                side = new net.minecraft.core.BlockPos(posX, posY - 1, posZ + 1);
                if (level.getBlockState(side).isAir()) {
                    ++air;
                }
                if (level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.DIRT)
                        || level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.STONE)
                        || level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    ++non_air;
                }
                side = new net.minecraft.core.BlockPos(posX, posY - 1, posZ - 1);
                if (level.getBlockState(side).isAir()) {
                    ++air;
                }
                if (level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.DIRT)
                        || level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.STONE)
                        || level.getBlockState(side).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    ++non_air;
                }
                if (air == 0 || non_air == 0) {
                    continue block0;
                }
                int what = random.nextInt(2);
                if (what == 0) {
                    ChaosPersists.setBlockFast(level, posX, posY, posZ, Blocks.WATER, 0, 3);
                    ChaosPersists.setBlockFast(level, posX, posY - 1, posZ, Blocks.WATER, 0, 3);
                    ChaosPersists.setBlockFast(level, posX, posY - 2, posZ, Blocks.WATER, 0, 3);
                } else {
                    ChaosPersists.setBlockFast(level, posX, posY, posZ, Blocks.LAVA, 0, 3);
                    ChaosPersists.setBlockFast(level, posX, posY - 1, posZ, Blocks.LAVA, 0, 3);
                    ChaosPersists.setBlockFast(level, posX, posY - 2, posZ, Blocks.LAVA, 0, 3);
                }
                return;
            }
        }
    }

    public boolean addOtherTrees(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        int nc = 5;
        int count = 0;
        if (random.nextInt(30) != 0) {
            return false;
        }
        if (ChaosPersists.LessLag == 1) {
            if (random.nextInt(2) != 0) {
                return false;
            }
            nc = 4;
        }
        if (ChaosPersists.LessLag == 2) {
            if (random.nextInt(4) != 0) {
                return false;
            }
            nc = 3;
        }
        if (level.dimension().equals(ChaosPersists.getUtopiaDimensionKey())) {
            int dir = 0;
            int what = random.nextInt(2);
            block0:
            for (int i = 0; i < nc; ++i) {
                int posX = 3 + chunkX + random.nextInt(10);
                int posZ = 3 + chunkZ + random.nextInt(10);
                for (int posY = 100; posY > 50; --posY) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(pos).isAir()) {
                        break;
                    }
                    if (!level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                            .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        continue;
                    }
                    ++count;
                    if (what == 0) {
                        com.astryxion.chaospersists.util.UtopiaBigTrees.windTree(level, posX, posY - 1, posZ, dir);
                        if (count < 4) {
                            continue block0;
                        }
                        return true;
                    }
                    com.astryxion.chaospersists.util.UtopiaBigTrees.skyTree(level, posX, posY - 1, posZ);
                    if (count < 3) {
                        continue block0;
                    }
                    return true;
                }
            }
        }
        if (count > 0) {
            return true;
        }
        return false;
    }

    public boolean addKingAltar(
            net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(2000) != 1) {
            return false;
        }
        for (int i = 0; i < 8; ++i) {
            int posX = 3 + chunkX + random.nextInt(10);
            int posZ = 3 + chunkZ + random.nextInt(10);
            for (int posY = 100; posY > 50; --posY) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(posX, posY, posZ);
                if (!level.getBlockState(pos).isAir()
                        || !level.getBlockState(new net.minecraft.core.BlockPos(posX, posY - 1, posZ))
                                .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                    continue;
                }
                if (!this.quickReallyBigSpaceCheck(level, posX, posY - 1, posZ)) {
                    return false;
                }
                int altarY = posY - 1;
                if (random.nextInt(2) == 0) {
                    ChaosPersists.MyDungeon.makeKingAltar(level, posX, altarY, posZ);
                } else {
                    ChaosPersists.MyDungeon.makeQueenAltar(level, posX, altarY, posZ);
                }
                recently_placed = 100;
                return true;
            }
        }
        return false;
    }

    public void addBasiliskMaze(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
        int lowestY = 128;
        int lowestX = chunkX;
        int lowestZ = chunkZ;
        boolean found = false;
        for (int i = 0; i < 16; i += 3) {
            block1 : for (int j = 0; j < 16; j += 3) {
                int posX = chunkX + i;
                int posZ = chunkZ + j;
                for (int posY = 128; posY > 30; --posY) {
                    net.minecraft.core.BlockPos above = new net.minecraft.core.BlockPos(posX, posY + 1, posZ);
                    net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(above).isAir() || level.getBlockState(ground).isAir()) {
                        continue;
                    }
                    if (posY >= lowestY) continue block1;
                    lowestY = posY;
                    lowestX = posX;
                    lowestZ = posZ;
                    found = true;
                    continue block1;
                }
            }
        }
        if (found && lowestY > 40) {
            int mazeY = lowestY - 2;
            ChaosPersists.BMaze.buildBasiliskMaze(level, lowestX, mazeY, lowestZ);
            recently_placed = 50;
        }
    }

    public void addKyuubiDungeon(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
        int lowestY = 128;
        int lowestX = chunkX;
        int lowestZ = chunkZ;
        boolean found = false;
        for (int i = 0; i < 16; i += 3) {
            block1 : for (int j = 0; j < 16; j += 3) {
                int posX = chunkX + i;
                int posZ = chunkZ + j;
                for (int posY = 128; posY > 30; --posY) {
                    net.minecraft.core.BlockPos above = new net.minecraft.core.BlockPos(posX, posY + 1, posZ);
                    net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(posX, posY, posZ);
                    if (!level.getBlockState(above).isAir()
                            || level.getBlockState(ground).isAir()) {
                        continue;
                    }
                    if (posY >= lowestY) continue block1;
                    lowestY = posY;
                    lowestX = posX;
                    lowestZ = posZ;
                    found = true;
                    continue block1;
                }
            }
        }
        if (found && lowestY > 40) {
            int dungeonY = lowestY - 2;
            ChaosPersists.MyDungeon.makeKyuubiDungeon(level, lowestX, dungeonY, lowestZ);
            recently_placed = 50;
        }
    }

    /**
     * Preloads every chunk in a structure footprint, runs deferred {@link #generate} on each
     * chunk once, then clears terrain so multi-chunk builds are not clipped or overwritten.
     */
    public void prepareStructureFootprint(
            net.minecraft.world.level.Level level,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ) {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide() || isDuringPopulateFeature()) {
            return;
        }
        int loX = Math.min(minX, maxX);
        int hiX = Math.max(minX, maxX);
        int loY = Math.max(level.getMinBuildHeight(), Math.min(minY, maxY));
        int hiY = Math.min(level.getMaxBuildHeight() - 1, Math.max(minY, maxY));
        int loZ = Math.min(minZ, maxZ);
        int hiZ = Math.max(minZ, maxZ);
        this.ensureChunksGenerated(serverLevel, loX, loZ, hiX, hiZ);
        int minChunkX = loX >> 4;
        int maxChunkX = hiX >> 4;
        int minChunkZ = loZ >> 4;
        int maxChunkZ = hiZ >> 4;
        FLUSHING_FOR_STRUCTURE.set(true);
        try {
            for (int cx = minChunkX; cx <= maxChunkX; ++cx) {
                for (int cz = minChunkZ; cz <= maxChunkZ; ++cz) {
                    this.flushChunkWorldGenForStructure(serverLevel, cx, cz);
                }
            }
        } finally {
            FLUSHING_FOR_STRUCTURE.set(false);
        }
        ChaosPersists.clearStructureVolume(serverLevel, loX, loY, loZ, hiX, hiY, hiZ);
    }

    private void flushChunkWorldGenForStructure(ServerLevel serverLevel, int chunkX, int chunkZ) {
        PendingChunkGen pending = new PendingChunkGen(serverLevel.dimension(), chunkX, chunkZ);
        if (!PROCESSED_WORLD_GEN.add(pending)) {
            return;
        }
        SCHEDULED_CHUNK_GEN.remove(pending);
        CHUNK_GEN_QUEUE.remove(pending);
        if (serverLevel.hasChunk(chunkX, chunkZ)) {
            this.runChunkWorldGen(serverLevel, serverLevel.getChunk(chunkX, chunkZ));
        }
    }

    /**
     * Forces generation/loading of every chunk touched by large multi-chunk structures so
     * later terrain generation does not overwrite pre-placed blocks.
     */
    private void ensureChunksGenerated(
            net.minecraft.world.level.Level level, int minX, int minZ, int maxX, int maxZ) {
        if (level == null || isDuringPopulateFeature()) {
            return;
        }
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; ++cx) {
            for (int cz = minChunkZ; cz <= maxChunkZ; ++cz) {
                level.getChunk(new net.minecraft.core.BlockPos((cx << 4) + 8, 0, (cz << 4) + 8));
            }
        }
    }

    private boolean quickSpaceCheck(net.minecraft.world.level.Level level, int posX, int posY, int posZ) {
        for (int i = -2; i < 10; ++i) {
            for (int k = -2; k < 10; ++k) {
                if (level.getBlockState(new net.minecraft.core.BlockPos(posX + i, posY + 4, posZ + k)).isAir()) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private boolean quickBigSpaceCheck(net.minecraft.world.level.Level level, int posX, int posY, int posZ) {
        for (int i = -5; i < 25; ++i) {
            for (int k = -5; k < 25; ++k) {
                if (level.getBlockState(new net.minecraft.core.BlockPos(posX + i, posY + 8, posZ + k)).isAir()) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private boolean quickReallyBigSpaceCheck(net.minecraft.world.level.Level level, int posX, int posY, int posZ) {
        for (int i = -5; i < 55; ++i) {
            for (int k = -5; k < 55; ++k) {
                if (level.getBlockState(new net.minecraft.core.BlockPos(posX + i, posY + 8, posZ + k)).isAir()) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private boolean D4BigSpaceCheck(net.minecraft.world.level.Level level, int posX, int posY, int posZ) {
        Block appleLeaves = ChaosPersists.MyAppleLeaves;
        Block scaryLeaves = ChaosPersists.MyScaryLeaves;
        for (int i = -25; i < 40; ++i) {
            for (int k = -25; k < 30; ++k) {
                Block bid = level.getBlockState(new net.minecraft.core.BlockPos(posX + i, posY + 4, posZ + k))
                        .getBlock();
                if (bid == Blocks.AIR || bid == Blocks.OAK_LOG || bid == appleLeaves || bid == scaryLeaves) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private Entity spawnCreature(Level level, String par1, double par2, double par4, double par6) {
        Entity var8 = null;
        net.minecraft.resources.ResourceLocation requested;
        if (par1 != null && par1.contains(":")) {
            String[] parts = par1.split(":", 2);
            requested = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        } else {
            String normalizedPath = (par1 == null ? "" : par1.trim().toLowerCase(Locale.ROOT).replace(' ', '_'));
            requested = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chaospersists", normalizedPath);
        }
        requested = SpawnerFixHelper.normalizeEntityLookupId(requested);
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(requested);
        if (entityType != null) {
            var8 = entityType.create(level);
        }
        if (var8 != null) {
            if (par2 > 0.0) {
                par2 += 0.5;
            }
            if (par2 < 0.0) {
                par2 -= 0.5;
            }
            if (par6 > 0.0) {
                par6 += 0.5;
            }
            if (par6 < 0.0) {
                par6 -= 0.5;
            }
            var8.moveTo(par2, par4 + 0.01, par6, level.getRandom().nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(var8);
            if (var8 instanceof Mob mob) {
                MyUtils.playAmbientSound(mob);
            }
        }
        return var8;
    }
}

