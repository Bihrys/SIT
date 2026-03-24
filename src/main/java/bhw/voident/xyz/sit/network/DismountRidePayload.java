package bhw.voident.xyz.sit.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import bhw.voident.xyz.sit.Sit;

public record DismountRidePayload() implements CustomPayload {
    public static final DismountRidePayload INSTANCE = new DismountRidePayload();
    public static final Id<DismountRidePayload> ID = new Id<>(Identifier.of(Sit.MOD_ID, "dismount_ride"));
    public static final PacketCodec<RegistryByteBuf, DismountRidePayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
