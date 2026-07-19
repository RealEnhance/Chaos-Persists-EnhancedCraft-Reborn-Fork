package com.astryxion.chaospersists.compat.minecraft.world.gen.structure;

import java.util.Random;
import net.minecraft.core.BlockPos;
import com.astryxion.chaospersists.compat.minecraft.util.math.ChunkPos;
import com.astryxion.chaospersists.compat.minecraft.world.World;
import com.astryxion.chaospersists.compat.minecraft.world.chunk.ChunkPrimer;
import com.astryxion.chaospersists.compat.minecraft.world.gen.MapGenBase;

/** Legacy 1.12 structure map generator base. */
public abstract class MapGenStructure extends MapGenBase {
  @Override
  public void generate(World worldIn, int chunkX, int chunkZ, ChunkPrimer primer) {
    this.world = worldIn;
  }

  protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
    return false;
  }

  public boolean generateStructure(World worldIn, Random randomIn, ChunkPos pos) {
    this.world = worldIn;
    return canSpawnStructureAtCoords(pos.x, pos.z);
  }

  public BlockPos getNearestStructurePos(World worldIn, BlockPos pos, boolean findUnexplored) {
    return null;
  }

  public boolean isInsideStructure(BlockPos pos) {
    return false;
  }
}
