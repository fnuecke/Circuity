package li.cil.lib.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageSynchronizationSubscribe implements IMessage {
    private int dimension;
    private long entity;

    // --------------------------------------------------------------------- //

    public MessageSynchronizationSubscribe(final int dimension, final long entity) {
        this.dimension = dimension;
        this.entity = entity;
    }

    @SuppressWarnings("unused")
    public MessageSynchronizationSubscribe() {
    }

    // --------------------------------------------------------------------- //

    public int getDimension() {
        return dimension;
    }

    public long getEntity() {
        return entity;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        dimension = packet.readVarInt();
        entity = packet.readVarLong();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarInt(dimension);
        packet.writeVarLong(entity);
    }
}
