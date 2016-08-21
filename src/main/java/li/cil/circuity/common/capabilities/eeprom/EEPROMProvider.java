package li.cil.circuity.common.capabilities.eeprom;

import li.cil.circuity.api.item.EEPROM;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

public final class EEPROMProvider implements ICapabilityProvider, INBTSerializable<NBTBase> {
    private final EEPROM eeprom;

    // --------------------------------------------------------------------- //

    public EEPROMProvider() {
        this.eeprom = CapabilityEEPROM.EEPROM_CAPABILITY.getDefaultInstance();
    }

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityEEPROM.EEPROM_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityEEPROM.EEPROM_CAPABILITY) {
            return CapabilityEEPROM.EEPROM_CAPABILITY.cast(eeprom);
        }
        return null;
    }

    // --------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTBase serializeNBT() {
        return CapabilityEEPROM.EEPROM_CAPABILITY.writeNBT(eeprom, null);
    }

    @Override
    public void deserializeNBT(final NBTBase nbt) {
        CapabilityEEPROM.EEPROM_CAPABILITY.readNBT(eeprom, null, nbt);
    }
}
