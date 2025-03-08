package survivalblock.atmosphere.atta_v.common;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityTypes;

public class AttaV implements ModInitializer {
	public static final String MOD_ID = "atta_v";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AttaVEntityTypes.init();

		// to whoever is reading this: I'm very sorry
		PayloadTypeRegistry.playS2C().register(TripodLegUpdatePayload.ID, TripodLegUpdatePayload.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(TripodLegUpdatePayload.ID, TripodLegUpdatePayload.PACKET_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TripodLegUpdatePayload.ID, (payload, context) -> {
			PlayerEntity player = context.player();
			Entity entity = player.getWorld().getEntityById(payload.entityId());
			if (entity instanceof WalkingCubeEntity walkingCube && player.equals(walkingCube.getControllingPassenger())) {
				walkingCube.readLegDataFromNbt(payload.nbt());
			}
		});
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}