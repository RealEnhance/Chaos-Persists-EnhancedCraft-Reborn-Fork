package com.astryxion.chaospersists.util;

import com.astryxion.chaospersists.entity.ThePrince;
import com.astryxion.chaospersists.entity.ThePrinceAdult;
import com.astryxion.chaospersists.entity.ThePrinceTeen;
import com.astryxion.chaospersists.entity.ThePrincess;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/** Prince/princess follow helpers modeled on Mythic Mounts {@code MountFollowOwnerGoal}. */
public final class RoyalPetFollowHelper {

    private RoyalPetFollowHelper() {}

    public static boolean isStayingPut(TamableAnimal pet) {
        return pet.isOrderedToSit() || pet.isInSittingPose();
    }

    public static boolean isRoyalPet(TamableAnimal pet) {
        return pet instanceof ThePrince
                || pet instanceof ThePrincess
                || pet instanceof ThePrinceTeen
                || pet instanceof ThePrinceAdult;
    }

    /** Move tamed royal pets to the player's dimension when they change worlds. */
    public static void bringRoyalPetsToPlayer(ServerPlayer player) {
        if (player.level().isClientSide) {
            return;
        }
        UUID ownerId = player.getUUID();
        for (ServerLevel level : player.server.getAllLevels()) {
            List<TamableAnimal> pets = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal pet)) {
                    continue;
                }
                if (!isRoyalPet(pet) || !pet.isTame() || !ownerId.equals(pet.getOwnerUUID())) {
                    continue;
                }
                if (isStayingPut(pet)) {
                    continue;
                }
                if (pet.getVehicle() == player) {
                    continue;
                }
                pets.add(pet);
            }
            for (TamableAnimal pet : pets) {
                teleportNearOwner(pet, player);
            }
        }
    }

    /** Teleport only when the owner is in another dimension (does not affect in-flight movement). */
    public static void syncDimensionOnly(TamableAnimal pet) {
        if (pet.level().isClientSide || !pet.isTame() || isStayingPut(pet) || !isRoyalPet(pet)) {
            return;
        }
        if (!pet.getPassengers().isEmpty()) {
            return;
        }
        LivingEntity owner = pet.getOwner();
        if (owner == null || pet.level() == owner.level()) {
            return;
        }
        teleportNearOwner(pet, owner);
    }

    /**
     * Mythic Mounts-style catch-up teleport when pathing fails and the owner is at least 12 blocks away.
     * Supports cross-dimension travel via {@link Entity#teleportTo}.
     */
    public static boolean tryFollowTeleport(TamableAnimal pet, LivingEntity owner) {
        if (owner == null || isStayingPut(pet)) {
            return false;
        }
        if (pet.level() != owner.level()) {
            return teleportNearOwner(pet, owner);
        }
        if (pet.distanceToSqr(owner) < 144.0) {
            return false;
        }
        BlockPos base = owner.blockPosition();
        for (int attempt = 0; attempt < 10; ++attempt) {
            int dx = pet.getRandom().nextInt(7) - 3;
            int dy = pet.getRandom().nextInt(3) - 1;
            int dz = pet.getRandom().nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            BlockPos target = base.offset(dx, dy, dz);
            if (canStandAt(pet, target)) {
                pet.moveTo(
                        target.getX() + 0.5,
                        target.getY(),
                        target.getZ() + 0.5,
                        pet.getYRot(),
                        pet.getXRot());
                PathNavigation navigation = pet.getNavigation();
                if (navigation != null) {
                    navigation.stop();
                }
                return true;
            }
        }
        return teleportNearOwner(pet, owner);
    }

    public static boolean teleportNearOwner(TamableAnimal pet, LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel dest) || pet.isRemoved()) {
            return false;
        }
        if (!pet.getPassengers().isEmpty()) {
            return false;
        }
        pet.stopRiding();
        double x = owner.getX() + (pet.getRandom().nextDouble() - 0.5) * 2.0;
        double y = owner.getY() + 1.0;
        double z = owner.getZ() + (pet.getRandom().nextDouble() - 0.5) * 2.0;
        boolean moved = pet.teleportTo(dest, x, y, z, Set.of(), owner.getYRot(), pet.getXRot());
        if (moved) {
            MyUtils.clearChaosFlight(pet);
            PathNavigation navigation = pet.getNavigation();
            if (navigation != null) {
                navigation.stop();
            }
        }
        return moved;
    }

    private static boolean canStandAt(TamableAnimal pet, BlockPos pos) {
        Level level = pet.level();
        BlockPathTypes nodeType = WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable());
        if (nodeType != BlockPathTypes.WALKABLE && nodeType != BlockPathTypes.OPEN) {
            return false;
        }
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
            return false;
        }
        return level.noCollision(
                pet,
                pet.getBoundingBox()
                        .move(
                                pos.getX() + 0.5 - pet.getX(),
                                pos.getY() - pet.getY(),
                                pos.getZ() + 0.5 - pet.getZ()));
    }
}
