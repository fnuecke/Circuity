package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.ComponentHosted;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.common.Constants;
import li.cil.circuity.server.processor.BusControllerAccess;
import li.cil.circuity.server.processor.z80.Z80;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedLong;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;

@Serializable
public final class BusDeviceZ80Processor extends AbstractComponentBusDevice {
    private static final int CYCLES_PER_TICK = 2_000_000 / 20;

    @Serialize
    private final BusDeviceZ80Impl device = new BusDeviceZ80Impl();

    // Component ID of controller for client.
    public final SynchronizedLong controllerId = new SynchronizedLong();

    // --------------------------------------------------------------------- //

    public BusDeviceZ80Processor(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // BusDeviceHost

    @Override
    public BusDevice getBusDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //
    // ActivationListener

    @Serializable
    public final class BusDeviceZ80Impl extends AbstractBusDevice implements ComponentHosted, InterruptSink, BusStateListener, AsyncTickable {
        @Serialize
        public final Z80 z80;

        // --------------------------------------------------------------------- //

        public BusDeviceZ80Impl() {
            this.z80 = new Z80(new BusControllerAccess(this::getBusController, 0, 0xFFFF), new BusControllerAccess(this::getBusController, 0x10000, 0x00FF));
        }

        // --------------------------------------------------------------------- //
        // BusElement

        @Override
        public void setBusController(@Nullable final BusController controller) {
            super.setBusController(controller);
            if (controller instanceof ComponentHosted) {
                final ComponentHosted hosted = (ComponentHosted) controller;
                final long componentId = hosted.getHostComponent().getId();
                controllerId.set(componentId);
            } else {
                controllerId.set(0);
            }
        }

        // --------------------------------------------------------------------- //
        // ComponentHosted

        @Override
        public Component getHostComponent() {
            return BusDeviceZ80Processor.this;
        }

        // --------------------------------------------------------------------- //
        // InterruptSink

        @Override
        public int getAcceptedInterrupts() {
            return 2;
        }

        @Nullable
        @Override
        public ITextComponent getInterruptName(final int interrupt) {
            switch (interrupt) {
                case 0:
                    return new TextComponentTranslation(Constants.I18N.INTERRUPT_SINK_NON_MASKABLE_INTERRUPT);
                case 1:
                    return new TextComponentTranslation(Constants.I18N.INTERRUPT_SINK_INTERRUPT_REQUEST);
                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public void interrupt(final int interrupt, final int data) {
            switch (interrupt) {
                case 0:
                    z80.nmi();
                    break;
                case 1:
                    z80.irq((byte) data);
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        // --------------------------------------------------------------------- //
        // BusStateAware

        @Override
        public void handleBusOnline() {
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
