package com.astryxion.chaospersists.util;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public final class CrystalDimensionSpawnHelper {
    private CrystalDimensionSpawnHelper() {}

    public static boolean isCrystalDimension(Level world) {
        return world != null && world.dimension().equals(ChaosPersists.getCrystalDimensionKey());
    }

    public static boolean isCrystalDimension(LevelAccessor world) {
        return world instanceof Level level && isCrystalDimension(level);
    }
}
