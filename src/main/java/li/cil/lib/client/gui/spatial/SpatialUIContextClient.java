package li.cil.lib.client.gui.spatial;

import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.network.Network;
import li.cil.lib.network.message.MessageSpatialUIData;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

public class SpatialUIContextClient implements SpatialUIContext {
    private final ICapabilityProvider target;
    private final EnumFacing side;

    // --------------------------------------------------------------------- //

    public SpatialUIContextClient(final ICapabilityProvider target, final EnumFacing side) {
        this.target = target;
        this.side = side;
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
        return Minecraft.getMinecraft().player;
    }

    @Override
    public void sendData(final NBTTagCompound value) {
        Network.INSTANCE.getWrapper().sendToServer(new MessageSpatialUIData(value));
    }
}
