package li.cil.lib.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public final class NullStorage<T> implements Capability.IStorage<T> {
    @Override
    @Nullable
    public NBTBase writeNBT(final Capability<T> capability, final T instance, final EnumFacing side) {
        return null;
    }

    @Override
    public void readNBT(final Capability<T> capability, final T instance, final EnumFacing side, final NBTBase nbt) {
    }
}
