package li.cil.lib.network;

import li.cil.lib.network.handler.MessageHandlerComponentData;
import li.cil.lib.network.handler.MessageHandlerSynchronizationInitialize;
import li.cil.lib.network.handler.MessageHandlerSynchronizationSubscribe;
import li.cil.lib.network.handler.MessageHandlerSynchronizationUnsubscribeComponent;
import li.cil.lib.network.handler.MessageHandlerSynchronizationUnsubscribeEntity;
import li.cil.lib.network.handler.MessageHandlerSynchronizeValue;
import li.cil.lib.network.handler.MessageHandlerTypeInfo;
import li.cil.lib.network.handler.MessageHandlerTypeInfoList;
import li.cil.lib.network.message.MessageComponentData;
import li.cil.lib.network.message.MessageSynchronizationInitialize;
import li.cil.lib.network.message.MessageSynchronizationSubscribe;
import li.cil.lib.network.message.MessageSynchronizationUnsubscribeComponent;
import li.cil.lib.network.message.MessageSynchronizationUnsubscribeEntity;
import li.cil.lib.network.message.MessageSynchronizeValue;
import li.cil.lib.network.message.MessageTypeInfo;
import li.cil.lib.network.message.MessageTypeInfoList;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public enum Network {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final SimpleNetworkWrapper wrapper;

    // --------------------------------------------------------------------- //

    private enum Messages {
        TypeInfoList,
        TypeInfo,
        SynchronizationSubscribe,
        SynchronizationInitialize,
        SynchronizationUnsubscribeEntity,
        SynchronizationUnsubscribeComponent,
        SynchronizeValue,
        ComponentData
    }

    // --------------------------------------------------------------------- //

    Network() {
        final String channelName = Network.class.getName();
        wrapper = NetworkRegistry.INSTANCE.newSimpleChannel(channelName);
    }

    // --------------------------------------------------------------------- //

    public SimpleNetworkWrapper getWrapper() {
        return wrapper;
    }

    // --------------------------------------------------------------------- //

    public static void init() {
        final SimpleNetworkWrapper wrapper = INSTANCE.getWrapper();
        wrapper.registerMessage(MessageHandlerTypeInfoList.class, MessageTypeInfoList.class, Messages.TypeInfoList.ordinal(), Side.CLIENT);
        wrapper.registerMessage(MessageHandlerTypeInfo.class, MessageTypeInfo.class, Messages.TypeInfo.ordinal(), Side.CLIENT);
        wrapper.registerMessage(MessageHandlerSynchronizationSubscribe.class, MessageSynchronizationSubscribe.class, Messages.SynchronizationSubscribe.ordinal(), Side.SERVER);
        wrapper.registerMessage(MessageHandlerSynchronizationInitialize.class, MessageSynchronizationInitialize.class, Messages.SynchronizationInitialize.ordinal(), Side.CLIENT);
        wrapper.registerMessage(MessageHandlerSynchronizationUnsubscribeEntity.class, MessageSynchronizationUnsubscribeEntity.class, Messages.SynchronizationUnsubscribeEntity.ordinal(), Side.SERVER);
        wrapper.registerMessage(MessageHandlerSynchronizationUnsubscribeComponent.class, MessageSynchronizationUnsubscribeComponent.class, Messages.SynchronizationUnsubscribeComponent.ordinal(), Side.SERVER);
        wrapper.registerMessage(MessageHandlerSynchronizeValue.class, MessageSynchronizeValue.class, Messages.SynchronizeValue.ordinal(), Side.CLIENT);
        wrapper.registerMessage(MessageHandlerComponentData.class, MessageComponentData.class, Messages.ComponentData.ordinal(), Side.CLIENT);
        wrapper.registerMessage(MessageHandlerComponentData.class, MessageComponentData.class, Messages.ComponentData.ordinal(), Side.SERVER);
    }
}
