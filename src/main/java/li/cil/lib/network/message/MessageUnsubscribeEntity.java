package li.cil.lib.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageUnsubscribeEntity implements IMessage {
    private int dimension;
    private long entity;

    // --------------------------------------------------------------------- //

    public MessageUnsubscribeEntity(final int dimension, final long entity) {
        this.dimension = dimension;
        this.entity = entity;
    }

    @SuppressWarnings("unused")
    public MessageUnsubscribeEntity() {
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
        dimension = packet.readVarIntFromBuffer();
        entity = packet.readVarLong();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarIntToBuffer(dimension);
        packet.writeVarLong(entity);
    }
}
