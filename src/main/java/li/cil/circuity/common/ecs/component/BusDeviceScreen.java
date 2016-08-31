package li.cil.circuity.common.ecs.component;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.ModCircuity;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressableInterruptSource;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.BusChangeListener;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.InterruptList;
import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.circuity.client.gui.GuiType;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedUUID;
import li.cil.lib.util.PlayerUtil;
import li.cil.lib.util.RingBuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.UUID;

public class BusDeviceScreen extends AbstractComponentBusDevice implements ActivationListener {
    private final Object lock = new Object();

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

    @Override
    public void handleComponentData(final ByteBuf data) {
        synchronized (lock) {
            if (device.buffer.isWritable()) {
                device.buffer.write(data.readByte());
            }
            device.triggerInterrupt(0, 0);
        }
    }

    // --------------------------------------------------------------------- //
    // ActivationListener

    @Override
    public boolean handleActivated(final EntityPlayer player, final EnumHand hand, @Nullable final ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (player.isSneaking()) {
            return false;
        }

        PlayerUtil.openGui(player, GuiType.SCREEN, this);

        return true;
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.SCREEN, Constants.DeviceInfo.SCREEN_NAME);

    @Serializable
    public final class ScreenImpl extends AbstractAddressableInterruptSource implements AddressHint, BusStateListener, BusChangeListener {
        @Serialize
        public RingBuffer buffer = new RingBuffer(16);

        // --------------------------------------------------------------------- //
        // AbstractAddressableInterruptSource

        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            return memory.take(Constants.SCREEN_ADDRESS, 4);
        }

        @Override
        protected int[] validateEmittedInterrupts(final InterruptList interrupts) {
            return interrupts.take(1);
        }

        @Nullable
        @Override
        public ITextComponent getInterruptName(final int interruptId) {
            return new TextComponentTranslation(Constants.I18N.INTERRUPT_SOURCE_KEYBOARD_INPUT);
        }

        // --------------------------------------------------------------------- //
        // Addressable

        @Nullable
        @Override
        public DeviceInfo getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Override
        public int read(final int address) {
            switch (address) {
                case 0: { // Number of available renderers.
                    return 0;
                }
                case 1: { // Selected renderer.
                    return 0;
                }
                case 2: { // Read UUID of selected renderer.
                    return 0;
                }
                case 3: { // Read keyboard input.
                    synchronized (BusDeviceScreen.this.lock) {
                        return buffer.isReadable() ? buffer.read() : 0;
                    }
                }
            }
            return 0;
        }

        @Override
        public void write(final int address, final int value) {
            switch (address) {
                case 1: { // Select renderer.
                    break;
                }
                case 2: { // Reset UUID read index.
                    break;
                }
            }
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.SCREEN_ADDRESS;
        }

        // --------------------------------------------------------------------- //
        // BusStateListener

        @Override
        public void handleBusOnline() {
        }

        @Override
        public void handleBusOffline() {
            buffer.clear();
        }

        // --------------------------------------------------------------------- //
        // BusChangeListener

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
