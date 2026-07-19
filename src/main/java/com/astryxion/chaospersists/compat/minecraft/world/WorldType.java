package com.astryxion.chaospersists.compat.minecraft.world;

/** Legacy 1.12 world types for custom chunk providers. */
public enum WorldType {
  DEFAULT,
  FLAT,
  LARGE_BIOMES,
  AMPLIFIED,
  CUSTOMIZED,
  DEBUG_ALL_BLOCK_STATES,
  DEFAULT_1_1;

  public static WorldType parseWorldType(String id) {
    return DEFAULT;
  }
}
