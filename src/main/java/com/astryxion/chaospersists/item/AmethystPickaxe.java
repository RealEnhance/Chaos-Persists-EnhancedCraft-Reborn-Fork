package com.astryxion.chaospersists.item;

import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;

public class AmethystPickaxe extends PickaxeItem {
    private static final int WEAPON_DAMAGE = 13;

    public AmethystPickaxe(Tier tier) {
        super(
                tier,
                ChaosWeaponDamage.modifierFor(tier, WEAPON_DAMAGE),
                -2.8f,
                new Properties().stacksTo(1).durability(2000));
    }

    public String getMaterialName() {
        return "Amethyst";
    }
}
