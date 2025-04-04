package survivalblock.atmosphere.atta_v.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import survivalblock.atmosphere.atta_v.client.entity.WandererRenderer;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

@Debug(export = true)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderHitboxes:Z", opcode = Opcodes.GETFIELD))
    private boolean renderWandererAsBox(boolean original, @Local(argsOnly = true) Entity entity, @Local(argsOnly = true)MatrixStack matrices, @Local(argsOnly = true) VertexConsumerProvider vertexConsumerProvider) {
        if (!(entity instanceof WalkingCubeEntity walkingCube)) {
            return original;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return original;
        }
        if (player.equals(walkingCube.getControllingPassenger()) && client.options.getPerspective().isFirstPerson()) {
            if (!original) {
                Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
                WorldRenderer.drawBox(matrices, WandererRenderer.LINES.apply(vertexConsumerProvider), box, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        return original;
    }

    @Inject(method = "renderHitbox", at = @At(value = "HEAD"))
    private static void renderLegHitboxes(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, float red, float green, float blue, CallbackInfo ci) {
        if (entity instanceof WalkingCubeEntity walkingCube) {
            walkingCube.getLegBoundingBoxes(tickDelta).forEach(pair -> {
                Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
                Box box = pair.boundingBox().offset(-lerpedPos.x, -lerpedPos.y, -lerpedPos.z);
                matrices.push();
                Vec3d pos = pair.pos();
                Vec3d legLerpedPos = pair.lerpedPos();
                if (pos != null && legLerpedPos != null) {
                    matrices.translate(pos.x - legLerpedPos.x, pos.y - legLerpedPos.y, pos.z - legLerpedPos.z);
                }
                WorldRenderer.drawBox(matrices, vertices, box, red, green, blue, 1.0F);
                matrices.pop();
            });
        }
    }
}
