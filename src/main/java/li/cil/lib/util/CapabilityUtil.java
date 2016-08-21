package li.cil.lib.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public final class CapabilityUtil {
    /**
     * Get a capability from the block at the specified location.
     * <p>
     * Failing that, if the tile entity implements the capability interface
     * directly, return that.
     *
     * @param world      the world the block is in.
     * @param pos        the position of the block
     * @param side       the side of the block.
     * @param capability the capability to get.
     * @param clazz      the type of the capability to get.
     * @param <T>        the type of the capability to get.
     * @return the capability or the tile entity implementing the interface; failing that, <code>null</code>.
     */
    @Nullable
    public static <T> T getCapability(final IBlockAccess world, final BlockPos pos, final EnumFacing side, final Capability<T> capability, final Class<T> clazz) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        return getCapability(tileEntity, side, capability, clazz);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> T getCapability(@Nullable final TileEntity tileEntity, final EnumFacing side, final Capability<T> capability, final Class<T> clazz) {
        if (tileEntity != null) {
            if (tileEntity.hasCapability(capability, side)) {
                return tileEntity.getCapability(capability, side);
            } else if (clazz.isAssignableFrom(tileEntity.getClass())) {
                return (T) tileEntity;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------- //

    private CapabilityUtil() {
    }
}
