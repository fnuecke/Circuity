package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressableInterruptSource;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.InterruptList;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Arrays;

public final class BusDeviceSerialConsole extends AbstractComponentBusDevice {

    public static final int CONS_WIDTH = 40;
    public static final int CONS_HEIGHT = 25;
    public static final int CONS_TAB_STOP = 4;

    @Serialize
    private final SerialConsoleImpl device = new SerialConsoleImpl();

    public BusDeviceSerialConsole(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //

    @Override
    public BusDevice getDevice() {
        return device;
    }

    // --------------------------------------------------------------------- //

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.SERIAL_INTERFACE, Constants.DeviceInfo.SERIAL_CONSOLE_NAME);

    public final class SerialConsoleImpl extends AbstractAddressableInterruptSource implements AddressHint {
        // --------------------------------------------------------------------- //
        // AbstractAddressableInterruptSource

        @Serialize
        private char[][] scrBuf = new char[CONS_HEIGHT][CONS_WIDTH];
        @Serialize
        private int scrX = 0; // Range: [0,CONS_WIDTH] (yes, inclusive)
        @Serialize
        private int scrY = 0; // Range: [0,CONS_HEIGHT) (not a typo!)
        @Serialize
        private int scrOffY = 0; // Range: [0,CONS_HEIGHT)

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

        private void scrollDown() {
            Arrays.fill(this.scrBuf[this.scrOffY], (char)0);
            this.scrOffY++;
            this.scrOffY %= CONS_HEIGHT;
            this.scrY--;
            if(this.scrY < 0) {
                this.scrY = 0;
            }
        }

        private void advanceLine() {
            // Print line (temporary measure)
            String outbuf = "";
            final char[] inbuf = this.scrBuf[(this.scrOffY+this.scrY)%CONS_HEIGHT];

            for(int i = 0; i < this.scrX; i++) {
                char ch = inbuf[i];
                if(ch >= (char)0x20) {
                    outbuf += ch;
                } else if(ch == '\t') {
                    outbuf += ' ';
                }
            }

            //System.out.print(outbuf + "\n");
            BusDeviceSerialConsole.this.getWorld().getMinecraftServer().getPlayerList().sendChatMsg(
                    new TextComponentString("SC: " + outbuf)
            );

            //
            this.scrY++;
            while(this.scrY >= CONS_HEIGHT) {
                scrollDown();
            }
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

                    switch(ch) {
                        case '\b': // Backspace
                            do {
                                this.scrX--;
                            } while(this.scrX >= 0 && this.scrX % CONS_WIDTH == 0
                                && this.scrBuf[(this.scrOffY+this.scrY)%CONS_HEIGHT][this.scrX] == '\t');

                            if(this.scrX < 0) {
                                this.scrX = 0;
                            }
                            break;

                        case '\t': // Tab
                            do {
                                this.scrBuf[(this.scrOffY+this.scrY)%CONS_HEIGHT][this.scrX] = '\t';
                                this.scrX++;
                                if(this.scrX >= CONS_WIDTH) {
                                    advanceLine();
                                    this.scrX = 0;
                                }
                            } while(this.scrX % CONS_TAB_STOP == 0);
                            break;

                        case '\n': // Line feed
                            advanceLine();
                            this.scrX = 0;
                            break;

                        case '\r': // Carriage return
                            this.scrX = 0;
                            break;

                        case '\u001B': // Escape
                            // TODO: VT-100/VT-220 codes
                            break;

                        default:
                            if(this.scrX >= CONS_WIDTH) {
                                advanceLine();
                                this.scrX = 0;
                            }
                            this.scrBuf[(this.scrOffY+this.scrY)%CONS_HEIGHT][this.scrX] = ch;
                            this.scrX++;
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
    }
}
