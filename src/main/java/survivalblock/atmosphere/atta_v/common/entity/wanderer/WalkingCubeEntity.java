package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import survivalblock.atmosphere.atta_v.common.entity.ControlBoarder;
import survivalblock.atmosphere.atta_v.common.entity.paths.EntityPath;
import survivalblock.atmosphere.atta_v.common.entity.paths.EntityPathComponent;
import survivalblock.atmosphere.atta_v.common.entity.paths.Pathfinder;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityComponents;
import survivalblock.atmosphere.atta_v.common.networking.RideWandererS2CPayload;
import survivalblock.atmosphere.atta_v.common.networking.TripodLegUpdatePayload;
import survivalblock.atmosphere.atta_v.common.init.AttaVGameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class WalkingCubeEntity extends Entity implements ControlBoarder, Pathfinder {

    public static final double SQAURED_DISTANCE_THRESHOLD = 45*45;
    public static final int BODY_HEIGHT_OFFSET = 5;

    protected final List<@NotNull TripodLeg> legs = new ArrayList<>();
    protected final ClawOfLines claw = new ClawOfLines(this);

    private boolean isPosNull;

    private boolean shouldAccelerateForward;
    private boolean shouldGoBackward;
    private boolean shouldTurnLeft;
    private boolean shouldTurnRight;

    protected final AtomicReference<@Nullable TripodLeg> activeLeg = new AtomicReference<>();

    protected @Nullable Vec3d targetPos;
    protected @Nullable PlayerEntity targetPlayer;

    protected boolean rideable = true;

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
            this.syncNbt(world, true);
        }
        super.tick();
        LivingEntity controllingPassenger = this.getControllingPassenger();
        EntityPathComponent entityPathComponent = AttaVEntityComponents.ENTITY_PATH.get(this);
        this.targetPos = null;
        this.targetPlayer = null;
        if (controllingPassenger != null) {
            this.tickRotation(getControlledRotation(controllingPassenger));
            if (logicalSide && (this.shouldAccelerateForward || this.shouldGoBackward || this.shouldTurnRight || this.shouldTurnLeft)) {
                this.activateLegs();
            }
        } else {
            this.setInputs();
            if (entityPathComponent.entityPath != null) {
                List<Vec3d> nodes = entityPathComponent.entityPath.nodes;
                int size = nodes.size();
                if (entityPathComponent.nodeIndex < 0 || entityPathComponent.nodeIndex > size - 1) {
                    entityPathComponent.nodeIndex = 0;
                }
                Vec3d target = nodes.get(entityPathComponent.nodeIndex);
                if (this.targetPos == null) {
                    this.targetPos = target;
                }
                if (target.squaredDistanceTo(this.getPos()) < 56) {
                    entityPathComponent.nodeIndex = (entityPathComponent.nodeIndex + 1) % size;
                    if (!client) {
                        entityPathComponent.sync();
                    }
                }
                this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos);
                if (logicalSide) {
                    this.activateLegs();
                }
            } else {
                PlayerEntity player = null;
                if (world.getGameRules().getBoolean(AttaVGameRules.WANDERER_SEEKS_OUT_PLAYERS)) {
                    player = world.getClosestPlayer(this.getX(), this.getY(), this.getZ(), 96,
                            entity -> entity.isAlive() && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity)
                                    && this != entity.getRootVehicle() && !entity.isTeammate(this));
                }
                if (player != null) {
                    this.targetPlayer = player;
                    this.targetPos = player.getPos();
                    this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos);
                    if (logicalSide && player.squaredDistanceTo(this.getEyePos()) > 8) {
                        this.activateLegs();
                    }
                } else {
                    this.setYaw(0);
                    this.setPitch(0);
                }
            }
        }
        this.legs.forEach(tripodLeg -> {
            tripodLeg.tick();
            if (tripodLeg.getPos().squaredDistanceTo(pos) > SQAURED_DISTANCE_THRESHOLD) {
                this.recalibrateLeg(tripodLeg, this.legs.indexOf(tripodLeg), pos, this.getYaw());
            }
        });
        double x = this.legs.stream().mapToDouble(TripodLeg::getX).average().orElse(this.getX());
        double y = findMedian(this.legs.stream().map(TripodLeg::getY).sorted(Double::compare).toList()) + BODY_HEIGHT_OFFSET;
        double z = this.legs.stream().mapToDouble(TripodLeg::getZ).average().orElse(this.getZ());
        Vec3d updatedPos = new Vec3d(x, y, z);
        if (this.getPos().squaredDistanceTo(updatedPos) > 1.0E-7) {
            this.setPosition(updatedPos);
            if (!client || logicalSide) {
                this.updateTrackedPosition(this.getX(), this.getY(), this.getZ());
            }
        }
        this.claw.tick();
    }

    @SuppressWarnings({"RedundantMethodOverride", "RedundantSuppression"})
    @Override
    protected double getGravity() {
        return 0.0;
    }

    public static Vec3d fromYaw(float yaw) {
        return Vec3d.fromPolar(0, yaw).normalize();
    }

    public void recalibrateLegs() {
        this.recalibrateLegs(false);
    }

    public void recalibrateLegs(boolean sync) {
        Vec3d pos = this.getPos();
        float yaw = this.getYaw();
        for (int i = 0; i < this.legs.size(); i++) {
            TripodLeg leg = this.legs.get(i);
            this.recalibrateLeg(leg, i, pos, yaw);
        }
        if (sync) {
            this.syncNbt(this.getWorld(), false);
        }
    }

    private void syncNbt(World world, boolean syncClient) {
        NbtCompound nbt = new NbtCompound();
        this.writeLegDataToNbt(nbt);
        TripodLegUpdatePayload payload = new TripodLegUpdatePayload(this.getId(), nbt);
        if (world instanceof ServerWorld serverWorld) {
            payload.sendS2C(serverWorld, this, null);
        } else if (syncClient) {
            payload.sendC2S();
        }
    }

    public void recalibrateLeg(TripodLeg leg, int index, Vec3d pos, float yaw) {
        leg.setPosition(pos.add(this.getDesiredOffset(index, yaw)));
        leg.setVelocityKeepGravity();
    }

    public void activateLegs() {
        TripodLeg active = this.resetActiveLeg();
        if (active.isOnGround()) {
            final double sizeMultiplier = Math.max(0, Math.log10(this.legs.size())) + 0.6;
            TripodLeg leg = this.getNextLeg(active);
            this.activeLeg.set(leg);
            final float yaw = this.getYaw();
            Vec3d newPos = this.getPos().add(fromYaw(yaw).multiply(8));
            newPos = newPos.add(this.getDesiredOffset(this.legs.indexOf(leg), yaw)).subtract(leg.getPos()).normalize();
            leg.setVelocity(new Vec3d(newPos.x * sizeMultiplier, 1.5, newPos.z * sizeMultiplier).multiply(0.92d)); // slightly faster than a sprinting player when it has three legs
        }
    }

    @NotNull
    private TripodLeg resetActiveLeg() {
        TripodLeg active = this.activeLeg.get();
        if (active == null) {
            TripodLeg legOne = this.legs.getFirst();
            this.activeLeg.set(legOne);
            active = legOne;
        }
        return active;
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
        if (nbt.contains("clawData")) {
            this.claw.readNbt(nbt.getCompound("clawData"));
        }
        if (nbt.contains("rideable")) {
            this.rideable = nbt.getBoolean("rideable");
        }
    }

    public void readLegDataFromNbt(NbtCompound nbt) {
        int size = nbt.getInt("numberOfLegs");
        int active = -1;
        if (nbt.contains("activeLeg")) {
            active = nbt.getInt("activeLeg");
        }
        this.activeLeg.set(null);
        boolean dirty = false;
        while (size < this.legs.size()) {
            this.legs.removeLast();
            dirty = true;
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
        if (this.activeLeg.get() == null && !this.legs.isEmpty()) {
            this.resetActiveLeg();
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        this.writeLegDataToNbt(nbt);
        nbt.put("clawData", this.claw.writeNbt(new NbtCompound()));
        nbt.putBoolean("rideable", this.rideable);
    }

    protected void writeLegDataToNbt(NbtCompound nbt) {
        int size = this.legs.size();
        nbt.putInt("numberOfLegs", size);
        TripodLeg active = activeLeg.get();
        if (active != null) {
            nbt.putInt("activeLeg", this.legs.indexOf(active));
        }
        for (int i = 0; i < size; i++) {
            TripodLeg leg = this.legs.get(i);
            nbt.put("leg" + i, leg.writeNbt(new NbtCompound()));
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

    public List<Appendage.PositionColorContainer> getLegPositions(final float tickDelta) {
        return this.legs.stream().map(leg -> new Appendage.PositionColorContainer(leg.getPositions(tickDelta), leg == this.activeLeg.get() ? 0xFFFF0000 : 0xFF000000)).toList();
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    public double getLegGravity() {
        return 0.08;
    }

    @Override
    public boolean canHit() {
        return !(this.getControllingPassenger() instanceof PlayerEntity);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.hasPassengers() || this.isRemoved()) {
            return super.interact(player, hand);
        }
        World world = this.getWorld();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (this.rideable && world.getGameRules().getBoolean(AttaVGameRules.PLAYERS_CAN_RIDE_WANDERERS)) {
                serverPlayer.startRiding(this);
                ServerPlayNetworking.send(serverPlayer, new RideWandererS2CPayload(this));
            }
        }
        return ActionResult.success(world.isClient);
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
        this.resetActiveLeg();
    }

    public Stream<BoxPosContainer> getLegBoundingBoxes(float tickDelta) {
        return this.legs.stream().map(leg -> new BoxPosContainer(leg.getBoundingBox(), leg.getPos(), leg.getLerpedPos(tickDelta)));
    }

    public ClawOfLines getClaw() {
        return this.claw;
    }

    @Override
    public void followPath(@Nullable EntityPath entityPath) {
        EntityPathComponent entityPathComponent = AttaVEntityComponents.ENTITY_PATH.get(this);
        entityPathComponent.entityPath = entityPath;
        entityPathComponent.nodeIndex = -1;
        entityPathComponent.sync();
    }

    @Override
    public boolean doesRenderOnFire() {
        return false;
    }

    public record BoxPosContainer(Box boundingBox, Vec3d pos, Vec3d lerpedPos) {

    }
}
