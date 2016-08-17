package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.server.processor.BusControllerAccess;
import li.cil.circuity.server.processor.z80.Z80;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;

import javax.annotation.Nullable;

@Serializable
public class BusDeviceZ80Processor extends AbstractComponentBusDevice implements ActivationListener {
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

    @Override
    public boolean handleActivated(final EntityPlayer player, final EnumHand hand, @Nullable final ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final BusController controller = device.getController();
        if (controller != null) {
            device.z80.reset(0);

            // TODO Configurable address.
            final int eepromAddress = 0xC100;
            for (int offset = 0; offset < 4 * 1024; offset++) {
                final int value = controller.mapAndRead(eepromAddress + offset);
                controller.mapAndWrite(offset, value);
            }

        }
        return true;
    }

    @Serializable
    private class BusDeviceZ80Impl extends AbstractBusDevice implements InterruptSink, AsyncTickable {
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
        // AsyncTickable

        @Override
        public void updateAsync() {
            z80.run(CYCLES_PER_TICK);
        }
    }
}
