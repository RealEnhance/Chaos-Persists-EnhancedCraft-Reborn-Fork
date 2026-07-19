package com.astryxion.chaospersists.util;

import java.util.Comparator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

public class GenericTargetSorter implements Comparator<Entity> {
    private final Entity theEntity;

    public GenericTargetSorter(Entity par2Entity) {
        this.theEntity = par2Entity;
    }

    public int compareDistanceSq(Entity par1Entity, Entity par2Entity) {
        double weight;
        double var3 = this.theEntity.distanceToSqr(par1Entity);
        if (par1Entity instanceof Creeper) {
            var3 /= 2.0;
        }
        weight = par1Entity.getBbHeight() * par1Entity.getBbWidth();
        if (weight > 1.0) {
            var3 /= weight;
        }
        double var5 = this.theEntity.distanceToSqr(par2Entity);
        if (par2Entity instanceof Creeper) {
            var5 /= 2.0;
        }
        weight = par2Entity.getBbHeight() * par2Entity.getBbWidth();
        if (weight > 1.0) {
            var5 /= weight;
        }
        return Double.compare(var3, var5);
    }

    @Override
    public int compare(Entity o1, Entity o2) {
        return this.compareDistanceSq(o1, o2);
    }
}
