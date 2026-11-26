package cofh.thermal.dynamics.common.network.data.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record AttachmentConfigPayload(BlockPos pos, Direction side, byte[] data) implements CustomPacketPayload {

    public static final Type<AttachmentConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ID_THERMAL_DYNAMICS, "attachment_config_packet"));
    public static final StreamCodec<ByteBuf, AttachmentConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AttachmentConfigPayload::pos,
            Direction.STREAM_CODEC, AttachmentConfigPayload::side,
            ByteBufCodecs.byteArray(16 * 1024), AttachmentConfigPayload::data,
            AttachmentConfigPayload::new);

    @Override
    public Type<AttachmentConfigPayload> type() {

        return TYPE;
    }
}
