package com.astryxion.chaospersists.entity;

import com.astryxion.chaospersists.item.LaserBall;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class DeadIrukandji extends LaserBall {
    private int my_index = 86;

    public DeadIrukandji(EntityType<? extends DeadIrukandji> type, Level level) {
        super(type, level);
        super.setIrukandji();
    }

    public DeadIrukandji(EntityType<? extends DeadIrukandji> type, Level level, int par2) {
        super(type, level, par2);
        super.setIrukandji();
    }

    public DeadIrukandji(EntityType<? extends DeadIrukandji> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
        super.setIrukandji();
    }

    public DeadIrukandji(EntityType<? extends DeadIrukandji> type, LivingEntity shooter, Level level, int par3) {
        super(type, shooter, level, par3);
        super.setIrukandji();
    }

    public DeadIrukandji(EntityType<? extends DeadIrukandji> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
        super.setIrukandji();
    }

    public int getIrukandjiIndex() {
        return this.my_index;
    }
}
