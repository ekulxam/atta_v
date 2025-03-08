package survivalblock.atmosphere.atta_v.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Shadow public Input input;

    @WrapOperation(method = "tickRiding", constant = @Constant(classValue = BoatEntity.class, ordinal = 0))
    private boolean controlWanderer(Object obj, Operation<Boolean> original) {
        if (original.call(obj)) {
            return true;
        }
        if (obj instanceof WalkingCubeEntity walkingCube) {
            walkingCube.setInputs(this.input.pressingLeft, this.input.pressingRight, this.input.pressingForward, this.input.pressingBack);
        }
        return false;
    }
}
