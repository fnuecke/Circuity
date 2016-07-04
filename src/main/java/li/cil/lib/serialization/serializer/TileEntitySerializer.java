package li.cil.lib.serialization.serializer;

import li.cil.lib.serialization.SerializerCollectionImpl;
import li.cil.lib.util.WorldUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class TileEntitySerializer implements Serializer {
    private static final String DIMENSION_TAG = "dimension";
    private static final String TILEENTITY_POS_TAG = "position";

    // --------------------------------------------------------------------- //

    private final SerializerCollectionImpl serialization;

    // --------------------------------------------------------------------- //

    public TileEntitySerializer(final SerializerCollectionImpl serialization) {
        this.serialization = serialization;
    }

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return TileEntity.class.isAssignableFrom(clazz);
    }

    @Override
    public NBTBase serialize(final Object object) {
        final TileEntity tileEntity = (TileEntity) object;
        final World world = tileEntity.getWorld();

        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(DIMENSION_TAG, world.provider.getDimension());
        tag.setTag(TILEENTITY_POS_TAG, serialization.serialize(tileEntity.getPos()));

        return tag;
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final NBTTagCompound entityInfo = (NBTTagCompound) tag;
        final int dimension = entityInfo.getInteger(DIMENSION_TAG);
        final World world = WorldUtil.getWorld(dimension, serialization.isClientSide());
        if (world != null) {
            final BlockPos pos = serialization.deserialize(BlockPos.class, entityInfo.getTag(TILEENTITY_POS_TAG));
            assert (pos != null);
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity != null) {
                return tileEntity;
            }
        }

        return null;
    }

    @Override
    @Nullable
    public Object getDefault() {
        return null;
    }
}
