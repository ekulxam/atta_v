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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WalkingCubeEntity extends Entity {

    public static final double SQAURED_DISTANCE_THRESHOLD = 50*50;

    protected final List<@NotNull TripodLeg> legs = new ArrayList<>();

    private boolean isPosNull;

    private boolean shouldAccelerateForward;
    private boolean shouldGoBackward;
    private boolean shouldTurnLeft;
    private boolean shouldTurnRight;

    protected final AtomicReference<@Nullable TripodLeg> activeLeg = new AtomicReference<>();

    public WalkingCubeEntity(EntityType<?> type, World world) {
        super(type, world);
        this.isPosNull = true;
        this.setInputs();
        this.initLegs();
    }

    @Override
    public void tick() {
        if (this.legs.isEmpty()) {
            this.initLegs();
        }
        Vec3d pos = this.getPos();
        if (isPosNull) {
            this.isPosNull = false;
            this.recalibrateLegs();
        }
        boolean logicalSide = this.isLogicalSideForUpdatingMovement();
        World world = this.getWorld();
        boolean client = world.isClient();
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
            PlayerEntity player = world.getClosestPlayer(this.getX(), this.getY(), this.getZ(), 64, true);
            if (player != null) {
                this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, player.getPos());
                if (logicalSide) {
                    this.activateLegs();
                }
            } else {
                this.setYaw(0);
                this.setPitch(0);
            }
        }
        this.legs.forEach(tripodLeg -> {
            tripodLeg.tick();
            if (tripodLeg.getPos().squaredDistanceTo(pos) > SQAURED_DISTANCE_THRESHOLD) {
                this.recalibrateLeg(tripodLeg, this.legs.indexOf(tripodLeg), pos, this.getYaw());
            }
        });
        double x = this.legs.stream().mapToDouble(TripodLeg::getX).average().orElse(this.getX());
        double y = findMedian(this.legs.stream().map(TripodLeg::getY).sorted(Double::compare).toList()) + 5;
        double z = this.legs.stream().mapToDouble(TripodLeg::getZ).average().orElse(this.getZ());
        Vec3d updatedPos = new Vec3d(x, y, z);
        if (this.getPos().squaredDistanceTo(updatedPos) > 1.0E-7) {
            this.setPosition(updatedPos);
            if (!client || logicalSide) {
                this.updateTrackedPosition(this.getX(), this.getY(), this.getZ());
            }
        }
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
        for (int i = 0; i < this.legs.size(); i++) {
            TripodLeg leg = this.legs.get(i);
            this.recalibrateLeg(leg, i, pos, yaw);
        }
    }

    public void recalibrateLeg(TripodLeg leg, int index, Vec3d pos, float yaw) {
        leg.setPosition(pos.add(this.getDesiredOffset(index, yaw)));
        leg.setVelocityKeepGravity();
    }

    public void activateLegs() {
        TripodLeg active = this.activeLeg.get();
        if (active == null) {
            TripodLeg legOne = this.legs.getFirst();
            this.activeLeg.set(legOne);
            active = legOne;
        }
        if (active.isOnGround()) {
            final double sizeMultiplier = Math.max(0, Math.log10((double) this.legs.size())) + 0.5;
            TripodLeg leg = this.getNextLeg(active);
            this.activeLeg.set(leg);
            final float yaw = this.getYaw();
            Vec3d newPos = this.getPos().add(fromYaw(yaw).normalize().multiply(3));
            newPos = newPos.add(this.getDesiredOffset(this.legs.indexOf(leg), yaw)).subtract(leg.getPos()).normalize();
            leg.setVelocity(new Vec3d(newPos.x * sizeMultiplier, 1, newPos.z * sizeMultiplier).multiply(0.8d));
        }
    }

    public Vec3d getDesiredOffset(int index, float yaw) {
        final float turn = 360f / this.legs.size();
        return fromYaw(yaw + index * turn).multiply(8f);
    }

    public TripodLeg getNextLeg(TripodLeg leg) {
        return this.legs.get((this.legs.indexOf(leg) + 1) % this.legs.size());
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
        int size = nbt.getInt("numberOfLegs");
        int active = -1;
        if (nbt.contains("activeLeg")) {
            active = nbt.getInt("activeLeg");
        }
        boolean dirty = false;
        if (size < this.legs.size()) {
            this.legs.clear();
        }
        while (size > this.legs.size()) {
            this.legs.add(new TripodLeg(this));
            dirty = true;
        }
        Vec3d pos = this.getPos();
        final float yaw = this.getYaw();
        for (int i = 0; i < size; i++) {
            TripodLeg leg = this.legs.get(i);
            String key = "leg" + i;
            if (nbt.contains(key)) {
                leg.readNbt(nbt.getCompound(key));
            }
            if (i == active) {
                this.activeLeg.set(leg);
            }
            if (dirty) {
                this.recalibrateLeg(leg, i, pos, yaw);
            }
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        this.writeLegDataToNbt(nbt);
    }

    protected void writeLegDataToNbt(NbtCompound nbt) {
        int size = this.legs.size();
        nbt.putInt("numberOfLegs", size);
        TripodLeg active = activeLeg.get();
        if (active != null) {
            nbt.putInt("activeLeg", this.legs.indexOf(active));
        }
        for (int i = 0; i < size; i++) {
            nbt.put("leg" + i, this.legs.get(i).writeNbt(new NbtCompound()));
        }
    }

    /**
     * The provided list should already be sorted
     */
    public static double findMedian(List<Double> list) {
        if (list.isEmpty()) {
            throw new IllegalStateException("The median of a list is undefined when the list is empty!");
        }
        int size = list.size();
        if (size % 2 == 0) {
            // 2, 4, 6, 8
            int sizeDiv2 = size / 2;
            return (list.get(sizeDiv2) - 1 + list.get(sizeDiv2)) / 2;
        }
        // 1, 3, 5, 7
        return list.get(Math.floorDiv(size, 2));
    }

    public List<LegRenderState> getLegsForRendering(final float tickDelta) {
        Vec3d pos = this.getPos();
        final float yaw = this.getYaw(tickDelta);
        int size = this.legs.size();
        final float turn = 360f / size;
        TripodLeg active = this.activeLeg.get();
        ImmutableList.Builder<LegRenderState> builder = ImmutableList.builder();
        for (int i = 0; i < size; i++) {
            TripodLeg leg = this.legs.get(i);
            Vec3d legPos = leg.getLerpedPos(tickDelta);
            if (legPos == null) {
                continue;
            }
            builder.add(new LegRenderState(pos.add(fromYaw(yaw + i * turn)), legPos, leg == active ? 0xFFFF0000 : 0xFF000000));
        }
        return builder.build();
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

    public void initLegs() {
        // start with three legs
        this.legs.add(new TripodLeg(this));
        this.legs.add(new TripodLeg(this));
        this.legs.add(new TripodLeg(this));
    }

    public record LegRenderState(Vec3d base, Vec3d end, int color) {

    }
}
