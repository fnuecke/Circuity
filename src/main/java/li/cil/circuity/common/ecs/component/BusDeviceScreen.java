package li.cil.circuity.common.ecs.component;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.ModCircuity;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.BusChangeListener;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.InterruptSource;
import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.circuity.api.bus.device.util.SerialPortManager;
import li.cil.circuity.client.gui.GuiType;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.bus.util.SerialPortManagerProxy;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedUUID;
import li.cil.lib.util.PlayerUtil;
import li.cil.lib.util.RingBuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Serializable
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
            final BusController controller = device.getBusController();
            if (controller != null) {
                controller.getSubsystem(InterruptMapper.class).interrupt(device, 0, 0);
            }
        }
    }

    // --------------------------------------------------------------------- //
    // ActivationListener

    @Override
    public boolean handleActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (player.isSneaking()) {
            return false;
        }

        PlayerUtil.openGui(player, GuiType.SCREEN, this);

        return true;
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusElement getBusElement() {
        return device;
    }

    // --------------------------------------------------------------------- //

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.SCREEN, Constants.DeviceInfo.SCREEN_NAME);

    @Serializable
    public final class ScreenImpl extends AbstractBusDevice implements Addressable, AddressHint, InterruptSource, BusStateListener, BusChangeListener, SerialPortManagerProxy {
        private final SerialPortManager serialPortManager = new SerialPortManager();
        private final List<ScreenRenderer> renderers = new ArrayList<>();

        @Serialize
        private final RingBuffer buffer = new RingBuffer(16);

        @Serialize
        private int selectedRenderer;

        @Serialize
        private int uuidIndex;

        // --------------------------------------------------------------------- //

        public ScreenImpl() {
            serialPortManager.setPreferredAddressOffset(Constants.SCREEN_ADDRESS);
            serialPortManager.addSerialPort(this::readRendererCount, null, null);
            serialPortManager.addSerialPort(this::readSelectedRenderer, this::writeSelectedRenderer, null);
            serialPortManager.addSerialPort(this::readSelectedUUID, this::writeResetUUIDIndex, null);
            serialPortManager.addSerialPort(this::readKey, null, null);
        }

        // --------------------------------------------------------------------- //
        // BusDevice

        @Nullable
        @Override
        public DeviceInfo getDeviceInfo() {
            return DEVICE_INFO;
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.SCREEN_ADDRESS;
        }

        // --------------------------------------------------------------------- //
        // InterruptSource

        @Override
        public int getEmittedInterrupts() {
            return 1;
        }

        @Nullable
        @Override
        public ITextComponent getInterruptName(final int interrupt) {
            return new TextComponentTranslation(Constants.I18N.INTERRUPT_SOURCE_KEYBOARD_INPUT);
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
            renderers.clear();
            for (final BusElement element : controller.getElements()) {
                if (element instanceof ScreenRenderer) {
                    final ScreenRenderer renderer = (ScreenRenderer) element;
                    final int index = Collections.binarySearch(renderers, renderer);
                    assert index < 0 : "Two renderers with the same persistent ID.";
                    renderers.add(~index, renderer);
                }
            }

            selectRenderer(selectedRenderer);
        }

        // --------------------------------------------------------------------- //
        // SerialPortManagerProxy

        @Override
        public SerialPortManager getSerialPortManager() {
            return serialPortManager;
        }

        // --------------------------------------------------------------------- //

        private int readRendererCount(final long address) {
            return renderers.size();
        }

        private int readSelectedRenderer(final long address) {
            return selectedRenderer;
        }

        private void writeSelectedRenderer(final long address, final int value) {
            selectRenderer(value);
        }

        private int readSelectedUUID(final long address) {
            if (selectedRenderer >= 0 && selectedRenderer < renderers.size()) {
                final String uuid = renderers.get(selectedRenderer).getPersistentId().toString();
                return uuidIndex < uuid.length() ? uuid.charAt(uuidIndex++) : 0;
            }
            return 0;
        }

        private void writeResetUUIDIndex(final long address, final int value) {
            uuidIndex = 0;
        }

        private int readKey(final long address) {
            synchronized (BusDeviceScreen.this.lock) {
                return buffer.isReadable() ? buffer.read() : 0;
            }
        }

        private void selectRenderer(final int value) {
            selectedRenderer = value;

            if (selectedRenderer < 0) {
                selectedRenderer = 0;
            }
            if (selectedRenderer >= renderers.size()) {
                selectedRenderer = renderers.size() - 1;
            }

            if (renderers.size() > 0) {
                BusDeviceScreen.this.rendererId.set(renderers.get(selectedRenderer).getPersistentId());
            } else {
                BusDeviceScreen.this.rendererId.set(null);
            }
        }
    }
}
