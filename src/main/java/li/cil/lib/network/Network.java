package li.cil.lib.network;

import li.cil.lib.network.handler.MessageHandlerComponentData;
import li.cil.lib.network.handler.MessageHandlerInitialize;
import li.cil.lib.network.handler.MessageHandlerSubscribe;
import li.cil.lib.network.handler.MessageHandlerSynchronizeValue;
import li.cil.lib.network.handler.MessageHandlerTypeInfo;
import li.cil.lib.network.handler.MessageHandlerTypeInfoList;
import li.cil.lib.network.handler.MessageHandlerUnsubscribeComponent;
import li.cil.lib.network.handler.MessageHandlerUnsubscribeEntity;
import li.cil.lib.network.message.MessageComponentData;
import li.cil.lib.network.message.MessageInitialize;
import li.cil.lib.network.message.MessageSubscribe;
import li.cil.lib.network.message.MessageSynchronizeValue;
import li.cil.lib.network.message.MessageTypeInfo;
import li.cil.lib.network.message.MessageTypeInfoList;
import li.cil.lib.network.message.MessageUnsubscribeComponent;
import li.cil.lib.network.message.MessageUnsubscribeEntity;
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
        Subscribe,
        Initialize,
        UnsubscribeEntity,
        UnsubscribeComponent,
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
        wrapper.registerMessage(MessageHandlerSubscribe.class, MessageSubscribe.class, Messages.Subscribe.ordinal(), Side.SERVER);
        wrapper.registerMessage(MessageHandlerInitialize.class, MessageInitialize.class, Messages.Initialize.ordinal(), Side.CLIENT);
        wrapper.registerMessage(MessageHandlerUnsubscribeEntity.class, MessageUnsubscribeEntity.class, Messages.UnsubscribeEntity.ordinal(), Side.SERVER);
        wrapper.registerMessage(MessageHandlerUnsubscribeComponent.class, MessageUnsubscribeComponent.class, Messages.UnsubscribeComponent.ordinal(), Side.SERVER);
        wrapper.registerMessage(MessageHandlerSynchronizeValue.class, MessageSynchronizeValue.class, Messages.SynchronizeValue.ordinal(), Side.CLIENT);
        wrapper.registerMessage(MessageHandlerComponentData.class, MessageComponentData.class, Messages.ComponentData.ordinal(), Side.CLIENT);
        wrapper.registerMessage(MessageHandlerComponentData.class, MessageComponentData.class, Messages.ComponentData.ordinal(), Side.SERVER);
    }
}
