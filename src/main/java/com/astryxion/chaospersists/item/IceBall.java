package com.astryxion.chaospersists.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class IceBall extends LaserBall {
    private int my_index = 84;
    private int icemaker = 0;

    public IceBall(EntityType<? extends IceBall> type, Level level) {
        super(type, level);
        super.setIceBall();
    }

    public IceBall(EntityType<? extends IceBall> type, Level level, int par2) {
        super(type, level, par2);
        super.setIceBall();
    }

    public IceBall(EntityType<? extends IceBall> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
        super.setIceBall();
    }

    public IceBall(EntityType<? extends IceBall> type, LivingEntity shooter, Level level, int par3) {
        super(type, shooter, level, par3);
        super.setIceBall();
    }

    public IceBall(EntityType<? extends IceBall> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
        super.setIceBall();
    }

    public int getIceBallIndex() {
        return this.my_index;
    }

    public void setIceMaker(int i) {
        this.icemaker = i;
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        if (result.getType() == HitResult.Type.ENTITY) {
            Entity hit = ((EntityHitResult) result).getEntity();
            if (isRoyalty(hit)) {
                this.discard();
                return;
            }
        }
        Vec3 hitVec = result.getLocation();
        super.onHit(result);
        if (this.icemaker != 0) {
            for (int i = 0; i < 5; ++i) {
                int x = this.random.nextInt(4);
                if (this.random.nextInt(2) == 1) {
                    x = -x;
                }
                int y = this.random.nextInt(4);
                if (this.random.nextInt(2) == 1) {
                    y = -y;
                }
                int z = this.random.nextInt(4);
                if (this.random.nextInt(2) == 1) {
                    z = -z;
                }
                x = (int) ((double) x + hitVec.x);
                y = (int) ((double) y + hitVec.y);
                z = (int) ((double) z + hitVec.z);
                this.level().setBlock(new BlockPos(x, y, z), Blocks.ICE.defaultBlockState(), 3);
            }
        }
    }

    private static boolean isRoyalty(Entity e) {
        if (!(e instanceof LivingEntity)) {
            return false;
        }
        String name = e.getClass().getSimpleName();
        return name.equals("ThePrince")
                || name.equals("ThePrinceTeen")
                || name.equals("ThePrinceAdult")
                || name.equals("ThePrincess")
                || name.equals("TheKing")
                || name.equals("KingHead")
                || name.equals("TheQueen")
                || name.equals("QueenHead")
                || name.equals("PurplePower");
    }
}
