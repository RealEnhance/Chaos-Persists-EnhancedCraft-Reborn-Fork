package com.astryxion.chaospersists.compat.forge.event;

import java.util.Random;
import com.astryxion.chaospersists.compat.minecraft.world.World;
import com.astryxion.chaospersists.compat.minecraft.world.chunk.ChunkPrimer;
import com.astryxion.chaospersists.compat.minecraft.world.gen.IChunkGenerator;
import com.astryxion.chaospersists.compat.forge.event.terraingen.PopulateChunkEvent;

/** Legacy Forge worldgen event factory hooks. */
public final class ForgeEventFactory {
  private ForgeEventFactory() {}

  public static boolean onReplaceBiomeBlocks(
      IChunkGenerator generator, int x, int z, ChunkPrimer primer, World world) {
    return PopulateChunkEvent.onReplaceBiomeBlocks(generator, x, z, primer, world);
  }

  public static boolean onChunkPopulate(
      boolean newChunk,
      IChunkGenerator generator,
      World world,
      Random rand,
      int chunkX,
      int chunkZ,
      boolean hasVillage) {
    return PopulateChunkEvent.onChunkPopulate(
        newChunk, generator, world, rand, chunkX, chunkZ, hasVillage);
  }
}
