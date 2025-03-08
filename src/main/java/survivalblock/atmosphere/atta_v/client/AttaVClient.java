package survivalblock.atmosphere.atta_v.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.Entity;
import survivalblock.atmosphere.atta_v.client.entity.WandererModel;
import survivalblock.atmosphere.atta_v.client.entity.WandererRenderer;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.TripodLegUpdatePayload;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityTypes;


public class AttaVClient implements ClientModInitializer {

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
	}
}