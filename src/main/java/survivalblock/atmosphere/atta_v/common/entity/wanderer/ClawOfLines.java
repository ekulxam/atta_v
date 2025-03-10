package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClawOfLines {

    public static final int SEGMENTS = 60;

    private List<Vec3d> positions = new ArrayList<>();
    private final @NotNull WalkingCubeEntity controller;

    private @Nullable Vec3d targetPosition = null;
    private @Nullable PlayerEntity target = null;
    private boolean grab = false;
    private int grabTicks = 0;

    public ClawOfLines(WalkingCubeEntity controller) {
        this.controller = Objects.requireNonNull(controller);
        this.resetPositions();
    }

    private void resetPositions() {
        this.positions.clear();
        Vec3d pos = this.controller.getEyePos();
        for (int i = 0; i < SEGMENTS; i++) {
            this.positions.add(new Vec3d(pos.x, pos.y + 0.3 * i, pos.z));
        }
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
        this.target = this.controller.targetPlayer;
        if (this.targetPosition != null) {
            this.positions = FabrIKSolver.solve(this.positions, this.targetPosition);
        }
        if (!this.controller.getWorld().isClient() && this.target != null) {
            Vec3d end = this.getEnd();
            double distance = end.distanceTo(target.getPos());
            if (distance < 2 || (grab && distance < 10)) {
                this.grab = true;
                this.grabTicks++;
                if (this.grabTicks > 30) {
                    Random random = this.controller.getRandom();
                    target.setVelocity(random.nextDouble(), 2, random.nextDouble());
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

    /**
     * Returns a read-only view of {@link ClawOfLines#positions}
     * @return an {@link ImmutableList}
     */
    public List<Vec3d> getPositions() {
        return ImmutableList.copyOf(this.positions);
    }

    // anchor?
    public Vec3d getRoot() {
        return this.positions.getFirst();
    }

    public Vec3d getEnd() {
        return this.positions.getLast();
    }
}
