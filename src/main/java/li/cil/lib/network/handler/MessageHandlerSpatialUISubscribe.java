package li.cil.lib.network.handler;

import li.cil.lib.common.gui.spatial.SpatialUIManagerServer;
import li.cil.lib.network.message.MessageSpatialUISubscribe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class MessageHandlerSpatialUISubscribe extends AbstractMessageHandlerNoResponse<MessageSpatialUISubscribe> {
    @Nullable
    @Override
    public IMessage onMessage(final MessageSpatialUISubscribe message, final MessageContext context) {
        final int dimension = message.getDimension();
        final BlockPos pos = message.getPos();
        final EnumFacing side = message.getSide();

        final World world = getWorld(dimension, context);
        if (world != null && world.isBlockLoaded(pos)) {
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity != null) {
                SpatialUIManagerServer.INSTANCE.subscribe(context.getServerHandler(), message.getProviderClass(), tileEntity, side);
            }
        }

        return null;
    }
}
