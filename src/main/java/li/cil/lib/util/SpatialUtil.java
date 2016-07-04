package li.cil.lib.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class SpatialUtil {
    private static final Map<BlockPos, EnumFacing> VECTOR_TO_FACING = new HashMap<>();

    static {
        VECTOR_TO_FACING.put(BlockPos.ORIGIN.down(), EnumFacing.DOWN);
        VECTOR_TO_FACING.put(BlockPos.ORIGIN.up(), EnumFacing.UP);
        VECTOR_TO_FACING.put(BlockPos.ORIGIN.north(), EnumFacing.NORTH);
        VECTOR_TO_FACING.put(BlockPos.ORIGIN.south(), EnumFacing.SOUTH);
        VECTOR_TO_FACING.put(BlockPos.ORIGIN.west(), EnumFacing.WEST);
        VECTOR_TO_FACING.put(BlockPos.ORIGIN.east(), EnumFacing.EAST);
    }

    public static EnumFacing getNeighborFacing(final BlockPos pos, final BlockPos neighborPos) {
        final BlockPos delta = neighborPos.subtract(pos);
        return VECTOR_TO_FACING.get(delta);
    }

    private SpatialUtil() {
    }
}
