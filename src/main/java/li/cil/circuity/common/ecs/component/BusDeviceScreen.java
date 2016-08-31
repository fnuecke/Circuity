package li.cil.circuity.common.ecs.component;

import li.cil.circuity.ModCircuity;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.BusChangeListener;
import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedUUID;

import javax.annotation.Nullable;
import java.util.UUID;

public class BusDeviceScreen extends AbstractComponentBusDevice {
    @Serialize
    private final ScreenImpl device = new ScreenImpl();
    @Serialize
    private final SynchronizedUUID rendererId = new SynchronizedUUID();

    // --------------------------------------------------------------------- //

    public BusDeviceScreen(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public ScreenRenderer getScreenRenderer() {
        final UUID id = rendererId.get();
        if (id != null) {
            final Object renderer = SillyBeeAPI.globalObjects.get(getWorld(), id);
            if (renderer instanceof ScreenRenderer) {
                return (ScreenRenderer) renderer;
            } else if (renderer != null) {
                ModCircuity.getLogger().warn("Got an incompatible type retrieving ScreenRenderer. UUID collision?");
            }
        }
        return null;
    }

    // --------------------------------------------------------------------- //
    // AbstractComponent

    @Override
    public void onDestroy() {
        super.onDestroy();

        final UUID id = rendererId.get();
        if (id != null) {
            SillyBeeAPI.globalObjects.remove(getWorld(), id);
        }
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //

    @Serializable
    public final class ScreenImpl extends AbstractBusDevice implements BusChangeListener {
        @Override
        public void handleBusChanged() {
            // TODO Build list of candidates, allow user to select current one.
            for (final BusDevice device : controller.getDevices()) {
                if (device instanceof ScreenRenderer) {
                    final ScreenRenderer renderer = (ScreenRenderer) device;
                    BusDeviceScreen.this.rendererId.set(renderer.getPersistentId());
                    return;
                }
            }
        }
    }
}
