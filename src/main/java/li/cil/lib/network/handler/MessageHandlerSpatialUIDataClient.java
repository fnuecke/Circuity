package li.cil.lib.network.handler;

import li.cil.lib.client.gui.spatial.SpatialUIManagerClient;
import li.cil.lib.network.message.MessageSpatialUIData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class MessageHandlerSpatialUIDataClient extends AbstractMessageHandlerNoResponse<MessageSpatialUIData> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageSpatialUIData message, final MessageContext context) {
        final NBTTagCompound data = message.getData();

        SpatialUIManagerClient.INSTANCE.handleData(data);

        return null;
    }
}
