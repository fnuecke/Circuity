package li.cil.lib.network.handler;

import li.cil.lib.common.gui.spatial.SpatialUIManagerServer;
import li.cil.lib.network.message.MessageSpatialUIUnsubscribe;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class MessageHandlerSpatialUIUnsubscribe extends AbstractMessageHandlerNoResponse<MessageSpatialUIUnsubscribe> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageSpatialUIUnsubscribe message, final MessageContext context) {
        SpatialUIManagerServer.INSTANCE.unsubscribe(context.getServerHandler());

        return null;
    }
}
