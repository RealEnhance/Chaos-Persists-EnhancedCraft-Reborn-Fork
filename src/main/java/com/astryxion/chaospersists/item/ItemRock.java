package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.util.MyUtils;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.entity.EntityThrownRock;
import com.astryxion.chaospersists.entity.RockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public class ItemRock extends Item {
    public ItemRock(int i) {
        super(new Item.Properties().stacksTo(64));
    }

    private int rockTypeForStack(ItemStack stack) {
        Item it = stack.getItem();
        if (it == ChaosPersists.MySmallRock) {
            return 1;
        }
        if (it == ChaosPersists.MyRock) {
            return 2;
        }
        if (it == ChaosPersists.MyRedRock) {
            return 3;
        }
        if (it == ChaosPersists.MyGreenRock) {
            return 4;
        }
        if (it == ChaosPersists.MyBlueRock) {
            return 5;
        }
        if (it == ChaosPersists.MyPurpleRock) {
            return 6;
        }
        if (it == ChaosPersists.MySpikeyRock) {
            return 7;
        }
        if (it == ChaosPersists.MyTNTRock) {
            return 8;
        }
        if (it == ChaosPersists.MyCrystalRedRock) {
            return 9;
        }
        if (it == ChaosPersists.MyCrystalGreenRock) {
            return 10;
        }
        if (it == ChaosPersists.MyCrystalBlueRock) {
            return 11;
        }
        if (it == ChaosPersists.MyCrystalTNTRock) {
            return 12;
        }
        return 0;
    }

    private void applyRockTypeToMob(Entity entity, ItemStack stack) {
        int t = rockTypeForStack(stack);
        if (t == 0) {
            return;
        }
        if (entity instanceof RockBase rock) {
            rock.placeRock(t);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int type = rockTypeForStack(stack);
        if (type == 0) {
            return InteractionResultHolder.pass(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL,
                0.5f,
                0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClientSide) {
            EntityThrownRock rock =
                    new EntityThrownRock(ChaosPersists.ENTITY_TYPE_THROWN_ROCK.get(), player, world, type);
            rock.setRockType(type);
            rock.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 1.0f);
            world.addFreshEntity(rock);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        int x = pos.getX();
        int z = pos.getZ();
        if (x < 0) {
            ++x;
        }
        if (z < 0) {
            ++z;
        }
        if (!world.isClientSide) {
            Entity e = spawnPlacedRock(world, x, pos.getY() + 1.01, z);
            if (e != null) {
                applyRockTypeToMob(e, stack);
            }
        }
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private Entity spawnPlacedRock(Level world, double par2, double par4, double par6) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = ChaosPersists.ENTITY_TYPE_ROCK.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
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
        entity.moveTo(par2, par4 + 0.01, par6, world.getRandom().nextFloat() * 360.0f, 0.0f);
        serverLevel.addFreshEntity(entity);
        if (entity instanceof LivingEntity living) {
            MyUtils.playAmbientSound(living);
        }
        return entity;
    }
}
