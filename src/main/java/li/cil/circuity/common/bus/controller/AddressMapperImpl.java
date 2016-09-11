package li.cil.circuity.common.bus.controller;

import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
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
public class AddressMapperImpl implements AddressMapper {
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
     * The controller hosting this system.
     */
    private final AbstractBusController controller;

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
    private int selectedMapping = 0;

    /**
     * The currently set word size of the bus.
     * <p>
     * This is the width of the data bus.
     */
    @Serialize
    private byte wordSize = 8;

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
    public void setDeviceAddress(final Addressable device, final AddressBlock address) {
        controller.scheduleScan();
        for (final Mapping mapping : mappings) {
            mapping.setDeviceAddress(device, address);
        }
    }

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
    public void setActiveConfiguration(final int index) {
        if (index < 0 || index >= mappings.length) {
            throw new IndexOutOfBoundsException();
        }
        selectedMapping = index;
    }

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
            final long mappedAddress = address - memory.getOffset();
            return device.read(mappedAddress);
        } else {
            segfault();
            return 0xFFFFFFFF;
        }
    }

    // --------------------------------------------------------------------- //
    // Subsystem

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

    @Override
    public boolean validate() {
        boolean areAllMappingsValid = true;
        for (final Mapping mapping : mappings) {
            // Important: & not &&, to make sure validate() is always called.
            areAllMappingsValid &= mapping.validate();
        }
        return areAllMappingsValid;
    }

    @Override
    public void dispose() {
        for (final Mapping mapping : mappings) {
            mapping.dispose();
        }
        selectedMapping = 0;
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

        public void add(final Addressable addressable) {
            if (persistentDeviceToAddress.containsKey(addressable.getPersistentId())) {
                final AddressBlock addressBlock = persistentDeviceToAddress.get(addressable.getPersistentId());
                deviceToAddress.put(addressable, addressBlock);
                addressToDevice.add(addressable, addressBlock.getOffset(), addressBlock.getLength());
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
            // mapping of address to device.
            return addressToDevice.size() == deviceToAddress.size();
        }

        public void dispose() {
            addressToDevice.clear();
            deviceToAddress.clear();
            persistentDeviceToAddress.clear();
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
