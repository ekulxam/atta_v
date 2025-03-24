package survivalblock.atmosphere.atta_v.common.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

public record RideWandererS2CPayload(int entityId) implements CustomPayload {

    public static final PacketCodec<RegistryByteBuf, RideWandererS2CPayload> PACKET_CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, payload -> payload.entityId,
                    RideWandererS2CPayload::new);
    public static final Id<RideWandererS2CPayload> ID = new Id<>(AttaV.id("ride_wanderer_s2c_payload"));

    public RideWandererS2CPayload(WalkingCubeEntity walkingCube) {
        this(walkingCube.getId());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
