package li.cil.lib.network.handler;

import li.cil.lib.Manager;
import li.cil.lib.Synchronization;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageSynchronizationUnsubscribeComponent;
import li.cil.lib.synchronization.SynchronizationManagerServerImpl;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerSynchronizationUnsubscribeComponent extends AbstractMessageHandlerNoResponse<MessageSynchronizationUnsubscribeComponent> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageSynchronizationUnsubscribeComponent message, final MessageContext context) {
        final int dimension = message.getDimension();
        final long componentId = message.getComponentId();

        final World world = getWorld(dimension, context);
        if (world != null) {
            final SynchronizationManagerServerImpl synchronization = Synchronization.INSTANCE.getServer();
            final EntityComponentManagerImpl manager = Manager.INSTANCE.getManager(world);

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
