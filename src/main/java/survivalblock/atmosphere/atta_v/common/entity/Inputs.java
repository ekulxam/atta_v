package survivalblock.atmosphere.atta_v.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec2f;
import survivalblock.atmosphere.atmospheric_api.not_mixin.util.PitchYawPair;

import java.util.function.Supplier;

public class Inputs {

    protected boolean shouldAccelerateForward;
    protected boolean shouldGoBackward;
    protected boolean shouldTurnLeft;
    protected boolean shouldTurnRight;

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

    public boolean shouldMove() {
        return this.shouldAccelerateForward || this.shouldGoBackward || this.shouldTurnRight || this.shouldTurnLeft;
    }

    public void tickRotation(Vec2f rotation, RotationModifier set, RotationModifier add, Supplier<PitchYawPair> defaultRotationSupplier, Runnable yawUpdater) {
        set.invoke(rotation.y, rotation.x);
        if (this.shouldTurnRight) {
            add.invoke(90.0f, 0);
            if (this.shouldAccelerateForward) {
                add.invoke(-45.0f, 0);
            }
            if (this.shouldGoBackward) {
                add.invoke(45.0f, 0);
            }
        } else if (this.shouldTurnLeft) {
            add.invoke(-90.0f, 0);
            if (this.shouldAccelerateForward) {
                add.invoke(45.0f, 0);
            }
            if (this.shouldGoBackward) {
                add.invoke(-45.0f, 0);
            }
        } else if (this.shouldGoBackward) {
            add.invoke(180.0f, 0);
        } else if (!this.shouldAccelerateForward) {
            PitchYawPair defaultRotation = defaultRotationSupplier.get();
            set.invoke(defaultRotation.yaw(), defaultRotation.pitch());
        }
        yawUpdater.run();
    }

    @FunctionalInterface
    public interface RotationModifier {
        void invoke(float yaw, float pitch);
    }
}
