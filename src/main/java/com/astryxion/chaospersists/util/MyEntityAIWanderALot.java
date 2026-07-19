package com.astryxion.chaospersists.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MyEntityAIWanderALot extends Goal {
    private final PathfinderMob entity;
    private double xPosition;
    private double yPosition;
    private double zPosition;
    private final double speed;
    private final int xzRange;
    private int busy = 0;

    public MyEntityAIWanderALot(PathfinderMob par1EntityCreature, int par1, double par2) {
        this.entity = par1EntityCreature;
        this.xzRange = par1;
        this.speed = par2;
        this.setFlags(java.util.EnumSet.of(Flag.MOVE));
    }

    public void setBusy(int i) {
        this.busy = i;
    }

    @Override
    public boolean canUse() {
        if (this.busy != 0) {
            return false;
        }
        if (this.entity.getRandom().nextInt(30) != 0) {
            return false;
        }
        if (this.entity instanceof TamableAnimal tamable
                && (tamable.isInSittingPose() || tamable.isOrderedToSit())) {
            return false;
        }
        Vec3 var1 = DefaultRandomPos.getPos(this.entity, this.xzRange, 7);
        if (var1 == null) {
            return false;
        }
        this.xPosition = var1.x;
        this.yPosition = var1.y;
        this.zPosition = var1.z;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.entity.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.entity.getNavigation().moveTo(this.xPosition, this.yPosition, this.zPosition, this.speed);
    }
}
