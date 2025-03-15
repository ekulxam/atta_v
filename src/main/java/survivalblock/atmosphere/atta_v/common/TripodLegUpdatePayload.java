package survivalblock.atmosphere.atta_v.common;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

public record TripodLegUpdatePayload(int entityId, NbtCompound nbt) implements CustomPayload {

    public static final PacketCodec<RegistryByteBuf, TripodLegUpdatePayload> PACKET_CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, payload -> payload.entityId,
                    PacketCodecs.NBT_COMPOUND, payload -> payload.nbt,
                    TripodLegUpdatePayload::new);
    public static final Id<TripodLegUpdatePayload> ID = new Id<>(AttaV.id("tripod_leg_update_payload"));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void sendS2C(ServerWorld serverWorld, WalkingCubeEntity walkingCube, @Nullable ServerPlayerEntity except) {
        serverWorld.getPlayers().forEach(serverPlayer -> {
            if (!serverPlayer.equals(except) && Math.ceil(serverPlayer.distanceTo(walkingCube)) <= (walkingCube.getType().getMaxTrackDistance() * 16)) {
                ServerPlayNetworking.send(serverPlayer, this);
            }
        });
    }

    public void sendC2S() {
        ClientPlayNetworking.send(this);
    }
}
