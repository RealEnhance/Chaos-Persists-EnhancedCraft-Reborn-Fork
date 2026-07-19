package com.astryxion.chaospersists.item;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class Acid extends LaserBall {
    private int my_index = 85;

    public Acid(EntityType<? extends Acid> type, Level level) {
        super(type, level);
        super.setAcid();
    }

    public Acid(EntityType<? extends Acid> type, Level level, int par2) {
        super(type, level, par2);
        super.setAcid();
    }

    public Acid(EntityType<? extends Acid> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
        super.setAcid();
    }

    public Acid(EntityType<? extends Acid> type, LivingEntity shooter, Level level, int par3) {
        super(type, shooter, level, par3);
        super.setAcid();
    }

    public Acid(EntityType<? extends Acid> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
        super.setAcid();
    }

    public int getAcidIndex() {
        return this.my_index;
    }
}
