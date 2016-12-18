package li.cil.lib.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageUnsubscribeComponent implements IMessage {
    private int dimension;
    private long componentId;

    // --------------------------------------------------------------------- //

    public MessageUnsubscribeComponent(final int dimension, final long componentId) {
        this.dimension = dimension;
        this.componentId = componentId;
    }

    @SuppressWarnings("unused")
    public MessageUnsubscribeComponent() {
    }

    // --------------------------------------------------------------------- //

    public int getDimension() {
        return dimension;
    }

    public long getComponentId() {
        return componentId;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        dimension = packet.readVarInt();
        componentId = packet.readVarLong();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarInt(dimension);
        packet.writeVarLong(componentId);
    }
}
