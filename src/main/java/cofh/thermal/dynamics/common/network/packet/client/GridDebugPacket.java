package cofh.thermal.dynamics.common.network.packet.client;

import cofh.thermal.dynamics.client.DebugRenderer;
import cofh.thermal.dynamics.common.network.data.client.GridDebugPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

import static cofh.thermal.dynamics.util.TDynUtil.takePacketBytes;

public class GridDebugPacket {

    public static final GridDebugPacket INSTANCE = new GridDebugPacket();

    public static GridDebugPacket get() {

        return INSTANCE;
    }

    public void handle(final GridDebugPayload payload, final IPayloadContext context) {

        context.enqueueWork(() -> {

            var body = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
            try {
                Map<UUID, Map<BlockPos, List<BlockPos>>> grids = new HashMap<>();
                int numGrids = body.readVarInt();
                for (int g = 0; g < numGrids; g++) {
                    Map<BlockPos, List<BlockPos>> newNodes = new HashMap<>();
                    UUID uuid = body.readUUID();
                    int numNodes = body.readVarInt();
                    for (int i = 0; i < numNodes; ++i) {
                        BlockPos nodePos = body.readBlockPos();
                        int numEdges = body.readVarInt();
                        List<BlockPos> edges = new ArrayList<>(numEdges);
                        for (int j = 0; j < numEdges; j++) {
                            edges.add(body.readBlockPos());
                        }
                        newNodes.put(nodePos, edges);
                    }
                    grids.put(uuid, newNodes);
                }
                DebugRenderer.grids = grids;
            } finally {
                body.release();
            }
        });
    }

    public static void sendToClients(FriendlyByteBuf buf) {

        if (buf == null) {
            return;
        }
        PacketDistributor.sendToAllPlayers(new GridDebugPayload(takePacketBytes(buf)));
    }

}
