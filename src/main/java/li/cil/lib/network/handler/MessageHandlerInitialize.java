package li.cil.lib.network.handler;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageInitialize;
import li.cil.lib.network.message.MessageUnsubscribeEntity;
import li.cil.lib.synchronization.SynchronizationManagerClientImpl;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerInitialize extends AbstractMessageHandler<MessageInitialize, MessageUnsubscribeEntity> {
    @Nullable
    @Override
    public MessageUnsubscribeEntity onMessage(final MessageInitialize message, final MessageContext context) {
        final int dimension = message.getDimension();
        final long entity = message.getEntity();
        final NBTTagList componentsNbt = message.getComponents();

        final World world = getWorld(dimension, context);
        if (world == null) {
            return new MessageUnsubscribeEntity(dimension, entity);
        }

        // We need/want to cast here, because we don't want this method in the public API.
        final SynchronizationManagerClientImpl synchronization = (SynchronizationManagerClientImpl) SillyBeeAPI.synchronization.getClient();
        final EntityComponentManagerImpl manager = (EntityComponentManagerImpl) SillyBeeAPI.manager.getManager(world);

        final ReentrantLock lock = manager.getLock();
        lock.lock();
        try {
            if (!manager.hasEntity(entity)) {
                return new MessageUnsubscribeEntity(dimension, entity);
            }

            for (int i = 0; i < componentsNbt.tagCount(); i++) {
                final NBTTagCompound componentInfo = componentsNbt.getCompoundTagAt(i);
                final long componentId = componentInfo.getLong(MessageInitialize.COMPONENT_ID_TAG);
                final Class componentClass = SillyBeeAPI.synchronization.getClient().getTypeByTypeId(componentInfo.getInteger(MessageInitialize.COMPONENT_CLASS_TAG));
                assert (componentClass != null);

                final Component component = manager.addComponent(entity, componentId, componentClass);
                if (manager.hasComponent(component) && componentInfo.hasKey(MessageInitialize.COMPONENT_TAG)) {
                    final NBTTagList componentNbt = componentInfo.getTagList(MessageInitialize.COMPONENT_TAG, Constants.NBT.TAG_COMPOUND);
                    synchronization.update(component, componentNbt);
                }
            }
        } finally {
            lock.unlock();
        }

        return null;
    }
}
