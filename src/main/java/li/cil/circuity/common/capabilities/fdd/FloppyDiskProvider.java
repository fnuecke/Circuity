package li.cil.circuity.common.capabilities.fdd;

import li.cil.circuity.api.item.FloppyDisk;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

public final class FloppyDiskProvider implements ICapabilityProvider, INBTSerializable<NBTBase> {
    private final FloppyDisk floppy;

    // --------------------------------------------------------------------- //

    public FloppyDiskProvider() {
        this.floppy = CapabilityFloppyDisk.FLOPPY_DISK_CAPABILITY.getDefaultInstance();
    }

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityFloppyDisk.FLOPPY_DISK_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityFloppyDisk.FLOPPY_DISK_CAPABILITY) {
            return CapabilityFloppyDisk.FLOPPY_DISK_CAPABILITY.cast(floppy);
        }
        return null;
    }

    // --------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTBase serializeNBT() {
        return CapabilityFloppyDisk.FLOPPY_DISK_CAPABILITY.writeNBT(floppy, null);
    }

    @Override
    public void deserializeNBT(final NBTBase nbt) {
        CapabilityFloppyDisk.FLOPPY_DISK_CAPABILITY.readNBT(floppy, null, nbt);
    }
}
