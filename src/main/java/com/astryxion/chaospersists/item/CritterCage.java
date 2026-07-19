package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.entity.EntityCage;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class CritterCage extends Item {
    public int cage_id = 0;

    public CritterCage(int i, int j) {
        super(new Properties().stacksTo(16));
        this.cage_id = j;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            EntityCage cage =
                    new EntityCage(ChaosPersists.ENTITY_TYPE_CAGE.get(), level, player, this.cage_id);
            cage.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(cage);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            EntityCage cage =
                    new EntityCage(ChaosPersists.ENTITY_TYPE_CAGE.get(), level, player, this.cage_id);
            cage.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(cage);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /** Spawns the mob for the given filled cage_id at the given position. Used by EntityCage.onHit for release. */
    public static Entity spawnMobForCage(Level level, int cageId, double x, double y, double z) {
        int entityID = 0;
        int skelly_type = 0;
        String name = null;
        switch (cageId) {
            case 161: {
                entityID = 52;
                break;
            }
            case 162: {
                entityID = 65;
                break;
            }
            case 163: {
                entityID = 92;
                break;
            }
            case 164: {
                entityID = 90;
                break;
            }
            case 165: {
                entityID = 94;
                break;
            }
            case 166: {
                entityID = 93;
                break;
            }
            case 167: {
                entityID = 50;
                break;
            }
            case 188: {
                skelly_type = 1;
            }
            case 168: {
                entityID = 51;
                break;
            }
            case 169: {
                entityID = 54;
                break;
            }
            case 170: {
                entityID = 55;
                break;
            }
            case 171: {
                entityID = 56;
                break;
            }
            case 172: {
                entityID = 57;
                break;
            }
            case 173: {
                entityID = 58;
                break;
            }
            case 174: {
                entityID = 59;
                break;
            }
            case 175: {
                entityID = 60;
                break;
            }
            case 176: {
                entityID = 62;
                break;
            }
            case 177: {
                entityID = 66;
                break;
            }
            case 178: {
                entityID = 91;
                break;
            }
            case 179: {
                entityID = 95;
                break;
            }
            case 180: {
                entityID = 96;
                break;
            }
            case 181: {
                entityID = 98;
                break;
            }
            case 182: {
                entityID = 61;
                break;
            }
            case 184: {
                entityID = 63;
                break;
            }
            case 185: {
                entityID = 97;
                break;
            }
            case 186: {
                entityID = 99;
                break;
            }
            case 187: {
                entityID = 64;
                break;
            }
            case 253: {
                entityID = 100;
                break;
            }
            case 217: {
                entityID = 120;
                break;
            }
            case 183: {
                name = "Girlfriend";
                break;
            }
            case 215: {
                name = "Boyfriend";
                break;
            }
            case 189: {
                name = "Apple Cow";
                break;
            }
            case 190: {
                name = "Golden Apple Cow";
                break;
            }
            case 191: {
                name = "Enchanted Golden Apple Cow";
                break;
            }
            case 208: {
                name = "Mothra";
                break;
            }
            case 209: {
                name = "Alosaurus";
                break;
            }
            case 210: {
                name = "Cryolophosaurus";
                break;
            }
            case 211: {
                name = "Camarasaurus";
                break;
            }
            case 212: {
                name = "Velocity Raptor";
                break;
            }
            case 213: {
                name = "Hydrolisc";
                break;
            }
            case 214: {
                name = "Basilisk";
                break;
            }
            case 220: {
                name = "Dragonfly";
                break;
            }
            case 222: {
                name = "Emperor Scorpion";
                break;
            }
            case 224: {
                name = "Scorpion";
                break;
            }
            case 226: {
                name = "CaveFisher";
                break;
            }
            case 228: {
                name = "Baby Dragon";
                break;
            }
            case 230: {
                name = "Baryonyx";
                break;
            }
            case 232: {
                name = "WTF?";
                break;
            }
            case 234: {
                name = "Bird";
                break;
            }
            case 236: {
                name = "Kyuubi";
                break;
            }
            case 238: {
                name = "Alien";
                break;
            }
            case 240: {
                name = "Attack Squid";
                break;
            }
            case 242: {
                name = "Water Dragon";
                break;
            }
            case 244: {
                name = "The Kraken";
                break;
            }
            case 246: {
                name = "Lizard";
                break;
            }
            case 248: {
                name = "Cephadrome";
                break;
            }
            case 250: {
                name = "Dragon";
                break;
            }
            case 252: {
                name = "Bee";
                break;
            }
            case 255: {
                name = "Firefly";
                break;
            }
            case 256: {
                name = "Chipmunk";
                break;
            }
            case 257: {
                name = "Gazelle";
                break;
            }
            case 258: {
                name = "Ostrich";
                break;
            }
            case 259: {
                name = "Jumpy Bug";
                break;
            }
            case 260: {
                name = "Spit Bug";
                break;
            }
            case 261: {
                name = "Stink Bug";
                break;
            }
            case 268: {
                name = "Creeping Horror";
                break;
            }
            case 269: {
                name = "Terrible Terror";
                break;
            }
            case 270: {
                name = "Cliff Racer";
                break;
            }
            case 271: {
                name = "Triffid";
                break;
            }
            case 272: {
                name = "Nightmare";
                break;
            }
            case 273: {
                name = "Lurking Terror";
                break;
            }
            case 281: {
                name = "Small Worm";
                break;
            }
            case 283: {
                name = "Large Worm";
                break;
            }
            case 282: {
                name = "Medium Worm";
                break;
            }
            case 284: {
                name = "Cassowary";
                break;
            }
            case 285: {
                name = "Cloud Shark";
                break;
            }
            case 286: {
                name = "Gold Fish";
                break;
            }
            case 287: {
                name = "Leaf Monster";
                break;
            }
            case 296: {
                name = "Ender Knight";
                break;
            }
            case 297: {
                name = "Ender Reaper";
                break;
            }
            case 300: {
                name = "Beaver";
                break;
            }
            case 323: {
                name = "Crystal Urchin";
                break;
            }
            case 319: {
                name = "Flounder";
                break;
            }
            case 322: {
                name = "Skate";
                break;
            }
            case 313: {
                name = "Rotator";
                break;
            }
            case 315: {
                name = "Peacock";
                break;
            }
            case 316: {
                name = "Fairy";
                break;
            }
            case 317: {
                name = "Dungeon Beast";
                break;
            }
            case 314: {
                name = "Vortex";
                break;
            }
            case 318: {
                name = "Rat";
                break;
            }
            case 320: {
                name = "Whale";
                break;
            }
            case 321: {
                name = "Irukandji";
                break;
            }
            case 345: {
                name = "T. Rex";
                break;
            }
            case 346: {
                name = "Hercules Beetle";
                break;
            }
            case 347: {
                name = "Mantis";
                break;
            }
            case 348: {
                name = "Stinky";
                break;
            }
            case 150: {
                name = "Easter Bunny";
                break;
            }
            case 151: {
                name = "CaterKiller";
                break;
            }
            case 152: {
                name = "Molenoid";
                break;
            }
            case 153: {
                name = "Sea Monster";
                break;
            }
            case 154: {
                name = "Sea Viper";
                break;
            }
            case 357: {
                name = "Leonopteryx";
                break;
            }
            case 359: {
                name = "Hammerhead";
                break;
            }
            case 361: {
                name = "Rubber Ducky";
                break;
            }
            case 216: {
                name = "Crystal Apple Cow";
                break;
            }
            case 218: {
                name = "Criminal";
                break;
            }
            case 373: {
                name = "Brutalfly";
                break;
            }
            case 374: {
                name = "Nastysaurus";
                break;
            }
            case 375: {
                name = "Pointysaurus";
                break;
            }
            case 376: {
                name = "Cricket";
                break;
            }
            case 377: {
                name = "Frog";
                break;
            }
            case 382: {
                name = "Spider Driver";
                break;
            }
            case 384: {
                name = "Crab";
                break;
            }
            default:
                return null;
        }
        if (name != null) {
            if ("WTF?".equals(name)) {
                name = "gamma_metroid";
            } else if ("CaveFisher".equals(name)) {
                name = "cave_fisher";
            } else if ("T. Rex".equals(name)) {
                name = "trex";
            }
        }
        return ItemSpawnEgg.spawnCreature(level, entityID, name, skelly_type, x + 0.5, y + 1.1, z + 0.5);
    }
}
