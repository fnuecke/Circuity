package li.cil.lib.network.handler;

import li.cil.lib.common.gui.spatial.SpatialUIManagerServer;
import li.cil.lib.network.message.MessageSpatialUIData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class MessageHandlerSpatialUIDataServer extends AbstractMessageHandlerNoResponse<MessageSpatialUIData> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageSpatialUIData message, final MessageContext context) {
        final NBTTagCompound data = message.getData();

        SpatialUIManagerServer.INSTANCE.handleData(context.getServerHandler(), data);

        return null;
    }
}
