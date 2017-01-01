package li.cil.lib.network.handler;

import li.cil.lib.common.Manager;
import li.cil.lib.common.Synchronization;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageSynchronizationInitialize;
import li.cil.lib.network.message.MessageSynchronizationUnsubscribeEntity;
import li.cil.lib.synchronization.SynchronizationManagerClientImpl;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerSynchronizationInitialize extends AbstractMessageHandler<MessageSynchronizationInitialize, MessageSynchronizationUnsubscribeEntity> {
    @Nullable
    @Override
    public MessageSynchronizationUnsubscribeEntity onMessage(final MessageSynchronizationInitialize message, final MessageContext context) {
        final int dimension = message.getDimension();
        final long entity = message.getEntity();
        final NBTTagList componentsNbt = message.getComponents();

        final World world = getWorld(dimension, context);
        if (world == null) {
            return new MessageSynchronizationUnsubscribeEntity(dimension, entity);
        }

        final SynchronizationManagerClientImpl synchronization = Synchronization.INSTANCE.getClient();
        final EntityComponentManagerImpl manager = Manager.INSTANCE.getManager(world);

        final ReentrantLock lock = manager.getLock();
        lock.lock();
        try {
            if (!manager.hasEntity(entity)) {
                return new MessageSynchronizationUnsubscribeEntity(dimension, entity);
            }

            for (int i = 0; i < componentsNbt.tagCount(); i++) {
                final NBTTagCompound componentInfo = componentsNbt.getCompoundTagAt(i);
                final long componentId = componentInfo.getLong(MessageSynchronizationInitialize.COMPONENT_ID_TAG);
                final Class componentClass = SillyBeeAPI.synchronization.getClient().getTypeByTypeId(componentInfo.getInteger(MessageSynchronizationInitialize.COMPONENT_CLASS_TAG));
                assert (componentClass != null);

                @SuppressWarnings("unchecked")
                final Component component = manager.addComponent(entity, componentId, componentClass);
                if (manager.hasComponent(component) && componentInfo.hasKey(MessageSynchronizationInitialize.COMPONENT_TAG)) {
                    final NBTTagList componentNbt = componentInfo.getTagList(MessageSynchronizationInitialize.COMPONENT_TAG, Constants.NBT.TAG_COMPOUND);
                    synchronization.update(component, componentNbt);
                }
            }
        } finally {
            lock.unlock();
        }

        return null;
    }
}
