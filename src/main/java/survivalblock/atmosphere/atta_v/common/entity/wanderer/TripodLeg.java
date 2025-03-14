package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.datagen.AttaVSoundEvents;
import survivalblock.atmosphere.atta_v.common.init.AttaVDamageTypes;
import survivalblock.atmosphere.atta_v.common.init.AttaVGameRules;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// the amalgamation that is entities
@SuppressWarnings("deprecation")
public class TripodLeg extends Appendage {

    public static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(0.5f, 0.1f);
    private static final Box NULL_BOX = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private Box boundingBox = NULL_BOX;
    private Vec3d velocity = Vec3d.ZERO;

    public TripodLeg(WalkingCubeEntity controller) {
        super(controller, 6, 400, true);
    }

    private boolean onGround;
    public double prevX;
    public double prevY;
    public double prevZ;
    private Vec3d pos;
    private BlockPos blockPos;
    private ChunkPos chunkPos;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean groundCollision;
    public boolean velocityModified;
    public float fallDistance;
    public double lastRenderX;
    public double lastRenderY;
    public double lastRenderZ;
    public boolean velocityDirty;
    private final double[] pistonMovementDelta = new double[]{0.0, 0.0, 0.0};
    private long pistonMovementTick;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Optional<BlockPos> supportingBlockPos = Optional.empty();
    private boolean forceUpdateSupportingBlockPos = false;

    final double xz = Math.sqrt(this.segmentLength) / 2.5;

