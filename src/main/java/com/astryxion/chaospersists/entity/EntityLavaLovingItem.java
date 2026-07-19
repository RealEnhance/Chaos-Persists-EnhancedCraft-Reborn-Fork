package com.astryxion.chaospersists.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityLavaLovingItem extends ItemEntity {
    private boolean chaosFireImmune = true;
    private int chaosInvulnerableTime = 300;

    public EntityLavaLovingItem(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        this.noFire();
    }

    public EntityLavaLovingItem(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
        this.noFire();
    }

    public void noFire() {
        this.chaosFireImmune = true;
        this.chaosInvulnerableTime = 300;
    }

    public void yesFire() {
        this.chaosFireImmune = false;
        this.chaosInvulnerableTime = 0;
    }

    @Override
    public boolean fireImmune() {
        return this.chaosFireImmune || super.fireImmune();
    }

    @Override
    public void lavaHurt() {
        if (!this.fireImmune()) {
            this.hurt(this.damageSources().lava(), 4.0f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.chaosInvulnerableTime > 0) {
            --this.chaosInvulnerableTime;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.chaosInvulnerableTime > 0 && !source.is(DamageTypes.IN_FIRE)) {
            return false;
        }
        if (source.is(DamageTypes.IN_FIRE) && this.fireImmune()) {
            return false;
        }
        return super.hurt(source, amount);
    }
}
