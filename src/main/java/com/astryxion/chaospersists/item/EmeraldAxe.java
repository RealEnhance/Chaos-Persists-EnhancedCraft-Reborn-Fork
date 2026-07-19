package com.astryxion.chaospersists.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tier;

public class EmeraldAxe extends AxeItem {
    private static final float WEAPON_DAMAGE = 9.0f;

    public EmeraldAxe(Tier tier) {
        super(
                tier,
                ChaosWeaponDamage.modifierFor(tier, WEAPON_DAMAGE),
                -3.0f,
                new Properties().stacksTo(1).durability(1300));
    }

    public String getMaterialName() {
        return "Emerald";
    }
}
