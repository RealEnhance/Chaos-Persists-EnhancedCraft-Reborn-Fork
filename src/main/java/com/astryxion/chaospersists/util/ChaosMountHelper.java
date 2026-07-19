package com.astryxion.chaospersists.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared mount/dismount cleanup inspired by Mythic Mounts and PrinceTeen. */
public final class ChaosMountHelper {
    public static final int DISMOUNT_COOLDOWN_TICKS = 80;
    public static final int GROUND_DISMOUNT_COOLDOWN_TICKS = 25;

    private ChaosMountHelper() {}

    public static boolean canPlayerMount(Player player, Mob mount, int dismountCooldown) {
        return dismountCooldown <= 0
                && mount.getPassengers().isEmpty()
                && !player.isPassenger()
                && player.distanceToSqr(mount) < 256.0;
    }

    public static void onPlayerMounted(Mob mount) {
        clearTamableSitState(mount);
        mount.setNoGravity(false);
        mount.noPhysics = false;
        MyUtils.clearChaosFlight(mount);
    }

    public static void finishDismountLanding(Mob mount) {
        mount.setNoGravity(false);
        mount.noPhysics = false;
        MyUtils.clearChaosFlight(mount);
        mount.getNavigation().stop();
        mount.setTarget(null);
        clearTamableSitState(mount);
        Vec3 motion = mount.getDeltaMovement();
        mount.setDeltaMovement(motion.x, Math.min(motion.y, -0.25), motion.z);
        MyUtils.enforceDragonMountGroundSafety(mount);
    }

    public static void finishFlyingDismountLanding(Mob mount) {
        finishDismountLanding(mount);
        if (!mount.onGround()) {
            snapToNearestGround(mount);
        }
        mount.setDeltaMovement(Vec3.ZERO);
    }

    public static void applyGroundGravityWhenIdle(Mob mount) {
        if (mount.level().isClientSide || !mount.getPassengers().isEmpty()) {
            return;
        }
        mount.setNoGravity(false);
        mount.noPhysics = false;
        MyUtils.clearChaosFlight(mount);
        if (!mount.onGround()) {
            Vec3 motion = mount.getDeltaMovement();
            mount.setDeltaMovement(motion.x, Math.min(motion.y - 0.06, -0.12), motion.z);
        }
    }

    public static void snapToNearestGround(Mob mount) {
        if (mount.level().isClientSide || mount.onGround()) {
            return;
        }
        Level level = mount.level();
        int x = Mth.floor(mount.getX());
        int z = Mth.floor(mount.getZ());
        int startY = Mth.floor(mount.getY());
        for (int y = startY; y > level.getMinBuildHeight(); --y) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.blocksMotion()) {
                mount.moveTo(mount.getX(), y + 1.0, mount.getZ());
                return;
            }
        }
    }

    private static void clearTamableSitState(Mob mount) {
        if (mount instanceof TamableAnimal tame) {
            tame.setOrderedToSit(false);
            tame.setInSittingPose(false);
        }
    }

    /** Seat height only — vanilla already applies passenger pose offsets elsewhere. */
    public static double riderSeatY(Mob mount, Entity passenger, double ridingOffset) {
        return mount.getY() + ridingOffset;
    }
}
