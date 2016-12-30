package li.cil.circuity.common.bus.controller;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.controller.detail.ElementManager;
import li.cil.circuity.api.bus.controller.detail.SerialInterfaceDeviceSelector;
import li.cil.circuity.api.bus.controller.detail.SerialInterfaceProvider;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.util.SerialPortManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.util.RangeMap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Serializable
public class AddressMapperImpl implements AddressMapper, ElementManager, SerialInterfaceProvider {
    /**
     * The number of addressable words via this buses address space.
     */
    public static final long ADDRESS_COUNT = 0x100000L;

    /**
     * Address block representing the full address space of the bus.
     */
    private static final AddressBlock FULL_ADDRESS_BLOCK = new AddressBlock(0, ADDRESS_COUNT);

    // --------------------------------------------------------------------- //

    /**
     * Lock used to ensure mutating calls happen synchronously, e.g. setting
     * addresses of devices (which may be called from the networking thread).
     */
    private final Object lock = new Object();

    /**
     * The controller hosting this system.
     */
    private final AbstractBusController controller;

    /**
     * The instance tracking the currently selected device for the serial interface.
     */
    private SerialInterfaceDeviceSelector selector;

    /**
     * Mapping of addressable devices to the address block they're assigned to.
     * <p>
     * We support multiple configurations that may be switched at any time.
     */
    @Serialize
    private final Mapping[] mappings = new Mapping[]{new Mapping(), new Mapping()};

    /**
     * The currently active mapping.
     */
    @Serialize
    private int selectedMapping;

    /**
     * The currently set word size of the bus.
     * <p>
     * This is the width of the data bus.
     */
    @Serialize
    private byte wordSize = 8;

    /**
     * Current index/shift of the address of the selected device for serial interface.
     */
    @Serialize
    private int addressShift;

    /**
     * Current index/shift of the size of the selected device for serial interface.
     */
    @Serialize
    private int sizeShift;

    // --------------------------------------------------------------------- //

    public AddressMapperImpl(final AbstractBusController controller) {
        this.controller = controller;
    }

    /**
     * Set the data bus width.
     * <p>
     * Note that changing this value completely resets the bus controller.
     * <p>
     * This method is thread safe.
     *
     * @param value the new data bus width.
     */
    public void setWordSize(final byte value) {
        controller.scheduleScan();
        this.wordSize = value;
    }

    // --------------------------------------------------------------------- //
    // AddressMapper

    @Override
    public int getWordSize() {
        return wordSize;
    }

    @Override
    public int getWordMask() {
        return 0xFFFFFFFF >>> (32 - wordSize);
    }

    @Override
    public int getConfigurationCount() {
        return mappings.length;
    }

    @Override
    public int getActiveConfiguration() {
        return selectedMapping;
    }

    @Override
    public void setActiveConfiguration(final int index) {
        if (index < 0 || index >= mappings.length) {
            throw new IndexOutOfBoundsException();
        }
        selectedMapping = index;
    }

    @Override
    public void setDeviceAddress(final Addressable device, final AddressBlock address) {
        if (!FULL_ADDRESS_BLOCK.contains(address)) {
            throw new IllegalArgumentException("Address out of bounds.");
        }
        // This call synchronizes with the controller's executor thread.
        controller.scheduleScan();
        synchronized (lock) {
            for (final Mapping mapping : mappings) {
                mapping.setDeviceAddress(device, address);
            }
        }
    }

    @Override
    public boolean isDeviceAddressValid(final Addressable device) {
        return mappings[selectedMapping].isDeviceAddressValid(device);
    }

    @Nullable
    @Override
    public AddressBlock getAddressBlock(final Addressable device) {
        return mappings[selectedMapping].deviceToAddress.get(device);
    }

    @Nullable
    @Override
    public Addressable getDevice(final long address) {
        return mappings[selectedMapping].addressToDevice.get(address);
    }

    @Override
    public void mapAndWrite(final long address, final int value) {
        final Addressable device = getDevice(address);
        if (device != null) {
            final AddressBlock memory = getAddressBlock(device);
            assert memory != null : "address mapper in corrupted state";
            final long mappedAddress = address - memory.getOffset();
            device.write(mappedAddress, value);
        } else {
            segfault();
        }
    }

