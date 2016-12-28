package li.cil.lib.network.handler;

import li.cil.lib.Manager;
import li.cil.lib.Synchronization;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageSynchronizationInitialize;
import li.cil.lib.network.message.MessageSynchronizationSubscribe;
import li.cil.lib.synchronization.SynchronizationManagerServerImpl;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerSynchronizationSubscribe extends AbstractMessageHandler<MessageSynchronizationSubscribe, MessageSynchronizationInitialize> {
    @Nullable
    @Override
    public MessageSynchronizationInitialize onMessage(final MessageSynchronizationSubscribe message, final MessageContext context) {
        final int dimension = message.getDimension();
        final long entity = message.getEntity();

        final World world = getWorld(dimension, context);
        if (world != null) {
            final SynchronizationManagerServerImpl synchronization = Synchronization.INSTANCE.getServer();
            final EntityComponentManagerImpl manager = Manager.INSTANCE.getManager(world);

            final ReentrantLock lock = manager.getLock();
            lock.lock();
            try {
                synchronization.subscribeEntity(context.getServerHandler(), manager, entity);

                final NBTTagList componentsNbt = new NBTTagList();
                for (final Component component : manager.getComponents(entity)) {
                    componentsNbt.appendTag(MessageSynchronizationInitialize.getComponentNBT(component));
                }

                return new MessageSynchronizationInitialize(dimension, entity, componentsNbt);
            } finally {
                lock.unlock();
            }
        }

        return null;
    }
}
