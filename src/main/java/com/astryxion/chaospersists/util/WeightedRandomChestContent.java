package com.astryxion.chaospersists.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 1.12.2 {@code WeightedRandomChestContent} equivalent for 1.20.1 chest loot.
 */
public class WeightedRandomChestContent implements WeightedEntry {
    private final Weight weight;
    public final Item item;
    public final int metadata;
    public final int minStackSize;
    public final int maxStackSize;

    public WeightedRandomChestContent(Object item, int metadata, int minStackSize, int maxStackSize, int weight) {
        this.weight = Weight.of(weight);
        this.item = resolveItem(item);
        this.metadata = metadata;
        this.minStackSize = minStackSize;
        this.maxStackSize = maxStackSize;
    }

    private static Item resolveItem(Object item) {
        if (item == null) {
            return null;
        }
        if (item instanceof Item registered) {
            return registered;
        }
        if (item instanceof Block block) {
            return block.asItem();
        }
        return (Item) item;
    }

    public WeightedRandomChestContent(ItemStack stack, int minChance, int maxChance, int weight) {
        this(stack.getItem(), 0, minChance, maxChance, weight);
    }

      @Override
    public Weight getWeight() {
        return this.weight;
    }

    public static void generateChestContents(RandomSource random, WeightedRandomChestContent[] content, Container inv, int max) {
        List<WeightedRandomChestContent> list = Arrays.asList(content);
        int total = WeightedRandom.getTotalWeight(list);
        for (int i = 0; i < max; i++) {
            Optional<WeightedRandomChestContent> entry = WeightedRandom.getRandomItem(random, list, total);
            if (entry.isEmpty()) {
                continue;
            }
            WeightedRandomChestContent pick = entry.get();
            if (pick.item == null) {
                continue;
            }
            int count = pick.minStackSize
                    + (pick.maxStackSize > pick.minStackSize
                            ? random.nextInt(pick.maxStackSize - pick.minStackSize + 1)
                            : 0);
            int limit = pick.item.getMaxStackSize(new ItemStack(pick.item));
            ItemStack stack = new ItemStack(pick.item, Math.min(count, limit));
            int slot = random.nextInt(inv.getContainerSize());
            inv.setItem(slot, stack);
        }
    }

    public static void generateChestContents(
            java.util.Random random, WeightedRandomChestContent[] content, Container inv, int max) {
        generateChestContents(RandomSource.create(random.nextLong()), content, inv, max);
    }

    public static void generateChestContents(
            java.util.Random random, List<WeightedRandomChestContent> content, Container inv, int max) {
        generateChestContents(RandomSource.create(random.nextLong()), content.toArray(new WeightedRandomChestContent[0]), inv, max);
    }
}
