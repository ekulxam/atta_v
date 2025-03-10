package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class ClawOfLines extends Appendage {

    private @Nullable Vec3d targetPosition = null;
    private @Nullable PlayerEntity target = null;
    private boolean grab = false;
    private int grabTicks = 0;

    public ClawOfLines(WalkingCubeEntity controller) {
        super(controller, 60, 0.3);
    }

    public void tick() {
        this.resetPositions();
        if (this.controller.targetPos != null) {
            if (this.targetPosition == null) {
                this.targetPosition = this.controller.targetPos;
            } else {
                this.targetPosition = this.targetPosition.lerp(this.controller.targetPos, 0.05);
            }
        }
        super.tick();
        this.target = this.controller.targetPlayer;
        if (!this.controller.getWorld().isClient() && this.target != null) {
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

    @Override
    protected @Nullable Vec3d getDesiredEndPosition() {
        return this.targetPosition;
    }
}
