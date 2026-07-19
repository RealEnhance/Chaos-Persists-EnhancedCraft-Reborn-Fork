package com.astryxion.chaospersists.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class RubySword extends SwordItem {
    private static final int WEAPON_DAMAGE = 20;

    public RubySword(Tier tier) {
        super(
                tier,
                ChaosWeaponDamage.modifierFor(tier, WEAPON_DAMAGE),
                -2.4f,
                new Properties().stacksTo(1).durability(1500));
    }

    public String getMaterialName() {
        return "Ruby";
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 4000;
    }
}
