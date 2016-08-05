package li.cil.circuity.common.capabilities.eeprom;

import li.cil.circuity.api.item.EEPROM;
import li.cil.lib.ModSillyBee;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.UUID;

public final class EEPROMStorage implements Capability.IStorage<EEPROM> {
    private static final String MOST_SIGNIFICANT_BITS_TAG = "mostSigBits";
    private static final String LEAST_SIGNIFICANT_BITS_TAG = "leastSignificantBits";

    // --------------------------------------------------------------------- //
    // Capability.IStorage

    @Override
    @Nullable
    public NBTBase writeNBT(final Capability<EEPROM> capability, final EEPROM instance, final EnumFacing side) {
        if (instance instanceof EEPROMImpl) {
            final EEPROMImpl eeprom = (EEPROMImpl) instance;
            if (eeprom.uuid != null) {
                final NBTTagCompound compound = new NBTTagCompound();
                compound.setLong(MOST_SIGNIFICANT_BITS_TAG, eeprom.uuid.getMostSignificantBits());
                compound.setLong(LEAST_SIGNIFICANT_BITS_TAG, eeprom.uuid.getLeastSignificantBits());
                return compound;
            }
        } else {
            ModSillyBee.getLogger().error("Trying to save a non-default implementation of EEPROM using the default EEPROM storage implementation.");
        }

        return null;
    }

    @Override
    public void readNBT(final Capability<EEPROM> capability, final EEPROM instance, final EnumFacing side, final NBTBase nbt) {
        if (instance instanceof EEPROMImpl) {
            if (nbt instanceof NBTTagCompound) {
                final EEPROMImpl eeprom = (EEPROMImpl) instance;
                final NBTTagCompound compound = (NBTTagCompound) nbt;
                final long mostSignificantBits = compound.getLong(MOST_SIGNIFICANT_BITS_TAG);
                final long leastSignificantBits = compound.getLong(LEAST_SIGNIFICANT_BITS_TAG);
                eeprom.uuid = new UUID(mostSignificantBits, leastSignificantBits);
            }
        } else {
            ModSillyBee.getLogger().error("Trying to load a non-default implementation of EEPROM using the default EEPROM storage implementation.");
        }
    }
}
