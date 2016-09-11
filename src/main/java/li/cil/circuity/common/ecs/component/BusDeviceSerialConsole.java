package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.circuity.api.bus.device.util.SerialPortManager;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.bus.util.SerialPortManagerProxy;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.api.synchronization.SynchronizationListener;
import li.cil.lib.api.synchronization.SynchronizedValue;
import li.cil.lib.client.renderer.font.FontRenderer;
import li.cil.lib.client.renderer.font.FontRendererCodePage437;
import li.cil.lib.synchronization.value.SynchronizedByteArray;
import li.cil.lib.synchronization.value.SynchronizedInt;
import li.cil.lib.synchronization.value.SynchronizedUUID;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

@Serializable
public final class BusDeviceSerialConsole extends AbstractComponentBusDevice implements SynchronizationListener {
    public static final int CONS_WIDTH = 40;
    public static final int CONS_HEIGHT = 25;
    public static final int CONS_TAB_STOP = 4;

    @Serialize
    private final SerialConsoleImpl device = new SerialConsoleImpl();
    @Serialize
    private final SynchronizedUUID persistentId = new SynchronizedUUID(UUID.randomUUID());
    @Serialize
    private final SynchronizedByteArray buffer = new SynchronizedByteArray(CONS_WIDTH * CONS_HEIGHT);
    @Serialize
    private final SynchronizedInt scrOffY = new SynchronizedInt(0); // Range: [0,CONS_HEIGHT)

    // --------------------------------------------------------------------- //

