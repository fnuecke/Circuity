package li.cil.lib.network.handler;

import li.cil.lib.common.Synchronization;
import li.cil.lib.network.message.MessageTypeInfo;
import li.cil.lib.synchronization.SynchronizationManagerClientImpl;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class MessageHandlerTypeInfo extends AbstractMessageHandlerNoResponse<MessageTypeInfo> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageTypeInfo message, final MessageContext context) {
        final Class type = message.getType();
        final int typeId = message.getTypeId();

        final SynchronizationManagerClientImpl synchronization = Synchronization.INSTANCE.getClient();

        synchronization.registerType(type, typeId);

        return null;
    }
}
