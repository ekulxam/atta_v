package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.init.AttaVGameRules;

import java.util.List;

public class ClawOfLines extends Appendage implements PositionContainer {

    private @Nullable Vec3d targetPosition = null;
    private @Nullable PlayerEntity target = null;
    private boolean grab = false;
    private int grabTicks = 0;

    public ClawOfLines(WalkingCubeEntity controller) {
        super(controller, 60, 0.3, false);
    }

    public void tick() {
        if (this.controller.targetPos != null) {
            if (this.targetPosition == null) {
                this.targetPosition = this.controller.targetPos;
            } else {
                this.targetPosition = this.targetPosition.lerp(this.controller.targetPos, 0.05);
            }
        }
        super.tick();
        this.target = this.controller.targetPlayer;
        World world = this.controller.getWorld();
        if (!world.isClient() && this.target != null && world.getGameRules().getBoolean(AttaVGameRules.WANDERER_FLINGS_PLAYERS)) {
            Vec3d end = this.getEnd();
            double distance = end.distanceTo(target.getPos());
            if (distance < 2 || (grab && distance < 10)) {
                this.grab = true;
                this.grabTicks++;
                if (this.grabTicks > 30) {
                    target.setVelocity(this.random.nextDouble(), 2, this.random.nextDouble());
                    target.velocityModified = true;
                } else {
                    target.teleport(end.x, end.y, end.z, false);
                    target.updateTrackedPosition(end.x, end.y, end.z);
                }
            } else {
                this.grab = false;
                this.grabTicks = 0;
            }
        }
    }

    @SuppressWarnings("unused")
    public Vec3d getRoot() {
        return this.positions.getFirst();
    }

    public Vec3d getEnd() {
        return this.positions.getLast();
    }

    @Override
    protected Vec3d getDesiredRootPosition() {
        return this.controller.getEyePos();
    }

    protected @Nullable Vec3d getDesiredEndPosition() {
        return this.targetPosition;
    }

    protected NbtCompound writeNbt(NbtCompound nbt) {
        RegistryWrapper.WrapperLookup wrapperLookup = this.controller.getRegistryManager();
        if (this.targetPosition != null) {
            nbt.put("targetPos", Vec3d.CODEC.encodeStart(wrapperLookup.getOps(NbtOps.INSTANCE), this.targetPosition).getOrThrow());
        }
        nbt.putBoolean("grabbing", this.grab);
        nbt.putInt("grabTicks", this.grabTicks);
        return nbt;
    }

    protected void readNbt(NbtCompound nbt) {
        RegistryWrapper.WrapperLookup wrapperLookup = this.controller.getRegistryManager();
        if (nbt.contains("targetPos")) {
            this.targetPosition = Vec3d.CODEC.parse(wrapperLookup.getOps(NbtOps.INSTANCE), nbt.get("targetPos"))
                    .resultOrPartial(error -> AttaV.LOGGER.error("Tried to load invalid Vec3d for targetPos: '{}'", error))
                    .orElse(null);
        }
        this.grab = nbt.getBoolean("grabbing");
        this.grabTicks = nbt.getInt("grabTicks");
    }

    @Override
    public List<Vec3d> positions() {
        return this.positions;
    }
}
