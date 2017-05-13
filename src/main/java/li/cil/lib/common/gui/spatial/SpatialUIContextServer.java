package li.cil.lib.common.gui.spatial;

import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.network.Network;
import li.cil.lib.network.message.MessageSpatialUIData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

public class SpatialUIContextServer implements SpatialUIContext {
    private final ICapabilityProvider target;
    private final EnumFacing side;
    private final NetHandlerPlayServer client;

    // --------------------------------------------------------------------- //

    public SpatialUIContextServer(final ICapabilityProvider target, @Nullable final EnumFacing side, final NetHandlerPlayServer client) {
        this.target = target;
        this.side = side;
        this.client = client;
    }

    // --------------------------------------------------------------------- //

    @Override
    public ICapabilityProvider getTarget() {
        return target;
    }

    @Nullable
    @Override
    public EnumFacing getSide() {
        return side;
    }

    @Override
    public EntityPlayer getPlayer() {
        return client.player;
    }

    @Override
    public void sendData(final NBTTagCompound value) {
        Network.INSTANCE.getWrapper().sendTo(new MessageSpatialUIData(value), client.player);
    }
}
