package com.astryxion.chaospersists.util;

import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class MiningDropHelper {

  private static final String MODID = "chaospersists";

  private MiningDropHelper() {}

  public static List<ItemStack> selfDrops(Block block, LootParams.Builder builder) {
    ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
    if (tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
      return Collections.singletonList(new ItemStack(block));
    }
    return Collections.singletonList(new ItemStack(block.asItem()));
  }

  /**
   * Ore drop: Silk Touch yields the ore block itself, otherwise a random {@code [min, max]} count of
   * the chaospersists item named {@code dropPath}, multiplied by the vanilla Fortune ore bonus.
   */
  public static List<ItemStack> oreDrops(
      Block block, LootParams.Builder builder, String dropPath, int min, int max) {
    ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
    if (tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
      return Collections.singletonList(new ItemStack(block));
    }
    Item item =
        BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MODID, dropPath));
    if (item == null || item == Items.AIR) {
      return Collections.emptyList();
    }
    RandomSource random = RandomSource.create();
    int count = max <= min ? min : min + random.nextInt(max - min + 1);
    int fortune =
        tool == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
    if (fortune > 0) {
      // Vanilla ORE_DROPS bonus: uniform 1..(fortune+1) multiplier, weighted toward 1.
      int multiplier = Math.max(0, random.nextInt(fortune + 2) - 1) + 1;
      count *= multiplier;
    }
    return Collections.singletonList(new ItemStack(item, count));
  }
}
