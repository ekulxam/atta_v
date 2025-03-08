package survivalblock.atmosphere.atta_v.common;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

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
}
