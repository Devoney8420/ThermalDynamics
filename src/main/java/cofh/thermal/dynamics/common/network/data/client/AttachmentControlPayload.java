package cofh.thermal.dynamics.common.network.data.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record AttachmentControlPayload(BlockPos pos, Direction side, byte[] data) implements CustomPacketPayload {

    public static final Type<AttachmentControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ID_THERMAL_DYNAMICS, "attachment_control_packet"));
    public static final StreamCodec<ByteBuf, AttachmentControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AttachmentControlPayload::pos,
            Direction.STREAM_CODEC, AttachmentControlPayload::side,
            ByteBufCodecs.byteArray(16 * 1024), AttachmentControlPayload::data,
            AttachmentControlPayload::new);

    @Override
    public Type<AttachmentControlPayload> type() {

        return TYPE;
    }
}
