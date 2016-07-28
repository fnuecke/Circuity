package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.circuity.common.capabilities.NoSuchCapabilityException;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.ecs.component.AbstractComponent;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

public abstract class AbstractComponentBusDevice extends AbstractComponent implements ICapabilityProvider {
    protected AbstractComponentBusDevice(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    protected abstract BusDevice getDevice();

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityBusDevice.BUS_DEVICE_CAPABILITY;
    }

    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityBusDevice.BUS_DEVICE_CAPABILITY) {
            return CapabilityBusDevice.BUS_DEVICE_CAPABILITY.cast(getDevice());
        }
        throw new NoSuchCapabilityException();
    }
}