    @Override
    public int mapAndRead(final long address) {
        final Addressable device = getDevice(address);
        if (device != null) {
            final AddressBlock memory = getAddressBlock(device);
            assert memory != null : "address mapper in corrupted state";
            final long mappedAddress = address - memory.getOffset();
            return device.read(mappedAddress);
        } else {
            segfault();
            return 0xFFFFFFFF;
        }
    }

    // --------------------------------------------------------------------- //
    // ElementManager

    @Override
    public void add(final BusElement element) {
        if (element instanceof Addressable) {
            final Addressable addressable = (Addressable) element;

            for (final Mapping mapping : mappings) {
                mapping.add(addressable);
            }
        }
    }

    @Override
    public void remove(final BusElement element) {
        if (element instanceof Addressable) {
            final Addressable addressable = (Addressable) element;

            for (final Mapping mapping : mappings) {
                mapping.remove(addressable);
            }
        }
    }

    // --------------------------------------------------------------------- //
    // Subsystem

    @Override
    public void reset() {
        addressShift = 0;
        sizeShift = 0;
    }

    @Override
    public boolean validate() {
        boolean areAllMappingsValid = true;
        for (final Mapping mapping : mappings) {
            // Important: & not &&, to make sure validate() is always called.
            areAllMappingsValid &= mapping.validate();
        }
        return areAllMappingsValid;
    }

    // --------------------------------------------------------------------- //
    // SerialInterfaceProvider

    @Override
    public void initializeSerialInterface(final SerialPortManager manager, final SerialInterfaceDeviceSelector selector) {
        this.selector = selector;
        selector.registerSelectionChangedListener(this::handleSelectedDeviceChanged);
        manager.addSerialPort(this::readDeviceAddress, this::writeResetDeviceAddressShift, null);
        manager.addSerialPort(this::readDeviceSize, this::writeResetDeviceSizeShift, null);
        manager.addSerialPort(this::readSelectedMapping, this::writeSelectedMapping, null);
    }

    public void handleSelectedDeviceChanged(final BusDevice newDevice) {
        addressShift = 0;
        sizeShift = 0;
    }

    // --------------------------------------------------------------------- //

    private int readDeviceAddress(final long address) {
        final BusDevice device = selector.getSelectedDevice();
        if (device instanceof Addressable) {
            final Addressable addressable = (Addressable) device;
            final AddressBlock memory = getAddressBlock(addressable);
            assert memory != null : "address mapper in corrupted state";
            return (int) ((memory.getOffset() >>> (addressShift++ * getWordSize())) & getWordMask());
        }
        return 0xFFFFFFFF;
    }

    private void writeResetDeviceAddressShift(final long address, final int value) {
        addressShift = 0;
    }

    private int readDeviceSize(final long address) {
        final BusDevice device = selector.getSelectedDevice();
        if (device instanceof Addressable) {
            final Addressable addressable = (Addressable) device;
            final AddressBlock memory = getAddressBlock(addressable);
            assert memory != null : "address mapper in corrupted state";
            return (int) ((memory.getLength() >>> (sizeShift++ * getWordSize())) & getWordMask());
        }
        return 0xFFFFFFFF;
    }

    private void writeResetDeviceSizeShift(final long address, final int value) {
        sizeShift = 0;
    }

    private int readSelectedMapping(final long address) {
        return selectedMapping;
    }

    private void writeSelectedMapping(final long address, final int value) {
        selectedMapping = value;

        if (selectedMapping < 0) {
            selectedMapping = 0;
        }
        if (selectedMapping >= mappings.length) {
            selectedMapping = mappings.length - 1;
        }
    }

    // --------------------------------------------------------------------- //

    void segfault() {
        // TODO Interrupt?
//        final InterruptMapper mapper = controller.getSubsystem(InterruptMapper.class);
//        mapper.interrupt(controller, 0, 0);
    }

    @Serializable
    private static final class Mapping {
        /**
         * Used to keep list of pending adds in order based on address hints,
         * so that lower hints get added first.
         */
        private static final Comparator<Addressable> ADDRESSABLE_COMPARATOR = Comparator.comparing(Mapping::compareAddressHints);

        // --------------------------------------------------------------------- //

