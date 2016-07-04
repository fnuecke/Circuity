package li.cil.lib.serialization.serializer;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public final class BlockPosSerializer implements Serializer {
    public static final String X_TAG = "x";
    public static final String Y_TAG = "y";
    public static final String Z_TAG = "z";

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz == BlockPos.class;
    }

    @Override
    public NBTBase serialize(final Object object) {
        final BlockPos pos = (BlockPos) object;
        final NBTTagCompound posInfo = new NBTTagCompound();
        posInfo.setInteger(X_TAG, pos.getX());
        posInfo.setInteger(Y_TAG, pos.getY());
        posInfo.setInteger(Z_TAG, pos.getZ());
        return posInfo;
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        final NBTTagCompound posInfo = (NBTTagCompound) tag;
        return new BlockPos(posInfo.getInteger(X_TAG), posInfo.getInteger(Y_TAG), posInfo.getInteger(Z_TAG));
    }

    @Nullable
    @Override
    public Object getDefault() {
        return BlockPos.ORIGIN;
    }
}
