package li.cil.lib.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.common.Synchronization;
import li.cil.lib.synchronization.SynchronizationManagerServerImpl;
import li.cil.lib.util.NBTUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageSynchronizationInitialize implements IMessage {
    public static final String COMPONENT_ID_TAG = "componentId";
    public static final String COMPONENT_CLASS_TAG = "componentClass";
    public static final String COMPONENT_TAG = "component";

    // --------------------------------------------------------------------- //

    public static NBTTagCompound getComponentNBT(final Component component) {
        final NBTTagCompound componentInfo = new NBTTagCompound();
        componentInfo.setLong(COMPONENT_ID_TAG, component.getId());
        componentInfo.setInteger(COMPONENT_CLASS_TAG, SillyBeeAPI.synchronization.getServer().getTypeIdByValue(component));

        final SynchronizationManagerServerImpl synchronization = Synchronization.INSTANCE.getServer();
        final NBTTagList componentNbt = synchronization.getAllFieldValues(component);
        if (componentNbt != null) {
            componentInfo.setTag(COMPONENT_TAG, componentNbt);
        }

        return componentInfo;
    }

    // --------------------------------------------------------------------- //

    private int dimension;
    private long entity;
    private NBTTagList components;

    // --------------------------------------------------------------------- //

    public MessageSynchronizationInitialize(final int dimension, final long entity, final NBTTagList components) {
        this.dimension = dimension;
        this.entity = entity;
        this.components = components;
    }

    @SuppressWarnings("unused")
    public MessageSynchronizationInitialize() {
    }

    // --------------------------------------------------------------------- //

    public int getDimension() {
        return dimension;
    }

    public long getEntity() {
        return entity;
    }

    public NBTTagList getComponents() {
        return components;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        dimension = packet.readVarInt();
        entity = packet.readVarLong();
        components = (NBTTagList) NBTUtil.read(packet);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarInt(dimension);
        packet.writeVarLong(entity);
        NBTUtil.write(components, packet);
    }
}
