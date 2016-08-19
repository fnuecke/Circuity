package li.cil.lib.network.handler;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageSynchronizeValue;
import li.cil.lib.network.message.MessageUnsubscribeComponent;
import li.cil.lib.synchronization.SynchronizationManagerClientImpl;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerSynchronizeValue extends AbstractMessageHandler<MessageSynchronizeValue, MessageUnsubscribeComponent> {
    @Nullable
    @Override
    public MessageUnsubscribeComponent onMessage(final MessageSynchronizeValue message, final MessageContext context) {
        final int dimension = message.getDimension();
        final long componentId = message.getComponentId();

        // Check if the world still exists. The client *should* have destroyed and
        // unsubscribed from all entities in a world it unloads, but maybe someone
        // forgot to, or the notification to the server was still on the way to the
        // server when this packet was sent to the client.
        final World world = getWorld(dimension, context);
        if (world == null) {
            return new MessageUnsubscribeComponent(dimension, componentId);
        }

        // We need/want to cast here, because we don't want this method in the public API.
        final EntityComponentManagerImpl manager = (EntityComponentManagerImpl) SillyBeeAPI.manager.getManager(world);

        final ReentrantLock lock = manager.getLock();
        lock.lock();
        try {
            // Check if the component still exists. If it does not, the client destroyed
            // the component's entity while this packet was on its way to the client.
            final Component component = manager.getComponent(componentId);
            if (component == null) {
                // Just in case, though, tell the server we're not interested in this
                // component anymore. This will typically result in a no-op on the server,
                // as this should, as mentioned above, only happen when the client
                // destroyed the component's entity.
                return new MessageUnsubscribeComponent(dimension, componentId);
            }

            // We need/want to cast here, because we don't want this method in the public API.
            final SynchronizationManagerClientImpl synchronization = (SynchronizationManagerClientImpl) SillyBeeAPI.synchronization.getClient();

            synchronization.update(component, message.getValues());

            return null;
        } finally {
            lock.unlock();
        }
    }
}
