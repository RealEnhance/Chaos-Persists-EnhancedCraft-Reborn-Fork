package com.astryxion.chaospersists.util;

import java.util.Comparator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

public class MyEntityAINearestAttackableTargetSorter implements Comparator<Entity> {
    private final Entity theEntity;

    public MyEntityAINearestAttackableTargetSorter(
            MyEntityAINearestAttackableTarget par1EntityAINearestAttackableTarget, Entity par2Entity) {
        this.theEntity = par2Entity;
    }

    public int compareDistanceSq(Entity par1Entity, Entity par2Entity) {
        double var3 = this.theEntity.distanceToSqr(par1Entity);
        if (par1Entity instanceof Creeper) {
            var3 /= 2.0;
        }
        double var5 = this.theEntity.distanceToSqr(par2Entity);
        if (par2Entity instanceof Creeper) {
            var5 /= 2.0;
        }
        return Double.compare(var3, var5);
    }

    @Override
    public int compare(Entity par1Obj, Entity par2Obj) {
        return this.compareDistanceSq(par1Obj, par2Obj);
    }
}
