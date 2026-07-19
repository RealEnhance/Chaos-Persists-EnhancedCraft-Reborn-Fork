package com.astryxion.chaospersists.compat.forge.fml.common.registry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

/** Merges legacy queued recipes into a loaded {@link RecipeManager} (1.12 GameRegistry behavior). */
final class LegacyRecipeInjector {

  private LegacyRecipeInjector() {}

  static void inject(RecipeManager manager, List<Recipe<?>> recipes) {
    if (recipes.isEmpty()) {
      return;
    }
    List<Recipe<?>> merged = new ArrayList<>(manager.getRecipes());
    merged.addAll(recipes);
    manager.replaceRecipes(merged);
  }

  static List<Recipe<?>> drainPending(
      List<GameRegistry.RecipeEntry> entries, List<GameRegistry.SmeltEntry> smelts) {
    List<Recipe<?>> out = new ArrayList<>();
    for (GameRegistry.RecipeEntry entry : entries) {
      out.add(entry.build());
    }
    for (GameRegistry.SmeltEntry entry : smelts) {
      out.add(entry.build());
    }
    return out;
  }
}
