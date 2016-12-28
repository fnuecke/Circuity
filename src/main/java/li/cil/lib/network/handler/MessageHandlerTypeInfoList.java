package li.cil.lib.network.handler;

import li.cil.lib.Synchronization;
import li.cil.lib.network.message.MessageTypeInfoList;
import li.cil.lib.synchronization.SynchronizationManagerClientImpl;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.List;

public class MessageHandlerTypeInfoList extends AbstractMessageHandlerNoResponse<MessageTypeInfoList> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageTypeInfoList message, final MessageContext context) {
        final List<Class> types = message.getTypes();

        final SynchronizationManagerClientImpl synchronization = Synchronization.INSTANCE.getClient();

        synchronization.registerTypes(types);

        return null;
    }
}
