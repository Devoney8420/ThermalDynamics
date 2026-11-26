package cofh.thermal.dynamics.common.network.packet.server;

import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.attachment.IPacketHandlerAttachment;
import cofh.thermal.dynamics.common.network.data.server.AttachmentConfigPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static cofh.thermal.dynamics.util.TDynUtil.takePacketBytes;

public class AttachmentConfigPacket {

    public static final AttachmentConfigPacket INSTANCE = new AttachmentConfigPacket();

    public static AttachmentConfigPacket get() {

        return INSTANCE;
    }

    public void handle(final AttachmentConfigPayload payload, final IPayloadContext context) {

        context.enqueueWork(() -> {

            Player player = context.player();

            Level world = player.level();
            if (!world.isLoaded(payload.pos()) || !player.canInteractWithBlock(payload.pos(), 0.0D)) {
                return;
            }
            BlockEntity tile = world.getBlockEntity(payload.pos());
            if (tile instanceof IDuct<?, ?> duct && duct.getAttachment(payload.side()) instanceof IPacketHandlerAttachment attachment) {
                var body = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), world.registryAccess());
                try {
                    attachment.handleConfigPacket(body);
                } finally {
                    body.release();
                }
            }
        });
    }

    public static void sendToServer(IPacketHandlerAttachment attachment) {

        if (attachment == null || attachment.world() == null) {
            return;
        }
        PacketDistributor.sendToServer(new AttachmentConfigPayload(attachment.pos(), attachment.side(), takePacketBytes(attachment.getConfigPacket(
                new RegistryFriendlyByteBuf(Unpooled.buffer(), attachment.world().registryAccess())))));
    }

}
