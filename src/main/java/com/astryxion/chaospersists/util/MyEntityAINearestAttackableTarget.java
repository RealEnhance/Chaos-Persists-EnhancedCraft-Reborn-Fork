package com.astryxion.chaospersists.util;

import com.astryxion.chaospersists.entity.Boyfriend;
import com.astryxion.chaospersists.entity.Girlfriend;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;

public class MyEntityAINearestAttackableTarget extends MyEntityAITarget {
    protected LivingEntity targetEntity;
    protected final Class<? extends LivingEntity> targetClass;
    protected final int targetChance;
    private final Predicate<LivingEntity> targetEntitySelector;
    private final MyEntityAINearestAttackableTargetSorter theNearestAttackableTargetSorter;

    public MyEntityAINearestAttackableTarget(
            Mob par1EntityLiving, Class<? extends LivingEntity> par2Class, float par3, int par4, boolean par5) {
        this(par1EntityLiving, par2Class, par3, par4, par5, false);
    }

    public MyEntityAINearestAttackableTarget(
            Mob par1EntityLiving,
            Class<? extends LivingEntity> par2Class,
            float par3,
            int par4,
            boolean par5,
            boolean par6) {
        this(par1EntityLiving, par2Class, par3, par4, par5, par6, null);
    }

    public MyEntityAINearestAttackableTarget(
            Mob par1,
            Class<? extends LivingEntity> par2,
            float par3,
            int par4,
            boolean par5,
            boolean par6,
            Predicate<LivingEntity> par7IEntitySelector) {
        super(par1, par3, par5, par6);
        this.targetClass = par2;
        this.targetDistance = par3;
        this.targetChance = par4;
        this.theNearestAttackableTargetSorter = new MyEntityAINearestAttackableTargetSorter(this, par1);
        this.targetEntitySelector =
                par7IEntitySelector != null ? par7IEntitySelector : MyUtils::isHostileMobTarget;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.taskOwner instanceof TamableAnimal tame && !tame.isTame()) {
            return false;
        }
        if (this.taskOwner instanceof Girlfriend gf && !gf.isTame()) {
            return false;
        }
        if (this.taskOwner instanceof Girlfriend gf && gf.isInSittingPose()) {
            return false;
        }
        if (this.taskOwner instanceof Girlfriend gf && gf.isOrderedToSit()) {
            return false;
        }
        if (this.taskOwner instanceof Boyfriend bf && !bf.isTame()) {
            return false;
        }
        if (this.taskOwner instanceof Boyfriend bf && bf.isInSittingPose()) {
            return false;
        }
        if (this.taskOwner instanceof Boyfriend bf && bf.isOrderedToSit()) {
            return false;
        }
        if (this.targetChance > 0 && this.taskOwner.getRandom().nextInt(100) > this.targetChance) {
            return false;
        }
        AABB box =
                this.taskOwner
                        .getBoundingBox()
                        .inflate(this.targetDistance, 4.0, this.targetDistance);
        List<? extends LivingEntity> var5 =
                this.taskOwner.level().getEntitiesOfClass(this.targetClass, box, this.targetEntitySelector);
        Collections.sort(var5, this.theNearestAttackableTargetSorter);
        for (LivingEntity var4 : var5) {
            if (this.isSuitableTarget(var4, false)) {
                this.targetEntity = var4;
                return true;
            }
        }
        this.targetEntity = null;
        return false;
    }

    @Override
    public void start() {
        this.taskOwner.setTarget(this.targetEntity);
        super.start();
    }
}
