package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Appendage {

    protected List<Vec3d> positions = new ArrayList<>();
    protected final @NotNull WalkingCubeEntity controller;
    protected final Random random = Random.create();
    protected final int segments;
    protected final double segmentLength;

    public Appendage(WalkingCubeEntity controller, final int segments, final double segmentLength) {
        this.controller = Objects.requireNonNull(controller);
        this.segments = segments;
        this.segmentLength = segmentLength;
        this.resetPositions();
    }

    protected void resetPositions() {
        this.positions.clear();
        Vec3d pos = this.getDesiredRootPosition();
        if (pos == null) {
            return;
        }
        for (int i = 0; i < this.segments; i++) {
            this.positions.add(new Vec3d(pos.x, pos.y + this.segmentLength * i, pos.z));
        }
    }

    public void tick() {
        Vec3d targetPosition = this.getDesiredEndPosition();
        if (targetPosition != null) {
            this.positions = FabrIKSolver.solve(this.positions, targetPosition);
        }
    }

    protected abstract @Nullable Vec3d getDesiredRootPosition();
    protected abstract @Nullable Vec3d getDesiredEndPosition();

    /**
     * Returns a read-only view of {@link Appendage#positions}
     * @return an {@link ImmutableList}
     */
    public final List<Vec3d> getPositions() {
        return ImmutableList.copyOf(this.positions);
    }

    public record PositionColorContainer(List<Vec3d> positions, int color) {

    }
}