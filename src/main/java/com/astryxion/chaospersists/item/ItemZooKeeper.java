package com.astryxion.chaospersists.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * OreSpawn 1.7.10 "ZooKeeper Shard": left-click a mob with it to run persistence
 * (mob no longer despawns). Same particles/sound as the original; uses one durability (max damage 1, takes 2 "damage steps").
 */
public class ItemZooKeeper extends Item {

    public ItemZooKeeper(int i) {
        super(new Properties().durability(1));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity == null) {
            return false;
        }

        playEffects(player, entity);

        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        if (!player.level().isClientSide) {
            if (living instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            stack.hurtAndBreak(
                    2,
                    player,
                    p -> p.broadcastBreakEvent(
                            player.getMainHandItem() == stack
                                    ? InteractionHand.MAIN_HAND
                                    : InteractionHand.OFF_HAND));
            clearSlotIfBroken(player, stack);
        }

        return true;
    }

    private static void playEffects(Player player, Entity entity) {
        Level level = player.level();
        for (int i = 0; i < 8; ++i) {
            float f1 = level.random.nextFloat() * 3.0f - level.random.nextFloat() * 3.0f;
            float f2 = 0.25f + level.random.nextFloat() * 2.0f;
            float f3 = level.random.nextFloat() * 3.0f - level.random.nextFloat() * 3.0f;
            level.addParticle(
                    ParticleTypes.SMOKE,
                    entity.getX() + f1,
                    entity.getY() + f2,
                    entity.getZ() + f3,
                    0.0,
                    0.0,
                    0.0);
            f1 = level.random.nextFloat() * 3.0f - level.random.nextFloat() * 3.0f;
            f2 = 0.25f + level.random.nextFloat() * 2.0f;
            f3 = level.random.nextFloat() * 3.0f - level.random.nextFloat() * 3.0f;
            level.addParticle(
                    ParticleTypes.EXPLOSION,
                    entity.getX() + f1,
                    entity.getY() + f2,
                    entity.getZ() + f3,
                    0.0,
                    0.0,
                    0.0);
            f1 = level.random.nextFloat() * 3.0f - level.random.nextFloat() * 3.0f;
            f2 = 0.25f + level.random.nextFloat() * 2.0f;
            f3 = level.random.nextFloat() * 3.0f - level.random.nextFloat() * 3.0f;
            level.addParticle(
                    new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.5f), 1.0f),
                    entity.getX() + f1,
                    entity.getY() + f2,
                    entity.getZ() + f3,
                    0.0,
                    0.0,
                    0.0);
        }
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                0.5f,
                1.5f);
    }

    private static void clearSlotIfBroken(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            return;
        }
        if (player.getMainHandItem() == stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } else if (player.getOffhandItem() == stack) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        } else {
            player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
        }
    }
}
