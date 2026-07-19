package com.astryxion.chaospersists.entity;

import com.astryxion.chaospersists.core.ChaosPersists;
import com.astryxion.chaospersists.core.ChaosSounds;
import com.astryxion.chaospersists.entity.Kraken;
import com.astryxion.chaospersists.render.RenderInfo;
import com.astryxion.chaospersists.util.SpawnerFixHelper;
import com.astryxion.chaospersists.util.GenericTargetSorter;
import com.astryxion.chaospersists.util.MyEntityAIFollowOwner;
import com.astryxion.chaospersists.util.MyEntityAIWander;
import com.astryxion.chaospersists.util.MyUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import com.astryxion.chaospersists.util.ChaosHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.player.LocalPlayer;

public class Leon extends TamableAnimal {
    private static final EntityDataAccessor<Integer> INT20 =
            SynchedEntityData.defineId(Leon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> INT21 =
            SynchedEntityData.defineId(Leon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> INT22 =
            SynchedEntityData.defineId(Leon.class, EntityDataSerializers.INT);
    private int boatPosRotationIncrements;
    private double boatX;
    private double boatY;
    private double boatZ;
    private double boatYaw;
    private double boatPitch;
    private double boatYawHead;
    private final GenericTargetSorter targetSorter;
    private RenderInfo renderdata = new RenderInfo();
    private int hurt_timer = 0;
    private int wing_sound = 0;
    private BlockPos currentFlightTarget = null;
    private boolean target_in_sight = false;
    private int owner_flying = 0;
    private int flyaway = 0;
    private int stuck_count = 0;
    private int lastX = 0;
    private int lastZ = 0;
    private int unstick_timer = 0;
    private int dismountCooldown = 0;
    private float moveSpeed = 0.25f;
    private float deltasmooth = 0.0f;

    public Leon(EntityType<? extends Leon> type, Level level) {
        super(type, level);
        this.xpReward = 300;
        this.moveSpeed = 0.25f;
        this.setOrderedToSit(false);
        this.targetSorter = new GenericTargetSorter(this);
        this.renderdata = new RenderInfo();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MyEntityAIFollowOwner(this, 1.1f, 16.0f, 2.0f));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.25, Ingredient.of(Items.BEEF), false));
        this.goalSelector.addGoal(3, new MyEntityAIWander(this, 0.75f));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 9.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        if (ChaosPersists.PlayNicely == 0) {
            this.targetSelector.addGoal(
                    1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, false));
        }
        this.targetSelector.addGoal(2, new ChaosHurtByTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 55.0)
                .add(Attributes.ARMOR, 16.0);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(3.5f, 8.25f);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        this.refreshDimensions();
    }

    public boolean shouldRiderSit() {
        return true;
    }

    public int getTrackingRange() {
        return 64;
    }

    public int getUpdateFrequency() {
        return 10;
    }

    public boolean sendsVelocityUpdates() {
        return true;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity rider = this.getFirstPassenger();
        return rider instanceof LivingEntity living ? living : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 3.75;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            float f = 0.65f;
            double x = this.getX() - (double) f * Math.sin(Math.toRadians(this.getYRot()));
            double y = this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset();
            double z = this.getZ() + (double) f * Math.cos(Math.toRadians(this.getYRot()));
            moveFunction.accept(passenger, x, y, z);
        } else {
            super.positionRider(passenger, moveFunction);
        }
    }

    public boolean canBeSteered() {
        return true;
    }

    public boolean canPassengerSteer() {
        return true;
    }

    /**
     * Same pipeline as {@link ThePrinceTeen} / {@link Cephadrome}: vanilla calls {@code travel} with rider
     * input synced from the client via {@code ServerboundPlayerInputPacket}. Flight must run here, not only in
     * {@code aiStep}, or {@code zza} stays zero on the server and the mount cannot fly.
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (MyUtils.usesChaosFlight(this)) {
            return;
        }
        Vec3 dm = this.getDeltaMovement();
        double mx = dm.x;
        double my = dm.y;
        double mz = dm.z;
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player pp && this.getActivity() != 0) {
            if (pp.isDeadOrDying()) {
                this.ejectPassengers();
                this.setNoGravity(false);
                super.travel(travelVector);
                return;
            }
            this.setNoGravity(true);
            double obstruction_factor;
            double relative_g;
            double max_speed = 1.15;
            double gh;
            double deltav;

            if (mx < -2.0) {
                mx = -2.0;
            }
            if (mx > 2.0) {
                mx = 2.0;
            }
            if (mz < -2.0) {
                mz = -2.0;
            }
            if (mz > 2.0) {
                mz = 2.0;
            }
            double velocity = Math.sqrt(mx * mx + mz * mz);

            gh = 1.55;
            BlockState ground =
                    this.level()
                            .getBlockState(
                                    BlockPos.containing(
                                            this.getX(), (float) this.getY() - (float) gh, this.getZ()));
            if (!ground.isAir()) {
                my += 0.03;
                this.setPos(this.getX(), this.getY() + 0.1, this.getZ());
            } else {
                my -= 0.018;
            }

            obstruction_factor = 0.0;
            int distLimit = 3 + (int) (velocity * 7.0);
            if (distLimit < 3) {
                distLimit = 3;
            }
            if (distLimit > 24) {
                distLimit = 24;
            }
            int iMax = Math.min(distLimit * 2, 48);
            for (int k = 1; k < distLimit; k++) {
                for (int i = 1; i < iMax; i++) {
                    double dx = i * Math.cos(Math.toRadians(this.getYRot() + 90.0f));
                    double dz = i * Math.sin(Math.toRadians(this.getYRot() + 90.0f));
                    BlockState scan =
                            this.level()
                                    .getBlockState(
                                            BlockPos.containing(
                                                    this.getX() + dx, this.getY() - k, this.getZ() + dz));
                    if (!scan.isAir()) {
                        obstruction_factor += 0.05;
                    }
                }
            }

            my += obstruction_factor * 0.07000000000000001;
            this.setPos(this.getX(), this.getY() + obstruction_factor * 0.07000000000000001, this.getZ());
            if (my > 2.0) {
                my = 2.0;
            }

            double d4 = pp.getYRot();
            d4 %= 360.0;
            while (d4 < 0.0) {
                d4 += 360.0;
            }
            double d5 = this.getYRot();
            d5 %= 360.0;
            while (d5 < 0.0) {
                d5 += 360.0;
            }
            relative_g = (d4 - d5) % 180.0;
            while (relative_g < 0.0) {
                relative_g += 180.0;
            }
            if (relative_g > 90.0) {
                relative_g -= 180.0;
            }

            if (velocity > 0.01) {
                d4 = 1.85 - velocity;
                d4 = Math.abs(d4);
                if (d4 < 0.01) {
                    d4 = 0.01;
                }
                if (d4 > 0.9) {
                    d4 = 0.9;
                }
                this.setYRot(pp.getYRot() + (float) (relative_g * d4));
            } else {
                this.setYRot(pp.getYRot());
            }
            relative_g = Math.abs(relative_g) * velocity;
            if (relative_g > 50.0) {
                relative_g = 0.0;
            }

            this.setXRot(2.0f * (float) velocity);
            this.setRot(this.getYRot(), this.getXRot());
            this.setYHeadRot(this.getYRot());

            double newvelocity = Math.sqrt(mx * mx + mz * mz);
            double im = pp.zza;

            boolean riderJumping =
                    pp instanceof LocalPlayer lp && lp.input.jumping || ChaosPersists.flyup_keystate != 0;
            if (riderJumping) {
                my += 0.035;
                my += velocity * 0.038;
            }

            if (Math.abs(im) > 0.001) {
                if (im > 0.0) {
                    deltav = 0.028;
                    if (max_speed > 1.0) {
                        deltav += 0.06;
                    }
                    if (this.deltasmooth < 0.0f) {
                        this.deltasmooth = 0.0f;
                    }
                    this.deltasmooth = (float) (this.deltasmooth + deltav / 10.0);
                    if (this.deltasmooth > deltav) {
                        this.deltasmooth = (float) deltav;
                    }
                } else {
                    max_speed = 0.35;
                    deltav = -0.02;
                    if (this.deltasmooth > 0.0f) {
                        this.deltasmooth = 0.0f;
                    }
                    this.deltasmooth = (float) (this.deltasmooth + deltav / 10.0);
                    if (this.deltasmooth < deltav) {
                        this.deltasmooth = (float) deltav;
                    }
                }

                newvelocity += this.deltasmooth;
                if (newvelocity >= 0.0) {
                    if (newvelocity > max_speed) {
                        newvelocity = max_speed;
                    }
                    mx = Math.cos(Math.toRadians(this.getYRot() + 90.0f)) * newvelocity;
                    mz = Math.sin(Math.toRadians(this.getYRot() + 90.0f)) * newvelocity;
                } else {
                    if (newvelocity < -max_speed) {
                        newvelocity = -max_speed;
                    }
                    newvelocity = -newvelocity;
                    mx = Math.cos(Math.toRadians(this.getYRot() + 270.0f)) * newvelocity;
                    mz = Math.sin(Math.toRadians(this.getYRot() + 270.0f)) * newvelocity;
                }
            } else if (newvelocity >= 0.0) {
                mx = Math.cos(Math.toRadians(this.getYRot() + 90.0f)) * newvelocity;
                mz = Math.sin(Math.toRadians(this.getYRot() + 90.0f)) * newvelocity;
            } else {
                mx = Math.cos(Math.toRadians(this.getYRot() + 270.0f)) * (newvelocity * -1.0);
                mz = Math.sin(Math.toRadians(this.getYRot() + 270.0f)) * (newvelocity * -1.0);
            }

            this.setDeltaMovement(mx, my, mz);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            dm = this.getDeltaMovement();
            this.setDeltaMovement(dm.x * 0.985, dm.y * 0.94, dm.z * 0.985);

            if (!this.level().isClientSide) {
                List<Entity> list =
                        this.level()
                                .getEntities(
                                        this,
                                        this.getBoundingBox().inflate(2.25, 2.0, 2.25),
                                        ent -> ent != pp && ent.isPushable() && ent.isAlive());
                for (Entity listEntity : list) {
                    listEntity.push(this);
                }
            }
            return;
        }
        this.setNoGravity(false);
        super.travel(travelVector);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide && this.getPassengers().isEmpty()) {
            this.finishDismountLanding();
        }
    }

    private void finishDismountLanding() {
        this.dismountCooldown = 80;
        this.owner_flying = 0;
        this.currentFlightTarget = null;
        if (MyUtils.isPrinceAirborne(this)) {
            this.setActivity(1);
            this.setNoGravity(true);
            this.noPhysics = true;
            return;
        }
        this.setActivity(0);
        this.setNoGravity(false);
        this.noPhysics = false;
        Vec3 dm = this.getDeltaMovement();
        this.setDeltaMovement(dm.x, Math.min(dm.y, -0.25), dm.z);
        MyUtils.enforceDragonMountGroundSafety(this);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.fallDistance = 0.0f;
    }

    protected boolean canTriggerWalking() {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(INT20, 0);
        this.entityData.define(INT21, 0);
        this.entityData.define(INT22, 0);
        this.setActivity(0);
        this.setAttacking(0);
        this.setBeingRidden(0);
        this.setTame(false);
        if (this.renderdata == null) {
            this.renderdata = new RenderInfo();
        }
        this.renderdata.rf1 = 0.0f;
        this.renderdata.rf2 = 0.0f;
        this.renderdata.rf3 = 0.0f;
        this.renderdata.rf4 = 0.0f;
        this.renderdata.ri1 = 0;
        this.renderdata.ri2 = 0;
        this.renderdata.ri3 = 0;
        this.renderdata.ri4 = 0;
    }

    public int mygetMaxHealth() {
        return 250;
    }

    public int getLeonHealth() {
        return (int) this.getHealth();
    }

    public RenderInfo getRenderInfo() {
        return this.renderdata;
    }

    public void setRenderInfo(RenderInfo r) {
        this.renderdata.rf1 = r.rf1;
        this.renderdata.rf2 = r.rf2;
        this.renderdata.rf3 = r.rf3;
        this.renderdata.rf4 = r.rf4;
        this.renderdata.ri1 = r.ri1;
        this.renderdata.ri2 = r.ri2;
        this.renderdata.ri3 = r.ri3;
        this.renderdata.ri4 = r.ri4;
    }

    @Override
    protected void jumpFromGround() {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.25, 0.0));
        super.jumpFromGround();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isInSittingPose()) {
            return null;
        }
        if (this.getActivity() == 1 && this.getPassengers().isEmpty()) {
            return ChaosSounds.LEON_LIVING;
        }
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ChaosSounds.LEON_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ChaosSounds.LEON_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.75f;
    }

    @Override
    public float getVoicePitch() {
        return 0.85f;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public int getArmorValue() {
        return 16;
    }

    private ItemStack dropItemRand(Item index, int par1) {
        ItemStack is = new ItemStack(index, par1);
        ItemEntity var3 =
                new ItemEntity(
                        this.level(),
                        this.getX()
                                + (double) ChaosPersists.ChaosRand.nextInt(5)
                                - (double) ChaosPersists.ChaosRand.nextInt(5),
                        this.getY() + 1.0,
                        this.getZ()
                                + (double) ChaosPersists.ChaosRand.nextInt(5)
                                - (double) ChaosPersists.ChaosRand.nextInt(5),
                        is);
        this.level().addFreshEntity(var3);
        return is;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        int i = 4 + this.getRandom().nextInt(6);
        int var4;
        for (var4 = 0; var4 < i; ++var4) {
            this.dropItemRand(Items.CHICKEN, 1);
        }
        i = 16 + this.getRandom().nextInt(6);
        for (var4 = 0; var4 < i; ++var4) {
            this.dropItemRand(Items.FEATHER, 1);
        }
        i = 2 + this.getRandom().nextInt(6);
        for (var4 = 0; var4 < i; ++var4) {
            if (ChaosPersists.KrakenRepellent != null) {
                this.dropItemRand(ChaosPersists.KrakenRepellent.asItem(), 1);
            }
        }
        if (this.getRandom().nextInt(5) == 1 && ChaosPersists.MyBattleAxe != null) {
            this.dropItemRand(ChaosPersists.MyBattleAxe, 1);
        }
    }

    @Override
    public boolean doHurtTarget(Entity par1Entity) {
        double ks = 1.25;
        double inair = 0.15;
        float iskraken = 1.0f;
        if (par1Entity instanceof EnderDragon dr) {
            if (this.getRandom().nextInt(6) == 1) {
                dr.hurt(this.damageSources().explosion(null), 55.0f);
            } else {
                dr.hurt(this.damageSources().explosion(null), 55.0f);
            }
            return true;
        }
        if (par1Entity instanceof LivingEntity living) {
            if (par1Entity instanceof Kraken) {
                iskraken = 4.0f;
            }
            living.hurt(this.damageSources().mobAttack(this), iskraken * 55.0f);
            float f3 = (float) Math.atan2(living.getZ() - this.getZ(), living.getX() - this.getX());
            if (!living.isAlive() || par1Entity instanceof Player) {
                inair *= 2.0;
            }
            living.push(Math.cos(f3) * ks, inair, Math.sin(f3) * ks);
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource par1DamageSource, float par2) {
        boolean ret = false;
        Entity e;
        if (this.hurt_timer > 0) {
            return false;
        }
        if (this.isInvulnerableTo(par1DamageSource)) {
            return false;
        }
        if (par1DamageSource.is(DamageTypes.IN_WALL)) {
            return ret;
        }
        if (!this.level().isClientSide) {
            this.setOrderedToSit(false);
        }
        if (!this.level().isClientSide) {
            this.setActivity(1);
        }
        e = par1DamageSource.getEntity();
        if (e instanceof Leon) {
            return false;
        }
        ret = super.hurt(par1DamageSource, par2);
        if (ret) {
            this.hurt_timer = 15;
        }
        if (e instanceof LivingEntity living && !this.level().isClientSide && MyUtils.isValidAggroTarget(living)) {
            if (this.isTame() && e instanceof Player) {
                return false;
            }
            this.setTarget(living);
            this.getNavigation().moveTo(living, 1.2);
            ret = true;
        }
        return ret;
    }

    @Override
    protected void customServerAiStep() {
        if (!this.getPassengers().isEmpty()) {
            return;
        }
        if (this.getActivity() != 0) {
            return;
        }
        super.customServerAiStep();
        LivingEntity e;
        if (!this.isInSittingPose()
                && this.getActivity() == 0
                && this.getPassengers().isEmpty()
                && this.level().getDifficulty() != Difficulty.PEACEFUL
                && this.getRandom().nextInt(10) == 1
                && (e = this.findSomethingToAttack()) != null) {
            this.setActivity(1);
        }
    }

    public void always_do() {
        Player p = null;
        LivingEntity e;
        if (!this.isInSittingPose()
                && this.getActivity() == 0
                && this.getPassengers().isEmpty()
                && this.level().getDifficulty() != Difficulty.PEACEFUL
                && this.getRandom().nextInt(10) == 1
                && (e = this.findSomethingToAttack()) != null) {
            this.setActivity(1);
        }
        if (this.getRandom().nextInt(250) == 1 && this.getHealth() < (float) this.mygetMaxHealth()) {
            this.heal(2.0f);
        }
        if (this.isInSittingPose()) {
            return;
        }
        this.owner_flying = 0;
        if (this.isTame()
                && this.getOwner() != null
                && this.getPassengers().isEmpty()
                && !this.isInSittingPose()) {
            p = (Player) this.getOwner();
            if (p.getAbilities().flying) {
                this.owner_flying = 1;
                this.setActivity(1);
            }
        }
        if (this.isTame()
                && this.getOwner() != null
                && !this.isInSittingPose()
                && this.distanceToSqr((p = (Player) this.getOwner())) > 400.0) {
            this.setActivity(1);
        }
        if (this.getRandom().nextInt(50) == 1
                && this.dismountCooldown == 0
                && !this.isInSittingPose()
                && !this.target_in_sight
                && this.getPassengers().isEmpty()) {
            if (MyUtils.isPrinceAirborne(this)) {
                this.setActivity(1);
            } else if (this.getRandom().nextInt(15) == 1) {
                this.setActivity(1);
            } else {
                this.setActivity(0);
            }
        }
    }

    private boolean shouldStopFlying() {
        if (this.dismountCooldown > 0 || this.target_in_sight || this.owner_flying != 0) {
            return false;
        }
        if (!MyUtils.isPrinceAirborne(this)) {
            return true;
        }
        if (this.getOwner() == null) {
            BlockPos feet = this.blockPosition();
            int groundY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, feet.getX(), feet.getZ());
            if (this.getY() <= groundY + 4.0) {
                return true;
            }
        }
        return false;
    }

    public void fly_with_rider() {
        LivingEntity e;
        int freq = 7;
        if (this.isDeadOrDying()) {
            return;
        }
        if (this.isInSittingPose()) {
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        if (this.getRandom().nextInt(freq) == 1 && this.level().getDifficulty() != Difficulty.PEACEFUL) {
            if (this.getRandom().nextInt(250) == 0) {
                this.setTarget(null);
            }
            if ((e = this.getTarget()) != null && !e.isAlive()) {
                this.setTarget(null);
                e = null;
            }
            if (e == null) {
                e = this.findSomethingToAttack();
            }
            if (e != null) {
                this.setAttacking(1);
                if (this.distanceToSqr(e)
                        < (double)
                                ((7.0f + e.getBbWidth() / 2.0f) * (7.0f + e.getBbWidth() / 2.0f))) {
                    this.doHurtTarget(e);
                }
                return;
            }
            this.setAttacking(0);
        }
    }

    private boolean isSuitableTarget(LivingEntity par1EntityLiving, boolean par2) {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (ChaosPersists.PlayNicely != 0) {
            return false;
        }
        if (par1EntityLiving == null) {
            return false;
        }
        if (par1EntityLiving == this) {
            return false;
        }
        if (!par1EntityLiving.isAlive()) {
            return false;
        }
        if (MyUtils.isIgnoreable(par1EntityLiving)) {
            return false;
        }
        if (!this.hasLineOfSight(par1EntityLiving)) {
            return false;
        }
        if (par1EntityLiving instanceof Leon) {
            return false;
        }
        if (par1EntityLiving instanceof Monster) {
            return true;
        }
        if (par1EntityLiving instanceof Player p) {
            if (p.isCreative()) {
                return false;
            }
            if (this.isTame()) {
                return false;
            }
            return true;
        }
        if (!this.isTame() && MyUtils.isAttackableNonMob(par1EntityLiving)) {
            return true;
        }
        return false;
    }

    private LivingEntity findSomethingToAttack() {
        if (ChaosPersists.PlayNicely != 0) {
            return null;
        }
        List<LivingEntity> var5 =
                this.level()
                        .getEntitiesOfClass(
                                LivingEntity.class, this.getBoundingBox().inflate(20.0, 20.0, 20.0));
        Collections.sort(var5, this.targetSorter);
        for (LivingEntity var4 : var5) {
            if (!this.isSuitableTarget(var4, false)) {
                continue;
            }
            return var4;
        }
        return null;
    }

    public boolean doesEntityNotTriggerPressurePlate() {
        return false;
    }

    public static boolean checkLeonSpawnRules(
            EntityType<Leon> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int k = -3; k < 3; ++k) {
            for (int j = -3; j < 3; ++j) {
                for (int i = 0; i < 5; ++i) {
                    checkPos.set(pos.getX() + j, pos.getY() + i, pos.getZ() + k);
                    if (MyUtils.getBlockStateForSpawnRules(level, checkPos).getBlock() != Blocks.SPAWNER) {
                        continue;
                    }
                    if (!(MyUtils.getBlockEntityForSpawnRules(level, checkPos) instanceof SpawnerBlockEntity spawner)) {
                        continue;
                    }
                    ResourceLocation id = SpawnerFixHelper.getMobSpawnerEntityIdFromBlockEntity(spawner);
                    if (id == null) {
                        continue;
                    }
                    ResourceLocation leonId =
                            ResourceLocation.fromNamespaceAndPath("chaospersists", "leonopteryx");
                    ResourceLocation norm = SpawnerFixHelper.normalizeSpawnerEntityId(id);
                    if (SpawnerFixHelper.entityIdsMatchForSpawner(norm, leonId)
                            || "Leonopteryx".equals(id.getPath())) {
                        return true;
                    }
                }
            }
        }
        if (random.nextInt(16) != 0) {
            return false;
        }
        if (!MyUtils.isDay(level)) {
            return false;
        }
        List<Leon> nearby =
                level.getLevel()
                        .getEntitiesOfClass(Leon.class, new AABB(pos).inflate(48.0, 16.0, 48.0));
        if (!nearby.isEmpty()) {
            return false;
        }
        if (pos.getY() < 50) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnReason) {
        if (this.getRandom().nextInt(16) != 0) {
            return false;
        }
        BlockPos pos = this.blockPosition();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int k = -3; k < 3; ++k) {
            for (int j = -3; j < 3; ++j) {
                for (int i = 0; i < 5; ++i) {
                    checkPos.set(pos.getX() + j, pos.getY() + i, pos.getZ() + k);
                    if (MyUtils.getBlockStateForSpawnRules(level, checkPos).getBlock() != Blocks.SPAWNER) {
                        continue;
                    }
                    if (!(MyUtils.getBlockEntityForSpawnRules(level, checkPos) instanceof SpawnerBlockEntity spawner)) {
                        continue;
                    }
                    ResourceLocation id = SpawnerFixHelper.getMobSpawnerEntityIdFromBlockEntity(spawner);
                    if (id == null) {
                        continue;
                    }
                    ResourceLocation leonId =
                            ResourceLocation.fromNamespaceAndPath("chaospersists", "leonopteryx");
                    ResourceLocation norm = SpawnerFixHelper.normalizeSpawnerEntityId(id);
                    if (SpawnerFixHelper.entityIdsMatchForSpawner(norm, leonId)
                            || "Leonopteryx".equals(id.getPath())) {
                        return true;
                    }
                }
            }
        }
        if (level instanceof Level world && !world.isDay()) {
            return false;
        }
        if (!this.level()
                .getEntitiesOfClass(Leon.class, this.getBoundingBox().inflate(48.0, 16.0, 48.0))
                .isEmpty()) {
            return false;
        }
        if (this.getY() < 50.0) {
            return false;
        }
        return true;
    }

    public boolean canSeeTarget(double pX, double pY, double pZ) {
        HitResult hit =
                this.level()
                        .clip(
                                new ClipContext(
                                        new Vec3(this.getX(), this.getY() + 0.75, this.getZ()),
                                        new Vec3(pX, pY, pZ),
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));
        return hit.getType() == HitResult.Type.MISS;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpTo(
            double par1, double par3, double par5, float par7, float par8, int par9, boolean interpolate) {
        this.boatPosRotationIncrements = par9;
        this.boatX = par1;
        this.boatY = par3;
        this.boatZ = par5;
        this.boatYaw = par7;
        this.boatPitch = par8;
        this.boatYawHead = par7;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpMotion(double par1, double par3, double par5) {
        super.lerpMotion(par1, par3, par5);
    }

    @Override
    public void tick() {
        LivingEntity e;
        if (this.dismountCooldown > 0) {
            --this.dismountCooldown;
        }
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double) this.moveSpeed);
        this.noPhysics = false;
        if (!this.level().isClientSide
                && this.getPassengers().isEmpty()
                && !this.isInSittingPose()
                && this.getActivity() == 0
                && MyUtils.isPrinceAirborne(this)) {
            this.setActivity(1);
        }
        if (!this.level().isClientSide
                && this.getActivity() != 0
                && this.getPassengers().isEmpty()
                && !this.isInSittingPose()) {
            this.fly_without_rider();
        }
        super.tick();
        if (this.hurt_timer > 0) {
            --this.hurt_timer;
        }
        if (this.getActivity() == 1) {
            ++this.wing_sound;
            if (this.wing_sound > 20) {
                if (!this.level().isClientSide && ChaosSounds.MOTHRA_WINGS != null) {
                    this.level()
                            .playSound(
                                    null,
                                    this.getX(),
                                    this.getY(),
                                    this.getZ(),
                                    ChaosSounds.MOTHRA_WINGS,
                                    SoundSource.NEUTRAL,
                                    0.5f,
                                    1.0f);
                }
                this.wing_sound = 0;
            }
        }
        if (this.isInWater()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.07, 0.0));
        }
        if (this.level().isClientSide) {
            return;
        }
        if (this.getActivity() == 0
                && this.isTame()
                && this.getOwner() != null
                && !this.isInSittingPose()
                && this.distanceToSqr((e = this.getOwner())) > 144.0) {
            this.setActivity(1);
        }
        MyUtils.enforceDragonMountGroundSafety(this);
        if (this.getPassengers().isEmpty()) {
            this.moveTo(this.getX(), this.getY(), this.getZ());
        }
    }

    private void fly_without_rider() {
        Vec3 dm = this.getDeltaMovement();
        double mx = dm.x;
        double my = dm.y;
        double mz = dm.z;
        Block bid;
        int xdir = 1;
        int zdir = 1;
        int keep_trying = 50;
        boolean do_new = false;
        double ox = 0.0;
        double oy = 0.0;
        double oz = 0.0;
        boolean has_owner = false;
        LivingEntity e = null;
        double speed_factor = 0.5;
        double var1 = 0.0;
        double var3 = 0.0;
        double var5 = 0.0;
        double yoff = 1.25;
        double xzoff = 2.25;
        double gh = 1.25;
        double obstruction_factor = 0.0;
        boolean toofar = false;
        if (this.currentFlightTarget == null) {
            do_new = true;
            this.currentFlightTarget = new BlockPos((int)this.getX(), (int)this.getY(), (int)this.getZ());
        }
        if (this.isInSittingPose()) {
            return;
        }
        if ((this.getPassengers().isEmpty() ? null : this.getPassengers().get(0)) != null) {
            return;
        }
        if (this.shouldStopFlying()) {
            this.setActivity(0);
            this.setNoGravity(false);
            this.noPhysics = false;
            this.currentFlightTarget = null;
            return;
        }
        if (this.unstick_timer > 0) {
            --this.unstick_timer;
        }
        if (this.lastX == (int)this.getX() && this.lastZ == (int)this.getZ()) {
            ++this.stuck_count;
            if (this.stuck_count > 50) {
                this.stuck_count = 0;
                this.unstick_timer = 100;
                this.target_in_sight = false;
                this.setAttacking(0);
                this.setActivity(1);
                do_new = true;
            }
        } else {
            this.stuck_count = 0;
            this.lastX = (int)this.getX();
            this.lastZ = (int)this.getZ();
        }
        if (this.getY() < (double) this.currentFlightTarget.getY() + 2.0) {
            my *= 0.7;
        } else if (this.getY() > (double) this.currentFlightTarget.getY() - 2.0) {
            my *= 0.5;
        } else {
            my *= 0.61;
        }
        if (this.getRandom().nextInt(300) == 1) {
            do_new = true;
        }
        if (this.isTame() && this.getOwner() != null) {
            e = this.getOwner();
            has_owner = true;
            ox = e.getX();
            oy = e.getY();
            oz = e.getZ();
            if (this.distanceToSqr(e) > 144.0) {
                toofar = true;
                this.target_in_sight = false;
                this.setAttacking(0);
                this.flyaway = 0;
                do_new = true;
            }
        }
        if (this.flyaway > 0) {
            --this.flyaway;
        }
        if (!toofar && this.unstick_timer == 0 && this.flyaway == 0 && this.level().getDifficulty() != Difficulty.PEACEFUL && this.getRandom().nextInt(8) == 1) {
            e = this.findSomethingToAttack();
            if (e != null) {
                if (this.isTame() && this.getHealth() / (float) this.mygetMaxHealth() < 0.25f) {
                    this.setActivity(1);
                    this.setAttacking(0);
                    this.target_in_sight = false;
                    do_new = false;
                    this.currentFlightTarget =
                            new BlockPos(
                                    (int) (this.getX() + (this.getX() - e.getX())),
                                    (int) (this.getY() + 1.0),
                                    (int) (this.getZ() + (this.getZ() - e.getZ())));
                } else {
                    this.setActivity(1);
                    this.setAttacking(1);
                    this.target_in_sight = true;
                    this.currentFlightTarget =
                            new BlockPos((int) e.getX(), (int) (e.getY() + 1.0), (int) e.getZ());
                    do_new = false;
                    if (this.distanceToSqr(e)
                            < (double)
                                    ((7.0f + e.getBbWidth() / 2.0f)
                                            * (7.0f + e.getBbWidth() / 2.0f))) {
                        this.doHurtTarget(e);
                    }
                }
            } else {
                this.target_in_sight = false;
                this.flyaway = 0;
                this.setAttacking(0);
            }
        }
        if (this.currentFlightTarget.distToCenterSqr(this.getX(), this.getY(), this.getZ()) < 2.1) {
            do_new = true;
        }
        if (do_new && !this.target_in_sight || do_new && this.flyaway != 0) {
            bid = Blocks.STONE;
            while (bid != Blocks.AIR && keep_trying != 0) {
                int gox = (int)this.getX();
                int goy = (int)this.getY();
                int goz = (int)this.getZ();
                if (has_owner && this.unstick_timer == 0) {
                    gox = (int)ox;
                    goy = (int)oy;
                    goz = (int)oz;
                    if (this.owner_flying == 0) {
                        zdir = this.getRandom().nextInt(12) + 6;
                        xdir = this.getRandom().nextInt(12) + 6;
                    } else {
                        zdir = this.getRandom().nextInt(8);
                        xdir = this.getRandom().nextInt(8);
                    }
                } else {
                    zdir = this.getRandom().nextInt(20) + 6;
                    xdir = this.getRandom().nextInt(20) + 6;
                }
                if (this.getRandom().nextInt(2) == 1) {
                    zdir = - zdir;
                }
                if (this.getRandom().nextInt(2) == 1) {
                    xdir = - xdir;
                }
                this.currentFlightTarget = new BlockPos(gox + xdir, goy + this.getRandom().nextInt(9 + this.owner_flying * 2) - 4, goz + zdir);
                bid = this.level().getBlockState(this.currentFlightTarget).getBlock();
                if (bid == Blocks.AIR && !this.canSeeTarget((double)this.currentFlightTarget.getX(), (double)this.currentFlightTarget.getY(), (double)this.currentFlightTarget.getZ())) {
                    bid = Blocks.STONE;
                }
                --keep_trying;
            }
        }
        obstruction_factor = 0.0;
        // Match ThePrinceTeen: horizontal speed for kMax after targeting/damping, not at method entry.
        double velocity = Math.sqrt(mx * mx + mz * mz);
        // Match ThePrinceTeen: small inner loop only — large scanDist*2 loops stacked huge Y boosts (random "rocket up").
        int kMax = 2 + (int)(Math.max(0.0, velocity) * 4.0);
        if (kMax < 2) {
            kMax = 2;
        }
        if (kMax > 24) {
            kMax = 24;
        }
        for (int k = 1; k < kMax; ++k) {
            for (int i = 1; i < 4; ++i) {
                double dz;
                double dx = (double)i * Math.cos(Math.toRadians(this.getYRot() + 90.0f));
                bid = this.level().getBlockState(new BlockPos((int)(this.getX() + dx), (int)this.getY() - k, (int)(this.getZ() + (dz = (double)i * Math.sin(Math.toRadians(this.getYRot() + 90.0f)))))).getBlock();
                if (bid == Blocks.AIR) continue;
                obstruction_factor += 0.05;
            }
        }
        my += obstruction_factor * 0.05;
        this.setPos(this.getX(), this.getY() + obstruction_factor * 0.05, this.getZ());
        speed_factor = 0.5;
        var1 = (double)this.currentFlightTarget.getX() + 0.5 - this.getX();
        var3 = (double)this.currentFlightTarget.getY() + 0.1 - this.getY();
        var5 = (double)this.currentFlightTarget.getZ() + 0.5 - this.getZ();
        if (this.owner_flying != 0) {
            speed_factor = 1.75;
            if (this.isTame() && this.getOwner() != null && this.distanceToSqr((e = this.getOwner())) > 49.0) {
                speed_factor = 3.5;
            }
        }
        mx += (Math.signum(var1) - mx) * 0.15 * speed_factor;
        my += (Math.signum(var3) - my) * 0.21 * speed_factor;
        mz += (Math.signum(var5) - mz) * 0.15 * speed_factor;
        float var7 = (float)(Math.atan2(mz, mx) * 180.0 / 3.141592653589793) - 90.0f;
        float var8 = Mth.wrapDegrees((float)(var7 - this.getYRot()));
        this.zza = (float) (0.75 * speed_factor);
        this.setYRot(this.getYRot() + var8 / 4.0f);
        this.setDeltaMovement(mx, my, mz);
        MyUtils.applyChaosFlightMovement(this);
    }

    @Override
    public void aiStep() {
        if (this.isDeadOrDying()) {
            super.aiStep();
            return;
        }
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.getActivity() != 0 && !this.getPassengers().isEmpty()) {
                Entity rider = this.getPassengers().get(0);
                if (rider instanceof LocalPlayer pp) {
                    pp.connection.send(
                            new ServerboundMovePlayerPacket.Rot(pp.getYRot(), pp.getXRot(), pp.onGround()));
                    pp.connection.send(
                            new ServerboundPlayerInputPacket(
                                    pp.xxa, pp.zza, pp.input.jumping, pp.input.shiftKeyDown));
                }
            }
            if (this.boatPosRotationIncrements > 0 && this.getActivity() != 0) {
                double d4 = this.getX() + (this.boatX - this.getX()) / (double) this.boatPosRotationIncrements;
                double d5 = this.getY() + (this.boatY - this.getY()) / (double) this.boatPosRotationIncrements;
                double d11 = this.getZ() + (this.boatZ - this.getZ()) / (double) this.boatPosRotationIncrements;
                this.setPos(d4, d5, d11);
                this.setXRot(
                        (float)
                                ((double) this.getXRot()
                                        + (this.boatPitch - (double) this.getXRot())
                                                / (double) this.boatPosRotationIncrements));
                double d10 = Mth.wrapDegrees(this.boatYaw - (double) this.getYRot());
                if (!this.getPassengers().isEmpty()) {
                    d10 =
                            Mth.wrapDegrees(
                                    (double) this.getPassengers().get(0).getYRot() - (double) this.getYRot());
                }
                this.setYRot((float) ((double) this.getYRot() + d10 / (double) this.boatPosRotationIncrements));
                this.setRot(this.getYRot(), this.getXRot());
                --this.boatPosRotationIncrements;
            }
        } else {
            if (this.getControllingPassenger() != null) {
                this.setBeingRidden(1);
            } else {
                this.setBeingRidden(0);
            }
            if (this.getActivity() != 0 && !this.getPassengers().isEmpty()) {
                this.fly_with_rider();
            }
            this.always_do();
        }
    }

    @Override
    public InteractionResult mobInteract(Player par1EntityPlayer, InteractionHand hand) {
        ItemStack var2 = par1EntityPlayer.getItemInHand(hand);
        if (!var2.isEmpty() && var2.getCount() <= 0) {
            par1EntityPlayer.setItemInHand(hand, ItemStack.EMPTY);
            var2 = ItemStack.EMPTY;
        }
        if (!var2.isEmpty()
                && var2.is(net.minecraft.world.item.Items.DIAMOND)
                && par1EntityPlayer.distanceToSqr(this) < 49.0) {
            if (!this.level().isClientSide) {
                this.setTame(true);
                this.setOwnerUUID(par1EntityPlayer.getUUID());
                spawnTamingParticles(true);
                this.level().broadcastEntityEvent(this, (byte) 7);
                this.heal((float) this.mygetMaxHealth() - this.getHealth());
            }
            if (!par1EntityPlayer.getAbilities().instabuild) {
                var2.shrink(1);
                if (var2.isEmpty()) {
                    par1EntityPlayer.setItemInHand(hand, ItemStack.EMPTY);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (!this.isTame()) {
            if (!var2.isEmpty() && var2.is(Items.BEEF) && par1EntityPlayer.distanceToSqr(this) < 49.0) {
                if (!this.level().isClientSide) {
                    if (this.getRandom().nextInt(3) == 1) {
                        this.setTame(true);
                        this.setOwnerUUID(par1EntityPlayer.getUUID());
                        spawnTamingParticles(true);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                        this.heal((float) this.mygetMaxHealth() - this.getHealth());
                    } else {
                        spawnTamingParticles(false);
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    }
                }
                if (!par1EntityPlayer.getAbilities().instabuild) {
                    var2.shrink(1);
                    if (var2.isEmpty()) {
                        par1EntityPlayer.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            if (!this.isOwnedBy(par1EntityPlayer)) {
                return InteractionResult.FAIL;
            }
            if (var2.isEmpty() && par1EntityPlayer.distanceToSqr(this) < 49.0) {
                if (!this.level().isClientSide) {
                    par1EntityPlayer.startRiding(this);
                    this.setActivity(1);
                    this.setOrderedToSit(false);
                }
                return InteractionResult.SUCCESS;
            }
            if (!var2.isEmpty() && var2.is(Items.BEEF) && par1EntityPlayer.distanceToSqr(this) < 49.0) {
                if (this.level().isClientSide) {
                    spawnTamingParticles(true);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                }
                if ((float) this.mygetMaxHealth() > this.getHealth()) {
                    this.heal((float) this.mygetMaxHealth() - this.getHealth());
                }
                if (!par1EntityPlayer.getAbilities().instabuild) {
                    var2.shrink(1);
                    if (var2.isEmpty()) {
                        par1EntityPlayer.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (!var2.isEmpty()
                    && var2.is(Blocks.DEAD_BUSH.asItem())
                    && par1EntityPlayer.distanceToSqr(this) < 49.0) {
                if (!this.level().isClientSide) {
                    this.setTame(false);
                    this.setOwnerUUID(null);
                    spawnTamingParticles(false);
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
                if (!par1EntityPlayer.getAbilities().instabuild) {
                    var2.shrink(1);
                    if (var2.isEmpty()) {
                        par1EntityPlayer.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (this.isTame()
                    && !var2.isEmpty()
                    && var2.is(Items.NAME_TAG)
                    && par1EntityPlayer.distanceToSqr(this) < 49.0
                    && this.isOwnedBy(par1EntityPlayer)) {
                this.setCustomName(var2.getHoverName());
                if (!par1EntityPlayer.getAbilities().instabuild) {
                    var2.shrink(1);
                    if (var2.isEmpty()) {
                        par1EntityPlayer.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (var2 != null
                    && par1EntityPlayer.distanceToSqr(this) < 49.0
                    && this.getPassengers().isEmpty()) {
                if (!this.isInSittingPose()) {
                    this.setOrderedToSit(true);
                    this.setActivity(0);
                } else {
                    this.setOrderedToSit(false);
                    this.setActivity(0);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    public boolean isWheat(ItemStack par1ItemStack) {
        return !par1ItemStack.isEmpty() && par1ItemStack.is(Items.BEEF);
    }

    public int getAttacking() {
        return this.entityData.get(INT20);
    }

    public void setAttacking(int par1) {
        if (this.level() != null && this.level().isClientSide) {
            return;
        }
        this.entityData.set(INT20, par1);
    }

    public int getActivity() {
        return this.entityData.get(INT21);
    }

    public void setActivity(int par1) {
        if (this.level() != null && this.level().isClientSide) {
            return;
        }
        this.entityData.set(INT21, par1);
    }

    public int getBeingRidden() {
        return this.entityData.get(INT22);
    }

    public void setBeingRidden(int par1) {
        if (this.level() != null && this.level().isClientSide) {
            return;
        }
        this.entityData.set(INT22, par1);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        if (this.isPersistenceRequired()) {
            return false;
        }
        if (!this.getPassengers().isEmpty()) {
            return false;
        }
        if (this.isTame()) {
            return false;
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LeonAttacking", this.getAttacking());
        tag.putInt("LeonActivity", this.getActivity());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAttacking(tag.getInt("LeonAttacking"));
        this.setActivity(tag.getInt("LeonActivity"));
    }
}

