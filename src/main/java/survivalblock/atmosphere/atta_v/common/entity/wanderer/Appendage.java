package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import survivalblock.atmosphere.atmospheric_api.not_mixin.entity.PositionContainer;

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
        this.resetPositions(this.positions);
        this.prevPositions = new ArrayList<>(this.positions);
    }

    protected void resetPositions(List<Vec3d> list) {
        if (!this.controller.getWorld().isClient() && this.clientOnly) {
            return;
        }

        list.clear();
        Vec3d pos = this.getDesiredRootPosition();
        if (pos == null) {
            return;
        }

        for (int i = 0; i < this.segments; i++) {
            list.add(new Vec3d(pos.x, pos.y + this.segmentLength * i, pos.z));
        }
    }

    public void tick() {
        if (!this.controller.getWorld().isClient() && this.clientOnly) {
            return;
        }

        this.prevPositions = new ArrayList<>(this.positions);
        this.resetPositions(this.positions);
        Vec3d targetPosition = this.getDesiredEndPosition();
        if (targetPosition != null) {
            FabrIKSolver.solve(this.positions, targetPosition);
        }
    }

    protected abstract @Nullable Vec3d getDesiredRootPosition();
    protected abstract @Nullable Vec3d getDesiredEndPosition();

    /**
     * Returns a read-only view of {@link Appendage#positions}
     * @return an {@link ImmutableList}
     */
    public final List<Vec3d> getPositions(final float tickDelta) {
        List<Vec3d> list = new ArrayList<>();
        for (int i = 0; i < prevPositions.size(); i++) {
            list.add(prevPositions.get(i).lerp(positions.get(i), tickDelta));
        }
        Vec3d prev;
        Vec3d prev2;
        Vec3d start;
        Vec3d end;
        Vec3d current;
        List<Vec3d> returned = new ArrayList<>();
        returned.add(list.getFirst());
        for (int i = 2; i < list.size(); i++) {
            prev = list.get(i - 2);
            prev2 = list.get(i - 1);
            current = list.get(i);
            start = prev.lerp(prev2, 0.5f);
            end = prev2.lerp(current, 0.5f);
            for (float f = 0; f < 1; f += 0.1f) {
                returned.add(doubleLerp(start, prev2, end, f));
            }
        }
        returned.add(list.getLast());
        return returned;
    }

    // thanks to falkreon for teaching me about splines and béziers
    public static Vec3d doubleLerp(Vec3d one, Vec3d two, Vec3d three, final float delta) {
        return one.lerp(two, delta).lerp( two.lerp(three, delta) , delta);
    }

    public record PositionColorContainer(List<Vec3d> positions, int color) implements PositionContainer {

    }
}