    public BusDeviceSerialConsole(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // AbstractComponent

    @Override
    public void onCreate() {
        super.onCreate();

        final World world = getWorld();
        if (world.isRemote) {
            SillyBeeAPI.globalObjects.put(world, device.getPersistentId(), device);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final World world = getWorld();
        if (world.isRemote) {
            SillyBeeAPI.globalObjects.remove(world, device.getPersistentId());
        }
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusElement getBusElement() {
        return device;
    }

    // --------------------------------------------------------------------- //
    // SynchronizationListener

    @Override
    public void onBeforeSynchronize(final Iterable<SynchronizedValue> values) {
        if (isValid()) {
            SillyBeeAPI.globalObjects.remove(getWorld(), device.getPersistentId());
        }
    }

    @Override
    public void onAfterSynchronize(final Iterable<SynchronizedValue> values) {
        if (isValid()) {
            SillyBeeAPI.globalObjects.put(getWorld(), device.getPersistentId(), device);
        }
    }

    // --------------------------------------------------------------------- //

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.SERIAL_INTERFACE, Constants.DeviceInfo.SERIAL_CONSOLE_NAME);

    public final class SerialConsoleImpl extends AbstractBusDevice implements Addressable, AddressHint, BusStateListener, ScreenRenderer, SerialPortManagerProxy {
        private final SerialPortManager serialPortManager = new SerialPortManager();

        @Serialize
        private int scrX = 0; // Range: [0,CONS_WIDTH] (yes, inclusive)
        @Serialize
        private int scrY = 0; // Range: [0,CONS_HEIGHT) (not a typo!)

        // --------------------------------------------------------------------- //

        public SerialConsoleImpl() {
            serialPortManager.setPreferredAddressOffset(Constants.SERIAL_CONSOLE_ADDRESS);
            serialPortManager.addSerialPort(null, this::writeCharacter, null);
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
            return Constants.SERIAL_CONSOLE_ADDRESS;
        }

        // --------------------------------------------------------------------- //
        // BusStateListener

        @Override
        public void handleBusOnline() {
        }

        @Override
        public void handleBusOffline() {
            BusDeviceSerialConsole.this.buffer.fill((byte) 0);
            BusDeviceSerialConsole.this.scrOffY.set(0);
            scrX = 0;
            scrY = 0;
        }

        // --------------------------------------------------------------------- //
        // ScreenRenderer

        @SuppressWarnings("ConstantConditions") // We make sure persistentId is never null.
        @Override
        public UUID getPersistentId() {
            return BusDeviceSerialConsole.this.persistentId.get();
        }

        @Override
        public void render(final int width, final int height) {
            final FontRenderer fontRenderer = FontRendererCodePage437.INSTANCE;
            final byte[] data = BusDeviceSerialConsole.this.buffer.array();
            final int yOffset = BusDeviceSerialConsole.this.scrOffY.get();

            GlStateManager.pushMatrix();

            final int textWidth = CONS_WIDTH * fontRenderer.getCharWidth();
            final int textHeight = CONS_HEIGHT * fontRenderer.getCharHeight();

            final float scaleX = width / (float) textWidth;
            final float scaleY = height / (float) textHeight;

            final float scale = Math.min(scaleX, scaleY);
            GlStateManager.scale(scale, scale, scale);

            final float scaledWidth = textWidth * scale;
            final float scaledHeight = textHeight * scale;
            GlStateManager.translate((width - scaledWidth) * 0.5f, (height - scaledHeight) * 0.5f, 0);

            for (int y = 0; y < CONS_HEIGHT; y++) {
                fontRenderer.drawString(data, ((y + yOffset) % CONS_HEIGHT) * CONS_WIDTH, CONS_WIDTH);

                GlStateManager.translate(0, fontRenderer.getCharHeight(), 0);
            }

            GlStateManager.popMatrix();
        }

        // --------------------------------------------------------------------- //
        // SerialPortManagerProxy

        @Override
        public SerialPortManager getSerialPortManager() {
            return serialPortManager;
        }

        // --------------------------------------------------------------------- //

        private void writeCharacter(final long address, final int value) {
            final char ch = (char) value;

            switch (ch) {
                case '\b': { // Backspace
                    do {
                        scrX--;
                    } while (scrX >= 0 && scrX % CONS_WIDTH != 0
                            && get(scrX, line()) == '\t');

                    if (scrX < 0) {
                        scrX = 0;
                    }
                    break;
                }

                case '\t': { // Tab
                    do {
                        set(scrX, line(), '\t');
                        scrX++;
                        if (scrX >= CONS_WIDTH) {
                            advanceLine();
                            scrX = 0;
                        }
                    } while (scrX % CONS_TAB_STOP != 0);
                    break;
                }

                case '\n': // Line feed
                    advanceLine();
                    scrX = 0;
                    break;

                case '\r': // Carriage return
                    scrX = 0;
                    break;

                case '\u001B': // Escape
                    // TODO: VT-100/VT-220 codes
                    break;

                default: {
                    if (scrX >= CONS_WIDTH) {
                        advanceLine();
                        scrX = 0;
                    }
                    set(scrX, line(), ch);
                    scrX++;
                    break;
                }
            }

            BusDeviceSerialConsole.this.markChanged();
        }

        // --------------------------------------------------------------------- //

        private int line() {
            return (BusDeviceSerialConsole.this.scrOffY.get() + scrY) % CONS_HEIGHT;
        }

        private char get(final int x, final int y) {
            return (char) BusDeviceSerialConsole.this.buffer.get(y * CONS_WIDTH + x);
        }

        private void set(final int x, final int y, final char ch) {
            BusDeviceSerialConsole.this.buffer.set(y * CONS_WIDTH + x, (byte) ch);
        }

        private void advanceLine() {
            int scrOffY = BusDeviceSerialConsole.this.scrOffY.get();
            scrY++;
            while (scrY >= CONS_HEIGHT) {
                for (int index = scrOffY * CONS_WIDTH, end = index + CONS_WIDTH; index < end; index++) {
                    BusDeviceSerialConsole.this.buffer.set(index, (byte) 0);
                }

                scrOffY++;
                scrOffY %= CONS_HEIGHT;
                scrY--;
                if (scrY < 0) {
                    scrY = 0;
                }
            }
            BusDeviceSerialConsole.this.scrOffY.set(scrOffY);
        }
    }
}
