package li.cil.circuity.common.item;

import li.cil.circuity.api.item.EEPROM;
import li.cil.circuity.common.capabilities.CapabilityEEPROM;
import li.cil.circuity.common.capabilities.NoSuchCapabilityException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

public class ItemEEPROM extends Item {
    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, final NBTTagCompound nbt) {
        return new CapabilityProviderEEPROM();
    }

    private static final class CapabilityProviderEEPROM implements ICapabilityProvider, INBTSerializable<NBTBase> {
        private final EEPROM eeprom;

        // --------------------------------------------------------------------- //

        private CapabilityProviderEEPROM() {
            this.eeprom = CapabilityEEPROM.EEPROM_CAPABILITY.getDefaultInstance();
        }

        // --------------------------------------------------------------------- //
        // ICapabilityProvider

        @Override
        public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
            return capability == CapabilityEEPROM.EEPROM_CAPABILITY;
        }

        @Override
        public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
            if (capability == CapabilityEEPROM.EEPROM_CAPABILITY) {
                return CapabilityEEPROM.EEPROM_CAPABILITY.cast(eeprom);
            }
            throw new NoSuchCapabilityException();
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
}
