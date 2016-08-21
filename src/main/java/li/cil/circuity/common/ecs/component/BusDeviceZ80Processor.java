package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractInterruptSink;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusStateAware;
import li.cil.circuity.api.bus.device.InterruptList;
import li.cil.circuity.common.Constants;
import li.cil.circuity.server.processor.BusControllerAccess;
import li.cil.circuity.server.processor.z80.Z80;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

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
            this.z80 = new Z80(new BusControllerAccess(this::getBusController, 0), new BusControllerAccess(this::getBusController, 0x10000));
        }

        // --------------------------------------------------------------------- //
        // AbstractInterruptSink

        @Override
        protected int[] validateAcceptedInterrupts(final InterruptList interrupts) {
            return interrupts.take(2);
        }

        @Override
        protected ITextComponent getInterruptNameIndexed(final int interrupt) {
            if (interrupt == 0) {
                return new TextComponentTranslation(Constants.I18N.INTERRUPT_SINK_NON_MASKABLE_INTERRUPT);
            } else {
                return new TextComponentTranslation(Constants.I18N.INTERRUPT_SINK_INTERRUPT_REQUEST);
            }
        }

        @Override
        protected void interruptIndexed(final int interrupt, final int data) {
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
