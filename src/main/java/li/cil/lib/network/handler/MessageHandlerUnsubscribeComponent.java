package li.cil.lib.network.handler;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageUnsubscribeComponent;
import li.cil.lib.synchronization.SynchronizationManagerServerImpl;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerUnsubscribeComponent extends AbstractMessageHandlerNoResponse<MessageUnsubscribeComponent> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageUnsubscribeComponent message, final MessageContext context) {
        final int dimension = message.getDimension();
        final long componentId = message.getComponentId();

        final World world = getWorld(dimension, context);
        if (world != null) {
            // We need/want to cast here, because we don't want this method in the public API.
            final SynchronizationManagerServerImpl synchronization = (SynchronizationManagerServerImpl) SillyBeeAPI.synchronization.getServer();
            final EntityComponentManagerImpl manager = (EntityComponentManagerImpl) SillyBeeAPI.manager.getManager(world);

            final ReentrantLock lock = manager.getLock();
            lock.lock();
            try {
                synchronization.unsubscribeComponent(context.getServerHandler(), manager, componentId);
            } finally {
                lock.unlock();
            }
        }

        return null;
    }
}
