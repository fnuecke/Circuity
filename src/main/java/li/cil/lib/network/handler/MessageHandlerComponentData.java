package li.cil.lib.network.handler;

import io.netty.buffer.ByteBuf;
import li.cil.lib.ModSillyBee;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.component.MessageReceiver;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.message.MessageComponentData;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandlerComponentData extends AbstractMessageHandlerNoResponse<MessageComponentData> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageComponentData message, final MessageContext context) {
        final int dimension = message.getDimension();
        final long componentId = message.getComponentId();
        final ByteBuf data = message.getData();

        final World world = getWorld(dimension, context);
        if (world != null) {
            // We need/want to cast here, because we don't want this method in the public API.
            final EntityComponentManagerImpl manager = (EntityComponentManagerImpl) SillyBeeAPI.manager.getManager(world);

            final ReentrantLock lock = manager.getLock();
            lock.lock();
            try {
                final Component component = manager.getComponent(componentId);
                if (component instanceof MessageReceiver) {
                    final MessageReceiver receiver = (MessageReceiver) component;
                    receiver.handleComponentData(data);
                } else if (component != null) {
                    ModSillyBee.getLogger().warn("Received a message for a component that is not a MessageReceiver: {}", component.getClass());
                }
            } finally {
                lock.unlock();
            }
        }

        return null;
    }
}
