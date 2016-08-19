package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractInterruptSink;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusStateAware;
import li.cil.circuity.api.bus.device.InterruptList;
import li.cil.circuity.server.processor.BusControllerAccess;
import li.cil.circuity.server.processor.z80.Z80;
import li.cil.circuity.util.IntelHexLoader;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Serializable
public class BusDeviceZ80Processor extends AbstractComponentBusDevice {
    private static final int CYCLES_PER_TICK = 2_000_000 / 20;

    @Serialize
    private final BusDeviceZ80Impl device = new BusDeviceZ80Impl();

    // --------------------------------------------------------------------- //

    public BusDeviceZ80Processor(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    protected BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //
    // ActivationListener

    @Serializable
    private class BusDeviceZ80Impl extends AbstractInterruptSink implements BusStateAware, AsyncTickable {
        @Serialize
        private final Z80 z80;

        // --------------------------------------------------------------------- //

        public BusDeviceZ80Impl() {
            this.z80 = new Z80(new BusControllerAccess(this::getController, 0), new BusControllerAccess(this::getController, 0x10000));
        }

        // --------------------------------------------------------------------- //
        // AbstractInterruptSink

        @Override
        protected int[] validateInterrupts(final InterruptList interrupts) {
            return interrupts.take(2);
        }

        @Override
        protected void handleInterrupt(final int interrupt, final int data) {
            if (interrupt == 0) {
                z80.nmi();
            } else {
                z80.irq((byte) data);
            }
        }

        // --------------------------------------------------------------------- //
        // BusStateAware

        @Override
        public void handleBusOnline() {
            // TODO Configurable address.
            final int eepromAddress = 0xC100;
            for (int offset = 0; offset < 4 * 1024; offset++) {
                final int value = controller.mapAndRead(eepromAddress + offset);
                controller.mapAndWrite(offset, value);
            }

            try {
                IntelHexLoader.load(Files.readAllLines(Paths.get("C:\\Users\\fnuecke\\Desktop\\sdcc\\monitor.ihx")), controller::mapAndWrite);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleBusOffline() {
            device.z80.reset(0);
        }

        // --------------------------------------------------------------------- //
        // AsyncTickable

        @Override
        public void updateAsync() {
            z80.run(CYCLES_PER_TICK);
        }
    }
}
