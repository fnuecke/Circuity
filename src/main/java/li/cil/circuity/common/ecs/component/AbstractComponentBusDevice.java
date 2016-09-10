package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
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

    public abstract BusElement getBusElement();

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityBusElement.BUS_ELEMENT_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityBusElement.BUS_ELEMENT_CAPABILITY) {
            return CapabilityBusElement.BUS_ELEMENT_CAPABILITY.cast(getBusElement());
        }
        return null;
    }

    protected static long getBusControllerId(@Nullable final BusController controller) {
        if (controller instanceof BusControllerBlock.BlockBusControllerImpl) {
            final BusControllerBlock.BlockBusControllerImpl hosted = (BusControllerBlock.BlockBusControllerImpl) controller;
            return hosted.getComponentId();
        } else {
            return 0;
        }
    }
}
