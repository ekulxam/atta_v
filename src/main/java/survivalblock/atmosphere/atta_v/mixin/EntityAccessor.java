package survivalblock.atmosphere.atta_v.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(Entity.class)
public interface EntityAccessor {

    @Invoker("findCollisionsForMovement")
    static List<VoxelShape> atta_v$invokeFindCollisionsForMovement(@Nullable Entity entity, World world, List<VoxelShape> regularCollisions, Box movingEntityBoundingBox) {
        throw new UnsupportedOperationException("Mixin invoker");
    }

    @Invoker("collectStepHeights")
    static float[] atta_v$invokeCollectStepHeights(Box collisionBox, List<VoxelShape> collisions, float f, float stepHeight) {
        throw new UnsupportedOperationException("Mixin invoker");
    }
}
