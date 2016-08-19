package li.cil.lib.network.handler;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageUnsubscribeEntity;
import li.cil.lib.synchronization.SynchronizationManagerServerImpl;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerUnsubscribeEntity extends AbstractMessageHandlerNoResponse<MessageUnsubscribeEntity> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageUnsubscribeEntity message, final MessageContext context) {
        final int dimension = message.getDimension();
        final long entity = message.getEntity();

        final World world = getWorld(dimension, context);
        if (world != null) {
            // We need/want to cast here, because we don't want this method in the public API.
            final SynchronizationManagerServerImpl synchronization = (SynchronizationManagerServerImpl) SillyBeeAPI.synchronization.getServer();
            final EntityComponentManagerImpl manager = (EntityComponentManagerImpl) SillyBeeAPI.manager.getManager(world);

            final ReentrantLock lock = manager.getLock();
            lock.lock();
            try {
                synchronization.unsubscribeEntity(context.getServerHandler(), SillyBeeAPI.manager.getManager(world), entity);
            } finally {
                lock.unlock();
            }
        }

        return null;
    }
}
