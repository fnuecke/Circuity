package li.cil.lib.network.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageComponentData implements IMessage {
    private int dimension;
    private long componentId;
    private ByteBuf data;

    // --------------------------------------------------------------------- //

    public MessageComponentData(final int dimension, final long componentId, final ByteBuf data) {
        this.dimension = dimension;
        this.componentId = componentId;
        this.data = data;
    }

    @SuppressWarnings("unused")
    public MessageComponentData() {
    }

    // --------------------------------------------------------------------- //

    public int getDimension() {
        return dimension;
    }

    public long getComponentId() {
        return componentId;
    }

    public ByteBuf getData() {
        return data;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        dimension = packet.readVarIntFromBuffer();
        componentId = packet.readVarLong();
        final int length = packet.readVarIntFromBuffer();
        data = new PacketBuffer(Unpooled.buffer(length));
        packet.readBytes(data, length);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarIntToBuffer(dimension);
        packet.writeVarLong(componentId);
        packet.writeVarIntToBuffer(data.readableBytes());
        packet.writeBytes(data, 0, data.readableBytes());
    }
}
