package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ItemThunderStaff extends Item {

    private int ticker = 50;

    public ItemThunderStaff(int i) {
        super(new Properties().stacksTo(1).durability(50));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getMaxDamage() - stack.getDamageValue() <= 1) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            ThunderBolt lb =
                    new ThunderBolt(ChaosPersists.ENTITY_TYPE_THUNDER_BOLT.get(), player, level);
            Vec3 look = player.getLookAngle();
            double spawnDist = 0.65;
            double px = player.getX() + look.x * spawnDist;
            double py = player.getY() + player.getEyeHeight() + look.y * spawnDist;
            double pz = player.getZ() + look.z * spawnDist;
            lb.setPos(px, py, pz);
            double speed = 1.85;
            lb.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
            level.addFreshEntity(lb);
        }

        player.swing(hand);

        player.push(
                Mth.cos((player.getYRot() - 90.0f) * Mth.DEG_TO_RAD) * 0.5,
                0.15,
                Mth.sin((player.getYRot() - 90.0f) * Mth.DEG_TO_RAD) * 0.5);

        stack.hurtAndBreak(1, player, e -> e.broadcastBreakEvent(hand));

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isRaining() && level.isThundering()) {
            if (this.ticker > 0) {
                --this.ticker;
            }
            if (this.ticker <= 0 && stack.getDamageValue() > 0) {
                stack.setDamageValue(stack.getDamageValue() - 1);
                this.ticker = 50;
            }
        }
    }

    public String getMaterialName() {
        return "Unknown";
    }
}
