package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressableInterruptSource;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.InterruptList;
import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.api.synchronization.SynchronizationListener;
import li.cil.lib.api.synchronization.SynchronizedValue;
import li.cil.lib.synchronization.value.SynchronizedByteArray;
import li.cil.lib.synchronization.value.SynchronizedInt;
import li.cil.lib.synchronization.value.SynchronizedUUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

public final class BusDeviceSerialConsole extends AbstractComponentBusDevice implements SynchronizationListener {
    public static final int CONS_WIDTH = 40;
    public static final int CONS_HEIGHT = 25;
    public static final int CONS_TAB_STOP = 4;

    @Serialize
    private final SerialConsoleImpl device = new SerialConsoleImpl();
    @Serialize
    private final SynchronizedByteArray buffer = new SynchronizedByteArray(CONS_WIDTH * CONS_HEIGHT);
    @Serialize
    private final SynchronizedUUID persistentId = new SynchronizedUUID(UUID.randomUUID());
    @Serialize
    private final SynchronizedInt scrX = new SynchronizedInt(0); // Range: [0,CONS_WIDTH] (yes, inclusive)
    @Serialize
    private final SynchronizedInt scrY = new SynchronizedInt(0); // Range: [0,CONS_HEIGHT) (not a typo!)
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
    public BusDevice getDevice() {
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

    public final class SerialConsoleImpl extends AbstractAddressableInterruptSource implements AddressHint, ScreenRenderer {
        // --------------------------------------------------------------------- //
        // AbstractAddressableInterruptSource

        @Nullable
        @Override
        public DeviceInfo getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            return memory.take(Constants.SERIAL_CONSOLE_ADDRESS, 1);
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

        @Override
        public int read(final int address) {
            switch (address) {
                case 0: {
                    return 0;
                }
            }
            return 0;
        }

        @Override
        public void write(final int address, final int value) {
            switch (address) {
                case 0: {
                    final char ch = (char) value;

                    switch (ch) {
                        case '\b': { // Backspace
                            int scrX = BusDeviceSerialConsole.this.scrX.get();
                            do {
                                scrX--;
                            } while (scrX >= 0 && scrX % CONS_WIDTH != 0
                                    && get(scrX, line()) == '\t');

                            if (scrX < 0) {
                                scrX = 0;
                            }
                            BusDeviceSerialConsole.this.scrX.set(scrX);
                            break;
                        }

                        case '\t': { // Tab
                            int scrX = BusDeviceSerialConsole.this.scrX.get();
                            do {
                                set(scrX, line(), '\t');
                                scrX++;
                                if (scrX >= CONS_WIDTH) {
                                    advanceLine();
                                    scrX = 0;
                                }
                            } while (scrX % CONS_TAB_STOP != 0);
                            BusDeviceSerialConsole.this.scrX.set(scrX);
                            break;
                        }

                        case '\n': // Line feed
                            advanceLine();
                            BusDeviceSerialConsole.this.scrX.set(0);
                            break;

                        case '\r': // Carriage return
                            BusDeviceSerialConsole.this.scrX.set(0);
                            break;

                        case '\u001B': // Escape
                            // TODO: VT-100/VT-220 codes
                            break;

                        default: {
                            int scrX = BusDeviceSerialConsole.this.scrX.get();
                            if (scrX >= CONS_WIDTH) {
                                advanceLine();
                                scrX = 0;
                            }
                            set(scrX, line(), ch);
                            scrX++;
                            BusDeviceSerialConsole.this.scrX.set(scrX);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.SERIAL_CONSOLE_ADDRESS;
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
            final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
            final byte[] data = BusDeviceSerialConsole.this.buffer.array();
            final int yOffset = line();
            for (int y = 0; y < CONS_HEIGHT; y++) {
                // TODO Write a font renderer that operates directly on the byte array.
                final StringBuilder sb = new StringBuilder();
                for (int x = 0; x < CONS_WIDTH; x++) {
                    final char ch = (char) data[((y + yOffset) % CONS_HEIGHT) * CONS_WIDTH + x];
                    if (ch >= (char) 0x20) {
                        sb.append(ch);
                    } else if (ch == '\t') {
                        sb.append(' ');
                    }
                }
                fontRenderer.drawString(sb.toString(), 0, y * fontRenderer.FONT_HEIGHT, 0xFFFFFF);
            }
        }

        // --------------------------------------------------------------------- //

        private int line() {
            return (BusDeviceSerialConsole.this.scrOffY.get() + BusDeviceSerialConsole.this.scrY.get()) % CONS_HEIGHT;
        }

        private char get(final int x, final int y) {
            return (char) BusDeviceSerialConsole.this.buffer.get(y * CONS_WIDTH + x);
        }

        private void set(final int x, final int y, final char ch) {
            BusDeviceSerialConsole.this.buffer.set(y * CONS_WIDTH + x, (byte) ch);
        }

        private void advanceLine() {
            int scrY = BusDeviceSerialConsole.this.scrY.get();
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
            BusDeviceSerialConsole.this.scrY.set(scrY);
            BusDeviceSerialConsole.this.scrOffY.set(scrOffY);
        }
    }
}
