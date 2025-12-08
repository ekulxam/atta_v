package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import survivalblock.atmosphere.atmospheric_api.not_mixin.entity.ControlBoarder;
import survivalblock.atmosphere.atmospheric_api.not_mixin.util.PitchYawPair;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.entity.Inputs;
import survivalblock.atmosphere.atta_v.common.entity.paths.EntityPath;
import survivalblock.atmosphere.atta_v.common.entity.paths.EntityPathComponent;
import survivalblock.atmosphere.atta_v.common.entity.paths.Pathfinder;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityComponents;
import survivalblock.atmosphere.atta_v.common.networking.RideWandererS2CPayload;
import survivalblock.atmosphere.atta_v.common.networking.TripodLegUpdatePayload;
import survivalblock.atmosphere.atta_v.common.init.AttaVGameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class WalkingCubeEntity extends Entity implements ControlBoarder, Pathfinder {

    public static final double EPSILON = 1.0E-7;
    public static final double SQUARED_DISTANCE_THRESHOLD = 45*45;
    public static final int BODY_HEIGHT_OFFSET = 5;
    public static final Codec<List<Integer>> INT_LIST_CODEC = Codec.INT.listOf();

    protected final List<@NotNull TripodLeg> legs = new ArrayList<>();
    protected final List<@NotNull TripodLeg> activeLegs = new ArrayList<>();
    
    //protected final ClawOfLines claw = new ClawOfLines(this);
    protected final Inputs inputs = new Inputs();

    private boolean isPosNull;
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
        boolean logicalSide = this.isLogicalSideForUpdatingMovement();
        World world = this.getWorld();
        boolean client = world.isClient();
        Vec3d pos = this.getPos();
        if (isPosNull) {
            this.isPosNull = false;
            this.recalibrateLegs();
        }
        if (logicalSide) {
            this.syncNbt(world, true);
        }

        super.tick();
        LivingEntity controllingPassenger = this.getControllingPassenger();
        EntityPathComponent entityPathComponent = AttaVEntityComponents.ENTITY_PATH.get(this);
        this.targetPos = null;
        this.targetPlayer = null;

        if (controllingPassenger != null) {
            this.tickControlled(controllingPassenger, logicalSide);
        } else {
            this.setInputs();
            if (entityPathComponent.entityPath != null) {
                this.tickFollowPath(entityPathComponent, client, logicalSide);
            } else {
                this.tickFollowPlayer(world, logicalSide);
            }
        }

        float segmentFactor = Math.max(1, this.legs.size() / 5.0F);
        this.legs.forEach(tripodLeg -> {
            tripodLeg.setSegments(segmentFactor);
            tripodLeg.tick();
            Vec3d difference = tripodLeg.getPos().subtract(pos);
            if (difference.horizontalLengthSquared() > SQUARED_DISTANCE_THRESHOLD || Math.abs(difference.y) > 20) {
                this.recalibrateLeg(tripodLeg, this.legs.indexOf(tripodLeg), pos, this.getYaw());
            }
        });

        double x = this.legs.stream().mapToDouble(TripodLeg::getX).average().orElse(this.getX());
        double y = findMedian(this.legs.stream().map(TripodLeg::getY).sorted(Double::compare).toList()).map(aDouble -> aDouble + BODY_HEIGHT_OFFSET).orElseGet(this::getY);
        double z = this.legs.stream().mapToDouble(TripodLeg::getZ).average().orElse(this.getZ());
        Vec3d updatedPos = new Vec3d(x, y, z);
        if (this.getPos().squaredDistanceTo(updatedPos) > EPSILON) {
            this.setPosition(updatedPos);
            if (!client || logicalSide) {
                this.updateTrackedPosition(this.getX(), this.getY(), this.getZ());
            }
        }
        //this.claw.tick();
    }

    protected void tickControlled(LivingEntity controllingPassenger, boolean logicalSide) {
        this.tickRotation(getControlledRotation(controllingPassenger));
        if (logicalSide && this.inputs.shouldMove()) {
            this.activateLegs();
        }
    }

    protected void tickFollowPath(EntityPathComponent entityPathComponent, boolean client, boolean logicalSide) {
        //noinspection DataFlowIssue (this should never be null when called)
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
    }

    private void tickFollowPlayer(World world, boolean logicalSide) {
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
        if (world instanceof ServerWorld) {
            payload.sendS2C(this, null);
        } else if (syncClient) {
            payload.sendC2S();
        }
    }

    public void recalibrateLeg(TripodLeg leg, int index, Vec3d pos, float yaw) {
        leg.setPosition(pos.add(this.getDesiredOffset(index, yaw)));
        leg.setVelocityKeepGravity();
    }

    public void activateLegs() {
        if (this.legs.isEmpty()) {
            return;
        }

        this.resetActiveLegs();
        boolean next = true;
        for (TripodLeg leg : this.activeLegs) {
            if (!leg.isOnGround()) {
                next = false;
                break;
            }
        }

        if (next) {
            final double sizeMultiplier = Math.max(0, Math.log10(this.legs.size())) + 0.6;
            final float yaw = this.getYaw();
            Vec3d destination = this.getPos().add(fromYaw(yaw).multiply(10));
            List<TripodLeg> newActives = new ArrayList<>();
            for (TripodLeg original : this.activeLegs) {
                TripodLeg leg = this.getNextLeg(original);

                Vec3d direction = destination.add(this.getDesiredOffset(this.legs.indexOf(leg), yaw)).subtract(leg.getPos()).normalize();
                leg.setVelocity(new Vec3d(direction.x * sizeMultiplier, 1.5, direction.z * sizeMultiplier).multiply(0.92d)); // slightly faster than a sprinting player when it has three legs
                newActives.add(leg);
            }
            this.activeLegs.clear();
            this.activeLegs.addAll(newActives);
        }
    }

    private void resetActiveLegs() {
        int size = this.legs.size();
        int movingLegs = this.getNumberOfMovingLegs(size);

        if (size > 0 && this.activeLegs.size() != movingLegs) {
            this.activeLegs.clear();
            int mul = (int) Math.floor((float) size / movingLegs);
            for (int i = 1; i <= movingLegs; i++) {
                TripodLeg leg = this.legs.get(mul * i - 1);
                this.activeLegs.add(leg);
            }
        }
    }

    public int getNumberOfMovingLegs(int numberOfLegs) {
        return Math.max(1, (int) Math.floor((numberOfLegs - 1) / 2.0F));
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

    @SuppressWarnings({"RedundantMethodOverride", "RedundantSuppression"})
    @Override
    protected double getGravity() {
        return 0.0;
    }

    @Override
    public boolean doesRenderOnFire() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return true;
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
            if (this.rideable && world.getGameRules().getBoolean(AttaVGameRules.PLAYERS_CAN_RIDE_WANDERERS) && !this.isFollowingPath()) {
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

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.readLegDataFromNbt(nbt);
        /*if (nbt.contains("clawData")) {
            this.claw.readNbt(nbt.getCompound("clawData"));
        }*/
        if (nbt.contains("rideable")) {
            this.rideable = nbt.getBoolean("rideable");
        }
    }

    public void readLegDataFromNbt(NbtCompound nbt) {
        RegistryWrapper.WrapperLookup wrapperLookup = this.getRegistryManager();

        int size = nbt.contains("numberOfLegs") ? nbt.getInt("numberOfLegs") : this.legs.size();
        List<Integer> newActives = new ArrayList<>();
        if (nbt.contains("activeLegs")) {
            INT_LIST_CODEC.parse(wrapperLookup.getOps(NbtOps.INSTANCE), nbt.get("activeLegs"))
                    .resultOrPartial(error -> AttaV.LOGGER.error("Tried to load an invalid list of integers: '{}'", error))
                    .ifPresent(newActives::addAll);
        }

        boolean dirty = false;
        while (size < this.legs.size()) {
            this.legs.removeLast();
            dirty = true;
        }
        while (size > this.legs.size()) {
            this.legs.add(new TripodLeg(this));
            dirty = true;
        }

        this.activeLegs.clear();
        Vec3d pos = this.getPos();
        final float yaw = this.getYaw();
        for (int i = 0; i < size; i++) {
            TripodLeg leg = this.legs.get(i);
            String key = "leg" + i;
            if (nbt.contains(key)) {
                leg.readNbt(nbt.getCompound(key));
            }
            if (newActives.contains(i)) {
                this.activeLegs.add(leg);
            }
            if (dirty) {
                this.recalibrateLeg(leg, i, pos, yaw);
            }
        }

        if (this.activeLegs.isEmpty() && !this.legs.isEmpty()) {
            this.resetActiveLegs();
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        this.writeLegDataToNbt(nbt);
        //nbt.put("clawData", this.claw.writeNbt(new NbtCompound()));
        nbt.putBoolean("rideable", this.rideable);
    }

    protected void writeLegDataToNbt(NbtCompound nbt) {
        RegistryWrapper.WrapperLookup wrapperLookup = this.getRegistryManager();
        int size = this.legs.size();
        nbt.putInt("numberOfLegs", size);
        if (!this.activeLegs.isEmpty()) {
            nbt.put("activeLegs", INT_LIST_CODEC
                    .encodeStart(
                            wrapperLookup.getOps(NbtOps.INSTANCE),
                            this.activeLegs.stream().map(this.legs::indexOf).toList()
                    )
                    .getOrThrow()
            );
        }

        for (int i = 0; i < size; i++) {
            TripodLeg leg = this.legs.get(i);
            nbt.put("leg" + i, leg.writeNbt(new NbtCompound()));
        }
    }

    /**
     * The provided list should already be sorted
     */
    public static Optional<Double> findMedian(List<Double> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        }
        int size = list.size();
        if (size % 2 == 0) {
            // 2, 4, 6, 8
            int sizeDiv2 = size / 2;
            return Optional.of((list.get(sizeDiv2) - 1 + list.get(sizeDiv2)) / 2);
        }
        // 1, 3, 5, 7
        return Optional.of(list.get(Math.floorDiv(size, 2)));
    }

    @SuppressWarnings("CodeBlock2Expr")
    public List<Appendage.PositionColorContainer> getLegPositions(final float tickDelta) {
        return this.legs.stream()
                .map(leg -> {
                    return new Appendage.PositionColorContainer(
                            leg.getPositions(tickDelta),
                            this.activeLegs.contains(leg) ? 0xFFFF0000 : 0xFF000000
                    );
                })
                .toList();
    }

    public double getLegGravity() {
        return 0.08;
    }

    protected Vec2f getControlledRotation(LivingEntity controllingPassenger) {
        return new Vec2f(controllingPassenger.getPitch() * 0.5f, controllingPassenger.getYaw());
    }

    @SuppressWarnings("SameParameterValue")
    protected final void addRotation(float yaw, float pitch){
        this.setRotation(this.getYaw() + yaw, this.getPitch() + pitch);
    }

    protected void tickRotation(Vec2f rotation) {
        this.inputs.tickRotation(rotation, this::setRotation, this::addRotation, () -> new PitchYawPair(this.getPitch(), this.prevYaw), () -> this.setYaw(this.getYaw()));
    }

    @Override
    public void setInputs(boolean pressingLeft, boolean pressingRight, boolean pressingForward, boolean pressingBack){
        this.inputs.setInputs(pressingLeft, pressingRight, pressingForward, pressingBack);
    }

    public void initLegs() {
        // start with three legs
        this.legs.add(new TripodLeg(this));
        this.legs.add(new TripodLeg(this));
        this.legs.add(new TripodLeg(this));
        this.resetActiveLegs();
    }

    public Stream<BoxPosContainer> getLegBoundingBoxes(float tickDelta) {
        return this.legs.stream().map(leg -> new BoxPosContainer(leg.getBoundingBox(), leg.getPos(), leg.getLerpedPos(tickDelta)));
    }

    /*public ClawOfLines getClaw() {
        return this.claw;
    }*/

    @Override
    public void followPath(@Nullable EntityPath entityPath) {
        EntityPathComponent entityPathComponent = AttaVEntityComponents.ENTITY_PATH.get(this);
        entityPathComponent.entityPath = entityPath;
        entityPathComponent.nodeIndex = -1;
        entityPathComponent.sync();
    }

    @Override
    public boolean isFollowingPath() {
        return AttaVEntityComponents.ENTITY_PATH.get(this).entityPath != null;
    }

    @Override
    public @Nullable Identifier getPathId() {
        EntityPath path = AttaVEntityComponents.ENTITY_PATH.get(this).entityPath;
        return path == null ? null : path.id;
    }

    public record BoxPosContainer(Box boundingBox, Vec3d pos, Vec3d lerpedPos) {

    }
}
