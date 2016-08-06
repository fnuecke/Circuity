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
        final NBTTagCompound compound = new NBTTagCompound();

        if (instance instanceof EEPROMImpl) {
            final EEPROMImpl eeprom = (EEPROMImpl) instance;
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
    public void readNBT(final Capability<EEPROM> capability, final EEPROM instance, final EnumFacing side, final NBTBase nbt) {
        if (instance instanceof EEPROMImpl) {
            final EEPROMImpl eeprom = (EEPROMImpl) instance;
            if (nbt instanceof NBTTagCompound) {
                final NBTTagCompound compound = (NBTTagCompound) nbt;
                if (compound.hasKey(MOST_SIGNIFICANT_BITS_TAG) && compound.hasKey(LEAST_SIGNIFICANT_BITS_TAG)) {
                    final long mostSignificantBits = compound.getLong(MOST_SIGNIFICANT_BITS_TAG);
                    final long leastSignificantBits = compound.getLong(LEAST_SIGNIFICANT_BITS_TAG);
                    eeprom.uuid = new UUID(mostSignificantBits, leastSignificantBits);
                }
            } else {
                ModSillyBee.getLogger().error("Trying to load EEPROM from incompatible tag type.");
            }
        } else {
            ModSillyBee.getLogger().error("Trying to load a non-default implementation of EEPROM using the default EEPROM storage implementation.");
        }
    }
}
