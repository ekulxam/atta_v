package survivalblock.atmosphere.atta_v.client.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import survivalblock.atmosphere.atta_v.client.AttaVClient;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

import java.util.List;
import java.util.function.Function;

public class WandererRenderer extends EntityRenderer<WalkingCubeEntity> {

    public static final Function<VertexConsumerProvider, VertexConsumer> LINES = (provider) -> provider.getBuffer(RenderLayer.getLines());

    protected WandererModel model;

    public WandererRenderer(EntityRendererFactory.Context context, WandererModel model) {
        super(context);
        this.model = model;
    }

    public WandererRenderer(EntityRendererFactory.Context context) {
        this(context, new WandererModel(context.getPart(AttaVClient.WANDERER)));
    }

    public void render(WalkingCubeEntity entity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        matrixStack.push();
        matrixStack.translate(0, -3, 0);
        matrixStack.scale(3, 3, 3);
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        boolean showBody = this.isVisible(entity);
        boolean translucent = !showBody && !entity.isInvisibleTo(player);
        boolean outline = client.hasOutline(entity);
        RenderLayer renderLayer = this.getRenderLayer(entity, showBody, translucent, outline);
        if (renderLayer != null) {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);
            if (player != null) {
                if (!player.equals(entity.getControllingPassenger()) || !client.options.getPerspective().isFirstPerson()) {
                    renderForReal(matrixStack, light, vertexConsumer, translucent);
                }
                // entityrenderdispatchermixin handles the otherwise part
            } else {
                renderForReal(matrixStack, light, vertexConsumer, translucent);
            }
        }
        matrixStack.pop();
        // unscale MatrixStack BEFORE rendering legs, otherwise you'll be in for a world of (rendering) pain
        VertexConsumer lines = LINES.apply(vertexConsumerProvider);
        Vec3d lerpedPos =  entity.getLerpedPos(tickDelta);
        entity.getLegPositions(tickDelta).forEach(container -> renderAppendage(entity, lerpedPos, container.positions(), matrixStack, lines, container.color()));
        renderAppendage(entity, lerpedPos, entity.getClaw().getPositions(tickDelta), matrixStack, lines);
        super.render(entity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }

    private void renderForReal(MatrixStack matrixStack, int light, VertexConsumer vertexConsumer, boolean showBody) {
        this.model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, showBody ? 654311423 : -1);
    }

    private static void renderAppendage(WalkingCubeEntity walkingCube, Vec3d entityPos, List<Vec3d> positions, MatrixStack matrixStack, VertexConsumer lines) {
        renderAppendage(walkingCube, entityPos, positions, matrixStack, lines, 0xFF0000FF);
    }

    private static void renderAppendage(WalkingCubeEntity walkingCube, Vec3d entityPos, List<Vec3d> positions, MatrixStack matrixStack, VertexConsumer lines, int color) {
        Vec3d previous = positions.getFirst();
        Vec3d current;
        for (int i = 1; i < positions.size(); i++) {
            current = positions.get(i);
            drawLine(entityPos, current, previous, matrixStack, lines, color);
            previous = current;
        }
    }

    public static void drawLine(Vec3d pos, Vec3d start, Vec3d end, MatrixStack matrixStack, VertexConsumer lines, int color) {
        matrixStack.push();
        MatrixStack.Entry entry = matrixStack.peek();
        Vector3f offset = start.subtract(pos).toVector3f();
        Vec3d rotationVec = end.subtract(start);
        lines.vertex(entry, offset).color(color).normal(entry, (float)rotationVec.x, (float)rotationVec.y, (float)rotationVec.z);
        lines.vertex(entry, (float)(offset.x() + rotationVec.x), (float)(offset.y() + rotationVec.y), (float)(offset.z() + rotationVec.z))
                .color(color)
                .normal(entry, (float)rotationVec.x, (float)rotationVec.y, (float)rotationVec.z);
        matrixStack.pop();
    }

    @Override
    public Identifier getTexture(WalkingCubeEntity entity) {
        return AttaV.id("textures/entity/wanderer.png");
    }

    @Nullable
    protected RenderLayer getRenderLayer(WalkingCubeEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        Identifier identifier = this.getTexture(entity);
        if (translucent) {
            return RenderLayer.getItemEntityTranslucentCull(identifier);
        } else if (showBody) {
            return this.model.getLayer(identifier);
        } else {
            return showOutline ? RenderLayer.getOutline(identifier) : null;
        }
    }

    protected boolean isVisible(WalkingCubeEntity entity) {
        return !entity.isInvisible();
    }

    @Override
    public boolean shouldRender(WalkingCubeEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}
