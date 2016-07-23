package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusSegment;
import li.cil.circuity.common.bus.BusControllerImpl;
import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Serializable
public class ComponentBlockBusController extends ComponentBlockBusSegment {
    private final Set<BusDevice> neighbors = new HashSet<>();

    @Serialize
    private final BlockBusControllerImpl controller = new BlockBusControllerImpl();

    public ComponentBlockBusController(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public void onCreate() {
        controller.scheduleScan();
    }

    @Override
    public void onDestroy() {
        // Not needed, because our controller is by necessity our own, so we
        // really don't need to schedule a scan anymore.
        // super.onDestroy();
        controller.dispose();
    }

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityBusDevice.BUS_DEVICE_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityBusDevice.BUS_DEVICE_CAPABILITY) {
            return CapabilityBusDevice.BUS_DEVICE_CAPABILITY.cast(controller);
        }
        return null;
    }

    // --------------------------------------------------------------------- //

    private Iterable<BusSegment> getSeedSegments() {
        final Collection<BusDevice> devices = getDevicesCollection();
        return devices.stream().
                filter(device -> device instanceof BusSegment).
                map(device -> (BusSegment) device).
                collect(Collectors.toSet());
    }

    private final class BlockBusControllerImpl extends BusControllerImpl {
        @Override
        protected World getBusWorld() {
            return ComponentBlockBusController.this.getWorld();
        }

        @Override
        protected Iterable<BusSegment> getSeedSegments() {
            return ComponentBlockBusController.this.getSeedSegments();
        }
    }
}
