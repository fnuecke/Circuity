package li.cil.circuity.common.ecs.component;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.util.SerialPortManager;
import li.cil.circuity.api.item.FloppyDisk;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.capabilities.fdd.CapabilityFloppyDisk;
import li.cil.circuity.server.bus.util.SerialPortManagerProxy;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.event.InventoryChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.scheduler.ScheduledCallback;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.ecs.component.InventoryMutable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public final class BusDeviceFloppyDiskDrive extends AbstractComponentBusDevice implements InventoryChangeListener {
    @Serialize
    private final FloppyDiskDriveImpl device = new FloppyDiskDriveImpl();

    private AbstractFloppyController.DiskImage image;
    private ScheduledCallback scheduledDataUpdate;

    // --------------------------------------------------------------------- //

    public BusDeviceFloppyDiskDrive(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        scheduleUpdateData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (scheduledDataUpdate != null) {
            SillyBeeAPI.scheduler.cancel(getWorld(), scheduledDataUpdate);
            scheduledDataUpdate = null;
        }
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusElement getBusElement() {
        return device;
    }

    // --------------------------------------------------------------------- //
    //InventoryChangeListener

    @Override
    public void handleInventoryChange(final IItemHandler inventory, final int slot) {
        scheduleUpdateData();
    }

    // --------------------------------------------------------------------- //

    private void scheduleUpdateData() {
        if (scheduledDataUpdate != null) {
            return;
        }

        final World world = getWorld();
        if (world.isRemote) {
            return;
        }

        scheduledDataUpdate = SillyBeeAPI.scheduler.schedule(world, this::updateData);
    }

    private void updateData() {
        image = null;
        scheduledDataUpdate = null;

        final IItemHandler inventory = getComponent(InventoryMutable.class).orElseThrow(IllegalStateException::new);
        final ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            final FloppyDisk disk = stack.getCapability(CapabilityFloppyDisk.FLOPPY_DISK_CAPABILITY, null);
            if (disk != null) {
                image = new AbstractFloppyController.DiskImageBuffer(disk);
            }
        }

        device.reset();
    }

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.FLOPPY_DISK_DRIVE, Constants.DeviceInfo.FLOPPY_DISK_DRIVE_NAME);

    // http://z00m.speccy.cz/docs/wd1793.htm
    public static abstract class AbstractFloppyController extends AbstractBusDevice implements Addressable, AddressHint, InterruptSourceProxy, SerialPortManagerProxy {
        // Common status bits.
        private static final int S_BUSY = 0x01; // When set command is in progress. When reset no command is in progress.
        //        private static final int S_CRC_ERROR = 0x08; // CRC error in ID field or data.
        private static final int S_NOT_READY = 0x80; // This bit when set indicates the drive is not ready. When reset it indicates that the drive is ready.

        // Type I command status bits.
        private static final int S_INDEX = 0x02; // When set, indicates index mark detected from drive.
        private static final int S_TRACK0 = 0x04; // When set, indicates Read/Write head is positioned to Track 0.
        private static final int S_SEEK_ERROR = 0x10; // When set, the desired track was not verified.
        private static final int S_HEAD_LOADED = 0x20; // When set, it indicates the head is loaded an engaged.
//        private static final int S_PROTECTED = 0x40; // When set, indicates Write Protect is activated.

        // Type II and III command status bits.
        private static final int S_DATA_REQUEST = 0x02; // This bit is a copy of the DRQ output. When set, it indicates the DR is full on a Read operation or the DR is empty on a Write operation.
        private static final int S_LOST_DATA = 0x04; // When set, it indicates the computer did not respond to DRQ in one byte time.
        private static final int S_RECORD_NOT_FOUND = 0x10; // When set, it indicates the desired track, sector, or side were not found.
//        private static final int S_RECORD_TYPE = 0x20;
//        private static final int S_WRITE_FAULT = 0x20;

        private static final int SC_DRIVE = 0x03;
        private static final int SC_RESET = 0x04;
        private static final int SC_HALT = 0x08;
        private static final int SC_SIDE = 0x10;

        // Type I args
//        private static final int C_ARG_RATE0 = 0x01;
//        private static final int C_ARG_RATE1 = 0x02;
//        private static final int C_ARG_VERIFY = 0x04;
        private static final int C_ARG_LOAD_HEAD = 0x08;

        // Type II args
        private static final int C_ARG_SET_TRACK = 0x10;
        private static final int C_ARG_SIDE_COMPARE = 0x02;
        //        private static final int C_ARG_DELAY = 0x04;
        private static final int C_ARG_SIDE_FLAG = 0x08;
        private static final int C_ARG_MULTIPLE = 0x10;
        private static final int C_ARG_IMMEDIATE_IRQ = 0x08;

        // Type I commands
        private static final int C_RESTORE = 0x00;
        private static final int C_SEEK = 0x10;
        private static final int C_STEP = 0x20;
        private static final int C_STEP_UPDATE = C_STEP | C_ARG_SET_TRACK;
        private static final int C_STEP_IN = 0x40;
        private static final int C_STEP_IN_UPDATE = C_STEP_IN | C_ARG_SET_TRACK;
        private static final int C_STEP_OUT = 0x60;
        private static final int C_STEP_OUT_UPDATE = C_STEP_OUT | C_ARG_SET_TRACK;

        // Type II commands
        private static final int C_READ_SECTOR = 0x80;
        private static final int C_READ_SECTOR_MULTIPLE = C_READ_SECTOR | C_ARG_MULTIPLE;
        private static final int C_WRITE_SECTOR = 0xA0;
        private static final int C_WRITE_SECTOR_MULTIPLE = C_WRITE_SECTOR | C_ARG_MULTIPLE;

        // Type III commands
        private static final int C_READ_ADDRESS = 0xC0;
        private static final int C_READ_TRACK = 0xE0;
        private static final int C_WRITE_TRACK = 0xF0;

        // Type IV commands
        private static final int C_FORCE_INTERRUPT = 0xD0;

        private static final int WD1793_IRQ = 0x80;
        private static final int WD1793_DRQ = 0x40;

        private final SerialPortManager serialPortManager = new SerialPortManager();
        private int status, track, sector, data, system;
        private int drive, side;
        private int irq;
        private int lastStep = C_STEP_IN;
        private int writesLeft, readsLeft;
        private int wait;

        public AbstractFloppyController() {
            serialPortManager.setPreferredAddressOffset(Constants.DISK_DRIVE_ADDRESS);
            serialPortManager.addSerialPort(this::readStatus, this::writeCommand, null);
            serialPortManager.addSerialPort(this::readTrack, this::writeTrack, null);
            serialPortManager.addSerialPort(this::readSector, this::writeSector, null);
            serialPortManager.addSerialPort(this::readData, this::writeData, null);
            serialPortManager.addSerialPort(this::readInterrupt, this::writeSystem, null);
        }

        public void reset() {
            status = track = sector = data = 0;
            system = SC_RESET | SC_HALT;
            drive = side = 0;
            irq = 0;
            lastStep = C_STEP_IN;
            writesLeft = readsLeft = 0;
            wait = 0;
        }

        // --------------------------------------------------------------------- //

        @Nullable
        protected abstract DiskImage getDisk(final int drive);

        // --------------------------------------------------------------------- //
        // InterruptSource

        @Override
        public int getEmittedInterrupts() {
            return 2;
        }

        @Nullable
        @Override
        public ITextComponent getEmittedInterruptName(final int interrupt) {
            switch (interrupt) {
                case 0:
                    return new TextComponentTranslation("IRQ");
                case 1:
                    return new TextComponentTranslation("DRQ");
            }

            return null;
        }

        // --------------------------------------------------------------------- //
        // SerialPortManagerProxy

        @Override
        public SerialPortManager getSerialPortManager() {
            return serialPortManager;
        }

        // --------------------------------------------------------------------- //

        private int readStatus(final long address) {
            clearRequest();

            int result = status;
            if (getDisk() == null) {
                result |= S_NOT_READY;
            }
            status &= S_BUSY | S_NOT_READY;
            return result;
        }

        private void writeCommand(final long address, final int value) {
            clearRequest();

            final int command = value & 0xF0;

            if (isBusy() && command != C_FORCE_INTERRUPT) {
                return;
            }

            status = S_BUSY;

            final DiskImage image = getDisk();

            switch (command) {
                // Type I commands

                case C_RESTORE: {
                    if (image == null) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    // TODO Make this an asynchronous operation.
                    image.setTrack(0);

                    status = S_INDEX | S_TRACK0;
                    if ((value & C_ARG_LOAD_HEAD) == C_ARG_LOAD_HEAD)
                        status |= S_HEAD_LOADED;
                    track = 0;

                    setInterruptRequest();
                    break;
                }

                case C_SEEK: {
                    if (image == null) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    if (data < 0 || data >= image.getTrackCount()) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    // TODO Make this an asynchronous operation.
                    if (!image.setTrack(data)) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    status = S_INDEX;
                    if (image.getTrack() == 0)
                        status |= S_TRACK0;
                    if ((value & C_ARG_LOAD_HEAD) == C_ARG_LOAD_HEAD)
                        status |= S_HEAD_LOADED;
                    track = image.getTrack();

                    setInterruptRequest();
                    break;
                }
                case C_STEP:
                case C_STEP_UPDATE: {
                    writeCommand(address, lastStep | (value & ~C_STEP));
                    break;
                }
                case C_STEP_IN:
                case C_STEP_IN_UPDATE: {
                    lastStep = C_STEP_IN;

                    if (image == null) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    if (image.getTrack() >= image.getTrackCount()) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    // TODO Make this an asynchronous operation.
                    if (!image.setTrack(image.getTrack() + 1)) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    status = S_INDEX;
                    if (image.getTrack() == 0)
                        status |= S_TRACK0;
                    if ((value & C_ARG_LOAD_HEAD) == C_ARG_LOAD_HEAD)
                        status |= S_HEAD_LOADED;

                    if ((command & C_STEP_IN_UPDATE) != 0)
                        track = image.getTrack();

                    setInterruptRequest();
                    break;
                }
                case C_STEP_OUT:
                case C_STEP_OUT_UPDATE: {
                    lastStep = C_STEP_OUT;

                    if (image == null) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    if (image.getTrack() < 1) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    // TODO Make this an asynchronous operation.
                    if (!image.setTrack(image.getTrack() - 1)) {
                        status = S_SEEK_ERROR;
                        setInterruptRequest();
                        break;
                    }

                    status = S_INDEX;
                    if (image.getTrack() == 0)
                        status |= S_TRACK0;
                    if ((value & C_ARG_LOAD_HEAD) == C_ARG_LOAD_HEAD)
                        status |= S_HEAD_LOADED;

                    if ((command & C_STEP_OUT_UPDATE) != 0)
                        track = image.getTrack();

                    setInterruptRequest();
                    break;
                }

                // Type II commands

                case C_READ_SECTOR:
                case C_READ_SECTOR_MULTIPLE: {
                    readsLeft = beginDiskIO(command, image);
                    setDataRequest();
                    break;
                }
                case C_WRITE_SECTOR:
                case C_WRITE_SECTOR_MULTIPLE: {
                    writesLeft = beginDiskIO(command, image);
                    setDataRequest();
                    break;
                }

                // Type III commands

                case C_READ_ADDRESS: {
                    // Not supported.
                    status = S_RECORD_NOT_FOUND;
                    setInterruptRequest();
                    break;
                }
                case C_READ_TRACK: {
                    // Not supported.
                    status = S_RECORD_NOT_FOUND;
                    setInterruptRequest();
                    break;
                }
                case C_WRITE_TRACK: {
                    // Not supported.
                    status = S_RECORD_NOT_FOUND;
                    setInterruptRequest();
                    break;
                }

                // Type IV commands

                case C_FORCE_INTERRUPT: {
                    writesLeft = readsLeft = 0;
                    if (isBusy()) status &= ~S_BUSY;
                    else status = (image != null && image.getTrack() == 0) ? S_TRACK0 : 0;
                    if ((value & C_ARG_IMMEDIATE_IRQ) == C_ARG_IMMEDIATE_IRQ)
                        setInterruptRequest();
                    break;
                }
            }
        }

        private int beginDiskIO(final int command, @Nullable final DiskImage image) {
            if (image == null) {
                status = S_RECORD_NOT_FOUND;
                setInterruptRequest();
                return 0;
            }

            final int side;
            if ((command & C_ARG_SIDE_COMPARE) == C_ARG_SIDE_COMPARE)
                side = ((command & C_ARG_SIDE_FLAG) == C_ARG_SIDE_FLAG) ? 1 : 0;
            else
                side = this.side;

            if (!image.seek(side, track, sector)) {
                status = S_RECORD_NOT_FOUND;
                setInterruptRequest();
                return 0;
            }

            if ((command & C_ARG_MULTIPLE) != 0)
                return (image.getSectorCount() - sector) * image.getSectorSize();
            else
                return image.getSectorSize();
        }

        private int readTrack(final long address) {
            return track;
        }

        private void writeTrack(final long address, final int value) {
            if (!isBusy()) {
                track = value & 0xFF;
            }
        }

        private int readSector(final long address) {
            return sector + 1;
        }

        private void writeSector(final long address, final int value) {
            if (!isBusy()) {
                sector = (value - 1) & 0xFF;
            }
        }

        private int readData(final long address) {
            if (readsLeft > 0) {
                final DiskImage image = getDisk();
                if (image == null) {
                    finishRead();
                    return 0xFF;
                }

                try {
                    data = image.read();
                    if (--readsLeft > 0) {
                        wait = 255;
                        if ((readsLeft % image.getSectorSize()) == 0) {
                            ++sector;
                        }
                    } else {
                        finishRead();
                    }
                    return data;
                } catch (final IndexOutOfBoundsException e) {
                    finishRead();
                }
            }

            return 0xFF;
        }

        private void writeData(final long address, final int value) {
            data = value & 0xFF;

            if (writesLeft > 0) {
                final DiskImage image = getDisk();
                if (image == null) {
                    finishWrite();
                    return;
                }

                try {
                    image.write(value);
                    if (--writesLeft > 0) {
                        wait = 255;
                        if ((writesLeft % image.getSectorSize()) == 0) {
                            ++sector;
                        }
                    } else {
                        finishWrite();
                    }
                } catch (final IndexOutOfBoundsException e) {
                    finishWrite();
                }
            } // else: still writing even though we should be done... ignore it.
        }

        private int readInterrupt(final long address) {
            if (wait > 0) {
                if (--wait == 0) { // Timeout
                    writesLeft = readsLeft = 0;
                    status &= ~(S_DATA_REQUEST | S_BUSY);
                    status |= S_LOST_DATA;
                    setInterruptRequest();
                }
            }
            return irq;
        }

        private void writeSystem(final long address, final int value) {
            if (((system ^ value) & value & SC_RESET) != 0) {
                reset();
            }
            drive = value & SC_DRIVE;
            side = (value & SC_SIDE) == 0 ? 0 : 1;
            system = value;
        }

        // --------------------------------------------------------------------- //

        @Nullable
        private DiskImage getDisk() {
            return getDisk(drive);
        }

        private boolean isBusy() {
            return (status & S_BUSY) != 0;
        }

        private void finishRead() {
            readsLeft = 0;
            status &= ~(S_DATA_REQUEST | S_BUSY);
            setInterruptRequest();
        }

        private void finishWrite() {
            writesLeft = 0;
            status &= ~(S_DATA_REQUEST | S_BUSY);
            setInterruptRequest();
        }

        private void setInterruptRequest() {
            irq = WD1793_IRQ;
            triggerInterrupt(0, irq);
        }

        private void setDataRequest() {
            status |= S_DATA_REQUEST;
            irq = WD1793_DRQ;
            wait = 255;
            triggerInterrupt(1, irq);
        }

        private void clearRequest() {
            irq = 0;
        }

        // --------------------------------------------------------------------- //

        public interface DiskImage {
            int getTrackCount();

            int getSectorCount();

            int getSectorSize();

            int getTrack();

            boolean setTrack(final int value);

            boolean seek(final int side, final int track, final int sector);

            int read();

            void write(final int value);
        }

        public static final class DiskImageBuffer implements DiskImage {
            public final int sides;
            public final int tracks;
            public final int sectors;
            public final int sectorSize;
            public final ByteBuf data;
            private int track;
            private int offset;

            public DiskImageBuffer(final FloppyDisk disk) {
                sides = disk.getSideCount();
                tracks = disk.getTrackCount();
                sectors = disk.getSectorCount();
                sectorSize = disk.getSectorSize();
                this.data = disk.getData();
            }

            @Override
            public int getTrackCount() {
                return tracks;
            }

            @Override
            public int getSectorCount() {
                return sectors;
            }

            @Override
            public int getSectorSize() {
                return sectorSize;
            }

            public int getTrack() {
                return track;
            }

            public boolean setTrack(final int value) {
                if (track < 0 || track >= tracks) {
                    return false;
                }

                track = value;

                return true;
            }

            public boolean seek(final int side, final int track, final int sector) {
                if (side < 0 || side >= sides) {
                    return false;
                }
                if (track < 0 || track >= tracks) {
                    return false;
                }
                this.track = track;
                if (sector < 0 || sector >= sectors) {
                    return false;
                }
                offset = side * tracks * sectors * sectorSize + track * sectors * sectorSize + sector * sectorSize;
                return true;
            }

            public int read() {
                return data.getByte(offset++);
            }

            public void write(final int value) {
                data.setByte(offset++, value);
            }
        }
    }

    public final class FloppyDiskDriveImpl extends AbstractFloppyController implements BusStateListener {
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
            return Constants.DISK_DRIVE_ADDRESS;
        }

        // --------------------------------------------------------------------- //
        // AbstractFloppyController

        @Nullable
        @Override
        protected DiskImage getDisk(final int drive) {
            return drive == 0 ? image : null;
        }

        // --------------------------------------------------------------------- //
        // BusStateListener

        @Override
        public void handleBusOnline() {
        }

        @Override
        public void handleBusOffline() {
            reset();
        }
    }
}
