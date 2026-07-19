package com.astryxion.chaospersists.util;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.entity.Girlfriend;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
public class MyEntityAIFollowOwner extends Goal {
    private final TamableAnimal thePet;
    private LivingEntity theOwner;
    private final Level theWorld;
    private final float field_75336_f;
    private final PathNavigation petPathfinder;
    private int field_75343_h;
    private final float maxDist;
    private final float minDist;

    public MyEntityAIFollowOwner(TamableAnimal par1EntityTameable, float par2, float par3, float par4) {
        this.thePet = par1EntityTameable;
        this.theWorld = par1EntityTameable.level();
        this.field_75336_f = par2;
        this.petPathfinder = par1EntityTameable.getNavigation();
        this.minDist = par4;
        this.maxDist = par3;
        this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean isPetStaying() {
        return this.thePet.isInSittingPose() || this.thePet.isOrderedToSit();
    }

    @Override
    public boolean canUse() {
        LivingEntity var1 = this.thePet.getOwner();
        if (var1 == null) {
            return false;
        }
        this.theOwner = var1;
        if (this.isPetStaying()) {
            return false;
        }
        if (MyUtils.isPrinceFlying(this.thePet)) {
            return false;
        }
        if (Girlfriend.class.isInstance(this.thePet) && ChaosPersists.valentines_day != 0) {
            return false;
        }
        double distSq = this.thePet.distanceToSqr(this.theOwner);
        if (!(this.thePet.getY() >= 60.0 && MyUtils.isDay(this.theWorld)
                || distSq <= (double) (this.maxDist / 2.0f * (this.maxDist / 2.0f)))) {
            return true;
        }
        if (distSq < (double) (this.maxDist * this.maxDist)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.isPetStaying()) {
            return false;
        }
        if (MyUtils.isPrinceFlying(this.thePet)) {
            return false;
        }
        LivingEntity owner = this.thePet.getOwner();
        if (owner == null) {
            return false;
        }
        this.theOwner = owner;
        if ((int) this.thePet.getZ() == (int) owner.getZ()
                && (int) this.thePet.getX() == (int) owner.getX()
                && (int) this.thePet.getY() < (int) owner.getY() + 2
                && (int) this.thePet.getY() > (int) owner.getY() - 2) {
            return false;
        }
        if (this.thePet.distanceToSqr(this.theOwner) <= (double) (this.minDist * this.minDist)) {
            return false;
        }
        if (this.thePet.distanceToSqr(this.theOwner) >= 144.0) {
            return true;
        }
        return !this.petPathfinder.isDone();
    }

    @Override
    public void start() {
        this.field_75343_h = 0;
    }

    @Override
    public void stop() {
        this.theOwner = null;
        this.petPathfinder.stop();
    }

    @Override
    public void tick() {
        MyUtils.setChaseTarget(this.thePet, this.theOwner);
        if (this.isPetStaying()) {
            return;
        }
        if (this.theOwner.level() != this.thePet.level()) {
            RoyalPetFollowHelper.tryFollowTeleport(this.thePet, this.theOwner);
            return;
        }
        if (--this.field_75343_h <= 0) {
            this.field_75343_h = 10;
            if (!this.petPathfinder.moveTo(this.theOwner, (double) this.field_75336_f)
                    && this.thePet.distanceToSqr(this.theOwner) >= 144.0
                    && !MyUtils.shouldPrinceSkipFollowTeleport(this.thePet, this.theOwner)) {
                RoyalPetFollowHelper.tryFollowTeleport(this.thePet, this.theOwner);
            }
        }
    }
}
