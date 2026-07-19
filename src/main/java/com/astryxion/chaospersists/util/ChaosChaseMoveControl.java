package com.astryxion.chaospersists.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * 1.7.10 chase parity: while pathing toward a target, steer body yaw at the living target
 * instead of the next path node. Vanilla {@link MoveControl} fighting {@code faceEntity}
 * caused large mobs to wobble during pursuit.
 */
public class ChaosChaseMoveControl extends MoveControl {
    public ChaosChaseMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        if (this.operation == Operation.MOVE_TO) {
            LivingEntity chaseTarget = this.resolveChaseTarget();
            if (chaseTarget != null) {
                this.tickChaseTowardTarget(chaseTarget);
                return;
            }
        }
        super.tick();
    }

    private LivingEntity resolveChaseTarget() {
        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) {
            return target;
        }
        return MyUtils.getChaseTarget(this.mob);
    }

    private void tickChaseTowardTarget(LivingEntity target) {
        double dx = this.wantedX - this.mob.getX();
        double dz = this.wantedZ - this.mob.getZ();
        double dy = this.wantedY - this.mob.getY();
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
            return;
        }
        float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        this.mob.setSpeed(speed);
        float desiredYaw =
                (float) (Mth.atan2(target.getZ() - this.mob.getZ(), target.getX() - this.mob.getX())
                                * (180.0 / Math.PI))
                        - 90.0F;
        float yaw = this.rotlerp(this.mob.getYRot(), desiredYaw, 10.0F);
        this.mob.setYRot(Mth.wrapDegrees(yaw));
        this.mob.yHeadRot = this.mob.getYRot();
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.setZza(speed);
    }
}
