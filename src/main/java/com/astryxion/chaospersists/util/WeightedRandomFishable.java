package com.astryxion.chaospersists.util;

import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.item.ItemStack;

/**
 * 1.12.2 compat: replaces removed net.minecraft.util.WeightedRandomFishable for fishing loot.
 */
public class WeightedRandomFishable implements WeightedEntry {
    private final ItemStack stack;
    private final int weight;
    private float chance = 1.0f;
    private boolean treasure = false;

    public WeightedRandomFishable(ItemStack stack, int itemWeightIn) {
        this.stack = stack;
        this.weight = itemWeightIn;
    }

    public WeightedRandomFishable func_150709_a(float chanceIn) {
        this.chance = chanceIn;
        return this;
    }

    public WeightedRandomFishable func_150707_a() {
        this.treasure = true;
        return this;
    }

    public ItemStack getItemStack(RandomSource random) {
        ItemStack out = this.stack.copy();
        if (out.getCount() > 1) {
            out.setCount(1 + random.nextInt(out.getCount()));
        }
        return out;
    }

    public float getChance() {
        return chance;
    }

    public boolean isTreasure() {
        return treasure;
    }

    @Override
    public Weight getWeight() {
        return Weight.of(this.weight);
    }
}
