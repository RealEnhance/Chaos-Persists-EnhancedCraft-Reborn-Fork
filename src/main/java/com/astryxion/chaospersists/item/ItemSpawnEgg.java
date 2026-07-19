package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.util.MyUtils;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.entity.Godzilla;
import com.astryxion.chaospersists.entity.PitchBlack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemSpawnEgg extends Item {
    public int my_id = 0;

    public ItemSpawnEgg(int i, int j) {
        super(new Properties().stacksTo(64));
        this.my_id = j;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        double spawnX = pos.getX() + 0.5;
        double spawnY = pos.getY() + 1.0;
        double spawnZ = pos.getZ() + 0.5;
        if (doSpawn(context.getItemInHand(), context.getPlayer(), level, spawnX, spawnY, spawnZ)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    private boolean doSpawn(ItemStack stack, Player player, Level level, double spawnX, double spawnY, double spawnZ) {
        Entity ent = ItemSpawnEgg.spawn_something(this.my_id, level, spawnX, spawnY, spawnZ);
        if (ent == null) {
            return false;
        }
        if (ent instanceof PitchBlack pitchBlack) {
            pitchBlack.setSpawnedFromEgg();
        }
        if (ent instanceof Godzilla) {
            ChaosPersists.godzilla_has_spawned = 1;
        }
        if (ent instanceof LivingEntity living && stack.hasCustomHoverName()) {
            living.setCustomName(stack.getHoverName());
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    public static Entity spawn_something(int id, Level level, double d0, double d1, double d2) {
        int entityID = 0;
        int skelly_type = 0;
        String name = null;
        switch (id) {
            case 192: {
                skelly_type = 1;
                entityID = 51;
                break;
            }
            case 193: {
                entityID = 63;
                break;
            }
            case 194: {
                entityID = 97;
                break;
            }
            case 195: {
                entityID = 99;
                break;
            }
            case 196: {
                entityID = 64;
                break;
            }
            case 197: {
                name = "girlfriend";
                break;
            }
            case 198: {
                name = "apple_cow";
                break;
            }
            case 363: {
                name = "crystal_apple_cow";
                break;
            }
            case 199: {
                name = "golden_apple_cow";
                break;
            }
            case 200: {
                name = "enchanted_golden_apple_cow";
                break;
            }
            case 202: {
                name = "alosaurus";
                break;
            }
            case 203: {
                name = "cryolophosaurus";
                break;
            }
            case 204: {
                name = "camarasaurus";
                break;
            }
            case 205: {
                name = "velocity_raptor";
                break;
            }
            case 206: {
                name = "hydrolisc";
                break;
            }
            case 207: {
                name = "basilisk";
                break;
            }
            case 201: {
                name = "mothra";
                break;
            }
            case 221: {
                name = "dragonfly";
                break;
            }
            case 223: {
                name = "emperor_scorpion";
                break;
            }
            case 225: {
                name = "scorpion";
                break;
            }
            case 227: {
                name = "cave_fisher";
                break;
            }
            case 229: {
                name = "baby_dragon";
                break;
            }
            case 231: {
                name = "baryonyx";
                break;
            }
            case 233: {
                name = "gamma_metroid";
                break;
            }
            case 235: {
                name = "bird";
                break;
            }
            case 237: {
                name = "kyuubi";
                break;
            }
            case 239: {
                name = "alien";
                break;
            }
            case 241: {
                name = "attack_squid";
                break;
            }
            case 243: {
                name = "water_dragon";
                break;
            }
            case 245: {
                name = "the_kraken";
                break;
            }
            case 247: {
                name = "lizard";
                break;
            }
            case 249: {
                name = "cephadrome";
                break;
            }
            case 251: {
                name = "dragon";
                break;
            }
            case 254: {
                name = "bee";
                break;
            }
            case 262: {
                name = "jumpy_bug";
                break;
            }
            case 263: {
                name = "spit_bug";
                break;
            }
            case 264: {
                name = "stink_bug";
                break;
            }
            case 265: {
                name = "ostrich";
                break;
            }
            case 266: {
                name = "gazelle";
                break;
            }
            case 267: {
                name = "chipmunk";
                break;
            }
            case 274: {
                name = "creeping_horror";
                break;
            }
            case 275: {
                name = "terrible_terror";
                break;
            }
            case 276: {
                name = "cliff_racer";
                break;
            }
            case 277: {
                name = "triffid";
                break;
            }
            case 278: {
                name = "nightmare";
                break;
            }
            case 279: {
                name = "lurking_terror";
                break;
            }
            case 288: {
                name = "small_worm";
                break;
            }
            case 289: {
                name = "medium_worm";
                break;
            }
            case 290: {
                name = "large_worm";
                break;
            }
            case 291: {
                name = "cassowary";
                break;
            }
            case 292: {
                name = "cloud_shark";
                break;
            }
            case 293: {
                name = "gold_fish";
                break;
            }
            case 294: {
                name = "leaf_monster";
                break;
            }
            case 295: {
                name = "tshirt";
                break;
            }
            case 280: {
                name = "mobzilla";
                break;
            }
            case 298: {
                name = "ender_knight";
                break;
            }
            case 299: {
                name = "ender_reaper";
                break;
            }
            case 301: {
                name = "beaver";
                break;
            }
            case 306: {
                name = "dungeon_beast";
                break;
            }
            case 303: {
                name = "vortex";
                break;
            }
            case 302: {
                name = "rotator";
                break;
            }
            case 304: {
                name = "peacock";
                break;
            }
            case 305: {
                name = "fairy";
                break;
            }
            case 307: {
                name = "rat";
                break;
            }
            case 308: {
                name = "flounder";
                break;
            }
            case 309: {
                name = "whale";
                break;
            }
            case 310: {
                name = "irukandji";
                break;
            }
            case 311: {
                name = "skate";
                break;
            }
            case 312: {
                name = "crystal_urchin";
                break;
            }
            case 324: {
                name = "bomb_omb";
                break;
            }
            case 325: {
                name = "robo_pounder";
                break;
            }
            case 326: {
                name = "robo_gunner";
                break;
            }
            case 327: {
                name = "robo_warrior";
                break;
            }
            case 328: {
                name = "ghost";
                break;
            }
            case 329: {
                name = "ghost_pumpkin_skelly";
                break;
            }
            case 330: {
                name = "ant";
                break;
            }
            case 331: {
                name = "red_ant";
                break;
            }
            case 332: {
                name = "rainbow_ant";
                break;
            }
            case 333: {
                name = "unstable_ant";
                break;
            }
            case 334: {
                name = "termite";
                break;
            }
            case 335: {
                name = "butterfly";
                break;
            }
            case 336: {
                name = "moth";
                break;
            }
            case 337: {
                name = "mosquito";
                break;
            }
            case 338: {
                name = "firefly";
                break;
            }
            case 339: {
                name = "trex";
                break;
            }
            case 340: {
                name = "hercules_beetle";
                break;
            }
            case 341: {
                name = "mantis";
                break;
            }
            case 342: {
                name = "stinky";
                break;
            }
            case 343: {
                name = "robo_sniper";
                break;
            }
            case 344: {
                name = "coin";
                break;
            }
            case 349: {
                name = "boyfriend";
                break;
            }
            case 350: {
                name = "the_king";
                break;
            }
            case 366: {
                name = "the_queen";
                break;
            }
            case 351: {
                name = "the_prince";
                break;
            }
            case 352: {
                name = "easter_bunny";
                break;
            }
            case 353: {
                name = "molenoid";
                break;
            }
            case 354: {
                name = "sea_monster";
                break;
            }
            case 355: {
                name = "sea_viper";
                break;
            }
            case 356: {
                name = "caterkiller";
                break;
            }
            case 358: {
                name = "leonopteryx";
                break;
            }
            case 360: {
                name = "hammerhead";
                break;
            }
            case 362: {
                name = "rubber_ducky";
                break;
            }
            case 365: {
                name = "criminal";
                break;
            }
            case 367: {
                name = "brutalfly";
                break;
            }
            case 368: {
                name = "nastysaurus";
                break;
            }
            case 369: {
                name = "pointysaurus";
                break;
            }
            case 370: {
                name = "cricket";
                break;
            }
            case 371: {
                name = "the_princess";
                break;
            }
            case 372: {
                name = "frog";
                break;
            }
            case 378: {
                name = "jeffery";
                break;
            }
            case 379: {
                name = "robot_red_ant";
                break;
            }
            case 380: {
                name = "robot_spider";
                break;
            }
            case 381: {
                name = "spider_driver";
                break;
            }
            case 383: {
                name = "crab";
                break;
            }
        }
        Entity ent = null;
        if (entityID == 51 && skelly_type != 0) {
            ent = spawnVanillaCreature(level, EntityType.WITHER_SKELETON, d0, d1, d2);
        } else if (entityID != 0 || name != null) {
            ent = spawnCreature(level, entityID, name, d0, d1, d2);
        }
        return ent;
    }

    public static Entity spawnCreature(Level level, int entityId, String name, double x, double y, double z) {
        return spawnCreature(level, entityId, name, 0, x, y, z);
    }

    public static Entity spawnCreature(
            Level level, int entityId, String name, int skellyType, double x, double y, double z) {
        EntityType<?> type = null;
        if (name != null) {
            ResourceLocation loc = legacySpawnNameToRegistry(name);
            if (loc != null) {
                type = ForgeRegistries.ENTITY_TYPES.getValue(loc);
            }
        } else if (entityId != 0) {
            if (entityId == 51 && skellyType != 0) {
                type = EntityType.WITHER_SKELETON;
            } else {
                type = legacyVanillaEntityType(entityId);
            }
        }
        Entity entity = spawnEntityType(level, type, x, y, z);
        if (entity instanceof Mob mob && level instanceof ServerLevel serverLevel) {
            if (entityId == 100 || entityId == 120) {
                BlockPos spawnPos = BlockPos.containing(x, y, z);
                mob.finalizeSpawn(
                        serverLevel,
                        serverLevel.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.SPAWN_EGG,
                        null,
                        null);
            }
        }
        return entity;
    }

    private static Entity spawnVanillaCreature(Level level, EntityType<?> type, double x, double y, double z) {
        return spawnEntityType(level, type, x, y, z);
    }

    private static Entity spawnEntityType(Level level, EntityType<?> type, double x, double y, double z) {
        if (type == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = type.create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        boolean restorePhysics = false;
        if (entity instanceof Godzilla) {
            restorePhysics = !entity.noPhysics;
            entity.noPhysics = true;
        }
        if (entity instanceof Mob mob) {
            BlockPos spawnPos = BlockPos.containing(x, y, z);
            mob.finalizeSpawn(
                    serverLevel,
                    serverLevel.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.SPAWN_EGG,
                    null,
                    null);
            mob.setPersistenceRequired();
            MyUtils.playAmbientSound(mob);
        }
        if (!serverLevel.addFreshEntity(entity)) {
            return null;
        }
        if (restorePhysics) {
            entity.noPhysics = false;
        }
        return entity;
    }

    /** 1.12 numeric entity ids used by {@link CritterCage} filled cages (EntityList.getClassFromID). */
    private static EntityType<?> legacyVanillaEntityType(int legacyId) {
        return switch (legacyId) {
            case 50 -> EntityType.CREEPER;
            case 51 -> EntityType.SKELETON;
            case 52 -> EntityType.SPIDER;
            case 54 -> EntityType.ZOMBIE;
            case 55 -> EntityType.SLIME;
            case 56 -> EntityType.GHAST;
            case 57 -> EntityType.ZOMBIFIED_PIGLIN;
            case 58 -> EntityType.ENDERMAN;
            case 59 -> EntityType.CAVE_SPIDER;
            case 60 -> EntityType.SILVERFISH;
            case 61 -> EntityType.BLAZE;
            case 62 -> EntityType.MAGMA_CUBE;
            case 63 -> EntityType.ENDER_DRAGON;
            case 64 -> EntityType.WITHER;
            case 65 -> EntityType.BAT;
            case 66 -> EntityType.WITCH;
            case 90 -> EntityType.PIG;
            case 91 -> EntityType.SHEEP;
            case 92 -> EntityType.COW;
            case 93 -> EntityType.CHICKEN;
            case 94 -> EntityType.SQUID;
            case 95 -> EntityType.WOLF;
            case 96 -> EntityType.MOOSHROOM;
            case 97 -> EntityType.SNOW_GOLEM;
            case 98 -> EntityType.CAT;
            case 99 -> EntityType.IRON_GOLEM;
            case 100 -> EntityType.HORSE;
            case 120 -> EntityType.VILLAGER;
            default -> null;
        };
    }

    private static ResourceLocation legacySpawnNameToRegistry(String legacyName) {
        if (legacyName == null) {
            return null;
        }
        switch (legacyName) {
            case "Rat":
                return ResourceLocation.fromNamespaceAndPath("chaospersists", "rat");
            case "Fairy":
                return ResourceLocation.fromNamespaceAndPath("chaospersists", "fairy");
            case "Red Ant":
                return ResourceLocation.fromNamespaceAndPath("chaospersists", "red_ant");
            case "Termite":
                return ResourceLocation.fromNamespaceAndPath("chaospersists", "termite");
            default:
                return ResourceLocation.fromNamespaceAndPath(
                        "chaospersists", legacyName.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'));
        }
    }
}

