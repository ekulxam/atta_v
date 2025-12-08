package survivalblock.atmosphere.atta_v.client.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import survivalblock.atmosphere.atmospheric_api.not_mixin.entity.PositionContainer;
import survivalblock.atmosphere.atmospheric_api.not_mixin.util.PitchYawPair;
import survivalblock.atmosphere.atta_v.client.AttaVClient;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

import java.util.List;
import java.util.function.Function;

public class WandererRenderer extends EntityRenderer<WalkingCubeEntity> {

    public static final Function<VertexConsumerProvider, VertexConsumer> LINES = (provider) -> provider.getBuffer(RenderLayer.getLines());
    public static final Identifier TEXTURE = AttaV.id("textures/entity/wanderer.png");

    protected static final BlockState ANVIL = Blocks.ANVIL.getDefaultState();
    //protected static final BlockState TINTED_GLASS = Blocks.TINTED_GLASS.getDefaultState();

    protected WandererModel model;
    protected final BlockRenderManager blockRenderManager;

    public WandererRenderer(EntityRendererFactory.Context context, WandererModel model) {
        super(context);
        this.model = model;
        this.blockRenderManager = context.getBlockRenderManager();
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
        //noinspection CodeBlock2Expr
        entity.getLegPositions(tickDelta).forEach(container -> {
            renderAppendage(lerpedPos, container, matrixStack, lines, container.color(), vertexConsumerProvider, blockRenderManager, ANVIL, light);
        });
        super.render(entity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }

    private void renderForReal(MatrixStack matrixStack, int light, VertexConsumer vertexConsumer, boolean showBody) {
        this.model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, showBody ? 654311423 : -1);
    }

    @SuppressWarnings("SameParameterValue")
    private static void renderAppendage(Vec3d entityPos, PositionContainer positionContainer, MatrixStack matrixStack, VertexConsumer lines, int color, VertexConsumerProvider vertexConsumerProvider, BlockRenderManager blockRenderManager, BlockState state, int light) {
        List<Vec3d> positions = positionContainer.positions();
        if (positions.isEmpty()) {
            return;
        }

        Vec3d previous = positions.getFirst();
        Vec3d current;
        int size = positions.size();
        for (int i = 1; i < size; i++) {
            current = positions.get(i);
            drawLine(entityPos, current, previous, matrixStack, lines, color);

            if (i > 1) {
                matrixStack.push();
                matrixStack.translate(previous.x - entityPos.x, previous.y - entityPos.y, previous.z - entityPos.z);
                PitchYawPair pair = PitchYawPair.fromVec3ds(current, previous);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - pair.yaw()));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(- pair.pitch()));
                matrixStack.translate(-0.5f, -0.5f, -0.5f);
                blockRenderManager.renderBlockAsEntity(state, matrixStack, vertexConsumerProvider, light, OverlayTexture.DEFAULT_UV);
                matrixStack.pop();
            }

            previous = current;
        }
    }

    public static void drawLine(Vec3d pos, Vec3d start, Vec3d end, MatrixStack matrixStack, VertexConsumer lines, int color) {
        drawLine(start.subtract(pos).toVector3f(), end.subtract(start), matrixStack, lines, color);
    }

    public static void drawLine(Vec3d start, Vec3d end, MatrixStack matrixStack, VertexConsumer lines, int color) {
        drawLine(start.toVector3f(), end.subtract(start), matrixStack, lines, color);
    }

    private static void drawLine(Vector3f offset, Vec3d rotationVec, MatrixStack matrixStack, VertexConsumer lines, int color) {
        matrixStack.push();
        MatrixStack.Entry entry = matrixStack.peek();
        lines.vertex(entry, offset).color(color).normal(entry, (float)rotationVec.x, (float)rotationVec.y, (float)rotationVec.z);
        lines.vertex(entry, (float)(offset.x() + rotationVec.x), (float)(offset.y() + rotationVec.y), (float)(offset.z() + rotationVec.z))
                .color(color)
                .normal(entry, (float)rotationVec.x, (float)rotationVec.y, (float)rotationVec.z);
        matrixStack.pop();
    }

    @Override
    public Identifier getTexture(WalkingCubeEntity entity) {
        return TEXTURE;
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
