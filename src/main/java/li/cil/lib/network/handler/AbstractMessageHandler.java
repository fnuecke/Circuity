package li.cil.lib.network.handler;

import li.cil.lib.util.WorldUtil;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

public abstract class AbstractMessageHandler<REQ extends IMessage, REPLY extends IMessage> implements IMessageHandler<REQ, REPLY> {
    protected void processSynchronously(final REQ message, final MessageContext context, final BiConsumer<REQ, MessageContext> processor) {
        final IThreadListener thread = FMLCommonHandler.instance().getWorldThread(context.netHandler);
        if (thread.isCallingFromMinecraftThread()) {
            processor.accept(message, context);
        } else {
            thread.addScheduledTask(() -> processor.accept(message, context));
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    protected World getWorld(final int dimension, final MessageContext context) {
        return WorldUtil.getWorld(dimension, context.side);
    }
}
