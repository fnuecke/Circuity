package li.cil.lib.network.message;

import com.google.common.base.Throwables;
import io.netty.buffer.ByteBuf;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageTypeInfo implements IMessage {
    private int typeId;
    private Class type;

    // --------------------------------------------------------------------- //

    public MessageTypeInfo(final int typeId, final Class type) {
        this.typeId = typeId;
        this.type = type;
    }

    @SuppressWarnings("unused")
    public MessageTypeInfo() {
    }

    // --------------------------------------------------------------------- //

    public int getTypeId() {
        return typeId;
    }

    public Class getType() {
        return type;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        typeId = packet.readVarInt();
        try {
            type = ReflectionUtil.getClass(packet.readString(0xFFFF));
        } catch (final ClassNotFoundException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarInt(typeId);
        packet.writeString(type.getName());
    }
}
