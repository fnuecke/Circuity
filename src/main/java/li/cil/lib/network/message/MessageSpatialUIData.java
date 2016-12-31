package li.cil.lib.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.lib.ModSillyBee;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.io.IOException;

public class MessageSpatialUIData implements IMessage {
    private NBTTagCompound data;

    // --------------------------------------------------------------------- //

    public MessageSpatialUIData(final NBTTagCompound data) {
        this.data = data;
    }

    @SuppressWarnings("unused")
    public MessageSpatialUIData() {
    }

    // --------------------------------------------------------------------- //

    public NBTTagCompound getData() {
        return data;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @SuppressWarnings("unchecked")
    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        try {
            data = packet.readCompoundTag();
        } catch (final IOException e) {
            ModSillyBee.getLogger().error("Failed reading UI data.", e);
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeCompoundTag(data);
    }
}
