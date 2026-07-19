package com.astryxion.chaospersists.item;

import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;

public class RubyShovel extends ShovelItem {
    private static final float WEAPON_DAMAGE = 17.0f;

    public RubyShovel(Tier tier) {
        super(
                tier,
                ChaosWeaponDamage.modifierFor(tier, WEAPON_DAMAGE),
                -3.0f,
                new Properties().stacksTo(1).durability(1500));
    }

    public String getMaterialName() {
        return "Ruby";
    }
}
