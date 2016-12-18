package li.cil.lib.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

public final class WorldUtil {
    @Nullable
    public static World getWorld(final int dimension, final Side side) {
        return getWorld(dimension, side.isClient());
    }

    @Nullable
    public static World getWorld(final int dimension, final boolean isClientSide) {
        if (isClientSide) {
            return getWorldClient(dimension);
        } else {
            return getWorldServer(dimension);
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private static World getWorldClient(final int dimension) {
        final World world = Minecraft.getMinecraft().world;
        if (world.provider.getDimension() == dimension) {
            return world;
        } else {
            return null;
        }
    }

    @Nullable
    private static World getWorldServer(final int dimension) {
        return DimensionManager.getWorld(dimension);
    }

    // --------------------------------------------------------------------- //

    private WorldUtil() {
    }
}