    @SuppressWarnings("unused")
    public final void setPosition(Vec3d pos) {
        this.setPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    protected void resetPositions(List<Vec3d> list) {
        list.clear();
        Vec3d pos = this.getDesiredRootPosition();
        if (pos == null) {
            return;
        }
        Vec3d offset = this.controller.getDesiredOffset(this.controller.legs.indexOf(this), this.controller.getYaw()).normalize();
        for (int i = 0; i < this.segments; i++) {
            list.add(new Vec3d(pos.x + offset.x * xz * i, pos.y + i * 0.2, pos.z + offset.z * xz * i));
        }
    }

    public void setPosition(double x, double y, double z) {
        this.setPos(x, y, z);
        this.setBoundingBox(this.calculateBoundingBox());
    }

    protected Box calculateBoundingBox() {
        return DIMENSIONS.getBoxAt(this.pos);
    }

    protected void refreshPosition() {
        this.setPosition(this.pos.x, this.pos.y, this.pos.z);
    }

    @Override
    public void tick() {
        this.resetPosition();
        this.applyGravity();
        this.move(MovementType.SELF, this.getVelocity());

        if (!this.getWorld().isSpaceEmpty(this.getBoundingBox())) {
            this.pushOutOfBlocks(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
        }
        super.tick();
    }

    @Override
    protected @Nullable Vec3d getDesiredRootPosition() {
        return this.controller.getEyePos().lerp(this.controller.getPos(), 0.5);
    }

    @Override
    protected @Nullable Vec3d getDesiredEndPosition() {
        return this.getPos();
    }

    public void setOnGround(boolean onGround, Vec3d movement) {
        this.onGround = onGround;
        this.updateSupportingBlockPos(onGround, movement);
    }

    protected void updateSupportingBlockPos(boolean onGround, @Nullable Vec3d movement) {
        if (onGround) {
            Box box = this.getBoundingBox();
            Box box2 = new Box(box.minX, box.minY - 1.0E-6, box.minZ, box.maxX, box.minY, box.maxZ);
            Optional<BlockPos> optional = this.getWorld().findSupportingBlockPos(this.controller, box2);
            if (optional.isPresent() || this.forceUpdateSupportingBlockPos) {
                this.supportingBlockPos = optional;
            } else if (movement != null) {
                Box box3 = box2.offset(-movement.x, 0.0, -movement.z);
                optional = this.getWorld().findSupportingBlockPos(this.controller, box3);
                this.supportingBlockPos = optional;
            }

            this.forceUpdateSupportingBlockPos = optional.isEmpty();
        } else {
            this.forceUpdateSupportingBlockPos = false;
            if (this.supportingBlockPos.isPresent()) {
                this.supportingBlockPos = Optional.empty();
            }
        }
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void move(MovementType movementType, Vec3d movement) {
        if (movementType == MovementType.PISTON) {
            movement = this.adjustMovementForPiston(movement);
            if (movement.equals(Vec3d.ZERO)) {
                return;
            }
        }

        this.getWorld().getProfiler().push("move");

        Vec3d vec3d = this.adjustMovementForCollisions(movement);
        double d = vec3d.lengthSquared();
        if (d > 1.0E-7) {
            if (this.fallDistance != 0.0F && d >= 1.0) {
                BlockHitResult blockHitResult = this.getWorld()
                        .raycast(
                                new RaycastContext(this.getPos(), this.getPos().add(vec3d), RaycastContext.ShapeType.FALLDAMAGE_RESETTING, RaycastContext.FluidHandling.WATER, this.controller)
                        );
                if (blockHitResult.getType() != HitResult.Type.MISS) {
                    this.onLanding();
                }
            }

            this.setPosition(this.getX() + vec3d.x, this.getY() + vec3d.y, this.getZ() + vec3d.z);
        }

        this.getWorld().getProfiler().pop();
        this.getWorld().getProfiler().push("rest");
        boolean bl = !MathHelper.approximatelyEquals(movement.x, vec3d.x);
        boolean bl2 = !MathHelper.approximatelyEquals(movement.z, vec3d.z);
        this.horizontalCollision = bl || bl2;
        this.verticalCollision = movement.y != vec3d.y;
        this.groundCollision = this.verticalCollision && movement.y < 0.0;

        this.setOnGround(this.groundCollision, vec3d);
        BlockPos blockPos = this.getLandingPos();
        BlockState blockState = this.getWorld().getBlockState(blockPos);
        this.fall(vec3d.y, this.isOnGround(), blockState, blockPos);
        if (this.horizontalCollision) {
            Vec3d vec3d2 = this.getVelocity();
            this.setVelocity(bl ? 0.0 : vec3d2.x, vec3d2.y, bl2 ? 0.0 : vec3d2.z);
        }

        Block block = blockState.getBlock();
        if (movement.y != vec3d.y) {
            block.onEntityLand(this.getWorld(), this.controller);
        }

        if (this.isOnGround()) {
            block.onSteppedOn(this.getWorld(), blockPos, blockState, this.controller);
        }

        this.tryCheckBlockCollision();
        this.getWorld().getProfiler().pop();
    }

    protected void tryCheckBlockCollision() {
        this.checkBlockCollision();
    }

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public BlockPos getLandingPos() {
        return this.getPosWithYOffset(0.2F);
    }

    @SuppressWarnings("SameParameterValue")
    protected BlockPos getPosWithYOffset(float offset) {
        if (this.supportingBlockPos.isPresent()) {
            BlockPos blockPos = this.supportingBlockPos.get();
            if (!(offset > 1.0E-5F)) {
                return blockPos;
            } else {
                BlockState blockState = this.getWorld().getBlockState(blockPos);
                return (!(offset <= 0.5) || !blockState.isIn(BlockTags.FENCES)) && !blockState.isIn(BlockTags.WALLS) && !(blockState.getBlock() instanceof FenceGateBlock)
                        ? blockPos.withY(MathHelper.floor(this.pos.y - offset))
                        : blockPos;
            }
        } else {
            int i = MathHelper.floor(this.pos.x);
            int j = MathHelper.floor(this.pos.y - offset);
            int k = MathHelper.floor(this.pos.z);
            return new BlockPos(i, j, k);
        }
    }

    protected Vec3d adjustMovementForPiston(Vec3d movement) {
        if (movement.lengthSquared() <= 1.0E-7) {
            return movement;
        } else {
            long l = this.getWorld().getTime();
            if (l != this.pistonMovementTick) {
                Arrays.fill(this.pistonMovementDelta, 0.0);
                this.pistonMovementTick = l;
            }

            if (movement.x != 0.0) {
                double d = this.calculatePistonMovementFactor(Direction.Axis.X, movement.x);
                return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(d, 0.0, 0.0);
            } else if (movement.y != 0.0) {
                double d = this.calculatePistonMovementFactor(Direction.Axis.Y, movement.y);
                return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(0.0, d, 0.0);
            } else if (movement.z != 0.0) {
                double d = this.calculatePistonMovementFactor(Direction.Axis.Z, movement.z);
                return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(0.0, 0.0, d);
            } else {
                return Vec3d.ZERO;
            }
        }
    }

    private double calculatePistonMovementFactor(Direction.Axis axis, double offsetFactor) {
        int i = axis.ordinal();
        double d = MathHelper.clamp(offsetFactor + this.pistonMovementDelta[i], -0.51, 0.51);
        offsetFactor = d - this.pistonMovementDelta[i];
        this.pistonMovementDelta[i] = d;
        return offsetFactor;
    }

    private Vec3d adjustMovementForCollisions(Vec3d movement) {
        Box box = this.getBoundingBox();
        List<VoxelShape> list = this.getWorld().getEntityCollisions(this.controller, box.stretch(movement));
        return movement.lengthSquared() == 0.0 ? movement : adjustMovementForCollisions(this, movement, box, this.getWorld(), list);
    }

    public static Vec3d adjustMovementForCollisions(@NotNull TripodLeg tripod, Vec3d movement, Box entityBoundingBox, World world, List<VoxelShape> collisions) {
        List<VoxelShape> list = findCollisionsForMovement(tripod, world, collisions, entityBoundingBox.stretch(movement));
        return adjustMovementForCollisions(movement, entityBoundingBox, list);
    }

    private static List<VoxelShape> findCollisionsForMovement(
            @NotNull TripodLeg tripod, World world, List<VoxelShape> regularCollisions, Box movingEntityBoundingBox
    ) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(regularCollisions.size() + 1);
        if (!regularCollisions.isEmpty()) {
            builder.addAll(regularCollisions);
        }

        WorldBorder worldBorder = world.getWorldBorder();
        if (worldBorder.canCollide(tripod.controller, movingEntityBoundingBox)) {
            builder.add(worldBorder.asVoxelShape());
        }

        builder.addAll(world.getBlockCollisions(tripod.controller, movingEntityBoundingBox));
        return builder.build();
    }

    private static Vec3d adjustMovementForCollisions(Vec3d movement, Box entityBoundingBox, List<VoxelShape> collisions) {
        if (collisions.isEmpty()) {
            return movement;
        } else {
            double d = movement.x;
            double e = movement.y;
            double f = movement.z;
            if (e != 0.0) {
                e = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entityBoundingBox, collisions, e);
                if (e != 0.0) {
                    entityBoundingBox = entityBoundingBox.offset(0.0, e, 0.0);
                }
            }

            boolean bl = Math.abs(d) < Math.abs(f);
            if (bl && f != 0.0) {
                f = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, collisions, f);
                if (f != 0.0) {
                    entityBoundingBox = entityBoundingBox.offset(0.0, 0.0, f);
                }
            }

            if (d != 0.0) {
                d = VoxelShapes.calculateMaxOffset(Direction.Axis.X, entityBoundingBox, collisions, d);
                if (!bl && d != 0.0) {
                    entityBoundingBox = entityBoundingBox.offset(d, 0.0, 0.0);
                }
            }

            if (!bl && f != 0.0) {
                f = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, collisions, f);
            }

            return new Vec3d(d, e, f);
        }
    }

    protected void checkBlockCollision() {
        Box box = this.getBoundingBox();
        BlockPos blockPos = BlockPos.ofFloored(box.minX + 1.0E-7, box.minY + 1.0E-7, box.minZ + 1.0E-7);
        BlockPos blockPos2 = BlockPos.ofFloored(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7);
        if (this.getWorld().isRegionLoaded(blockPos, blockPos2)) {
            BlockPos.Mutable mutable = new BlockPos.Mutable();

            for (int i = blockPos.getX(); i <= blockPos2.getX(); i++) {
                for (int j = blockPos.getY(); j <= blockPos2.getY(); j++) {
                    for (int k = blockPos.getZ(); k <= blockPos2.getZ(); k++) {
                        mutable.set(i, j, k);
                        BlockState blockState = this.getWorld().getBlockState(mutable);

                        try {
                            blockState.onEntityCollision(this.getWorld(), mutable, this.controller);
                            this.onBlockCollision(blockState);
                        } catch (Throwable var12) {
                            CrashReport crashReport = CrashReport.create(var12, "Colliding TripodLeg with block");
                            CrashReportSection crashReportSection = crashReport.addElement("Block being collided with");
                            CrashReportSection.addBlockInfo(crashReportSection, this.getWorld(), mutable, blockState);
                            throw new CrashException(crashReport);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    protected void onBlockCollision(BlockState state) {
    }

    protected double getGravity() {
        return this.controller.getLegGravity();
    }

    public final double getFinalGravity() {
        return this.getGravity();
    }

    protected void applyGravity() {
        double d = this.getFinalGravity();
        if (d != 0.0) {
            Vec3d v = this.getVelocity();
            this.setVelocity(v.x, v.y -d, v.z);
        }
    }

    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
        if (onGround) {
            if (this.fallDistance > 0.0F) {
                World world = this.getWorld();
                state.getBlock().onLandedUpon(world, state, landedPosition, this.controller, this.fallDistance);
                world.emitGameEvent(
                                GameEvent.HIT_GROUND,
                                this.pos,
                                GameEvent.Emitter.of(this.controller, this.supportingBlockPos.map(blockPos -> this.getWorld().getBlockState(blockPos)).orElse(state))
                        );
                if (world instanceof ServerWorld serverWorld) handleFall(serverWorld);
            }
            this.onLanding();
        } else if (heightDifference < 0.0) {
            this.fallDistance -= (float)heightDifference;
        }
    }

    private void handleFall(ServerWorld world) {
        world.playSound(this.controller, this.blockPos,
                AttaVSoundEvents.WANDERER_LEG_LAND, this.controller.getSoundCategory(),
                1.2F, 1.0F);
        final double expand = 4;
        if (world.getGameRules().getBoolean(AttaVGameRules.WANDERER_STOMP_DOES_DAMAGE)) {
            // mace-like
            DamageSource source = new DamageSource(world.getRegistryManager().getWrapperOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(AttaVDamageTypes.WANDERER_STOMP), this.controller);
            world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(expand), living ->
                    living.isAlive() && !living.isInvulnerable() && !living.isSpectator() && !this.controller.equals(living.getRootVehicle()) && !living.isTeammate(this.controller)).forEach(living -> {
                living.damage(source, 3f);
                Vec3d vec3d = living.getPos().subtract(this.pos);
                double knockback = Math.max(0, (expand + 0.7 - vec3d.length())) * 1.4F
                        * (1.0 - living.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE));
                if (knockback > 0.0) {
                    Vec3d vec3d2 = vec3d.normalize().multiply(knockback);
                    living.addVelocity(vec3d2.x, 0.7F, vec3d2.z);
                    if (living instanceof ServerPlayerEntity serverPlayerEntity) {
                        serverPlayerEntity.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayerEntity));
                    }
                }
            });
        }
        BlockPos blockPos = this.getLandingPos();
        BlockState blockState = world.getBlockState(blockPos);
        Vec3d vec3d = blockPos.toCenterPos().add(0.0, 0.5, 0.0);
        int i = (int)MathHelper.clamp(50.0F * this.fallDistance, 0.0F, 200.0F);
        double particleDelta = expand / 2 - 1;
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), vec3d.x, vec3d.y, vec3d.z, i, particleDelta, particleDelta, particleDelta, 0.15F);
    }

    @Deprecated
    protected BlockState getLandingBlockState() {
        return this.getWorld().getBlockState(this.getLandingPos());
    }

    /**
     * Not to be confused with {@link Appendage#resetPositions(List)}
     */
    public final void resetPosition() {
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        this.prevX = d;
        this.prevY = e;
        this.prevZ = f;
        this.lastRenderX = d;
        this.lastRenderY = e;
        this.lastRenderZ = f;
    }

    @SuppressWarnings("unused")
    public void addVelocity(Vec3d velocity) {
        this.addVelocity(velocity.x, velocity.y, velocity.z);
    }

    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        this.setVelocity(this.getVelocity().add(deltaX, deltaY, deltaZ));
        this.velocityDirty = true;
    }

    @SuppressWarnings("unused")
    protected void scheduleVelocityUpdate() {
        this.velocityModified = true;
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        RegistryWrapper.WrapperLookup wrapperLookup = this.controller.getRegistryManager();
        nbt.put("velocity", Vec3d.CODEC.encodeStart(wrapperLookup.getOps(NbtOps.INSTANCE), this.velocity).getOrThrow());
        if (this.pos != null) {
            nbt.put("pos", Vec3d.CODEC.encodeStart(wrapperLookup.getOps(NbtOps.INSTANCE), this.pos).getOrThrow());
        }
        nbt.putFloat("FallDistance", this.fallDistance);
        nbt.putBoolean("OnGround", this.isOnGround());
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        RegistryWrapper.WrapperLookup wrapperLookup = this.controller.getRegistryManager();
        if (nbt.contains("velocity")) {
            this.velocity = Vec3d.CODEC.parse(wrapperLookup.getOps(NbtOps.INSTANCE), nbt.get("velocity"))
                    .resultOrPartial(error -> AttaV.LOGGER.error("Tried to load invalid Vec3d for velocity: '{}'", error))
                    .orElse(Vec3d.ZERO);
        } else {
            this.velocity = Vec3d.ZERO;
        }
        if (nbt.contains("pos")) {
            this.pos = Vec3d.CODEC.parse(wrapperLookup.getOps(NbtOps.INSTANCE), nbt.get("pos"))
                    .resultOrPartial(error -> AttaV.LOGGER.error("Tried to load invalid Vec3d for pos: '{}'", error))
                    .orElse(Vec3d.ZERO);
        } else {
            this.pos = Vec3d.ZERO;
        }
        this.resetPosition();
        this.onGround = nbt.getBoolean("OnGround");
        if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
            throw new IllegalStateException("TripodLeg has invalid position");
        } else {
            this.refreshPosition();
        }
    }

    public void onLanding() {
        this.fallDistance = 0.0F;
        this.setVelocityKeepGravity();
    }

    protected void pushOutOfBlocks(double x, double y, double z) {
        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        Vec3d vec3d = new Vec3d(x - blockPos.getX(), y - blockPos.getY(), z - blockPos.getZ());
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        Direction direction = Direction.UP;
        double d = Double.MAX_VALUE;

        for (Direction direction2 : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
            mutable.set(blockPos, direction2);
            if (!this.getWorld().getBlockState(mutable).isFullCube(this.getWorld(), mutable)) {
                double e = vec3d.getComponentAlongAxis(direction2.getAxis());
                double f = direction2.getDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - e : e;
                if (f < d) {
                    d = f;
                    direction = direction2;
                }
            }
        }

        float g = this.random.nextFloat() * 0.2F + 0.1F;
        float h = direction.getDirection().offset();
        Vec3d vec3d2 = this.getVelocity().multiply(0.75);
        if (direction.getAxis() == Direction.Axis.X) {
            this.setVelocity(h * g, vec3d2.y, vec3d2.z);
        } else if (direction.getAxis() == Direction.Axis.Y) {
            this.setVelocity(vec3d2.x, h * g, vec3d2.z);
        } else if (direction.getAxis() == Direction.Axis.Z) {
            this.setVelocity(vec3d2.x, vec3d2.y, h * g);
        }
    }

    public final Box getBoundingBox() {
        return this.boundingBox;
    }

    public final void setBoundingBox(Box boundingBox) {
        this.boundingBox = boundingBox;
    }

    public boolean isLogicalSideForUpdatingMovement() {
        return this.controller.isLogicalSideForUpdatingMovement();
    }

    public Vec3d getPos() {
        return this.pos;
    }

    public Vec3d getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vec3d velocity) {
        this.velocity = velocity;
    }

    public void setVelocityKeepGravity() {
        this.setVelocity(0, this.velocity.y, 0);
    }

    public void setVelocity(double x, double y, double z) {
        this.setVelocity(new Vec3d(x, y, z));
    }

    public final void setPos(double x, double y, double z) {
        // guess what? pos can be null now. Oops
        if (this.pos == null || this.pos.x != x || this.pos.y != y || this.pos.z != z) {
            this.pos = new Vec3d(x, y, z);
            int i = MathHelper.floor(x);
            int j = MathHelper.floor(y);
            int k = MathHelper.floor(z);
            if (this.blockPos == null || i != this.blockPos.getX() || j != this.blockPos.getY() || k != this.blockPos.getZ()) {
                this.blockPos = new BlockPos(i, j, k);
                if (this.chunkPos == null || ChunkSectionPos.getSectionCoord(i) != this.chunkPos.x || ChunkSectionPos.getSectionCoord(k) != this.chunkPos.z) {
                    this.chunkPos = new ChunkPos(this.blockPos);
                }
            }
        }
    }

    public World getWorld() {
        return this.controller.getWorld();
    }

    public final double getX() {
        return this.pos.x;
    }

    public final double getY() {
        return this.pos.y;
    }

    public final double getZ() {
        return this.pos.z;
    }

    public final Vec3d getLerpedPos(float delta) {
        Vec3d pos = this.pos;
        if (pos == null) {
            return null;
        }
        double d = MathHelper.lerp(delta, this.prevX, pos.x);
        double e = MathHelper.lerp(delta, this.prevY, pos.y);
        double f = MathHelper.lerp(delta, this.prevZ, pos.z);
        return new Vec3d(d, e, f);
    }
}
