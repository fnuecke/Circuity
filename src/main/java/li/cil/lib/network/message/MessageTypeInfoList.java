package li.cil.lib.network.message;

import com.google.common.base.Throwables;
import io.netty.buffer.ByteBuf;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageTypeInfoList implements IMessage {
    private final List<Class> types;

    // --------------------------------------------------------------------- //

    public MessageTypeInfoList(final List<Class> types) {
        this.types = types;
    }

    @SuppressWarnings("unused")
    public MessageTypeInfoList() {
        this.types = new ArrayList<>();
    }

    // --------------------------------------------------------------------- //

    public List<Class> getTypes() {
        return types;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        final int count = packet.readVarIntFromBuffer();
        try {
            for (int i = 0; i < count; i++) {
                types.add(ReflectionUtil.getClass(packet.readStringFromBuffer(0xFFFF)));
            }
        } catch (final ClassNotFoundException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarIntToBuffer(types.size());
        for (final Class type : types) {
            packet.writeString(type.getName());
        }
    }
}
