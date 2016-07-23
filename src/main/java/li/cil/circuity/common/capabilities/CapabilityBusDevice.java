package li.cil.circuity.common.capabilities;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public final class CapabilityBusDevice {
    @CapabilityInject(BusDevice.class)
    public static Capability<BusDevice> BUS_DEVICE_CAPABILITY;

    public static void register() {
        CapabilityManager.INSTANCE.register(BusDevice.class, new Capability.IStorage<BusDevice>() {
            @Override
            @Nullable
            public NBTBase writeNBT(final Capability<BusDevice> capability, final BusDevice instance, final EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(final Capability<BusDevice> capability, final BusDevice instance, final EnumFacing side, final NBTBase nbt) {
            }
        }, () -> new BusDevice() {
            @Override
            public void setBusController(@Nullable final BusController controller) {
            }
        });
    }
}
