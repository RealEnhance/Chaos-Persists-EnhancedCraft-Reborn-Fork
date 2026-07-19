package com.astryxion.chaospersists.util;

import com.astryxion.chaospersists.entity.Boyfriend;
import com.astryxion.chaospersists.entity.Girlfriend;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;

public class MyEntityAIJealousy extends MyEntityAINearestAttackableTarget {
    private final TamableAnimal theTameable;

    public MyEntityAIJealousy(
            TamableAnimal par1EntityTameable, Class<? extends LivingEntity> par2Class, float par3, int par4, boolean par5) {
        super((Mob) par1EntityTameable, par2Class, par3, par4, par5);
        this.theTameable = par1EntityTameable;
    }

    @Override
    public boolean canUse() {
        if (!this.theTameable.isTame()) {
            return false;
        }
        if (this.theTameable.isInSittingPose()) {
            return false;
        }
        if (this.theTameable.isOrderedToSit()) {
            return false;
        }
        if (!super.canUse()) {
            return false;
        }
        LivingEntity victim = this.targetEntity;
        if (victim == null) {
            return false;
        }
        if (this.theTameable instanceof Girlfriend) {
            if (victim instanceof Girlfriend gf && gf.isTame()) {
                return false;
            }
        } else if (victim instanceof Boyfriend bf && bf.isTame()) {
            return false;
        }
        if (this.theTameable.getOwner() == null) {
            return false;
        }
        return true;
    }
}
