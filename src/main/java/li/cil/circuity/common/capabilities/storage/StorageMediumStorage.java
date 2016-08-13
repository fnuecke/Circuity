package li.cil.circuity.common.capabilities.storage;

import li.cil.circuity.api.item.StorageMedium;
import li.cil.lib.ModSillyBee;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.UUID;

public final class StorageMediumStorage<T extends StorageMedium> implements Capability.IStorage<T> {
    private static final String MOST_SIGNIFICANT_BITS_TAG = "mostSigBits";
    private static final String LEAST_SIGNIFICANT_BITS_TAG = "leastSignificantBits";

    // --------------------------------------------------------------------- //
    // Capability.IStorage

    @Override
    @Nullable
    public NBTBase writeNBT(final Capability<T> capability, final T instance, final EnumFacing side) {
        final NBTTagCompound compound = new NBTTagCompound();

        if (instance instanceof AbstractStorageMedium) {
            final AbstractStorageMedium eeprom = (AbstractStorageMedium) instance;
            if (eeprom.uuid != null) {
                compound.setLong(MOST_SIGNIFICANT_BITS_TAG, eeprom.uuid.getMostSignificantBits());
                compound.setLong(LEAST_SIGNIFICANT_BITS_TAG, eeprom.uuid.getLeastSignificantBits());
            }
        } else {
            ModSillyBee.getLogger().error("Trying to save a non-default implementation of EEPROM using the default EEPROM storage implementation.");
        }

        return compound;
    }

    @Override
    public void readNBT(final Capability<T> capability, final T instance, final EnumFacing side, final NBTBase nbt) {
        if (instance instanceof AbstractStorageMedium) {
            final AbstractStorageMedium eeprom = (AbstractStorageMedium) instance;
            if (nbt instanceof NBTTagCompound) {
                final NBTTagCompound compound = (NBTTagCompound) nbt;
                if (compound.hasKey(MOST_SIGNIFICANT_BITS_TAG) && compound.hasKey(LEAST_SIGNIFICANT_BITS_TAG)) {
                    final long mostSignificantBits = compound.getLong(MOST_SIGNIFICANT_BITS_TAG);
                    final long leastSignificantBits = compound.getLong(LEAST_SIGNIFICANT_BITS_TAG);
                    eeprom.uuid = new UUID(mostSignificantBits, leastSignificantBits);
                }
            } else {
                ModSillyBee.getLogger().error("Trying to load storage medium from incompatible tag type.");
            }
        } else {
            ModSillyBee.getLogger().error("Trying to load a non-default implementation of storage medium using the default storage medium storage implementation.");
        }
    }
}
