package com.astryxion.chaospersists.util;

import java.util.Comparator;
import net.minecraft.world.entity.Entity;

public class MyValentineTargetSorter implements Comparator<Entity> {
    private final Entity theEntity;

    public MyValentineTargetSorter(MyValentineTarget par1EntityAINearestAttackableTarget, Entity par2Entity) {
        this.theEntity = par2Entity;
    }

    public int compareDistanceSq(Entity par1Entity, Entity par2Entity) {
        double var3 = this.theEntity.distanceToSqr(par1Entity);
        double var5 = this.theEntity.distanceToSqr(par2Entity);
        return Double.compare(var3, var5);
    }

    @Override
    public int compare(Entity par1Obj, Entity par2Obj) {
        return this.compareDistanceSq(par1Obj, par2Obj);
    }
}
