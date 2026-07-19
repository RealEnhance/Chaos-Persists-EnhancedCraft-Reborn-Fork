package com.astryxion.chaospersists.world.dimension.teleporter;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/**
 * Teleports into the Crystal dimension at the player's X/Z (1.12 {@code CommandCrystal} used Y=120).
 */
public class CrystalTeleporter implements ITeleporter {
    private static final double FALLBACK_Y = 120.0D;

    private final double targetX;
    private final double targetZ;

    public CrystalTeleporter(double targetX, double targetZ) {
        this.targetX = targetX;
        this.targetZ = targetZ;
    }

    @Override
    public Entity placeEntity(
            Entity entity,
            ServerLevel currentWorld,
            ServerLevel destWorld,
            float yaw,
            Function<Boolean, Entity> repositionEntity) {
        int blockX = (int) Math.floor(this.targetX);
        int blockZ = (int) Math.floor(this.targetZ);
        ChunkPos chunkPos = new ChunkPos(blockX >> 4, blockZ >> 4);
        destWorld.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, entity.getId());
        destWorld.getChunk(chunkPos.x, chunkPos.z);

        double spawnY = findCrystalSpawnY(destWorld, blockX, blockZ);

        Entity moved = repositionEntity.apply(false);
        if (moved != null) {
            moved.moveTo(this.targetX, spawnY, this.targetZ, yaw, moved.getXRot());
            moved.setDeltaMovement(0.0D, 0.0D, 0.0D);
            moved.fallDistance = 0.0F;
            moved.setRemainingFireTicks(0);
        }
        return moved;
    }

    private static double findCrystalSpawnY(ServerLevel level, int blockX, int blockZ) {
        int heightmapY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        if (heightmapY > level.getMinBuildHeight() + 1) {
            BlockPos ground = new BlockPos(blockX, heightmapY - 1, blockZ);
            if (ChaosPersists.CrystalGrass != null && level.getBlockState(ground).is(ChaosPersists.CrystalGrass)) {
                return heightmapY + 0.5D;
            }
        }
        int minY = level.getMinBuildHeight() + 1;
        for (int y = Math.min(200, level.getMaxBuildHeight() - 2); y > minY; --y) {
            BlockPos feet = new BlockPos(blockX, y, blockZ);
            BlockPos ground = feet.below();
            if (level.getBlockState(feet).isAir()
                    && ChaosPersists.CrystalGrass != null
                    && level.getBlockState(ground).is(ChaosPersists.CrystalGrass)) {
                return y + 0.5D;
            }
        }
        return FALLBACK_Y;
    }
}
