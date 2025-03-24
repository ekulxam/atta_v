package survivalblock.atmosphere.atta_v.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import survivalblock.atmosphere.atta_v.client.entity.WandererModel;
import survivalblock.atmosphere.atta_v.client.entity.WandererRenderer;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.entity.paths.EntityPath;
import survivalblock.atmosphere.atta_v.common.entity.paths.WorldPathComponent;
import survivalblock.atmosphere.atta_v.common.init.AttaVWorldComponents;
import survivalblock.atmosphere.atta_v.common.networking.RideWandererS2CPayload;
import survivalblock.atmosphere.atta_v.common.networking.TripodLegUpdatePayload;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityTypes;

import java.util.List;


public class AttaVClient implements ClientModInitializer {

	public static boolean showEntityPaths = false;
	public static final EntityModelLayer WANDERER = new EntityModelLayer(AttaV.id("wanderer"), "main");

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(WANDERER, WandererModel::getTexturedModelData);

		EntityRendererRegistry.register(AttaVEntityTypes.WANDERER, WandererRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(TripodLegUpdatePayload.ID, (payload, context) -> {
			Entity entity = context.player().getWorld().getEntityById(payload.entityId());
			if (entity instanceof WalkingCubeEntity walkingCube) {
				walkingCube.readLegDataFromNbt(payload.nbt());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(RideWandererS2CPayload.ID, (payload, context) -> {
			PlayerEntity player = context.player();
			Entity entity = context.player().getWorld().getEntityById(payload.entityId());
			if (entity instanceof WalkingCubeEntity walkingCube) {
				player.startRiding(walkingCube);
			}
		});

		ClientCommandRegistrationCallback.EVENT.register(AttaVClientCommands::register);

		WorldRenderEvents.AFTER_ENTITIES.register(worldRenderContext -> {
			if (!showEntityPaths) {
				return;
			}
			WorldPathComponent worldPathComponent = AttaVWorldComponents.WORLD_PATH.get(worldRenderContext.world());
			if (worldPathComponent.isEmpty()) {
				return;
			}
			MatrixStack matrixStack = worldRenderContext.matrixStack();
			if (matrixStack == null) {
				return;
			}
			matrixStack.push();
			Vec3d cameraPos = worldRenderContext.camera().getPos();
			matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
			VertexConsumerProvider vertexConsumerProvider = worldRenderContext.consumers();
			if (vertexConsumerProvider == null) {
				return;
			}
			VertexConsumer lines = WandererRenderer.LINES.apply(vertexConsumerProvider);
			worldPathComponent.getPaths().forEach(entityPath -> {
				if (entityPath.nodes.isEmpty()) {
					return;
				}
 				Vec3d previous = entityPath.nodes.getFirst();
				int color = entityPath.color;
				final float red = ColorHelper.Argb.getRed(color) / 255f;
				final float green = ColorHelper.Argb.getGreen(color) / 255f;
				final float blue = ColorHelper.Argb.getBlue(color) / 255f;
				color = ColorHelper.Argb.fullAlpha(color);
				final int size = entityPath.nodes.size();
				if (size < 1) {
					return;
				}
				Vec3d current;
				for (int i = 1; i < size + 1; i++) {
					current = i >= size ? entityPath.nodes.get(i - size) : entityPath.nodes.get(i);
					if (i - size == 0) {
						WorldRenderer.drawBox(matrixStack, lines,
								new Box(current.subtract(0.25, 0.25, 0.25),
										current.add(0.25, 0.25, 0.25)),
								1.0F, 1.0F, 1.0F, 1.0F);
					} else if (i == 1) {
						WorldRenderer.drawBox(matrixStack, lines,
								new Box(current.subtract(0.25, 0.25, 0.25),
										current.add(0.25, 0.25, 0.25)),
								0.8F, 0.8F, 0.8F, 0.8F);
					} else {
						WorldRenderer.drawBox(matrixStack, lines,
								new Box(current.subtract(0.25, 0.25, 0.25),
										current.add(0.25, 0.25, 0.25)),
								red, green, blue, 1.0F);
					}
					WandererRenderer.drawLine(Vec3d.ZERO, previous, current, matrixStack, lines, color);
					previous = current;
				}
			});
			matrixStack.pop();
		});
	}
}