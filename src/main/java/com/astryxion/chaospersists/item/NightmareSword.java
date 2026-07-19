package com.astryxion.chaospersists.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public class NightmareSword extends SwordItem {
    private static final int WEAPON_DAMAGE = 20;

    public NightmareSword(Tier tier) {
        super(tier, ChaosWeaponDamage.modifierFor(tier, WEAPON_DAMAGE), -2.4f, new Properties().stacksTo(1).durability(1200));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        ensureEnchantments(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ensureEnchantments(stack);
    }

    private void ensureEnchantments(ItemStack stack) {
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, stack) <= 0) {
            stack.enchant(Enchantments.SHARPNESS, 1);
            stack.enchant(Enchantments.KNOCKBACK, 3);
            stack.enchant(Enchantments.FIRE_ASPECT, 1);
        }
    }

    public String getMaterialName() {
        return "Uranium/Titanium";
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 5000;
    }
}
