package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import com.google.common.collect.ImmutableList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import survivalblock.atmosphere.atta_v.common.TripodLegUpdatePayload;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WalkingCubeEntity extends Entity {

    public static final double SQAURED_DISTANCE_THRESHOLD = 9801;
    public static final double LEG_VELOCITY_MULTIPLIER = 0.8f;

    private final TripodLeg legOne = new TripodLeg(this);
    private final TripodLeg legTwo = new TripodLeg(this);
    private final TripodLeg legThree = new TripodLeg(this);

    private boolean isPosNull;

    private boolean shouldAccelerateForward;
    private boolean shouldGoBackward;
    private boolean shouldTurnLeft;
    private boolean shouldTurnRight;

    private final AtomicReference<TripodLeg> activeLeg = new AtomicReference<>(legOne);

    public WalkingCubeEntity(EntityType<?> type, World world) {
        super(type, world);
        this.isPosNull = true;
        this.setInputs();
    }

    @Override
    public void tick() {
        if (isPosNull) {
            Vec3d pos = this.getPos();
            if (pos != null) {
                this.isPosNull = false;
                this.recalibrateLegs();
            }
        }
        boolean logicalSide = this.isLogicalSideForUpdatingMovement();
        World world = this.getWorld();
        if (logicalSide) {
            NbtCompound nbt = new NbtCompound();
            this.writeLegDataToNbt(nbt);
            TripodLegUpdatePayload payload = new TripodLegUpdatePayload(this.getId(), nbt);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.getPlayers().forEach(player -> {
                    if (player.distanceTo(this) < 128) {
                        ServerPlayNetworking.send(player, payload);
                    }
                });
            } else {
                ClientPlayNetworking.send(payload);
            }
        }
        super.tick();
        LivingEntity controllingPassenger = this.getControllingPassenger();
        if (controllingPassenger != null) {
            this.tickRotation(getControlledRotation(controllingPassenger));
            if (logicalSide && (this.shouldAccelerateForward || this.shouldGoBackward || this.shouldTurnRight || this.shouldTurnLeft)) {
                this.activateLegs();
            }
        } else {
            this.setInputs();
            PlayerEntity player = world.getClosestPlayer(this.getX(), this.getY(), this.getZ(), 32, false);
            if (player != null) {
                this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, player.getPos());
                if (logicalSide) {
                    this.activateLegs();
                }
            }
        }
        legOne.tick();
        legTwo.tick();
        legThree.tick();
        Vec3d one = legOne.getPos();
        Vec3d two = legTwo.getPos();
        Vec3d three = legThree.getPos();
        if (one.squaredDistanceTo(two) > SQAURED_DISTANCE_THRESHOLD
                || two.squaredDistanceTo(three) > SQAURED_DISTANCE_THRESHOLD
                || three.squaredDistanceTo(one) > SQAURED_DISTANCE_THRESHOLD) {
            this.recalibrateLegs();
        }
        this.setPosition(avg(one.x, two.x, three.x), getMiddle(one.y, two.y, three.y) + 5, avg(one.z, two.z, three.z));
    }

    public void resetLegVelocities() {
        legOne.setVelocityKeepGravity();
        legTwo.setVelocityKeepGravity();
        legThree.setVelocityKeepGravity();
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    protected double getGravity() {
        return 0.0;
    }

    public static Vec3d fromYaw(float yaw) {
        return Vec3d.fromPolar(0, yaw).normalize();
    }

    public void recalibrateLegs() {
        Vec3d pos = this.getPos();
        float yaw = this.getYaw();
        this.legOne.setPosition(pos.add(this.getDesiredOffset(legOne, yaw)));
        this.legTwo.setPosition(pos.add(this.getDesiredOffset(legTwo, yaw)));
        this.legThree.setPosition(pos.add(this.getDesiredOffset(legThree, yaw)));
        this.resetLegVelocities();
    }

    public void activateLegs() {
        TripodLeg active = this.activeLeg.get();
        if (active == null) {
            this.activeLeg.set(this.legOne);
            active = legOne;
        }
        if (active.isOnGround()) {
            TripodLeg leg = this.getNextLeg(active);
            this.activeLeg.set(leg);
            final float yaw = this.getYaw();
            Vec3d newPos = this.getPos().add(fromYaw(yaw).normalize().multiply(3));
            newPos = newPos.add(this.getDesiredOffset(leg, yaw)).subtract(leg.getPos()).normalize();
            leg.setVelocity(new Vec3d(newPos.x * LEG_VELOCITY_MULTIPLIER, 0.8, newPos.z * LEG_VELOCITY_MULTIPLIER));
        }
    }

    public Vec3d getDesiredOffset(TripodLeg leg, float yaw) {
        Vec3d offset;
        if (leg == legOne) {
            offset = fromYaw(yaw);
        } else if (leg == legTwo) {
            offset = fromYaw(yaw + 120);
        } else if (leg == legThree) {
            offset = fromYaw(yaw - 120);
        } else {
            offset = new Vec3d(0, 0, 0);
        }
        return offset.multiply(8f);
    }

    public TripodLeg getNextLeg(@NotNull TripodLeg leg) {
        if (leg == legOne) {
            return legTwo;
        }
        if (leg == legTwo) {
            return legThree;
        }
        if (leg == legThree) {
            return legOne;
        }
        throw new IllegalArgumentException("TripodLeg " + leg + " was not found!");
    }

    @Override
    protected boolean canStartRiding(Entity entity) {
        return false;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.readLegDataFromNbt(nbt);
    }

    public void readLegDataFromNbt(NbtCompound nbt) {
        this.legOne.readNbt(nbt.getCompound("legOne"));
        this.legTwo.readNbt(nbt.getCompound("legTwo"));
        this.legThree.readNbt(nbt.getCompound("legThree"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        this.writeLegDataToNbt(nbt);
    }

    protected void writeLegDataToNbt(NbtCompound nbt) {
        nbt.put("legOne", this.legOne.writeNbt(new NbtCompound()));
        nbt.put("legTwo", this.legTwo.writeNbt(new NbtCompound()));
        nbt.put("legThree", this.legThree.writeNbt(new NbtCompound()));
    }

    public static double avg(double a, double b, double c) {
        return (a + b + c) / 3d;
    }

    public double getMiddle(double a, double b, double c) {
        if ((a >= b && b >= c) || (a <= b && b <= c)) {
            return b;
        }
        if ((b >= a && a >= c) || (b <= a && a <= c)) {
            return a;
        }
        return c;
    }

    public List<BaseEndPair> getLegPositions(final float tickDelta) {
        Vec3d one = legOne.getLerpedPos(tickDelta);
        Vec3d two = legTwo.getLerpedPos(tickDelta);
        Vec3d three = legThree.getLerpedPos(tickDelta);
        if (one == null || two == null || three == null) {
            return ImmutableList.of();
        }
        Vec3d pos = this.getLerpedPos(tickDelta);
        final float yaw = this.getYaw(tickDelta);
        BaseEndPair first = new BaseEndPair(pos.add(fromYaw(yaw)), one);
        BaseEndPair second = new BaseEndPair(pos.add(fromYaw(yaw + 120)), two);
        BaseEndPair third = new BaseEndPair(pos.add(fromYaw(yaw - 120)), three);
        return ImmutableList.of(first, second, third);
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    public double getLegGravity() {
        return 0.05;
    }

    @Override
    public boolean canHit() {
        return !(this.getControllingPassenger() instanceof PlayerEntity);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.hasPassengers()) {
            return super.interact(player, hand);
        }
        player.startRiding(this);
        return ActionResult.success(this.getWorld().isClient);
    }

    /**
     * For implementation purposes, always returns a {@link PlayerEntity} or null
     * @return {@link #getFirstPassenger()} if it is a {@link PlayerEntity}
     * @see Entity#getControllingPassenger()
     */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity entity = this.getFirstPassenger();
        return entity instanceof PlayerEntity player && player.shouldControlVehicles() ? player : null;
    }

    protected Vec2f getControlledRotation(LivingEntity controllingPassenger) {
        return new Vec2f(controllingPassenger.getPitch() * 0.5f, controllingPassenger.getYaw());
    }

    private void tickRotation(Vec2f rotation) {
        this.setRotation(rotation.y, rotation.x);
        if (this.shouldTurnRight) {
            this.addRotation(90.0f, 0);
            if (this.shouldAccelerateForward) {
                this.addRotation(-45.0f, 0);
            }
            if (this.shouldGoBackward) {
                this.addRotation(45.0f, 0);
            }
        } else if (this.shouldTurnLeft) {
            this.addRotation(-90.0f, 0);
            if (this.shouldAccelerateForward) {
                this.addRotation(45.0f, 0);
            }
            if (this.shouldGoBackward) {
                this.addRotation(-45.0f, 0);
            }
        } else if (this.shouldGoBackward) {
            this.addRotation(180.0f, 0);
        } else if (!this.shouldAccelerateForward) {
            this.setRotation(this.prevYaw, this.getPitch());
        }
        this.setYaw(this.getYaw());
        this.prevYaw = this.getYaw();
    }

    public void setInputs(){
        this.setInputs(false, false, false, false);
    }

    public void setInputs(boolean pressingLeft, boolean pressingRight, boolean pressingForward, boolean pressingBack){
        this.shouldAccelerateForward = pressingForward;
        if (pressingForward) {
            this.shouldGoBackward = false;
        } else {
            this.shouldGoBackward = pressingBack;
        }
        if (pressingLeft && pressingRight) {
            this.shouldTurnLeft = false;
            this.shouldTurnRight = false;
        } else {
            this.shouldTurnLeft = pressingLeft;
            this.shouldTurnRight = pressingRight;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void addRotation(float yaw, float pitch){
        this.setRotation(this.getYaw() + yaw, this.getPitch() + pitch);
    }

    public record BaseEndPair(Vec3d base, Vec3d end) {

    }
}
