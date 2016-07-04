package li.cil.lib.network.handler;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.network.message.MessageTypeInfo;
import li.cil.lib.synchronization.SynchronizationManagerClientImpl;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class MessageHandlerTypeInfo extends AbstractMessageHandlerNoResponse<MessageTypeInfo> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageTypeInfo message, final MessageContext ctx) {
        final Class type = message.getType();
        final int typeId = message.getTypeId();

        // We need/want to cast here, because we don't want this method in the public API.
        final SynchronizationManagerClientImpl synchronization = (SynchronizationManagerClientImpl) SillyBeeAPI.synchronization.getClient();

        synchronization.registerType(type, typeId);

        return null;
    }
}
