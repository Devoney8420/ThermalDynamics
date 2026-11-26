package cofh.thermal.dynamics.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public final class TDynUtil {
    private TDynUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object value) {
        return (T) value;
    }

    public static byte[] takePacketBytes(ByteBuf buffer) {
        try {
            return ByteBufUtil.getBytes(buffer);
        } finally {
            buffer.release();
        }
    }
}
