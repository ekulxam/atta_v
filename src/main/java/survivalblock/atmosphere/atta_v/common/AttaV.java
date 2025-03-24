package survivalblock.atmosphere.atta_v.common;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import survivalblock.atmosphere.atta_v.common.datagen.AttaVSoundEvents;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;
import survivalblock.atmosphere.atta_v.common.init.AttaVCommands;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityTypes;
import survivalblock.atmosphere.atta_v.common.init.AttaVGameRules;
import survivalblock.atmosphere.atta_v.common.networking.RideWandererS2CPayload;
import survivalblock.atmosphere.atta_v.common.networking.TripodLegUpdatePayload;

public class AttaV implements ModInitializer {
	public static final String MOD_ID = "atta_v";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AttaVSoundEvents.init();
		AttaVGameRules.init();
		AttaVEntityTypes.init();

		// to whoever is reading this: I'm very sorry
		PayloadTypeRegistry.playS2C().register(TripodLegUpdatePayload.ID, TripodLegUpdatePayload.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(TripodLegUpdatePayload.ID, TripodLegUpdatePayload.PACKET_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TripodLegUpdatePayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ServerWorld serverWorld = player.getServerWorld();
			Entity entity = serverWorld.getEntityById(payload.entityId());
			if (entity instanceof WalkingCubeEntity walkingCube && player.equals(walkingCube.getControllingPassenger())) {
				walkingCube.readLegDataFromNbt(payload.nbt());
				payload.sendS2C(serverWorld, walkingCube, player);
			}
		});

		PayloadTypeRegistry.playS2C().register(RideWandererS2CPayload.ID, RideWandererS2CPayload.PACKET_CODEC);

		CommandRegistrationCallback.EVENT.register(AttaVCommands::register);
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}