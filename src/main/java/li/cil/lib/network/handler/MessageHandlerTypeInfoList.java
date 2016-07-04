package li.cil.lib.network.handler;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.network.message.MessageTypeInfoList;
import li.cil.lib.synchronization.SynchronizationManagerClientImpl;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.List;

public class MessageHandlerTypeInfoList extends AbstractMessageHandlerNoResponse<MessageTypeInfoList> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageTypeInfoList message, final MessageContext ctx) {
        final List<Class> types = message.getTypes();

        // We need/want to cast here, because we don't want this method in the public API.
        final SynchronizationManagerClientImpl synchronization = (SynchronizationManagerClientImpl) SillyBeeAPI.synchronization.getClient();

        synchronization.registerTypes(types);

        return null;
    }
}
