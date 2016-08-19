package li.cil.lib.network.handler;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageInitialize;
import li.cil.lib.network.message.MessageSubscribe;
import li.cil.lib.synchronization.SynchronizationManagerServerImpl;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerSubscribe extends AbstractMessageHandler<MessageSubscribe, MessageInitialize> {
    @Nullable
    @Override
    public MessageInitialize onMessage(final MessageSubscribe message, final MessageContext context) {
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
                synchronization.subscribeEntity(context.getServerHandler(), manager, entity);

                final NBTTagList componentsNbt = new NBTTagList();
                for (final Component component : manager.getComponents(entity)) {
                    componentsNbt.appendTag(MessageInitialize.getComponentNBT(component));
                }

                return new MessageInitialize(dimension, entity, componentsNbt);
            } finally {
                lock.unlock();
            }
        }

        return null;
    }
}
