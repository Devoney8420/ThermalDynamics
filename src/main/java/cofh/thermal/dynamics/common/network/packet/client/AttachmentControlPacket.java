package cofh.thermal.dynamics.common.network.packet.client;


import cofh.core.util.ProxyUtils;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.attachment.IPacketHandlerAttachment;
import cofh.thermal.dynamics.common.network.data.client.AttachmentControlPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static cofh.thermal.dynamics.util.TDynUtil.takePacketBytes;

public class AttachmentControlPacket {

    public static final AttachmentControlPacket INSTANCE = new AttachmentControlPacket();

    public static AttachmentControlPacket get() {

        return INSTANCE;
    }

    public void handle(final AttachmentControlPayload payload, final IPayloadContext context) {

        context.enqueueWork(() -> {
            Level world = ProxyUtils.getClientWorld();

            if (world == null) {
                return;
            }
            BlockPos pos = payload.pos();
            Direction side = payload.side();

            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof IDuct<?, ?> duct && duct.getAttachment(side) instanceof IPacketHandlerAttachment attachment) {
                var body = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), world.registryAccess());
                try {
                    attachment.handleControlPacket(body);
                } finally {
                    body.release();
                }
            }
        });
    }

    public static void sendToClient(IPacketHandlerAttachment attachment) {

        if (attachment == null || !(attachment.world() instanceof ServerLevel level) || !attachment.hasControlPacket()) {
            return;
        }
        var pos = attachment.pos();
        var data = takePacketBytes(attachment.getControlPacket(new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess())));
        PacketDistributor.sendToPlayersNear(level, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                64.0, new AttachmentControlPayload(pos, attachment.side(), data));
    }
}
