package com.astryxion.chaospersists.world.dimension.teleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/**
 * Teleports into Utopia at the player's X/Z, finding a safe surface Y (1.12 {@code TeleporterUtopia} used Y=120).
 */
public class UtopiaTeleporter implements ITeleporter {
    private final double targetX;
    private final double targetZ;

    public UtopiaTeleporter(double targetX, double targetZ) {
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

        int spawnY = destWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        if (spawnY < destWorld.getMinBuildHeight() + 8) {
            spawnY = 120;
        } else {
            spawnY = Math.max(spawnY + 1, 64);
        }

        Entity moved = repositionEntity.apply(false);
        if (moved != null) {
            moved.moveTo(this.targetX, spawnY, this.targetZ, yaw, moved.getXRot());
            moved.setDeltaMovement(0.0D, 0.0D, 0.0D);
            moved.fallDistance = 0.0F;
            moved.setRemainingFireTicks(0);
        }
        return moved;
    }
}
