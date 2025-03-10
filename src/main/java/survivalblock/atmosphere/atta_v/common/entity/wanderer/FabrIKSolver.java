package survivalblock.atmosphere.atta_v.common.entity.wanderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import survivalblock.atmosphere.atta_v.common.AttaV;

import java.util.ArrayList;
import java.util.List;

/**
 * Java (Minecraft-specific) implementation of the FABRIK Inverse Kinematics Algorithm
 * @author Survivalblock
 */
public final class FabrIKSolver {

    /**
     * Implementation as described by
     * <a href="http://www.andreasaristidou.com/publications/papers/FABRIK.pdf">
     *     http://www.andreasaristidou.com/</a>
     * @param positions The joint positions pi for i = 1,...,n. The first position should be the root chain.
     *                  Note that the distance di between each joint is equivalent to |p(i+1) - pi| for i = 1,...,n-1.
     * @param t the target position
     * @return The new joint positions pi for i = 1,...,n.
     */
    public static List<Vec3d> solve(List<Vec3d> positions, Vec3d t) {
        if (positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("positions cannot be empty!");
        }

        List<Vec3d> updatedPositions = new ArrayList<>(positions);
        int size = updatedPositions.size();
        List<Double> distances = getDistances(updatedPositions);

        // if max chain length > root to t distance
        if (distances.stream().mapToDouble(d -> d).sum() < updatedPositions.getFirst().distanceTo(t)) {
            for (int i = 0; i < size - 1; i++) {
                Vec3d pi = updatedPositions.get(i);
                double ri = t.distanceTo(pi);
                double lambdai = distances.get(i) / ri; // λi
                updatedPositions.set(i + 1, pi
                        .multiply((1 - lambdai))
                        .add( t.multiply(lambdai) ));
            }
            return updatedPositions;
        }

        final Vec3d b = positions.getFirst();
        final double tol = 0.1;
        double difA = updatedPositions.getLast().distanceTo(t);
        while (difA > tol) {
            // stage 1: forward reaching
            updatedPositions.set(size - 1, t); // pn = t
            Vec3d previous = updatedPositions.getLast();
            Vec3d current;
            for (int i = size - 2; i >= 0; i--) {  // n - 1 = (size - 1) - 1
                current = updatedPositions.get(i);
                double ri = previous.distanceTo(current);
                double lambdai = distances.get(i) / ri;
                updatedPositions.set(i, previous
                        .multiply((1 - lambdai))
                        .add( current.multiply(lambdai) ));
                previous = updatedPositions.get(i);
            }
            // stage 2: backward reaching
            updatedPositions.set(0, b);
            previous = updatedPositions.getFirst();
            for (int i = 1; i < size - 1; i++) {
                current = updatedPositions.get(i);
                double ri = current.distanceTo(previous);
                double lambdai = distances.get(i) / ri;
                updatedPositions.set(i, previous
                        .multiply((1 - lambdai))
                        .add( current.multiply(lambdai) ));
                previous = updatedPositions.get(i);
            }
            difA = updatedPositions.getLast().distanceTo(t);
        }
        return updatedPositions;
    }

    private static List<Double> getDistances(List<Vec3d> positions) {
        List<Double> distances = new ArrayList<>();
        Vec3d previous = positions.getFirst();
        Vec3d current;
        // [1,2,3,4,5], size = 5. position.getfirst = 1. i = 1, current = 2, i = 2, current = 3, i = 3, current = 4, i = 4, current = 5
        for (int i = 1; i < positions.size(); i++) {
            current = positions.get(i);
            distances.add(current.distanceTo(previous));
            previous = current;
        }
        return distances;
    }
}
