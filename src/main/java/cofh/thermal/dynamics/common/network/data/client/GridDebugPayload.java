package cofh.thermal.dynamics.common.network.data.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record GridDebugPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<GridDebugPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ID_THERMAL_DYNAMICS, "grid_debug_packet"));
    public static final StreamCodec<ByteBuf, GridDebugPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.byteArray(1024 * 1024 - 16), GridDebugPayload::data,
            GridDebugPayload::new);

    @Override
    public Type<GridDebugPayload> type() {

        return TYPE;
    }
}
