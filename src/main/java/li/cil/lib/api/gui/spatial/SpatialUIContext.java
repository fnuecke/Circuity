package li.cil.lib.api.gui.spatial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

/**
 * Common context information passed along to both {@link SpatialUIProviderClient#provide(SpatialUIContext)}
 * and {@link SpatialUIProviderServer#provide(SpatialUIContext)}.
 * <p>
 * Beside the current target object of the UI (i.e. the object for which the
 * UI has been created), it in particular provides an easy way of communication
 * between the client and server side of a spatial UI via {@link #sendData(NBTTagCompound)}.
 */
public interface SpatialUIContext {
    /**
     * The player the UI has been opened for/by.
     * <p>
     * On the client this will always be the local player.
     *
     * @return the player this UI has been opened for.
     */
    EntityPlayer getPlayer();

    /**
     * The target object of the UI, i.e. the object the UI was opened for.
     * <p>
     * This is typically a tile entity of the block the player is currently
     * looking at.
     *
     * @return the target object of the UI.
     */
    ICapabilityProvider getTarget();

    /**
     * The side of the target the UI was opened for.
     * <p>
     * This is typically the side of the block the player is currently looking
     * at.
     *
     * @return the side of the target object.
     */
    @Nullable
    EnumFacing getSide();

    /**
     * For contexts of a {@link SpatialUIClient}, this will send the specified
     * data to its {@link SpatialUIServer} and vice versa.
     * <p>
     * Received data is forwarded to {@link SpatialUIClient#handleData(NBTTagCompound)}
     * and {@link SpatialUIServer#handleData(NBTTagCompound)}.
     *
     * @param value the data to send.
     */
    void sendData(final NBTTagCompound value);
}
