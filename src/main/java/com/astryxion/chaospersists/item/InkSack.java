package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.entity.AttackSquid;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class InkSack extends Projectile {
    private float my_rotation = 0.0f;
    private int my_index = 65;

    public InkSack(EntityType<? extends InkSack> type, Level level) {
        super(type, level);
    }

    public InkSack(Level level) {
        this(ChaosPersists.ENTITY_TYPE_INK_SACK.get(), level);
    }

    public InkSack(Level level, int par2) {
        this(level);
    }

    public InkSack(Level level, LivingEntity thrower) {
        this(level);
        this.setOwner(thrower);
    }

    public InkSack(Level level, LivingEntity thrower, int par3) {
        this(level, thrower);
    }

    public InkSack(EntityType<? extends InkSack> type, double x, double y, double z, Level level) {
        super(type, level);
        this.setPos(x, y, z);
    }

    public InkSack(Level level, double x, double y, double z) {
        this(ChaosPersists.ENTITY_TYPE_INK_SACK.get(), x, y, z, level);
    }

    public void shoot(double xd, double yd, double zd, float velocity, float inaccuracy) {
        Vec3 vec3 = new Vec3(xd, yd, zd).normalize().scale(velocity);
        vec3 = vec3.add(
                this.random.triangle(0.0, inaccuracy * 0.0075),
                this.random.triangle(0.0, inaccuracy * 0.0075),
                this.random.triangle(0.0, inaccuracy * 0.0075));
        this.setDeltaMovement(vec3);
    }

    public int getInkSackIndex() {
        return this.my_index;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        this.my_rotation += 30.0f;
        while (this.my_rotation > 360.0f) {
            this.my_rotation -= 360.0f;
        }
        this.setXRot(this.my_rotation);
        this.xRotO = this.my_rotation;
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
        }
        this.checkInsideBlocks();

        Vec3 delta = this.getDeltaMovement();
        double newX = this.getX() + delta.x;
        double newY = this.getY() + delta.y;
        double newZ = this.getZ() + delta.z;
        this.setPos(newX, newY, newZ);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) result).getEntity();
            float damage = 1.0f;
            if (entity instanceof Creeper) {
                damage = 4.0f;
            }
            if (entity instanceof com.astryxion.chaospersists.entity.WaterDragon) {
                return;
            }
            if (entity instanceof AttackSquid) {
                return;
            }
            Entity owner = this.getOwner();
            entity.hurt(
                    this.damageSources().thrown(this, owner instanceof LivingEntity ? (LivingEntity) owner : null),
                    damage);
            if (entity instanceof LivingEntity living && this.random.nextInt(2) == 0) {
                living.addEffect(
                        new MobEffectInstance(MobEffects.BLINDNESS, 100 + 50 * this.random.nextInt(8), 0));
            }
        }
        if (this.level().isClientSide) {
            for (int i = 0; i < 4; ++i) {
                this.level()
                        .addParticle(
                                ParticleTypes.SMOKE,
                                this.getX() + this.random.nextFloat() - this.random.nextFloat(),
                                this.getY() + this.random.nextFloat() - this.random.nextFloat(),
                                this.getZ() + this.random.nextFloat(),
                                0.0,
                                0.0,
                                0.0);
            }
        }
        this.playSound(
                SoundEvents.GENERIC_SPLASH,
                0.5f,
                1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.5f);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
