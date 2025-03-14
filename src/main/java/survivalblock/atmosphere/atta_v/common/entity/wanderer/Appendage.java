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

    protected final List<Vec3d> positions = new ArrayList<>();
    private List<Vec3d> prevPositions = new ArrayList<>();
    protected final @NotNull WalkingCubeEntity controller;
    protected final Random random = Random.create();
    protected final int segments;
    protected final double segmentLength;
    private final boolean clientOnly;

    public Appendage(WalkingCubeEntity controller, final int segments, final double segmentLength, final boolean clientOnly) {
        this.controller = Objects.requireNonNull(controller);
        this.segments = segments;
        this.segmentLength = segmentLength;
        this.clientOnly = clientOnly;
        this.resetPositions(this.positions, 1.0F);
        this.prevPositions = new ArrayList<>(this.positions);
    }

    protected void resetPositions(List<Vec3d> list, final float tickDelta) {
        if (!this.controller.getWorld().isClient() && clientOnly) {
            return;
        }
        list.clear();
        Vec3d pos = this.getDesiredRootPosition(tickDelta);
        if (pos == null) {
            return;
        }
        for (int i = 0; i < this.segments; i++) {
            list.add(new Vec3d(pos.x, pos.y + this.segmentLength * i, pos.z));
        }
    }

    public void tick() {
        if (!this.controller.getWorld().isClient() && clientOnly) {
            return;
        }
        this.prevPositions = new ArrayList<>(this.positions);
        this.baseTick(1.0F);
    }

    public void baseTick(final float tickDelta) {
        this.resetPositions(this.positions, tickDelta);
        Vec3d targetPosition = this.getDesiredEndPosition(tickDelta);
        if (targetPosition != null) {
            FabrIKSolver.solve(this.positions, targetPosition);
        }
    }

    protected abstract @Nullable Vec3d getDesiredRootPosition(final float tickDelta);
    protected abstract @Nullable Vec3d getDesiredEndPosition(final float tickDelta);

    /**
     * Returns a read-only view of {@link Appendage#positions}
     * @return an {@link ImmutableList}
     */
    public final ImmutableList<Vec3d> getPositions(final float tickDelta) {
        ImmutableList.Builder<Vec3d> builder = ImmutableList.builder();
        for (int i = 0; i < prevPositions.size(); i++) {
            builder.add(positions.get(i));
        }
        return builder.build();
    }

    public record PositionColorContainer(List<Vec3d> positions, int color) {

    }
}