        /**
         * Mapping of addresses to devices.
         */
        private final RangeMap<Addressable> addressToDevice = new RangeMap<>(ADDRESS_COUNT);

        /**
         * Mapping of devices to their currently mapped address block.
         */
        private final HashMap<Addressable, AddressBlock> deviceToAddress = new HashMap<>();
        @Serialize
        private final HashMap<UUID, AddressBlock> persistentDeviceToAddress = new HashMap<>();

        /**
         * List of pending adds, ordered by their address hint. This list is
         * processed in validate(), to ensure the order is based on the added
         * devices' address hints.
         */
        private final List<Addressable> pendingAdds = new ArrayList<>();

        // --------------------------------------------------------------------- //

        public void setDeviceAddress(final Addressable addressable, final AddressBlock addressBlock) {
            remove(addressable);
            deviceToAddress.put(addressable, addressBlock);
            persistentDeviceToAddress.put(addressable.getPersistentId(), addressBlock);
            addressToDevice.tryAdd(addressable, addressBlock.getOffset(), addressBlock.getLength());
        }

        public boolean isDeviceAddressValid(final Addressable addressable) {
            final AddressBlock referenceAddress = deviceToAddress.get(addressable);
            if (referenceAddress == null) {
                return false;
            }

            for (final AddressBlock address : deviceToAddress.values()) {
                if (address == referenceAddress) continue;
                if (address.intersects(referenceAddress)) {
                    return false;
                }
            }

            return true;
        }

        public void add(final Addressable addressable) {
            if (persistentDeviceToAddress.containsKey(addressable.getPersistentId())) {
                final AddressBlock addressBlock = persistentDeviceToAddress.get(addressable.getPersistentId());
                deviceToAddress.put(addressable, addressBlock);
                addressToDevice.tryAdd(addressable, addressBlock.getOffset(), addressBlock.getLength());
            } else {
                final int index = Collections.binarySearch(pendingAdds, addressable, ADDRESSABLE_COMPARATOR);
                pendingAdds.add(index < 0 ? ~index : index, addressable);
            }
        }

        public void remove(final Addressable addressable) {
            final AddressBlock addressBlock = deviceToAddress.remove(addressable);
            persistentDeviceToAddress.remove(addressable.getPersistentId());
            addressToDevice.remove(addressBlock.getOffset(), addressable);
        }

        public boolean validate() {
            for (final Addressable addressable : pendingAdds) {
                final AddressBlock addressBlock = tryGetFreeAddress(addressable);
                deviceToAddress.put(addressable, addressBlock);
                persistentDeviceToAddress.put(addressable.getPersistentId(), addressBlock);
                addressToDevice.tryAdd(addressable, addressBlock.getOffset(), addressBlock.getLength());
            }

            pendingAdds.clear();

            // We have no overlap if all devices were successfully added to the
            // mapping of address to device. Empty blocks are *not* added to the
            // range map, so we only want to count the non-empty ones.
            int nonEmptyCount = 0;
            for (final AddressBlock address : deviceToAddress.values()) {
                if (address.getLength() > 0) {
                    ++nonEmptyCount;
                }
            }
            return addressToDevice.size() == nonEmptyCount;
        }

        // --------------------------------------------------------------------- //

        private AddressBlock tryGetFreeAddress(final Addressable newAddressable) {
            final Iterator<RangeMap.Interval> it = addressToDevice.gapIterator();
            while (it.hasNext()) {
                final RangeMap.Interval gap = it.next();
                final AddressBlock candidate = new AddressBlock(gap.offset, gap.length);
                final AddressBlock requested = newAddressable.getPreferredAddressBlock(candidate);
                if (requested.getOffset() >= candidate.getOffset() && requested.getOffset() + requested.getLength() <= candidate.getOffset() + candidate.getLength()) {
                    return requested;
                }
            }

            return newAddressable.getPreferredAddressBlock(FULL_ADDRESS_BLOCK).clamp(FULL_ADDRESS_BLOCK);
        }

        private static int compareAddressHints(final Addressable addressable) {
            if (addressable instanceof AddressHint) {
                final AddressHint addressHint = (AddressHint) addressable;
                return addressHint.getSortHint();
            }
            return Integer.MAX_VALUE;
        }
    }
}
