package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractInterruptSink;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusStateAware;
import li.cil.circuity.api.bus.device.InterruptList;
import li.cil.circuity.common.Constants;
import li.cil.circuity.server.processor.BusControllerAccess;
import li.cil.circuity.server.processor.mips.Mips3;
import li.cil.circuity.util.IntelHexLoader;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * MIPS3 implementation, heavily based on the NEC VR4300.
 */
public class BusDeviceMips3Processor extends AbstractComponentBusDevice {

    private static final int CYCLES_PER_TICK = 2_000_000 / 20;

    @Serialize
    private final BusDeviceMips3Impl device = new BusDeviceMips3Impl();

    // --------------------------------------------------------------------- //

    public BusDeviceMips3Processor(final EntityComponentManager manager, final long entity, final long id) {
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
    private class BusDeviceMips3Impl extends AbstractInterruptSink implements BusStateAware, AsyncTickable {
        @Serialize
        private final Mips3 mips;

        // --------------------------------------------------------------------- //

        public BusDeviceMips3Impl() {
            this.mips = new Mips3(new BusControllerAccess(this::getBusController, 0));
        }

        // --------------------------------------------------------------------- //
        // AbstractInterruptSink

        @Override
        protected int[] validateAcceptedInterrupts(final InterruptList interrupts) {
            return interrupts.take(6);
        }

        @Override
        protected ITextComponent getInterruptNameIndexed(final int interrupt) {
            // TODO: name for each IRQ
            return new TextComponentTranslation(Constants.I18N.INTERRUPT_SINK_INTERRUPT_REQUEST);
        }

        @Override
        protected void interruptIndexed(final int interrupt, final int data) {
            mips.irq(interrupt);
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

            if(true) {
                try {
                    // TODO: Replace the path with something more sensible
                    // Until then, you can replace it on your own end!
                    System.out.printf("LOADING REQUIRED BOOT FILE\n");
                    IntelHexLoader.load(Files.readAllLines(Paths.get("/home/ben/Downloads/mcmods/Circuity/mips/derp.ihx")), controller::mapAndWrite);
                    System.out.printf("YES I THINK IT'S LOADED\n");
                } catch (IOException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void handleBusOffline() {
            device.mips.reset();
        }

        // --------------------------------------------------------------------- //
        // AsyncTickable

        @Override
        public void updateAsync() {
            mips.run(CYCLES_PER_TICK);
        }
    }
}
