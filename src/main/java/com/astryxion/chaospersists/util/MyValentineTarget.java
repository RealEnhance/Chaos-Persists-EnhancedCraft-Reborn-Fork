package com.astryxion.chaospersists.util;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.entity.Girlfriend;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public class MyValentineTarget extends MyEntityAITarget {
    protected LivingEntity targetEntity;
    protected final LivingEntity me;
    protected final Class<? extends LivingEntity> targetClass;
    protected final int targetChance;
    private final Predicate<LivingEntity> targetEntitySelector;
    private final MyValentineTargetSorter theNearestAttackableTargetSorter;

    public MyValentineTarget(Mob par1EntityLiving, Class<? extends LivingEntity> par2Class, float par3, int par4, boolean par5) {
        this(par1EntityLiving, par2Class, par3, par4, par5, false);
    }

    public MyValentineTarget(
            Mob par1EntityLiving,
            Class<? extends LivingEntity> par2Class,
            float par3,
            int par4,
            boolean par5,
            boolean par6) {
        this(par1EntityLiving, par2Class, par3, par4, par5, par6, null);
    }

    public MyValentineTarget(
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
        this.theNearestAttackableTargetSorter = new MyValentineTargetSorter(this, par1);
        this.targetEntitySelector = par7IEntitySelector != null ? par7IEntitySelector : entity -> true;
        this.setFlags(EnumSet.of(Flag.TARGET));
        this.me = par1;
    }

    @Override
    public boolean canUse() {
        if (ChaosPersists.valentines_day == 0) {
            return false;
        }
        if (this.me instanceof Girlfriend gf && gf.feelingBetter != 0) {
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
