package li.cil.lib.network.handler;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.network.message.MessageUnsubscribeEntity;
import li.cil.lib.synchronization.SynchronizationManagerServerImpl;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class MessageHandlerUnsubscribeEntity extends AbstractMessageHandlerNoResponse<MessageUnsubscribeEntity> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageUnsubscribeEntity message, final MessageContext ctx) {
        final int dimension = message.getDimension();
        final long entity = message.getEntity();

        final World world = getWorld(dimension, ctx);
        if (world != null) {
            // We need/want to cast here, because we don't want this method in the public API.
            final SynchronizationManagerServerImpl synchronization = (SynchronizationManagerServerImpl) SillyBeeAPI.synchronization.getServer();

            synchronization.unsubscribeEntity(ctx.getServerHandler(), SillyBeeAPI.manager.getManager(world), entity);
        }

        return null;
    }
}
