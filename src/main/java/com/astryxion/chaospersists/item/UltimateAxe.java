package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.entity.Boyfriend;
import com.astryxion.chaospersists.entity.Girlfriend;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public class UltimateAxe extends AxeItem {
    private static final int WEAPON_DAMAGE = 39;

    public UltimateAxe(Tier tier) {
        super(
                tier,
                ChaosWeaponDamage.modifierFor(tier, WEAPON_DAMAGE),
                -3.0f,
                new Properties().stacksTo(1).durability(3000));
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
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, stack) <= 0) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Girlfriend || target instanceof Player) {
            if (attacker instanceof Player player) {
                target.hurt(player.damageSources().playerAttack(player), 1.0f);
            } else {
                target.hurt(target.damageSources().generic(), 1.0f);
            }
            stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            return true;
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity != null && ChaosPersists.ultimate_sword_pvp == 0) {
            if (entity instanceof Player
                    || entity instanceof Girlfriend
                    || entity instanceof Boyfriend
                    || (entity instanceof TamableAnimal t && t.isTame())) {
                return true;
            }
        }
        return false;
    }

    public String getMaterialName() {
        return "Uranium/Titanium";
    }
}
