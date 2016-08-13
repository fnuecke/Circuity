package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.server.processor.z80.Z80;
import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedBoolean;
import net.minecraft.util.ITickable;

@Serializable
public class BusDeviceZ80Processor extends AbstractComponentBusDevice implements ITickable {
    private static final int CYCLES_PER_TICK = Z80.CYCLES_1MHZ * 2;

    @Serialize
    private final Z80 device = new Z80();

    @Serialize
    private final SynchronizedBoolean isRunning = new SynchronizedBoolean();

    private Redstone redstone;

    // --------------------------------------------------------------------- //

    public BusDeviceZ80Processor(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        redstone = getComponent(Redstone.class).orElseThrow(IllegalStateException::new);
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    protected BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        final BusController controller = device.getController();
        if (redstone.getInput(null) > 0 && controller != null) {
            if (!isRunning.get()) {
                device.reset();

                // TODO Configurable address.
                final int eepromAddress = 0xC100;
                for (int offset = 0; offset < 4 * 1024; offset++) {
                    final int value = controller.mapAndRead(eepromAddress + offset);
                    controller.mapAndWrite(offset, value);
                }

                isRunning.set(true);
            }

            device.run(CYCLES_PER_TICK);
        } else {
            isRunning.set(false);
        }
    }
}
