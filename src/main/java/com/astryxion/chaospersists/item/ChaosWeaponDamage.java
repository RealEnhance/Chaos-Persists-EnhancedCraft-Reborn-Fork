package com.astryxion.chaospersists.item;

import net.minecraft.world.item.Tier;

/**
 * Converts official OreSpawn wiki attack values into 1.20.1 item attack modifiers.
 * In 1.20.1, displayed attack damage is {@code modifier + tier.getAttackDamageBonus() + 1}.
 */
public final class ChaosWeaponDamage {

    private static final float ATTACK_OFFSET = 1.0f;

    /** 1.7.10 {@link net.minecraft.item.ItemSword} used tier material damage + 4. */
    public static final int TIER_SWORD_MODIFIER = 3;

    /** 1.7.10 tools without a fixed override used tier material damage only. */
    public static final int TIER_TOOL_MODIFIER = -1;
    public static final float TIER_TOOL_MODIFIER_F = -1.0f;

    private ChaosWeaponDamage() {}

    public static int modifierFor(Tier tier, float legacyDamage) {
        return Math.round(legacyDamage - tier.getAttackDamageBonus() - ATTACK_OFFSET);
    }
}
