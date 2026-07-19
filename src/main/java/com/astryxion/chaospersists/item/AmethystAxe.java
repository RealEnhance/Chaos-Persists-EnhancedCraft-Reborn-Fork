package com.astryxion.chaospersists.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tier;

public class AmethystAxe extends AxeItem {
    private static final float WEAPON_DAMAGE = 14.0f;

    public AmethystAxe(Tier tier) {
        super(
                tier,
                ChaosWeaponDamage.modifierFor(tier, WEAPON_DAMAGE),
                -3.0f,
                new Properties().stacksTo(1).durability(2000));
    }

    public String getMaterialName() {
        return "Amethyst";
    }
}
