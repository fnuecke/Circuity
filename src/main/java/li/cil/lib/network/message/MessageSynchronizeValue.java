package li.cil.lib.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.util.NBTUtil;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageSynchronizeValue implements IMessage {
    private int dimension;
    private long componentId;
    private NBTTagList values;

    // --------------------------------------------------------------------- //

    public MessageSynchronizeValue(final World world, final Component component, final NBTTagList values) {
        this.dimension = world.provider.getDimension();
        this.componentId = component.getId();
        this.values = values;
    }

    @SuppressWarnings("unused")
    public MessageSynchronizeValue() {
    }

    // --------------------------------------------------------------------- //

    public int getDimension() {
        return dimension;
    }

    public long getComponentId() {
        return componentId;
    }

    public NBTTagList getValues() {
        return values;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        dimension = packet.readVarInt();
        componentId = packet.readVarLong();
        values = (NBTTagList) NBTUtil.read(packet);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarInt(dimension);
        packet.writeVarLong(componentId);
        NBTUtil.write(values, packet);
    }
}
