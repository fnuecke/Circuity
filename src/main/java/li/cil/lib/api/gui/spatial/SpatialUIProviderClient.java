package li.cil.lib.api.gui.spatial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

/**
 * Used on the client to determine which UI to display for an object.
 */
public interface SpatialUIProviderClient {
    /**
     * This is called to determine if the provider works for the specified object.
     * <p>
     * The first provider that returns <code>true</code> will be used.
     *
     * @param value the object to check if the provider works for.
     * @param side  the side of the object to check for. May be <code>null</code>.
     * @return <code>true</code> if the provider works for the specified object; <code>false</code> otherwise.
     */
    boolean canProvideFor(final EntityPlayer player, final ICapabilityProvider value, @Nullable final EnumFacing side);

    /**
     * Provide a spatial UI for the specified object.
     * <p>
     * The specified context contains information about the target the UI is
     * created for, as well as a method that will allow sending data to the
     * server representation of the UI.
     *
     * @param context the context for the UI, such as the target object.
     * @return the UI to display for the specified object.
     */
    SpatialUIClient provide(final SpatialUIContext context);
}
