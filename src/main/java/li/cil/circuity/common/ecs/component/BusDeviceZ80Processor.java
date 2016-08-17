package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusStateAware;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.server.processor.BusControllerAccess;
import li.cil.circuity.server.processor.z80.Z80;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

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
    private class BusDeviceZ80Impl extends AbstractBusDevice implements InterruptSink, BusStateAware, AsyncTickable {
        @Serialize
        private final Z80 z80;

        // --------------------------------------------------------------------- //

        public BusDeviceZ80Impl() {
            this.z80 = new Z80(new BusControllerAccess(this::getController, 0), new BusControllerAccess(this::getController, 0x10000));
        }

        // --------------------------------------------------------------------- //
        // InterruptSink

        @Override
        public int[] getAcceptedInterrupts(final int[] interrupts) {
            return new int[]{interrupts[0]};
        }

        @Override
        public void setAcceptedInterrupts(@Nullable final int[] interrupts) {
        }

        @Override
        public void interrupt(final int interrupt) {
            // TODO THIS IS BULLSHIT
            // No seriously, it's just for testing. Should replace with
            // providing multiple interrupts, then getting the index of
            // the one that's triggered and providing that.
            if (interrupt < 0) {
                z80.nmi();
            } else {
                z80.irq((byte) interrupt);
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
